package de.blau.android.presets;

import android.app.Dialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;

import de.blau.android.R;
import de.blau.android.osm.OsmElement;
import de.blau.android.presets.Preset.PresetClickHandler;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;

public class PresetDialog extends Dialog implements PresetClickHandler {
	
	private final Context context;
	
	/** The OSM element to which the preset will be applied (used for filtering) */
	private OsmElement element;
	
	private PresetGroup currentGroup;
	
	private PresetItem dialogResult = null; 
	
	/**
	 * Creates a new preset dialog
	 * @param context the context to use
	 * @param preset the Preset data to use
	 * @param element the OSM element to which the preset will be applied (used for filtering)
	 */
	public PresetDialog(Context context, Preset preset, OsmElement element) {
		super(context, android.R.style.Theme_DeviceDefault_NoActionBar);
		this.context = context;
		this.element = element;
		
		currentGroup = preset.getRootGroup();
		
		updateView();
	}
	
	private void updateView() {
		View view = currentGroup.getGroupView(context, this, element.getType());
		view.setBackgroundColor(context.getResources().getColor(R.color.preset_bg));
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
