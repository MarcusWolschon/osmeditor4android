package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.acra.ACRA;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import de.blau.android.Application;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.names.Names.TagMap;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMemberDescription;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.BugFixedAppCompatActivity;
import de.blau.android.util.PlaceTagValueAdapter;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.StreetTagValueAdapter;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.views.ExtendedViewPager;

/**
 * An Activity to edit OSM-Tags. Sends the edited Tags as Result to its caller-Activity (normally {@link Main}).
 * 
 * @author mb
 * @author simon
 */
public class PropertyEditor extends BugFixedAppCompatActivity implements 
		 OnPresetSelectedListener, EditorUpdate, FormUpdate, PresetFilterUpdate, NameAdapters, OnSaveListener {
	private static final String CURRENTITEM = "current_item";
	private static final String PANELAYOUT = "pane_layout";
	static final String PRESET_FRAGMENT = "preset_fragment";
	static final String RECENTPRESETS_FRAGMENT = "recentpresets_fragment";
	
	public static final String TAGEDIT_DATA = "dataClass";
	public static final String TAGEDIT_LAST_ADDRESS_TAGS = "applyLastTags";
	public static final String TAGEDIT_SHOW_PRESETS = "showPresets";
	public static final String TAGEDIT_ASK_FOR_NAME = "askForName";
	public static final String TAGEDIT_EXTRA_TAGS = "extra";

	
	/** The layout containing the edit rows */
	LinearLayout rowLayout = null;
	
	private PresetFragment presetFragment;
	int presetFragmentPosition = -1;
	
	private TagFormFragment tagFormFragment;
	int	tagFormFragmentPosition = -1;
	
	TagEditorFragment tagEditorFragment;
	int	tagEditorFragmentPosition = -1;

	RelationMembershipFragment relationMembershipFragment;
	RelationMembersFragment relationMembersFragment;
	RecentPresetsFragment recentPresetsFragment;
	
	PropertyEditorPagerAdapter  propertyEditorPagerAdapter;
	
	/**
	 * The tag we use for Android-logging.
	 */
	private static final String DEBUG_TAG = PropertyEditor.class.getSimpleName();
	
	private long osmIds[];
	
	private String types[];
	
	Preset[] presets = null;
	/**
	 * The OSM element for reference.
	 * DO NOT ATTEMPT TO MODIFY IT.
	 */
	OsmElement elements[];
	
	private PropertyEditorData[] loadData;
	
	private boolean applyLastAddressTags = false;
	private boolean showPresets = false;
	private boolean askForName = false;
	private HashMap<String,String> extraTags = null;
	
	/**
	 * Handles "enter" key presses.
	 */
	final OnKeyListener myKeyListener = new MyKeyListener();
	
	
	/**
	 * True while the activity is between onResume and onPause.
	 * Used to suppress autocomplete dropdowns while the activity is not running (showing them can lead to crashes).
	 * Needs to be static to be accessible in TagEditRow.
	 */
	static boolean running = false;
	
	/**
	 * Display form based editing
	 */
	private boolean formEnabled = false;
	
	/**
	 * Used both in the form and conventional tag editor fragments
	 */
	private StreetTagValueAdapter streetNameAutocompleteAdapter = null;
	private PlaceTagValueAdapter placeNameAutocompleteAdapter = null;
	
	/**
	 * 
	 */
	private ArrayList<LinkedHashMap<String, String>> originalTags;
	
	/**
	 * the same for relations
	 */
	private HashMap<Long,String> originalParents;
	private ArrayList<RelationMemberDescription> originalMembers;
	
	static final String COPIED_TAGS_FILE = "copiedtags.dat";
	 
	private SavingHelper<LinkedHashMap<String,String>> savingHelper
				= new SavingHelper<LinkedHashMap<String,String>>();
		
	private Preferences prefs = null;
	ExtendedViewPager    mViewPager;
	boolean usePaneLayout = false;
	boolean isRelation = false;

	public static void startForResult(@NonNull Activity activity,
									  @NonNull PropertyEditorData[] dataClass,
									  boolean applyLastTags,
									  boolean showPresets,
									  boolean askForName, 
									  HashMap<String,String> extraTags,
									  int requestCode) {
		Intent intent = new Intent(activity, PropertyEditor.class);
		intent.putExtra(TAGEDIT_DATA, dataClass);
		intent.putExtra(TAGEDIT_LAST_ADDRESS_TAGS, Boolean.valueOf(applyLastTags));
		intent.putExtra(TAGEDIT_SHOW_PRESETS, Boolean.valueOf(showPresets));
		intent.putExtra(TAGEDIT_ASK_FOR_NAME, Boolean.valueOf(askForName));
		intent.putExtra(TAGEDIT_EXTRA_TAGS, extraTags);
		activity.startActivityForResult(intent, requestCode);
	}
	
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		int currentItem = -1; // used when restoring
		prefs = new Preferences(this);
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.Theme_customTagEditor_Light);
		}
		
		super.onCreate(savedInstanceState);
		
		// requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		if (prefs.splitActionBarEnabled()) {
			// TODO determine if we ant to reinstate the bottom bar
		} 

		// tags
		if (savedInstanceState == null) {
			// No previous state to restore - get the state from the intent
			Log.d(DEBUG_TAG, "Initializing from intent");
			loadData = PropertyEditorData.deserializeArray(getIntent().getSerializableExtra(TAGEDIT_DATA));
			applyLastAddressTags = (Boolean)getIntent().getSerializableExtra(TAGEDIT_LAST_ADDRESS_TAGS); 
			showPresets = (Boolean)getIntent().getSerializableExtra(TAGEDIT_SHOW_PRESETS);
			askForName = (Boolean)getIntent().getSerializableExtra(TAGEDIT_ASK_FOR_NAME);
			extraTags = (HashMap<String,String>)getIntent().getSerializableExtra(TAGEDIT_EXTRA_TAGS);
			usePaneLayout = Util.isLandscape(this);
		} else {
			// Restore activity from saved state
			Log.d(DEBUG_TAG, "Restoring from savedInstanceState");
			loadData = PropertyEditorData.deserializeArray(getIntent().getSerializableExtra(TAGEDIT_DATA));
			currentItem = savedInstanceState.getInt(CURRENTITEM,-1);
			usePaneLayout = savedInstanceState.getBoolean(PANELAYOUT); //FIXME this disables layout changes on restarting
			
			Logic logic = Application.newLogic(); //
			StorageDelegator delegator = Application.getDelegator();
			if (!delegator.isDirty() && delegator.isEmpty()) { // this can't means: need to load state
				Log.d(DEBUG_TAG, "Loading saved state");
				logic.syncLoadFromFile(); // sync load
				Application.getTaskStorage().readFromFile(this);
			}
		}
				
		Log.d(DEBUG_TAG, "... done.");
		
		// sanity check
		StorageDelegator delegator = Application.getDelegator();
		if (delegator == null || loadData == null) {
			abort("Delegator null");
		}
		
		osmIds = new long[loadData.length];
		types = new String[loadData.length];
		elements = new OsmElement[loadData.length];

		for (int i=0;i<loadData.length;i++) {
			osmIds[i] = loadData[i].osmId;
			types[i] = loadData[i].type;
			elements[i] = delegator.getOsmElement(types[i], osmIds[i]);
			// and another sanity check
			if (elements[i] == null) {
				abort("Missing elements");
			}
		}
		
		presets = Application.getCurrentPresets(this);
		
		if (usePaneLayout) {
			setContentView(R.layout.pane_view);
			Log.d(DEBUG_TAG, "Using layout for large devices");
		} else {
			setContentView(R.layout.tab_view);	
		}
		
        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.propertyEditorBar);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        setSupportActionBar(toolbar);
        
		// tags
		ArrayList<LinkedHashMap<String, String>> tags = new ArrayList<LinkedHashMap<String, String>>();
		originalTags = new ArrayList<LinkedHashMap<String, String>>();
		for (int i=0;i<loadData.length;i++) {
			originalTags.add((LinkedHashMap<String, String>) (loadData[i].originalTags != null ? loadData[i].originalTags : loadData[i].tags));
			tags.add((LinkedHashMap<String, String>) loadData[i].tags);
		}
				
		if (loadData.length == 1) { // for now no support of relations and form for multi-select
			// parent relations
			originalParents = loadData[0].originalParents != null ? loadData[0].originalParents : loadData[0].parents;

			if (types[0].endsWith(Relation.NAME)) {
				// members of this relation
				originalMembers = loadData[0].originalMembers != null ? loadData[0].originalMembers : loadData[0].members;
				isRelation = true;
			}
			
			formEnabled = prefs.tagFormEnabled();
		}
		
		propertyEditorPagerAdapter =
                new PropertyEditorPagerAdapter(getSupportFragmentManager(),tags);
		mViewPager = (ExtendedViewPager) findViewById(R.id.pager);
		PagerTabStrip pagerTabStrip = (PagerTabStrip) mViewPager.findViewById(R.id.pager_header);
		pagerTabStrip.setDrawFullUnderline(true);
		pagerTabStrip.setTabIndicatorColor(ThemeUtils.getStyleAttribColorValue(this,R.attr.colorAccent,R.color.dark_grey));

		ActionBar actionbar = getSupportActionBar();
		actionbar.setDisplayShowTitleEnabled(false);
		actionbar.setDisplayHomeAsUpEnabled(true);
		
		if (usePaneLayout) { // add both preset fragments to panes
			Log.d(DEBUG_TAG,"Adding MRU prests");
			FragmentManager fm = getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			Fragment recentPresetsFragment = fm.findFragmentByTag(RECENTPRESETS_FRAGMENT);
			if (recentPresetsFragment != null) {
				ft.remove(recentPresetsFragment);
			}
			recentPresetsFragment = RecentPresetsFragment.newInstance(elements[0]); // FIXME collect tags
			ft.add(R.id.recent_preset_row,recentPresetsFragment,RECENTPRESETS_FRAGMENT);
			
			presetFragment = (PresetFragment) fm.findFragmentByTag(PRESET_FRAGMENT);
			if (presetFragment != null) {
				ft.remove(presetFragment);
			}
			presetFragment = PresetFragment.newInstance(elements[0],true); // FIXME collect tags
			ft.add(R.id.preset_row,presetFragment,PRESET_FRAGMENT);
			
			ft.commit();
			
			// this essentially has to be hardwired
			presetFragmentPosition = 0;
			if (formEnabled) {
				tagFormFragmentPosition = 0;
				tagEditorFragmentPosition = 1; // FIXME
			} else {
				tagFormFragmentPosition = 0;
				tagEditorFragmentPosition = 0; // FIXME
			}
		} else {
			presetFragmentPosition = 0;
			if (formEnabled) {
				tagFormFragmentPosition = 1;
				tagEditorFragmentPosition = 2; // FIXME
			} else {
				tagFormFragmentPosition = 1;
				tagEditorFragmentPosition = 1; // FIXME
			}
		}
		
		mViewPager.setOffscreenPageLimit(4); // FIXME currently this is required or else some of the logic between the fragments will not work
		mViewPager.setAdapter(propertyEditorPagerAdapter);
		mViewPager.addOnPageChangeListener(new PageChangeListener());
		mViewPager.setCurrentItem(currentItem != -1 ? currentItem : (showPresets ? presetFragmentPosition : (formEnabled ? tagFormFragmentPosition : tagEditorFragmentPosition)));
	}
	
	private void abort(String cause) {
		Toast.makeText(this, R.string.toast_inconsistent_state, Toast.LENGTH_LONG).show();
		ACRA.getErrorReporter().putCustomData("CAUSE",cause);
		ACRA.getErrorReporter().handleException(null);
		finish();
	}
	
	@Override
	protected void onStart() {
		Log.d(DEBUG_TAG,"onStart");
		super.onStart();
		Log.d(DEBUG_TAG,"onStart done");
	}
	
	@Override
	protected void onResume() {
		Log.d(DEBUG_TAG,"onResume");
		super.onResume();
		running = true;
		Address.loadLastAddresses(this);
		Log.d(DEBUG_TAG,"onResume done");
	}

	@Override
	protected void onDestroy() {
		Log.d(DEBUG_TAG,"onDestroy");
		super.onDestroy();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Due to a problem of not being able to intercept android.R.id.home in fragments on older android versions
		// we start passing the event to the currently displayed fragment.
		// REF: http://stackoverflow.com/questions/21938419/intercepting-actionbar-home-button-in-fragment	
		Fragment fragment = ((PropertyEditorPagerAdapter) mViewPager.getAdapter()).getItem(false,mViewPager.getCurrentItem());
		if (item.getItemId() == android.R.id.home && fragment != null && fragment.getView() != null && fragment.onOptionsItemSelected(item)) {
			Log.d(DEBUG_TAG,"called fragment onOptionsItemSelected");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
		
	public class PropertyEditorPagerAdapter extends FragmentPagerAdapter {
		
	    private ArrayList<LinkedHashMap<String, String>> tags;
	    private boolean restoring = false;
		
	    public PropertyEditorPagerAdapter(FragmentManager fm, ArrayList<LinkedHashMap<String, String>> tags) {
	        super(fm);
	        this.tags = tags;
	    }

	    @Override
	    public int getCount() {
	    	int pages = 0;
	    	if (loadData.length == 1) {
	    		if (types[0].endsWith(Relation.NAME)) {
	    			pages = 4;
	    		} else {
	    			pages = 3;
	    		}
	    		if (formEnabled) {
	    			pages++;
	    		}
	    	} else {
	    		pages = 2;
	    	}
	    	return usePaneLayout ? pages -1 : pages; // preset page not in pager
	    }

	    Fragment tagFormFragment(int position, boolean displayRecentPresets) {
			tagFormFragmentPosition = position;
			tagFormFragment = TagFormFragment.newInstance(displayRecentPresets, applyLastAddressTags, loadData[0].focusOnKey, askForName);
			return tagFormFragment;
	    }
	    
	    Fragment tagEditorFragment(int position, boolean displayRecentPresets) {
	    	tagEditorFragmentPosition = position;
	    	tagEditorFragment = TagEditorFragment.newInstance(elements, tags, applyLastAddressTags, loadData[0].focusOnKey, displayRecentPresets, extraTags);
			return tagEditorFragment;
	    }
	    
	    Fragment relationMembershipFragment() {
	    	if (loadData.length == 1) {
	    		relationMembershipFragment = RelationMembershipFragment.newInstance(loadData[0].parents);
				return relationMembershipFragment;
			}
	    	return null;
	    }
	    
	    Fragment relationMembersFragment() {
	    	if (loadData.length == 1 && types[0].endsWith(Relation.NAME)) {
				relationMembersFragment = RelationMembersFragment.newInstance(osmIds[0],loadData[0].members);
				return relationMembersFragment;
			}
	    	return null;
	    }
	    
	    @Override
	    public Fragment getItem(int position) {
	    	return getItem(true, position);
	    }
	    
	    public Fragment getItem(boolean instantiate, int position) {
	    	Log.d(DEBUG_TAG, "getItem " + instantiate + " " + position);
	    	if (formEnabled) {
	    		if (!usePaneLayout) {
	    			switch(position) {
	    			case 0: 
	    				if (instantiate) {
	    					presetFragment = PresetFragment.newInstance(elements[0], false); // 
	    				}
	    				return presetFragment;
	    			case 1: 		
	    				return instantiate ? tagFormFragment(position, true) : tagFormFragment;
	    			case 2: 		
	    				return instantiate ? tagEditorFragment(position, false) : tagEditorFragment;
	    			case 3:
	    				return isRelation ? (instantiate ? relationMembersFragment() : relationMembersFragment) : (instantiate ? relationMembershipFragment() : relationMembershipFragment);
	    			case 4:
	    				return instantiate ? relationMembershipFragment() : relationMembershipFragment;
	    			}
	    		} else {
	    			switch(position) {
	    			case 0: 		
	    				return instantiate ? tagFormFragment(position, false) : tagFormFragment;
	    			case 1: 		
	    				return instantiate ? tagEditorFragment(position, false) : tagEditorFragment;
	    			case 2:
	    				return isRelation ? (instantiate ? relationMembersFragment() : relationMembersFragment) : (instantiate ? relationMembershipFragment() : relationMembershipFragment);
	    			case 3:
	    				return instantiate ? relationMembershipFragment() : relationMembershipFragment;
	    			}
	    		}
	    	} else  {
	    		if (!usePaneLayout) {
	    			switch(position) {
	    			case 0: 
	    				if (instantiate) {
	    					presetFragment = PresetFragment.newInstance(elements[0], false); // 
	    				}
	    				return presetFragment;
	    			case 1: 		
	    				return instantiate ? tagEditorFragment(position, true) : tagEditorFragment;
	    			case 2:
	    				return isRelation ? (instantiate ? relationMembersFragment() : relationMembersFragment) : (instantiate ? relationMembershipFragment() : relationMembershipFragment);
	    			case 3:
	    				return instantiate ? relationMembershipFragment() : relationMembershipFragment;
	    			}
	    		} else {
	    			switch(position) {
	    			case 0: 		
	    				return instantiate ? tagEditorFragment(position, false) :tagEditorFragment;
	    			case 2:
	    				return isRelation ? (instantiate ? relationMembersFragment() : relationMembersFragment) : (instantiate ? relationMembershipFragment() : relationMembershipFragment);
	    			case 3:
	    				return instantiate ? relationMembershipFragment() : relationMembershipFragment;
	    			}
	    		}
	    	}
	    	return null;
	    }

	    @Override
	    public CharSequence getPageTitle(int position) {
	    	if (formEnabled) {
	    		if (!usePaneLayout) {
	    			switch(position) {
	    			case 0: return getString(R.string.tag_menu_preset);
	    			case 1: return getString(R.string.menu_tags);
	    			case 2: return getString(R.string.tag_details);
	    			case 3: return isRelation ? getString(R.string.members) : getString(R.string.relations);
	    			case 4: return getString(R.string.relations);
	    			}
	    		} else {
	    			switch(position) {
	    			case 0: return getString(R.string.menu_tags);
	    			case 1: return getString(R.string.tag_details);
	    			case 2: return isRelation ? getString(R.string.members) : getString(R.string.relations);
	    			case 3: return getString(R.string.relations);
	    			}
	    		}
	    	} else {
	    		if (!usePaneLayout) {
	    			switch(position) {
	    			case 0: return getString(R.string.tag_menu_preset);
	    			case 1: return getString(R.string.menu_tags);
	    			case 2: return isRelation ? getString(R.string.members) : getString(R.string.relations);
	    			case 3: return getString(R.string.relations);
	    			}
	    		} else {
	    			switch(position) {
	    			case 0: return getString(R.string.menu_tags);
	    			case 1: return isRelation ? getString(R.string.members) : getString(R.string.relations);
	    			case 2: return getString(R.string.relations);
	    			}
	    		}
	    	}
	    	return "error";
	    }
	    
	    @Override
	    public Object instantiateItem(ViewGroup container, int position) {
	        BaseFragment fragment = (BaseFragment) super.instantiateItem(container, position);
	        // update fragment refs here
	        if (fragment instanceof TagFormFragment) {
	        	tagFormFragment = (TagFormFragment) fragment;
	        	Log.d(DEBUG_TAG, "Restored ref to TagFormFragment");
	        } else if (fragment instanceof TagEditorFragment) {
	        	tagEditorFragment = (TagEditorFragment) fragment;
	        	Log.d(DEBUG_TAG, "Restored ref to TagEditorFragment");
	        } else if (fragment instanceof RelationMembershipFragment) {
	        	relationMembershipFragment = (RelationMembershipFragment) fragment;
	        	Log.d(DEBUG_TAG, "Restored ref to RelationMembershipFragment");
	        } else if (fragment instanceof RelationMembersFragment) {
	        	relationMembersFragment = (RelationMembersFragment) fragment;
	        	Log.d(DEBUG_TAG, "Restored ref to RelationMembersFragment");
	        } else if (fragment instanceof PresetFragment) {
	        	presetFragment = (PresetFragment) fragment;
	        	Log.d(DEBUG_TAG, "Restored ref to PresetFragment");
	        } else {
	        	Log.d(DEBUG_TAG, "Unknown fragment ...");
	        }
	        // hack to recreate the form ui when restoring as there is no callback that
	        // runs after the references here have been recreated
	        if (restoring && tagFormFragment != null && tagEditorFragment != null) {
	        	tagsUpdated();
	        }
	        return fragment;
	    }
	    
	    @Override
		public void restoreState (Parcelable state, ClassLoader loader) {
	    	Log.d(DEBUG_TAG, "restoreState");
	    	super.restoreState(state, loader);
	    	restoring = true;
	    	Log.d(DEBUG_TAG, "restoreState done");
	    }
	}
	
	class PageChangeListener extends ViewPager.SimpleOnPageChangeListener {
		@Override
		public void onPageSelected(int page) {
			Log.d(DEBUG_TAG,"page " + page + " selected");
			if (formEnabled && page == tagFormFragmentPosition && tagFormFragment != null) {
				tagFormFragment.update();
			}
		}
	}
	
	/**
	 * Removes an old RecentPresetView and replaces it by a new one (to update it)
	 */
	void recreateRecentPresetView() {
		if (usePaneLayout) {
			FragmentManager fm = getSupportFragmentManager();
			Fragment recentPresetsFragment = fm.findFragmentByTag(RECENTPRESETS_FRAGMENT);
			if (recentPresetsFragment != null) {
				((RecentPresetsFragment)recentPresetsFragment).recreateRecentPresetView();
			}
		} else {
			if (tagFormFragment != null) {
				tagFormFragment.recreateRecentPresetView();
			} else {
				tagEditorFragment.recreateRecentPresetView();
			}
		}
	}
	
	@Override
	public void onBackPressed() {
		// sendResultAndFinish();
		ArrayList<LinkedHashMap<String, String>> currentTags = tagEditorFragment.getUpdatedTags();
		HashMap<Long,String> currentParents = null;
		ArrayList<RelationMemberDescription> currentMembers = null;
		if (relationMembershipFragment != null) {
			currentParents = relationMembershipFragment.getParentRelationMap();
		}
		if (relationMembersFragment != null) {
			currentMembers = new ArrayList<RelationMemberDescription>(); // FIXME
			if (types[0].equals(Relation.NAME)) { // FIXME
				currentMembers = relationMembersFragment.getMembersList();
			}
		}
		// if we haven't edited just exit
		if (!same(currentTags,originalTags) // tags different 
				|| ((currentParents != null && !currentParents.equals(originalParents)) && !(originalParents==null && (currentParents == null || currentParents.size()==0))) // parents changed
				|| (elements[0] != null && elements[0].getName().equals(Relation.NAME) && (currentMembers != null && !sameMembers(currentMembers,originalMembers)))) {
			new AlertDialog.Builder(this)
			.setNeutralButton(R.string.cancel, null)
			.setNegativeButton(R.string.tag_menu_revert,        	
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					doRevert();
				}
			})
	        .setPositiveButton(R.string.tag_menu_exit_no_save, 
	        	new DialogInterface.OnClickListener() {
		            @Override
					public void onClick(DialogInterface arg0, int arg1) {
		                PropertyEditor.super.onBackPressed();
		            }
	        }).create().show();
		} else {
			PropertyEditor.super.onBackPressed();
		}
	}
	
	/*
	 * Revert changes in all fragments
	 */
	private void doRevert() {
		if (tagEditorFragment != null) {
			tagEditorFragment.doRevert();
		}
		if (relationMembershipFragment != null) {
			relationMembershipFragment.doRevert();
		}
		if (relationMembersFragment != null) {
			relationMembersFragment.doRevert();
		}
	}
		
	/**
	 * Get current values from the fragments and end the activity
	 */
	protected void sendResultAndFinish() {
		
		ArrayList<LinkedHashMap<String,String>> currentTags = tagEditorFragment.getUpdatedTags();
//		for (LinkedHashMap<String,String>map:currentTags) {
//			for (String k:map.keySet()) {
//				Log.d(DEBUG_TAG, "current key " + k + " " + map.get(k) );
//			}
//		}
//		for (LinkedHashMap<String,String>map:originalTags) {
//			for (String k:map.keySet()) {
//				Log.d(DEBUG_TAG, "original key " + k + " " + map.get(k) );
//			}
//		}
		
		// Save tags to our clipboard
		LinkedHashMap<String,String> copiedTags = tagEditorFragment.getCopiedTags();
		if (copiedTags != null) {
			savingHelper.save(this, COPIED_TAGS_FILE, copiedTags, false);
		}
		// save any address tags for "last address tags"
		if (currentTags != null && currentTags.size() == 1) {
			Address.updateLastAddresses(tagEditorFragment, Util.getArrayListMap(currentTags.get(0)));// FIXME
		}
		Intent intent = new Intent();
		
		HashMap<Long,String> currentParents = null;
		ArrayList<RelationMemberDescription> currentMembers = null;
		PropertyEditorData[] newData = new PropertyEditorData[currentTags.size()];
		if (currentTags != null && currentTags.size() == 1) { // normal single mode, relations might have changed
			currentParents = relationMembershipFragment.getParentRelationMap();
			currentMembers = new ArrayList<RelationMemberDescription>(); //FIXME
			if (types[0].endsWith(Relation.NAME)) {
				currentMembers = relationMembersFragment.getMembersList();
			}

			if (!same(currentTags, originalTags) || !(originalParents==null && currentParents.size()==0) && !currentParents.equals(originalParents) 
					|| (elements != null && elements[0].getName().equals(Relation.NAME) && !currentMembers.equals(originalMembers))) {
				// changes were made
				Log.d(DEBUG_TAG, "saving tags");
				for (int i=0;i<currentTags.size();i++) {
					newData[i] = new PropertyEditorData(osmIds[i], types[i], 
							currentTags.get(i).equals(originalTags.get(i)) ? null : currentTags.get(i),  null, 
									(originalParents==null && currentParents.size()==0) || currentParents.equals(originalParents)?null:currentParents, null, 
											currentMembers.equals(originalMembers)?null:currentMembers, null);
				}
			}
		} else { // multi select just tags could have been changed
			if (!same(currentTags, originalTags)) {
				// changes were made
				for (int i=0;i<currentTags.size();i++) {
					newData[i] = new PropertyEditorData(osmIds[i], types[i], 
							currentTags.get(i).equals(originalTags.get(i)) ? null : currentTags.get(i),  null, null, null, null, null);
				}		
			}
		}
		intent.putExtra(TAGEDIT_DATA, newData);
		setResult(RESULT_OK, intent);
		finish();
	}
	
	/**
	 * Check if two lists of tags are the same
	 * Note: this considers order relevant
	 * @return
	 */
	boolean same(ArrayList<LinkedHashMap<String,String>> tags1, ArrayList<LinkedHashMap<String,String>> tags2){
		// check for tag changes
		if (tags1.size() != tags2.size()) { /// serious error
			return false;
		}
		for (int i=0;i<tags1.size();i++) {
			if (!tags1.get(i).equals(tags2.get(i))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Check if two lists of RelationMembetDescription are the same
	 * Note: this considers order relevant
	 * @return
	 */
	boolean sameMembers(ArrayList<RelationMemberDescription> rmds1, ArrayList<RelationMemberDescription> rmds2){
		if (rmds1==null) {
			return rmds2==null;
		}
		if (rmds2==null) {
			return rmds1==null;
		}
		if (rmds1.size() != rmds2.size()) { /// serious error
			return false;
		}
		for (int i=0;i<rmds1.size();i++) {
			RelationMemberDescription rmd1 = rmds1.get(i);
			RelationMemberDescription rmd2 = rmds2.get(i);
			if (rmd1 == rmd2) {
				continue;
			}
			if (rmd1 != null && !rmd1.equals(rmd2)) {
				return false;
			}
		}
		return true;
	}
	
	/** Save the state of this activity instance for future restoration.
	 * @param outState The object to receive the saved state.
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		Log.d(DEBUG_TAG,"onSaveInstaceState");
		super.onSaveInstanceState(outState);
		// no call through. We restore our state from scratch, auto-restore messes up the already loaded edit fields.
		// outState.putSerializable(TAGEDIT_DATA, new PropertyEditorData(osmId, type, tagEditorFragment.getKeyValueMap(true), originalTags, relationMembershipFragment.getParentRelationMap(), originalParents, relationMembersFragment.getMembersList(), originalMembers));
		outState.putInt(CURRENTITEM, mViewPager.getCurrentItem());
		outState.putBoolean(PANELAYOUT, usePaneLayout);
	}
	
	/** When the Activity is interrupted, save MRUs and address cache*/
	@Override
	protected void onPause() {
		running = false;
		Preset[] presets = Application.getCurrentPresets(this);
		if (presets != null)  {
			for (Preset p:presets) {
				if (p!=null) {
					p.saveMRU();
				}
			}
		}
		Address.saveLastAddresses(this);
		super.onPause();
	}
	

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.d(DEBUG_TAG,"onRestoreInstanceState");
	    super.onRestoreInstanceState(savedInstanceState);
	    Log.d(DEBUG_TAG,"onRestoreInstanceState done");
	}
	
	/**
	 * Insert a new row of key+value -edit-widgets if some text is entered into the current one.
	 * 
	 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
	 */
	class MyKeyListener implements OnKeyListener {
		@Override
		public boolean onKey(final View view, final int keyCode, final KeyEvent keyEvent) {
			if (keyEvent.getAction() == KeyEvent.ACTION_UP || keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE) {
				if (view instanceof EditText) {
					//on Enter -> goto next EditText
					if (keyCode == KeyEvent.KEYCODE_ENTER) {
						View nextView = view.focusSearch(View.FOCUS_RIGHT);
						if (!(nextView instanceof EditText)) {
							nextView = view.focusSearch(View.FOCUS_LEFT);
							if (nextView != null) {
								nextView = nextView.focusSearch(View.FOCUS_DOWN);
							}
						}
						if (nextView != null && nextView instanceof EditText) {
							nextView.requestFocus();
							return true;
						}
					}
				}
			}
			return false;
		}
	}
	
	@Override
	public void onPresetSelected(PresetItem item) {
		onPresetSelected(item, false);
	}
	
	@Override
	public void onPresetSelected(PresetItem item, boolean applyOptional) {
		if (item != null) {
			tagEditorFragment.applyPreset(item, applyOptional, true);
			if (tagFormFragment != null) {
				tagFormFragment.update();
				mViewPager.setCurrentItem(tagFormFragmentPosition);
			} else {
				mViewPager.setCurrentItem(tagEditorFragmentPosition);
			}
			recreateRecentPresetView();
		}
	}
	
	/**
	 * Allow ViewPager to work
	 */
	public void enablePaging() {
		mViewPager.setPagingEnabled(true);
	}
	
	/**
	 * Disallow ViewPAger to work
	 */
	public void disablePaging() {
		mViewPager.setPagingEnabled(false);
	}
	
	/**
	 * Allow presets to be applied
	 */
	public void enablePresets() {
		if (usePaneLayout) {
			FragmentManager fm = getSupportFragmentManager();
			Fragment recentPresetsFragment = fm.findFragmentByTag(RECENTPRESETS_FRAGMENT);
			if (recentPresetsFragment != null) {
				((RecentPresetsFragment)recentPresetsFragment).enable();
			}
		} else {
			tagEditorFragment.enableRecentPresets();
		}
	}
	
	/**
	 * Disallow presets to be applied
	 */
	public void disablePresets() {
		if (usePaneLayout) {
			FragmentManager fm = getSupportFragmentManager();
			Fragment recentPresetsFragment = fm.findFragmentByTag(RECENTPRESETS_FRAGMENT);
			if (recentPresetsFragment != null) {
				((RecentPresetsFragment)recentPresetsFragment).disable();
			}
			presetFragment.disable();
		} else {
			tagEditorFragment.disableRecentPresets();
		}
	}

	@Override
	public void updateSingleValue(String key, String value) {
		if (tagEditorFragment != null) {
			tagEditorFragment.updateSingleValue(key, value);
		} else {
			Log.e(DEBUG_TAG,"updateSingleValue tagEditorFragment is null");
		}	
	}

	@Override
	public void updateTags(Map<String, String> tags, boolean flush) {
		if (tagEditorFragment != null) {
			tagEditorFragment.updateTags(tags, flush);
		} else {
			Log.e(DEBUG_TAG,"updateSingleValue tagEditorFragment is null");
		}	
	}
	
	@Override
	public void revertTags() {
		if (tagEditorFragment != null) {
			tagEditorFragment.revertTags();
		} else {
			Log.e(DEBUG_TAG,"revertTags tagEditorFragment is null");
		}
	}
	
	@Override
	public void deleteTag(final String key) {
		if (tagEditorFragment != null) {
			tagEditorFragment.deleteTag(key);
		} else {
			Log.e(DEBUG_TAG,"deleteTag tagEditorFragment is null");
		}
	}
	
	public ArrayList<LinkedHashMap<String, String>> getUpdatedTags() {
		if (tagEditorFragment != null) {
			return tagEditorFragment.getUpdatedTags();
		} else {
			Log.e(DEBUG_TAG,"getUpdatedTags tagEditorFragment is null");
			return null;
		}	
	}
	
	@Override
	public LinkedHashMap<String, String> getKeyValueMapSingle(
			boolean allowBlanks) {
		if (tagEditorFragment != null) {
			return tagEditorFragment.getKeyValueMapSingle(allowBlanks);
		} else {
			Log.e(DEBUG_TAG,"getUpdatedTags tagEditorFragment is null");
			return null;
		}	
	}
	
	@Override
	public PresetItem getBestPreset() {
		if (tagEditorFragment != null) {
			return tagEditorFragment.getBestPreset();
		} else {
			Log.e(DEBUG_TAG,"getBestPreset tagEditorFragment is null");
			return null;
		}	
	}
	
	@Override
	public List<PresetItem> getSecondaryPresets() {
		if (tagEditorFragment != null) {
			return tagEditorFragment.getSecondaryPresets();
		} else {
			Log.e(DEBUG_TAG,"getSecondaryPresets tagEditorFragment is null");
			return null;
		}	
	}
	
	@Override
	public Map<String,PresetItem> getAllPresets() {
		if (tagEditorFragment != null) {
			return tagEditorFragment.getAllPresets();
		} else {
			Log.e(DEBUG_TAG,"getAllPresets tagEditorFragment is null");
			return null;
		}	
	}
	
	@Override
	public void updatePresets() {
		if (tagEditorFragment != null) {
			tagEditorFragment.updatePresets();
		} else {
			Log.e(DEBUG_TAG,"updatePresets tagEditorFragment is null");
		}	
	}
	
	@Override
	public void predictAddressTags(boolean allowBlanks) {
		if (tagEditorFragment != null) {
			tagEditorFragment.predictAddressTags(allowBlanks);
		} else {
			Log.e(DEBUG_TAG,"predictAddressTags tagEditorFragment is null");
		}	
	}
	

	@Override
	public void applyTagSuggestions(TagMap tags) {
		if (tagEditorFragment != null) {
			tagEditorFragment.applyTagSuggestions(tags);
		} else {
			Log.e(DEBUG_TAG,"applyTagSuggestions tagEditorFragment is null");
		}	
	}

	@Override
	public boolean pasteIsPossible() {
		if (tagEditorFragment != null) {
			return tagEditorFragment.pasteIsPossible();
		} else {
			Log.e(DEBUG_TAG,"pasteIsPossible tagEditorFragment is null");
		}
		return false;
	}

	@Override
	public boolean paste(boolean replace) {
		if (tagEditorFragment != null) {
			return tagEditorFragment.paste(replace);
		} else {
			Log.e(DEBUG_TAG,"paste tagEditorFragment is null");
		}
		return false;
	}

	@Override
	public boolean pasteFromClipboardIsPossible() {
		if (tagEditorFragment != null) {
			return tagEditorFragment.pasteFromClipboardIsPossible();
		} else {
			Log.e(DEBUG_TAG,"pasteFromClipboardIsPossible tagEditorFragment is null");
		}
		return false;
	}

	@Override
	public boolean pasteFromClipboard(boolean replace) {
		if (tagEditorFragment != null) {
			return tagEditorFragment.pasteFromClipboard(replace);
		} else {
			Log.e(DEBUG_TAG,"pasteFromClipboard tagEditorFragment is null");
		}
		return false;
	}
	

	@Override
	public void copyTags(Map<String, String> tags) {
		if (tagEditorFragment != null) {
			tagEditorFragment.copyTags(tags);
		} else {
			Log.e(DEBUG_TAG,"copyTags tagEditorFragment is null");
		}
	}
	
	@Override
	public void tagsUpdated() {
		if (tagFormFragment != null) {
			tagFormFragment.tagsUpdated();
		} else {
			Log.e(DEBUG_TAG,"tagFormFragment is null");
		}	
	}
	

	@Override
	public void typeUpdated(ElementType type) {
		if (presetFragment != null) {
			presetFragment.typeUpdated(type);
		} else {
			Log.e(DEBUG_TAG,"presetFragment is null");
		}	
		
	}	
	
	/**
	 * Gets an adapter for the autocompletion of street names based on the neighborhood of the edited item.
	 * @param tagValues 
	 * @return
	 */
	public ArrayAdapter<ValueWithCount> getStreetNameAdapter(ArrayList<String> tagValues) {
		if (Application.getDelegator() == null) {
			return null;
		}
		if (streetNameAutocompleteAdapter == null) {
			streetNameAutocompleteAdapter =	new StreetTagValueAdapter(this,
					R.layout.autocomplete_row, Application.getDelegator(),
					types[0], osmIds[0], tagValues); // FIXME
		}
		return streetNameAutocompleteAdapter;
	}
	
	/**
	 * Gets an adapter for the autocompletion of place names based on the neighborhood of the edited item.
	 * @return
	 */
	public ArrayAdapter<ValueWithCount> getPlaceNameAdapter(ArrayList<String> tagValues) {
		if (Application.getDelegator() == null) {
			return null;
		}
		if (placeNameAutocompleteAdapter == null) {
			placeNameAutocompleteAdapter =	new PlaceTagValueAdapter(this,
					R.layout.autocomplete_row, Application.getDelegator(),
					types[0], osmIds[0], tagValues); // FIXME
		}
		return placeNameAutocompleteAdapter;
	}
	
	public OsmElement getElement() {
		return elements[0]; // FIXME validate
	}
	
	
	@Override
	public void onSupportActionModeFinished(ActionMode mode) {
		super.onSupportActionModeFinished(mode);
	}
	
	@Override
	/**
	 * Workaround for bug mentioned below
	 */
	public ActionMode startSupportActionMode(final ActionMode.Callback callback) {
	  // Fix for bug https://code.google.com/p/android/issues/detail?id=159527
	  final ActionMode mode = super.startSupportActionMode(callback);
	  if (mode != null) {
	    mode.invalidate();
	  }
	  return mode;
	}

	@Override
	/**
	 * A tag has been updated, reflect this in both editors
	 */
	public void save(String key, String value) {
		updateSingleValue(key,value);
		tagsUpdated();
	}
}
