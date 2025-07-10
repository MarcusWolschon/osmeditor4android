package de.blau.android.dialogs;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.openlocationcode.OpenLocationCode;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.GeoPoint;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElementInterface;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationInterface;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Tags;
import de.blau.android.osm.UndoStorage;
import de.blau.android.osm.UndoStorage.UndoElement;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.osm.WayInterface;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.InfoDialogFragment;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.validation.Validator;

/**
 * Very simple dialog fragment to display some info on an OsmElement and potentially compare it with an UndoElement
 * 
 * @author simon
 *
 */
public class ElementInfo extends InfoDialogFragment {
    private static final int NO_PRIOR_STATE = -1;

    private static final String DEBUG_TAG = ElementInfo.class.getSimpleName().substring(0, Math.min(23, ElementInfo.class.getSimpleName().length()));

    private static final int    DISPLAY_LIMIT         = 10;
    private static final String ELEMENT_KEY           = "element";
    private static final String UNDOELEMENT_INDEX_KEY = "undoelement_index";
    private static final String SHOW_JUMP_TO_KEY      = "showJumpTo";
    private static final String SHOW_EDIT_TAGS_KEY    = "showEditTags";
    private static final String ELEMENT_ID_KEY        = "elementId";
    private static final String ELEMENT_TYPE_KEY      = "elementType";
    private static final String PARENT_TAG_KEY        = "parent_tag";

    private static final String TWO_ROW_DATE_FORMAT = "yyyy-MM-dd'\n'HH:mm:ss zzz";

    private static final String TAG = "fragment_element_info";

    private static final String TEL    = "tel:";
    private static final String MAILTO = "mailto:";

    private static SpannableString emptyRole;

    private OsmElement          element;
    private OsmElementInterface ue;
    private int                 ueIndex = NO_PRIOR_STATE;
    private String              parentTag;

    /**
     * Show an info dialog for the supplied OsmElement
     * 
     * @param activity the calling Activity
     * @param e the OsmElement
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull OsmElement e) {
        showDialog(activity, NO_PRIOR_STATE, e, false, true, null);
    }

    /**
     * Show an info dialog for the supplied OsmElement
     * 
     * @param activity the calling Activity
     * @param e the OsmElement
     * @param showJumpTo display button to jump to object
     * @param showEditTags display button to edit tags
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull OsmElement e, boolean showJumpTo, boolean showEditTags) {
        showDialog(activity, NO_PRIOR_STATE, e, showJumpTo, showEditTags, null);
    }

    /**
     * Show an info dialog for the supplied OsmElement
     * 
     * @param activity the calling Activity
     * @param e the OsmElement
     * @param showJumpTo display button to jump to object
     * @param parentTag tag of any parent dialog
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull OsmElement e, boolean showJumpTo, @Nullable String parentTag) {
        showDialog(activity, NO_PRIOR_STATE, e, showJumpTo, true, parentTag);
    }

    /**
     * Show an info dialog for the supplied OsmElement
     * 
     * @param activity the calling Activity
     * @param ueIndex index of an UndoElement to compare with (0 is the original element)
     * @param e the OsmElement
     * @param showJumpTo display button to jump to object
     */
    public static void showDialog(@NonNull FragmentActivity activity, int ueIndex, @NonNull OsmElement e, boolean showJumpTo) {
        showDialog(activity, ueIndex, e, showJumpTo, true, null);
    }

