package de.blau.android.dialogs;

import java.util.List;

import com.kunzisoft.androidclearchroma.ChromaDialog;
import com.kunzisoft.androidclearchroma.IndicatorMode;
import com.kunzisoft.androidclearchroma.colormode.ColorMode;
import com.kunzisoft.androidclearchroma.listener.OnColorSelectedListener;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.layer.StyleableInterface;
import de.blau.android.layer.StyleableLayer;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.Density;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.Screen;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog allowing the user to change some properties of the current background
 *
 */
public class LayerStyle extends ImmersiveDialogFragment {

    private static final String DEBUG_TAG = LayerStyle.class.getSimpleName();

    private static final String TAG = "fragment_layer_style";

    private static final String LAYERINDEX = "layer_index";

    private final Map map;

    private StyleableInterface layer;
    private View               colorView;

    /**
     * Show an instance of this dialog
     * 
     * @param activity the calling FragmentActivity
     * @param layerIndex the index of the dialog we want to style
     */
    public static void showDialog(@NonNull FragmentActivity activity, int layerIndex) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            LayerStyle backgroundPropertiesFragment = newInstance(layerIndex);
            backgroundPropertiesFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss any current showing instance of this dialog
     * 
     * @param activity the calling FragmentActivity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Get a new instance of this fragment
     * 
     * @param layerIndex the index of the dialog we want to style
     * @return a new instance of LayerStyle
     */
    private static LayerStyle newInstance(int layerIndex) {
        LayerStyle f = new LayerStyle();
        Bundle args = new Bundle();
        args.putInt(LAYERINDEX, layerIndex);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    /**
     * Default constructor
     */
    public LayerStyle() {
        map = App.getLogic().getMap();
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.layer_style_title);

        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        View layout = inflater.inflate(R.layout.layer_style, null);
        View labelContainer = layout.findViewById(R.id.layer_label_container);
        SeekBar seeker = (SeekBar) layout.findViewById(R.id.layer_line_width);
        View lineWidthView = layout.findViewById(R.id.layer_line_width_view);
        colorView = layout.findViewById(R.id.layer_color);

        int layerIndex = getArguments().getInt(LAYERINDEX);
        MapViewLayer tempLayer = map.getLayer(layerIndex);
        if (tempLayer instanceof StyleableInterface) {
            builder.setTitle(tempLayer.getName());
            layer = (StyleableInterface) tempLayer;
            final List<String> labelKeys = layer.getLabelList();
            if (!labelKeys.isEmpty()) {
                final Spinner labelSpinner = (Spinner) layout.findViewById(R.id.layer_style_label);
                labelKeys.add(0, getString(R.string.none));
                String[] labelArray = new String[labelKeys.size()];
                labelKeys.toArray(labelArray);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, labelArray);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                labelSpinner.setAdapter(adapter);
                String label = layer.getLabel();
                if (label != null && !"".equals(label)) {
                    int pos = labelKeys.indexOf(label);
                    if (pos != -1) {
                        labelSpinner.setSelection(pos);
                    }
                }
                labelSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                        layer.setLabel(pos == 0 ? "" : labelKeys.get(pos));
                        map.invalidate();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                        // required, but not used
                    }
                });
            } else {
                labelContainer.setVisibility(View.GONE);
            }
            colorView.setBackgroundColor(layer.getColor());
            colorView.setOnClickListener(v -> {
                ChromaDialog chromaDialog = new ChromaDialog.Builder().initialColor(layer.getColor()).colorMode(ColorMode.ARGB).indicatorMode(IndicatorMode.HEX)
                        .create();
                chromaDialog.setOnColorSelectedListener(new OnColorSelectedListener() {

                    @Override
                    public void onNegativeButtonClick(int color) {
                        // do nothing
                    }

                    @Override
                    public void onPositiveButtonClick(int color) {
                        layer.setColor(color);
                        colorView.setBackgroundColor(color);
                        map.invalidate();
                    }
                });
                chromaDialog.show(getChildFragmentManager(), "ChromaDialog");
            });

            lineWidthView.setBackgroundColor(Color.BLACK);

            LayoutParams layoutParams = lineWidthView.getLayoutParams();
            layoutParams.height = (int) layer.getStrokeWidth();
            lineWidthView.setLayoutParams(layoutParams);
            seeker.setOnSeekBarChangeListener(createSeekBarListener(lineWidthView, layer));
            seeker.setProgress(Density.pxToDp(getContext(), (int) layer.getStrokeWidth()));
        }
        builder.setView(layout);
        builder.setPositiveButton(R.string.okay, doNothingListener);

        return builder.create();

    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout((int) (Screen.getScreenSmallDimemsion(getActivity()) * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * Create a listener for the stroke width seek bar
     * 
     * @param strokeWidthView the view showing the width
     * @param layer the layer we are changing this for
     * @return an OnSeekBarChangeListener
     */
    private OnSeekBarChangeListener createSeekBarListener(@NonNull final View strokeWidthView, @NonNull final StyleableInterface layer) {
        return new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, int progress, final boolean fromTouch) {
                LayoutParams layoutParams = strokeWidthView.getLayoutParams();
                layoutParams.height = Density.dpToPx(getContext(), progress);
                strokeWidthView.setLayoutParams(layoutParams);
                strokeWidthView.invalidate();
                layer.setStrokeWidth(layoutParams.height);
                map.invalidate();
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
                // required, but not used
            }

            @Override
            public void onStopTrackingTouch(final SeekBar arg0) {
                // required, but not used
            }
        };
    }
}
