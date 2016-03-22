package de.blau.android.dialogs;

import org.acra.ACRA;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.SherlockDialogFragment;

import de.blau.android.R;


public class Progress extends SherlockDialogFragment
{
	
	private static final String DEBUG_TAG = Progress.class.getSimpleName();
	
	public static final int PROGRESS_LOADING = 1;
	
	public static final int PROGRESS_DOWNLOAD = 2;
	
	public static final int PROGRESS_DELETING = 3;
	
	public static final int PROGRESS_SEARCHING = 4;
	
	public static final int PROGRESS_SAVING = 5;
	
	public static final int PROGRESS_OAUTH = 6;
	
	private int titleId;
	private int messageId;
	
	static public void showDialog(FragmentActivity activity, int dialogType) {
		dismissDialog(activity, dialogType);

		FragmentManager fm = activity.getSupportFragmentManager();
	    Progress progressDialogFragment = newInstance(dialogType);
	    try {
	    	if (progressDialogFragment != null) {
	    		progressDialogFragment.show(fm, getTag(dialogType));
	    	} else {
	    		Log.e(DEBUG_TAG,"Unable to create dialog for value " + dialogType);
	    	}
	    } catch (IllegalStateException isex) {
	    	Log.e(DEBUG_TAG,"showDialog",isex);
	    	ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
			ACRA.getErrorReporter().handleException(isex);
	    }
	}
	
	static public void dismissDialog(FragmentActivity activity, int dialogType) {
		FragmentManager fm = activity.getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
	    Fragment fragment = fm.findFragmentByTag(getTag(dialogType));
	    try {
	    	if (fragment != null) {
	    		ft.remove(fragment);
	    	}
	    	ft.commit(); 
	    } catch (IllegalStateException isex) {
	    	Log.e(DEBUG_TAG,"dismissDialog",isex);
	    	ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
			ACRA.getErrorReporter().handleException(isex);
	    }
	}
	
	private static String getTag(int dialogType) {
		switch (dialogType) {
		case PROGRESS_LOADING:
			return "dialog_loading";
		case PROGRESS_DOWNLOAD:
			return "dialog_download";
		case PROGRESS_DELETING:
			return "dialog_deleting";
		case PROGRESS_SEARCHING:
			return "dialog_searching";
		case PROGRESS_SAVING:
			return "dialog_saving";
		case PROGRESS_OAUTH:
			return "dialog_oauth";
		}
		return null;
	}
	
	static private Progress newInstance(int dialogType) {
		switch (dialogType) {
		case PROGRESS_LOADING:
			return createNewInstance(R.string.progress_message);
		case PROGRESS_DOWNLOAD:
			return createNewInstance(R.string.progress_download_message);
		case PROGRESS_DELETING:
			return createNewInstance(R.string.progress_general_title, R.string.progress_deleting_message);
		case PROGRESS_SEARCHING:
			return createNewInstance(R.string.progress_general_title, R.string.progress_searching_message);
		case PROGRESS_SAVING:
			return createNewInstance(R.string.progress_general_title, R.string.progress_saving_message);
		case PROGRESS_OAUTH:
			return createNewInstance(R.string.progress_general_title, R.string.progress_oauth);
		}
		return null;
	}
	
    /**
     */
    static private Progress createNewInstance(final int messageId) {
    	return createNewInstance(R.string.progress_title, messageId);
    }
	
    static private Progress createNewInstance(final int titleId, final int messageId) {
    	Progress f = new Progress();

        Bundle args = new Bundle();
        args.putSerializable("title", titleId);
        args.putSerializable("message", messageId);

        f.setArguments(args);
        f.setShowsDialog(true);
        
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        titleId = (Integer) getArguments().getSerializable("title");
        messageId = (Integer) getArguments().getSerializable("message");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        ProgressDialog dialog = new ProgressDialog(getActivity(), getTheme());
        dialog.setTitle(getString(titleId));
        dialog.setMessage(getString(messageId));
        // dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        return dialog;
    }
}
