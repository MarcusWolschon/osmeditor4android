package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.AsyncResult;
import de.blau.android.ErrorCodes;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.osm.ApiResponse;
import de.blau.android.osm.ApiResponse.Conflict;
import de.blau.android.osm.MergeAction;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Result;
import de.blau.android.osm.Storage;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.InfoDialogFragment;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Dialog to resolve upload conflicts one by one
 * 
 * @author simon
 *
 */
public class UploadConflict extends ImmersiveDialogFragment {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, UploadConflict.class.getSimpleName().length());
    private static final String DEBUG_TAG = UploadConflict.class.getSimpleName().substring(0, TAG_LEN);

    private static final String CONFLICT_KEY = "uploadresult";
    private static final String ELEMENTS_KEY = "elements";

    private static final String TAG = "fragment_upload_conflict";

    private Conflict         conflict;
    private List<OsmElement> elements;

    private class RestartHandler implements PostAsyncActionHandler {
        private final String           errorMessage;
        private final FragmentActivity activity;

        /**
         * Construct a new handler
         * 
         * @param activity current Activity
         * @param errorMessage error message to display
         */
        RestartHandler(@NonNull FragmentActivity activity, @NonNull String errorMessage) {
            this.activity = activity;
            this.errorMessage = errorMessage;
        }

        @Override
        public void onSuccess() {
            if (activity instanceof Main) {
                ((Main) activity).invalidateMap();
            }
            if (App.getDelegator().hasChanges()) {
                ReviewAndUpload.showDialog(activity, elements);
            }
        }

        @Override
        public void onError(@Nullable AsyncResult result) {
            ScreenMessage.toastTopError(activity, errorMessage);
        }
    }

    /**
     * Show a dialog after a conflict has been detected and allow the user to fix it
     * 
     * @param activity the calling Activity
     * @param elements optional list of elements in upload
     * @param result the UploadResult
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull Conflict conflict, @Nullable List<OsmElement> elements) {
        dismissDialog(activity);

        FragmentManager fm = activity.getSupportFragmentManager();
        try {
            UploadConflict uploadConflictDialogFragment = newInstance(conflict, elements);
            uploadConflictDialogFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "dismissDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Construct a new UploadConflict dialog
     * 
     * @param conflict an COnflict object with the relevant info
     * @param elements optional list of elements in upload
     * 
     * @return an UploadConflict dialog
     */
    @NonNull
    private static UploadConflict newInstance(@NonNull final Conflict conflict, List<OsmElement> elements) {
        UploadConflict f = new UploadConflict();

        Bundle args = new Bundle();
        args.putSerializable(CONFLICT_KEY, conflict);
        if (elements != null) {
            args.putSerializable(ELEMENTS_KEY, new ArrayList<>(elements));
        }

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "restoring from saved state");
            conflict = de.blau.android.util.Util.getSerializeable(savedInstanceState, CONFLICT_KEY, Conflict.class);
            elements = Util.getSerializeableArrayList(savedInstanceState, ELEMENTS_KEY, OsmElement.class);
        } else {
            conflict = de.blau.android.util.Util.getSerializeable(getArguments(), CONFLICT_KEY, Conflict.class);
            elements = Util.getSerializeableArrayList(getArguments(), ELEMENTS_KEY, OsmElement.class);
        }
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(), R.attr.alert_dialog));
        builder.setTitle(R.string.upload_conflict_title);
        builder.setNegativeButton(R.string.cancel, null); // set early in case of exceptions
        final FragmentActivity activity = getActivity();
        Resources res = activity.getResources();
        final Logic logic = App.getLogic();
        Map<String, Runnable> resolveActions = new LinkedHashMap<>();

        try {
            final StorageDelegator delegator = App.getDelegator();
            final String conflictElementType = conflict.getElementType();
            final long conflictElementId = conflict.getElementId();
            final OsmElement elementLocal = delegator.getOsmElement(conflictElementType, conflictElementId);
            final OsmElement elementOnServer = elementLocal.getState() == OsmElement.STATE_CREATED ? null
                    : getServerElement(logic, conflictElementType, conflictElementId);

            LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);
            final LayoutParams tp = InfoDialogFragment.getTableLayoutParams();

            if (conflict instanceof ApiResponse.StillUsedConflict) {
                //
                // server element should always be available. local is deleted
                //

                // we are deleting an element that is still in use on the server
                // get the elements that are still using it
                final String usedByElementType = ((ApiResponse.StillUsedConflict) conflict).getUsedByElementType();
                final long[] usedByElementIds = ((ApiResponse.StillUsedConflict) conflict).getUsedByElementIds();
                final Storage usedByOnServer = logic.getElementsWithDeleted(activity, usedByElementType, usedByElementIds);

                builder.setTitle(R.string.upload_conflict_message_referential);
                TableLayout tl = (TableLayout) inflater.inflate(R.layout.missing_element_view, null);
                ScrollView sv = (ScrollView) inflater.inflate(R.layout.element_info_view, null, false);
                if (elementOnServer == null) {
                    throw new IllegalStateException("elementOnSerer should not be null here");
                }
                sv = ElementInfo.createComparisionView(activity, sv, tp, null, null, res.getString(R.string.server_side_object), elementOnServer);

                tl.setColumnStretchable(1, true);
                tl.setColumnStretchable(2, true);
                tl.addView(TableLayoutUtils.createFullRowTitle(activity, res.getString(R.string.still_in_use_by_elements), tp));

                for (long id : usedByElementIds) {
                    OsmElement e = usedByOnServer.getOsmElement(usedByElementType, id);
                    tl.addView(TableLayoutUtils.createFullRow(activity,
                            e != null ? e.getDescription(activity, true) : res.getString(R.string.unable_to_download_referring_element, usedByElementType, id),
                            tp));
                }
                LinearLayout infoLayout = sv.findViewById(R.id.element_info_layout);
                infoLayout.addView(tl);
                builder.setView(sv);
                resolveActions.put(res.getString(R.string.undoing_local_delete), () -> {
                    logic.createCheckpoint(activity, R.string.undo_action_fix_conflict);
                    delegator.undoLast(elementLocal);
                    if (delegator.getApiElementCount() > 0) {
                        ReviewAndUpload.showDialog(activity, elements);
                    }
                });
                resolveActions.put(res.getString(R.string.deleting_references_on_server), () -> {
                    logic.createCheckpoint(activity, R.string.undo_action_fix_conflict);
                    // first undelete
                    delegator.removeFromUpload(elementLocal, OsmElement.STATE_UNCHANGED);
                    if (elements != null) {
                        elements.remove(elementLocal);
                    }
                    delegator.insertElementSafe(elementLocal);
                    // now download referring elements
                    for (long id : usedByElementIds) {
                        if (logic.downloadElement(activity, usedByElementType, id, false, true, null) != ErrorCodes.OK) {
                            throw new IllegalStateException(res.getString(R.string.unable_to_download_referring_element_for_deletion, usedByElementType, id));
                        }
                        delegator.removeOnUndo(delegator.getOsmElement(usedByElementType, id));
                    }
                    // local element will likely be new instance, so get it again
                    OsmElement newElementLocal = delegator.getOsmElement(conflictElementType, conflictElementId);
                    switch (elementLocal.getName()) {
                    case Node.NAME:
                        delegator.removeNode((Node) newElementLocal);
                        break;
                    case Way.NAME:
                        delegator.removeWay((Way) newElementLocal);
                        break;
                    case Relation.NAME:
                        delegator.removeRelation((Relation) newElementLocal);
                        break;
                    default:
                        throw new IllegalStateException("Unknown element type");
                    }
                    ReviewAndUpload.showDialog(activity, elements);
                });
            } else if (conflict instanceof ApiResponse.VersionConflict) {
                //
                // server element should always be available
                //
                builder.setTitle(R.string.upload_conflict_message_version);
                if (elementOnServer == null) {
                    throw new IllegalStateException("elementOnServer should not be null here");
                }
                final RestartHandler restartHandler = new RestartHandler(activity,
                        activity.getString(R.string.toast_download_server_version_failed, elementLocal.getDescription()));
                resolveActions.put(res.getString(R.string.use_local_version), () -> {
                    logic.fixElementWithConflict(activity, elementOnServer.getOsmVersion(), elementLocal, elementOnServer, true);
                    ReviewAndUpload.showDialog(activity, elements);
                });
                resolveActions.put(res.getString(R.string.merge_tags_in_to_server), () -> {
                    Map<String, String> mergedTags = MergeAction.mergeTags(elementOnServer, elementLocal);
                    logic.replaceElement(activity, elementLocal, new PostAsyncActionHandler() {
                        @Override
                        public void onSuccess() {
                            OsmElement updatedElement = delegator.getOsmElement(conflict.getElementType(), conflict.getElementId());
                            if (updatedElement == null) {
                                throw new IllegalStateException(
                                        res.getString(R.string.unable_to_download_server_version, conflictElementType, conflictElementId));
                            }
                            setMergedTags(activity, logic, updatedElement, elementOnServer, elementLocal, mergedTags, restartHandler);
                            // as updatedElement is actually a new instance it will be added twice to the undo
                            // checkpoint therefore remove it
                            delegator.getUndo().remove(updatedElement);
                        }

                        @Override
                        public void onError(@Nullable AsyncResult result) {
                            restartHandler.onError(result);
                        }
                    });
                });
                resolveActions.put(res.getString(R.string.merge_tags_in_to_local), () -> {
                    logic.createCheckpoint(activity, R.string.undo_action_fix_conflict);
                    setMergedTags(activity, logic, elementLocal, elementLocal, elementOnServer, MergeAction.mergeTags(elementLocal, elementOnServer),
                            restartHandler);
                });
                resolveActions.put(res.getString(R.string.use_server_version), () -> resolveConflict(activity, logic, elementOnServer, elementLocal));

                builder.setView(ElementInfo.createComparisionView(activity, (ScrollView) inflater.inflate(R.layout.element_info_view, null, false), tp,
                        res.getString(R.string.local_object), elementLocal, res.getString(R.string.server_side_object), elementOnServer));

            } else if (conflict instanceof ApiResponse.RequiredElementsConflict) {
                //
                // server element could be available
                //
                builder.setTitle(R.string.upload_conflict_message_missing_references);
                final String requiredElementType = ((ApiResponse.RequiredElementsConflict) conflict).getRequriedElementType();
                final long[] requiredElementsIds = ((ApiResponse.RequiredElementsConflict) conflict).getRequiredElementsIds();
                final Storage requiredElements = logic.getElementsWithDeleted(activity, requiredElementType, requiredElementsIds);

                ScrollView sv = (ScrollView) inflater.inflate(R.layout.element_info_view, null, false);
                sv = elementOnServer != null
                        ? ElementInfo.createComparisionView(activity, sv, tp, res.getString(R.string.local_object), elementLocal,
                                res.getString(R.string.server_side_object), elementOnServer)
                        : ElementInfo.createComparisionView(activity, sv, tp, null, null, res.getString(R.string.local_object), elementLocal);

                final LinearLayout infoLayout = sv.findViewById(R.id.element_info_layout);
                final TableLayout tl = (TableLayout) inflater.inflate(R.layout.missing_element_view, null);
                tl.setColumnStretchable(1, true);
                tl.addView(TableLayoutUtils.createFullRowTitle(activity, res.getString(R.string.missing_referenced_elements), tp));
                tl.addView(TableLayoutUtils.createHeaderRow(activity, res.getString(R.string.delete_locally), res.getString(R.string.undelete_on_server), tp,
                        true));
                for (long id : requiredElementsIds) {
                    OsmElement e = requiredElements.getOsmElement(requiredElementType, id);
                    tl.addView(createMissingReferenceRow(activity, e, tp));
                }

                infoLayout.addView(tl);
                builder.setView(sv);
                final RestartHandler restartHandler = new RestartHandler(activity, "");
                resolveActions.put(res.getString(R.string.resolve), () -> {
                    logic.createCheckpoint(activity, R.string.undo_action_fix_conflict);
                    for (int i = 0; i < tl.getChildCount(); i++) {
                        View tr = tl.getChildAt(i);
                        if (tr instanceof TableRow) {
                            Object rowTag = tr.getTag();
                            if (rowTag instanceof MissingReferenceAction) {
                                long id = ((MissingReferenceAction) rowTag).id;
                                OsmElement local = delegator.getOsmElement(requiredElementType, id);
                                if (local == null) {
                                    throw new IllegalArgumentException("Local element " + requiredElementType + " #" + id + " should not be null");
                                }
                                if (((MissingReferenceAction) rowTag).deleteLocally) {
                                    // NOTE edge case if all way nodes of a way are deleted here the way will vanish
                                    logic.updateToDeleted(activity, local, false);
                                } else {
                                    OsmElement remote = requiredElements.getOsmElement(requiredElementType, id);
                                    logic.fixElementWithConflict(activity, remote.getOsmVersion(), local, remote, false);
                                }
                            }
                        }
                    }
                    restartHandler.onSuccess();
                });
            } else {
                throw new IllegalArgumentException(
                        res.getString(R.string.unexpected_conflict_type, conflict == null ? " is null" : " " + conflict.getClass().getSimpleName()));
            }

            builder.setNeutralButton(R.string.resolve_by, null);

            AlertDialog dialog = builder.create();

            dialog.setOnShowListener((DialogInterface d) -> {
                final Button neutral = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                if (resolveActions.size() == 1) {
                    Entry<String, Runnable> action = resolveActions.entrySet().iterator().next();
                    neutral.setText(action.getKey());
                    neutral.setOnClickListener((View v) -> {
                        action.getValue().run();
                        dialog.dismiss();
                    });
                    return;
                }
                neutral.setOnClickListener((View v) -> {
                    PopupMenu popup = new PopupMenu(getActivity(), neutral);
                    for (Entry<String, Runnable> action : resolveActions.entrySet()) {
                        MenuItem item = popup.getMenu().add(action.getKey());
                        item.setOnMenuItemClickListener(unused -> {
                            action.getValue().run();
                            dialog.dismiss();
                            return false;
                        });
                    }
                    popup.show();
                });

            });
            return dialog;
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Caught exception " + e);
            builder.setMessage(e.getLocalizedMessage());
            ACRAHelper.nocrashReport(e, e.getMessage());
        }

        return builder.create();
    }

    /**
     * Set and check the merged tags, potentially showing a dialog that will allow editing the target
     * 
     * @param activity calling Activity
     * @param logic current Logic instance
     * @param target target OsmElement
     * @param into OsmElement the tags where merged into
     * @param from OsmElement the tags where merged from
     * @param mergedTags the merged tags
     * @param restartHandler handler that restart the upload
     */
    private void setMergedTags(@NonNull final FragmentActivity activity, @NonNull final Logic logic, @NonNull OsmElement target, @NonNull final OsmElement into,
            @NonNull final OsmElement from, @NonNull Map<String, String> mergedTags, @NonNull RestartHandler restartHandler) {
        Result mergeResult = MergeAction.checkForMergedTags(into.getTags(), from.getTags(), mergedTags);
        mergeResult.setElement(target);
        logic.setTags(activity, target, mergedTags, false);
        if (mergeResult.hasIssue()) {
            ((Main) activity).edit(into);
            // NOTE Arrays.asList doesn't work here
            TagConflictDialog.showDialog(((Main) activity), de.blau.android.util.Util.wrapInList(mergeResult));
        } else {
            restartHandler.onSuccess();
        }
    }

    /**
     * Retrieve a single element from, catches OsmServerException
     * 
     * @param logic current Login instance
     * @param elementType the element type
     * @param osmId the OSM id
     * @return the element or null if it didn't exist
     */
    @Nullable
    private OsmElement getServerElement(@NonNull final Logic logic, @NonNull String elementType, long osmId) {
        try {
            return logic.getElementWithDeleted(getActivity(), elementType, osmId);
        } catch (Exception ex) {
            Log.d(DEBUG_TAG, " getServerElement " + ex.getMessage());
            return null;
        }
    }

    /**
     * Resolve conflict by conforming to the server version
     * 
     * @param activity calling Activity
     * @param logic current Login instance
     * @param elementOnServer remote element
     * @param elementLocal local element
     */
    private void resolveConflict(@NonNull final FragmentActivity activity, @NonNull final Logic logic, @NonNull final OsmElement elementOnServer,
            @NonNull final OsmElement elementLocal) {
        RestartHandler restartHandler = new RestartHandler(activity,
                activity.getString(R.string.toast_download_server_version_failed, elementLocal.getDescription()));
        if (elementOnServer.getState() != OsmElement.STATE_DELETED) {
            logic.replaceElement(activity, elementLocal, restartHandler);
        } else { // delete local element
            logic.updateToDeleted(activity, elementLocal, true);
            restartHandler.onSuccess();
        }
    }

    private static class MissingReferenceAction {
        long    id;
        boolean deleteLocally;
    }

    /**
     * Create a row for a missing reference conflict with two radio buttons
     * 
     * @param context an Android Context
     * @param e the referenced element
     * @param tp row layout params
     * @return a TableRow
     */
    @NonNull
    public static TableRow createMissingReferenceRow(@NonNull Context context, @NonNull OsmElement e, @NonNull TableLayout.LayoutParams tp) {
        TableRow tr = new TableRow(context);
        final MissingReferenceAction tag = new MissingReferenceAction();
        tag.id = e.getOsmId();
        tr.setTag(tag);
        TextView cell = new TextView(context);
        cell.setMinEms(TableLayoutUtils.FIRST_CELL_WIDTH);
        cell.setMaxEms(TableLayoutUtils.MAX_FIRST_CELL_WIDTH);
        cell.setMaxLines(2);
        cell.setText(e.getDescription(context));
        cell.setEllipsize(TruncateAt.MARQUEE);
        cell.setTextIsSelectable(true);
        tr.addView(cell);
        TableRow.LayoutParams trp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        RadioButton localDelete = new RadioButton(context);
        localDelete.setLayoutParams(trp);
        tr.addView(localDelete);
        RadioButton remoteUndelete = new RadioButton(context);
        remoteUndelete.setLayoutParams(trp);
        tr.addView(remoteUndelete);
        localDelete.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                remoteUndelete.setChecked(false);
                tag.deleteLocally = true;
            }
        });
        remoteUndelete.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                localDelete.setChecked(false);
                tag.deleteLocally = false;
            }
        });
        remoteUndelete.setChecked(true);
        tr.setLayoutParams(tp);
        return tr;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(CONFLICT_KEY, conflict);
        if (elements != null) {
            outState.putSerializable(ELEMENTS_KEY, new ArrayList<>(elements));
        }
    }
}
