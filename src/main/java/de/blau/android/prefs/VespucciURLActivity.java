package de.blau.android.prefs;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.acra.ACRA;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase.PresetInfo;
import de.blau.android.prefs.URLListEditActivity.ListEditItem;
import de.blau.android.util.OAuthHelper;
import oauth.signpost.exception.OAuthException;

/**
 * Will process vespucci:// URLs.
 * Accepts the following URL parameters:<br>
 *   apiurl - API URL<br>
 *   apiname - name for the API (if it gets added)<br>
 *   apiuser, apipass - login data for the API (if it gets added)<br>
 *   apipreseturl - preset URL to be set for the API after adding (only if present!)<br>
 *   apiicons - set to 1 if icons should be shown<br>
 *   Note the above are no longer used.
 *   preseturl - preset URL to add to the preset list<br>
 *   presetname - name for the preset (if it gets added)<br>
 *   oauth_token = oauth token, used during retrieving oauth access tokens<br>
 *   oauth_verifier - oauth verifier, used during retrieving oauth access tokens<br>
 * @author Jan
 *
 */
public class VespucciURLActivity extends Activity implements OnClickListener {
	private static final String DEBUG_TAG = "VespucciURLActivity";
	private static final int REQUEST_PRESETEDIT = 0;
	private static final int REQUEST_APIEDIT = 1;
	
	private String command;
	private String apiurl, apiname, apiuser, apipass, apipreseturl, apiicons, apioauth;
	private String preseturl, presetname;
	private PresetInfo existingPreset = null;
	private PresetInfo apiPresetInfo = null;
	private String oauth_token, oauth_verifier;
	private AdvancedPrefDatabase prefdb;
	private boolean downloadSucessful = false;
	
