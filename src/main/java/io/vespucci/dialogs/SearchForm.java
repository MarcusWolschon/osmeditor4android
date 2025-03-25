package io.vespucci.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentManager;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.geocode.Search;
import io.vespucci.geocode.SearchItemSelectedCallback;
import io.vespucci.osm.ViewBox;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.prefs.AdvancedPrefDatabase.Geocoder;
import io.vespucci.util.ImmersiveDialogFragment;
import io.vespucci.util.ThemeUtils;

/**
 * Display a dialog asking for a search string that is then found with nominatim
 *
 */
public class SearchForm extends ImmersiveDialogFragment {
    private static final String DEBUG_TAG = SearchForm.class.getSimpleName().substring(0, Math.min(23, SearchForm.class.getSimpleName().length()));

    private static final String BBOX_KEY = "bbox";

    private static final String TAG = "fragment_search_form";

    private ViewBox                    bbox;
    private SearchItemSelectedCallback callback;

    /**
     * Display a dialog asking for a search term and allowing selection of geocoders
     * 
     * @param activity the calling FragmentActivity
     * @param bbox a BoundingBox to restrict the query to if null the whole world is considered
     */
    public static void showDialog(@NonNull AppCompatActivity activity, @Nullable final ViewBox bbox) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            SearchForm searchFormFragment = newInstance(bbox);
            searchFormFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the Dialog
     * 
     * @param activity the calling FragmentActivity
     */
    private static void dismissDialog(@NonNull AppCompatActivity activity) {
        io.vespucci.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create new instance of this object
     * 
     * @param bbox a BoundingBox to restrict the query to if null the whole world is considered
     * @return a SearchForm instance
     */
    private static SearchForm newInstance(@Nullable final ViewBox bbox) {
        SearchForm f = new SearchForm();

        Bundle args = new Bundle();
        args.putSerializable(BBOX_KEY, bbox);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            bbox = io.vespucci.util.Util.getSerializeable(savedInstanceState, BBOX_KEY, ViewBox.class);
        } else {
            bbox = io.vespucci.util.Util.getSerializeable(getArguments(), BBOX_KEY, ViewBox.class);
        }
    }

    @Override
    public void onAttach(Context context) {
        Log.d(DEBUG_TAG, "onAttach");
        super.onAttach(context);
        try {
            callback = (SearchItemSelectedCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ");
        }
    }

    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        LinearLayout searchLayout = (LinearLayout) inflater.inflate(R.layout.query_entry, null);

        Builder searchBuilder = new AlertDialog.Builder(getActivity());
        searchBuilder.setTitle(R.string.menu_find);
        searchBuilder.setMessage(R.string.find_message);
        searchBuilder.setView(searchLayout);

        final EditText searchEdit = (EditText) searchLayout.findViewById(R.id.location_search_edit);
        searchEdit.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        final Spinner searchGeocoder = (Spinner) searchLayout.findViewById(R.id.location_search_geocoder);
        final CheckBox limitSearch = searchLayout.findViewById(R.id.location_search_limit);
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(getActivity())) {
            final Geocoder[] geocoders = db.getActiveGeocoders();
            String[] geocoderNames = new String[geocoders.length];
            for (int i = 0; i < geocoders.length; i++) {
                geocoderNames[i] = geocoders[i].name;
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, geocoderNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            searchGeocoder.setAdapter(adapter);
            final Preferences prefs = App.getPreferences(getActivity());
            int geocoderIndex = prefs.getGeocoder();
            // if a non-active geocoder is selected revert to default
            if (geocoderIndex > adapter.getCount() - 1) {
                geocoderIndex = 0;
                prefs.setGeocoder(geocoderIndex);
            }
            searchGeocoder.setSelection(geocoderIndex);
            searchGeocoder.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                    prefs.setGeocoder(pos);
                    if (geocoders[pos].type == AdvancedPrefDatabase.GeocoderType.NOMINATIM) {
                        limitSearch.setEnabled(true);
                        limitSearch.setChecked(prefs.getGeocoderLimit());
                    } else {
                        limitSearch.setEnabled(false);
                        limitSearch.setChecked(false);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    // empty
                }
            });
            limitSearch.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setGeocoderLimit(isChecked));
            searchBuilder.setPositiveButton(R.string.search, null);
            searchBuilder.setNegativeButton(R.string.cancel, null);

            final AppCompatDialog searchDialog = searchBuilder.create();

            Search search = new Search((AppCompatActivity) getActivity(), searchDialog, sr -> {
                searchDialog.dismiss();
                callback.onItemSelected(sr);
            });

            searchDialog.setOnShowListener(dialog -> {
                Button positive = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                positive.setOnClickListener(view -> search.find(geocoders[searchGeocoder.getSelectedItemPosition()], searchEdit.getText().toString(), bbox,
                        limitSearch.isChecked()));
            });

            searchEdit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    search.find(geocoders[searchGeocoder.getSelectedItemPosition()], v.getText().toString(), bbox, limitSearch.isChecked());
                }
                return false;
            });

            return searchDialog;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(BBOX_KEY, bbox);
    }
}
