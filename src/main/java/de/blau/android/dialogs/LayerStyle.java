package de.blau.android.dialogs;

import java.util.List;

import com.pavelsikun.vintagechroma.IndicatorMode;
import com.pavelsikun.vintagechroma.OnColorSelectedListener;
import com.pavelsikun.vintagechroma.colormode.ColorMode;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.layer.StyleableLayer;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.Density;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Display a dialog allowing the user to change some properties of the current background
 *
 */
public class LayerStyle extends ImmersiveDialogFragment {

    private static final String DEBUG_TAG = LayerStyle.class.getSimpleName();

    private static final String TAG = "fragment_layer_style";

    private static final String LAYERINDEX = "layer_index";

    private final Map map;

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
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment != null) {
                ft.remove(fragment);
            }
            ft.commit();
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "dismissDialog", isex);
        }
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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(DEBUG_TAG, "onAttach");
        if (!(context instanceof Main)) {
            throw new ClassCastException(context.toString() + " can only be called from Main");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
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
        View lineWidthView = (View) layout.findViewById(R.id.layer_line_width_view);
        final View colorView = (View) layout.findViewById(R.id.layer_color);

        int layerIndex = getArguments().getInt(LAYERINDEX);
        MapViewLayer tempLayer = map.getLayer(layerIndex);
        if (tempLayer instanceof StyleableLayer) {
            builder.setTitle(tempLayer.getName());
            final StyleableLayer layer = (StyleableLayer) tempLayer;
            final List<String> labelKeys = layer.getLabelList();
            if (labelKeys != null && !labelKeys.isEmpty()) {
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
                    }
                });
            } else {
                labelContainer.setVisibility(View.GONE);
            }
            colorView.setBackgroundColor(layer.getColor());
            colorView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    new FixedChromaDialog.Builder().initialColor(layer.getColor()).colorMode(ColorMode.ARGB).indicatorMode(IndicatorMode.HEX)
                            .onColorSelected(new OnColorSelectedListener() {
                                @Override
                                public void onColorSelected(int color) {
                                    layer.setColor(color);
                                    colorView.setBackgroundColor(color);
                                    map.invalidate();
                                }
                            }).create().show(getChildFragmentManager(), "ChromaDialog");
                }
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
            dialog.getWindow().setLayout((int) (Util.getScreenSmallDimemsion(getActivity()) * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * Create a listener for the stroke width seek bar
     * 
     * @param strokeWidthView the view showing the width
     * @param layer the layer we are changing this for
     * @return an OnSeekBarChangeListener
     */
    private OnSeekBarChangeListener createSeekBarListener(@NonNull final View strokeWidthView, @NonNull  final StyleableLayer layer) {
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
            }

            @Override
            public void onStopTrackingTouch(final SeekBar arg0) {
            }
        };
    }
}
