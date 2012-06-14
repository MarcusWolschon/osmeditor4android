package de.blau.android.prefs;

import java.util.List;

import android.os.Bundle;

import de.blau.android.Main;
import de.blau.android.prefs.AdvancedPrefDatabase.API;
import de.blau.android.prefs.AdvancedPrefDatabase.PresetInfo;

public class PresetEditorActivity extends URLListEditActivity {

	private AdvancedPrefDatabase db;
	
	public PresetEditorActivity() {
		super();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		db = new AdvancedPrefDatabase(this);
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onLoadList(List<ListEditItem> items) {
		PresetInfo[] presets = db.getPresets();
		for (PresetInfo preset : presets) {
			items.add(new ListEditItem(preset.id, preset.name, preset.url));
		}
	}

	@Override
	protected void onItemClicked(ListEditItem item) {
		db.setCurrentAPIPreset(item.id);
		finish();
	}

	@Override
	protected void onItemCreated(ListEditItem item) {
		db.addPreset(item.id, item.name, item.value);
	}

	@Override
	protected void onItemEdited(ListEditItem item) {
		db.setPresetInfo(item.id, item.name, item.value);
		db.removePresetDirectory(item.id);
		Main.resetPreset();
	}

	@Override
	protected void onItemDeleted(ListEditItem item) {
		db.deletePreset(item.id);
		Main.resetPreset();
	}
	

}
