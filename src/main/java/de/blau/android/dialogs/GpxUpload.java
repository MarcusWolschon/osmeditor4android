package de.blau.android.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.listener.GpxUploadListener;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog to select a file to upload
 *
 */
public class GpxUpload extends DialogFragment
{
	
	private static final String DEBUG_TAG = GpxUpload.class.getSimpleName();
	
	private static final String TAG = "fragment_gpx_upload";
		
	
   	/**
	 
	 */
	static public void showDialog(FragmentActivity activity) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    GpxUpload gpxUploadFragment = newInstance();
	    if (gpxUploadFragment != null) {
	    	gpxUploadFragment.show(fm, TAG);
	    } else {
	    	Log.e(DEBUG_TAG,"Unable to create gpx upload dialog ");
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
    static private GpxUpload newInstance() {
    	GpxUpload f = new GpxUpload();

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

    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState)
    {
    	final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
    	Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setTitle(R.string.confirm_upload_title);
		DoNothingListener doNothingListener = new DoNothingListener();
		View layout = inflater.inflate(R.layout.upload_gpx, null);
		builder.setView(layout);
		builder.setPositiveButton(R.string.transfer_download_current_upload, new GpxUploadListener((Main) getActivity(), (EditText)layout.findViewById(R.id.upload_gpx_description), 
				(EditText)layout.findViewById(R.id.upload_gpx_tags), (Spinner)layout.findViewById(R.id.upload_gpx_visibility)));
		builder.setNegativeButton(R.string.cancel, doNothingListener);
    	return builder.create();
    }
}
