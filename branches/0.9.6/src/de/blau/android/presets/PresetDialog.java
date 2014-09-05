package de.blau.android.presets;

import java.util.ArrayList;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;

import android.app.Dialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import de.blau.android.R;
import de.blau.android.osm.OsmElement;
import de.blau.android.presets.Preset.PresetClickHandler;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;

public class PresetDialog extends Dialog implements PresetClickHandler {
	
	private final Context context;
	
	/** The OSM element to which the preset will be applied (used for filtering) */
	private OsmElement element;
	
	private PresetGroup currentGroup;
	private PresetGroup rootGroup;
	
	private PresetItem dialogResult = null; 
	
	/**
	 * Creates a new preset dialog
	 * @param context the context to use
	 * @param preset the Preset data to use
	 * @param element the OSM element to which the preset will be applied (used for filtering)
	 */
	public PresetDialog(Context context, Preset[] presets, OsmElement element) {
		// super(context, android.R.style.Theme_DeviceDefault_NoActionBar);
		super(context,R.style.Theme_customTagEditor);
		
		this.context = context;
		this.element = element;
		
		if (presets.length == 1)
			currentGroup = presets[0].getRootGroup();
		else {
			// a bit of a hack ... this adds the elements from other presets to the root group of the first one
			rootGroup = presets[0].getRootGroup();
			ArrayList<PresetElement> rootElements = rootGroup.getElements();
			for (int i=1;i<presets.length;i++) {
				for (PresetElement e:presets[i].getRootGroup().getElements()) {
					if (!rootElements.contains(e)) { // only do this if not already present
						rootGroup.addElement(e);
						e.setParent(rootGroup);
					}
				}
			}
			currentGroup = rootGroup;
		}	
		updateView();
	}
	
	private void updateView() {
		View view = currentGroup.getGroupView(context, this, element.getType());
		view.setBackgroundColor(context.getResources().getColor(R.color.abs__background_holo_dark));
		setContentView(view);
	}
	
	/**
	 * If this is not the root group, back goes one group up, otherwise, the default is triggered (cancelling the dialog)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			PresetGroup group = currentGroup.getParent();
			if (group != null) {
				currentGroup = group;
				updateView();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	/**
	 * Handle clicks on icons representing an item (closing the dialog with the item as a result)
	 */
	@Override
	public void onItemClick(PresetItem item) {
		dialogResult = item;
		dismiss();
	}
	
	/**
	 * for now do the same
	 */
	@Override
	public boolean onItemLongClick(PresetItem item) {
		dialogResult = item;
		dismiss();
		return true;
	}
	
	/**
	 * Handle clicks on icons representing a group (changing to that group)
	 */
	@Override
	public void onGroupClick(PresetGroup group) {
		currentGroup = group;
		updateView();
	}
	
	public PresetItem getDialogResult() {
		return dialogResult;
	}
	
}
