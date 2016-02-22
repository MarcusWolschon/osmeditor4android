package de.blau.android.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.SherlockDialogFragment;

import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.listener.ConfirmUploadListener;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.listener.DownloadCurrentListener;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog asking for confirmation before starting an activity that might result in data loss.
 *
 */
public class DownloadCurrentWithChanges extends SherlockDialogFragment
{
	
	private static final String DEBUG_TAG = DownloadCurrentWithChanges.class.getSimpleName();
	
	private static final String TAG = "fragment_newversion";
		
	
   	/**
	 
	 */
	static public void showDialog(FragmentActivity activity) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    DownloadCurrentWithChanges downloadCurrentWithChangesFragment = newInstance();
	    if (downloadCurrentWithChangesFragment != null) {
	    	downloadCurrentWithChangesFragment.show(fm, TAG);
	    } else {
	    	Log.e(DEBUG_TAG,"Unable to create new version dialog ");
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
    static private DownloadCurrentWithChanges newInstance() {
    	DownloadCurrentWithChanges f = new DownloadCurrentWithChanges();

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
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
    	Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(),R.attr.alert_dialog));
    	builder.setTitle(R.string.transfer_download_current_dialog_title);
    	builder.setMessage(R.string.transfer_download_current_dialog_message);
    	DoNothingListener doNothingListener = new DoNothingListener();
    	builder.setPositiveButton(R.string.transfer_download_current_upload,
				new ConfirmUploadListener((Main) getActivity()));
    	builder.setNeutralButton(R.string.transfer_download_current_back, doNothingListener);
    	builder.setNegativeButton(R.string.transfer_download_current_download,
				new DownloadCurrentListener((Main) getActivity()));
			
    	return builder.create();
    }	
}
