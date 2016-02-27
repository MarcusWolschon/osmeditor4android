package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.osm.RelationMemberDescription;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;

public class RelationMembersFragment extends SherlockFragment {
	private static final String DEBUG_TAG = RelationMembersFragment.class.getSimpleName();
	

	private LayoutInflater inflater = null;


	private ArrayList<RelationMemberDescription> savedMembers = null;
	private long id = -1;

	static MemberSelectedActionModeCallback memberSelectedActionModeCallback = null;
	
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
			for (RelationMemberDescription rmd :  members) {
				insertNewMember(membersVerticalLayout, members.indexOf(rmd) +"", rmd, -1);
			}
		}
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
	protected RelationMemberRow insertNewMember(final LinearLayout membersVerticalLayout, final String pos, final RelationMemberDescription rmd, final int position) {
		RelationMemberRow row = null; 
		
		if (rmd.downloaded()) {
			row = (RelationMemberRow)inflater.inflate(R.layout.relation_member_downloaded_row, null);
		} else {
			row = (RelationMemberRow)inflater.inflate(R.layout.relation_member_row, null);
		}
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) { // stop Hint from wrapping
			row.roleEdit.setEllipsize(TruncateAt.END);
		}
		
		row.setValues(pos, id, rmd);
		membersVerticalLayout.addView(row, (position == -1) ? membersVerticalLayout.getChildCount() : position);
		
		row.selected.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				if (isChecked) {
					memberSelected();
				} else {
					memberDeselected();
				}
			}
		});
		
		return row;
	}
	
	/**
	 * A row representing an editable member of a relation, consisting of edits for role and display of other values and a delete button.
	 */
	public static class RelationMemberRow extends LinearLayout {
		
		private PropertyEditor owner;
		private long elementId;
		private long relationId;
		private CheckBox selected;
		private AutoCompleteTextView roleEdit;
		private ArrayAdapter<String> roleAdapter;
		private TextView typeView;
		private TextView elementView;
		
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
			
			typeView = (TextView)findViewById(R.id.memberType);
			
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
		}
				
		/**
		 * Sets the per row values for a relation member
		 * @param pos not used
		 * @param rmd the information on the relation member
		 * @return elationMemberRow object for convenience
		 */
		public RelationMemberRow setValues(String pos, long id, RelationMemberDescription rmd) {
			
			String desc = rmd.getDescription();
			String objectType = rmd.getType() == null ? "--" : rmd.getType();
			elementId = rmd.getRef();
			roleEdit.setText(rmd.getRole());
			typeView.setText(objectType);
			elementView.setText(desc);
			relationId = id;
			return this;
		}
		
		public long getOsmId() {
			return elementId;
		}
		
		public String getRole() {
			return roleEdit.getText().toString();
		}
		
		
		/**
		 * Deletes this row
		 */
		public void deleteRow() {
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
		
		public void deSelect() {
			selected.setChecked(false);
		}
		
		public void disableCheckBox() {
			selected.setEnabled(false);
		}
		
		protected void enableCheckBox() {
			selected.setEnabled(true);
		}
		
		protected ArrayAdapter<String> getMemberRoleAutocompleteAdapter() { // FIXME 
			// Use a set to prevent duplicate keys appearing
			Set<String> roles = new HashSet<String>();
			
			if (owner.tagEditorFragment != null) {		
				ArrayList<LinkedHashMap<String, String>> allTags = owner.tagEditorFragment.getUpdatedTags();
				if (allTags != null && allTags.size() > 0) {
					if ( owner.presets != null) { // 
						PresetItem relationPreset = Preset.findBestMatch(owner.presets,allTags.get(0));
						if (relationPreset != null) {
							roles.addAll(relationPreset.getRoles());
						}
					}
				}
			}
			
			List<String> result = new ArrayList<String>(roles);
			Collections.sort(result);
			roleAdapter = new ArrayAdapter<String>(owner, R.layout.autocomplete_row, result);
			
			return roleAdapter;
		}
	}

	protected synchronized void memberSelected() {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		if (memberSelectedActionModeCallback == null) {
			memberSelectedActionModeCallback = new MemberSelectedActionModeCallback(this, rowLayout);
			((SherlockFragmentActivity)getActivity()).startActionMode(memberSelectedActionModeCallback);
		}	
	}
	
	protected synchronized void memberDeselected() {
		if (memberSelectedActionModeCallback != null) {
			if (memberSelectedActionModeCallback.memberDeselected()) {
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
		abstract void handleRelationMember(final TextView typeView, final long elementId, final EditText roleEdit, final TextView descView);
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
			handler.handleRelationMember(row.typeView, row.elementId, row.roleEdit, row.elementView);
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
			public void handleRelationMember(final TextView typeView, final long elementId, final EditText roleEdit, final TextView descView) {
				String type = typeView.getText().toString().trim();
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
		}
		
		return false;
	}
	
	/**
	 * reload original arguments
	 */
	void doRevert() {
		loadMembers((ArrayList<RelationMemberDescription>)getArguments().getSerializable("members"));
	}
	
	void deselectHeaderCheckBox() {
		CheckBox headerCheckBox = (CheckBox) getView().findViewById(R.id.header_member_selected);
		headerCheckBox.setChecked(false);
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
