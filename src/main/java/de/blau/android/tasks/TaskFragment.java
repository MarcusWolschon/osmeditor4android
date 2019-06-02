package de.blau.android.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.listener.UpdateViewListener;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.tasks.Task.State;
import de.blau.android.util.GeoMath;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.IssueAlert;
import de.blau.android.util.Util;

/**
 * Very simple dialog fragment to display bug or notes
 * 
 * @author simon
 *
 */
public class TaskFragment extends ImmersiveDialogFragment {
    private static final String DEBUG_TAG = TaskFragment.class.getSimpleName();

    private static final String TAG = "fragment_bug";

    private static final String BUG_KEY = "bug";

    private UpdateViewListener mListener;

    private Task task = null;

    /**
     * Display a dialog for editing Taskss
     * 
     * @param activity the calling FragmentActivity
     * @param t Task we want to edit
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull Task t) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            TaskFragment taskFragment = newInstance(t);
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
    private static TaskFragment newInstance(@NonNull Task t) {
        TaskFragment f = new TaskFragment();

        Bundle args = new Bundle();
        args.putSerializable(BUG_KEY, t);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

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
        builder.setView(v).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                saveTask(v, task);
                cancelAlert(task);
                updateMenu(getActivity());
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        if (task.canBeUploaded()) {
            builder.setNeutralButton(R.string.transfer_download_current_upload, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
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
                            if (task instanceof Note) {
                                Note n = (Note) task;
                                NoteComment nc = n.getLastComment();
                                TransferTasks.uploadNote(activity, prefs.getServer(), n, (nc != null && nc.isNew()) ? nc.getText() : null,
                                        n.getState() == State.CLOSED, false, handler);
                            } else if (task instanceof OsmoseBug) {
                                TransferTasks.updateOsmoseBug(activity, (OsmoseBug) task, false, handler);
                            } else if (task instanceof MapRouletteTask) {
                                TransferTasks.updateMapRouletteTask(activity, prefs.getServer(), (MapRouletteTask) task, false, handler);
                            }
                            return null;
                        }
                    }).execute();
                    cancelAlert(task);
                }
            });
        }

        final Spinner state = (Spinner) v.findViewById(R.id.openstreetbug_state);
        ArrayAdapter<CharSequence> adapter = null;

        TextView title = (TextView) v.findViewById(R.id.openstreetbug_title);
        TextView comments = (TextView) v.findViewById(R.id.openstreetbug_comments);
        EditText comment = (EditText) v.findViewById(R.id.openstreetbug_comment);
        TextView commentLabel = (TextView) v.findViewById(R.id.openstreetbug_comment_label);
        LinearLayout elementLayout = (LinearLayout) v.findViewById(R.id.openstreetbug_element_layout);
        if (task instanceof Note) {
            title.setText(getString((task.isNew() && ((Note) task).count() == 0) ? R.string.openstreetbug_new_title : R.string.openstreetbug_edit_title));
            comments.setText(Util.fromHtml(((Note) task).getComment())); // ugly
            comments.setAutoLinkMask(Linkify.WEB_URLS);
            comments.setMovementMethod(LinkMovementMethod.getInstance());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                comments.setTextIsSelectable(true);
            }
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
            adapter = ArrayAdapter.createFromResource(getActivity(), R.array.note_state, android.R.layout.simple_spinner_item);
        } else {
            // these are only used for Notes
            commentLabel.setVisibility(View.GONE);
            comment.setVisibility(View.GONE);
            //
            if (task instanceof OsmoseBug || task instanceof CustomBug) {
                title.setText(R.string.openstreetbug_bug_title);
                comments.setText(Util.fromHtml(((Bug) task).getLongDescription(getActivity(), false)));
                final StorageDelegator storageDelegator = App.getDelegator();
                for (final OsmElement e : ((Bug) task).getElements()) {
                    String text;
                    if (e.getOsmVersion() < 0) { // fake element
                        text = getString(R.string.bug_element_1, e.getName(), e.getOsmId());
                    } else { // real
                        text = getString(R.string.bug_element_2, e.getName(), e.getDescription(false));
                    }
                    TextView tv = new TextView(getActivity());
                    tv.setClickable(true);
                    tv.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) { // FIXME assumption that we are being called from Main
                            dismiss();
                            final FragmentActivity activity = getActivity();
                            final int lonE7 = task.getLon();
                            final int latE7 = task.getLat();
                            if (e.getOsmVersion() < 0) { // fake element
                                try {
                                    BoundingBox b = GeoMath.createBoundingBoxForCoordinates(latE7 / 1E7D, lonE7 / 1E7, 50, true);
                                    App.getLogic().downloadBox(activity, b, true, new PostAsyncActionHandler() {
                                        @Override
                                        public void onSuccess() {
                                            OsmElement osm = storageDelegator.getOsmElement(e.getName(), e.getOsmId());
                                            if (osm != null && activity != null && activity instanceof Main) {
                                                ((Main) activity).zoomToAndEdit(lonE7, latE7, osm);
                                            }
                                        }

                                        @Override
                                        public void onError() {
                                         // Ignore
                                        }
                                    });
                                } catch (OsmException e1) {
                                    Log.e(DEBUG_TAG, "onCreateDialog got " + e1.getMessage());
                                }
                            } else if (activity instanceof Main) { // real
                                ((Main) activity).zoomToAndEdit(lonE7, latE7, e);
                            }
                        }
                    });
                    tv.setTextColor(ContextCompat.getColor(getActivity(), R.color.holo_blue_light));
                    tv.setText(text);
                    elementLayout.addView(tv);
                }
                //
                adapter = ArrayAdapter.createFromResource(getActivity(), R.array.bug_state, android.R.layout.simple_spinner_item);
            } else if (task instanceof MapRouletteTask) {
                title.setText(R.string.maproulette_task_title);
                comments.setText(Util.fromHtml(((MapRouletteTask) task).getDescription()));
                adapter = ArrayAdapter.createFromResource(getActivity(), R.array.maproulette_state, android.R.layout.simple_spinner_item);
                MapRouletteChallenge challenge = App.getTaskStorage().getChallenges().get(((MapRouletteTask) task).getParentId());
                if (challenge != null) {
                    final StringBuilder explanationsBuilder = new StringBuilder();
                    //
                    if (challenge.blurb != null && !"".equals(challenge.blurb)) {
                        explanationsBuilder.append(challenge.blurb);
                    } else if (challenge.description != null && !"".equals(challenge.description)) {
                        explanationsBuilder.append(challenge.description);
                    }
                    //
                    if (challenge.instruction != null && !"".equals(challenge.instruction)) {
                        if (explanationsBuilder.length() > 0) {
                            explanationsBuilder.append("<br><br>");
                        }
                        explanationsBuilder.append(challenge.instruction);
                    }

                    if (explanationsBuilder.length() > 0) {
                        TextView instructionText = new TextView(getActivity());
                        instructionText.setClickable(true);
                        instructionText.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final FragmentActivity activity = getActivity();
                                Builder builder = new AlertDialog.Builder(activity);
                                builder.setMessage(Util.fromHtml(explanationsBuilder.toString()));
                                builder.show();
                            }
                        });
                        instructionText.setTextColor(ContextCompat.getColor(getActivity(), R.color.holo_blue_light));
                        instructionText.setText(R.string.maproulette_task_explanations);
                        elementLayout.addView(instructionText);
                    }
                }
                // add a clickable link to the location
                TextView locationText = new TextView(getActivity());
                locationText.setClickable(true);
                final double lon = task.getLon() / 1E7D;
                final double lat = task.getLat() / 1E7D;
                locationText.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) { // FIXME assumption that we are being called from Main
                        dismiss();
                        final FragmentActivity activity = getActivity();
                        try {
                            final BoundingBox b = GeoMath.createBoundingBoxForCoordinates(lat, lon, 50, true);
                            App.getLogic().downloadBox(activity, b, true, new PostAsyncActionHandler() {
                                @Override
                                public void onSuccess() {
                                    Logic logic = App.getLogic();
                                    logic.getViewBox().fitToBoundingBox(logic.getMap(), b);
                                    logic.getMap().invalidate();
                                }

                                @Override
                                public void onError() {
                                    // unused
                                }
                            });
                        } catch (OsmException e1) {
                            Log.e(DEBUG_TAG, "onCreateDialog got " + e1.getMessage());
                        }
                    }
                });
                locationText.setTextColor(ContextCompat.getColor(getActivity(), R.color.holo_blue_light));
                locationText.setText(getString(R.string.maproulette_task_coords, lon, lat));
                elementLayout.addView(locationText);
            } else {
                Log.d(DEBUG_TAG, "Unknown task type " + task.getDescription());
                builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.openstreetbug_unknown_task_type)
                        .setMessage(getString(R.string.openstreetbug_not_supported, task.getClass().getCanonicalName()))
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                return builder.create();
            }
        }

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

        state.setEnabled(!task.isNew()); // new bugs always open
        AppCompatDialog d = builder.create();
        d.setOnShowListener(new OnShowListener() { // old API, buttons are enabled by default
            @Override
            public void onShow(DialogInterface dialog) { //
                final Button save = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                if ((App.getTaskStorage().contains(task)) && (!task.hasBeenChanged() || task.isNew())) {
                    save.setEnabled(false);
                }
                final Button upload = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
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
                    }
                });
                EditText comment = (EditText) v.findViewById(R.id.openstreetbug_comment);
                comment.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable arg0) {
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        save.setEnabled(true);
                        upload.setEnabled(true);
                        state.setSelection(State.OPEN.ordinal());
                    }
                });
            }
        });
        return d;
    }

    /**
     * Invalidate the menu and map if we are called from Main
     * 
     * @param activity the calling FragmentActivity
     */
    private void updateMenu(@NonNull final FragmentActivity activity) {
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
            throw new ClassCastException(context.toString() + " must implement OnPresetSelectedListener");
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
                if (selectedTask != null && selectedTask.equals(task) && !(task instanceof Note && ((Note) task).isNew())) {
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
     * @param ordinal the ordinal value
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
     * @param v the view containing the EditText with the text of the note
     * @param bug the Task object
     */
    private void saveTask(@NonNull View v, @NonNull Task bug) {
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
     * @param bug the task we want to cancel the Notification for
     */
    private void cancelAlert(@NonNull final Task bug) {
        if (bug.hasBeenChanged() && bug.isClosed()) {
            IssueAlert.cancel(getActivity(), bug);
        }
    }
}
