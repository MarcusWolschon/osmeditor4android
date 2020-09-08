package de.blau.android.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
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
import de.blau.android.tasks.OsmoseMeta.OsmoseClass;
import de.blau.android.tasks.Task.State;
import de.blau.android.util.GeoMath;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.IssueAlert;
import de.blau.android.util.Util;
import io.noties.markwon.Markwon;

/**
 * Very simple dialog fragment to display bug or notes etc
 * 
 * This started off simple, but is now far too complex and should be split up in to separate classes
 * 
 * @author Simon
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
        builder.setView(v).setPositiveButton(R.string.save, (dialog, id) -> {
            saveTask(v, task);
            cancelAlert(task);
            updateMenu(getActivity());
        }).setNegativeButton(R.string.cancel, (dialog, id) -> {
            // unused
        });

        final boolean isOsmoseBug = task instanceof OsmoseBug;
        final boolean isCustomBug = task instanceof CustomBug;
        final boolean isMapRouletteTask = task instanceof MapRouletteTask;
        final boolean isNote = task instanceof Note;
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
                        if (isNote) {
                            Note n = (Note) task;
                            NoteComment nc = n.getLastComment();
                            TransferTasks.uploadNote(activity, prefs.getServer(), n, (nc != null && nc.isNew()) ? nc.getText() : null,
                                    n.getState() == State.CLOSED, false, handler);
                        } else if (isOsmoseBug) {
                            TransferTasks.updateOsmoseBug(activity, (OsmoseBug) task, false, handler);
                        } else if (isMapRouletteTask) {
                            TransferTasks.updateMapRouletteTask(activity, prefs.getServer(), (MapRouletteTask) task, false, handler);
                        }
                        return null;
                    }
                }).execute();
                cancelAlert(task);
            });
        }

        final Spinner state = (Spinner) v.findViewById(R.id.openstreetbug_state);
        ArrayAdapter<CharSequence> adapter = null;

        TextView title = (TextView) v.findViewById(R.id.openstreetbug_title);
        TextView comments = (TextView) v.findViewById(R.id.openstreetbug_comments);
        EditText comment = (EditText) v.findViewById(R.id.openstreetbug_comment);
        TextView commentLabel = (TextView) v.findViewById(R.id.openstreetbug_comment_label);
        LinearLayout elementLayout = (LinearLayout) v.findViewById(R.id.openstreetbug_element_layout);
        if (isNote) {
            title.setText(getString((task.isNew() && ((Note) task).count() == 0) ? R.string.openstreetbug_new_title : R.string.openstreetbug_edit_title));
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
            adapter = ArrayAdapter.createFromResource(getActivity(), R.array.note_state, android.R.layout.simple_spinner_item);
        } else {
            // these are only used for Notes
            commentLabel.setVisibility(View.GONE);
            comment.setVisibility(View.GONE);
            //
            if (isOsmoseBug || isCustomBug) {
                title.setText(R.string.openstreetbug_bug_title);
                comments.setText(Util.fromHtml(((Bug) task).getLongDescription(getActivity(), false)));
                if (!isCustomBug && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    // provide dialog with some additional text
                    TextView instructionText = new TextView(getActivity());
                    instructionText.setClickable(true);
                    instructionText.setOnClickListener(unused -> {
                        final FragmentActivity activity = getActivity();
                        final Markwon markwon = Markwon.create(activity);
                        OsmoseMeta meta = App.getTaskStorage().getOsmoseMeta();
                        final int itemId = ((OsmoseBug) task).getOsmoseItem();
                        final int classId = ((OsmoseBug) task).getOsmoseClass();
                        OsmoseClass osmoseClass = meta.getOsmoseClass(itemId, classId);
                        if (osmoseClass == null) {
                            new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected Void doInBackground(Void... arg0) {
                                    OsmoseServer.getMeta(getContext(), itemId, classId);
                                    return null;
                                }

                                @Override
                                protected void onPostExecute(Void arg0) {
                                    OsmoseClass osmoseClass = meta.getOsmoseClass(itemId, classId);
                                    if (osmoseClass != null) {
                                        String text = osmoseClass.getText();
                                        if (text != null) {
                                            showAdditionalText(activity, markwon.toMarkdown(text));
                                        }
                                    }
                                }

                            }.execute();
                        } else {
                            String text = osmoseClass.getText();
                            if (text != null) {
                                showAdditionalText(activity, markwon.toMarkdown(text));
                            }
                        }
                    });
                    instructionText.setTextColor(ContextCompat.getColor(getActivity(), R.color.holo_blue_light));
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
                            final FragmentActivity activity = getActivity();
                            final int lonE7 = task.getLon();
                            final int latE7 = task.getLat();
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
                        });
                        tv.setTextColor(ContextCompat.getColor(getActivity(), R.color.holo_blue_light));
                    }
                    tv.setText(text);
                    elementLayout.addView(tv);
                }
                //
                adapter = ArrayAdapter.createFromResource(getActivity(), R.array.bug_state, android.R.layout.simple_spinner_item);
            } else if (isMapRouletteTask) {
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
                        instructionText.setOnClickListener(unused -> showAdditionalText(getActivity(), Util.fromHtml(explanationsBuilder.toString())));
                        instructionText.setTextColor(ContextCompat.getColor(getActivity(), R.color.holo_blue_light));
                        instructionText.setText(R.string.maproulette_task_explanations);
                        elementLayout.addView(instructionText);
                    }
                }
                // add a clickable link to the location
                TextView locationText = new TextView(getActivity());
                final double lon = task.getLon() / 1E7D;
                final double lat = task.getLat() / 1E7D;
                if (getActivity() instanceof Main) {
                    locationText.setClickable(true);
                    locationText.setOnClickListener(unused -> {
                        dismiss();
                        try {
                            final BoundingBox b = GeoMath.createBoundingBoxForCoordinates(lat, lon, 50, true);
                            App.getLogic().downloadBox(getActivity(), b, true, () -> {
                                Logic logic = App.getLogic();
                                logic.getViewBox().fitToBoundingBox(logic.getMap(), b);
                                logic.getMap().invalidate();
                            });
                        } catch (OsmException e1) {
                            Log.e(DEBUG_TAG, "onCreateDialog got " + e1.getMessage());
                        }
                    });
                }
                locationText.setTextColor(ContextCompat.getColor(getActivity(), R.color.holo_blue_light));
                locationText.setText(getString(R.string.maproulette_task_coords, lon, lat));
                elementLayout.addView(locationText);
            } else {
                Log.d(DEBUG_TAG, "Unknown task type " + task.getDescription());
                builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.openstreetbug_unknown_task_type)
                        .setMessage(getString(R.string.openstreetbug_not_supported, task.getClass().getCanonicalName()))
                        .setNegativeButton(R.string.cancel, (dialog, id) -> {
                            // not used
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

        boolean uploadedOsmoseBug = isOsmoseBug && task.isClosed() && !task.hasBeenChanged();
        state.setEnabled(!task.isNew() && !uploadedOsmoseBug); // new bugs always open and OSMOSE bugs can't be reopened
                                                               // once uploaded
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
                    EditText commentText = (EditText) v.findViewById(R.id.openstreetbug_comment);
                    commentText.addTextChangedListener(new TextWatcher() {
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
                });
        return d;
    }

    /**
     * Show some additional text in a dialog
     * 
     * @param context an Android context
     * @param text the text to display
     */
    private void showAdditionalText(@NonNull Context context, @NonNull Spanned text) {
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
