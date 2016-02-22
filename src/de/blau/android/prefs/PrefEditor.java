package de.blau.android.prefs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.R;

/**
 * Simple class for Android's standard-Preference Activity
 * 
 * @author mb
 */
public class PrefEditor extends SherlockFragmentActivity {
	
	public static void start(@NonNull Context context) {
		Intent intent = new Intent(context, PrefEditor.class);
		context.startActivity(intent);
	}
	
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.d("PrefEditor", "onCreate");
		Preferences prefs = new Preferences(this);
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.Theme_Sherlock_Light);
		}
		
		super.onCreate(savedInstanceState);
		
		getSupportFragmentManager().beginTransaction().replace(android.R.id.content,
                new PrefEditorFragment()).commit();
		
		ActionBar actionbar = getSupportActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		Log.d("PrefEditor", "onOptionsItemSelected");
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
