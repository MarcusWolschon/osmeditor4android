package de.blau.android.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.listener.UpdateViewListener;
import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import de.blau.android.tasks.Task.State;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.IssueAlert;

/**
 * Very simple dialog fragment to display bug or notes etc
 * 
 * This started off simple, but is now far too complex and should be split up in to separate classes
 * 
 * @author Simon
 *
 */
public abstract class TaskFragment extends ImmersiveDialogFragment {
    private static final String   DEBUG_TAG = TaskFragment.class.getSimpleName();

    protected static final String BUG_KEY   = "bug";

    private UpdateViewListener    mListener;

    private Task                  task      = null;

    protected TextView            title;
    protected TextView            comments;
    protected EditText            comment;
    protected TextView            commentLabel;
    protected LinearLayout        elementLayout;
    protected Spinner             state;

    @SuppressLint({ "NewApi", "InflateParams" })
    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        task = (Task) getArguments().getSerializable(BUG_KEY);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final Preferences prefs = new Preferences(getActivity());

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View v = inflater.inflate(R.layout.openstreetbug_edit, null);
        builder.setView(v).setPositiveButton(R.string.save, (dialog, id) -> {
            saveTask(v, task);
            cancelAlert(task);
            updateMenu(getActivity());
        }).setNegativeButton(R.string.cancel, (dialog, id) -> {
            // unused
        });

        if (task.canBeUploaded()) {
            builder.setNeutralButton(R.string.transfer_download_current_upload, (dialog, id) -> {
                saveTask(v, task);
                final FragmentActivity activity = getActivity();
                if (activity == null || !isAdded()) {
                    Log.e(DEBUG_TAG, "Activity vanished");
                    return;
                }
                (new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... arg0) {
                        PostAsyncActionHandler handler = new PostAsyncActionHandler() {
                            @Override
                            public void onSuccess() {
                                updateMenu(activity);
                            }

                            @Override
                            public void onError() {
                                updateMenu(activity);
                            }
                        };
                        update(prefs.getServer(), handler, task);
                        return null;
                    }
                }).execute();
                cancelAlert(task);
            });
        }

        title = (TextView) v.findViewById(R.id.openstreetbug_title);
        comments = (TextView) v.findViewById(R.id.openstreetbug_comments);
        comment = (EditText) v.findViewById(R.id.openstreetbug_comment);
        commentLabel = (TextView) v.findViewById(R.id.openstreetbug_comment_label);
        elementLayout = (LinearLayout) v.findViewById(R.id.openstreetbug_element_layout);
        state = (Spinner) v.findViewById(R.id.openstreetbug_state);

        ArrayAdapter<CharSequence> adapter = setupView(v, task);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        state.setAdapter(adapter);

        int stateOrdinal = task.getState().ordinal();
        if (adapter.getCount() > stateOrdinal) {
            state.setSelection(stateOrdinal);
        } else {
            Log.e(DEBUG_TAG, "ArrayAdapter too short state " + stateOrdinal + " adapter " + adapter.getCount());
        }

        enableStateSpinner(task);

        AppCompatDialog d = builder.create();
        d.setOnShowListener( // old API, buttons are enabled by default
                dialog -> { //
                    final Button save = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                    if ((App.getTaskStorage().contains(task)) && (!task.hasBeenChanged() || task.isNew())) {
                        save.setEnabled(false);
                    }
                    final Button upload = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEUTRAL);
                    if (!task.hasBeenChanged()) {
                        upload.setEnabled(false);
                    }
                    state.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                            save.setEnabled(true);
                            upload.setEnabled(true);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> arg0) {
                            // required, but not used
                        }
                    });
                    onShowListener(save, upload);
                });
        // this should keep the buttons visible
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return d;
    }

    protected abstract <T extends Task> void update(@NonNull Server server, @NonNull PostAsyncActionHandler handler,
            @NonNull T task);

    protected abstract <T extends Task> ArrayAdapter<CharSequence> setupView(@NonNull View v, @NonNull T task);

    protected abstract <T extends Task> void enableStateSpinner(@NonNull T task);

    protected void onShowListener(@NonNull Button save, @NonNull Button upload) {
        // empty
    }

    /**
     * Show some additional text in a dialog
     * 
     * @param context
     *            an Android context
     * @param text
     *            the text to display
     */
    protected void showAdditionalText(@NonNull Context context, @NonNull Spanned text) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.task_help, null);
        TextView message = layout.findViewById(R.id.message);
        message.setMovementMethod(LinkMovementMethod.getInstance());
        message.setText(text);
        Builder b = new AlertDialog.Builder(context);
        b.setView(layout);
        b.setPositiveButton(R.string.dismiss, null);
        b.show();
    }

    /**
     * Invalidate the menu and map if we are called from Main
     * 
     * @param activity
     *            the calling FragmentActivity
     */
    private void updateMenu(@Nullable final FragmentActivity activity) {
        if (activity != null) {
            if (activity instanceof AppCompatActivity) {
                ((AppCompatActivity) activity).invalidateOptionsMenu();
            }
            if (activity instanceof Main) {
                ((Main) activity).invalidateMap();
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(DEBUG_TAG, "onAttach");
        try {
            mListener = (UpdateViewListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement UpdateViewListener");
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (getActivity() instanceof Main) {
            de.blau.android.layer.tasks.MapOverlay layer = ((Main) getActivity()).getMap().getTaskLayer();
            if (layer != null) {
                Task selectedTask = layer.getSelected();
                // ugly way of only de-selecting if we're not in the new note action mode
                if (selectedTask != null && selectedTask.equals(task)
                        && !(task instanceof Note && ((Note) task).isNew())) {
                    layer.deselectObjects();
                }
            }
        }
        if (mListener != null) {
            mListener.update();
        }
    }

    /**
     * ¨ Get the State value corresponding to ordinal
     * 
     * @param ordinal
     *            the ordinal value
     * @return the State value corresponding to ordinal
     */
    @NonNull
    static State pos2state(int ordinal) {
        State[] values = State.values();
        if (ordinal >= 0 && ordinal < values.length) {
            return values[ordinal];
        }
        Log.e(DEBUG_TAG, "pos2state out of range " + ordinal);
        return values[0];
    }

    /**
     * Saves bug to storage if it is new, otherwise update comment and/or state
     * 
     * @param v
     *            the view containing the EditText with the text of the note
     * @param bug
     *            the Task object
     */
    protected void saveTask(@NonNull View v, @NonNull Task bug) {
        if (bug.isNew() && ((Note) bug).count() == 0) {
            App.getTaskStorage().add(bug); // sets dirty
        }
        String c = ((EditText) v.findViewById(R.id.openstreetbug_comment)).getText().toString();
        if (c.length() > 0) {
            ((Note) bug).addComment(c);
        }
        final Spinner state = (Spinner) v.findViewById(R.id.openstreetbug_state);
        bug.setState(pos2state(state.getSelectedItemPosition()));
        bug.setChanged(true);
        App.getTaskStorage().setDirty();
    }

    /**
     * Cancel a Notification for the specified task
     * 
     * @param bug
     *            the task we want to cancel the Notification for
     */
    private void cancelAlert(@NonNull final Task bug) {
        if (bug.hasBeenChanged() && bug.isClosed()) {
            IssueAlert.cancel(getActivity(), bug);
        }
    }
}
