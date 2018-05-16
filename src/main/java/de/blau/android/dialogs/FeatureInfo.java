package de.blau.android.dialogs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Very simple dialog fragment to display some info on an OSM element
 * 
 * @author simon
 *
 */
public class FeatureInfo extends DialogFragment {

    private static final String FEATURE_KEY = "feature";

    private static final String DEBUG_TAG = FeatureInfo.class.getName();

    private static final String TAG = "fragment_feature_info";

    private Feature feature = null;

    /**
     * Show an info dialog for the supplied GeoJSON Feature
     * 
     * @param activity the calling Activity
     * @param feature the Feature
     */
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

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activit
     */
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
     * Create a new instance of the FeatureInfo dialog
     * 
     * @param feature Feature to display the info on
     * @return an instance of ElementInfo
     */
    private static FeatureInfo newInstance(Feature feature) {
        FeatureInfo f = new FeatureInfo();

        Bundle args = new Bundle();
        args.putString(FEATURE_KEY, feature.toJson());

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String featureString = getArguments().getString(FEATURE_KEY);
        feature = Feature.fromJson(featureString);
    }

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.done, doNothingListener);
        if (feature != null && feature.getGeometry() != null && GeoJSONConstants.POINT.equals(feature.getGeometry().getType())) {
            builder.setNeutralButton(R.string.share_position, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    Position p = ((Point) feature.getGeometry()).getCoordinates();
                    double[] lonLat = new double[2];
                    lonLat[0] = p.getLongitude();
                    lonLat[1] = p.getLatitude();
                    Util.sharePosition(getActivity(), lonLat);

                }
            });
        }
        builder.setTitle(R.string.feature_information);
        builder.setView(createView(null));
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (!getShowsDialog()) {
            return createView(container);
        }
        return null;
    }

    /**
     * Create the view we want to display
     * 
     * @param container parent view or null
     * @return the View
     */
    private View createView(ViewGroup container) {
        LayoutInflater inflater;
        FragmentActivity activity = getActivity();
        inflater = ThemeUtils.getLayoutInflater(activity);
        ScrollView sv = (ScrollView) inflater.inflate(R.layout.element_info_view, container, false);
        TableLayout tl = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);

        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(10, 2, 10, 2);

        if (feature != null) {
            // tl.setShrinkAllColumns(true);
            tl.setColumnShrinkable(1, true);
            tl.addView(TableLayoutUtils.createRow(activity, R.string.type, feature.getGeometry().getType(), null, tp));
            tl.addView(TableLayoutUtils.divider(activity));
            tl.addView(TableLayoutUtils.createRow(activity, R.string.menu_tags, null, null, tp));
            JsonObject properties = feature.getProperties();
            if (properties != null) {
                for (String key : properties.keySet()) {
                    JsonElement e = properties.get(key);
                    if (e.isJsonArray()) {
                        // value not displayed yet
                        tl.addView(TableLayoutUtils.createRow(activity, key, toItalic(R.string.json_object_not_displayed), tp));
                    }
                    if (e.isJsonObject()) {
                        // value not displayed yet
                        tl.addView(TableLayoutUtils.createRow(activity, key, toItalic(R.string.json_object_not_displayed), tp));
                    } else if (e.isJsonPrimitive()) {
                        tl.addView(TableLayoutUtils.createRow(activity, key, e.getAsString(), tp));
                    }
                }
            }
        }
        return sv;
    }

    /**
     * Get the string resource formated as an italic string
     * 
     * @param resId String resource id
     * @return a Spanned containing the string
     */
    @SuppressWarnings("deprecation")
    private Spanned toItalic(int resId) {
        return Util.fromHtml("<i>" + getString(resId) + "</i>");
    }
}
