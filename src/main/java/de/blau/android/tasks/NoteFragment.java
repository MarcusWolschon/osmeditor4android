package de.blau.android.tasks;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.Arrays;

import android.os.Bundle;
import android.text.Editable;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.osm.Server;
import de.blau.android.tasks.Task.State;
import de.blau.android.util.AfterTextChangedWatcher;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.Util;

/**
 * Dialog fragment to display a note
 * 
 * @author Simon
 *
 */
public class NoteFragment extends TaskFragment {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, NoteFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = NoteFragment.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "fragment_note";

    private static final String COMMENT_KEY = "comment";

    /**
     * Display a dialog for editing Notes
     * 
     * @param activity the calling FragmentActivity
     * @param t Task we want to edit
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull Task t) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            NoteFragment taskFragment = (NoteFragment) fm.findFragmentByTag(TAG);
            if (taskFragment == null) {
                Log.i(DEBUG_TAG, "Creating new instance");
                taskFragment = newInstance(t);
                taskFragment.show(fm, TAG);
            }
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
     * @return fragment
     */
    private static NoteFragment newInstance(@NonNull Task t) {
        NoteFragment f = new NoteFragment();

        Bundle args = new Bundle();
        args.putSerializable(BUG_KEY, t);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String c = comment.getText().toString();
        if (!"".equals(c)) {
            Log.d(DEBUG_TAG, "onSaveInstanceState saved " + c);
            outState.putString(COMMENT_KEY, c);
        }
    }

    @Override
    protected <T extends Task> void update(Server server, PostAsyncActionHandler handler, T task) {
        Note n = (Note) task;
        NoteComment nc = n.getLastComment();
        TransferTasks.uploadNote(getActivity(), server, n, nc != null && nc.isNew() ? nc : null, n.getState() == State.CLOSED, handler);
    }

    @Override
    protected <T extends Task> ArrayAdapter<CharSequence> setupView(Bundle savedInstanceState, View v, T task) {
        title.setText(getString((task.isNew() && ((Note) task).count() == 0) ? R.string.openstreetbug_new_title : R.string.openstreetbug_edit_title));
        comments.setText(Util.fromHtml(((Note) task).getComment())); // ugly
        comments.setAutoLinkMask(Linkify.WEB_URLS);
        comments.setMovementMethod(LinkMovementMethod.getInstance());
        comments.setTextIsSelectable(true);
        elementLayout.setVisibility(View.GONE); // not used for notes
        boolean hasSavedState = savedInstanceState != null && savedInstanceState.containsKey(COMMENT_KEY);
        NoteComment lastComment = ((Note) task).getLastComment();
        String commentText = "";
        if (hasSavedState) {
            commentText = savedInstanceState.getString(COMMENT_KEY);
        } else if (lastComment != null && lastComment.isNew()) {
            commentText = lastComment.getText();
        }
        comment.setText(commentText);
        comment.setFocusable(true);
        comment.setFocusableInTouchMode(true);
        comment.setEnabled(true);
        return ArrayAdapter.createFromResource(getActivity(), R.array.note_state, android.R.layout.simple_spinner_item);
    }

    @Override
    protected <T extends Task> void enableStateSpinner(T task) {
        state.setEnabled(!task.isNew());
    }

    @Override
    protected void onShowListener(Task task, Button save, Button upload, Button cancel, Spinner state) {
        super.onShowListener(task, save, upload, cancel, state);
        comment.addTextChangedListener(new AfterTextChangedWatcher() {
            final String original = comment.getText().toString();

            @Override
            public void afterTextChanged(Editable edited) {
                boolean changed = !original.equals(edited.toString()) || NoteFragment.super.changed(state.getSelectedItemPosition());
                save.setEnabled(changed);
                upload.setEnabled(changed);
                final int openPos = state2pos(State.OPEN);
                if (changed && state.getSelectedItemPosition() != openPos) {
                    state.setSelection(openPos);
                    ScreenMessage.toastTopInfo(getContext(), R.string.toast_note_reopened);
                }
            }
        });
    }

    @Override
    protected boolean changed(int newState) {
        return comment.length() != 0 || super.changed(newState);
    }

    @Override
    protected <T extends Task> void saveTaskSpecific(T task) {
        String c = comment.getText().toString();
        NoteComment lastComment = ((Note) task).getLastComment();
        if (lastComment != null && lastComment.isNew()) {
            if (c.length() > 0 || super.changed(state.getSelectedItemPosition())) {
                lastComment.setText(c);
            } else {
                ((Note) task).removeLastComment();
                task.setChanged(false);
            }
        } else if (c.length() > 0) {
            ((Note) task).addComment(c);
        }
    }

    @Override
    protected State pos2state(int position) {
        String[] array = getResources().getStringArray(R.array.note_state_values);
        return State.valueOf(array[position]);
    }

    @Override
    protected int state2pos(State state) {
        String[] array = getResources().getStringArray(R.array.note_state_values);
        return Arrays.asList(array).indexOf(state.name());
    }
}
