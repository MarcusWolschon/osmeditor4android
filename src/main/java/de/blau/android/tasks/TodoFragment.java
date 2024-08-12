package de.blau.android.tasks;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
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
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, TodoFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = TodoFragment.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG         = "fragment_todo";
    private static final String CHANGED_KEY = "changed";

    private boolean changed; // comment text changed

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
        changed = savedInstanceState != null && savedInstanceState.getBoolean(CHANGED_KEY, false);
        commentLabel.setVisibility(View.GONE);
        comment.setVisibility(View.VISIBLE);
        comment.setHint(R.string.comment);
        removePadding(comment);
        title.setText(R.string.todo_title);
        comments.setText(((Todo) task).getListName());
        final String commentText = ((Todo) task).getTitle();
        if (commentText != null) {
            comment.setText(Util.fromHtml(commentText));
        }
        comment.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable edited) {
                changed = true;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // required, but not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // required, but not used
            }
        });
        addElementLinks(task, elementLayout);
        //
        return ArrayAdapter.createFromResource(getActivity(), R.array.todo_state, android.R.layout.simple_spinner_item);
    }

    @Override
    protected void onShowListener(Task task, Button save, Button upload, Button cancel, Spinner state) {
        Log.d(DEBUG_TAG, "onShowListener");
        // ignore the name of the buttons here, we simply re-purpose them
        upload.setText(R.string.Done);
        upload.setEnabled(true);
        upload.setOnClickListener((View v) -> {
            if (getContext() == null) {
                Log.e(DEBUG_TAG, "Save button onClickListener context is null");
                return;
            }
            String commentText = ((Todo) task).getTitle();
            if (commentText == null) {
                commentText = "";
            }
            if (pos2state(state.getSelectedItemPosition()) != task.getState() || changed) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.save_changes_title);
                builder.setPositiveButton(R.string.Yes, (dialog, id) -> {
                    saveTask(v, task);
                    dismiss();
                });
                builder.setNegativeButton(R.string.No, (dialog, id) -> dismiss());
                builder.setNeutralButton(R.string.cancel, null);
                builder.show();
                return;
            }
            dismiss();
        });
        final List<Todo> todos = App.getTaskStorage().getTodos(((Todo) task).getListName(), false);
        final int count = todos.size();
        final boolean nextExists = count > 1 || (count == 1 && !task.equals(todos.get(0)));
        save.setText(R.string.menu_todo_close_and_next);
        save.setEnabled(nextExists);
        save.setOnClickListener((View v) -> {
            state.setSelection(state2pos(State.CLOSED));
            showNext(v, task, todos);
        });
        cancel.setText(R.string.menu_todo_skip_and_next);
        cancel.setEnabled(nextExists);
        cancel.setOnClickListener((View v) -> {
            state.setSelection(state2pos(State.SKIPPED));
            showNext(v, task, todos);
        });
    }

    /**
     * Show the next todo // NOSONAR
     * 
     * @param v the current View
     * @param task the current Task
     * @param todos a list of Todos //NOSONAR
     */
    private void showNext(@NonNull View v, @NonNull Task task, @NonNull final List<Todo> todos) {
        saveTask(v, task);
        final FragmentActivity activity = getActivity();
        Todo next = ((Todo) task).getNearest(todos);
        if (activity instanceof Main) {
            Map map = ((Main) activity).getMap();
            map.getViewBox().moveTo(map, next.getLon(), next.getLat());
        }
        TodoFragment.showDialog(activity, next);
    }

    @Override
    protected <T extends Task> void saveTaskSpecific(T task) {
        ((Todo) task).setTitle(HtmlCompat.toHtml(comment.getText(), HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE));
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(CHANGED_KEY, changed);
    }
}
