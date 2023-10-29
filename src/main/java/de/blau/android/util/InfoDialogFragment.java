package de.blau.android.util;

import java.util.Locale;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class InfoDialogFragment extends ImmersiveDialogFragment {
    
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout((int) (Screen.getScreenSmallDimension(getActivity()) * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
    protected abstract View createView(@Nullable ViewGroup container);
    
    /**
     * Get the string resource formated as an italic string
     * 
     * @param resId String resource id
     * @return a Spanned containing the string
     */
    protected Spanned toItalic(int resId) {
        return Util.fromHtml("<i>" + getString(resId) + "</i>");
    }

    /**
     * Pretty print a coordinate value
     * 
     * @param coord the coordinate in WGS84
     * @return a reasonable looking string representation
     */
    @NonNull
    protected static String prettyPrintCoord(double coord) {
        return String.format(Locale.US, "%.7f°", coord);
    }
}
