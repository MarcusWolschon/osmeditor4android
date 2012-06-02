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
	
	private PresetItem dialogResult = null; 
	
	public PresetDialog(Context context, Preset preset, OsmElement element) {
		super(context, android.R.style.Theme_DeviceDefault_NoActionBar);
		this.context = context;
		this.preset = preset;
		this.element = element;
		
		setContentView(preset.getRootGroup().getGroupView(this, element.getType()));
		
		
	}

	
	@Override
	public void onBackPressed() {
		// TODO aks user if he really wants to keep it untagged?
		super.onBackPressed(); // remove if changing!
	}


	@Override
	public void onItemClick(PresetItem item) {
		Toast.makeText(context, item.getName(), Toast.LENGTH_LONG).show();
		dialogResult = item;
		dismiss();
	}


	@Override
	public void onGroupClick(PresetGroup group) {
		setContentView(group.getGroupView(this, element.getType()));
	}
	
	public PresetItem getDialogResult() {
		return dialogResult;
	}


	public OsmElement getElement() {
		return element;
	}
	
}
