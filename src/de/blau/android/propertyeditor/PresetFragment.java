package de.blau.android.propertyeditor;

import java.util.ArrayList;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.ScrollView;
import de.blau.android.ElementInfoFragment;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetClickHandler;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.propertyeditor.PropertyEditor;
import de.blau.android.propertyeditor.TagEditorFragment;

public class PresetFragment extends SherlockFragment implements PresetClickHandler {
	
	private static final String DEBUG_TAG = PresetFragment.class.getName();
	
    public interface OnPresetSelectedListener {
        public void onPresetSelected(PresetItem item);
    }
    
//	private final Context context;
    
    OnPresetSelectedListener mListener;
	
	/** The OSM element to which the preset will be applied (used for filtering) */
	private OsmElement element;
	
	private PresetGroup currentGroup;
	private PresetGroup rootGroup;
	
	private PresetItem dialogResult = null; 
	
	View view;

	/**
     */
    static public PresetFragment newInstance(Preset[] presets, OsmElement e) {
    	PresetFragment f = new PresetFragment();

        Bundle args = new Bundle();
        args.putSerializable("presets", presets);
        args.putSerializable("element", e);

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
        setHasOptionsMenu(true);
        getActivity().supportInvalidateOptionsMenu();
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
//       	if (presetView != null) {
//    		Log.d(DEBUG_TAG, "onCreateView recalled but we still have a view");
//    		return presetView;
//    	}
        element = (OsmElement) getArguments().getSerializable("element");
        Preset[] presets = (Preset[]) getArguments().getSerializable("presets");
        rootGroup = presets[0].getRootGroup();
		if (presets.length > 1) {
			// a bit of a hack ... this adds the elements from other presets to the root group of the first one
			
			ArrayList<PresetElement> rootElements = rootGroup.getElements();
			for (int i=1;i<presets.length;i++) {
				for (PresetElement e:presets[i].getRootGroup().getElements()) {
					if (!rootElements.contains(e)) { // only do this if not already present
						rootGroup.addElement(e);
						e.setParent(rootGroup);
					}
				}
			}
		}	
		currentGroup = rootGroup;
		return getPresetView();
    }
	
	private View getPresetView() {
		view = currentGroup.getGroupView(getActivity(), this, element.getType());
		view.setBackgroundColor(getActivity().getResources().getColor(R.color.abs__background_holo_dark));
		// view.setOnKeyListener(this);
		return view;
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
	 * If this is not the root group, back goes one group up, otherwise, the default is triggered (cancelling the dialog)
	 */
//	@Override
//	public boolean onKey(View v, int keyCode, KeyEvent event) {
//		if (keyCode == KeyEvent.KEYCODE_BACK) {
//			PresetGroup group = currentGroup.getParent();
//			if (group != null) {
//				currentGroup = group;
//				currentGroup.getGroupView(getActivity(), (ScrollView) view, this, element.getType());
//				view.invalidate();
//				return true;
//			}
//		}
//		return false;
//	}
	
	/**
	 * Handle clicks on icons representing an item (closing the dialog with the item as a result)
	 */
	@Override
	public void onItemClick(PresetItem item) {
		mListener.onPresetSelected(item);
		// dismiss();
	}
	
	/**
	 * for now do the same
	 */
	@Override
	public boolean onItemLongClick(PresetItem item) {
		mListener.onPresetSelected(item);
		// dismiss();
		return true;
	}
	
	/**
	 * Handle clicks on icons representing a group (changing to that group)
	 */
	@Override
	public void onGroupClick(PresetGroup group) {
		currentGroup = group;
		currentGroup.getGroupView(getActivity(), (ScrollView) view, this, element.getType());
		view.invalidate();
	}
	
	public PresetItem getDialogResult() {
		return dialogResult;
	}
	
	
	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		// final MenuInflater inflater = getSupportMenuInflater();
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.preset_menu, menu);
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
		case R.id.preset_menu_top:
			if (rootGroup != null) {
				currentGroup = rootGroup;
				currentGroup.getGroupView(getActivity(), (ScrollView) view, this, element.getType());
				view.invalidate();
				return true;
			}
			return true;
		case R.id.preset_menu_up:
			PresetGroup group = currentGroup.getParent();
			if (group != null) {
				currentGroup = group;
				currentGroup.getGroupView(getActivity(), (ScrollView) view, this, element.getType());
				view.invalidate();
				return true;
			}
			return true;
		case R.id.preset_menu_help:
			Intent startHelpViewer = new Intent(getActivity(), HelpViewer.class);
			startHelpViewer.putExtra(HelpViewer.TOPIC, "TagEditor");
			startActivity(startHelpViewer);
			return true;
		}
		
		return false;
	}
}
