package de.blau.android.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog giving new users minimal instructions
 *
 */
public class Newbie extends DialogFragment
{
	
	private static final String DEBUG_TAG = Newbie.class.getSimpleName();
	
	private static final String TAG = "fragment_newbie";
		
	
   	/**
	 
	 */
	static public void showDialog(FragmentActivity activity) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    Newbie newbieFragment = newInstance();
	    if (newbieFragment != null) {
	    	newbieFragment.show(fm, TAG);
	    } else {
	    	Log.e(DEBUG_TAG,"Unable to create newbie dialog ");
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
    static private Newbie newInstance() {
    	Newbie f = new Newbie();

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

    @NonNull
	@Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState)
    {
    	Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(),R.attr.alert_dialog));
    	builder.setTitle(R.string.welcome_title);
    	builder.setMessage(R.string.welcome_message);
    	builder.setPositiveButton(R.string.okay, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Main main = (Main) getActivity();
						if (main != null) {
							main.gotoBoxPicker();
						} else {
							//FIXME do something intelligent here
							Log.e(DEBUG_TAG,"getActivity returned null in onClick");
						}
					}
				});
    	builder.setNeutralButton(R.string.read_introduction, 	new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Context context = getActivity();
						if (context != null) {
							HelpViewer.start(context, R.string.help_introduction);
						} else {
							//FIXME do something intelligent here
							Log.e(DEBUG_TAG,"getActivity returned null in onClick");
						}
					}
				});

    	return builder.create();
    }	
}
