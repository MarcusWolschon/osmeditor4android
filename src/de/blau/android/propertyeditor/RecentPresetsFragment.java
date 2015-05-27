package de.blau.android.propertyeditor;

import java.util.HashMap;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockFragment;

import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetClickHandler;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;

public class RecentPresetsFragment extends SherlockFragment {

	private static final String DEBUG_TAG = RecentPresetsFragment.class.getName();
	
    private OnPresetSelectedListener mListener;
    private OsmElement element;
    private Preset[] presets;
    private boolean enabled = true;
    
	/**
     */
    static public RecentPresetsFragment newInstance(OsmElement element) {
    	RecentPresetsFragment f = new RecentPresetsFragment();

        Bundle args = new Bundle();
        args.putSerializable("element", element);

        f.setArguments(args);
        // f.setShowsDialog(true);
        
        return f;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(DEBUG_TAG, "onAttach");
        try {
            mListener = (OnPresetSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPresetSelectedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    		Bundle savedInstanceState) {
    	LinearLayout presetsLayout = (LinearLayout) inflater.inflate(R.layout.recentpresets_view,null);
   
     	element = (OsmElement) getArguments().getSerializable("element");

    	presets = Main.getCurrentPresets();
    	
    	View v = getRecentPresetsView(presetsLayout, element, presets);
    	if (v != null) {
        	presetsLayout.addView(v);
        	presetsLayout.setVisibility(View.VISIBLE);
    	}
		return presetsLayout;
    }

	View getRecentPresetsView(final LinearLayout presetLayout, final OsmElement element, final Preset[] presets) {
		View v = null;
	   	if (presets != null && presets.length >= 1 && element != null) {
    		// check if any of the presets has a MRU
    		boolean mruFound = false;
    		for (Preset p:presets) {
    			if (p!=null) {
    				if (p.hasMRU()) {
    					mruFound = true;
    					break;
    				}
    			}
    		}
    		if (mruFound) {
    			final ElementType filterType = element.getType();
    			final PresetClickHandler presetClickHandler = new PresetClickHandler() { 
    					@Override
    					public void onItemClick(PresetItem item) {
    						if (!enabled) {
    							return;
    						}
    						Log.d(DEBUG_TAG, "normal click");
    						mListener.onPresetSelected(item);
    						recreateRecentPresetView(presetLayout);
    					}

    					@Override
    					public boolean onItemLongClick(PresetItem item) {
    						if (!enabled) {
    							return true;
    						}
    						Log.d(DEBUG_TAG, "long click");
    						removePresetFromMRU(presetLayout, item);
    						return true;
    					}

    					@Override
    					public void onGroupClick(PresetGroup group) {
    						// should not have groups
    					}
    				};
    			v = presets[0].getRecentPresetView(getActivity(), presets, presetClickHandler, filterType); //TODO this should really be a call of a static method, all MRUs get added to this view

    			// v.setBackgroundColor(getResources().getColor(R.color.tagedit_field_bg));
    			// v.setPadding(Preset.SPACING, Preset.SPACING, Preset.SPACING, Preset.SPACING);
    			v.setId(R.id.recentPresets);
    		} else {
    			Log.d(DEBUG_TAG,"getRecentPresetsView no MRU found!");
    		}	
	   	} else {
			Log.d(DEBUG_TAG,"getRecentPresetsView problem with presets " + presets + " or element " + element);
		}	
	   	return v;
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
	 * Removes a preset from the MRU
	 * @param item the preset to apply
	 */
	private void removePresetFromMRU(LinearLayout presetLayout, PresetItem item) {
		
		//
		Preset[] presets = Main.getCurrentPresets();
		if (presets != null) {
			for (Preset p:presets) {
				if (p.contains(item)) {
					p.removeRecentlyUsed(item);
					break;
				}
			}
		}
		recreateRecentPresetView(presetLayout);
	}
	
	public void recreateRecentPresetView() {
		recreateRecentPresetView((LinearLayout) getOurView());
	}
	
	public void recreateRecentPresetView(LinearLayout presetLayout) {
		Log.d(DEBUG_TAG,"recreateRecentPresetView");
		presetLayout.removeAllViews();
		View v = getRecentPresetsView(presetLayout, element, presets);
		if (v != null) {
			presetLayout.addView(v);
			presetLayout.setVisibility(View.VISIBLE);
		} else {
			
		}
		presetLayout.invalidate();
	}
    	
	/**
	 * Return the view we have our rows in and work around some android craziness
	 * @return
	 */
	public View getOurView() {
		// android.support.v4.app.NoSaveStateFrameLayout
		View v =  getView();	
		if (v != null) {
			if ( v.getId() == R.id.recentpresets_layout) {
				Log.d(DEBUG_TAG,"got correct view in getView");
				return v;
			} else {
				v = v.findViewById(R.id.recentpresets_layout);
				if (v == null) {
					Log.d(DEBUG_TAG,"didn't find R.id.recentpresets_layoutt");
				}  else {
					Log.d(DEBUG_TAG,"Found R.id.recentpresets_layout");
				}
				return v;
			}
		} else {
			Log.d(DEBUG_TAG,"got null view in getView");
		}
		return null;
	}
	
	protected void enable() {
		enabled = true;
	}
	
	protected void disable() {
		enabled = false;
	}
}