	private View mainView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Preferences prefs = new Preferences(this);
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.Theme_customMain_Light);
		} else {
			setTheme(R.style.Theme_customMain);
		}
		super.onCreate(savedInstanceState);
		mainView = View.inflate(this, R.layout.url_activity, null);
		setContentView(mainView);
	    prefdb = new AdvancedPrefDatabase(this);
	}
	
	@Override
	protected void onStart() {
		Uri data = getIntent().getData();
		if (data != null) {
			try {
				command = data.getPath();
				Log.d(DEBUG_TAG,"Command " + command);
				apiurl     = data.getQueryParameter("apiurl");
				apiname    = data.getQueryParameter("apiname");
				apiuser    = data.getQueryParameter("apiuser");
				apipass    = data.getQueryParameter("apipass");
				apipreseturl  = data.getQueryParameter("apipreset");
				apiicons   = data.getQueryParameter("apiicons");
				apioauth   = data.getQueryParameter("apioauth");
				preseturl  = data.getQueryParameter("preseturl");
				presetname = data.getQueryParameter("presetname");
				oauth_token = data.getQueryParameter("oauth_token");
				oauth_verifier = data.getQueryParameter("oauth_verifier");
			} catch (Exception ex) {
				Log.e(DEBUG_TAG,"Uri " + data + " caused " + ex);
				ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
				ACRA.getErrorReporter().handleException(ex);
				finish();
			}
		} else {
			Log.e(DEBUG_TAG,"Received null Uri, ignoring");
		}
	    super.onStart();
	}
	
	@Override
	protected void onResume() {
		Log.i(DEBUG_TAG, "onResume");
		// determining what activity to do based purely on the parameters is rather hackish
	    if ((oauth_token != null) && (oauth_verifier != null)) {
	    	mainView.setVisibility(View.GONE);
	    	Log.i(DEBUG_TAG, "got oauth verifier " + oauth_token + " " + oauth_verifier);
	    	String errorMessage = null;
	    	try {
				oAuthHandshake(oauth_verifier);
			} catch (OAuthException e) {
				errorMessage = OAuthHelper.getErrorMessage(this, e);		
			} catch (InterruptedException e) {
				errorMessage = getString(R.string.toast_oauth_communication);
			} catch (ExecutionException e) {
				errorMessage = getString(R.string.toast_oauth_communication);
			} catch (TimeoutException e) {
				errorMessage = getString(R.string.toast_oauth_timeout);
			}
	    	if (errorMessage != null) {
	    		Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
	    	}
	    	setResult(RESULT_OK);
	    	finish();
	    }
	    else {
			mainView.findViewById(R.id.urldialog_nodata).setVisibility(preseturl == null && apiurl == null ? View.VISIBLE : View.GONE);
			
			if (preseturl != null) {
				mainView.findViewById(R.id.urldialog_layoutPreset).setVisibility(View.VISIBLE);
				mainView.findViewById(R.id.urldialog_layoutAPI).setVisibility(View.GONE);
		    
		    	((TextView)mainView.findViewById(R.id.urldialog_textPresetName)).setText(presetname);
		    	((TextView)mainView.findViewById(R.id.urldialog_textPresetURL)).setText(preseturl);
		    	existingPreset = prefdb.getPresetByURL(preseturl);
		    	if (downloadSucessful) {
		    		mainView.findViewById(R.id.urldialog_textPresetSuccessful).setVisibility(View.VISIBLE);
		    		mainView.findViewById(R.id.urldialog_textPresetExists).setVisibility(View.GONE);
		    	} else {
		    		mainView.findViewById(R.id.urldialog_textPresetExists).setVisibility(existingPreset != null ? View.VISIBLE : View.GONE);
		    		mainView.findViewById(R.id.urldialog_textPresetSuccessful).setVisibility(View.GONE);
		    	}
		    	mainView.findViewById(R.id.urldialog_checkboxEnable).setVisibility(existingPreset == null ? View.VISIBLE : View.GONE);
		    	mainView.findViewById(R.id.urldialog_buttonAddPreset).setVisibility(existingPreset == null ? View.VISIBLE : View.GONE);
		    	((Button)mainView.findViewById(R.id.urldialog_buttonAddPreset)).setOnClickListener(this);
		    } else if (apiurl != null) {
		    	mainView.findViewById(R.id.urldialog_layoutAPI).setVisibility(View.VISIBLE);
		    	mainView.findViewById(R.id.urldialog_layoutPreset).setVisibility(View.GONE);
		    
		    	((TextView)mainView.findViewById(R.id.urldialog_textAPIName)).setText(apiname);
		    	((TextView)mainView.findViewById(R.id.urldialog_textAPIURL)).setText(apiurl);
		    	boolean hasAPI = false;
		    	for (API api : prefdb.getAPIs()) {
		    		if (api.url.equals(apiurl)) {
		    			hasAPI = true;
		    			break;
		    		}
		    	}
		    	if (downloadSucessful) {
		    		mainView.findViewById(R.id.urldialog_textAPISuccessful).setVisibility(View.VISIBLE);
		    		mainView.findViewById(R.id.urldialog_textAPIExists).setVisibility(View.GONE);
		    	} else {
		    		mainView.findViewById(R.id.urldialog_textAPIExists).setVisibility(hasAPI ? View.VISIBLE : View.GONE);
		    		mainView.findViewById(R.id.urldialog_textAPISuccessful).setVisibility(View.GONE);
		    	}
		    	if (apipreseturl != null) {
		    		apiPresetInfo = prefdb.getPresetByURL(apipreseturl);
			    	mainView.findViewById(R.id.urldialog_textAPIPresetMissing).setVisibility(apiPresetInfo == null? View.VISIBLE : View.GONE);
		    	} else {
			    	mainView.findViewById(R.id.urldialog_textAPIPresetMissing).setVisibility(View.GONE);
			    	apiPresetInfo = null;
		    	}
		    	((Button)mainView.findViewById(R.id.urldialog_buttonAddAPI)).setOnClickListener(this);
		    }
	    }
		super.onResume();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.urldialog_buttonAddPreset:
			CheckBox enableCheckBox = (CheckBox)mainView.findViewById(R.id.urldialog_checkboxEnable);
			boolean enable = enableCheckBox != null && enableCheckBox.isChecked();
			PresetEditorActivity.startForResult(this, presetname, preseturl, enable, REQUEST_PRESETEDIT);
			break;
		case R.id.urldialog_buttonAddAPI:
			APIEditorActivity.startForResult(this, apiname, apiurl, REQUEST_APIEDIT);
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_APIEDIT && resultCode == RESULT_OK) {
			ListEditItem item = (ListEditItem)data.getExtras().get(URLListEditActivity.EXTRA_ITEM);
			if (item != null) {
				prefdb.selectAPI(item.id);
				if (apiuser != null && !apiuser.equals("")) {
					prefdb.setCurrentAPILogin(apiuser, apipass == null ? "" : apipass);
				}
			}
			downloadSucessful = true;
		} else if (requestCode == REQUEST_PRESETEDIT && resultCode == RESULT_OK) {
			downloadSucessful = true;
		}
	}
	
	private void oAuthHandshake(String verifier) throws OAuthException, InterruptedException, ExecutionException, TimeoutException {
		String[] s = {verifier};
		class MyTask extends AsyncTask<String, Void, Boolean> {
			private OAuthException ex = null;
			
			@Override
			protected Boolean doInBackground(String... s) {
		    	OAuthHelper oa = new OAuthHelper(); // if we got here it has already been initialized once
		    	try {
					String access[] = oa.getAccessToken(s[0]);
					prefdb.setAPIAccessToken(access[0], access[1]);
				} catch (OAuthException e) {
					Log.d("VespucciURL", "oAuthHandshake: " + e);
					ex = e;
					return false;
				} 
				return true;
			}
			
			@Override
			protected void onPostExecute(Boolean success) {
				Log.d(DEBUG_TAG, "oAuthHandshake onPostExecute");
				// FIXME this is fundamentally broken and needs to be re-thought
				if (App.mainActivity != null) {
					App.mainActivity.finishOAuth();
				}
			}
			
			OAuthException getException() {
				return ex;
			}
		}

		MyTask loader = new MyTask();
		loader.execute(s);
		if (!loader.get(60, TimeUnit.SECONDS)) {
			OAuthException ex = loader.getException();
			if (ex != null) {
				throw ex;
			}
		}
	}
}
