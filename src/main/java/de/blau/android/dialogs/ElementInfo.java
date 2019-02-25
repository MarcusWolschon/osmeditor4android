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

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmParser;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Tags;
import de.blau.android.osm.UndoStorage.UndoElement;
import de.blau.android.osm.UndoStorage.UndoNode;
import de.blau.android.osm.UndoStorage.UndoRelation;
import de.blau.android.osm.UndoStorage.UndoWay;
import de.blau.android.osm.Way;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.validation.Validator;

/**
 * Very simple dialog fragment to display some info on an OsmElement and potentially compare it with an UndoElement
 * 
 * @author simon
 *
 */
public class ElementInfo extends DialogFragment {

    private static final int    DISPLAY_LIMIT   = 10;
    private static final String ELEMENT_KEY     = "element";
    private static final String UNDOELEMENT_KEY = "undoelement";

    private static final String DEBUG_TAG = ElementInfo.class.getName();

    private static final String TAG = "fragment_element_info";

    private SpannableString emptyRole;

    /**
     * Show an info dialog for the supplied OsmElement
     * 
     * @param activity the calling Activity
     * @param e the OsmElement
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull OsmElement e) {
        showDialog(activity, null, e);
    }

    /**
     * Show an info dialog for the supplied OsmElement
     * 
     * @param activity the calling Activity
     * @param ue an UndoElement to compare with
     * @param e the OsmElement
     */
    public static void showDialog(@NonNull FragmentActivity activity, @Nullable UndoElement ue, @NonNull OsmElement e) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            ElementInfo elementInfoFragment = newInstance(ue, e);
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
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment != null) {
                ft.remove(fragment);
            }
            ft.commit();
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "dismissDialog", isex);
        }
    }

    /**
     * Create a new instance of the ElementInfo dialog
     * 
     * @param ue an UndoElement to compare with
     * @param e OSMElement to display the info on
     * @return an instance of ElementInfo
     */
    private static ElementInfo newInstance(@Nullable UndoElement ue, @NonNull OsmElement e) {
        ElementInfo f = new ElementInfo();

        Bundle args = new Bundle();
        args.putSerializable(UNDOELEMENT_KEY, ue);
        args.putSerializable(ELEMENT_KEY, e);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        emptyRole = new SpannableString(getString(R.string.empty_role));
        emptyRole.setSpan(new StyleSpan(Typeface.ITALIC), 0, emptyRole.length(), 0);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.done, doNothingListener);
        builder.setTitle(R.string.element_information);
        builder.setView(createView(null));
        return builder.create();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (!getShowsDialog()) {
            return createView(container);
        }
        return null;
    }

    /**
     * Pretty print a coordinate value
     * 
     * @param coordE7 the coordinate in WGS84*1E7
     * @return a reasonable looking string representation
     */
    private String prettyPrint(int coordE7) {
        return String.format(Locale.US, "%.7f", coordE7 / 1E7d) + "°";
    }

    /**
     * Create the view we want to display
     * 
     * @param container parent view or null
     * @return the View
     */
    private View createView(ViewGroup container) {
        FragmentActivity activity = getActivity();
        LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(activity);
        ScrollView sv = (ScrollView) themedInflater.inflate(R.layout.element_info_view, container, false);
        TableLayout tl = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);

        OsmElement e = (OsmElement) getArguments().getSerializable(ELEMENT_KEY);
        UndoElement ue = (UndoElement) getArguments().getSerializable(UNDOELEMENT_KEY);

        boolean compare = ue != null;

        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(10, 2, 10, 2);
        if (e != null) {
            boolean deleted = e.getState() == OsmElement.STATE_DELETED;
            tl.setColumnStretchable(1, true);
            tl.setColumnStretchable(2, true);

            tl.addView(TableLayoutUtils.createRow(activity, R.string.type, e.getName(), tp));
            Spanned id = e.getOsmId() > 0 ? Util.fromHtml("<a href=\"" + Urls.OSM + "/" + e.getName() + "/" + e.getOsmId() + "\">#" + e.getOsmId() + "</a>")
                    : Util.fromHtml("#" + e.getOsmId());
            tl.addView(TableLayoutUtils.createRow(activity, R.string.id, id, true, tp));
            tl.addView(TableLayoutUtils.createRow(activity, R.string.version, Long.toString(e.getOsmVersion()), tp));
            long timestamp = e.getTimestamp();
            if (timestamp > 0) {
                tl.addView(TableLayoutUtils.createRow(activity, R.string.last_edited,
                        DateFormatter.getUtcFormat(OsmParser.TIMESTAMP_FORMAT).format(timestamp * 1000L), tp));
            }

            tl.addView(TableLayoutUtils.divider(activity));
            if (compare) {
                tl.addView(TableLayoutUtils.createRow(activity, "", getString(R.string.original), deleted ? null : getString(R.string.current), tp));
            }
            if (e.getName().equals(Node.NAME)) {
                String oldLon = null;
                String oldLat = null;
                if (compare) {
                    oldLon = prettyPrint(((UndoNode) ue).getLon());
                    oldLat = prettyPrint(((UndoNode) ue).getLat());
                }
                tl.addView(TableLayoutUtils.createRow(activity, R.string.location_lon_label, oldLon, deleted ? null : prettyPrint(((Node) e).getLon()), tp));
                tl.addView(TableLayoutUtils.createRow(activity, R.string.location_lat_label, oldLat, deleted ? null : prettyPrint(((Node) e).getLat()), tp));
            } else if (e.getName().equals(Way.NAME)) {
                boolean isClosed = ((Way) e).isClosed();
                String nodeCount = nodeCountString(((Way) e).nodeCount(), isClosed);
                String lengthString = String.format(Locale.US, "%.2f", ((Way) e).length());
                if (compare) {
                    tl.addView(TableLayoutUtils.createRow(activity, R.string.length_m, String.format(Locale.US, "%.2f", ((UndoWay) ue).length()),
                            deleted ? null : lengthString, tp));
                    boolean oldIsClosed = ((UndoWay) ue).isClosed();
                    tl.addView(TableLayoutUtils.createRow(activity, R.string.nodes, nodeCountString(((UndoWay) ue).nodeCount(), oldIsClosed),
                            deleted ? null : nodeCount, tp));
                    tl.addView(
                            TableLayoutUtils.createRow(activity, R.string.closed, isClosedString(oldIsClosed), deleted ? null : isClosedString(isClosed), tp));
                } else {
                    tl.addView(TableLayoutUtils.createRow(activity, R.string.length_m, null, lengthString, tp));
                    tl.addView(TableLayoutUtils.createRow(activity, R.string.nodes, null, nodeCount, tp));
                    tl.addView(TableLayoutUtils.createRow(activity, R.string.closed, null, isClosedString(isClosed), tp));
                }
                // Make this expandable before enabling
                // for (Node n:((Way)e).getNodes()) {
                // tl.addView(createRow("", n.getDescription(),tp));
                // }
            } else if (e.getName().equals(Relation.NAME)) {
                List<RelationMember> members = ((Relation) e).getMembers();
                String oldMembersCount = null;
                List<RelationMember> oldMembers = null;
                if (compare) {
                    oldMembers = ((UndoRelation) ue).getMembers();
                    oldMembersCount = Integer.toString(oldMembers != null ? oldMembers.size() : 0);
                }
                tl.addView(TableLayoutUtils.createRow(activity, R.string.members, oldMembersCount,
                        !deleted ? Integer.toString(members != null ? members.size() : 0) : null, tp));

                int notDownloaded = countNotDownLoaded(members);
                int oldNotDownloaded = countNotDownLoaded(oldMembers);
                if (notDownloaded > 0 || oldNotDownloaded > 0) {
                    tl.addView(TableLayoutUtils.createRow(activity, R.string.not_downloaded, compare ? Integer.toString(oldNotDownloaded) : null,
                            !deleted ? Integer.toString(notDownloaded) : null, tp));
                }
            }
            Validator validator = App.getDefaultValidator(getActivity());
            if (!deleted && e.hasProblem(getActivity(), validator) != Validator.OK) {
                tl.addView(TableLayoutUtils.divider(activity));
                boolean first = true;
                for (String problem : validator.describeProblem(getActivity(), e)) {
                    String header = "";
                    if (first) {
                        header = getString(R.string.problem);
                        first = false;
                    }
                    if (compare) {
                        tl.addView(TableLayoutUtils.createRow(activity, header, "", problem, tp));
                    } else {
                        tl.addView(TableLayoutUtils.createRow(activity, header, problem, null, tp));
                    }
                }
            }

            // tag display
            if (e.getTags() != null && e.getTags().size() > 0) {
                tl.addView(TableLayoutUtils.divider(activity));
                tl.addView(TableLayoutUtils.createRow(activity, R.string.menu_tags, null, null, tp));
                Map<String, String> currentTags = e.getTags(); // the result of getTags is unmodifiable
                Set<String> keys = new TreeSet<>(currentTags.keySet());
                if (compare) {
                    if (deleted) {
                        keys = ue.getTags().keySet(); // just the original
                    } else {
                        keys.addAll(ue.getTags().keySet());
                    }
                }
                for (String k : keys) {
                    String currentValue = currentTags.get(k);
                    String oldValue = null;
                    boolean oldIsEmpty = true;
                    if (currentValue == null) {
                        currentValue = "";
                    }
                    if (compare) {
                        oldValue = ue.getTags().get(k);
                        if (oldValue == null) {
                            oldValue = "";
                        } else {
                            oldIsEmpty = false;
                        }
                    }
                    // special handling for some stuff
                    if (k.equals(Tags.KEY_WIKIPEDIA)) {
                        Log.d(DEBUG_TAG, Urls.WIKIPEDIA + encodeHttpPath(currentValue));
                        tl.addView(TableLayoutUtils.createRow(activity, k, !oldIsEmpty ? encodeUrl(Urls.WIKIPEDIA, oldValue) : (compare ? "" : null),
                                !deleted ? encodeUrl(Urls.WIKIPEDIA, currentValue) : null, true, tp));
                    } else if (k.equals(Tags.KEY_WIKIDATA)) {
                        tl.addView(TableLayoutUtils.createRow(activity, k, !oldIsEmpty ? encodeUrl(Urls.WIKIDATA, oldValue) : (compare ? "" : null),
                                !deleted ? encodeUrl(Urls.WIKIDATA, currentValue) : null, true, tp));
                    } else if (Tags.isWebsiteKey(k)) {
                        try {
                            tl.addView(TableLayoutUtils.createRow(activity, k, !oldIsEmpty ? encodeUrl(oldValue) : (compare ? "" : null),
                                    !deleted ? encodeUrl(currentValue) : null, true, tp));
                        } catch (MalformedURLException | URISyntaxException e1) {
                            Log.d(DEBUG_TAG, "Value " + currentValue + " caused " + e);
                            tl.addView(TableLayoutUtils.createRow(activity, k, currentValue, tp));
                        }
                    } else {
                        tl.addView(TableLayoutUtils.createRow(activity, k, oldValue, !deleted ? currentValue : null, false, tp));
                    }
                }
            }

            // relation member display
            if (e.getName().equals(Relation.NAME)) {
                List<RelationMember> members = ((Relation) e).getMembers();
                if (members != null) {
                    TableLayout t2 = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout_2);
                    t2.addView(TableLayoutUtils.divider(activity));
                    t2.addView(TableLayoutUtils.createRow(activity, R.string.members, null, null, tp));
                    List<RelationMember> origMembers = null;
                    boolean changesPrevious = false;
                    if (compare) {
                        t2.addView(TableLayoutUtils.createRow(activity, "", getString(R.string.original), deleted ? null : getString(R.string.current), tp));
                        origMembers = new ArrayList<>(((UndoRelation) ue).getMembers());
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
                                        t2.addView(skipped(activity, tp));
                                    }
                                    t2.addView(TableLayoutUtils.createRow(activity, memberDescription, getPrettyRole(origRole), getPrettyRole(role), tp));
                                } else {
                                    changesPrevious = false;
                                }
                            } else {
                                t2.addView(TableLayoutUtils.createRow(activity, memberDescription, getPrettyRole(origRole), getPrettyRole(role), tp));
                            }
                        } else {
                            t2.addView(TableLayoutUtils.createRow(activity, memberDescription, getPrettyRole(role), tp));
                            if (index == (DISPLAY_LIMIT - 1)) {
                                t2.addView(skipped(activity, tp));
                                break;
                            }
                        }
                    }
                    // write out any remaining original members
                    if (origMembers != null) {
                        for (RelationMember member : origMembers) {
                            if (!changesPrevious) {
                                changesPrevious = true;
                                t2.addView(skipped(activity, tp));
                            }
                            String memberDescription = member.getElement() != null ? member.getElement().getDescription()
                                    : member.getType() + " " + member.getRef();
                            t2.addView(TableLayoutUtils.createRow(activity, memberDescription, getPrettyRole(member.getRole()), "", tp));
                        }
                    } else if (!changesPrevious && compare) {
                        t2.addView(skipped(activity, tp));
                    }
                }
            }

            // relation membership display
            List<Relation> parentsList = e.getParentRelations();
            List<Relation> origParentsList = compare ? ue.getParentRelations() : null;

            // complicated stuff to determine if a role changed
            Map<Long, List<RelationMember>> undoMembersMap = null;
            boolean hasParents = parentsList != null && !parentsList.isEmpty();
            if (hasParents) {
                for (Relation parent : parentsList) {
                    UndoRelation parentUE = (UndoRelation) App.getDelegator().getUndo().getOriginal(parent);
                    if (parentUE != null) {
                        List<RelationMember> ueMembers = parentUE.getAllMembers(e);
                        List<RelationMember> members = parent.getAllMembers(e);
                        for (RelationMember ueMember : ueMembers) {
                            if (!hasRole(ueMember.getRole(), members)) {
                                if (undoMembersMap == null) {
                                    undoMembersMap = new HashMap<>();
                                    compare = true;
                                }
                                undoMembersMap.put(parentUE.getOsmId(), ueMembers);
                            }
                        }
                    }
                }
            }
            if (hasParents || (origParentsList != null && !origParentsList.isEmpty())) {
                TableLayout t3 = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout_3);
                t3.addView(TableLayoutUtils.divider(activity));
                t3.addView(TableLayoutUtils.createRow(activity, R.string.relation_membership, null, null, tp));
                if (compare) {
                    t3.addView(TableLayoutUtils.createRow(activity, "", getString(R.string.original), deleted ? null : getString(R.string.current), tp));
                }
                // get rid of duplicates and get something that we can modify ( but retain order)
                Set<Relation> parents = parentsList != null ? new LinkedHashSet<>(parentsList) : null;
                Set<Relation> origParents = origParentsList != null ? new LinkedHashSet<>(origParentsList) : null;

                if (parents != null) {
                    for (Relation r : parents) {
                        List<RelationMember> members = r.getAllMembers(e);
                        Set<RelationMember> origMembers = null;
                        UndoElement origRelation = null;
                        if (compare && origParentsList != null && origParentsList.contains(r)) {
                            origRelation = App.getDelegator().getUndo().getOriginal(r);
                            if (origRelation != null) {
                                origMembers = new LinkedHashSet<>(((UndoRelation) origRelation).getAllMembers(e));
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
                                    t3.addView(TableLayoutUtils.createRow(activity, r.getDescription(), getPrettyRole(origRole), getPrettyRole(role), tp));
                                } else {
                                    t3.addView(TableLayoutUtils.createRow(activity, r.getDescription(), getPrettyRole(role), tp));
                                }
                            } else {
                                // inconsistent state
                                String message = "inconsistent state: " + e.getDescription() + " is not a member of " + r;
                                Log.d(DEBUG_TAG, message);
                                ACRAHelper.nocrashReport(null, message);
                            }
                        }
                        // write out any relation members left
                        if (origMembers != null && !origMembers.isEmpty()) {
                            for (RelationMember origMember : origMembers) {
                                String role = origMember.getRole();
                                t3.addView(TableLayoutUtils.createRow(activity, r.getDescription(), "", getPrettyRole(role), tp));
                            }
                        }
                    }
                }

                if (origParents != null) {
                    // write out the relations we are no longer a member of
                    for (Relation r : origParents) {
                        UndoElement origRelation = App.getDelegator().getUndo().getOriginal(r);
                        if (origRelation != null) {
                            List<RelationMember> members = ((UndoRelation) origRelation).getAllMembers(e);
                            if (members != null) {
                                for (RelationMember rm : members) {
                                    t3.addView(TableLayoutUtils.createRow(activity, r.getDescription(), getPrettyRole(rm.getRole()), "", tp));
                                }
                            }
                        }
                    }
                }
            }
        }
        return sv;

    }

    /**
     * Indicate skipped content
     * 
     * @param activity the calling FragmentActivity
     * @param tp layout params
     * @return a TableRow Layout
     */
    @NonNull
    private TableRow skipped(@NonNull FragmentActivity activity, @NonNull TableLayout.LayoutParams tp) {
        return TableLayoutUtils.createRow(activity, "...", null, tp);
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
    private boolean hasRole(@Nullable String role, @NonNull List<RelationMember> members) {
        for (RelationMember rm : members) {
            String role2 = rm.getRole();
            if ((role == null && rm.getRole() == null) || (role != null && role.equals(role2))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a nice String for role if it is empty
     * 
     * @param role the role
     * @return role or a String indicating that it is empty
     */
    private SpannableString getPrettyRole(@Nullable String role) {
        return role == null || "".equals(role) ? emptyRole : new SpannableString(role);
    }

    /**
     * Get the count of RelationMembers that are not downloaded
     * 
     * @param members the List of RelationMembers
     * @return a count of those members that haven't been downloaded yet
     */
    private int countNotDownLoaded(@Nullable List<RelationMember> members) {
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
    private Spanned encodeUrl(@NonNull String url, @NonNull String value) {
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
    private Spanned encodeUrl(@NonNull String value) throws MalformedURLException, URISyntaxException {
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
    private String isClosedString(boolean isClosed) {
        return getString(isClosed ? R.string.yes : R.string.no);
    }

    /**
     * Get a string with the number of nodes in a way
     * 
     * @param count the raw node count
     * @param isClosed if true substract one
     * @return the count as a String
     */
    private String nodeCountString(int count, boolean isClosed) {
        return Integer.toString(count + (isClosed ? -1 : 0));
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * Url encode a String
     * 
     * @param path String to encode
     * @return the encoded String
     */
    private String encodeHttpPath(String path) {
        try {
            return URLEncoder.encode(path, OsmXml.UTF_8);
        } catch (UnsupportedEncodingException e) {
            Log.d(DEBUG_TAG, "Path " + path + " caused " + e);
            return "";
        }
    }
}
