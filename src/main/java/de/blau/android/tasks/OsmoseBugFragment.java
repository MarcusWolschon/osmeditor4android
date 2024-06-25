package de.blau.android.tasks;

import java.util.Arrays;

import android.content.Context;
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
import de.blau.android.Logic;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.osm.Server;
import de.blau.android.tasks.OsmoseMeta.OsmoseClass;
import de.blau.android.tasks.Task.State;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.NetworkStatus;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.Util;

/**
 * Very simple dialog fragment to display an OSMOSE bug
 * 
 * @author Simon
 *
 */
public class OsmoseBugFragment extends BugFragment {
    private static final String DEBUG_TAG = OsmoseBugFragment.class.getSimpleName().substring(0,
            Math.min(23, OsmoseBugFragment.class.getSimpleName().length()));

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
            OsmoseBugFragment taskFragment = newInstance(t);
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
    private static OsmoseBugFragment newInstance(@NonNull Task t) {
        OsmoseBugFragment f = new OsmoseBugFragment();

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
        // these are only used for Notes
        commentLabel.setVisibility(View.GONE);
        comment.setVisibility(View.GONE);

        title.setText(R.string.openstreetbug_bug_title);
        comments.setText(Util.fromHtml(((Bug) task).getLongDescription(getActivity(), false)));
        // provide dialog with some additional text
        OsmoseMeta meta = App.getTaskStorage().getOsmoseMeta();
        final String itemId = ((OsmoseBug) task).getOsmoseItem();
        final int classId = ((OsmoseBug) task).getOsmoseClass();
        OsmoseClass osmoseClass = meta.getOsmoseClass(itemId, classId);
        if (osmoseClass == null || osmoseClass.hasHelpText()) {
            TextView instructionText = new TextView(getActivity());
            instructionText.setClickable(true);
            instructionText.setOnClickListener(unused -> {
                final Context context = getContext();
                if (context != null) {
                    if (osmoseClass == null) {
                        getAndShowOsmoseClass(context, meta, itemId, classId);
                    } else {
                        showHelpText(context, osmoseClass.getHelpText());
                    }
                }
            });
            instructionText.setTextColor(ContextCompat.getColor(getContext(), R.color.holo_blue_light));
            instructionText.setText(R.string.more_information);
            elementLayout.addView(instructionText);
        }
        addElementLinks(task, elementLayout);
        //
        return ArrayAdapter.createFromResource(getActivity(), R.array.bug_state, android.R.layout.simple_spinner_item);
    }

    /**
     * Retrieve the meta information from the OSMOSE server, store and display any help text
     * 
     * @param context an Android Context
     * @param meta the storage for the meta information
     * @param itemId Osmose item
     * @param classId Osmose class
     */
    private void getAndShowOsmoseClass(@NonNull final Context context, @NonNull OsmoseMeta meta, final String itemId, final int classId) {
        if (new NetworkStatus(context).isConnected()) {
            Logic logic = App.getLogic();
            new ExecutorTask<Void, Void, Void>(logic.getExecutorService(), logic.getHandler()) {
                @Override
                protected Void doInBackground(Void arg0) {
                    OsmoseServer.getMeta(App.getPreferences(getActivity()).getOsmoseServer(), itemId, classId);
                    return null;
                }

                @Override
                protected void onPostExecute(Void arg0) {
                    OsmoseClass osmoseClass = meta.getOsmoseClass(itemId, classId);
                    if (osmoseClass != null) {
                        showHelpText(context, osmoseClass.getHelpText());
                    }
                }

            }.execute();
        } else {
            ScreenMessage.toastTopWarning(context, R.string.network_required);
        }
    }

    @Override
    protected <T extends Task> void enableStateSpinner(T task) {
        boolean uploadedOsmoseBug = !(task instanceof Todo) && task.isClosed() && !task.hasBeenChanged();
        // new bugs always open and OSMOSE bugs can't be reopened once uploaded
        state.setEnabled(!task.isNew() && !uploadedOsmoseBug);
    }

    @Override
    protected State pos2state(int position) {
        String[] array = getResources().getStringArray(R.array.bug_state_values);
        return State.valueOf(array[position]);
    }

    @Override
    protected int state2pos(State state) {
        String[] array = getResources().getStringArray(R.array.bug_state_values);
        return Arrays.asList(array).indexOf(state.name());
    }
}
