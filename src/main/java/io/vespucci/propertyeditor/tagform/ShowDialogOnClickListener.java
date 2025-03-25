package io.vespucci.propertyeditor.tagform;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;
import io.vespucci.R;

abstract class ShowDialogOnClickListener implements OnClickListener {

    private static final long DEBOUNCE_DELAY = 1000;

    /**
     * Get the AlertDialog to display
     * 
     * @return an AlertDialog
     */
    public abstract AlertDialog buildDialog();

    @SuppressLint("NewApi")
    @Override
    public void onClick(View v) {
        final AlertDialog dialog = buildDialog();
        v.setEnabled(false); // debounce
        v.postDelayed(() -> v.setEnabled(true), DEBOUNCE_DELAY);
        final Object tag = v.getTag();
        dialog.setOnShowListener(d -> {
            if (tag instanceof String) {
                ComboDialogRow.scrollDialogToValue((String) tag, dialog, R.id.valueGroup);
            }
        });
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(unused -> {
            LinearLayout valueGroup = (LinearLayout) dialog.findViewById(R.id.valueGroup);
            for (int pos = 0; pos < valueGroup.getChildCount(); pos++) {
                View c = valueGroup.getChildAt(pos);
                if (c instanceof AppCompatCheckBox) {
                    ((AppCompatCheckBox) c).setChecked(false);
                }
            }
        });
    }
}
