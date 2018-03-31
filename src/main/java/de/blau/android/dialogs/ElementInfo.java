package de.blau.android.dialogs;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.acra.ACRA;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmParser;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.util.ThemeUtils;
import de.blau.android.validation.Validator;

/**
 * Very simple dialog fragment to display some info on an OSM element
 * 
 * @author simon
 *
 */
public class ElementInfo extends DialogFragment {

    private static final String ELEMENT = "element";

    private static final String DEBUG_TAG = ElementInfo.class.getName();

    private static final String TAG = "fragment_element_info";

    static public void showDialog(FragmentActivity activity, OsmElement e) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            ElementInfo elementInfoFragment = newInstance(e);
            elementInfoFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    private static void dismissDialog(FragmentActivity activity) {
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment != null) {
                ft.remove(fragment);
            }
            ft.commit();
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     */
    private static ElementInfo newInstance(OsmElement e) {
        ElementInfo f = new ElementInfo();

        Bundle args = new Bundle();
        args.putSerializable(ELEMENT, e);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Preferences prefs = new Preferences(getActivity());
        // if (prefs.lightThemeEnabled()) {
        // setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_DialogLight);
        // } else {
        // setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_DialogDark);
        // }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(ThemeUtils.getThemedContext(getActivity(), R.style.Theme_DialogLight, R.style.Theme_DialogDark));
    }

