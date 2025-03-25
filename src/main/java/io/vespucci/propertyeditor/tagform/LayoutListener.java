package io.vespucci.propertyeditor.tagform;

import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import io.vespucci.views.CustomAutoCompleteTextView;

public class LayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
    final TextView                   ourKeyView;
    final CustomAutoCompleteTextView ourValueView;
    final int                        length;

    /**
     * Create a new listener
     * 
     * @param keyView the View holding the key
     * @param valueView the View holding the Value
     * @param length the length of the value view in characters
     */
    public LayoutListener(@NonNull TextView keyView, @NonNull CustomAutoCompleteTextView valueView, int length) {
        this.ourKeyView = keyView;
        this.ourValueView = valueView;
        this.length = length;
    }

    @Override
    public void onGlobalLayout() {
        ViewTreeObserver observer = ourValueView.getViewTreeObserver();
        observer.removeOnGlobalLayoutListener(this);
        float aM = ourValueView.getPaint().measureText("M"); // FIXME cache this
        int lines = Math.min((int) (length / aM), 4);
        if (lines > 1) {
            ourValueView.setLines(lines);
            ourValueView.setMaxLines(lines);
            ourValueView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) ourValueView.getLayoutParams();
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            ourValueView.setLayoutParams(layoutParams);
            ourValueView.setGravity(Gravity.TOP);
            layoutParams = (LinearLayout.LayoutParams) ourKeyView.getLayoutParams();
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            ourKeyView.setLayoutParams(layoutParams);
            ourKeyView.setGravity(Gravity.TOP);
            ourValueView.requestLayout();
        }
    }
}
