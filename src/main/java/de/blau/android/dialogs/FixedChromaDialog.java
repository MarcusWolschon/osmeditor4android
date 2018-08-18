package de.blau.android.dialogs;

import com.pavelsikun.vintagechroma.ChromaDialog;
import com.pavelsikun.vintagechroma.IndicatorMode;
import com.pavelsikun.vintagechroma.OnColorSelectedListener;
import com.pavelsikun.vintagechroma.R;
import com.pavelsikun.vintagechroma.colormode.ColorMode;
import com.pavelsikun.vintagechroma.view.ChromaView;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.WindowManager;

/**
 * 
 * Fixes to ChromaDialog from https://github.com/Kunzisoft/AndroidClearChroma
 * 
 * AndroidClearChroma utilizes support lib v 27 and currently can't be used directly because of that
 *
 */
public class FixedChromaDialog extends com.pavelsikun.vintagechroma.ChromaDialog {

    private final static String ARG_INITIAL_COLOR  = "arg_initial_color";
    private final static String ARG_COLOR_MODE_ID  = "arg_color_mode_id";
    private final static String ARG_INDICATOR_MODE = "arg_indicator_mode";

    private static FixedChromaDialog newInstance(@ColorInt int initialColor, ColorMode colorMode, IndicatorMode indicatorMode) {
        FixedChromaDialog fragment = new FixedChromaDialog();
        fragment.setArguments(makeArgs(initialColor, colorMode, indicatorMode));
        return fragment;
    }

    private static Bundle makeArgs(@ColorInt int initialColor, ColorMode colorMode, IndicatorMode indicatorMode) {
        Bundle args = new Bundle();
        args.putInt(ARG_INITIAL_COLOR, initialColor);
        args.putInt(ARG_COLOR_MODE_ID, colorMode.ordinal());
        args.putInt(ARG_INDICATOR_MODE, indicatorMode.ordinal());
        return args;
    }

    public static class Builder extends ChromaDialog.Builder {
        private @ColorInt int           initialColor  = ChromaView.DEFAULT_COLOR;
        private ColorMode               colorMode     = ChromaView.DEFAULT_MODE;
        private IndicatorMode           indicatorMode = IndicatorMode.DECIMAL;
        private OnColorSelectedListener listener      = null;

        @Override
        public Builder initialColor(@ColorInt int initialColor) {
            this.initialColor = initialColor;
            return this;
        }

        @Override
        public Builder colorMode(ColorMode colorMode) {
            this.colorMode = colorMode;
            return this;
        }

        @Override
        public Builder indicatorMode(IndicatorMode indicatorMode) {
            this.indicatorMode = indicatorMode;
            return this;
        }

        @Override
        public Builder onColorSelected(OnColorSelectedListener listener) {
            this.listener = listener;
            return this;
        }

        @Override
        public FixedChromaDialog create() {
            FixedChromaDialog fragment = newInstance(initialColor, colorMode, indicatorMode);
            fragment.setListener(listener);
            return fragment;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Dialog ad = super.onCreateDialog(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            Log.e("FixedChromaDialog", "setting listener");
            ad.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    measureLayout((AlertDialog) ad);
                }
            });
        }

        return ad;
    }

    private void measureLayout(AlertDialog ad) {
        int widthMultiplier = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 2 : 1;

        TypedValue typedValue = new TypedValue();
        getResources().getValue(R.dimen.chroma_dialog_height_multiplier, typedValue, true);
        float heightMultiplier = typedValue.getFloat();

        final int DELTA_HEIGHT = 0; // 100;
        int height = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                ? (int) (ad.getContext().getResources().getDisplayMetrics().heightPixels * heightMultiplier) + DELTA_HEIGHT
                : WindowManager.LayoutParams.WRAP_CONTENT;
        int width = getResources().getDimensionPixelSize(R.dimen.chroma_dialog_width) * widthMultiplier;

        ad.getWindow().setLayout(width, height);
    }

}
