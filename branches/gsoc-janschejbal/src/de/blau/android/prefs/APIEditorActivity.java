package de.blau.android.prefs;

import java.util.List;

import android.os.Bundle;
import de.blau.android.prefs.AdvancedPrefDatabase.API;

/** Provides an activity for editing the API list */
public class APIEditorActivity extends URLListEditActivity {

	private AdvancedPrefDatabase db;
	
	public APIEditorActivity() {
		super();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		db = new AdvancedPrefDatabase(this);
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onLoadList(List<ListEditItem> items) {
		API[] apis = db.getAPIs();
		for (API api : apis) {
			items.add(new ListEditItem(api.id, api.name, api.url));
		}
	}

	@Override
	protected void onItemClicked(ListEditItem item) {
		db.selectAPI(item.id);
		finish();
	}

	@Override
	protected void onItemCreated(ListEditItem item) {
		db.addAPI(item.id, item.name, item.value, "", "", "", false);
	}

	@Override
	protected void onItemEdited(ListEditItem item) {
		db.setAPIDescriptors(item.id, item.name, item.value);
	}

	@Override
	protected void onItemDeleted(ListEditItem item) {
		db.deleteAPI(item.id);
	}
	

}
