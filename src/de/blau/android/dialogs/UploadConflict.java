package de.blau.android.dialogs;

import org.acra.ACRA;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.SherlockDialogFragment;

import de.blau.android.Application;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.UploadResult;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.util.ThemeUtils;

/**
 * Dialog to resolve upload conflicts one by one
 * @author simon
 *
 */
public class UploadConflict extends SherlockDialogFragment
{
	
	private static final String DEBUG_TAG = UploadConflict.class.getSimpleName();
	
	private static final String TAG = "fragment_upload_conflict";
		
	private UploadResult result;
	
	static public void showDialog(FragmentActivity activity, UploadResult result) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    UploadConflict uploadConflictDialogFragment = newInstance(result);
	    if (uploadConflictDialogFragment != null) {
	    	uploadConflictDialogFragment.show(fm, TAG);
	    } else {
	    	Log.e(DEBUG_TAG,"Unable to create dialog for upload conflict " + result);
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
    static private UploadConflict newInstance(final UploadResult result) {
    	UploadConflict f = new UploadConflict();

        Bundle args = new Bundle();
        args.putSerializable("uploadresult", result);

        f.setArguments(args);
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
        result = (UploadResult) getArguments().getSerializable("uploadresult");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
    	Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(),R.attr.alert_dialog));
    	builder.setTitle(R.string.upload_conflict_title);
    	Resources res = getActivity().getResources();
    	final Logic logic = Application.getLogic();
    	final OsmElement elementOnServer = logic.getElement(result.elementType, result.osmId);
    	final OsmElement elementLocal = Application.getDelegator().getOsmElement(result.elementType, result.osmId);
    	final long newVersion;
    	try {
    		boolean useServerOnly = false;
    		if (elementOnServer != null) {
    			if (elementLocal.getState()==OsmElement.STATE_DELETED) {
    				builder.setMessage(res.getString(R.string.upload_conflict_message_referential, elementLocal.getDescription()));
    				useServerOnly = true;
    			} else {
    				builder.setMessage(res.getString(R.string.upload_conflict_message_version, elementLocal.getDescription(), elementLocal.getOsmVersion(), elementOnServer.getOsmVersion()));
    			}
    			newVersion = elementOnServer.getOsmVersion();
    		} else {
    			builder.setMessage(res.getString(R.string.upload_conflict_message_deleted, elementLocal.getDescription(), elementLocal.getOsmVersion()));
    			newVersion = elementLocal.getOsmVersion() + 1;
    		}
    		if (!useServerOnly) {
    			builder.setPositiveButton(R.string.use_local_version, 	new OnClickListener() {
    				@Override
    				public void onClick(DialogInterface dialog, int which) {
    					logic.fixElementWithConflict(newVersion, elementLocal, elementOnServer);
    					((Main)getActivity()).confirmUpload(); // FIXME this should be made independent from Main
    				}
    			});
    		}
    		builder.setNeutralButton(R.string.use_server_version,new OnClickListener() {
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    				StorageDelegator storageDelegator = Application.getDelegator();
    				storageDelegator.removeFromUpload(elementLocal);
    				if (elementOnServer != null) {
    					logic.downloadElement(getActivity(), elementLocal.getName(), elementLocal.getOsmId(), false, true, null);
    				} else { // delete local element
    					logic.updateToDeleted(elementLocal);
    				}
    				if (!storageDelegator.getApiStorage().isEmpty()) {
    					((Main)getActivity()).confirmUpload(); // FIXME this should be made independent from Main
    				}
    			}
    		});
    	} catch (Exception e) {
    		Log.e(DEBUG_TAG,"Caught exacption " + e);
    		ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
			ACRA.getErrorReporter().handleException(e);
    	}
    	builder.setNegativeButton(R.string.cancel, null);

    	return builder.create();
    }	
}
