package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
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
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.Density;
import de.blau.android.util.StringWithDescription;

public class RelationMembershipFragment extends BaseFragment implements
		PropertyRows,
		OnItemSelectedListener {
	
	private static final String DEBUG_TAG = RelationMembershipFragment.class.getSimpleName();
	
	private LayoutInflater inflater = null;

	private HashMap<Long, String> savedParents = null;

	private EditorUpdate tagListener = null;
	
	private static SelectedRowsActionModeCallback parentSelectedActionModeCallback = null;
	private static final Object actionModeCallbackLock = new Object();
	
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
    public void onAttachToContext(Context context) {
        Log.d(DEBUG_TAG, "onAttachToContext");
        try {
        	tagListener = (EditorUpdate) context;
        } catch (ClassCastException e) {
        	throw new ClassCastException(context.toString() + " must implement OnPresetSelectedListener");
        }
        setHasOptionsMenu(true);
        getActivity().supportInvalidateOptionsMenu();
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
    	parentRelationsLayout = (ScrollView) inflater.inflate(R.layout.membership_view, container, false);
		membershipVerticalLayout = (LinearLayout) parentRelationsLayout.findViewById(R.id.membership_vertical_layout);
		// membershipVerticalLayout.setSaveFromParentEnabled(false);
		membershipVerticalLayout.setSaveEnabled(false);
		
    	HashMap<Long,String> parents;
    	if (savedInstanceState != null) {
    		Log.d(DEBUG_TAG,"Restoring from saved state");
    		parents = (HashMap<Long, String>) savedInstanceState.getSerializable("PARENTS");
    	} else if (savedParents != null ) {
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
					selectAllRows();
				} else {
					deselectAllRows();
				}
			}
		});
		
       	// add some extra space for the FAB
		parentRelationsLayout.setPadding(0, 0, 0, Density.dpToPx(getResources(),64));
       	
		return parentRelationsLayout;
    }

    /**
	 * Creates edits from a SortedMap containing tags (as sequential key-value pairs)
	 */
	private void loadParents(final Map<Long, String> parents) {
		LinearLayout membershipVerticalLayout = (LinearLayout) getOurView();
		loadParents(membershipVerticalLayout, parents);
	}
	
	/**
	 * Creates edits from a SortedMap containing tags (as sequential key-value pairs)
	 */
	private void loadParents(LinearLayout membershipVerticalLayout, final Map<Long, String> parents) {
		membershipVerticalLayout.removeAllViews();
		if (parents != null && parents.size() > 0) {
			StorageDelegator storageDelegator = App.getDelegator();
			for (Long id :  parents.keySet()) {
				Relation r = (Relation) storageDelegator.getOsmElement(Relation.NAME, id.longValue());
				insertNewMembership(membershipVerticalLayout, parents.get(id),r,0, false);
			}
		} 
	}
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	Log.d(DEBUG_TAG, "onSaveInstanceState");
    	outState.putSerializable("PARENTS", savedParents);
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
	private RelationMembershipRow insertNewMembership(LinearLayout membershipVerticalLayout, final String role, final Relation r, final int position, boolean showSpinner) {
		RelationMembershipRow row = (RelationMembershipRow) inflater.inflate(R.layout.relation_membership_row, membershipVerticalLayout, false);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) { // stop Hint from wrapping
			row.roleEdit.setEllipsize(TruncateAt.END);
		}

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
					deselectRow();
				}
			}
		});
		
		return row;
	}
	
	/**
	 * A row representing a parent relation with an edit for role and further values and a delete button.
	 */
	public static class RelationMembershipRow extends LinearLayout implements
			SelectedRowsActionModeCallback.Row {
		
		private PropertyEditor owner;
		private long relationId =-1; // flag value for new relation memberships
		private CheckBox selected;
		private AutoCompleteTextView roleEdit;
		private Spinner parentEdit;
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
		
		ArrayAdapter<StringWithDescription> getMembershipRoleAutocompleteAdapter() {
			// Use a set to prevent duplicate keys appearing
			Set<StringWithDescription> roles = new HashSet<StringWithDescription>();
			Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, relationId);
			if ( r!= null) {			
				if ( owner.presets != null) {
					PresetItem relationPreset = Preset.findBestMatch(owner.presets,r.getTags());
					if (relationPreset != null) {
						List<StringWithDescription> presetRoles = relationPreset.getRoles();
						if (presetRoles != null) {
							roles.addAll(presetRoles);
						}
					}
				}
			}
			
			List<StringWithDescription> result = new ArrayList<StringWithDescription>(roles);
			Collections.sort(result);
			
			return new ArrayAdapter<StringWithDescription>(owner, R.layout.autocomplete_row, result);
		}
		
		ArrayAdapter<Relation> getRelationSpinnerAdapter() {
			//		
			List<Relation> result = App.getDelegator().getCurrentStorage().getRelations();
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
			parentEdit.setSelection(App.getDelegator().getCurrentStorage().getRelations().indexOf(r));
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
			parentEdit.setSelection(App.getDelegator().getCurrentStorage().getRelations().indexOf(r));
			Log.d(DEBUG_TAG, "Set parent relation to " + relationId + " " + r.getDescription());
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
		@Override
		public void delete() {
			if (owner != null) {
				View cf = owner.getCurrentFocus();
				if (cf == roleEdit) {
//					owner.focusRow(0); // FIXME focus is on this fragement
				}
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
	} // RelationMembershipRow
	    	
	private void parentSelected() {
		synchronized (actionModeCallbackLock) {
			LinearLayout rowLayout = (LinearLayout) getOurView();
			if (parentSelectedActionModeCallback == null) {
				parentSelectedActionModeCallback = new SelectedRowsActionModeCallback(this, rowLayout);
				((AppCompatActivity)getActivity()).startSupportActionMode(parentSelectedActionModeCallback);
			}	
		}
	}
	
	@Override
	public void deselectRow() {
		synchronized (actionModeCallbackLock) {
			if (parentSelectedActionModeCallback != null) {
				if (parentSelectedActionModeCallback.rowsDeselected(true)) {
					parentSelectedActionModeCallback = null;
				}
			}	
		}
	}
	
	@Override
	public void selectAllRows() { // select all parents
		LinearLayout rowLayout = (LinearLayout) getOurView();

		int i = rowLayout.getChildCount();
		while (--i >= 0) { 
			RelationMembershipRow row = (RelationMembershipRow)rowLayout.getChildAt(i);
			if (row.selected.isEnabled()) {
				row.selected.setChecked(true);
			}
		}
	}

	@Override
	public void deselectAllRows() { // // select all parents
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
	 */
	private interface ParentRelationHandler {
		void handleParentRelation(final EditText roleEdit, final long relationId);
	}
	
	/**
	 * Perform some processing for each row in the parent relation view.
	 * @param handler The handler that will be called for each row.
	 */
	private void processParentRelations(final ParentRelationHandler handler) {
		LinearLayout membershipVerticalLayout = (LinearLayout) getOurView();
		if (membershipVerticalLayout == null) {
			Log.e(DEBUG_TAG,"unable to process parent relations");
			return;
		}
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
    	Log.d(DEBUG_TAG, ((Relation)parent.getItemAtPosition(pos)).getDescription());
    	if (view != null) {
    		ViewParent pv = view.getParent();
    		while (!(pv instanceof RelationMembershipRow)) {
    			pv = pv.getParent();
    		}
    		((RelationMembershipRow)pv).setRelation((Relation)parent.getItemAtPosition(pos));
    	} else {
    		Log.d(DEBUG_TAG, "onItemselected view is null");    	
    	}
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
		case R.id.tag_menu_select_all:
			selectAllRows();
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
		loadParents((HashMap<Long,String>) getArguments().getSerializable("parents"));
	}
	
	/**
	 * Add this object to an existing relation
	 */
	private void addToRelation() {
		insertNewMembership((LinearLayout) getOurView(), null,null,-1, true).roleEdit.requestFocus();
	}
	
	@Override
	public void deselectHeaderCheckBox() {
		CheckBox headerCheckBox = (CheckBox) getView().findViewById(R.id.header_membership_selected);
		headerCheckBox.setChecked(false);
	}
	
	/**
	 * Return the view we have our rows in and work around some android craziness
	 * @return
	 */
	private View getOurView() {
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