    @SuppressWarnings("deprecation")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(activity);
        ScrollView sv = (ScrollView) themedInflater.inflate(R.layout.element_info_view, container, false);
        TableLayout tl = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);

        OsmElement e = (OsmElement) getArguments().getSerializable(ELEMENT);

        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(10, 2, 10, 2);

        if (e != null) {
            // tl.setShrinkAllColumns(true);
            tl.setColumnShrinkable(1, true);

            tl.addView(TableLayoutUtils.createRow(activity, R.string.type, e.getName(), tp));
            tl.addView(TableLayoutUtils.createRow(activity, R.string.id, "#" + e.getOsmId(), tp));
            tl.addView(TableLayoutUtils.createRow(activity, R.string.version, Long.toString(e.getOsmVersion()), tp));
            long timestamp = e.getTimestamp();
            if (timestamp > 0) {
                tl.addView(TableLayoutUtils.createRow(activity, R.string.last_edited,
                        new SimpleDateFormat(OsmParser.TIMESTAMP_FORMAT).format(timestamp * 1000L), tp));
            }

            if (e.getName().equals(Node.NAME)) {
                tl.addView(TableLayoutUtils.createRow(activity, R.string.location_lon_label, String.format(Locale.US, "%.7f", ((Node) e).getLon() / 1E7d) + "°",
                        tp));
                tl.addView(TableLayoutUtils.createRow(activity, R.string.location_lat_label, String.format(Locale.US, "%.7f", ((Node) e).getLat() / 1E7d) + "°",
                        tp));
            } else if (e.getName().equals(Way.NAME)) {
                tl.addView(TableLayoutUtils.divider(activity));
                boolean isClosed = ((Way) e).isClosed();
                tl.addView(TableLayoutUtils.createRow(activity, R.string.length_m, String.format(Locale.US, "%.2f", ((Way) e).length()), tp));
                tl.addView(TableLayoutUtils.createRow(activity, R.string.nodes, Integer.toString(((Way) e).nodeCount() + (isClosed ? -1 : 0)), tp));
                tl.addView(TableLayoutUtils.createRow(activity, R.string.closed, getString(isClosed ? R.string.yes : R.string.no), tp));
                // Make this expandable before enabling
                // for (Node n:((Way)e).getNodes()) {
                // tl.addView(createRow("", n.getDescription(),tp));
                // }
            } else if (e.getName().equals(Relation.NAME)) {
                tl.addView(TableLayoutUtils.divider(activity));
                List<RelationMember> members = ((Relation) e).getMembers();
                tl.addView(TableLayoutUtils.createRow(activity, R.string.members, Integer.toString(members != null ? members.size() : 0), tp));
                if (members != null) {
                    int notDownloaded = 0;
                    for (RelationMember rm : members) {
                        if (rm.getElement() == null) {
                            notDownloaded++;
                        }
                    }
                    if (notDownloaded > 0) {
                        tl.addView(TableLayoutUtils.createRow(activity, R.string.not_downloaded, Integer.toString(notDownloaded), tp));
                    }
                }
            }
            Validator validator = App.getDefaultValidator(getActivity());
            if (e.hasProblem(getActivity(), validator) != Validator.OK) {
                tl.addView(TableLayoutUtils.divider(activity));
                boolean first = true;
                for (String problem : validator.describeProblem(getActivity(), e)) {
                    String header = "";
                    if (first) {
                        header = getString(R.string.problem);
                        first = false;
                    }
                    tl.addView(TableLayoutUtils.createRow(activity, header, problem, tp));
                }
            }

            if (e.getTags() != null && e.getTags().size() > 0) {
                tl.addView(TableLayoutUtils.divider(activity));
                tl.addView(TableLayoutUtils.createRow(activity, R.string.menu_tags, null, tp));
                for (String k : e.getTags().keySet()) {
                    String value = e.getTags().get(k);
                    // special handling for some stuff
                    if (k.equals(Tags.KEY_WIKIPEDIA)) {
                        Log.d(DEBUG_TAG, Urls.WIKIPEDIA + encodeHttpPath(value));
                        tl.addView(TableLayoutUtils.createRow(activity, k,
                                Html.fromHtml("<a href=\"" + Urls.WIKIPEDIA + encodeHttpPath(value) + "\">" + value + "</a>"), tp));
                    } else if (k.equals(Tags.KEY_WIKIDATA)) {
                        tl.addView(TableLayoutUtils.createRow(activity, k,
                                Html.fromHtml("<a href=\"" + Urls.WIKIDATA + encodeHttpPath(value) + "\">" + value + "</a>"), tp));
                    } else if (Tags.isWebsiteKey(k)) {
                        try {
                            URL url = new URL(value);
                            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                            tl.addView(TableLayoutUtils.createRow(activity, k, Html.fromHtml("<a href=\"" + uri.toURL() + "\">" + value + "</a>"), tp));
                        } catch (MalformedURLException e1) {
                            Log.d(DEBUG_TAG, "Value " + value + " caused " + e);
                            tl.addView(TableLayoutUtils.createRow(activity, k, value, tp));
                        } catch (URISyntaxException e1) {
                            Log.d(DEBUG_TAG, "Value " + value + " caused " + e);
                            tl.addView(TableLayoutUtils.createRow(activity, k, value, tp));
                        }
                    } else {
                        tl.addView(TableLayoutUtils.createRow(activity, k, value, tp));
                    }
                }
            }

            if (e.getParentRelations() != null && !e.getParentRelations().isEmpty()) {
                tl.addView(TableLayoutUtils.divider(activity));
                tl.addView(TableLayoutUtils.createRow(activity, R.string.relation_membership, null, tp));
                for (Relation r : e.getParentRelations()) {
                    RelationMember rm = r.getMember(e);
                    if (rm != null) {
                        String role = rm.getRole();
                        tl.addView(TableLayoutUtils.createRow(activity, role.equals("") ? getString(R.string.empty_role) : role, r.getDescription(), tp));
                    } else {
                        // inconsistent state
                        String message = "inconsistent state: " + e.getDescription() + " is not a member of " + r;
                        Log.d(DEBUG_TAG, message);
                        ACRA.getErrorReporter().putCustomData("CAUSE", message);
                        ACRA.getErrorReporter().putCustomData("STATUS", "NOCRASH");
                        ACRA.getErrorReporter().handleException(null);
                    }
                }
            }
        }
        getDialog().setTitle(R.string.element_information);
        return sv;
    }

    private String encodeHttpPath(String path) {
        try {
            return URLEncoder.encode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.d(DEBUG_TAG, "Path " + path + " caused " + e);
            return "";
        }
    }
}
