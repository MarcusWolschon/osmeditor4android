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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;

import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.listener.UploadListener;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ThemeUtils;

/**
 * Dialog to resolve upload conflicts one by one
 * @author simon
 *
 */
public class ConfirmUpload extends SherlockDialogFragment
{
	
	private static final String DEBUG_TAG = ConfirmUpload.class.getSimpleName();
	
	private static final String TAG = "fragment_confirm_upload";
	
	static public void showDialog(FragmentActivity activity) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    ConfirmUpload uploadConflictDialogFragment = newInstance();
	    if (uploadConflictDialogFragment != null) {
	    	uploadConflictDialogFragment.show(fm, TAG);
	    } else {
	    	Log.e(DEBUG_TAG,"Unable to create dialog for upload confirmation");
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
    static private ConfirmUpload newInstance() {
    	ConfirmUpload f = new ConfirmUpload();
    	
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
    	// inflater needs to be got from a themed view or else all our custom stuff will not style correctly
    	final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
    	DoNothingListener doNothingListener = new DoNothingListener();
    	
    	Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(),R.attr.alert_dialog));
    	builder.setTitle(R.string.confirm_upload_title);
    	
		View layout = inflater.inflate(R.layout.upload_comment, null);
		builder.setView(layout);
		TextView changes = (TextView)layout.findViewById(R.id.upload_changes);
		changes.setText(getString(R.string.confirm_upload_text, ((Main) getActivity()).getPendingChanges()));
		CheckBox closeChangeset = (CheckBox)layout.findViewById(R.id.upload_close_changeset);
		closeChangeset.setChecked(new Preferences(getActivity()).closeChangesetOnSave());
		builder.setPositiveButton(R.string.transfer_download_current_upload, 
				new UploadListener((Main) getActivity(), (EditText)layout.findViewById(R.id.upload_comment), 
					(EditText)layout.findViewById(R.id.upload_source), closeChangeset));
		builder.setNegativeButton(R.string.no, doNothingListener);

    	return builder.create();
    }	
}
