package de.blau.android.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.AdvancedPrefDatabase.Geocoder;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.Search;
import de.blau.android.util.Search.SearchResult;
import de.blau.android.util.SearchItemFoundCallback;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog asking for  a search string that is then found with nominatim
 *
 */
public class SearchForm extends DialogFragment
{
    private static final String DEBUG_TAG = SearchForm.class.getSimpleName();
    
	private static final String BBOX_KEY = "bbox";

	private static final String TAG = "fragment_search_form";

    private BoundingBox bbox;
	private SearchItemFoundCallback callback;
	
   	/**
	 *
	 */
	static public void showDialog(AppCompatActivity activity, final BoundingBox bbox) {
		dismissDialog(activity);
		try {
			FragmentManager fm = activity.getSupportFragmentManager();
			SearchForm searchFormFragment = newInstance(bbox);
			searchFormFragment.show(fm, TAG);
		} catch (IllegalStateException isex) {
			Log.e(DEBUG_TAG,"showDialog",isex);
		}
	}

	private static void dismissDialog(AppCompatActivity activity) {
		try {
			FragmentManager fm = activity.getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			Fragment fragment = fm.findFragmentByTag(TAG);
			if (fragment != null) {
				ft.remove(fragment);
			}
			ft.commit();
		} catch (IllegalStateException isex) {
			Log.e(DEBUG_TAG,"dismissDialog",isex);
		}
	}

    /**
     */
    static private SearchForm newInstance(final BoundingBox bbox) {
    	SearchForm f = new SearchForm();

        Bundle args = new Bundle();
		args.putSerializable(BBOX_KEY, bbox);

        f.setArguments(args);
        f.setShowsDialog(true);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setCancelable(true);

        bbox = (BoundingBox) getArguments().getSerializable(BBOX_KEY);
    }
    
    @Override
    public void onAttach(Activity activity) {
        Log.d(DEBUG_TAG, "onAttach");
        super.onAttach(activity);
        try {
            callback = (SearchItemFoundCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ");
        }
    }

    @NonNull
	@Override
    @SuppressLint("InflateParams")
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState)
    {
    	final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
    	LinearLayout searchLayout = (LinearLayout) inflater.inflate(R.layout.query_entry, null);

    	Builder searchBuilder = new AlertDialog.Builder(getActivity());
    	// builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(),R.attr.alert_dialog));
    	searchBuilder.setTitle(R.string.menu_find);
    	searchBuilder.setMessage(R.string.find_message);
    	searchBuilder.setView(searchLayout);
    	
    	final EditText searchEdit = (EditText) searchLayout.findViewById(R.id.location_search_edit);
    	searchEdit.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
    	
    	final Spinner searchGeocoder = (Spinner) searchLayout.findViewById(R.id.location_search_geocoder);
    	AdvancedPrefDatabase db = new AdvancedPrefDatabase(getActivity());
    	final Geocoder[] geocoders = db.getActiveGeocoders();
    	String[] geocoderNames = new String[geocoders.length];
    	for (int i=0;i<geocoders.length;i++) {
    		geocoderNames[i] = geocoders[i].name; 
    	}
    	ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getActivity(), android.R.layout.simple_spinner_item, geocoderNames);
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	searchGeocoder.setAdapter(adapter);
    	final Preferences prefs = new Preferences(getActivity());
    	searchGeocoder.setSelection(prefs.getGeocoder());
      	searchGeocoder.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				prefs.setGeocoder(pos);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}});	
    	searchBuilder.setPositiveButton(R.string.search, null);
    	searchBuilder.setNegativeButton(R.string.cancel, null);

    	final AppCompatDialog searchDialog = searchBuilder.create();

    	searchDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positive = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                positive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        searchEdit.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)); // emulate pressing the enter button
                    }
                });
            }
    	});
    	
    	/*
    	 * NOTE this is slightly hackish but needed to ensure the original dialog (this) gets dismissed 
    	 */
    	final SearchItemFoundCallback realCallback = new SearchItemFoundCallback() {
			private static final long serialVersionUID = 1L;

			@Override
			public void onItemFound(SearchResult sr) {
				searchDialog.dismiss();
				callback.onItemFound(sr);
			}
    	};

    	searchEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
    		@Override
    		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
    			if (actionId == EditorInfo.IME_ACTION_SEARCH
    					|| (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
    				Search search = new Search((AppCompatActivity) getActivity(), realCallback);
    				search.find(geocoders[searchGeocoder.getSelectedItemPosition()],v.getText().toString(),bbox);
    			}
    			return false;
    		}
    	});
    	
    	return searchDialog;
    }	
    
    @Override
    public void onStart() {
    	super.onStart();
       	getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
}
