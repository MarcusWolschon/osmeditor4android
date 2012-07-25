package de.blau.android.presets;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import de.blau.android.osm.OsmElement;
import de.blau.android.presets.Preset.PresetClickHandler;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;

public class PresetDialog extends Dialog implements PresetClickHandler {
	
	private final Context context;
	/** The preset data used by this dialog */
	private final Preset preset;
	
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
		this.preset = preset;
		this.element = element;
		
		currentGroup = preset.getRootGroup();
		
		updateView();
	}

	private void updateView() {
		View view = currentGroup.getGroupView(this, element.getType());
		view.setBackgroundColor(0xff666666);
		setContentView(view);
	}
	
	/**
	 * If this is not the root group, back goes one group up, otherwise, the default is triggered (cancelling the dialog)
	 */
	@Override
	public void onBackPressed() {
		PresetGroup group = currentGroup.getParent();
		if (group != null) {
			currentGroup = group;
			updateView();
		} else {
			super.onBackPressed();
		}
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
