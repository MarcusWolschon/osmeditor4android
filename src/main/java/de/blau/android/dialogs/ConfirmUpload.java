package de.blau.android.dialogs;

import java.util.List;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.listener.UploadListener;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.FilterlessArrayAdapter;
import de.blau.android.util.ThemeUtils;

/**
 * Dialog for final review of changes and adding comment and source tags before upload
 * 
 * @author simon
 *
 */
public class ConfirmUpload extends DialogFragment
{
	
	private static final String DEBUG_TAG = ConfirmUpload.class.getSimpleName();
	
	private static final String TAG = "fragment_confirm_upload";
	
	private static final char LINE_DELIMITER = '\n';

	static public void showDialog(FragmentActivity activity) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    ConfirmUpload confirmUploadDialogFragment = newInstance();
	    try {
	    	confirmUploadDialogFragment.show(fm, TAG);
	    } catch (IllegalStateException isex) {
	    	Log.e(DEBUG_TAG,"showDialog",isex);
	    	ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
			ACRA.getErrorReporter().handleException(isex);
	    }
	}
	
	static public void dismissDialog(FragmentActivity activity) {
		FragmentManager fm = activity.getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
	    Fragment fragment = fm.findFragmentByTag(TAG);
	    if (fragment != null) {
	        ft.remove(fragment);
	    }
	    try {
	    	ft.commit();
	    } catch (IllegalStateException isex) {
	    	Log.e(DEBUG_TAG,"dismissDialog",isex);
	    	ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
			ACRA.getErrorReporter().handleException(isex);
	    }
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
            throw new ClassCastException(activity.toString() + " can only be called from Main");
        }
    }
    
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @NonNull
	@SuppressLint("InflateParams")
	@Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState)
    {
    	FragmentActivity activity = getActivity();
    	// inflater needs to be got from a themed view or else all our custom stuff will not style correctly
    	final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);
    	DoNothingListener doNothingListener = new DoNothingListener();
    	
    	Builder builder = new AlertDialog.Builder(activity);
    	builder.setIcon(ThemeUtils.getResIdFromAttribute(activity,R.attr.alert_dialog));
    	builder.setTitle(R.string.confirm_upload_title);
    	
		View layout = inflater.inflate(R.layout.upload_comment, null);
		builder.setView(layout);
		TextView changes = (TextView)layout.findViewById(R.id.upload_changes);
		int changeCount = App.getDelegator().getApiElementCount();
		if (changeCount == 1) {
			changes.setText(getString(R.string.confirm_one_upload_text, getPendingChanges(activity)));
		} else {
			changes.setText(getString(R.string.confirm_multiple_upload_text, changeCount, getPendingChanges(activity)));
		}
		CheckBox closeChangeset = (CheckBox)layout.findViewById(R.id.upload_close_changeset);
		closeChangeset.setChecked(new Preferences(activity).closeChangesetOnSave());
		AutoCompleteTextView comment = (AutoCompleteTextView)layout.findViewById(R.id.upload_comment);
        FilterlessArrayAdapter<String> commentAdapter = new FilterlessArrayAdapter<String>(activity,
                android.R.layout.simple_dropdown_item_1line, App.getLogic().getLastComments());
        comment.setAdapter(commentAdapter);
		String lastComment = App.getLogic().getLastComment();
		comment.setText(lastComment == null?"":lastComment);
		OnClickListener autocompleteOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v.hasFocus()) {
					((AutoCompleteTextView)v).showDropDown();
				}
			}
		};
		comment.setOnClickListener(autocompleteOnClick);
		comment.setThreshold(1);
		comment.setOnKeyListener(new MyKeyListener());
		
		AutoCompleteTextView source = (AutoCompleteTextView)layout.findViewById(R.id.upload_source);
		FilterlessArrayAdapter<String> sourceAdapter = new FilterlessArrayAdapter<String>(activity,
                android.R.layout.simple_dropdown_item_1line, App.getLogic().getLastSources());
        source.setAdapter(sourceAdapter);
		String lastSource = App.getLogic().getLastSource();
		source.setText(lastSource == null?"":lastSource);
		source.setOnClickListener(autocompleteOnClick);
		source.setThreshold(1);
		source.setOnKeyListener(new MyKeyListener());
		
		builder.setPositiveButton(R.string.transfer_download_current_upload, 
				new UploadListener((Main) activity, comment, source, closeChangeset));
		builder.setNegativeButton(R.string.no, doNothingListener);

    	return builder.create();
    }	
    
	/**
	 * @return a list of all pending changes to upload (contains newlines)
	 */
	public String getPendingChanges(Context ctx) {
		List<String> changes = App.getLogic().getPendingChanges(ctx);
		StringBuilder builder = new StringBuilder();
		for (String change : changes) {
			builder.append(change).append(LINE_DELIMITER);
		}
		return builder.toString();
	}
    
	/**
	 * For whatever reason the softkeyboard doesn't work as expected with AutoCompleteTextViews
	 * This listener simply moves focus to the next view below on enter being pressed or dismisses the keyboard
	 */
	private class MyKeyListener implements OnKeyListener {
		@Override
		public boolean onKey(final View view, final int keyCode, final KeyEvent keyEvent) {
			if (keyEvent.getAction() == KeyEvent.ACTION_UP || keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE) {
				if (view instanceof EditText) {
					if (keyCode == KeyEvent.KEYCODE_ENTER) {
						View nextView = view.focusSearch(View.FOCUS_DOWN);
						if (nextView != null && nextView.isFocusable()) {
							nextView.requestFocus();
							return true;
						} else {
							if(view instanceof AutoCompleteTextView) {
								((AutoCompleteTextView)view).dismissDropDown();
							}
							InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
						    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
						}
					}
				}
			}
			return false;
		}
	}
}