    /**
     * Show an info dialog for the supplied OsmElement
     * 
     * @param activity the calling Activity
     * @param ueIndex index of an UndoElement to compare with (0 is the original element)
     * @param e the OsmElement
     * @param showJumpTo display button to jump to object
     * @param showEditTags display button to edit tags
     * @param parentTag tag of any parent dialog
     */
    public static void showDialog(@NonNull FragmentActivity activity, int ueIndex, @NonNull OsmElement e, boolean showJumpTo, boolean showEditTags,
            @Nullable String parentTag) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            ElementInfo elementInfoFragment = newInstance(ueIndex, e, showJumpTo, showEditTags, parentTag);
            elementInfoFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
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
     * Create a new instance of the ElementInfo dialog
     * 
     * @param ueIndex index of an UndoElement to compare with
     * @param e OSMElement to display the info on
     * @param showJumpTo display button to jump to object
     * @param showEditTags display button to edit tags
     * @param parentTag tag of any parent dialog
     * @return an instance of ElementInfo
     */
    private static ElementInfo newInstance(int ueIndex, @NonNull OsmElement e, boolean showJumpTo, boolean showEditTags, @Nullable String parentTag) {
        ElementInfo f = new ElementInfo();

        Bundle args = new Bundle();
        args.putInt(UNDOELEMENT_INDEX_KEY, ueIndex);
        args.putSerializable(ELEMENT_KEY, e);
        args.putBoolean(SHOW_JUMP_TO_KEY, showJumpTo);
        args.putBoolean(SHOW_EDIT_TAGS_KEY, showEditTags);
        if (parentTag != null) {
            args.putString(PARENT_TAG_KEY, parentTag);
        }

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        synchronized (this) {
            emptyRole = new SpannableString(getString(R.string.empty_role));
            emptyRole.setSpan(new StyleSpan(Typeface.ITALIC), 0, emptyRole.length(), 0);
        }

        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "Restoring from saved state");
            // this will only work if the saved data is already loaded
            element = App.getDelegator().getOsmElement(savedInstanceState.getString(ELEMENT_TYPE_KEY), savedInstanceState.getLong(ELEMENT_ID_KEY));
            ueIndex = savedInstanceState.getInt(UNDOELEMENT_INDEX_KEY, NO_PRIOR_STATE);
            parentTag = savedInstanceState.getString(PARENT_TAG_KEY);
        } else {
            // always do this first
            element = Util.getSerializeable(getArguments(), ELEMENT_KEY, OsmElement.class);
            ueIndex = getArguments().getInt(UNDOELEMENT_INDEX_KEY, NO_PRIOR_STATE);
            parentTag = getArguments().getString(PARENT_TAG_KEY);
            /*
             * Saving the arguments (done by the FragmentManager) can exceed the 1MB transaction size limit and cause a
             * android.os.TransactionTooLargeException
             */
            getArguments().remove(ELEMENT_KEY);
            getArguments().remove(UNDOELEMENT_INDEX_KEY);
        }
        if (element == null) {
            Log.e(DEBUG_TAG, "element is null");
            ScreenMessage.toastTopError(getContext(), R.string.toast_element_not_found_on_restore);
            return; // dialog will come up empty
        }

        if (ueIndex >= UndoStorage.ORIGINAL_ELEMENT_INDEX) {
            List<UndoElement> undoElements = App.getDelegator().getUndo().getUndoElements(element);
            if (undoElements.size() > ueIndex) {
                UndoElement temp = undoElements.get(ueIndex);
                // suppress display of pre-creation state
                ue = !temp.wasInCurrentStorage() && ueIndex == 0 ? null : temp;
            }
        }

        if (ue == null && (OsmElement.STATE_DELETED == element.getState() || OsmElement.STATE_MODIFIED == element.getState())) {
            Log.e(DEBUG_TAG, "element " + element.getDescription() + " is modified but no prior state available");
            ScreenMessage.toastTopWarning(getContext(), R.string.toast_element_no_prior_state);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = ThemeUtils.getAlertDialogBuilder(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.done, doNothingListener);
        final FragmentActivity activity = getActivity();
        if (activity instanceof Main && element != null) {
            setupButtons(activity, builder);
        }
        builder.setTitle(R.string.element_information);
        builder.setView(createView(null));
        return builder.create();
    }

    /**
     * Set up the modal buttons
     */
    /**
     * @param activity the current activity
     * @param builder an AlertDialog.Builder
     */
    private void setupButtons(@NonNull final FragmentActivity activity, @NonNull Builder builder) {
        BoundingBox tempBox = element.getBounds();
        final ViewBox box = tempBox != null ? new ViewBox(tempBox) : null;
        if (getArguments().getBoolean(SHOW_JUMP_TO_KEY)) {
            builder.setNeutralButton(R.string.goto_element, (dialog, which) -> {
                if (parentTag != null) {
                    de.blau.android.dialogs.Util.dismissDialog(activity, parentTag);
                }
                if (box != null) {
                    double[] center = box.getCenter();
                    ((Main) activity).zoomToAndEdit((int) (center[0] * 1E7D), (int) (center[1] * 1E7D), element);
                } else {
                    ((Main) activity).edit(element);
                    ScreenMessage.toastTopWarning(activity, R.string.toast_no_geometry);
                }
            });
        }
        if (getArguments().getBoolean(SHOW_EDIT_TAGS_KEY) && OsmElement.STATE_DELETED != element.getState()) {
            builder.setNegativeButton(R.string.edit_properties, (dialog, which) -> ((Main) activity).performTagEdit(element, null, false, false));
        }
    }

    @Override
    protected View createView(ViewGroup container) {
        Context ctx = getContext();
        return createComparisionView(ctx, createEmptyView(container), getTableLayoutParams(),
                ctx.getString(ueIndex == 0 ? R.string.original : R.string.previous), ue, ctx.getString(R.string.current), element);
    }

    /**
     * Create the scroll view containing the information
     * 
     * @param ctx an Android Context
     * @param original if not null we compare to this element
     * @param element current or future element
     * @param container
     * 
     * @return a ScrollView
     */
    @NonNull
    static ScrollView createComparisionView(@NonNull Context ctx, @NonNull ScrollView sv, @NonNull TableLayout.LayoutParams tp, String origHeader,
            @Nullable OsmElementInterface original, String elementHeader, @NonNull OsmElement element) {
        if (element == null) {
            return sv;
        }

        TableLayout tl = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);

        boolean compare = original != null;
        boolean deleted = element.getState() == OsmElement.STATE_DELETED;
        tl.setColumnStretchable(1, true);
        tl.setColumnStretchable(2, true);

        tl.addView(TableLayoutUtils.createRow(ctx, R.string.type, element.getName(), tp));
        Spanned id = element.getOsmId() > 0
                ? Util.fromHtml("<a href=\"" + Urls.OSM + "/" + element.getName() + "/" + element.getOsmId() + "\">#" + element.getOsmId() + "</a>")
                : Util.fromHtml("#" + element.getOsmId());
        tl.addView(TableLayoutUtils.createRow(ctx, R.string.id, id, true, tp));
        if (original == null || original.getOsmVersion() == element.getOsmVersion()) {
            tl.addView(TableLayoutUtils.createRow(ctx, R.string.version, Long.toString(element.getOsmVersion()), tp));
            long timestamp = element.getTimestamp();
            if (timestamp > 0) {
                tl.addView(TableLayoutUtils.createRow(ctx, R.string.last_edited, utcDateString(timestamp), tp));
            }
        }

        tl.addView(TableLayoutUtils.divider(ctx));
        if (compare) {
            tl.addView(TableLayoutUtils.createHeaderRow(ctx, origHeader, deleted ? null : elementHeader, tp));
            if (original.getOsmVersion() != element.getOsmVersion()) {
                tl.addView(
                        TableLayoutUtils.createRow(ctx, R.string.version, Long.toString(original.getOsmVersion()), Long.toString(element.getOsmVersion()), tp));
                long originalTimestamp = original.getTimestamp();
                long elementTimestamp = element.getTimestamp();
                if (originalTimestamp > 0 && elementTimestamp > 0) {
                    tl.addView(TableLayoutUtils.createRow(ctx, R.string.last_edited, utcDateString(originalTimestamp), utcDateString(elementTimestamp), tp));
                }
            }
        }
        switch (element.getName()) {
        case Node.NAME:
            String oldLon = null;
            String oldLat = null;
            String oldOlc = null;
            if (compare) {
                oldLon = prettyPrint(((GeoPoint) original).getLon());
                oldLat = prettyPrint(((GeoPoint) original).getLat());
                oldOlc = OpenLocationCode.encode(((GeoPoint) original).getLat() / 1E7D, ((GeoPoint) original).getLon() / 1E7D);
            }
            tl.addView(TableLayoutUtils.createRow(ctx, R.string.location_lon_label, oldLon, deleted ? null : prettyPrint(((GeoPoint) element).getLon()), tp));
            tl.addView(TableLayoutUtils.createRow(ctx, R.string.location_lat_label, oldLat, deleted ? null : prettyPrint(((GeoPoint) element).getLat()), tp));
            tl.addView(TableLayoutUtils.createRow(ctx, R.string.location_olc, oldOlc,
                    deleted ? null : OpenLocationCode.encode(((GeoPoint) element).getLat() / 1E7D, ((GeoPoint) element).getLon() / 1E7D), tp));
            break;
        case Way.NAME:
            boolean isClosed = ((WayInterface) element).isClosed();
            String nodeCount = nodeCountString(((WayInterface) element).nodeCount(), isClosed);
            String lengthString = String.format(Locale.US, "%.2f", ((WayInterface) element).length());
            if (compare) {
                tl.addView(TableLayoutUtils.createRow(ctx, R.string.length_m, String.format(Locale.US, "%.2f", ((WayInterface) original).length()),
                        deleted ? null : lengthString, tp));
                boolean oldIsClosed = ((WayInterface) original).isClosed();
                tl.addView(TableLayoutUtils.createRow(ctx, R.string.nodes, nodeCountString(((WayInterface) original).nodeCount(), oldIsClosed),
                        deleted ? null : nodeCount, tp));
                tl.addView(
                        TableLayoutUtils.createRow(ctx, R.string.closed, isClosedString(ctx, oldIsClosed), deleted ? null : isClosedString(ctx, isClosed), tp));
            } else {
                tl.addView(TableLayoutUtils.createRow(ctx, R.string.length_m, null, lengthString, tp));
                tl.addView(TableLayoutUtils.createRow(ctx, R.string.nodes, null, nodeCount, tp));
                tl.addView(TableLayoutUtils.createRow(ctx, R.string.closed, null, isClosedString(ctx, isClosed), tp));
            }
            // Make this expandable before enabling
            // for (Node n:((Way)e).getNodes()) {
            // tl.addView(createRow("", n.getDescription(),tp));
            // }
            break;
        case Relation.NAME:
            List<RelationMember> members = ((RelationInterface) element).getMembers();
            String oldMembersCount = null;
            List<RelationMember> oldMembers = null;
            if (compare) {
                oldMembers = ((RelationInterface) original).getMembers();
                oldMembersCount = Integer.toString(oldMembers != null ? oldMembers.size() : 0);
            }
            tl.addView(TableLayoutUtils.createRow(ctx, R.string.members, oldMembersCount,
                    !deleted ? Integer.toString(members != null ? members.size() : 0) : null, tp));

            int notDownloaded = countNotDownLoaded(members);
            int oldNotDownloaded = countNotDownLoaded(oldMembers);
            if (notDownloaded > 0 || oldNotDownloaded > 0) {
                tl.addView(TableLayoutUtils.createRow(ctx, R.string.not_downloaded, compare ? Integer.toString(oldNotDownloaded) : null,
                        !deleted ? Integer.toString(notDownloaded) : null, tp));
            }
            break;
        default:
            Log.e(DEBUG_TAG, "Unkown element type " + element.getName());
        }
        Validator validator = App.getDefaultValidator(ctx);
        boolean originalHasProblem = compare && (original instanceof OsmElement) && ((OsmElement) original).hasProblem(ctx, validator) != Validator.OK;
        boolean hasProblem = !deleted && element.hasProblem(ctx, validator) != Validator.OK;
        if (originalHasProblem || hasProblem) {
            tl.addView(TableLayoutUtils.divider(ctx));
            final boolean addOriginalProblems = compare && originalHasProblem;
            if (addOriginalProblems) {
                addErrors(ctx, tl, tp, true, false, validator.describeProblem(ctx, (OsmElement) original));
            }
            if (hasProblem) {
                addErrors(ctx, tl, tp, !addOriginalProblems, compare, validator.describeProblem(ctx, element));
            }
        }

        // tag display
        if (element.hasTags() || (original instanceof OsmElementInterface && !original.getTags().isEmpty())) {
            tl.addView(TableLayoutUtils.divider(ctx));
            tl.addView(TableLayoutUtils.createRow(ctx, R.string.menu_tags, null, null, tp));
            Map<String, String> currentTags = element.getTags(); // the result of getTags is unmodifiable
            Set<String> keys = new TreeSet<>(currentTags.keySet());
            if (compare) {
                if (deleted || currentTags.isEmpty()) {
                    keys = original.getTags().keySet(); // just the original
                } else {
                    keys.addAll(original.getTags().keySet());
                }
            }
            final SpannedString compareEmpty = compare ? new SpannedString("") : null;
            for (String k : keys) {
                String currentValue = currentTags.get(k);
                String oldValue = null;
                boolean oldIsEmpty = true;
                if (currentValue == null) {
                    currentValue = "";
                }
                if (compare) {
                    oldValue = original.getTags().get(k);
                    if (oldValue == null) {
                        oldValue = "";
                    } else {
                        oldIsEmpty = false;
                    }
                }
                // special handling for some stuff
                if (Tags.KEY_WIKIPEDIA.equals(k)) {
                    Log.d(DEBUG_TAG, Urls.WIKIPEDIA + encodeHttpPath(currentValue));
                    addTagRow(ctx, tl, tp, deleted, k, !oldIsEmpty ? encodeUrl(Urls.WIKIPEDIA, oldValue) : compareEmpty,
                            encodeUrl(Urls.WIKIPEDIA, currentValue));
                } else if (Tags.KEY_WIKIDATA.equals(k)) {
                    addTagRow(ctx, tl, tp, deleted, k, !oldIsEmpty ? encodeUrl(Urls.WIKIDATA, oldValue) : compareEmpty, encodeUrl(Urls.WIKIDATA, currentValue));
                } else if (Tags.isWebsiteKey(k)) {
                    try {
                        addTagRow(ctx, tl, tp, deleted, k, !oldIsEmpty ? encodeUrl(oldValue) : compareEmpty, encodeUrl(currentValue));
                    } catch (MalformedURLException | URISyntaxException e1) {
                        Log.d(DEBUG_TAG, "Key " + k + " value " + currentValue + " caused " + element + " " + e1.getMessage());
                        addTagRow(ctx, tl, tp, deleted, k, oldValue, currentValue);
                    }
                } else if (Tags.isPhoneKey(k) || Tags.isEmailKey(k)) {
                    final String url = Tags.isEmailKey(k) ? MAILTO : TEL;
                    addTagRow(ctx, tl, tp, deleted, k, !oldIsEmpty ? encodeUrl(url, oldValue) : compareEmpty, encodeUrl(url, currentValue));
                } else {
                    addTagRow(ctx, tl, tp, deleted, k, oldValue, currentValue);
                }
            }
        }

        // relation member display
        if (element.getName().equals(Relation.NAME)) {
            List<RelationMember> members = ((Relation) element).getMembers();
            if (members != null) {
                TableLayout t2 = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout_2);
                t2.addView(TableLayoutUtils.divider(ctx));
                t2.addView(TableLayoutUtils.createRow(ctx, R.string.members, null, null, tp));
                List<RelationMember> origMembers = null;
                boolean changesPrevious = false;
                if (compare) {
                    t2.addView(TableLayoutUtils.createRow(ctx, "", origHeader, deleted ? null : elementHeader, tp));
                    origMembers = new ArrayList<>(((RelationInterface) original).getMembers());
                }
                int memberCount = members.size();
                for (int index = 0; index < memberCount; index++) {
                    RelationMember member = members.get(index);
                    OsmElement memberElement = member.getElement();
                    String role = member.getRole();
                    String memberDescription = memberElement != null ? memberElement.getDescription() : member.getType() + " " + member.getRef();
                    if (compare) {
                        String origRole = null;
                        if (origMembers != null) {
                            RelationMember origMember = null;
                            int origIndex = 0;
                            for (; origIndex < origMembers.size(); origIndex++) {
                                RelationMember tempMember = origMembers.get(origIndex);
                                if (member.getRef() == tempMember.getRef()) {
                                    origMember = tempMember;
                                    break;
                                }
                            }
                            if (origMember != null) {
                                origRole = origMember.getRole();
                                origMembers.remove(origIndex);
                            }
                        }
                        if (memberCount > DISPLAY_LIMIT) {
                            if ((role == null && origRole != null) || (role != null && !role.equals(origRole))) {
                                if (!changesPrevious) {
                                    changesPrevious = true;
                                    t2.addView(skipped(ctx, tp));
                                }
                                t2.addView(TableLayoutUtils.createRow(ctx, memberDescription, getPrettyRole(origRole), getPrettyRole(role), tp));
                            } else {
                                changesPrevious = false;
                            }
                        } else {
                            t2.addView(TableLayoutUtils.createRow(ctx, memberDescription, getPrettyRole(origRole), getPrettyRole(role), tp));
                        }
                    } else {
                        t2.addView(TableLayoutUtils.createRow(ctx, memberDescription, getPrettyRole(role), tp));
                        if (index == (DISPLAY_LIMIT - 1)) {
                            t2.addView(skipped(ctx, tp));
                            break;
                        }
                    }
                }
                // write out any remaining original members
                if (origMembers != null) {
                    for (RelationMember member : origMembers) {
                        if (!changesPrevious) {
                            changesPrevious = true;
                            t2.addView(skipped(ctx, tp));
                        }
                        String memberDescription = member.getElement() != null ? member.getElement().getDescription()
                                : member.getType() + " " + member.getRef();
                        t2.addView(TableLayoutUtils.createRow(ctx, memberDescription, getPrettyRole(member.getRole()), "", tp));
                    }
                } else if (!changesPrevious && compare) {
                    t2.addView(skipped(ctx, tp));
                }
            }
        }

        // relation membership display
        List<Relation> parentsList = element.getParentRelations();
        List<Relation> origParentsList = compare ? original.getParentRelations() : null;

        // complicated stuff to determine if a role changed
        Map<Long, List<RelationMember>> undoMembersMap = null;
        boolean hasParents = parentsList != null && !parentsList.isEmpty();
        // FIXME for non undo comparision
        if (hasParents && original instanceof UndoElement) {
            for (Relation parent : parentsList) {
                OsmElementInterface originalParent = App.getDelegator().getUndo().getOriginal(parent);
                if (originalParent != null) {
                    List<RelationMember> ueMembers = ((RelationInterface) originalParent).getAllMembers(element);
                    List<RelationMember> members = parent.getAllMembers(element);
                    for (RelationMember ueMember : ueMembers) {
                        if (!hasRole(ueMember.getRole(), members)) {
                            if (undoMembersMap == null) {
                                undoMembersMap = new HashMap<>();
                                compare = true;
                            }
                            undoMembersMap.put(originalParent.getOsmId(), ueMembers);
                        }
                    }
                }
            }
        }
        if (hasParents || (origParentsList != null && !origParentsList.isEmpty())) {
            TableLayout t3 = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout_3);
            t3.addView(TableLayoutUtils.divider(ctx));
            t3.addView(TableLayoutUtils.createRow(ctx, R.string.relation_membership, null, null, tp));
            if (compare) {
                t3.addView(TableLayoutUtils.createRow(ctx, "", ctx.getString(R.string.original), deleted ? null : ctx.getString(R.string.current), tp));
            }
            // get rid of duplicates and get something that we can modify ( but retain order)
            Set<Relation> parents = parentsList != null ? new LinkedHashSet<>(parentsList) : null;
            Set<Relation> origParents = origParentsList != null ? new LinkedHashSet<>(origParentsList) : null;

            if (parents != null) {
                for (Relation r : parents) {
                    List<RelationMember> members = r.getAllMembers(element);
                    Set<RelationMember> origMembers = null;
                    UndoElement origRelation = null;
                    if (compare && origParentsList != null && origParentsList.contains(r)) {
                        origRelation = App.getDelegator().getUndo().getOriginal(r);
                        if (origRelation != null) {
                            origMembers = new LinkedHashSet<>(((RelationInterface) origRelation).getAllMembers(element));
                            origParents.remove(r);
                        }
                    }
                    if (undoMembersMap != null) {
                        List<RelationMember> undoMembers = undoMembersMap.get(r.getOsmId());
                        if (undoMembers != null) {
                            origMembers = new LinkedHashSet<>(undoMembers); // override
                        }
                    }
                    for (RelationMember rm : members) {
                        if (rm != null) {
                            String role = rm.getRole();
                            if (compare) {
                                String origRole = null;
                                if (origMembers != null && !origMembers.isEmpty()) {
                                    for (RelationMember origMember : new ArrayList<>(origMembers)) {
                                        origRole = origMember.getRole();
                                        origMembers.remove(origMember);
                                        break; // NOSONAR there is no elegant way to get the first element
                                    }
                                }
                                t3.addView(TableLayoutUtils.createRow(ctx, r.getDescription(), getPrettyRole(origRole), getPrettyRole(role), tp));
                            } else {
                                t3.addView(TableLayoutUtils.createRow(ctx, r.getDescription(), getPrettyRole(role), tp));
                            }
                        } else {
                            // inconsistent state
                            String message = "inconsistent state: " + element.getDescription() + " is not a member of " + r;
                            Log.d(DEBUG_TAG, message);
                            ACRAHelper.nocrashReport(null, message);
                        }
                    }
                    // write out any relation members left
                    if (origMembers != null && !origMembers.isEmpty()) {
                        for (RelationMember origMember : origMembers) {
                            String role = origMember.getRole();
                            t3.addView(TableLayoutUtils.createRow(ctx, r.getDescription(), "", getPrettyRole(role), tp));
                        }
                    }
                }
            }

            if (origParents != null) {
                // write out the relations we are no longer a member of
                for (Relation r : origParents) {
                    UndoElement origRelation = App.getDelegator().getUndo().getOriginal(r);
                    if (origRelation != null) {
                        List<RelationMember> members = ((RelationInterface) origRelation).getAllMembers(element);
                        if (members != null) {
                            for (RelationMember rm : members) {
                                t3.addView(TableLayoutUtils.createRow(ctx, r.getDescription(), getPrettyRole(rm.getRole()), "", tp));
                            }
                        }
                    }
                }
            }
        }
        return sv;
    }

    /**
     * Add a list of errors to the display
     * 
     * @param ctx an Android Context
     * @param tl the TableLayout we are adding to
     * @param tp layout params
     * @param first if true a header will be added for the first error
     * @param compare if true the errors will be added to the 2nd column
     * @param errors the error strings
     */
    private static void addErrors(@NonNull Context ctx, @NonNull TableLayout tl, @NonNull TableLayout.LayoutParams tp, boolean first, boolean compare,
            @NonNull String[] errors) {
        for (String problem : errors) {
            String header = "";
            if (first) {
                header = ctx.getString(R.string.problem);
                first = false;
            }
            if (compare) {
                tl.addView(TableLayoutUtils.createRow(ctx, header, "", problem, tp, R.attr.error, R.color.material_red));
            } else {
                tl.addView(TableLayoutUtils.createRow(ctx, header, problem, null, tp, R.attr.error, R.color.material_red));
            }
        }
    }

    /**
     * Output the timestamp in a two row format
     * 
     * @param timestamp the timestamp in seconds
     * @return a two line timestamp string
     */
    private static String utcDateString(long timestamp) {
        return DateFormatter.getUtcFormat(TWO_ROW_DATE_FORMAT).format(timestamp * 1000L);
    }

    /**
     * Add a tag row to the table
     * 
     * @param ctx calling Activity
     * @param tl the TableLayout
     * @param tp LayoutParams
     * @param deleted true if object was deleted
     * @param key the tag key
     * @param oldValue the tag old value
     * @param currentValue the tag new value
     */
    private static void addTagRow(@NonNull Context ctx, @NonNull TableLayout tl, @NonNull TableLayout.LayoutParams tp, boolean deleted, @NonNull String key,
            @NonNull Spanned oldValue, @Nullable Spanned currentValue) {
        tl.addView(TableLayoutUtils.createRow(ctx, key, oldValue, !deleted ? currentValue : null, false, tp, R.attr.colorAccent, R.color.material_teal));
    }

    /**
     * Add a tag row to the table
     * 
     * @param ctx calling Activity
     * @param tl the TableLayout
     * @param tp LayoutParams
     * @param deleted true if object was deleted
     * @param key the tag key
     * @param oldValue the tag old value
     * @param currentValue the tag new value
     */
    private static void addTagRow(@NonNull Context ctx, @NonNull TableLayout tl, @NonNull TableLayout.LayoutParams tp, boolean deleted, @NonNull String key,
            @NonNull String oldValue, @Nullable String currentValue) {
        tl.addView(TableLayoutUtils.createRow(ctx, key, oldValue, !deleted ? currentValue : null, false, tp, R.attr.colorAccent, R.color.material_teal));
    }

    /**
     * Indicate skipped content
     * 
     * @param ctx the calling FragmentActivity
     * @param tp layout params
     * @return a TableRow Layout
     */
    @NonNull
    private static TableRow skipped(@NonNull Context ctx, @NonNull TableLayout.LayoutParams tp) {
        return TableLayoutUtils.createRow(ctx, "...", null, tp);
    }

    /**
     * Check if the List of RelatioNMember contains a member with the specified role
     * 
     * As the List of members will be very short, typically length one, there is no issue with searching sequentially
     * 
     * @param role the role
     * @param members the list of members
     * @return true if the role was found
     */
    private static boolean hasRole(@Nullable String role, @NonNull List<RelationMember> members) {
        for (RelationMember rm : members) {
            String role2 = rm.getRole();
            if ((role == null && rm.getRole() == null) || (role != null && role.equals(role2))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Pretty print a coordinate value
     * 
     * @param coordE7 the coordinate in WGS84*1E7
     * @return a reasonable looking string representation
     */
    @NonNull
    private static String prettyPrint(int coordE7) {
        return String.format(Locale.US, "%.7f", coordE7 / 1E7d) + "°";
    }

    /**
     * Get a nice String for role if it is empty
     * 
     * @param role the role
     * @return role or a String indicating that it is empty
     */
    @NonNull
    private static SpannableString getPrettyRole(@Nullable String role) {
        if (role != null) {
            return "".equals(role) ? emptyRole : new SpannableString(role);
        }
        return new SpannableString("");
    }

    /**
     * Get the count of RelationMembers that are not downloaded
     * 
     * @param members the List of RelationMembers
     * @return a count of those members that haven't been downloaded yet
     */
    private static int countNotDownLoaded(@Nullable List<RelationMember> members) {
        int notDownloaded = 0;
        if (members != null) {
            for (RelationMember rm : members) {
                if (rm.getElement() == null) {
                    notDownloaded++;
                }
            }
        }
        return notDownloaded;
    }

    /**
     * Create an clickable Url as text
     * 
     * @param url base url as String
     * @param value append this to the url
     * @return clickable text
     */
    private static Spanned encodeUrl(@NonNull String url, @NonNull String value) {
        return Util.fromHtml("<a href=\"" + url + encodeHttpPath(value) + "\">" + value + "</a>");
    }

    /**
     * Create an clickable Url as text
     * 
     * @param value url to use
     * @return clickable text
     * @throws MalformedURLException for broken URLs
     * @throws URISyntaxException for broken URLs
     */
    private static Spanned encodeUrl(@NonNull String value) throws MalformedURLException, URISyntaxException {
        if ("".equals(value)) {
            return new SpannedString("");
        }
        URL url = new URL(value);
        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        return Util.fromHtml("<a href=\"" + uri.toURL() + "\">" + value + "</a>");
    }

    /**
     * Get a text resource id reflecting if a Way is closed or not
     * 
     * @param isClosed true if the way is closed
     * @return yes or no
     */
    private static String isClosedString(@NonNull Context ctx, boolean isClosed) {
        return ctx.getString(isClosed ? R.string.yes : R.string.no);
    }

    /**
     * Get a string with the number of nodes in a way
     * 
     * @param count the raw node count
     * @param isClosed if true subtract one
     * @return the count as a String
     */
    private static String nodeCountString(int count, boolean isClosed) {
        return Integer.toString(count + (isClosed && count != 1 ? NO_PRIOR_STATE : 0));
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (element == null) {
            Log.e(DEBUG_TAG, "attempt to save null element");
            return;
        }
        outState.putString(ELEMENT_TYPE_KEY, element.getName());
        outState.putLong(ELEMENT_ID_KEY, element.getOsmId());
        outState.putInt(UNDOELEMENT_INDEX_KEY, ueIndex);
        outState.putString(PARENT_TAG_KEY, parentTag);
    }

    /**
     * Url encode a String
     * 
     * @param path String to encode
     * @return the encoded String
     */
    private static String encodeHttpPath(String path) {
        try {
            return URLEncoder.encode(path, OsmXml.UTF_8);
        } catch (UnsupportedEncodingException e) {
            Log.d(DEBUG_TAG, "Path " + path + " caused " + e);
            return "";
        }
    }
}
