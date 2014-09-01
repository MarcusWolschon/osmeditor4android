package de.blau.android.prefs;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import de.blau.android.Application;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase.API;
import de.blau.android.prefs.AdvancedPrefDatabase.PresetInfo;
import de.blau.android.prefs.URLListEditActivity.ListEditItem;
import de.blau.android.util.OAuthHelper;

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
	
	private String apiurl, apiname, apiuser, apipass, apipreseturl, apiicons, apioauth;
	private String preseturl, presetname;
	private PresetInfo existingPreset = null;
	private PresetInfo apiPresetInfo = null;
	private String oauth_token, oauth_verifier;
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
	    apioauth   = data.getQueryParameter("apioauth");
	    preseturl  = data.getQueryParameter("preseturl");
	    presetname = data.getQueryParameter("presetname");
	    oauth_token = data.getQueryParameter("oauth_token");
	    oauth_verifier = data.getQueryParameter("oauth_verifier");

	    super.onStart();
	}
	
	@Override
	protected void onResume() {
		Log.i("VespucciURLActivity", "onResume");
	    if ((oauth_token != null) && (oauth_verifier != null)) {
	    	mainView.setVisibility(View.GONE);
	    	Log.i("VespucciURLActivity", "got oauth verifier " + oauth_token + " " + oauth_verifier);
	    	oAuthHandshake(oauth_verifier);
	    	setResult(RESULT_OK);
	    	finish();
	    }
	    else {
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

	    }
		super.onResume();
	}

	@Override
	public void onClick(View v) {
		Intent intent;
		switch (v.getId()) {
		case R.id.urldialog_buttonAddPreset:
			intent = new Intent(this, PresetEditorActivity.class);
			intent.setAction(URLListEditActivity.ACTION_NEW);
			intent.putExtra(URLListEditActivity.EXTRA_NAME, presetname);
			intent.putExtra(URLListEditActivity.EXTRA_VALUE, preseturl);
			startActivityForResult(intent, REQUEST_PRESETEDIT);
			break;
		case R.id.urldialog_buttonAddAPI:
			intent = new Intent(this, APIEditorActivity.class);
			intent.setAction(URLListEditActivity.ACTION_NEW);
			intent.putExtra(URLListEditActivity.EXTRA_NAME, apiname);
			intent.putExtra(URLListEditActivity.EXTRA_VALUE, apiurl);
			startActivityForResult(intent, REQUEST_APIEDIT);
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
				if (apiPresetInfo != null) {
					prefdb.setCurrentAPIPreset(apiPresetInfo.id);
				}
				if (apiicons != null && apiicons.equals("1")) {
					prefdb.setCurrentAPIShowIcons(true);
				}
			}
		}
	}
	
	private void oAuthHandshake(String verifier) {
		String[] s = {verifier};
		AsyncTask<String, Void, Void> loader = new AsyncTask<String, Void, Void>() {
				
			@Override
			protected void onPreExecute() {
			
				Log.d("VespucciURLActivity", "oAuthHandshake onPreExecute");
			}
			
			@Override
			protected Void doInBackground(String... s) {

		    	OAuthHelper oa = new OAuthHelper(); // if we got here it has already been initialized once
		    	try {
					String access[] = oa.getAccessToken(s[0]);
					prefdb.setAPIAccessToken(access[0], access[1]);
				} catch (OAuthMessageSignerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OAuthNotAuthorizedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OAuthExpectationFailedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OAuthCommunicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void v) {
				Log.d("VespucciURLActivity", "oAuthHandshake onPostExecute");
				Application.mainActivity.finishOAuth();
			}
		};
		loader.execute(s);
		try {
			loader.get(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
