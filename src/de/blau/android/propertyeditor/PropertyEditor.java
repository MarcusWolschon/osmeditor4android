package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerTabStrip;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMemberDescription;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.util.SavingHelper;
import de.blau.android.views.ExtendedViewPager;

/**
 * An Activity to edit OSM-Tags. Sends the edited Tags as Result to its caller-Activity (normally {@link Main}).
 * 
 * @author mb
 */
public class PropertyEditor extends SherlockFragmentActivity implements 
		 OnPresetSelectedListener {
	public static final String TAGEDIT_DATA = "dataClass";
	public static final String TAGEDIT_LAST_ADDRESS_TAGS = "applyLastTags";
	
	/** The layout containing the edit rows */
	LinearLayout rowLayout = null;
	
	TagEditorFragment tagEditorFragment;
	int	tagEditorFragmentPosition = -1;
	int presetFragmentPosition = -1;
	RelationMembershipFragment relationMembershipFragment;
	RelationMembersFragment relationMembersFragment;
	RecentPresetsFragment recentPresetsFragment;
	
	/**
	 * The tag we use for Android-logging.
	 */
	private static final String DEBUG_TAG = PropertyEditor.class.getName();
	
	private long osmId;
	
	private String type;
	
	Preset[] presets = null;
	/**
	 * The OSM element for reference.
	 * DO NOT ATTEMPT TO MODIFY IT.
	 */
	OsmElement element;
	
	private PropertyEditorData loadData;
	
	private boolean applyLastAddressTags = false;
		
	/** Set to true once values are loaded. used to suppress adding of empty rows while loading. */
	private boolean loaded;
	
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
	 * The tags present when this editor was created (for undoing changes)
	 */
	private Map<String, String> originalTags;
	
	/**
	 * the same for relations
	 */
	private HashMap<Long,String> originalParents;
	private ArrayList<RelationMemberDescription> originalMembers;
	

	
	static final String COPIED_TAGS_FILE = "copiedtags.dat";
	 
	private SavingHelper<LinkedHashMap<String,String>> savingHelper
				= new SavingHelper<LinkedHashMap<String,String>>();
		
	private Preferences prefs = null;
	private PresetFragment presetFragment;
	ExtendedViewPager    mViewPager;
	boolean usePaneLayout = false;

	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		prefs = new Preferences(this);
		if (prefs.splitActionBarEnabled()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW); // this might need to be set with bit ops
			}
			// besides hacking ABS, there is no equivalent method to enable this for ABS
		} 

		loaded = false;
		
		// tags

		if (savedInstanceState == null) {
			// No previous state to restore - get the state from the intent
			Log.d(DEBUG_TAG, "Initializing from intent");
			loadData = (PropertyEditorData)getIntent().getSerializableExtra(TAGEDIT_DATA);
			applyLastAddressTags = (Boolean)getIntent().getSerializableExtra(TAGEDIT_LAST_ADDRESS_TAGS); 
		} else {
			// Restore activity from saved state
			Log.d(DEBUG_TAG, "Restoring from savedInstanceState");
			// loadData = (PropertyEditorData)savedInstanceState.getSerializable(TAGEDIT_DATA);
			// FIXME needs to be checked if this really works
			loadData = (PropertyEditorData)getIntent().getSerializableExtra(TAGEDIT_DATA);
			// applyLastTags = (Boolean)savedInstanceState.getSerializable(TAGEDIT_LASTTAGS); not saved 
		}
				
		Log.d(DEBUG_TAG, "... done.");
		
		// sanity check
		if (Main.getLogic() == null || Main.getLogic().getDelegator() == null) {
			abort();
		}
		osmId = loadData.osmId;
		type = loadData.type;
		element = Main.getLogic().getDelegator().getOsmElement(type, osmId);
		// and another sanity check
		if (element == null) {
			abort();
		}
		
		presets = Main.getCurrentPresets();
		
		int screenSize = getResources().getConfiguration().screenLayout &
		        Configuration.SCREENLAYOUT_SIZE_MASK;
		// reliable determine if we are in landscape mode
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		if ((screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE) && size.x > size.y) {
			usePaneLayout = true;
			setContentView(R.layout.pane_view);
			Log.d(DEBUG_TAG, "Using layout for large devices");
		} else {
			setContentView(R.layout.tab_view);
		}
		
		PropertyEditorPagerAdapter  propertyEditorPagerAdapter =
                new PropertyEditorPagerAdapter(
                        getSupportFragmentManager());
		mViewPager = (ExtendedViewPager) findViewById(R.id.pager);
		PagerTabStrip pagerTabStrip = (PagerTabStrip) mViewPager.findViewById(R.id.pager_header);
		pagerTabStrip.setDrawFullUnderline(true);
		pagerTabStrip.setTabIndicatorColorResource(android.R.color.holo_blue_dark);

		ActionBar actionbar = getSupportActionBar();
		actionbar.setDisplayShowTitleEnabled(false);
		actionbar.setDisplayHomeAsUpEnabled(true);
		
		// presets
		if (!usePaneLayout) {
			presetFragment = PresetFragment.newInstance(Main.getCurrentPresets(),element);
			presetFragmentPosition = propertyEditorPagerAdapter.addFragment(getString(R.string.tag_menu_preset),presetFragment);
		}
		// tags
		originalTags = loadData.originalTags != null ? loadData.originalTags : loadData.tags;

		
		tagEditorFragment = TagEditorFragment.newInstance(element,(LinkedHashMap<String, String>) loadData.tags, applyLastAddressTags, loadData.focusOnKey, !usePaneLayout);
		tagEditorFragmentPosition = propertyEditorPagerAdapter.addFragment(getString(R.string.menu_tags),tagEditorFragment);

		// parent relations
		originalParents = loadData.originalParents != null ? loadData.originalParents : loadData.parents;

		relationMembershipFragment = RelationMembershipFragment.newInstance(loadData.parents);
		propertyEditorPagerAdapter.addFragment(getString(R.string.relations),relationMembershipFragment);


		if (type.endsWith(Relation.NAME)) {
		// members of this relation
			originalMembers = loadData.originalMembers != null ? loadData.originalMembers : loadData.members;
		
			relationMembersFragment = RelationMembersFragment.newInstance(loadData.members);
			propertyEditorPagerAdapter.addFragment(getString(R.string.members),relationMembersFragment);
		}

		if (usePaneLayout) { // add both preset fragments to panes
			Log.d(DEBUG_TAG,"Adding MRU prests");
			FragmentManager fm = getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			Fragment recentPresetsFragment = fm.findFragmentByTag("recentpresets_fragment");
			if (recentPresetsFragment != null) {
				ft.remove(recentPresetsFragment);
			}
			recentPresetsFragment = RecentPresetsFragment.newInstance(element);
			ft.add(R.id.recent_preset_row,recentPresetsFragment,"recentpresets_fragment");
			
			presetFragment = (PresetFragment) fm.findFragmentByTag("preset_fragment");
			if (presetFragment != null) {
				ft.remove(presetFragment);
			}
			presetFragment = PresetFragment.newInstance(Main.getCurrentPresets(),element);
			ft.add(R.id.preset_row,presetFragment,"preset_fragment");
			
			ft.commit();
		}
		
		mViewPager.setOffscreenPageLimit(3); // hack keep all alive
		mViewPager.setAdapter(propertyEditorPagerAdapter);
		mViewPager.setCurrentItem(tagEditorFragmentPosition);
	}
	
	private void abort() {
		Toast.makeText(this, R.string.toast_inconsistent_state, Toast.LENGTH_LONG).show();
		ACRA.getErrorReporter().handleException(null);
		finish();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		running = true;
		Address.loadLastAddresses();
	}

	
	public class PropertyEditorPagerAdapter extends FragmentPagerAdapter {
		
	    private ArrayList<SherlockFragment> mFragmentList;
	    private ArrayList<String> mTitleList;
		
	    public PropertyEditorPagerAdapter(FragmentManager fm) {
	        super(fm);
	        mTitleList = new ArrayList<String>();
	        mFragmentList = new ArrayList<SherlockFragment>();
	    }

	    /**
	     *  add a fragment and return its index
	     * @param title
	     * @param fragment
	     * @return
	     */
	    public int addFragment(String title,SherlockFragment fragment) {
	    	mTitleList.add(title);
	        mFragmentList.add(fragment);
	        return mTitleList.size() - 1;
	    }

	    @Override
	    public int getCount() {
	        return mFragmentList.size();
	    }

	    @Override
	    public SherlockFragment getItem(int position) {
	    	Log.d(DEBUG_TAG, "getItem " + position);
	        return mFragmentList.get(position);
	    }

	    @Override
	    public CharSequence getPageTitle(int position) {
	    	return mTitleList.get(position);
	        // return ((Fragment) mFragmentList.get(position)).getPageTitle();
	    }
	}
	
	/**
	 * Removes an old RecentPresetView and replaces it by a new one (to update it)
	 */
	void recreateRecentPresetView() {
		if (usePaneLayout) {
			FragmentManager fm = getSupportFragmentManager();
			Fragment recentPresetsFragment = fm.findFragmentByTag("recentpresets_fragment");
			if (recentPresetsFragment != null) {
				((RecentPresetsFragment)recentPresetsFragment).recreateRecentPresetView();
			}
		} else {
			tagEditorFragment.recreateRecentPresetView();
		}
	}
	
	@Override
	public void onBackPressed() {
		// sendResultAndFinish();
		Map<String, String> currentTags = tagEditorFragment.getKeyValueMap(false);
		HashMap<Long,String> currentParents = relationMembershipFragment.getParentRelationMap();
		ArrayList<RelationMemberDescription> currentMembers = new ArrayList<RelationMemberDescription>(); // FIXME
		if (type.equals(Relation.NAME)) {
			currentMembers = relationMembersFragment.getMembersList();
		}
		// if we haven't edited just exit
		if (!currentTags.equals(originalTags) || !currentParents.equals(originalParents) || (element != null && element.getName().equals(Relation.NAME) && !currentMembers.equals(originalMembers))) {
		    new AlertDialog.Builder(this)
	        .setNeutralButton(R.string.cancel, null)
	        .setNegativeButton(R.string.tag_menu_revert,        	
	        		new DialogInterface.OnClickListener() {
	            	@Override
					public void onClick(DialogInterface arg0, int arg1) {
//	            		doRevert();
	            }})
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
		
	/**
	 * Get current values from the fragments and end the activity
	 */
	protected void sendResultAndFinish() {
		// Save current tags for "repeat last" button
		LinkedHashMap<String,String> tags = tagEditorFragment.getKeyValueMap(false);
		LinkedHashMap<String,String> copiedTags = tagEditorFragment.getCopiedTags();
		if (copiedTags != null) {
			savingHelper.save(COPIED_TAGS_FILE, copiedTags, false);
		}
		// save any address tags for "last address tags"
		Address.updateLastAddresses(tagEditorFragment, tags);
		
		Intent intent = new Intent();
		Map<String, String> currentTags = tagEditorFragment.getKeyValueMap(false);
		HashMap<Long,String> currentParents = relationMembershipFragment.getParentRelationMap();
		ArrayList<RelationMemberDescription> currentMembers = new ArrayList<RelationMemberDescription>(); //FIXME
		if (type.endsWith(Relation.NAME)) {
			currentMembers = relationMembersFragment.getMembersList();
		}
		
		if (!currentTags.equals(originalTags) || !(originalParents==null && currentParents.size()==0) && !currentParents.equals(originalParents) 
				|| (element != null && element.getName().equals(Relation.NAME) && !currentMembers.equals(originalMembers))) {
			// changes were made
			intent.putExtra(TAGEDIT_DATA, new PropertyEditorData(osmId, type, 
					currentTags.equals(originalTags)? null : currentTags,  null, 
					(originalParents==null && currentParents.size()==0) || currentParents.equals(originalParents)?null:currentParents, null, 
					currentMembers.equals(originalMembers)?null:currentMembers, null));
		}
		
		setResult(RESULT_OK, intent);
		finish();
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
	}
	
	/** When the Activity is interrupted, save MRUs and address cache*/
	@Override
	protected void onPause() {
		running = false;
		if (Main.getCurrentPresets() != null)  {
			for (Preset p:Main.getCurrentPresets()) {
				if (p!=null) {
					p.saveMRU();
				}
			}
		}
		Address.saveLastAddresses();
		super.onPause();
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
	
	/**
	 * @return the OSM ID of the element currently edited by the editor
	 */
	public long getOsmId() {
		return osmId;
	}
	
	/**
	 * Set the OSM ID currently edited by the editor
	 */
	public void setOsmId(final long osmId) {
		this.osmId = osmId;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(final String type) {
		this.type = type;
	}
	
	@Override
	public void onPresetSelected(PresetItem item) {
		if (item != null) {
			mViewPager.setCurrentItem(tagEditorFragmentPosition);
			tagEditorFragment.applyPreset(item);
			if (usePaneLayout) {
				FragmentManager fm = getSupportFragmentManager();
				Fragment recentPresetsFragment = fm.findFragmentByTag("recentpresets_fragment");
				if (recentPresetsFragment != null) {
					((RecentPresetsFragment)recentPresetsFragment).recreateRecentPresetView();
				}
			}
		}
	}
	

	/**
	 * Allow ViewPAger to work
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
			Fragment recentPresetsFragment = fm.findFragmentByTag("recentpresets_fragment");
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
			Fragment recentPresetsFragment = fm.findFragmentByTag("recentpresets_fragment");
			if (recentPresetsFragment != null) {
				((RecentPresetsFragment)recentPresetsFragment).disable();
			}
			presetFragment.disable();
		} else {
			tagEditorFragment.disableRecentPresets();
		}
	}

}
