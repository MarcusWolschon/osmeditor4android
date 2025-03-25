package io.vespucci.util;

import java.util.Locale;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;

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
     * Create the view we want to display
     * 
     * Classes extending LayerInfo need to override this but call through to the super method to get the view
     * 
     * @param container parent view or null
     * @return the View
     */
    @NonNull
    protected ScrollView createEmptyView(@Nullable ViewGroup container) {
        LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        return (ScrollView) inflater.inflate(R.layout.element_info_view, container, false);
    }
    
    /**
     * Setup the table layout params
     * 
     * @return an instance of TableLayout.LayoutParams
     */
    @NonNull
    public static TableLayout.LayoutParams getTableLayoutParams() {
        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.setMargins(10, 2, 10, 2);
        return tp;
    }
    
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
