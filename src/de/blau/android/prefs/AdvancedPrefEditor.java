package de.blau.android.prefs;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

public class AdvancedPrefEditor extends AppCompatActivity {

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("AdvancedPrefEditor", "onCreate");
		Preferences prefs = new Preferences(this);
		if (prefs.lightThemeEnabled()) {
			setTheme(android.R.style.Theme_Holo_Light);
		}
		
		super.onCreate(savedInstanceState);
		getSupportFragmentManager().beginTransaction().replace(android.R.id.content,
                new AdvancedPrefEditorFragment()).commit();
		
		ActionBar actionbar = getSupportActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
	}
	
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		Log.d("AdvancedPrefEditor", "onOptionsItemSelected");
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
