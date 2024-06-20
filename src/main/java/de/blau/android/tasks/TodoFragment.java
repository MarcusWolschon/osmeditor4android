package de.blau.android.tasks;

import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.osm.Server;
import de.blau.android.tasks.Task.State;
import de.blau.android.util.Util;

/**
 * Very simple dialog fragment to display an Todo // NOSONAR
 * 
 * @author Simon
 *
 */
public class TodoFragment extends BugFragment {
    private static final String DEBUG_TAG = TodoFragment.class.getSimpleName().substring(0, Math.min(23, TodoFragment.class.getSimpleName().length()));

    private static final String TAG = "fragment_todo";

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
            TodoFragment taskFragment = newInstance(t);
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
    private static TodoFragment newInstance(@NonNull Task t) {
        TodoFragment f = new TodoFragment();

        Bundle args = new Bundle();
        args.putSerializable(BUG_KEY, t);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    protected <T extends Task> ArrayAdapter<CharSequence> setupView(Bundle savedInstanceState, View v, T task) {
        // these are only used for Notes
        commentLabel.setVisibility(View.GONE);
        comment.setVisibility(View.GONE);
        title.setText(R.string.todo_title);
        comments.setText(Util.fromHtml(((Bug) task).getLongDescription(getActivity(), false)));
        addElementLinks(task, elementLayout);
        //
        return ArrayAdapter.createFromResource(getActivity(), R.array.todo_state, android.R.layout.simple_spinner_item);
    }

    @Override
    protected void onShowListener(Task task, Button save, Button upload, Button cancel, Spinner state) {
        save.setText(R.string.Done);
        save.setEnabled(true);
        cancel.setText(R.string.next);
        final List<Todo> todos = App.getTaskStorage().getTodos(((Todo) task).getListName(), false);
        final int count = todos.size();
        cancel.setEnabled(count > 1 || (count == 1 && !task.equals(todos.get(0))));
        cancel.setOnClickListener((View v) -> {
            saveTask(v, task);
            final FragmentActivity activity = getActivity();
            Todo next = ((Todo) task).getNearest(todos);
            if (activity instanceof Main) {
                Map map = ((Main) activity).getMap();
                map.getViewBox().moveTo(map, next.getLon(), next.getLat());
            }
            TodoFragment.showDialog(activity, next);
        });
    }

    @Override
    protected <T extends Task> void enableStateSpinner(T task) {
        state.setEnabled(true);
    }

    @Override
    protected State pos2state(int position) {
        String[] array = getResources().getStringArray(R.array.todo_state_values);
        return State.valueOf(array[position]);
    }

    @Override
    protected int state2pos(State state) {
        String[] array = getResources().getStringArray(R.array.todo_state_values);
        return Arrays.asList(array).indexOf(state.name());
    }

    @Override
    protected <T extends Task> void update(Server server, PostAsyncActionHandler handler, T task) {
        // do nothing
    }
}
