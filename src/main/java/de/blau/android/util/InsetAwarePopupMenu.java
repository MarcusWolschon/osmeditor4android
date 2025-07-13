package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.lang.reflect.Field;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.MenuPopupWindow;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Use Reflection to create an Inset aware PopupMenu
 */
public class InsetAwarePopupMenu extends PopupMenu {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, InsetAwarePopupMenu.class.getSimpleName().length());
    private static final String DEBUG_TAG = InsetAwarePopupMenu.class.getSimpleName().substring(0, TAG_LEN);

    private static final String M_POPUP = "mPopup";

    public InsetAwarePopupMenu(@NonNull Context context, @NonNull View anchor) {
        super(context, anchor);
    }

    @Override
    public void show() {
        super.show();
        // the popup doesn't exist before show is called
        try {
            final Field menuPopupHelperField = InsetAwarePopupMenu.class.getSuperclass().getDeclaredField(M_POPUP);
            menuPopupHelperField.setAccessible(true); // NOSONAR
            Object menuPopupHelper = menuPopupHelperField.get(this);
            if (menuPopupHelper == null) {
                throw new IllegalArgumentException("MenuPopupHelper not found");
            }

            final Field menuPopupField = menuPopupHelper.getClass().getDeclaredField(M_POPUP);
            menuPopupField.setAccessible(true); // NOSONAR
            Object menuPopup = menuPopupField.get(menuPopupHelper);
            if (menuPopup == null) {
                throw new IllegalArgumentException("MenuPopup not found");
            }

            final Field menuPopupWindowField = menuPopup.getClass().getDeclaredField(M_POPUP);
            menuPopupWindowField.setAccessible(true); // NOSONAR
            Object menuPopupWindow = menuPopupWindowField.get(menuPopup);
            if (menuPopupWindow == null) {
                throw new IllegalArgumentException("MenuPopupWindow not found");
            }

            View view = ((MenuPopupWindow) menuPopupWindow).getListView();
            if (view == null) {
                throw new IllegalArgumentException("MenuPopupWindow View not found");
            }

            View parent = (View) view.getParent();
            if (parent != null) {
                LayoutParams lp = parent.getLayoutParams();
                if (lp instanceof MarginLayoutParams) {
                    view = parent;
                }
            }
            ViewCompat.setOnApplyWindowInsetsListener(view, onApplyWindowInsetslistener);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
            Log.e(DEBUG_TAG, "Failed to use reflection to find view " + e.getMessage());
        }
    }

    /**
     * Non-Standard insets listener, setting the compat listeners doesn't seem to work here.
     */
    private static final OnApplyWindowInsetsListener onApplyWindowInsetslistener = (v, windowInsets) -> {
        LayoutParams lp = v.getLayoutParams();
        if (lp instanceof MarginLayoutParams) {
            Insets insets = windowInsets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.navigationBars());
            MarginLayoutParams mlp = (MarginLayoutParams) lp;
            mlp.leftMargin = insets.left;
            mlp.bottomMargin = insets.bottom;
            mlp.rightMargin = insets.right;
            mlp.topMargin = insets.top;
            v.setLayoutParams(mlp);
        } else {
            Log.e(DEBUG_TAG, "Failed to find MarginLayoutParams");
        }
        return windowInsets;
    };

}
