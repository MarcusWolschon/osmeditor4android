package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;
import static de.blau.android.dialogs.Util.hasState;
import static de.blau.android.util.GeoMath.OSM_SCALE;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.StorageException;
import de.blau.android.osm.Node;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.GeoMath;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Manually set the position of a Node
 */
public class PositionDialog extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PositionDialog.class.getSimpleName().length());
    private static final String DEBUG_TAG = PositionDialog.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "fragment_position";

    private static final String NODE_KEY    = "node";
    private static final String NODE_ID_KEY = "nodeId";
    private static final String LON_KEY     = "lon";
    private static final String LAT_KEY     = "lon";

    private Node     node;
    private EditText lon;
    private EditText lat;

    /**
     * Show a dialog with a list of issues
     * 
     * @param activity the calling FragmentActivity
     * @param titleRes resource id for the title
     * @param elment the object that we want to download along
     * @param refPoint a GeoPoint indicating what bit of element is in view for better sorting
     *
     */
    public static void show(@NonNull AppCompatActivity activity, @NonNull Node node) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            PositionDialog positionFragment = newInstance(node);
            positionFragment.show(fm, TAG);
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
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create new instance of this object
     * 
     * @param titleRes resource id for the title
     * @param refPoint
     * @param messageRes resource if for the message
     * @param tipKeyRes tip key resource
     * @param tipRes tip message resource
     * @param result the List of Result elements
     * @return a TagConflictDialog instance
     */
    private static PositionDialog newInstance(@NonNull Node node) {
        PositionDialog f = new PositionDialog();
        Bundle args = new Bundle();

        args.putSerializable(NODE_KEY, node);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public AppCompatDialog onCreateDialog(Bundle savedState) {
        if (savedState != null) {
            Log.d(DEBUG_TAG, "Recreating from saved state");
            long nodeId = savedState.getLong(NODE_ID_KEY);
            if (nodeId == 0) {
                Log.e(DEBUG_TAG, "No saved state");
                throw new IllegalStateException("No saved state");
            }
            node = (Node) App.getDelegator().getOsmElement(Node.NAME, nodeId);
            if (node == null) {
                throw new IllegalStateException("Node " + nodeId + " doesn't exist anymore");
            }
        } else {
            node = Util.getSerializeable(getArguments(), NODE_KEY, Node.class);
        }
        final FragmentActivity activity = getActivity();
        if (activity instanceof Main) {
            ((Main) activity).descheduleAutoLock();
            enableAutolockReschedule();
        }

        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);
        Builder builder = ThemeUtils.getAlertDialogBuilder(activity);
        builder.setTitle(R.string.set_position_title);

        View layout = inflater.inflate(R.layout.set_position, null);
        builder.setView(layout);
        // TODO add conversion to/from other datums
        TextView datum = (TextView) layout.findViewById(R.id.set_position_datum);
        datum.setText(R.string.WGS84);
        lon = (EditText) layout.findViewById(R.id.set_position_lon);
        lon.setText(hasState(savedState, LON_KEY) ? savedState.getString(LON_KEY) : String.format(Locale.US, "%.7f", node.getLon() / OSM_SCALE)); // NOSONAR
        lat = (EditText) layout.findViewById(R.id.set_position_lat);
        lat.setText(hasState(savedState, LAT_KEY) ? savedState.getString(LAT_KEY) : String.format(Locale.US, "%.7f", node.getLat() / OSM_SCALE)); // NOSONAR
        builder.setPositiveButton(R.string.set, null);
        builder.setNegativeButton(R.string.cancel, null);
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener((DialogInterface dialogInterface) -> {
            Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positive.setOnClickListener((View v) -> {
                try {
                    checkAndSet(activity, dialog, Double.parseDouble(lon.getText().toString()), Double.parseDouble(lat.getText().toString()));
                } catch (OsmIllegalOperationException | NumberFormatException | StorageException ex) {
                    Log.w(DEBUG_TAG, ex.getMessage());
                }
            });
        });
        return dialog;
    }

    /**
     * @param activity
     * @param dialog
     * @param longitude
     * @param latitude
     */
    private void checkAndSet(final FragmentActivity activity, final AlertDialog dialog, double longitude, double latitude) {
        if (GeoMath.coordinatesInCompatibleRange(longitude, latitude)) {
            App.getLogic().performSetPosition(activity, node, longitude, latitude);
            dialog.dismiss();
            if (activity instanceof Main) {
                ((Main) activity).getEasyEditManager().invalidate();
            }
        } else {
            ScreenMessage.toastTopWarning(activity, R.string.coordinates_out_of_range);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        Tip.showDialog(getActivity(), R.string.tip_download_along_key, R.string.tip_download_along);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        outState.putLong(NODE_ID_KEY, node.getOsmId());
        outState.putString(LON_KEY, lon.getText().toString());
        outState.putString(LAT_KEY, lat.getText().toString());
        Log.w(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
    }
}
