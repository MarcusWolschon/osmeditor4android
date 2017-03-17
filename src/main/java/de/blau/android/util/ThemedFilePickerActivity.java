package de.blau.android.util;

import com.nononsenseapps.filepicker.FilePickerActivity;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;

/**
 * Simple wrapper around FilePickerActivity so that we can switch themes and a
 * hack so that we can tint a menu icon
 * 
 * @author Simon
 *
 */
public class ThemedFilePickerActivity extends FilePickerActivity {
	private static final String DEBUG_TAG = "ThemedFilePickerAct...";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Preferences prefs = new Preferences(this);
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.FilePickerThemeLight);
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem createDirItem = menu.findItem(R.id.nnf_action_createdir);
		if (createDirItem != null) {
			Drawable createDirIcon = DrawableCompat.wrap(createDirItem.getIcon());
			createDirIcon = ThemeUtils.getTintedDrawable(this, createDirIcon, R.attr.colorAccent);
			createDirItem.setIcon(createDirIcon);
		} else {
			Log.d(DEBUG_TAG, "item not found");
		}
		return true;
	}
}
