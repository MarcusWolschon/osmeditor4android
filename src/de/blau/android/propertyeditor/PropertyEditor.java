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
	public static final String TAGEDIT_SHOW_PRESETS = "showPresets";
	
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
	private PresetFragment presetFragment;
	ExtendedViewPager    mViewPager;
	boolean usePaneLayout = false;

	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		prefs = new Preferences(this);
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.Theme_customTagEditor_Light);
		}
		
		super.onCreate(savedInstanceState);
		
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
			loadData = PropertyEditorData.deserializeArray(getIntent().getSerializableExtra(TAGEDIT_DATA));
			applyLastAddressTags = (Boolean)getIntent().getSerializableExtra(TAGEDIT_LAST_ADDRESS_TAGS); 
			showPresets = (Boolean)getIntent().getSerializableExtra(TAGEDIT_SHOW_PRESETS);
		} else {
			// Restore activity from saved state
			Log.d(DEBUG_TAG, "Restoring from savedInstanceState");
			// loadData = (PropertyEditorData)savedInstanceState.getSerializable(TAGEDIT_DATA);
			// FIXME needs to be checked if this really works
			loadData = PropertyEditorData.deserializeArray(getIntent().getSerializableExtra(TAGEDIT_DATA));
			// applyLastTags = (Boolean)savedInstanceState.getSerializable(TAGEDIT_LASTTAGS); not saved 
		}
				
		Log.d(DEBUG_TAG, "... done.");
		
		// sanity check
		if (Main.getLogic() == null || Main.getLogic().getDelegator() == null || loadData == null) {
			abort();
		}
		
		osmIds = new long[loadData.length];
		types = new String[loadData.length];
		elements = new OsmElement[loadData.length];
		
		for (int i=0;i<loadData.length;i++) {
			osmIds[i] = loadData[i].osmId;
			types[i] = loadData[i].type;
			elements[i] = Main.getLogic().getDelegator().getOsmElement(types[i], osmIds[i]);
			// and another sanity check
			if (elements[i] == null) {
				abort();
			}
		}
		
		presets = Main.getCurrentPresets();
		
		int screenSize = getResources().getConfiguration().screenLayout &
		        Configuration.SCREENLAYOUT_SIZE_MASK;
		// reliable determine if we are in landscape mode
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			display.getSize(size);
		} else {
			size.x = display.getWidth();
			size.y = display.getHeight();
		}
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
			presetFragment = PresetFragment.newInstance(Main.getCurrentPresets(),elements[0]); // FIXME collect tags to determine presets
			presetFragmentPosition = propertyEditorPagerAdapter.addFragment(getString(R.string.tag_menu_preset),presetFragment);
		}
		// tags
		
		
		
		ArrayList<LinkedHashMap<String, String>> tags = new ArrayList<LinkedHashMap<String, String>>();
		originalTags = new ArrayList<LinkedHashMap<String, String>>();
		for (int i=0;i<loadData.length;i++) {
			originalTags.add((LinkedHashMap<String, String>) (loadData[i].originalTags != null ? loadData[i].originalTags : loadData[i].tags));
			tags.add((LinkedHashMap<String, String>) loadData[i].tags);
		}
		
		
		tagEditorFragment = TagEditorFragment.newInstance(elements, tags, applyLastAddressTags, loadData[0].focusOnKey, !usePaneLayout);
		tagEditorFragmentPosition = propertyEditorPagerAdapter.addFragment(getString(R.string.menu_tags),tagEditorFragment);

		if (loadData.length == 1) { // for now no support of relations 
			// parent relations
			originalParents = loadData[0].originalParents != null ? loadData[0].originalParents : loadData[0].parents;

			relationMembershipFragment = RelationMembershipFragment.newInstance(loadData[0].parents);
			propertyEditorPagerAdapter.addFragment(getString(R.string.relations),relationMembershipFragment);


			if (types[0].endsWith(Relation.NAME)) {
				// members of this relation
				originalMembers = loadData[0].originalMembers != null ? loadData[0].originalMembers : loadData[0].members;

				relationMembersFragment = RelationMembersFragment.newInstance(loadData[0].members);
				propertyEditorPagerAdapter.addFragment(getString(R.string.members),relationMembersFragment);
			}
		}
		
		
		if (usePaneLayout) { // add both preset fragments to panes
			Log.d(DEBUG_TAG,"Adding MRU prests");
			FragmentManager fm = getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			Fragment recentPresetsFragment = fm.findFragmentByTag("recentpresets_fragment");
			if (recentPresetsFragment != null) {
				ft.remove(recentPresetsFragment);
			}
			recentPresetsFragment = RecentPresetsFragment.newInstance(elements[0]); // FIXME collect tags
			ft.add(R.id.recent_preset_row,recentPresetsFragment,"recentpresets_fragment");
			
			presetFragment = (PresetFragment) fm.findFragmentByTag("preset_fragment");
			if (presetFragment != null) {
				ft.remove(presetFragment);
			}
			presetFragment = PresetFragment.newInstance(Main.getCurrentPresets(),elements[0]); // FIXME collect tags
			ft.add(R.id.preset_row,presetFragment,"preset_fragment");
			
			ft.commit();
		}
		
		mViewPager.setOffscreenPageLimit(3); // hack keep all alive
		mViewPager.setAdapter(propertyEditorPagerAdapter);
		mViewPager.setCurrentItem(showPresets ? presetFragmentPosition : tagEditorFragmentPosition);
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
		ArrayList<LinkedHashMap<String, String>> currentTags = tagEditorFragment.getUpdatedTags();
		HashMap<Long,String> currentParents = relationMembershipFragment.getParentRelationMap();
		ArrayList<RelationMemberDescription> currentMembers = new ArrayList<RelationMemberDescription>(); // FIXME
		if (types.equals(Relation.NAME)) {
			currentMembers = relationMembersFragment.getMembersList();
		}
		// if we haven't edited just exit
		if (!same(currentTags,originalTags) || !currentParents.equals(originalParents) || (elements[0] != null && elements[0].getName().equals(Relation.NAME) && !currentMembers.equals(originalMembers))) {
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
			savingHelper.save(COPIED_TAGS_FILE, copiedTags, false);
		}
		// save any address tags for "last address tags"
		if (currentTags.size() == 1) {
			Address.updateLastAddresses(tagEditorFragment, currentTags.get(0));// FIXME
		}
		Intent intent = new Intent();
		
		HashMap<Long,String> currentParents = null;
		ArrayList<RelationMemberDescription> currentMembers = null;
		PropertyEditorData[] newData = new PropertyEditorData[currentTags.size()];
		if (currentTags.size() == 1) { // normal single mode, relations might have changed
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
	 * Check if two set of tags are the same
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
