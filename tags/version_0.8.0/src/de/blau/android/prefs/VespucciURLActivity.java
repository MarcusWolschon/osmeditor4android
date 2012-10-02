package de.blau.android.prefs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase.API;
import de.blau.android.prefs.AdvancedPrefDatabase.PresetInfo;
import de.blau.android.prefs.URLListEditActivity.ListEditItem;

/**
 * Will process vespucci:// URLs.
 * Accepts the following URL parameters:<br>
 *   apiurl - API URL<br>
 *   apiname - name for the API (if it gets added)<br>
 *   apiuser, apipass - login data for the API (if it gets added)<br>
 *   apipreseturl - preset URL to be set for the API after adding (only if present!)<br>
 *   apiicons - set to 1 if icons should be shown<br>
 *   preseturl - preset URL to add to the preset list<br>
 *   presetname - name for the preset (if it gets added)<br>
 * @author Jan
 *
 */
public class VespucciURLActivity extends Activity implements OnClickListener {
	private static final int REQUEST_PRESETEDIT = 0;
	private static final int REQUEST_APIEDIT = 1;
	
	private String apiurl, apiname, apiuser, apipass, apipreseturl, apiicons;
	private String preseturl, presetname;
	private PresetInfo existingPreset = null;
	private PresetInfo apiPresetInfo = null;
	AdvancedPrefDatabase prefdb;
	
	private View mainView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainView = View.inflate(this, R.layout.url_activity, null);
		setContentView(mainView);
	    prefdb = new AdvancedPrefDatabase(this);
	}
	
	@Override
	protected void onStart() {
		Uri data = getIntent().getData(); 
	    apiurl     = data.getQueryParameter("apiurl");
	    apiname    = data.getQueryParameter("apiname");
	    apiuser    = data.getQueryParameter("apiuser");
	    apipass    = data.getQueryParameter("apipass");
	    apipreseturl  = data.getQueryParameter("apipreset");
	    apiicons   = data.getQueryParameter("apiicons");
	    preseturl  = data.getQueryParameter("preseturl");
	    presetname = data.getQueryParameter("presetname");
	    
	    super.onStart();
	}
	
	@Override
	protected void onResume() {
		Log.i("VespucciURLActivity", "onResume");
		mainView.findViewById(R.id.urldialog_nodata).setVisibility(preseturl == null && apiurl == null ? View.VISIBLE : View.GONE);
		
    	mainView.findViewById(R.id.urldialog_layoutPreset).setVisibility(preseturl != null ? View.VISIBLE : View.GONE);
	    if (preseturl != null) {
	    	((TextView)mainView.findViewById(R.id.urldialog_textPresetName)).setText(presetname);
	    	((TextView)mainView.findViewById(R.id.urldialog_textPresetURL)).setText(preseturl);
	    	existingPreset = prefdb.getPresetByURL(preseturl);
	    	mainView.findViewById(R.id.urldialog_textPresetExists).setVisibility(existingPreset != null ? View.VISIBLE : View.GONE);
	    	mainView.findViewById(R.id.urldialog_buttonAddPreset).setVisibility(existingPreset == null ? View.VISIBLE : View.GONE);
	    }
	    
    	mainView.findViewById(R.id.urldialog_layoutAPI).setVisibility(apiurl != null ? View.VISIBLE : View.GONE);
	    if (apiurl != null) {
	    	((TextView)mainView.findViewById(R.id.urldialog_textAPIName)).setText(apiname);
	    	((TextView)mainView.findViewById(R.id.urldialog_textAPIURL)).setText(apiurl);
	    	boolean hasAPI = false;
	    	for (API api : prefdb.getAPIs()) {
	    		if (api.url.equals(apiurl)) {
	    			hasAPI = true;
	    			break;
	    		}
	    	}
	    	mainView.findViewById(R.id.urldialog_textAPIExists).setVisibility(hasAPI ? View.VISIBLE : View.GONE);
	    	if (apipreseturl != null) {
	    		apiPresetInfo = prefdb.getPresetByURL(apipreseturl);
		    	mainView.findViewById(R.id.urldialog_textAPIPresetMissing).setVisibility(apiPresetInfo == null? View.VISIBLE : View.GONE);
	    	} else {
		    	mainView.findViewById(R.id.urldialog_textAPIPresetMissing).setVisibility(View.GONE);
		    	apiPresetInfo = null;
	    	}
	    }
	    
    	((Button)mainView.findViewById(R.id.urldialog_buttonAddPreset)).setOnClickListener(this);
    	((Button)mainView.findViewById(R.id.urldialog_buttonAddAPI)).setOnClickListener(this);

	    
		super.onResume();
	}

	@Override
	public void onClick(View v) {
		Intent intent;
		switch (v.getId()) {
		case R.id.urldialog_buttonAddPreset:
			intent = new Intent(this, PresetEditorActivity.class);
			intent.setAction(PresetEditorActivity.ACTION_NEW);
			intent.putExtra(PresetEditorActivity.EXTRA_NAME, presetname);
			intent.putExtra(PresetEditorActivity.EXTRA_VALUE, preseturl);
			startActivityForResult(intent, REQUEST_PRESETEDIT);
			break;
		case R.id.urldialog_buttonAddAPI:
			intent = new Intent(this, APIEditorActivity.class);
			intent.setAction(PresetEditorActivity.ACTION_NEW);
			intent.putExtra(PresetEditorActivity.EXTRA_NAME, apiname);
			intent.putExtra(PresetEditorActivity.EXTRA_VALUE, apiurl);
			startActivityForResult(intent, REQUEST_APIEDIT);
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_APIEDIT && resultCode == RESULT_OK) {
			ListEditItem item = (ListEditItem)data.getExtras().get(APIEditorActivity.EXTRA_ITEM);
			if (item != null) {
				prefdb.selectAPI(item.id);
				if (apiuser != null && !apiuser.equals("")) {
					prefdb.setCurrentAPILogin(apiuser, apipass == null ? "" : apipass);
				}
				if (apiPresetInfo != null) {
					prefdb.setCurrentAPIPreset(apiPresetInfo.id);
				}
				if (apiicons != null && apiicons.equals("1")) {
					prefdb.setCurrentAPIShowIcons(true);
				}
			}
		}
	}
}
