package io.vespucci.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Main;
import io.vespucci.address.Address;
import io.vespucci.osm.Node;
import io.vespucci.osm.Tags;
import io.vespucci.osm.Way;
import io.vespucci.presets.Preset;
import io.vespucci.presets.PresetItem;
import io.vespucci.presets.PresetTagField;
import io.vespucci.presets.PresetTextField;
import io.vespucci.propertyeditor.tagform.TextRow;
import io.vespucci.util.ElementSearch;
import io.vespucci.util.ImmersiveDialogFragment;
import io.vespucci.util.IntCoordinates;
import io.vespucci.util.StreetPlaceNamesAdapter;
import io.vespucci.util.ThemeUtils;
import io.vespucci.util.Util;
import io.vespucci.views.CustomAutoCompleteTextView;
import io.vespucci.views.SimpleTextRow;

/**
 * Show a Dialog that allows the user to set the values on an address interpolation way, predicts likely values
 * 
 * Assumes a simple interpolation with address nodes only at the way ends. Currently doesn't create its own undo
 * checkpoint
 * 
 */
public class AddressInterpolationDialog extends ImmersiveDialogFragment {
    private static final String DEBUG_TAG = AddressInterpolationDialog.class.getSimpleName().substring(0, Math.min(23, AddressInterpolationDialog.class.getSimpleName().length()));

    private static final String TAG = "fragment_address_interpolation";

    private static final String WAY_ID_KEY = "way_id";

    private static final String ADDRESS_PRESET_ITEM = "Address";

    private Way way;

