package de.blau.android.dialogs;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import android.app.Dialog;
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
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmParser;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Tags;
import de.blau.android.osm.UndoStorage.UndoElement;
import de.blau.android.osm.UndoStorage.UndoNode;
import de.blau.android.osm.UndoStorage.UndoRelation;
import de.blau.android.osm.UndoStorage.UndoWay;
import de.blau.android.osm.Way;
import de.blau.android.util.ACRAHelper;
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

    private static final String ELEMENT_KEY     = "element";
    private static final String UNDOELEMENT_KEY = "undoelement";

    private static final String DEBUG_TAG = ElementInfo.class.getName();

    private static final String TAG = "fragment_element_info";

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
                        new SimpleDateFormat(OsmParser.TIMESTAMP_FORMAT).format(timestamp * 1000L), tp));
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

            List<Relation> parentsList = e.getParentRelations();
            List<Relation> origParentsList = compare ? ue.getParentRelations() : null;
            if ((parentsList != null && !parentsList.isEmpty()) || (origParentsList != null && !origParentsList.isEmpty())) {
                tl.addView(TableLayoutUtils.divider(activity));
                tl.addView(TableLayoutUtils.createRow(activity, R.string.relation_membership, null, null, tp));
                // get rid of duplicates and get something that we can modify
                Set<Relation> parents = parentsList != null ? new HashSet<>(parentsList) : null;
                Set<Relation> origParents = origParentsList != null ? new HashSet<>(origParentsList) : null;

                if (parents != null) {
                    for (Relation r : parents) {
                        List<RelationMember> members = r.getAllMembers(e);
                        List<RelationMember> origMembers = null;
                        UndoElement origRelation = null;
                        if (compare && origParentsList != null && origParentsList.contains(r)) {
                            origRelation = App.getDelegator().getUndo().getOriginal(r);
                            if (origRelation != null) {
                                origMembers = ((UndoRelation) origRelation).getAllMembers(e);
                                origParents.remove(r);
                            }
                        }
                        for (RelationMember rm : members) {
                            if (rm != null) {
                                String role = rm.getRole();
                                if (compare) {
                                    String origDescription = "";
                                    if (origMembers != null) {
                                        for (RelationMember origMember : new ArrayList<>(origMembers)) {
                                            String origRole = origMember.getRole();
                                            if ((origRole == null && role == null) || (origRole != null && origRole.equals(role))) {
                                                origDescription = r.getDescription(); // should really use origRelation
                                                origMembers.remove(origMember);
                                                break;
                                            }
                                        }
                                    }
                                    tl.addView(TableLayoutUtils.createRow(activity, getPrettyRole(role), origDescription, r.getDescription(), tp));
                                } else {
                                    tl.addView(TableLayoutUtils.createRow(activity, getPrettyRole(role), r.getDescription(), tp));
                                }
                            } else {
                                // inconsistent state
                                String message = "inconsistent state: " + e.getDescription() + " is not a member of " + r;
                                Log.d(DEBUG_TAG, message);
                                ACRAHelper.nocrashReport(null, message);
                            }
                        }
                        // write out any relation members left (typicall with different roles)
                        if (origMembers != null && !origMembers.isEmpty()) {
                            for (RelationMember origMember : origMembers) {
                                String role = origMember.getRole();
                                tl.addView(TableLayoutUtils.createRow(activity, getPrettyRole(role), r.getDescription(), "", tp));
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
                                    tl.addView(TableLayoutUtils.createRow(activity, getPrettyRole(rm.getRole()), r.getDescription(), "", tp));
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
     * Get a nice String for role if it is empty
     * 
     * @param role the role
     * @return role or a String indicating that it is empty
     */
    private String getPrettyRole(@Nullable String role) {
        return role == null || "".equals(role) ? getString(R.string.empty_role) : role;
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
            return URLEncoder.encode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.d(DEBUG_TAG, "Path " + path + " caused " + e);
            return "";
        }
    }
}
