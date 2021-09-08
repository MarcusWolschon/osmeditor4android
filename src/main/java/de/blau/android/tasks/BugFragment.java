package de.blau.android.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Server;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.tasks.OsmoseMeta.OsmoseClass;
import de.blau.android.util.GeoMath;
import de.blau.android.util.NetworkStatus;
import de.blau.android.util.Snack;
import de.blau.android.util.Util;
import io.noties.markwon.Markwon;

/**
 * Very simple dialog fragment to display an OSMOSE bug
 * 
 * @author Simon
 *
 */
public class BugFragment extends TaskFragment {
    private static final String DEBUG_TAG = BugFragment.class.getSimpleName();

    private static final String TAG = "fragment_bug";

    /**
     * Display a dialog for editing OSMOSE bugs
     * 
     * @param activity the calling FragmentActivity
     * @param t Task we want to edit
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull Task t) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            BugFragment taskFragment = newInstance(t);
            taskFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the Dialog
     * 
     * @param activity the calling FragmentActivity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new fragment to be displayed
     * 
     * @param t Task to show
     * @return the fragment
     */
    private static BugFragment newInstance(@NonNull Task t) {
        BugFragment f = new BugFragment();

        Bundle args = new Bundle();
        args.putSerializable(BUG_KEY, t);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    protected <T extends Task> void update(Server server, PostAsyncActionHandler handler, T task) {
        TransferTasks.updateOsmoseBug(getActivity(), (OsmoseBug) task, false, handler);
    }

    @Override
    protected <T extends Task> ArrayAdapter<CharSequence> setupView(Bundle savedInstanceState, View v, T task) {
        final boolean isCustomBug = task instanceof CustomBug;
        // these are only used for Notes
        commentLabel.setVisibility(View.GONE);
        comment.setVisibility(View.GONE);
        //

        title.setText(R.string.openstreetbug_bug_title);
        comments.setText(Util.fromHtml(((Bug) task).getLongDescription(getActivity(), false)));
        if (!isCustomBug && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // provide dialog with some additional text
            TextView instructionText = new TextView(getActivity());
            instructionText.setClickable(true);
            instructionText.setOnClickListener(unused -> {
                final Context context = getContext();
                if (context != null) {
                    final Markwon markwon = Markwon.create(context);
                    OsmoseMeta meta = App.getTaskStorage().getOsmoseMeta();
                    final int itemId = ((OsmoseBug) task).getOsmoseItem();
                    final int classId = ((OsmoseBug) task).getOsmoseClass();
                    OsmoseClass osmoseClass = meta.getOsmoseClass(itemId, classId);
                    if (osmoseClass == null) {
                        if (new NetworkStatus(context).isConnected()) {
                            new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected Void doInBackground(Void... arg0) {
                                    OsmoseServer.getMeta(context, itemId, classId);
                                    return null;
                                }

                                @Override
                                protected void onPostExecute(Void arg0) {
                                    OsmoseClass osmoseClass = meta.getOsmoseClass(itemId, classId);
                                    if (osmoseClass != null) {
                                        String text = osmoseClass.getText();
                                        if (text != null) {
                                            showAdditionalText(context, markwon.toMarkdown(text));
                                        }
                                    }
                                }

                            }.execute();
                        } else {
                            Snack.toastTopWarning(context, R.string.network_required);
                        }
                    } else {
                        String text = osmoseClass.getText();
                        if (text != null) {
                            showAdditionalText(context, markwon.toMarkdown(text));
                        }
                    }
                }
            });
            instructionText.setTextColor(ContextCompat.getColor(getContext(), R.color.holo_blue_light));
            instructionText.setText(R.string.maproulette_task_explanations);
            elementLayout.addView(instructionText);
        }
        final StorageDelegator storageDelegator = App.getDelegator();
        for (final OsmElement e : ((Bug) task).getElements()) {
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
                        if (e.getOsmVersion() < 0) { // fake element
                            try {
                                BoundingBox b = GeoMath.createBoundingBoxForCoordinates(latE7 / 1E7D, lonE7 / 1E7, 50, true);
                                App.getLogic().downloadBox(activity, b, true, () -> {
                                    OsmElement osm = storageDelegator.getOsmElement(e.getName(), e.getOsmId());
                                    if (osm != null && activity != null && activity instanceof Main) {
                                        ((Main) activity).zoomToAndEdit(lonE7, latE7, osm);
                                    }
                                });
                            } catch (OsmException e1) {
                                Log.e(DEBUG_TAG, "onCreateDialog got " + e1.getMessage());
                            }
                        } else { // real
                            ((Main) activity).zoomToAndEdit(lonE7, latE7, e);
                        }
                    }
                });
                tv.setTextColor(ContextCompat.getColor(getActivity(), R.color.holo_blue_light));
            }
            tv.setText(text);
            elementLayout.addView(tv);
        }
        //
        return ArrayAdapter.createFromResource(getActivity(), R.array.bug_state, android.R.layout.simple_spinner_item);
    }

    @Override
    protected <T extends Task> void enableStateSpinner(T task) {
        boolean uploadedOsmoseBug = !(task instanceof CustomBug) && task.isClosed() && !task.hasBeenChanged();
        state.setEnabled(!task.isNew() && !uploadedOsmoseBug); // new bugs always open and OSMOSE bugs can't be reopened
        // once uploaded
    }
}
