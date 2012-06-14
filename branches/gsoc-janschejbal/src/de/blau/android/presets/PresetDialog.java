package de.blau.android.presets;

import de.blau.android.osm.OsmElement;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.Preset.PresetClickHandler;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class PresetDialog extends Dialog implements PresetClickHandler {
	
	private final Context context;
	private final Preset preset;
	private OsmElement element;
	
	private PresetGroup currentGroup;
	
	private PresetItem dialogResult = null; 
	
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


	@Override
	public void onItemClick(PresetItem item) {
		dialogResult = item;
		dismiss();
	}


	@Override
	public void onGroupClick(PresetGroup group) {
		currentGroup = group;
		updateView();
	}
	
	public PresetItem getDialogResult() {
		return dialogResult;
	}

	
}
