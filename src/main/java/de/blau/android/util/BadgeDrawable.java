package de.blau.android.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import de.blau.android.R;

/**
 * Based on https://mobikul.com/adding-badge-count-on-menu-items-like-cart-notification-etc/
 * 
 * Unluckily google BadgeDrawable seems to rather non-functional and without a stable API currently.
 * 
 * @author simon
 *
 */
public class BadgeDrawable extends Drawable {

    private static final int MAX      = 1000;
    private Paint            mBadgePaint;
    private Paint            mBadgePaint1;
    private Paint            mTextPaint;
    private Rect             mTxtRect = new Rect();

    private final int okColor;
    private final int warnColor;
    private final int errorColor;

    private int     mCount = 0;
    private boolean mWillDraw;

    private final int okLimit;
    private final int warnLimit;

    /**
     * Create a new BadgeDrawable
     * 
     * @param context an Android Context
     * @param okLimit values below this are ok
     * @param warnLimit values below this have warning status
     */
    public BadgeDrawable(@NonNull Context context, int okLimit, int warnLimit) {
        this.okLimit = okLimit;
        this.warnLimit = warnLimit;

        float mTextSize = context.getResources().getDimension(R.dimen.badge_text_size);

        okColor = ContextCompat.getColor(context.getApplicationContext(), R.color.osm_green);
        warnColor = ContextCompat.getColor(context.getApplicationContext(), ThemeUtils.getResIdFromAttribute(context, R.attr.snack_warning));
        errorColor = ContextCompat.getColor(context.getApplicationContext(), ThemeUtils.getResIdFromAttribute(context, R.attr.colorError));

        mBadgePaint = new Paint();
        mBadgePaint.setColor(errorColor);
        mBadgePaint.setAntiAlias(true);
        mBadgePaint.setStyle(Paint.Style.FILL);
        mBadgePaint1 = new Paint();
        mBadgePaint1.setColor(ContextCompat.getColor(context.getApplicationContext(), R.color.colorLightGray));
        mBadgePaint1.setAntiAlias(true);
        mBadgePaint1.setStyle(Paint.Style.FILL);

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @SuppressLint("NewApi")
    @Override
    public void draw(Canvas canvas) {
        if (!mWillDraw) {
            return;
        }
        Rect bounds = getBounds();
        float width = (float) bounds.right - bounds.left;
        float height = (float) bounds.bottom - bounds.top;

        String countString = mCount >= MAX ? (MAX - 1) + "+" : Integer.toString(mCount);
        mTextPaint.getTextBounds(countString, 0, countString.length(), mTxtRect);

        // Position the badge in the top-right quadrant of the icon.
        float radius = (Math.max(width, height) / 2f) / 2;
        float halfWidth = Math.max(mTxtRect.width() / 2f, radius);
        float centerX = (width - halfWidth - 1) + 5;
        float centerY = radius - 5;
        if (mCount < okLimit) {
            mBadgePaint.setColor(okColor);
        } else if (mCount < warnLimit) {
            mBadgePaint.setColor(warnColor);
        } else {
            mBadgePaint.setColor(errorColor);
        }
        canvas.drawRoundRect(centerX - halfWidth - 7.5f, centerY - radius - 7.5f, centerX + halfWidth + 7.5f, centerY + radius + 7.5f, radius, radius,
                mBadgePaint1);
        canvas.drawRoundRect(centerX - halfWidth - 5.5f, centerY - radius - 5.5f, centerX + halfWidth + 5.5f, centerY + radius + 5.5f, radius, radius,
                mBadgePaint);
        float textHeight = (float) mTxtRect.bottom - mTxtRect.top;
        float textY = centerY + (textHeight / 2f);
        canvas.drawText(countString, centerX, textY, mTextPaint);
    }

    /**
     * Sets the count (i.e notifications) to display
     * 
     * @param count the count
     */
    public void setCount(int count) {
        mCount = count;

        // Only draw a badge if there are notifications.
        mWillDraw = count > 0;
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        // do nothing
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // do nothing
    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    /**
     * Add a badge with count to the icon
     * 
     * @param context an Android Context
     * @param icon the icon
     * @param count the current count
     * @param okLimit values below this are ok
     * @param warnLimit values below this have warning status
     */
    public static void setBadgeWithCount(@NonNull Context context, @NonNull LayerDrawable icon, int count, int okLimit, int warnLimit) {
        BadgeDrawable badge;

        // Reuse drawable if possible
        Drawable reuse = icon.findDrawableByLayerId(R.id.ic_badge);
        if (reuse instanceof BadgeDrawable && ((BadgeDrawable) reuse).okLimit == okLimit && ((BadgeDrawable) reuse).warnLimit == warnLimit) {
            badge = (BadgeDrawable) reuse;
        } else {
            badge = new BadgeDrawable(context, okLimit, warnLimit);
        }

        badge.setCount(count);
        icon.mutate();
        icon.setDrawableByLayerId(R.id.ic_badge, badge);
    }
}
