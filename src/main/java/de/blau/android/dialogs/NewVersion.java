package de.blau.android.dialogs;

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
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;

/**
 * Display a dialog asking for confirmation before starting an activity that might result in data loss.
 *
 */
public class NewVersion extends DialogFragment
{
	
	private static final String DEBUG_TAG = NewVersion.class.getSimpleName();
	
	private static final String TAG = "fragment_newversion";
		
	
   	/**
	 
	 */
	static public void showDialog(FragmentActivity activity) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    NewVersion newVersionFragment = newInstance();
	    if (newVersionFragment != null) {
	    	newVersionFragment.show(fm, TAG);
	    } else {
	    	Log.e(DEBUG_TAG,"Unable to create new version dialog ");
	    }
	}
	
	private static void dismissDialog(FragmentActivity activity) {
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
    static private NewVersion newInstance() {
    	NewVersion f = new NewVersion();

        f.setShowsDialog(true);
        
        return f;
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
    	// builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(),R.attr.alert_dialog));
    	builder.setTitle(R.string.upgrade_title);
    	builder.setMessage(R.string.upgrade_message);
    	DoNothingListener doNothingListener = new DoNothingListener();
    	builder.setNegativeButton(R.string.cancel, doNothingListener);
    	builder.setNeutralButton(R.string.read_upgrade, 	new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						HelpViewer.start(getActivity(), R.string.help_upgrade);
					}
				});
    	return builder.create();
    }	
}
