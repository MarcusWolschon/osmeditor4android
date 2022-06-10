package de.blau.android.tasks;

import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Snack;
import io.noties.markwon.Markwon;

/**
 * Very simple dialog fragment to display an OSMOSE bug or similar
 * 
 * @author Simon
 *
 */
public abstract class BugFragment extends TaskFragment {
    private static final String DEBUG_TAG = BugFragment.class.getSimpleName();

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
        if (!elements.isEmpty()) {
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
            if (getActivity() instanceof Main) { // only make clickable if in Main
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
            tv.setText(text);
            layout.addView(tv);
        }
    }

    /**
     * Goto the location of an OsmELement and start editing
     * 
     * @param main an instance of Main
     * @param storageDelegator the current StorageDelegator
     * @param e the OsmElement, real or fake
     * @param lonE7 longitude of Bug
     * @param latE7 latitude of Bug
     */
    public static void gotoAndEditElement(@NonNull final Main main, @NonNull final StorageDelegator storageDelegator, @NonNull final OsmElement e,
            final int lonE7, final int latE7) {
        if (e.getOsmVersion() < 0) { // fake element
            try {
                BoundingBox b = GeoMath.createBoundingBoxForCoordinates(latE7 / 1E7D, lonE7 / 1E7, 50);
                App.getLogic().downloadBox(main, b, true, () -> {
                    OsmElement osm = storageDelegator.getOsmElement(e.getName(), e.getOsmId());
                    if (osm != null) {
                        main.zoomToAndEdit(lonE7, latE7, osm);
                    }
                });
            } catch (OsmException e1) {
                Log.e(DEBUG_TAG, "setupView got " + e1.getMessage());
            }
        } else { // real
            main.zoomToAndEdit(lonE7, latE7, e);
        }
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
            Snack.toastTopWarning(context, R.string.toast_nothing_found);
        }
    }
}
