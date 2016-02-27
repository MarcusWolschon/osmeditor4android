package de.blau.android.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

/**
 * Hack: offsets dropdown to the left by the views height and make it wider by the same amount
 * @author simon
 *
 */
public class OffsettedAutoCompleteTextView extends AutoCompleteTextView {
	
	private static final String DEBUG_TAG = OffsettedAutoCompleteTextView.class.getName();
	
	private int parentWidth = -1;

	public OffsettedAutoCompleteTextView(Context context) {
		super(context);
	}
	
	public OffsettedAutoCompleteTextView(Context context,  AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		// Log.d(DEBUG_TAG, "onSizeChanged");

		if (w == 0 && h == 0) {
			return;
		}
		// Log.d(DEBUG_TAG,"w=" + w +" h="+h);
		// this is not really satisfactory
		if (parentWidth == -1) {
			// upps
			return;
		}
		int ddw = parentWidth - w;
		setDropDownHorizontalOffset(-ddw);
		setDropDownWidth(ddw);
	}	
	
	public void setParentWidth(int parentWidth) {
		this.parentWidth = parentWidth;
	}
}
