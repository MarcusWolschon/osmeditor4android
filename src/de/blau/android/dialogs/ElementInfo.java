package de.blau.android.dialogs;

import java.util.List;
import java.util.Locale;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;

/**
 * Very simple dialog fragment to display some info on an OSM element
 * @author simon
 *
 */
public class ElementInfo extends DialogFragment {
	
	private static final String DEBUG_TAG = ElementInfo.class.getName();
	
	private static final String TAG = "fragment_element_info";

	private static final int FIRST_CELL_WIDTH = 5;
	
	static public void showDialog(FragmentActivity activity, OsmElement e) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
		ElementInfo elementInfoFragment = newInstance(e);
	    if (elementInfoFragment != null) {
	    	elementInfoFragment.show(fm, TAG);
	    } else {
	    	Log.e(DEBUG_TAG,"Unable to create dialog for value " + e.getDescription());
	    }
	}
	
	static public void dismissDialog(FragmentActivity activity) {
		FragmentManager fm = activity.getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
	    Fragment fragment = fm.findFragmentByTag(TAG);
	    if (fragment != null) {
	        ft.remove(fragment);
	    }
	    ft.commit();
	}
	
    /**
     */
    static public ElementInfo newInstance(OsmElement e) {
    	ElementInfo f = new ElementInfo();

        Bundle args = new Bundle();
        args.putSerializable("element", e);

        f.setArguments(args);
        f.setShowsDialog(true);
        
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Preferences prefs = new Preferences(getActivity());
		if (prefs.lightThemeEnabled()) {
			setStyle(DialogFragment.STYLE_NORMAL,R.style.Theme_DialogLight);
		} else {
			setStyle(DialogFragment.STYLE_NORMAL,R.style.Theme_DialogDark);
		}
    }

    @SuppressWarnings("deprecation")
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ScrollView sv = (ScrollView) inflater.inflate(R.layout.element_info_view, container, false);
        TableLayout tl =  (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);
       
        OsmElement e = (OsmElement) getArguments().getSerializable("element");
        
        TableLayout.LayoutParams tp=
        		  new TableLayout.LayoutParams
        		  (TableLayout.LayoutParams.FILL_PARENT,TableLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(10, 2, 10, 2);
       
        if (e != null) {
        	// tl.setShrinkAllColumns(true);
        	tl.setColumnShrinkable(1, true);
        	
        	tl.addView(createRow(R.string.type,e.getName(),tp));
        	tl.addView(createRow(R.string.id,"#" + e.getOsmId(),tp));
        	tl.addView(createRow(R.string.version,"" + e.getOsmVersion(),tp));
        	
        	if (e.getName().equals(Node.NAME)) {
        		tl.addView(createRow(R.string.location_lon_label, String.format(Locale.US,"%.7f", ((Node)e).getLon()/1E7d) + "°",tp));
        		tl.addView(createRow(R.string.location_lat_label, String.format(Locale.US,"%.7f", ((Node)e).getLat()/1E7d) + "°",tp));
        	} else if (e.getName().equals(Way.NAME)) {
        		tl.addView(divider());
        		boolean isClosed = ((Way)e).isClosed();
        		tl.addView(createRow(R.string.length_m, String.format(Locale.US,"%.2f",((Way)e).length()),tp));
        		tl.addView(createRow(R.string.nodes, "" + (((Way)e).nodeCount() + (isClosed?-1:0)),tp));       		
        		tl.addView(createRow(R.string.closed, getString(isClosed ? R.string.yes : R.string.no),tp));
 //       		Make this expandable before enabling
 //       		for (Node n:((Way)e).getNodes()) {
 //       			tl.addView(createRow("", "" + n.getDescription(),tp));
 //       		}
        	} else if (e.getName().equals(Relation.NAME)) {
        		tl.addView(divider());
        		List<RelationMember> members = ((Relation)e).getMembers();
        		tl.addView(createRow(R.string.members, "" + (members != null ? members.size() : 0),tp));
        		if (members != null) {
        			int notDownloaded = 0;
        			for (RelationMember rm:members) {
        				if (rm.getElement()==null) {
        					notDownloaded++;
        				}
        			}
        			if (notDownloaded > 0) {
        				tl.addView(createRow(R.string.not_downloaded, "" + notDownloaded,tp));
        			}
        		}
        	}
        	if (e.hasProblem(getActivity())) {
        		tl.addView(divider());
        		tl.addView(createRow(R.string.problem,e.describeProblem(),tp));
        	}
        	
        	if (e.getTags() != null && e.getTags().size() > 0) {
        		tl.addView(divider());
        		tl.addView(createRow(R.string.menu_tags,null,tp));
        		for (String k:e.getTags().keySet()) {
        			String value = e.getTags().get(k);
        			// special handling for some stuff
        			if (k.equals(Tags.KEY_WIKIPEDIA)) {
        				tl.addView(createRow(k, Html.fromHtml("<a href=\"http://wikipedia.org/wiki/"+value+"\">"+value+"</a>"),tp));
        			} else if (k.equals(Tags.KEY_WIKIDATA)) {
        				tl.addView(createRow(k, Html.fromHtml("<a href=\"http://wikidata.org/wiki/"+value+"\">"+value+"</a>"),tp));
        			} else if (Tags.isWebsiteKey(k)) {
        				tl.addView(createRow(k, Html.fromHtml("<a href=\"" + value + "\">"+value+"</a>"),tp));
        			} else {
        				tl.addView(createRow(k,value,tp));
        			}
        		}
        	}
        	
        	if (e.getParentRelations() != null && e.getParentRelations().size() > 0) {
        		tl.addView(divider());
        		tl.addView(createRow(R.string.relation_membership,null,tp));
        		for (Relation r:e.getParentRelations()) {
        			RelationMember rm = r.getMember(e);
        			if (rm != null) {
        				String role = rm.getRole();
        				tl.addView(createRow(role.equals("")?getString(R.string.empty_role):role,r.getDescription(),tp));
        			} else {
        				// inconsistent state
        				String message = "inconsistent state: " + e.getDescription() + " is not a member of " + r;
        				Log.d(DEBUG_TAG, message);
        				ACRA.getErrorReporter().putCustomData("CAUSE", message);
        				ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
						ACRA.getErrorReporter().handleException(null);
        			}
        		}
        	}
        }
        
        getDialog().setTitle(R.string.element_information);

        return sv;
    }
    
    @SuppressLint("NewApi")
	private TableRow createRow(String cell1, CharSequence cell2, TableLayout.LayoutParams tp) {
    	TableRow tr = new TableRow(getActivity());
    	TextView cell = new TextView(getActivity());
    	cell.setSingleLine();
    	cell.setText(cell1);
    	cell.setMinEms(FIRST_CELL_WIDTH);
    	if (cell2 == null) {
    		cell.setTypeface(null,Typeface.BOLD);
    	}
    	cell.setEllipsize(TruncateAt.MARQUEE);
    	tr.addView(cell);
    	cell = new TextView(getActivity());
    	if (cell2 != null) {
    		cell.setText(cell2);
    		cell.setMinEms(FIRST_CELL_WIDTH);
    		// cell.setHorizontallyScrolling(true);
    		// cell.setSingleLine(true);
    		cell.setEllipsize(TextUtils.TruncateAt.END);
    		Linkify.addLinks(cell,Linkify.WEB_URLS);
    		cell.setMovementMethod(LinkMovementMethod.getInstance());
    		cell.setPadding(5, 0, 0, 0);
    		cell.setEllipsize(TruncateAt.MARQUEE);
// This stops links from working   		
//    		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//    			cell.setTextIsSelectable(true);
//    		}
    		tr.addView(cell);
    	}
    	tr.setLayoutParams(tp);
    	return tr;
    }
    
    @SuppressLint("NewApi")
	private TableRow createRow(int cell1, CharSequence cell2, TableLayout.LayoutParams tp) {
    	TableRow tr = new TableRow(getActivity());
    	TextView cell = new TextView(getActivity());
    	cell.setMinEms(FIRST_CELL_WIDTH);
    	cell.setMaxLines(2);
    	cell.setText(cell1);
    	if (cell2 == null) {
    		cell.setTypeface(null,Typeface.BOLD);
    	} 
    	cell.setEllipsize(TruncateAt.MARQUEE);
    	tr.addView(cell);
    	cell = new TextView(getActivity());
    	if (cell2 != null) {
    		cell.setText(cell2);
    		cell.setMinEms(FIRST_CELL_WIDTH);
    		Linkify.addLinks(cell,Linkify.WEB_URLS);
    		cell.setMovementMethod(LinkMovementMethod.getInstance());
    		cell.setPadding(5, 0, 0, 0);
    		cell.setEllipsize(TruncateAt.MARQUEE);
// This stops links from working   		
//    		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//    			cell.setTextIsSelectable(true);
//    		}
    		tr.addView(cell);
    	}
    	tr.setLayoutParams(tp);
    	return tr;
    }
    
    @SuppressWarnings("deprecation")
	private View divider() {
    	TableRow tr = new TableRow(getActivity());
    	View v = new View(getActivity());
    	TableRow.LayoutParams trp = new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, 1);
    	trp.span = 2;
    	v.setLayoutParams(trp);
    	v.setBackgroundColor(Color.rgb(204, 204, 204));
    	tr.addView(v);
    	return tr;
    }
}
