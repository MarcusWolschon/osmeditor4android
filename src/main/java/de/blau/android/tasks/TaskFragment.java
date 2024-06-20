package de.blau.android.tasks;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import de.blau.android.AsyncResult;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.listener.UpdateViewListener;
import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import de.blau.android.tasks.Task.State;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.IssueAlert;
import de.blau.android.util.Util;

/**
 * Very simple dialog fragment to display bug or notes etc
 * 
 * This started off simple, but is now far too complex and should be split up in to separate classes
 * 
 * @author Simon
 *
 */
public abstract class TaskFragment extends ImmersiveDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, TaskFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = TaskFragment.class.getSimpleName().substring(0, TAG_LEN);

    protected static final String BUG_KEY = "bug";

    private UpdateViewListener mListener;

    private Task task = null;

    protected LayoutInflater inflater;
    protected TextView       title;
    protected TextView       comments;
    protected EditText       comment;
    protected TextView       commentLabel;
    protected LinearLayout   elementLayout;
    protected Spinner        state;
    protected Preferences    prefs;

    @SuppressLint({ "NewApi", "InflateParams" })
    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "restoring from saved state");
            task = Util.getSerializeable(savedInstanceState, BUG_KEY, Task.class);
        } else {
            task = Util.getSerializeable(getArguments(), BUG_KEY, Task.class);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        inflater = getActivity().getLayoutInflater();

        prefs = App.getPreferences(getActivity());

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
                update(prefs.getServer(), new PostAsyncActionHandler() {
                    @Override
                    public void onSuccess() {
                        updateMenu(activity);
                    }

                    @Override
                    public void onError(@Nullable AsyncResult result) {
                        updateMenu(activity);
                    }
                }, task);
                cancelAlert(task);
            });
        }

        title = (TextView) v.findViewById(R.id.openstreetbug_title);
        comments = (TextView) v.findViewById(R.id.openstreetbug_comments);
        comment = (EditText) v.findViewById(R.id.openstreetbug_comment);
        commentLabel = (TextView) v.findViewById(R.id.openstreetbug_comment_label);
        elementLayout = (LinearLayout) v.findViewById(R.id.openstreetbug_element_layout);
        state = (Spinner) v.findViewById(R.id.openstreetbug_state);

        ArrayAdapter<CharSequence> adapter = setupView(savedInstanceState, v, task);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        state.setAdapter(adapter);

        int statePos = state2pos(task.getState());
        if (adapter.getCount() > statePos) {
            state.setSelection(statePos);
        } else {
            Log.e(DEBUG_TAG, "ArrayAdapter too short state " + statePos + " adapter " + adapter.getCount());
        }

        enableStateSpinner(task);

        AppCompatDialog d = builder.create();
        d.setOnShowListener( // old API, buttons are enabled by default
                dialog -> { //
                    final Button save = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                    final Button upload = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEUTRAL);
                    final Button cancel = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                    onShowListener(task, save, upload, cancel, state);
                });
        // this should keep the buttons visible
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return d;
    }

    /**
     * Check if we have changed the Task
     * 
     * @param newState the new state
     * @return true if we've changed something wrt the Task
     */
    protected boolean changed(int newState) {
        return newState != state2pos(task.getState());
    }

    /**
     * Update the task on its destination server
     * 
     * Note the process called by this must be async
     * 
     * @param <T> the Task type
     * @param server a Server instance
     * @param handler a PostAsyncActionHandler to call after the update
     * @param task the Task
     */
    protected abstract <T extends Task> void update(@NonNull Server server, @NonNull PostAsyncActionHandler handler, @NonNull T task);

    /**
     * Setup up any Task specific bits of the UI
     * 
     * @param <T> the Task type
     * @param savedInstanceState saved state or null
     * @param v the View
     * @param task the Task
     * @return an ArrayAdapter with the possible Task states as a String
     */
    @NonNull
    protected abstract <T extends Task> ArrayAdapter<CharSequence> setupView(@Nullable Bundle savedInstanceState, @NonNull View v, @NonNull T task);

    /**
     * Enable/disable the state Spinner depending on the Task
     * 
     * @param <T> the Task type
     * @param task the Task
     */
    protected abstract <T extends Task> void enableStateSpinner(@NonNull T task);

    /**
     * Do any Task specific things on showing of the dialog
     * 
     * @param task the current Task
     * @param save the save Button
     * @param upload the upload Button
     * @param cancel the cancel Button
     * @param state the state spinner
     */
    protected void onShowListener(@NonNull Task task, @NonNull Button save, @NonNull Button upload, @NonNull Button cancel, @NonNull Spinner state) {
        if ((App.getTaskStorage().contains(task)) && (!task.hasBeenChanged() || task.isNew())) {
            save.setEnabled(false);
        }
        if (!task.hasBeenChanged()) {
            upload.setEnabled(false);
        }
        state.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean changed = changed(position);
                save.setEnabled(changed);
                upload.setEnabled(changed);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // required, but not used
            }
        });
    }

    /**
     * Do any Task specific things on save
     * 
     * @param <T> the Task type
     * @param task the Task
     */
    protected <T extends Task> void saveTaskSpecific(T task) {
        // empty
    }

    /**
     * Show some additional text in a dialog
     * 
     * @param context an Android context
     * @param text the text to display
     */
    protected void showAdditionalText(@NonNull Context context, @NonNull Spanned text) {
        showAdditionalText(context, text, null);
    }

    /**
     * Show some additional text in a dialog
     * 
     * @param context an Android context
     * @param text the text to display
     * @param additionalViews Views to add to the layout
     */
    protected void showAdditionalText(@NonNull Context context, @NonNull Spanned text, @Nullable List<View> additionalViews) {
        ScrollView scrollView = (ScrollView) inflater.inflate(R.layout.task_help, null);
        LinearLayout layout = scrollView.findViewById(R.id.layout);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1.0f);
        TextView message = scrollView.findViewById(R.id.message);
        message.setMovementMethod(LinkMovementMethod.getInstance());
        message.setText(text);
        message.setTextIsSelectable(true);
        if (additionalViews != null) {
            for (View v : additionalViews) {
                // as we might be showing the same views more than once
                // we need to remove them from their previous parent
                ViewParent parent = v.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeAllViews();
                }
                if (v instanceof TextView) {
                    ((TextView) v).setTextIsSelectable(true);
                }
                v.setLayoutParams(lp);
                layout.addView(v);
                final View ruler = inflater.inflate(R.layout.ruler, null);
                layout.addView(ruler);
            }
        }
        Builder b = new AlertDialog.Builder(context);
        b.setView(scrollView);
        b.setPositiveButton(R.string.dismiss, null);
        b.show();
    }

    /**
     * Invalidate the menu and map if we are called from Main
     * 
     * @param activity the calling FragmentActivity
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
                if (selectedTask != null && selectedTask.equals(task) && !task.isNew()) {
                    layer.deselectObjects();
                }
            }
        }
        if (mListener != null) {
            mListener.update();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(BUG_KEY, task);
    }

    /**
     * ¨ Get the State value corresponding to position
     * 
     * @param position the ordinal value in the enum
     * @return the State value corresponding to position
     */
    @NonNull
    protected abstract State pos2state(int position);

    /**
     * Get the position for the state
     * 
     * @param state the State
     * @return the position
     */
    protected abstract int state2pos(@NonNull State state);

    /**
     * Saves bug to storage if it is new, otherwise update comment and/or state
     * 
     * @param v the view containing the EditText with the text of the note
     * @param bug the Task object
     */
    protected void saveTask(@NonNull View v, @NonNull Task bug) {
        if (bug.isNew() && ((Note) bug).count() == 0) { // isNew can currently only be true for Notes
            App.getTaskStorage().add(bug); // sets dirty
        }
        bug.setState(pos2state(state.getSelectedItemPosition()));
        bug.setChanged(true);
        saveTaskSpecific(bug);
        App.getTaskStorage().setDirty();
    }

    /**
     * Cancel a Notification for the specified task
     * 
     * @param bug the task we want to cancel the Notification for
     */
    private void cancelAlert(@NonNull final Task bug) {
        if (bug.hasBeenChanged() && bug.isClosed()) {
            IssueAlert.cancel(getActivity(), bug);
        }
    }

    /**
     * Get the task, note should never actually be null
     * 
     * @return the task
     */
    @Nullable
    protected Task getTask() {
        return task;
    }
}
