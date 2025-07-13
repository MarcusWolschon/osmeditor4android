package de.blau.android.dialogs;

import java.util.ArrayList;
import java.util.List;

import com.kunzisoft.androidclearchroma.ChromaDialog;
import com.kunzisoft.androidclearchroma.IndicatorMode;
import com.kunzisoft.androidclearchroma.colormode.ColorMode;
import com.kunzisoft.androidclearchroma.listener.OnColorSelectedListener;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import ch.poole.android.numberpicker.library.NumberPicker;
import ch.poole.android.numberpicker.library.Enums.ActionEnum;
import de.blau.android.App;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.layer.AbstractConfigurationDialog;
import de.blau.android.layer.LabelMinZoomInterface;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.layer.StyleableInterface;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.resources.symbols.Symbols;
import de.blau.android.util.Density;
import de.blau.android.util.Screen;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog allowing the user to change styling of a layer
 *
 */
public class LayerStyle extends AbstractConfigurationDialog {

    private static final String TAG = "fragment_layer_style";

    private static final String LAYER_INDEX_KEY = "layer_index";

    private Map map;

    private int                layerIndex;
    private StyleableInterface layer;
    private View               colorView;
    private View               labelContainer;
    private String             subLayerName = null;

    /**
     * Show an instance of this dialog
     * 
     * @param activity the calling FragmentActivity
     * @param layerIndex the index of the dialog we want to style
     */
    public static void showDialog(@NonNull FragmentActivity activity, int layerIndex) {
        showDialog(activity, newInstance(layerIndex), TAG);
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
        args.putInt(LAYER_INDEX_KEY, layerIndex);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    /**
     * Default constructor
     */
    public LayerStyle() {
        // empty
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            layerIndex = savedInstanceState.getInt(LAYER_INDEX_KEY);
        } else {
            layerIndex = getArguments().getInt(LAYER_INDEX_KEY);
        }
        map = App.getLogic().getMap();
        Builder builder = ThemeUtils.getAlertDialogBuilder(getActivity());
        builder.setTitle(R.string.layer_style_title);

        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        View layout = inflater.inflate(R.layout.layer_style, null);
        View layerContainer = layout.findViewById(R.id.layer_layer_container);
        labelContainer = layout.findViewById(R.id.layer_label_container);
        final Spinner labelSpinner = (Spinner) labelContainer.findViewById(R.id.layer_style_label);
        View labelMinZoomContainer = layout.findViewById(R.id.layer_label_min_zoom_container);
        final NumberPicker labelMinZoomPicker = (NumberPicker) labelMinZoomContainer.findViewById(R.id.label_zoom_min);
        View symbolContainer = layout.findViewById(R.id.layer_symbol_container);
        final Spinner symbolSpinner = (Spinner) layout.findViewById(R.id.layer_style_symbol);
        SeekBar seeker = (SeekBar) layout.findViewById(R.id.layer_line_width);
        View lineWidthView = layout.findViewById(R.id.layer_line_width_view);
        final NumberPicker minZoomPicker = (NumberPicker) layerContainer.findViewById(R.id.zoom_min);
        final NumberPicker maxZoomPicker = (NumberPicker) layerContainer.findViewById(R.id.zoom_max);
        colorView = layout.findViewById(R.id.layer_color);
        MapViewLayer tempLayer = map.getLayer(layerIndex);
        if (tempLayer instanceof StyleableInterface) {
            builder.setTitle(tempLayer.getName());
            layer = (StyleableInterface) tempLayer;
            final List<String> layerNames = layer.getLayerList();
            final boolean hasSubLayers = !layerNames.isEmpty();
            if (hasSubLayers) {
                final Spinner layerSpinner = (Spinner) layout.findViewById(R.id.layer_style_layer);
                String[] layerArray = new String[layerNames.size()];
                layerNames.toArray(layerArray);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, layerArray);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                layerSpinner.setAdapter(adapter);
                subLayerName = layerArray[layerSpinner.getSelectedItemPosition()];
                layerSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                        subLayerName = layerArray[pos];
                        setupLabelSpinner(labelSpinner);
                        setUpColorSelector(lineWidthView);
                        setUpLineWidthSelector(seeker, lineWidthView);
                        setupSymbolSpinner(symbolSpinner);
                        setUpZoomPickers(minZoomPicker, maxZoomPicker);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                        // required, but not used
                    }
                });
                setUpZoomPickers(minZoomPicker, maxZoomPicker);
            } else {
                layerContainer.setVisibility(View.GONE);
            }

            setupLabelSpinner(labelSpinner);
            if (tempLayer instanceof LabelMinZoomInterface) {
                labelContainer.setVisibility(View.VISIBLE);
                labelMinZoomContainer.setVisibility(View.VISIBLE);
                setUpMinLabelZoomPicker(labelMinZoomPicker);
            } else {
                labelMinZoomContainer.setVisibility(View.GONE);
            }
            if (layer.usesPointSymbol()) {
                setupSymbolSpinner(symbolSpinner);
            } else {
                symbolContainer.setVisibility(View.GONE);
            }
            setUpColorSelector(lineWidthView);
            setUpLineWidthSelector(seeker, lineWidthView);
        }
        builder.setView(layout);
        builder.setPositiveButton(R.string.okay, doNothingListener);

        return builder.create();
    }

    /**
     * Set up the pickers for layer min/max zoom
     * 
     * @param minZoomPicker min zoom picker
     * @param maxZoomPicker max zoom picker
     */
    private void setUpZoomPickers(NumberPicker minZoomPicker, NumberPicker maxZoomPicker) {
        minZoomPicker.setValue(layer.getMinZoom(subLayerName));
        final int maxZoom = layer.getMaxZoom(subLayerName);
        final int max = maxZoomPicker.getMax();
        if (maxZoom < 0) {
            maxZoomPicker.setValue(max);
        } else {
            maxZoomPicker.setValue(maxZoom);
        }
        minZoomPicker.setValueChangedListener((int zoom, ActionEnum action) -> {
            layer.setMinZoom(subLayerName, zoom);
            map.invalidate();
        });
        maxZoomPicker.setValueChangedListener((int zoom, ActionEnum action) -> {
            layer.setMaxZoom(subLayerName, zoom >= max ? -1 : zoom);
            map.invalidate();
        });
    }

    /**
     * Set up the picker for label min zoom
     * 
     * @param minZoomPicker min zoom picker
     */
    private void setUpMinLabelZoomPicker(@NonNull NumberPicker minZoomPicker) {
        int zoom = ((LabelMinZoomInterface) layer).getLabelMinZoom(subLayerName);
        final int min = minZoomPicker.getMin();
        final int max = minZoomPicker.getMax();
        if (zoom < min) {
            zoom = min;
        } else if (zoom > max) {
            zoom = max;
        }
        minZoomPicker.setValue(zoom);
        minZoomPicker.setValueChangedListener((int z, ActionEnum action) -> {
            ((LabelMinZoomInterface) layer).setLabelMinZoom(subLayerName, z);
            map.invalidate();
        });
    }

    /**
     * Line width selector setup
     * 
     * @param seeker the SeekBar
     * @param lineWidthView the View displaying the width
     */
    public void setUpLineWidthSelector(@NonNull SeekBar seeker, @NonNull View lineWidthView) {
        LayoutParams layoutParams = lineWidthView.getLayoutParams();
        layoutParams.height = (int) layer.getStrokeWidth(subLayerName);
        lineWidthView.setLayoutParams(layoutParams);
        seeker.setOnSeekBarChangeListener(createSeekBarListener(lineWidthView, layer));
        seeker.setProgress(Density.pxToDp(getContext(), (int) layer.getStrokeWidth(subLayerName)));
    }

    /**
     * Color selector setup
     * 
     * @param lineWidthView the View displaying the line width
     */
    public void setUpColorSelector(@NonNull View lineWidthView) {
        final int color = layer.getColor(subLayerName);
        colorView.setBackgroundColor(color);
        colorView.setOnClickListener(v -> {
            ChromaDialog chromaDialog = new ChromaDialog.Builder().initialColor(color).colorMode(ColorMode.ARGB).indicatorMode(IndicatorMode.HEX).create();
            chromaDialog.setOnColorSelectedListener(new OnColorSelectedListener() {

                @Override
                public void onNegativeButtonClick(int color) {
                    // do nothing
                }

                @Override
                public void onPositiveButtonClick(int color) {
                    layer.setColor(subLayerName, color);
                    colorView.setBackgroundColor(color);
                    lineWidthView.setBackgroundColor(color);
                    map.invalidate();
                }
            });
            chromaDialog.show(getChildFragmentManager(), "ChromaDialog");
        });

        lineWidthView.setBackgroundColor(color);
    }

    /**
     * Set up the label selection spinner
     * 
     * @param labelSpinner the Spinner itself
     */
    public void setupLabelSpinner(@NonNull Spinner labelSpinner) {
        final List<String> labelKeys = layer.getLabelList(subLayerName);
        if (!labelKeys.isEmpty()) {
            labelKeys.add(0, getString(R.string.none));
            String[] labelArray = new String[labelKeys.size()];
            labelKeys.toArray(labelArray);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, labelArray);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            labelSpinner.setAdapter(adapter);
            String label = layer.getLabel(subLayerName);
            if (label != null && !"".equals(label)) {
                int pos = labelKeys.indexOf(label);
                if (pos != -1) {
                    labelSpinner.setSelection(pos);
                }
            }
            labelSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                    layer.setLabel(subLayerName, pos == 0 ? "" : labelKeys.get(pos));
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
    }

    /**
     * Set up the symbol selection spinner
     * 
     * @param symbolSpinner the Spinner itself
     */
    public void setupSymbolSpinner(@NonNull final Spinner symbolSpinner) {
        final int color = layer.getColor(subLayerName);
        final float width = layer.getStrokeWidth(subLayerName);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(width);
        paint.setStyle(Style.STROKE);
        java.util.Map<String, ImageView> imageMap = Symbols.getImages(getActivity(), paint);
        List<String> symbolKeys = new ArrayList<>();
        List<ImageView> symbolValues = new ArrayList<>();
        for (java.util.Map.Entry<String, ImageView> entry : imageMap.entrySet()) {
            symbolKeys.add(entry.getKey());
            symbolValues.add(entry.getValue());
        }
        ImageView none = new ImageView(getActivity());
        final int size = Density.dpToPx(getActivity(), Symbols.SIZE);
        none.setMinimumWidth(size);
        none.setMinimumHeight(size);
        symbolValues.add(0, none);
        ImageView[] symbolArray = new ImageView[symbolValues.size()];
        symbolValues.toArray(symbolArray);
        SimpleImageArrayAdapter adapter = new SimpleImageArrayAdapter(getActivity(), symbolArray);
        symbolSpinner.setAdapter(adapter);
        String symbolName = layer.getPointSymbol(subLayerName);
        symbolSpinner.setSelection(symbolKeys.indexOf(symbolName) + 1); // if not found this will set 0
        symbolSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int pos, long arg3) {
                layer.setPointSymbol(subLayerName, pos == 0 ? null : symbolKeys.get(pos - 1));
                map.invalidate();
                // FIXME hack around "disappearing selection" issue, it would naturally be better to fix this properly
                symbolSpinner.postDelayed(() -> {
                    OnItemSelectedListener l = symbolSpinner.getOnItemSelectedListener();
                    symbolSpinner.setOnItemSelectedListener(null);
                    symbolSpinner.setSelection(pos);
                    symbolSpinner.setOnItemSelectedListener(l);
                }, 500);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // required, but not used
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout((int) (Screen.getScreenSmallDimension(getActivity()) * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(LAYER_INDEX_KEY, layerIndex);
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
                layer.setStrokeWidth(subLayerName, layoutParams.height);
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

    class SimpleImageArrayAdapter extends ArrayAdapter<ImageView> {

        /**
         * Construct an adapter for ImageViews
         * 
         * @param context an Android Context
         * @param images an array of ImageViews
         */
        public SimpleImageArrayAdapter(@NonNull Context context, @NonNull ImageView[] images) {
            super(context, 0, images);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getItem(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getItem(position);
        }
    }
}