    /**
     * Create an address interpolation from an (existing) way
     * 
     * @param activity the calling FragmentActivity
     * @param way the interpolation way
     */
    public static void showDialog(@NonNull AppCompatActivity activity, @NonNull Way way) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            if (activity instanceof Main) {
                ((Main) activity).descheduleAutoLock();
            }
            AddressInterpolationDialog tagConflictFragment = newInstance(way);
            tagConflictFragment.show(fm, TAG);
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
     * @param way the interpolation way
     * @return an AddressInterpolationDialog instance
     */
    private static AddressInterpolationDialog newInstance(@NonNull Way way) {
        AddressInterpolationDialog f = new AddressInterpolationDialog();
        Bundle args = new Bundle();
        args.putLong(WAY_ID_KEY, way.getOsmId());
        f.setArguments(args);
        f.setShowsDialog(true);
        return f;
    }

    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Long wayId = null;
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "Recreating from saved state");
            wayId = savedInstanceState.getLong(WAY_ID_KEY);

        } else {
            wayId = getArguments().getLong(WAY_ID_KEY);
        }
        way = (Way) App.getDelegator().getOsmElement(Way.NAME, wayId);
        // We needed the themed context or else styling of the adapters will not work
        final Context context = ThemeUtils.getThemedContext(getContext(), R.style.Theme_DialogLight, R.style.Theme_DialogDark);
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final FragmentActivity activity = getActivity();

        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.interpolation, null);
        EditText start = (EditText) layout.findViewById(R.id.start_edit);
        EditText end = (EditText) layout.findViewById(R.id.end_edit);
        RadioButton even = (RadioButton) layout.findViewById(R.id.even);
        RadioButton odd = (RadioButton) layout.findViewById(R.id.odd);
        RadioButton other = (RadioButton) layout.findViewById(R.id.other);
        CustomAutoCompleteTextView otherEdit = (CustomAutoCompleteTextView) layout.findViewById(R.id.other_edit);
        otherEdit.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if (hasFocus) {
                other.setChecked(true);
            }
        });
        other.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                otherEdit.requestFocus();
            }
        });
        LinearLayout tagLayout = (LinearLayout) layout.findViewById(R.id.commonTags);

        final Node firstNode = way.getFirstNode();
        Map<String, String> firstTags = predictNodeAddress(firstNode);
        final String firstHousenumber = firstTags.get(Tags.KEY_ADDR_HOUSENUMBER);
        start.setText(firstHousenumber);
        final Node lastNode = way.getLastNode();
        Map<String, String> lastTags = predictNodeAddress(lastNode);
        end.setText(lastTags.get(Tags.KEY_ADDR_HOUSENUMBER));
        Map<String, String> wayTags = new TreeMap<>();
        try {
            if (Integer.parseInt(firstHousenumber) % 2 == 0) {
                wayTags.put(Tags.KEY_ADDR_INTERPOLATION, Tags.VALUE_EVEN);
                even.setChecked(true);
            } else {
                wayTags.put(Tags.KEY_ADDR_INTERPOLATION, Tags.VALUE_ODD);
                odd.setChecked(true);
            }
        } catch (NumberFormatException nfex) {
            wayTags.put(Tags.KEY_ADDR_INTERPOLATION, "");
            other.setChecked(true);
        }

        StreetPlaceNamesAdapter streetNameAutocompleteAdapter = new StreetPlaceNamesAdapter(context, R.layout.autocomplete_row, App.getDelegator(), Node.NAME,
                firstNode.getOsmId(), null, false);
        StreetPlaceNamesAdapter placeNameAutocompleteAdapter = new StreetPlaceNamesAdapter(context, R.layout.autocomplete_row, App.getDelegator(), Node.NAME,
                firstNode.getOsmId(), null, true);

        PresetItem presetItem = null;
        for (Preset preset : App.getCurrentPresets(context)) {
            presetItem = preset != null ? preset.getItemByName(ADDRESS_PRESET_ITEM, App.getGeoContext(context).getIsoCodes(way)) : null;
            if (presetItem != null) {
                break;
            }
        }

        Set<String> addressTags = Address.getAddressKeys(context, firstNode.getLon() / 1E7D, firstNode.getLat() / 1E7D);
        Map<String, String> expandedTags = new HashMap<>(firstTags);
        for (String t : Tags.ADDRESS_LARGE) {
            if (!expandedTags.containsKey(t) && addressTags.contains(t)) {
                expandedTags.put(t, "");
            }
        }
        List<Entry<String, String>> temp = new ArrayList<>(expandedTags.entrySet());
        Collections.sort(temp, (Entry<String, String> e0, Entry<String, String> e1) -> {
            // checking for null is needed as keys from geocontext might not be in Tags.ADDRESS_SORT_ORDER
            final String k0 = e0.getKey();
            Integer i0 = Tags.ADDRESS_SORT_ORDER.get(k0);
            final String k1 = e1.getKey();
            Integer i1 = Tags.ADDRESS_SORT_ORDER.get(k1);
            if (i0 == null || i1 == null) {
                return k0.compareTo(k1);
            }
            return i0.compareTo(i1);
        });
        for (Entry<String, String> entry : temp) {
            String key = entry.getKey();
            if (key.startsWith(Tags.KEY_ADDR_BASE) && !Tags.KEY_ADDR_HOUSENUMBER.equals(key)) {
                ArrayAdapter<?> adapter = null;
                if (Tags.KEY_ADDR_STREET.equals(key)) {
                    adapter = streetNameAutocompleteAdapter;
                } else if (Tags.KEY_ADDR_PLACE.equals(key)) {
                    adapter = placeNameAutocompleteAdapter;
                }
                PresetTagField presetField = presetItem != null && presetItem.hasKey(key) ? presetItem.getField(key)
                        : (PresetTagField) new PresetTextField(key);
                tagLayout.addView(SimpleTextRow.getRow(context, inflater, tagLayout, presetItem, presetField, entry.getValue(), adapter));
            }
        }
        Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.address_interpolation_title);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.okay, (DialogInterface dialog, int which) -> {
            Map<String, String> newTags = new TreeMap<>();
            // collect common tags
            for (int i = 0; i < tagLayout.getChildCount(); i++) {
                TextRow row = (TextRow) tagLayout.getChildAt(i);
                String value = row.getValue();
                if (value != null && !"".equals(value)) {
                    newTags.put(row.getKey(), value);
                }
            }
            updateNode(firstNode, newTags, start.getText().toString());
            updateNode(lastNode, newTags, end.getText().toString());
            if (even.isChecked()) {
                wayTags.put(Tags.KEY_ADDR_INTERPOLATION, Tags.VALUE_EVEN);
            } else if (odd.isChecked()) {
                wayTags.put(Tags.KEY_ADDR_INTERPOLATION, Tags.VALUE_ODD);
            } else {
                wayTags.put(Tags.KEY_ADDR_INTERPOLATION, otherEdit.getText().toString());
            }
            App.getLogic().setTags(activity, Way.NAME, way.getOsmId(), wayTags, false);
            // update and save the addresses
            Address.updateLastAddresses(context, streetNameAutocompleteAdapter, Node.NAME, firstNode.getOsmId(), Util.getListMap(firstNode.getTags()), false);
            Address.updateLastAddresses(context, streetNameAutocompleteAdapter, Node.NAME, lastNode.getOsmId(), Util.getListMap(lastNode.getTags()), true);
            if (activity instanceof Main) {
                ((Main) activity).invalidateMap();
            }
        });

        builder.setView(layout);
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (getActivity() instanceof Main) {
            ((Main) getActivity()).scheduleAutoLock();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        outState.putLong(WAY_ID_KEY, way.getOsmId());
    }

    /**
     * Update address tags on a Node
     * 
     * * This will maintain any non-address tags
     * 
     * @param node the Node
     * @param newTags the new Tags to set
     * @param houseNumber the house number
     */
    private void updateNode(@NonNull Node node, @NonNull Map<String, String> newTags, @NonNull String houseNumber) {
        Map<String, String> temp = new TreeMap<>(node.getTags());
        // delete address tags
        for (String key : new ArrayList<>(temp.keySet())) {
            if (key.startsWith(Tags.KEY_ADDR_BASE)) {
                temp.remove(key);
            }
        }
        temp.putAll(newTags);
        temp.put(Tags.KEY_ADDR_HOUSENUMBER, houseNumber);
        App.getLogic().setTags(getActivity(), Node.NAME, node.getOsmId(), temp, false);
    }

    /**
     * Predict an address for a node
     * 
     * @param node the Node
     * @return a Map holding the predicted tags
     */
    private Map<String, String> predictNodeAddress(@NonNull final Node node) {
        return Address.multiValueToSingle(Address.predictAddressTags(getContext(), Node.NAME, node.getOsmId(),
                new ElementSearch(new IntCoordinates(node.getLon(), node.getLat()), true), Util.getListMap(node.getTags()), Address.NO_HYSTERESIS));
    }
}
