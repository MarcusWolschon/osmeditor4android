package de.blau.android.util;

import android.app.Dialog;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.core.view.WindowInsetsCompat;
import de.blau.android.R;

/**
 * Non-fixed width version of DialogFragment
 */
public abstract class MaxHeightDialogFragment extends CancelableDialogFragment {

    private int origHeight;

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            final Window window = dialog.getWindow();
            origHeight = window.getAttributes().height;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                // this doesn't really make any sense but works
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, window.getAttributes().height);
                return;
            }
            View parentPanel = dialog.findViewById(R.id.parentPanel);
            if (parentPanel != null) {
                ViewGroupCompat.installCompatInsetsDispatch(parentPanel);
                ViewCompat.setOnApplyWindowInsetsListener(parentPanel, onApplyWindowInsetslistener);
            }
        }
    }

    /**
     * Insets listener that sets window size
     */
    public final OnApplyWindowInsetsListener onApplyWindowInsetslistener = (v, windowInsets) -> {
        Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.navigationBars() | WindowInsetsCompat.Type.ime());
        Dialog dialog = getDialog();
        if (dialog != null) {
            final Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, origHeight - (insets.top + insets.bottom));
        }
        return  WindowInsetsCompat.CONSUMED;
    };
}
