package de.blau.android.propertyeditor.tagform;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;
import de.blau.android.R;

abstract class ShowDialogOnClickListener implements OnClickListener {

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
        View finalView = v;
        finalView.setEnabled(false); // debounce
        final Object tag = finalView.getTag();
        dialog.setOnShowListener(d -> {
            if (tag instanceof String) {
                ComboDialogRow.scrollDialogToValue((String) tag, dialog, R.id.valueGroup);
            }
        });
        dialog.setOnDismissListener(d -> finalView.setEnabled(true));
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(unused -> {
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
