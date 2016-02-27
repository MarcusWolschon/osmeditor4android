package de.blau.android;

import java.util.List;

import org.acra.ACRA;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
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

import com.actionbarsherlock.app.SherlockDialogFragment;

import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;

/**
 * Very simple dialog fragment to display some info on an OSM element
 * @author simon
 *
 */
public class ElementInfoFragment extends SherlockDialogFragment {
	
	private static final String DEBUG_TAG = ElementInfoFragment.class.getName();

    /**
     */
    static public ElementInfoFragment newInstance(OsmElement e) {
    	ElementInfoFragment f = new ElementInfoFragment();

        Bundle args = new Bundle();
        args.putSerializable("element", e);

        f.setArguments(args);
        f.setShowsDialog(true);
        
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

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
        	tl.setShrinkAllColumns(true);
        	
        	tl.addView(createRow(R.string.type,e.getName(),tp));
        	tl.addView(createRow(R.string.id,"#" + e.getOsmId(),tp));
        	tl.addView(createRow(R.string.version,"" + e.getOsmVersion(),tp));
        	
        	if (e.getName().equals(Node.NAME)) {
        		
        	} else if (e.getName().equals(Way.NAME)) {
        		tl.addView(divider());
        		boolean isClosed = ((Way)e).isClosed();
        		tl.addView(createRow(R.string.nodes, "" + (((Way)e).nodeCount() + (isClosed?-1:0)),tp));
        		tl.addView(createRow(R.string.length_m, String.format("%.2f",((Way)e).length()),tp));
        		tl.addView(createRow(R.string.closed, getString(isClosed ? R.string.yes : R.string.no),tp));
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
        				Log.d(DEBUG_TAG, "inconsistent state: " + e.getDescription() + " is not a member of " + r);
        				ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
						ACRA.getErrorReporter().handleException(null);
        			}
        		}
        	}
        }
        
        getDialog().setTitle(R.string.element_information);

        return sv;
    }
    
    private TableRow createRow(String cell1, CharSequence cell2, TableLayout.LayoutParams tp) {
    	TableRow tr = new TableRow(getActivity());
    	TextView cell = new TextView(getActivity());
    	cell.setText(cell1);
    	if (cell2 == null) {
    		cell.setTypeface(null,Typeface.BOLD);
    	}
    	cell.setEllipsize(TruncateAt.MARQUEE);
    	tr.addView(cell);
    	cell = new TextView(getActivity());
    	if (cell2 != null) {
    		cell.setText(cell2);
    		Linkify.addLinks(cell,Linkify.WEB_URLS);
    		cell.setMovementMethod(LinkMovementMethod.getInstance());
    		cell.setPadding(5, 0, 0, 0);
    		cell.setEllipsize(TruncateAt.MARQUEE);
    		tr.addView(cell);
    	}
    	tr.setLayoutParams(tp);
    	return tr;
    }
    
    private TableRow createRow(int cell1, CharSequence cell2, TableLayout.LayoutParams tp) {
    	TableRow tr = new TableRow(getActivity());
    	TextView cell = new TextView(getActivity());
    	cell.setText(cell1);
    	if (cell2 == null) {
    		cell.setTypeface(null,Typeface.BOLD);
    	}
    	cell.setEllipsize(TruncateAt.MARQUEE);
    	tr.addView(cell);
    	cell = new TextView(getActivity());
    	if (cell2 != null) {
    		cell.setText(cell2);
    		Linkify.addLinks(cell,Linkify.WEB_URLS);
    		cell.setMovementMethod(LinkMovementMethod.getInstance());
    		cell.setPadding(5, 0, 0, 0);
    		cell.setEllipsize(TruncateAt.MARQUEE);
    		tr.addView(cell);
    	}
    	tr.setLayoutParams(tp);
    	return tr;
    }
    
    private View divider() {
    	View v = new View(getActivity());
    	v.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, 1));
    	v.setBackgroundColor(Color.rgb(204, 204, 204));
    	return v;
    }
}
