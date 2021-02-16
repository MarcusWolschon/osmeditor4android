package de.blau.android.tasks;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.osm.Server;
import de.blau.android.tasks.Task.State;
import de.blau.android.util.Util;

/**
 * Very simple dialog fragment to display bug or notes etc
 * 
 * This started off simple, but is now far too complex and should be split up in to separate classes
 * 
 * @author Simon
 *
 */
public class NoteFragment extends TaskFragment {
    private static final String DEBUG_TAG = NoteFragment.class.getSimpleName();

    private static final String TAG       = "fragment_note";

    /**
     * Display a dialog for editing Notes
     * 
     * @param activity
     *            the calling FragmentActivity
     * @param t
     *            Task we want to edit
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull Task t) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            NoteFragment taskFragment = newInstance(t);
            taskFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the Dialog
     * 
     * @param activity
     *            the calling FragmentActivity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new fragment to be displayed
     * 
     * @param t
     *            Task to show
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
    }
    
    @Override
    protected <T extends Task> void update(Server server, PostAsyncActionHandler handler, T task) {
        Note n = (Note) task;
        NoteComment nc = n.getLastComment();
        TransferTasks.uploadNote(getActivity(), server, n, (nc != null && nc.isNew()) ? nc.getText() : null,
                n.getState() == State.CLOSED, false, handler);
    }

    @Override
    protected <T extends Task> ArrayAdapter<CharSequence> setupView(Bundle savedInstanceState, View v, T task) {
        title.setText(getString((task.isNew() && ((Note) task).count() == 0) ? R.string.openstreetbug_new_title
                : R.string.openstreetbug_edit_title));
        comments.setText(Util.fromHtml(((Note) task).getComment())); // ugly
        comments.setAutoLinkMask(Linkify.WEB_URLS);
        comments.setMovementMethod(LinkMovementMethod.getInstance());
        comments.setTextIsSelectable(true);
        NoteComment nc = ((Note) task).getLastComment();
        elementLayout.setVisibility(View.GONE); // not used for notes
        if ((task.isNew() && ((Note) task).count() == 0) || (nc != null && !nc.isNew())) {
            // only show comment field if we don't have an unsaved comment
            Log.d(DEBUG_TAG, "enabling comment field");
            comment.setText("");
            comment.setFocusable(true);
            comment.setFocusableInTouchMode(true);
            comment.setEnabled(true);
        } else {
            commentLabel.setVisibility(View.GONE);
            comment.setVisibility(View.GONE);
        }
        return ArrayAdapter.createFromResource(getActivity(), R.array.note_state, android.R.layout.simple_spinner_item);
    }

    @Override
    protected <T extends Task> void enableStateSpinner(T task) {
        state.setEnabled(!task.isNew());
    }

    @Override
    protected void onShowListener(Button save, Button upload) {
        comment.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                // required, but not used
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // required, but not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                save.setEnabled(true);
                upload.setEnabled(true);
                state.setSelection(State.OPEN.ordinal());
            }
        });
    }
}
