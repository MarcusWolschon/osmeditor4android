package de.blau.android.tasks;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.content.Context;
import android.text.SpannableString;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.exception.OsmServerException;
import de.blau.android.layer.data.MapOverlay;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.GeoMath;
import de.blau.android.util.ScreenMessage;
import io.noties.markwon.Markwon;

/**
 * Very simple dialog fragment to display an OSMOSE bug or similar
 * 
 * @author Simon
 *
 */
public abstract class BugFragment extends TaskFragment {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, BugFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = BugFragment.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * Add links to elements to the layout
     * 
     * @param <T> the task type
     * @param task the task
     * @param layout the layout we are adding the links too
     */
    protected <T extends Task> void addElementLinks(@NonNull T task, @NonNull LinearLayout layout) {
        final StorageDelegator storageDelegator = App.getDelegator();
        final List<OsmElement> elements = ((Bug) task).getElements();
        if (!elements.isEmpty() && comment.getVisibility() != View.VISIBLE) {
            final View ruler = inflater.inflate(R.layout.ruler, null);
            layout.addView(ruler);
        }
        for (final OsmElement e : elements) {
            String text;
            if (e.getOsmVersion() < 0) { // fake element
                text = getString(R.string.bug_element_1, e.getName(), e.getOsmId());
            } else { // real
                text = getString(R.string.bug_element_2, e.getName(), e.getDescription(false));
            }
            TextView tv = new TextView(getActivity());
            boolean deleted = OsmElement.STATE_DELETED == e.getState();
            if (!deleted && (getActivity() instanceof Main)) { // only make clickable if in Main
                tv.setClickable(true);
                tv.setOnClickListener(unused -> {
                    dismiss();
                    final int lonE7 = task.getLon();
                    final int latE7 = task.getLat();
                    final FragmentActivity activity = getActivity();
                    if (activity instanceof Main) { // activity may have vanished so re-check
                        gotoAndEditElement((Main) activity, storageDelegator, e, lonE7, latE7);
                    }
                });
                tv.setTextColor(ContextCompat.getColor(getActivity(), R.color.holo_blue_light));
            }
            SpannableString spannable = new SpannableString(text);
            if (deleted) {
                spannable.setSpan(new StrikethroughSpan(), 0, spannable.length(), 0);
            }
            tv.setText(spannable);
            layout.addView(tv);
        }
    }

    /**
     * Goto the location of an OsmElement and start editing
     * 
     * @param main an instance of Main
     * @param storageDelegator the current StorageDelegator
     * @param e the OsmElement, real or fake
     * @param lonE7 longitude of Bug
     * @param latE7 latitude of Bug
     */
    public static void gotoAndEditElement(@NonNull final Main main, @NonNull final StorageDelegator storageDelegator, @NonNull final OsmElement e,
            final int lonE7, final int latE7) {
        main.unlock();
        if (e.getOsmVersion() >= 0) { // real element
            main.zoomToAndEdit(lonE7, latE7, e);
            return;
        }
        // fake element, real element has to be downloaded
        new ExecutorTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void arg) throws SAXException, IOException, ParserConfigurationException {
                final Logic logic = App.getLogic();
                final String elementName = e.getName();
                final long osmId = e.getOsmId();
                BoundingBox issueBox = GeoMath.createBoundingBoxForCoordinates(latE7 / 1E7D, lonE7 / 1E7, MapOverlay.DEFAULT_AUTO_DOWNLOAD_RADIUS);
                OsmElement element = logic.getElementWithDeletedSync(main, elementName, osmId);
                if (element.getState() != OsmElement.STATE_DELETED) {
                    BoundingBox b = element.getBounds();
                    if (b != null && !(b.contains(issueBox) || b.intersects(issueBox))) {
                        // warning that element has been moved
                        issueBox = b;
                        main.runOnUiThread(() -> ScreenMessage.toastTopWarning(main, R.string.toast_element_may_have_been_moved));
                    }
                } else {
                    // warning that element has been deleted
                    main.runOnUiThread(() -> ScreenMessage.toastTopWarning(main, R.string.toast_element_has_been_deleted));
                }
                logic.downloadBox(main, issueBox, true, () -> {
                    OsmElement osm = storageDelegator.getOsmElement(elementName, osmId);
                    if (osm != null) {
                        main.zoomToAndEdit(lonE7, latE7, osm);
                    } else {
                        Log.e(DEBUG_TAG, "Element " + elementName + osmId + " not found in downloaded data");
                    }
                });
                return null;
            }

            @Override
            protected void onBackgroundError(Exception e) {
                Log.e(DEBUG_TAG, e.getMessage());
                if (e instanceof OsmServerException) {
                    main.runOnUiThread(() -> ScreenMessage.toastTopWarning(main,
                            main.getString(R.string.toast_download_failed, ((OsmServerException) e).getHttpErrorCode(), e.getLocalizedMessage())));
                    return;
                }
                main.runOnUiThread(() -> ScreenMessage.toastTopWarning(main, e.getLocalizedMessage()));
            }
        }.execute();

    }

    /**
     * Display some markdown formatted text in a dialog
     * 
     * @param context an Android Context
     * @param text the text
     */
    protected void showHelpText(@NonNull final Context context, @Nullable String text) {
        if (text != null) {
            final Markwon markwon = Markwon.create(context);
            showAdditionalText(context, markwon.toMarkdown(text));
        } else {
            ScreenMessage.toastTopWarning(context, R.string.toast_nothing_found);
        }
    }
}
