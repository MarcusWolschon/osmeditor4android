package de.blau.android.util;

import com.nononsenseapps.filepicker.FilePickerActivity;

import android.os.Bundle;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;

/**
 * Simple wrapper around FilePickerActivity so that we can switch themes
 * 
 * @author simon
 *
 */
public class ThemedFilePickerActivity extends FilePickerActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
		Preferences prefs = new Preferences(this);		
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.FilePickerThemeLight);
		}
    	super.onCreate(savedInstanceState);
    }
}
