package de.blau.android.dialogs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.services.commons.geojson.Feature;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;

/**
 * Very simple dialog fragment to display some info on an OSM element
 * 
 * @author simon
 *
 */
public class FeatureInfo extends DialogFragment {

    private static final String FEATURE = "feature";

    private static final String DEBUG_TAG = FeatureInfo.class.getName();

    private static final String TAG = "fragment_feature_info";

    static public void showDialog(FragmentActivity activity, Feature feature) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            FeatureInfo elementInfoFragment = newInstance(feature);
            elementInfoFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    private static void dismissDialog(FragmentActivity activity) {
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment != null) {
                ft.remove(fragment);
            }
            ft.commit();
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     */
    private static FeatureInfo newInstance(Feature feature) {
        FeatureInfo f = new FeatureInfo();

        Bundle args = new Bundle();
        args.putString(FEATURE, feature.toJson());

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(ThemeUtils.getThemedContext(getActivity(), R.style.Theme_DialogLight, R.style.Theme_DialogDark));
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        inflater = ThemeUtils.getLayoutInflater(activity);
        ScrollView sv = (ScrollView) inflater.inflate(R.layout.element_info_view, container, false);
        TableLayout tl = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);

        String featureString = getArguments().getString(FEATURE);
        Feature f = Feature.fromJson(featureString);

        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(10, 2, 10, 2);

        if (f != null) {
            // tl.setShrinkAllColumns(true);
            tl.setColumnShrinkable(1, true);

            tl.addView(TableLayoutUtils.createRow(activity, R.string.type, f.getGeometry().getType(), tp));
            tl.addView(TableLayoutUtils.divider(activity));
            tl.addView(TableLayoutUtils.createRow(activity, R.string.menu_tags, null, tp));
            JsonObject properties = f.getProperties();
            if (properties != null) {
                for (String key : properties.keySet()) {
                    JsonElement e = properties.get(key);
                    if (e.isJsonArray()) {
                        // value not displayed yet
                        tl.addView(TableLayoutUtils.createRow(activity, key, null, tp));
                    } if (e.isJsonObject()) {
                        // value not displayed yet
                        tl.addView(TableLayoutUtils.createRow(activity, key, null, tp));
                    } else if (e.isJsonPrimitive()){
                        tl.addView(TableLayoutUtils.createRow(activity, key, e.getAsString(), tp));
                    }
                }
            }
        }
        getDialog().setTitle(R.string.feature_information);
        return sv;
    }
}
