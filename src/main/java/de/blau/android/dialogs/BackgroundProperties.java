package de.blau.android.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog allowing the user to change some properties of the current background
 *
 */
public class BackgroundProperties extends DialogFragment
{
	
	private static final String DEBUG_TAG = BackgroundProperties.class.getSimpleName();
	
	private static final String TAG = "fragment_background_properties";
		
	
   	/**
	 
	 */
	static public void showDialog(FragmentActivity activity) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    BackgroundProperties backgroundPropertiesFragment = newInstance();
	    if (backgroundPropertiesFragment != null) {
	    	backgroundPropertiesFragment.show(fm, TAG);
	    } else {
	    	Log.e(DEBUG_TAG,"Unable to create background properties dialog ");
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
    static private BackgroundProperties newInstance() {
    	BackgroundProperties f = new BackgroundProperties();

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
	@SuppressLint("InflateParams")
	@Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState)
    {
    	Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setTitle(R.string.menu_tools_background_properties);
    	
    	final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
		DoNothingListener doNothingListener = new DoNothingListener();
		
		View layout = inflater.inflate(R.layout.background_properties, null);
		builder.setView(layout);
		builder.setPositiveButton(R.string.okay, doNothingListener);
		SeekBar seeker = (SeekBar) layout.findViewById(R.id.background_contrast_seeker);
		seeker.setOnSeekBarChangeListener(createSeekBarListener());

    	return builder.create();
    }	
	
	private OnSeekBarChangeListener createSeekBarListener() {
		return new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(final SeekBar seekBar, int progress, final boolean fromTouch) {
				Map map = ((Main) getActivity()).getMap();
				map.getOpenStreetMapTilesOverlay().setContrast(progress/127.5f - 1f); // range from -1 to +1
				map.invalidate();
			}
			
			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
			}
			
			@Override
			public void onStopTrackingTouch(final SeekBar arg0) {
			}
		};
	}
}
