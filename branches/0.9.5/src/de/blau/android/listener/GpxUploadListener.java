package de.blau.android.listener;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import de.blau.android.Main;
import de.blau.android.osm.Server.Visibility;

/**
 * @author mb
 */
public class GpxUploadListener implements OnClickListener {
	
	private final Main caller;
	private final EditText descriptionField;
	private final EditText tagsField;
	private final Spinner visibilitySpinner;
	
	/**
	 * @param caller
	 * @param closeChangeset TODO
	 */
	public GpxUploadListener(final Main caller, final EditText descriptionField, final EditText tagsField, final Spinner visibilitySpinner) {
		this.caller = caller;
		this.descriptionField = descriptionField;
		this.tagsField = tagsField;
		this.visibilitySpinner = visibilitySpinner;
	}
	
	/**
	 * note: the current code will only work if the string array in strings.xml is not changed
	 */
	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		Visibility visibility = Visibility.PRIVATE;
		switch (visibilitySpinner.getSelectedItemPosition()) {
		case 0:
			visibility = Visibility.PRIVATE;
			break;
		case 1:
			visibility = Visibility.PUBLIC;
			break;
		case 2:
			visibility = Visibility.TRACKABLE;
			break;
		case 3:
			visibility = Visibility.IDENTIFIABLE;
			break;
		}
		caller.performTrackUpload(descriptionField.getText().toString(), tagsField.getText().toString(), visibility);
	}
	
}
