package de.blau.android.propertyeditor;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.OsmElement;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetClickHandler;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.util.ThemeUtils;

public class PresetSearchResultsFragment extends DialogFragment {

	private static final String DEBUG_TAG = PresetSearchResultsFragment.class.getSimpleName();
	
    private OnPresetSelectedListener mListener;
    private OsmElement element;
    private ArrayList<PresetItem> presets;
    private boolean enabled = true;
    
	/**
     */
    static public PresetSearchResultsFragment newInstance(ArrayList<PresetItem>searchResults) {
    	PresetSearchResultsFragment f = new PresetSearchResultsFragment();

        Bundle args = new Bundle();
        args.putSerializable("searchResults", searchResults);

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
    
    @SuppressWarnings("unchecked")
	@SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState)
    {
    	final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
    	Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setTitle(R.string.search_results_title);
       	// inflater needs to be got from a themed view or else all our custom stuff will not style correctly
    	LinearLayout presetsLayout = (LinearLayout) inflater.inflate(R.layout.preset_search_results_view,null);
   
    	presets = (ArrayList<PresetItem>) getArguments().getSerializable("searchResults");
   	
    	View v = getResultsView(presetsLayout, presets);

    	if (v != null) {
    		builder.setView(v);
    	}
    	return builder.create();
    }
    

	private View getResultsView(final LinearLayout presetLayout, final ArrayList<PresetItem> presets) {
		View v = null;
		if (presets != null && presets.size() >= 1 ) {

			final PresetClickHandler presetClickHandler = new PresetClickHandler() { 
				@Override
				public void onItemClick(PresetItem item) {
					if (!enabled) {
						return;
					}
					Log.d(DEBUG_TAG, "normal click");
					mListener.onPresetSelected(item);
					dismiss();
				}

				@Override
				public void onGroupClick(PresetGroup group) {
					// should not have groups
				}

				@Override
				public boolean onItemLongClick(PresetItem item) {
					// TODO Auto-generated method stub
					return false;
				}
			};
			
			Preset[] currentPresets = App.getCurrentPresets(getActivity());
			
			if (!(currentPresets != null && currentPresets.length > 0)) {
				return null;
			}
			
			PresetGroup results = new Preset().new PresetGroup(null, "search results", null); 
			for (PresetItem p: presets) {
				if (p != null ) {
					results.addElement(p);
				}
			}
			v = results.getGroupView(getActivity(), presetClickHandler, null);

			// v.setBackgroundColor(getResources().getColor(R.color.tagedit_field_bg));
	    	  
	    	int padding = ThemeUtils.getDimensionFromAttribute(getActivity(), R.attr.dialogPreferredPadding);		
			v.setPadding(padding - Preset.SPACING, Preset.SPACING, padding- Preset.SPACING, padding);
	   	} else {
			Log.d(DEBUG_TAG,"getResultsView problem");
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
}
