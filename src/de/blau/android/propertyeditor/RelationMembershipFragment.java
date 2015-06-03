package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.HelpViewer;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.Relation;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;

public class RelationMembershipFragment extends SherlockFragment implements OnItemSelectedListener {
	
	private static final String DEBUG_TAG = RelationMembershipFragment.class.getName();
	
	private LayoutInflater inflater = null;

	private HashMap<Long, String> savedParents = null;
	
	static ParentSelectedActionModeCallback parentSelectedActionModeCallback = null;
	
	/**
     */
    static public RelationMembershipFragment newInstance(HashMap<Long,String> parents) {
    	RelationMembershipFragment f = new RelationMembershipFragment();

        Bundle args = new Bundle();
        args.putSerializable("parents", parents);

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
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	ScrollView parentRelationsLayout = null;
    	LinearLayout membershipVerticalLayout = null;

    	// Inflate the layout for this fragment
    	this.inflater = inflater;
    	parentRelationsLayout = (ScrollView) inflater.inflate(R.layout.membership_view,null);
		membershipVerticalLayout = (LinearLayout) parentRelationsLayout.findViewById(R.id.membership_vertical_layout);
		
    	HashMap<Long,String> parents;
    	if (savedParents != null ) {
    		Log.d(DEBUG_TAG,"Restoring from instance variable");
    		parents = savedParents;
    	} else {
    		parents = (HashMap<Long,String>) getArguments().getSerializable("parents");
    	}
    
    	loadParents(membershipVerticalLayout, parents);

		CheckBox headerCheckBox = (CheckBox) parentRelationsLayout.findViewById(R.id.header_membership_selected);
		headerCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					selectAllParents();
				} else {
					deselectAllParents();
				}
			}
		});
		
		return parentRelationsLayout;
    }

    /**
	 * Creates edits from a SortedMap containing tags (as sequential key-value pairs)
	 */
	protected void loadParents(final Map<Long,String> parents) {
		LinearLayout membershipVerticalLayout = (LinearLayout) getOurView();
		loadParents(membershipVerticalLayout, parents);
	}
	
	/**
	 * Creates edits from a SortedMap containing tags (as sequential key-value pairs)
	 */
	protected void loadParents(LinearLayout membershipVerticalLayout, final Map<Long,String> parents) {
		membershipVerticalLayout.removeAllViews();
		if (parents != null && parents.size() > 0) {
			for (Long id :  parents.keySet()) {
				Relation r = (Relation) Main.getLogic().getDelegator().getOsmElement(Relation.NAME, id.longValue());
				insertNewMembership(membershipVerticalLayout, parents.get(id),r,0, false);
			}
		}
	}
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	Log.d(DEBUG_TAG, "onSaveInstanceState");
    }
    
    
    @Override
    public void onPause() {
    	super.onPause();
    	Log.d(DEBUG_TAG, "onPause");
    	savedParents  = getParentRelationMap();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	Log.d(DEBUG_TAG, "onStop");
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.d(DEBUG_TAG, "onDestroy");
    }
	
	/**
	 * Insert a new row with a parent relation 
	 * 
	 * @param role		role of this element in the relation
	 * @param r			the relation
	 * @param position the position where this should be inserted. set to -1 to insert at end, or 0 to insert at beginning.
	 * @param showSpinner TODO
	 * @return the new RelationMembershipRow
	 */
	protected RelationMembershipRow insertNewMembership(LinearLayout membershipVerticalLayout, final String role, final Relation r, final int position, boolean showSpinner) {
		RelationMembershipRow row = (RelationMembershipRow) inflater.inflate(R.layout.relation_membership_row, null);
		if (r != null) {
			row.setValues(role, r);
		}
		membershipVerticalLayout.addView(row, (position == -1) ? membershipVerticalLayout.getChildCount() : position);
		row.showSpinner = showSpinner;
		
		row.selected.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				if (isChecked) {
					parentSelected();
				} else {
					parentDeselected();
				}
			}
		});
		
		return row;
	}
	
	/**
	 * A row representing a parent relation with an edits for role and further values and a delete button.
	 */
	public static class RelationMembershipRow extends LinearLayout {
		
		private PropertyEditor owner;
		private long relationId =-1; // flag value for new relation memberships
		private CheckBox selected;
		private AutoCompleteTextView roleEdit;
		private Spinner parentEdit;
		private ArrayAdapter<String> roleAdapter; 
		public boolean showSpinner = false;
		
		public RelationMembershipRow(Context context) {
			super(context);
			owner = (PropertyEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
		}
		
		public RelationMembershipRow(Context context, AttributeSet attrs) {
			super(context, attrs);
			owner = (PropertyEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
		}
		
//		public RelationMembershipRow(Context context, AttributeSet attrs, int defStyle) {
//			super(context, attrs, defStyle);
//			owner = (TagEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
//		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			selected = (CheckBox) findViewById(R.id.parent_selected);
			
			roleEdit = (AutoCompleteTextView)findViewById(R.id.editRole);
			roleEdit.setOnKeyListener(owner.myKeyListener);
			
			parentEdit = (Spinner)findViewById(R.id.editParent);
			ArrayAdapter<Relation> a = getRelationSpinnerAdapter();
			// a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			parentEdit.setAdapter(a);
			parentEdit.setOnItemSelectedListener(owner.relationMembershipFragment);

			roleEdit.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						roleEdit.setAdapter(getMembershipRoleAutocompleteAdapter());
						if (/*running && */roleEdit.getText().length() == 0) roleEdit.showDropDown();
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
		
		protected ArrayAdapter<String> getMembershipRoleAutocompleteAdapter() {
			// Use a set to prevent duplicate keys appearing
			Set<String> roles = new HashSet<String>();
			Relation r = (Relation) Main.getLogic().getDelegator().getOsmElement(Relation.NAME, relationId);
			if ( r!= null) {			
				if ( owner.presets != null) {
					PresetItem relationPreset = Preset.findBestMatch(owner.presets,r.getTags());
					if (relationPreset != null) {
						roles.addAll(relationPreset.getRoles());
					}
				}
			}
			
			List<String> result = new ArrayList<String>(roles);
			Collections.sort(result);
			roleAdapter = new ArrayAdapter<String>(owner, R.layout.autocomplete_row, result);
			
			return roleAdapter;
		}
		
		protected ArrayAdapter<Relation> getRelationSpinnerAdapter() {
			//
			
			List<Relation> result = Main.getLogic().getDelegator().getCurrentStorage().getRelations();;
			// Collections.sort(result);
			return new ArrayAdapter<Relation>(owner, R.layout.autocomplete_row, result);
		}
		
		
		/**
		 * Sets key and value values
		 * @param aTagKey the key value to set
		 * @param aTagValue the value value to set
		 * @return the TagEditRow object for convenience
		 */
		public RelationMembershipRow setValues(String role, Relation r) {
			relationId = r.getOsmId();
			roleEdit.setText(role);
			parentEdit.setSelection(Main.getLogic().getDelegator().getCurrentStorage().getRelations().indexOf(r));
			return this;
		}
		
		/**
		 * Sets key and value values
		 * @param aTagKey the key value to set
		 * @param aTagValue the value value to set
		 * @return the TagEditRow object for convenience
		 */
		public RelationMembershipRow setRelation(Relation r) {
			relationId = r.getOsmId();
			parentEdit.setSelection(Main.getLogic().getDelegator().getCurrentStorage().getRelations().indexOf(r));
			Log.d("TagEditor", "Set parent relation to " + relationId + " " + r.getDescription());
			roleEdit.setAdapter(getMembershipRoleAutocompleteAdapter()); // update 
			return this;
		}
		
		public long getOsmId() {
			return relationId;
		}
	
		
		public String getRole() {
			return roleEdit.getText().toString();
		}
		
		
		/**
		 * Deletes this row
		 */
		public void deleteRow() {
			View cf = owner.getCurrentFocus();
			if (cf == roleEdit) {
//				owner.focusRow(0); // FIXME focus is on this row 
			}
			if (owner != null) {
				LinearLayout membershipVerticalLayout = (LinearLayout) owner.relationMembershipFragment.getOurView();
				membershipVerticalLayout.removeView(this);
				membershipVerticalLayout.invalidate();
			} else {
				Log.d("PropertyEditor", "deleteRow owner null");
			}
		}
		
		/**
		 * awlful hack to show spinner after insert
		 */
		@Override
		public void onWindowFocusChanged (boolean hasFocus) {
			super.onWindowFocusChanged(hasFocus);
			if (showSpinner) {
				parentEdit.performClick();
				showSpinner = false;
			}
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
	} // RelationMembershipRow
	    
	
	protected void parentSelected() {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		if (parentSelectedActionModeCallback == null) {
			parentSelectedActionModeCallback = new ParentSelectedActionModeCallback(this, rowLayout);
			((SherlockFragmentActivity)getActivity()).startActionMode(parentSelectedActionModeCallback);
		}	
	}
	
	protected void parentDeselected() {
		if (parentSelectedActionModeCallback != null) {
			if (parentSelectedActionModeCallback.parentDeselected()) {
				parentSelectedActionModeCallback = null;
			}
		}	
	}
	
	protected void selectAllParents() {
		LinearLayout rowLayout = (LinearLayout) getOurView();

		int i = rowLayout.getChildCount();
		while (--i >= 0) { 
			RelationMembershipRow row = (RelationMembershipRow)rowLayout.getChildAt(i);
			if (row.selected.isEnabled()) {
				row.selected.setChecked(true);
			}
		}
	}

	protected void deselectAllParents() {
		LinearLayout rowLayout = (LinearLayout) getOurView();

		int i = rowLayout.getChildCount();
		while (--i >= 0) { 
			RelationMembershipRow row = (RelationMembershipRow)rowLayout.getChildAt(i);
			if (row.selected.isEnabled()) {
				row.selected.setChecked(false);
			}
		}
	}
	
	/**
	 * Get possible roles from the preset
	 * @return
	 */
	protected ArrayAdapter<String> getMemberRoleAutocompleteAdapter() {
		// Use a set to prevent duplicate keys appearing
		Set<String> roles = new HashSet<String>();
				
		if (((PropertyEditor)getActivity()).presets != null && ((PropertyEditor)getActivity()).tagEditorFragment.autocompletePresetItem != null) {
			PresetItem relationPreset = Preset.findBestMatch(((PropertyEditor)getActivity()).presets,((PropertyEditor)getActivity()).tagEditorFragment.getKeyValueMapSingle(false)); // FIXME
			if (relationPreset != null) {
				roles.addAll(((PropertyEditor)getActivity()).tagEditorFragment.autocompletePresetItem.getRoles());
			}
		}

		List<String> result = new ArrayList<String>(roles);
		Collections.sort(result);
		return new ArrayAdapter<String>(getActivity(), R.layout.autocomplete_row, result);
	}
	
	/**
	 */
	private interface ParentRelationHandler {
		abstract void handleParentRelation(final EditText roleEdit, final long relationId);
	}
	
	/**
	 * Perform some processing for each row in the parent relation view.
	 * @param handler The handler that will be called for each row.
	 */
	private void processParentRelations(final ParentRelationHandler handler) {
		LinearLayout membershipVerticalLayout = (LinearLayout) getOurView();
		final int size = membershipVerticalLayout.getChildCount();
		for (int i = 0; i < size; ++i) { 
			View view = membershipVerticalLayout.getChildAt(i);
			RelationMembershipRow row = (RelationMembershipRow)view;
			handler.handleParentRelation(row.roleEdit, row.relationId);
		}
	}
    
	/**
	 * Collect all interesting values from the parent relation view HashMap<String,String>, currently only the role value
	 * 
	 * @return The HashMap<Long,String> of relation and role in that relation pairs.
	 */
	HashMap<Long,String> getParentRelationMap() {
		final HashMap<Long,String> parents = new HashMap<Long,String>();
		processParentRelations(new ParentRelationHandler() {
			@Override
			public void handleParentRelation(final EditText roleEdit, final long relationId) {
				String role = roleEdit.getText().toString().trim();
				parents.put(Long.valueOf(relationId), role);
			}
		});
		return parents;
	}	
	
    @Override
	public void onItemSelected(AdapterView<?> parent, View view, 
            int pos, long id) {
    	
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
    	Log.d("TagEditor", ((Relation)parent.getItemAtPosition(pos)).getDescription());
    	ViewParent pv = view.getParent();
    	while (!(pv instanceof RelationMembershipRow)) {
    		pv = pv.getParent();
    	}
    	((RelationMembershipRow)pv).setRelation((Relation)parent.getItemAtPosition(pos));	
    }

    @Override
	public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }
    
	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		// final MenuInflater inflater = getSupportMenuInflater();
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.membership_menu, menu);
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
		case R.id.tag_menu_addtorelation:
			addToRelation();
			return true;
		case R.id.tag_menu_help:
			Intent startHelpViewer = new Intent(getActivity(), HelpViewer.class);
			startHelpViewer.putExtra(HelpViewer.TOPIC, "TagEditor");
			startActivity(startHelpViewer);
			return true;
		}
		
		return false;
	}
    
	/**
	 * reload original arguments
	 */
	private void doRevert() {
		loadParents((HashMap<Long,String>) getArguments().getSerializable("parents"));
	}
	
	/**
	 * Add this object to an existing relation
	 */
	private void addToRelation() {
		insertNewMembership((LinearLayout) getOurView(), null,null,-1, true).roleEdit.requestFocus();
	}
	
	void deselectHeaderCheckBox() {
		CheckBox headerCheckBox = (CheckBox) getView().findViewById(R.id.header_membership_selected);
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
			if ( v.getId() == R.id.membership_vertical_layout) {
				Log.d(DEBUG_TAG,"got correct view in getView");
				return v;
			} else {
				v = v.findViewById(R.id.membership_vertical_layout);
				if (v == null) {
					Log.d(DEBUG_TAG,"didn't find R.id.membership_vertical_layout");
				}  else {
					Log.d(DEBUG_TAG,"Found R.id.membership_vertical_layoutt");
				}
				return v;
			}
		} else {
			Log.d(DEBUG_TAG,"got null view in getView");
		}
		return null;
	}
}
