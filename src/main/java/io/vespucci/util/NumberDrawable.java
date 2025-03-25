package io.vespucci.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import io.vespucci.R;

/**
 * A drawable that displays a number
 * 
 * @author simon
 *
 */
public class NumberDrawable extends BaseDrawable {

    private Paint mTextPaint;
    private Rect  mTxtRect = new Rect();

    private final int numberColor;

    private String mNumberString = "0";

    /**
     * Create a new BadgeDrawable
     * 
     * @param context an Android Context
     */
    public NumberDrawable(@NonNull Context context) {

        float mTextSize = context.getResources().getDimension(R.dimen.button_text_size);

        numberColor = ContextCompat.getColor(context.getApplicationContext(), ThemeUtils.getResIdFromAttribute(context, R.attr.button_color));

        mTextPaint = new Paint();
        mTextPaint.setColor(numberColor);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @SuppressLint("NewApi")
    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float width = (float) bounds.right - bounds.left;
        float height = (float) bounds.bottom - bounds.top;

        float centerX = width / 2f + mTxtRect.width() / 2f - 5;
        float centerY = height / 2f + mTxtRect.height() / 2f + 5;

        canvas.drawText(mNumberString, centerX, centerY, mTextPaint);
    }

    /**
     * Sets the count (i.e notifications) to display
     * 
     * @param number the count
     */
    public void setNumber(int number) {
        mNumberString = Integer.toString(number);
        mTextPaint.getTextBounds(mNumberString, 0, mNumberString.length(), mTxtRect);

        invalidateSelf();
    }

    @Override
    public int getIntrinsicWidth() {
        return Math.max(getIntrinsicHeight(), mTxtRect.width()); // a "look nice" hack
    }

    @Override
    public int getIntrinsicHeight() {
        return mTxtRect.height();
    }
}
