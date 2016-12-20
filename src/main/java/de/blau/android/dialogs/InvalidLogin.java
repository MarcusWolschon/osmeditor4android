package de.blau.android.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog reporting that the login credentials don't work
 *
 */
public class InvalidLogin extends DialogFragment
{
	
	private static final String DEBUG_TAG = InvalidLogin.class.getSimpleName();
	
	private static final String TAG = "fragment_invalid_login";
		
	
   	/**
	 
	 */
	static public void showDialog(FragmentActivity activity) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    InvalidLogin invalidLoginFragment = newInstance();
	    if (invalidLoginFragment != null) {
	    	invalidLoginFragment.show(fm, TAG);
	    } else {
	    	Log.e(DEBUG_TAG,"Unable to create newbie dialog ");
	    }
	}
	
	static public void dismissDialog(FragmentActivity activity) {
		FragmentManager fm = activity.getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
	    Fragment fragment = fm.findFragmentByTag(TAG);
	    if (fragment != null) {
	        ft.remove(fragment);
	    }
	    ft.commit();
	}
		
    /**
     */
    static private InvalidLogin newInstance() {
    	InvalidLogin f = new InvalidLogin();

        f.setShowsDialog(true);
        
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(DEBUG_TAG, "onAttach");
        if (!(activity instanceof Main)) {
            throw new ClassCastException(activity.toString() + " can ownly be called from Main");
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState)
    {
    	Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(),R.attr.alert_dialog));
    	builder.setTitle(R.string.wrong_login_data_title);
    	builder.setMessage(R.string.wrong_login_data_message);
    	DoNothingListener doNothingListener = new DoNothingListener();
    	builder.setNegativeButton(R.string.cancel, doNothingListener); // logins in the preferences should no longer be used
		final Server server = new Preferences(getActivity()).getServer();
		if (server.getOAuth()) {
			builder.setPositiveButton(R.string.wrong_login_data_re_authenticate, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((Main) getActivity()).oAuthHandshake(server, null);
				}
			});
		}
    	return builder.create();
    }	
}
