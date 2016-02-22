package de.blau.android.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockDialogFragment;

import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.contract.Paths;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog asking for a file name to save to
 *
 */
public class SaveFile extends SherlockDialogFragment
{
	
	private static final String DEBUG_TAG = SaveFile.class.getSimpleName();
	
	private static final String TAG = "fragment_save_file";
		
	
   	/**
	 
	 */
	static public void showDialog(FragmentActivity activity) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    SaveFile saveFileFragment = newInstance();
	    if (saveFileFragment != null) {
	    	saveFileFragment.show(fm, TAG);
	    } else {
	    	Log.e(DEBUG_TAG,"Unable to create save file dialog ");
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
    static private SaveFile newInstance() {
    	SaveFile f = new SaveFile();

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
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
    	final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
    	Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setTitle(R.string.save_file);
    	LinearLayout searchLayout = (LinearLayout) inflater.inflate(R.layout.save_file, null);
    	builder.setView(searchLayout);
		final EditText saveFileEdit = (EditText) searchLayout.findViewById(R.id.save_file_edit);
		builder.setNegativeButton(R.string.cancel, null);
		builder.setPositiveButton(R.string.save, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// FIXME instead of hardcoding the directory, this should be the default and alternatives selectable by the user
				Main.getLogic().writeOsmFile(Environment.getExternalStorageDirectory().getPath() + "/" + Paths.DIRECTORY_PATH_VESPUCCI + "/" + saveFileEdit.getText().toString());
			}
		});
    	return builder.create();
    }
}
