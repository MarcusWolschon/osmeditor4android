package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.Application;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMemberDescription;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;

public class RelationMembersFragment extends SherlockFragment implements
		PropertyRows {

	private static final String DEBUG_TAG = RelationMembersFragment.class.getSimpleName();
	

	private LayoutInflater inflater = null;


	private ArrayList<RelationMemberDescription> savedMembers = null;
	private long id = -1;

	static SelectedRowsActionModeCallback memberSelectedActionModeCallback = null;
	
	static enum Connected { NOT, UP, DOWN, BOTH, RING }
	
	/**
     */
    static public RelationMembersFragment newInstance(long id, ArrayList<RelationMemberDescription> members) {
    	RelationMembersFragment f = new RelationMembersFragment();

        Bundle args = new Bundle();
        args.putLong("id", id);
        args.putSerializable("members", members);

        f.setArguments(args);
        // f.setShowsDialog(true);
        
        return f;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(DEBUG_TAG, "onAttach");
//        try {
//            mListener = (OnPresetSelectedListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString() + " must implement OnPresetSelectedListener");
//        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       	Log.d(DEBUG_TAG, "onCreate");
        setHasOptionsMenu(true);
        getActivity().supportInvalidateOptionsMenu();
    }
	
	/** 
	 * display member elements of the relation if any
	 * @param members 
	 */
    @SuppressLint("InflateParams")
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
       	
     	this.inflater = inflater;
     	ScrollView relationMembersLayout = (ScrollView) inflater.inflate(R.layout.members_view, null);
		LinearLayout membersVerticalLayout = (LinearLayout) relationMembersLayout.findViewById(R.id.members_vertical_layout);
		// membersVerticalLayout.setSaveFromParentEnabled(false);
		membersVerticalLayout.setSaveEnabled(false);
		
		// if this is a relation get members
    	ArrayList<RelationMemberDescription> members;
    	if (savedInstanceState != null) {
    		Log.d(DEBUG_TAG,"Restoring from saved state");
    		id = savedInstanceState.getLong("ID"); 
    		members = (ArrayList<RelationMemberDescription>)savedInstanceState.getSerializable("MEMBERS"); 		
    	} else if (savedMembers != null) {
    		Log.d(DEBUG_TAG,"Restoring from instance variable");
    		members = savedMembers;
    	} else {
    		id = getArguments().getLong("id");
    		members = (ArrayList<RelationMemberDescription>)getArguments().getSerializable("members");
    	}
    	loadMembers(membersVerticalLayout,  members);
		
		CheckBox headerCheckBox = (CheckBox) relationMembersLayout.findViewById(R.id.header_member_selected);
		headerCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					selectAllMembers();
				} else {
					deselectAllMembers();
				}
			}
		});
		
		return relationMembersLayout;
	}

    /**
	 * Creates edits from a SortedMap containing tags (as sequential key-value pairs)
	 */
	protected void loadMembers(final ArrayList<RelationMemberDescription> members) {
		LinearLayout membersVerticalLayout = (LinearLayout) getOurView();
		loadMembers(membersVerticalLayout, members);
	}
	
	/**
	 * Creates edits from a SortedMap containing tags (as sequential key-value pairs)
	 */
	protected void loadMembers(LinearLayout membersVerticalLayout, final ArrayList<RelationMemberDescription> members) {
		membersVerticalLayout.removeAllViews();
		if (members != null && members.size() > 0) {
			for (int i = 0; i < members.size(); i++) {
				Connected c = Connected.NOT;
				RelationMemberDescription current = members.get(i);
				String currentType = current.getType();
				if (current.downloaded() && !Relation.NAME.equals(currentType)) {
					RelationMemberDescription prev = null;
					RelationMemberDescription next = null;
					int count = members.size();
					Relation r = (Relation) Application.getDelegator().getOsmElement(Relation.NAME, id);
					if (r.hasTag(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON)) {
						prev = members.get((i==0?count:i)-1);
						next = members.get((i+1)%(count-1));
					} else {
						prev = i-1 >= 0 ? members.get(i -1) : null;
						next = i + 1 < count ? members.get(i + 1) : null;
					}
					c = getConnection(prev, current, next);
				}
				insertNewMember(membersVerticalLayout, i +"", current, -1, c);
			}
		}
	}
	
	void resetIcons() {
		LinearLayout rowLayout = (LinearLayout) getOurView();

		int i = rowLayout.getChildCount();
		while (--i >= 0) { 
			resetIcon(rowLayout, (RelationMemberRow)rowLayout.getChildAt(i));
		}
	}
	
	void resetIcon(LinearLayout ll, RelationMemberRow row) {
		if (!row.getRelationMemberDescription().downloaded()) {
			return;
		}
		int pos = ll.indexOfChild(row);
		OsmElement e = Application.getDelegator().getOsmElement(row.getType(), row.getOsmId());
		RelationMemberDescription prev = null;
		RelationMemberDescription next = null;
		Relation r = (Relation) Application.getDelegator().getOsmElement(Relation.NAME, id);
		int rows = ll.getChildCount();
		if (r.hasTag(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON)) {
			prev = ((RelationMemberRow)ll.getChildAt((pos==0?rows:pos)-1)).getRelationMemberDescription();
			next = ((RelationMemberRow)ll.getChildAt((pos+1)%(rows-1))).getRelationMemberDescription();
		} else {
			prev = pos-1 >= 0 ? ((RelationMemberRow)ll.getChildAt(pos -1)).getRelationMemberDescription() : null;
			next = pos + 1 < rows ? ((RelationMemberRow)ll.getChildAt(pos + 1)).getRelationMemberDescription() : null;
		}
		RelationMemberDescription current = row.getRelationMemberDescription();
		Connected c = getConnection(prev, current, next);
		row.setIcon(getActivity(), current, c);
	}
	
	
	Connected getConnection(RelationMemberDescription previous, RelationMemberDescription current, RelationMemberDescription next) {
		Connected result = Connected.NOT;
		String currentType = current.getType();
		if (Way.NAME.equals(currentType)) {
			Way w = (Way) current.getElement();
			Node first = w.getFirstNode();
			Node last = w.getLastNode();
			if (previous != null && previous.downloaded()) {
				if (Way.NAME.equals(previous.getType())) {
					Node prevFirst = ((Way)previous.getElement()).getFirstNode();
					Node prevLast = ((Way)previous.getElement()).getLastNode();
					if (prevLast.equals(first) || prevFirst.equals(first) || prevLast.equals(last) ||  prevFirst.equals(last)) {
						result = Connected.UP;
					}
				} else {
					Node prevNode = (Node)previous.getElement();
					if (prevNode.equals(first) || prevNode.equals(last)) {
						result = Connected.UP;
					}
				}
			}
			if (next != null && next.downloaded()) {
				if (Way.NAME.equals(next.getType())) {
					Node nextFirst = ((Way)next.getElement()).getFirstNode();
					Node nextLast = ((Way)next.getElement()).getLastNode();
					if (nextLast.equals(first) || nextFirst.equals(first) || nextLast.equals(last) ||  nextFirst.equals(last)) {
						if (result == Connected.UP) {
							result = Connected.BOTH;
						} else {
							result = Connected.DOWN;
						}
					}
				} else {
					Node nextNode = (Node)next.getElement();
					if (nextNode.equals(first) || nextNode.equals(last)) {
						if (result == Connected.UP) {
							result = Connected.BOTH;
						} else {
							result = Connected.DOWN;
						}
					}
				}
			}
		} else {
			Node n = (Node) current.getElement();
			if (previous != null && Way.NAME.equals(previous.getType()) && previous.downloaded()) {
				if (((Way)previous.getElement()).getLastNode().equals(n) || ((Way)previous.getElement()).getFirstNode().equals(n)) {
					result = Connected.UP;
				}
			}
			if (next != null && Way.NAME.equals(next.getType()) && next.downloaded()) {
				if (((Way)next.getElement()).getLastNode().equals(n) || ((Way)next.getElement()).getFirstNode().equals(n)) {
					if (result == Connected.UP) {
						result = Connected.BOTH;
					} else {
						result = Connected.DOWN;
					}
				}
			}
		}
		return result;
	}
    
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	Log.d(DEBUG_TAG, "onSaveInstanceState");
    	outState.putLong("ID", id);
    	outState.putSerializable("MEMBERS", savedMembers);
    }
    
    
    @Override
    public void onPause() {
    	super.onPause();
    	Log.d(DEBUG_TAG, "onPause");
    	savedMembers  = getMembersList();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	Log.d(DEBUG_TAG, "onStop");
    }
    
    @Override
    public void onDestroyView() {
    	super.onDestroyView();
    	Log.d(DEBUG_TAG, "onDestroyView");
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.d(DEBUG_TAG, "onDestroy");
    }
	
	/**
	 * Insert a new row with a relation member
	 * @param pos (currently unused)
	 * @param rmd information on the relation member
	 * @param position the position where this should be inserted. set to -1 to insert at end, or 0 to insert at beginning.
	 * @returns The new RelationMemberRow.
	 */
	protected RelationMemberRow insertNewMember(final LinearLayout membersVerticalLayout, final String pos, final RelationMemberDescription rmd, final int position, final Connected c) {
		RelationMemberRow row = null; 
		
		if (rmd.downloaded()) {
			row = (RelationMemberRow)inflater.inflate(R.layout.relation_member_downloaded_row, membersVerticalLayout, false);
		} else {
			row = (RelationMemberRow)inflater.inflate(R.layout.relation_member_row, membersVerticalLayout, false);
		}
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) { // stop Hint from wrapping
			row.roleEdit.setEllipsize(TruncateAt.END);
		}
		
		row.setValues(getActivity(),pos, id, rmd, c);
		membersVerticalLayout.addView(row, (position == -1) ? membersVerticalLayout.getChildCount() : position);
		
		row.selected.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				if (isChecked) {
					memberSelected();
				} else {
					deselectRow();
				}
			}
		});
		
		return row;
	}
	
	/**
	 * A row representing an editable member of a relation, consisting of edits for role and display of other values and a delete button.
	 */
	public static class RelationMemberRow extends LinearLayout implements
			SelectedRowsActionModeCallback.Row {
		
		private PropertyEditor owner;
		private long relationId;
		private CheckBox selected;
		private AutoCompleteTextView roleEdit;
		private ImageView typeView;
		private TextView elementView;
		
		private RelationMemberDescription rmd;
		
		public RelationMemberRow(Context context) {
			super(context);
			owner = (PropertyEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
		}
		
		public RelationMemberRow(Context context, AttributeSet attrs) {
			super(context, attrs);
			owner = (PropertyEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
		}
		
//		public RelationMemberRow(Context context, AttributeSet attrs, int defStyle) {
//			super(context, attrs, defStyle);
//			owner = (TagEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
//		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			selected = (CheckBox) findViewById(R.id.member_selected);
			
			roleEdit = (AutoCompleteTextView)findViewById(R.id.editMemberRole);
			roleEdit.setOnKeyListener(owner.myKeyListener);
			//lastEditKey.setSingleLine(true);
			
			typeView = (ImageView)findViewById(R.id.memberType);
			
			elementView = (TextView)findViewById(R.id.memberObject);
			
			roleEdit.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						roleEdit.setAdapter(getMemberRoleAutocompleteAdapter());
						if (/*running &&*/ roleEdit.getText().length() == 0) roleEdit.showDropDown();
					}
				}
			});
			
			OnClickListener autocompleteOnClick = new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (v.hasFocus()) {
						((AutoCompleteTextView)v).showDropDown();
					}
				}
			};

			roleEdit.setOnClickListener(autocompleteOnClick);
			
			roleEdit.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					Log.d(DEBUG_TAG,"onItemClicked value");
					Object o = parent.getItemAtPosition(position);			
					if (o instanceof StringWithDescription) {
						roleEdit.setText(((StringWithDescription)o).getValue());
					} else if (o instanceof String) {
						roleEdit.setText((String)o);
					}
				}
			});
		}
				
		/**
		 * Sets the per row values for a relation member
		 * @param pos not used
		 * @param rmd the information on the relation member
		 * @return elationMemberRow object for convenience
		 */
		public RelationMemberRow setValues(Context ctx, String pos, long id, RelationMemberDescription rmd, Connected c) {
			
			String desc = rmd.getDescription();
			String objectType = rmd.getType() == null ? "--" : rmd.getType();
			this.rmd = rmd;
			roleEdit.setText(rmd.getRole());
			
			setIcon(ctx, rmd, c);
			typeView.setTag(objectType);
			elementView.setText(desc);
			relationId = id;
			return this;
		}
		
		public String getType() {
			return (String) typeView.getTag();
		}
		
		public RelationMemberDescription getRelationMemberDescription() {
			return rmd;
		}
		
		public void setIcon(Context ctx, RelationMemberDescription rmd, Connected c) {
			String objectType = rmd.getType() == null ? "--" : rmd.getType();
			if (rmd.downloaded()) {
				if (Node.NAME.equals(objectType)) {
					switch (c) {
					case NOT: typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx,R.attr.node_small)); break;
					case UP: typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx,R.attr.node_up)); break;
					case DOWN: typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx,R.attr.node_down)); break;
					case BOTH: 
					case RING: typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx,R.attr.node_both)); break; 
					}
				} else if (Way.NAME.equals(objectType)) {
					switch (c) {
					case NOT: typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx,R.attr.line_small)); break;
					case UP: typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx,R.attr.line_up)); break;
					case DOWN: typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx,R.attr.line_down)); break;
					case BOTH: 
					case RING: typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx,R.attr.line_both)); break;
					}
				} else if (Relation.NAME.equals(objectType)) {
					typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx,R.attr.relation_small));
				} else {
					// don't know yet
				}
			} else {
				if (Node.NAME.equals(objectType)) {
					typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx,R.attr.not_downloaded_node_small));
				} else if (Way.NAME.equals(objectType)) {
					typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx,R.attr.not_downloaded_line_small));
				} else if (Relation.NAME.equals(objectType)) {
					typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx,R.attr.not_downloaded_line_small));
				} else {
					// don't know yet
				}
			}	
		}
		
		public long getOsmId() {
			return rmd.getRef();
		}
		
		public String getRole() {
			return roleEdit.getText().toString();
		}
		
		
		/**
		 * Deletes this row
		 */
		@Override
		public void delete() {
			if (owner != null) {
				View cf = owner.getCurrentFocus();
				if (cf == roleEdit) {
//					owner.focusRow(0); // FIXME focus is on this row 
				}
				LinearLayout membersVerticalLayout = (LinearLayout) owner.relationMembersFragment.getOurView();
				membersVerticalLayout.removeView(this);
				membersVerticalLayout.invalidate();
			} else {
				Log.d("PropertyEditor", "deleteRow owner null");
			}
		}
		
		/**
		 * Checks if the fields in this row are empty
		 * @return true if both fields are empty, false if at least one is filled
		 */
		public boolean isEmpty() {
			return  roleEdit.getText().toString().trim().equals("");
		}
		
		// return the status of the checkbox
		@Override
		public boolean isSelected() {
			return selected.isChecked();
		}
		
		@Override
		public void deselect() {
			selected.setChecked(false);
		}
		
		public void disableCheckBox() {
			selected.setEnabled(false);
		}
		
		protected void enableCheckBox() {
			selected.setEnabled(true);
		}
		
		protected ArrayAdapter<StringWithDescription> getMemberRoleAutocompleteAdapter() { // FIXME for multiselect
			// Use a set to prevent duplicate keys appearing
			Set<StringWithDescription> roles = new HashSet<StringWithDescription>();
			
			ArrayList<LinkedHashMap<String, String>> allTags = owner.getUpdatedTags();
			if (allTags != null && allTags.size() > 0) {
				if ( owner.presets != null) { // 
					PresetItem relationPreset = Preset.findBestMatch(owner.presets,allTags.get(0));
					if (relationPreset != null && relationPreset.getRoles() != null) {
						roles.addAll(relationPreset.getRoles());
					}
				}
			}
			
			List<StringWithDescription> result = new ArrayList<StringWithDescription>(roles);
			Collections.sort(result);
			return new ArrayAdapter<StringWithDescription>(owner, R.layout.autocomplete_row, result);
		}
	}

	protected synchronized void memberSelected() {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		if (memberSelectedActionModeCallback == null) {
			memberSelectedActionModeCallback = new RelationMemberSelectedActionModeCallback(this, rowLayout);
			((SherlockFragmentActivity)getActivity()).startActionMode(memberSelectedActionModeCallback);
		}	
	}
	
	@Override
	public synchronized void deselectRow() {
		if (memberSelectedActionModeCallback != null) {
			if (memberSelectedActionModeCallback.rowsDeselected(true)) {
				memberSelectedActionModeCallback = null;
			}
		}	
	}
	
	protected void selectAllMembers() {
		LinearLayout rowLayout = (LinearLayout) getOurView();

		int i = rowLayout.getChildCount();
		while (--i >= 0) { 
			RelationMemberRow row = (RelationMemberRow)rowLayout.getChildAt(i);
			if (row.selected.isEnabled()) {
				row.selected.setChecked(true);
			}
		}
	}

	protected void deselectAllMembers() {
		LinearLayout rowLayout = (LinearLayout) getOurView();

		int i = rowLayout.getChildCount();
		while (--i >= 0) { 
			RelationMemberRow row = (RelationMemberRow)rowLayout.getChildAt(i);
			if (row.selected.isEnabled()) {
				row.selected.setChecked(false);
			}
		}
	}
	
	/**
	 */
	private interface RelationMemberHandler {
		abstract void handleRelationMember(final ImageView typeView, final long elementId, final EditText roleEdit, final TextView descView);
	}
	
	/**
	 * Perform some processing for each row in the relation members view.
	 * @param handler The handler that will be called for each rowr.
	 */

	private void processRelationMembers(final RelationMemberHandler handler) {
		LinearLayout relationMembersLayout = (LinearLayout) getOurView();
		final int size = relationMembersLayout.getChildCount();
		for (int i = 0; i < size; ++i) { // -> avoid header 
			View view = relationMembersLayout.getChildAt(i);
			RelationMemberRow row = (RelationMemberRow)view;
			handler.handleRelationMember(row.typeView, row.rmd.getRef(), row.roleEdit, row.elementView);
		}
	}
	
	/**
	 * Collect all interesting values from the relation member view 
	 * RelationMemberDescritption is an extended version of RelationMember that holds a textual description of the element 
	 * instead of the element itself
	 * 
	 * @return ArrayList<RelationMemberDescription>.
	 */
	ArrayList<RelationMemberDescription> getMembersList() {
		final ArrayList<RelationMemberDescription> members = new ArrayList<RelationMemberDescription>();
		processRelationMembers(new RelationMemberHandler() {
			@Override
			public void handleRelationMember(final ImageView typeView, final long elementId, final EditText roleEdit, final TextView descView) {
				String type = ((String)typeView.getTag()).trim();
				String role = roleEdit.getText().toString().trim();
				String desc = descView.getText().toString().trim();
				RelationMemberDescription rmd = new RelationMemberDescription(type,elementId,role,desc);
				members.add(rmd);
			}
		});
		return members;
	}	
	
	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		// final MenuInflater inflater = getSupportMenuInflater();
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.members_menu, menu);
	}
	
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		// disable address tagging for stuff that won't have an address
		// menu.findItem(R.id.tag_menu_address).setVisible(!type.equals(Way.NAME) || element.hasTagKey(Tags.KEY_BUILDING));
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			((PropertyEditor)getActivity()).sendResultAndFinish();
			return true;
		case R.id.tag_menu_revert:
			doRevert();
			return true;
		case R.id.tag_menu_help:
			HelpViewer.start(getActivity(), R.string.help_propertyeditor);
			return true;
		case R.id.tag_menu_top:
		case R.id.tag_menu_bottom:
			scrollToRow(null,item.getItemId()==R.id.tag_menu_top,false);
			return true;
		}
		
		return false;
	}
	
	/**
	 * reload original arguments
	 */
	void doRevert() {
		loadMembers((ArrayList<RelationMemberDescription>)getArguments().getSerializable("members"));
	}
	
	@Override
	public void deselectHeaderCheckBox() {
		CheckBox headerCheckBox = (CheckBox) getView().findViewById(R.id.header_member_selected);
		headerCheckBox.setChecked(false);
	}
	
	public void scrollToRow(final RelationMemberRow row,final boolean up, boolean force) {	
		final View sv = getView();
		Rect scrollBounds = new Rect();
		sv.getHitRect(scrollBounds);
		if (row != null && row.getLocalVisibleRect(scrollBounds)&& !force) {
			return; // already on screen
		} 
		if (row==null) {
			new Handler().post(new Runnable() {
				@Override
				public void run() {
					if (sv != null && sv instanceof ScrollView) { // should always be the case
						((ScrollView)sv).fullScroll(up ? ScrollView.FOCUS_UP : ScrollView.FOCUS_DOWN);
					}
				}
			});
		} else {
			new Handler().post(new Runnable() {
				@Override
				public void run() {
					if (sv != null && sv instanceof ScrollView) { // should always be the case
						((ScrollView)sv).scrollTo(0, up ? row.getTop(): row.getBottom());
					}
				}
			});
		}
	}
	
	/**
	 * Return the view we have our rows in and work around some android craziness
	 * @return
	 */
	public View getOurView() {
		// android.support.v4.app.NoSaveStateFrameLayout
		View v =  getView();	
		if (v != null) {
			if ( v.getId() == R.id.members_vertical_layout) {
				Log.d(DEBUG_TAG,"got correct view in getView");
				return v;
			} else {
				v = v.findViewById(R.id.members_vertical_layout);
				if (v == null) {
					Log.d(DEBUG_TAG,"didn't find R.id.members_vertical_layout");
				}  else {
					Log.d(DEBUG_TAG,"Found members_vertical_layout");
				}
				return v;
			}
		} else {
			Log.d(DEBUG_TAG,"got null view in getView");
		}
		return null;
	}
}
