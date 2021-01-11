package de.blau.android.dialogs;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TableLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.osm.GeoJson;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.InfoDialogFragment;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Very simple dialog fragment to display some info on a GeoJSON element
 * 
 * @author simon
 *
 */
public class FeatureInfo extends InfoDialogFragment {

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
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull Feature feature) {
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
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new instance of the FeatureInfo dialog
     * 
     * @param feature Feature to display the info on
     * @return an instance of ElementInfo
     */
    @NonNull
    private static FeatureInfo newInstance(@NonNull Feature feature) {
        FeatureInfo f = new FeatureInfo();

        Bundle args = new Bundle();
        args.putString(FEATURE_KEY, feature.toJson());

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String featureString = getArguments().getString(FEATURE_KEY);
        feature = Feature.fromJson(featureString);
    }

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.done, doNothingListener);
        if (feature != null) {
            builder.setNeutralButton(R.string.create_osm_element, (dialog, which) -> {
                int maxNodes = App.getLogic().getPrefs().getServer().getCachedCapabilities().getMaxWayNodes();
                List<OsmElement> elements = GeoJson.toOsm(feature, maxNodes);
                FragmentActivity activity = getActivity();
                final Logic logic = App.getLogic();
                logic.addElements(activity, elements);
                if (activity instanceof Main) {
                    final OsmElement first = elements.get(0);
                    if (Tags.isMultiPolygon(first)) {
                        logic.setSelectedRelation((Relation) first);
                    } else {
                        logic.setSelection(elements);
                    }
                    ((Main) activity).getEasyEditManager().startElementSelectionMode();
                }
            });

            final JsonObject properties = feature.properties();
            if (properties != null) {
                builder.setNegativeButton(R.string.copy_properties, (dialog, which) -> {
                    App.getTagClipboard(getContext()).copy(GeoJson.extractTags(properties));
                    Snack.toastTopInfo(getContext(), R.string.toast_properties_copied);
                });
            }
        }
        builder.setTitle(R.string.feature_information);
        builder.setView(createView(null));
        return builder.create();
    }

    @Override
    protected View createView(@Nullable ViewGroup container) {
        LayoutInflater inflater;
        FragmentActivity activity = getActivity();
        inflater = ThemeUtils.getLayoutInflater(activity);
        ScrollView sv = (ScrollView) inflater.inflate(R.layout.element_info_view, container, false);
        TableLayout tl = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);

        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.setMargins(10, 2, 10, 2);

        if (feature != null) {
            tl.setColumnShrinkable(1, true);
            Geometry geometry = feature.geometry();
            if (geometry != null) {
                final String geometryType = geometry.type();
                tl.addView(TableLayoutUtils.createRow(activity, R.string.type, geometryType, tp));
                if (GeoJSONConstants.POINT.equals(geometryType)) {
                    ImageButton button = new ImageButton(activity);
                    button.setImageResource(ThemeUtils
                            .getResIdFromAttribute(ThemeUtils.getThemedContext(activity, R.style.Theme_DialogLight, R.style.Theme_DialogDark), R.attr.share));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        button.setBackground(null);
                    }
                    button.setPadding(12, 12, 0, 0);
                    final Point p = ((Point) geometry);
                    button.setOnClickListener(v -> {
                        Dialog dialog = getDialog();
                        if (dialog != null) {
                            dialog.dismiss();
                        }

                        Util.sharePosition(getActivity(), new double[] { p.longitude(), p.latitude() }, null);
                    });
                    tl.addView(TableLayoutUtils.createRowWithButton(activity, R.string.location_lon_label, prettyPrint(p.longitude()), button, tp));
                    tl.addView(TableLayoutUtils.createRow(activity, R.string.location_lat_label, prettyPrint(p.latitude()), tp));
                }
            }
            tl.addView(TableLayoutUtils.divider(activity));
            JsonObject properties = feature.properties();
            if (properties != null && !properties.isJsonNull()) {
                Set<String> keys = properties.keySet();
                if (!keys.isEmpty()) {
                    tl.addView(TableLayoutUtils.createRow(activity, R.string.menu_tags, null, tp));
                    for (String key : keys) {
                        JsonElement e = properties.get(key);
                        if (e.isJsonArray() || e.isJsonObject()) {
                            // structured values not displayed yet
                            tl.addView(TableLayoutUtils.createRow(activity, key, toItalic(R.string.json_object_not_displayed), tp));
                        } else if (e.isJsonPrimitive()) {
                            tl.addView(TableLayoutUtils.createRow(activity, key, e.getAsString(), tp));
                        }
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
    private Spanned toItalic(int resId) {
        return Util.fromHtml("<i>" + getString(resId) + "</i>");
    }

    /**
     * Pretty print a coordinate value
     * 
     * @param coord the coordinate in WGS84
     * @return a reasonable looking string representation
     */
    @NonNull
    private static String prettyPrint(double coord) {
        return String.format(Locale.US, "%.7f", coord) + "°";
    }
}
