/*
 * Android Split Pane Layout. https://github.com/MobiDevelop/android-split-pane-layout
 * 
 * Copyright (C) 2012 Justin Shapcott
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.vespucci.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;
import io.vespucci.exception.UiStateException;

/**
 * A layout that splits the available space between two child views.
 * 
 * An optionally movable bar exists between the children which allows the user to redistribute the space allocated to
 * each view.
 */
public class SplitPaneLayout extends ViewGroup {
    private static final int DEFAULT_DRAGGING_COLOR   = 0x88FFFFFF;
    private static final int DEFAULT_COLOR            = 0xFF000000;
    private static final int DEFAULT_HANDLE_DIMENSION = 36;
    private static final int DEFAULT_HANDLE_RADIUS    = 18;
    private static final int ORIENTATION_HORIZONTAL   = 0;
    private static final int ORIENTATION_VERTICAL     = 1;         // NOSONAR

    private int     mOrientation             = 0;
    private int     mSplitterSize            = 12;
    private boolean mSplitterMovable         = true;
    private int     mSplitterPosition        = Integer.MIN_VALUE;
    private float   mSplitterPositionPercent = 0.5f;

    private Drawable mSplitterDrawable;
    private Drawable mSplitterDraggingDrawable;
    private Drawable mHandleDrawable;
    private Drawable mHandleDraggingDrawable;

    private Rect mSplitterRect = new Rect();
    private Rect mHandleRect   = new Rect();

    private int     lastX;
    private int     lastY;
    private Rect    temp       = new Rect();
    private boolean isDragging = false;

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     */
    public SplitPaneLayout(@NonNull Context context) {
        super(context);
        mSplitterPositionPercent = 0.5f;
        mSplitterDrawable = new PaintDrawable(DEFAULT_COLOR);
        mSplitterDraggingDrawable = new PaintDrawable(DEFAULT_DRAGGING_COLOR);

        createDefaultHandle();
    }

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     * @param attrs the attributes of the XML tag that is inflating the view. This value may be null.
     */
    public SplitPaneLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        extractAttributes(context, attrs);
    }

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     * @param attrs the attributes of the XML tag that is inflating the view. This value may be null.
     * @param defStyle an attribute in the current theme that contains a reference to a style resource that supplies
     *            default values for the view. Can be 0 to not look for defaults.
     */
    public SplitPaneLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        extractAttributes(context, attrs);
    }

    /**
     * Create a default, circle, handle
     */
    private void createDefaultHandle() {
        Path handlePath = new Path();
        handlePath.addCircle(DEFAULT_HANDLE_RADIUS, DEFAULT_HANDLE_RADIUS, DEFAULT_HANDLE_RADIUS, Direction.CW);
        PathShape handleShape = new PathShape(handlePath, DEFAULT_HANDLE_DIMENSION, DEFAULT_HANDLE_DIMENSION);

        mHandleDrawable = new ShapeDrawable(handleShape);

        ((ShapeDrawable) mHandleDrawable).getPaint().setStyle(Style.STROKE);
        ((ShapeDrawable) mHandleDrawable).getPaint().setColor(DEFAULT_COLOR);
        ((ShapeDrawable) mHandleDrawable).getPaint().setStrokeWidth(mSplitterSize);

        ((ShapeDrawable) mHandleDrawable).setIntrinsicWidth(DEFAULT_HANDLE_DIMENSION);
        ((ShapeDrawable) mHandleDrawable).setIntrinsicHeight(DEFAULT_HANDLE_DIMENSION);
        mHandleDraggingDrawable = new ShapeDrawable(handleShape);

        ((ShapeDrawable) mHandleDraggingDrawable).getPaint().setStyle(Style.FILL_AND_STROKE);
        ((ShapeDrawable) mHandleDraggingDrawable).getPaint().setColor(DEFAULT_DRAGGING_COLOR);
        ((ShapeDrawable) mHandleDraggingDrawable).getPaint().setStrokeWidth(mSplitterSize);

        ((ShapeDrawable) mHandleDraggingDrawable).setIntrinsicWidth(DEFAULT_HANDLE_DIMENSION);
        ((ShapeDrawable) mHandleDraggingDrawable).setIntrinsicHeight(DEFAULT_HANDLE_DIMENSION);
    }

    /**
     * If an AttributeSet is available extract attributes from it and use those
     * 
     * @param context an Android Context
     * @param attrs the AttributeSet
     */
    private void extractAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
        createDefaultHandle();
        if (attrs == null) {
            return;
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SplitPaneLayout);
        mOrientation = a.getInt(R.styleable.SplitPaneLayout_orientation, 0);
        mSplitterSize = a.getDimensionPixelSize(R.styleable.SplitPaneLayout_splitterSize,
                context.getResources().getDimensionPixelSize(R.dimen.spl_default_splitter_size));
        mSplitterMovable = a.getBoolean(R.styleable.SplitPaneLayout_splitterMovable, true);
        TypedValue value = a.peekValue(R.styleable.SplitPaneLayout_splitterPosition);
        if (value != null) {
            if (value.type == TypedValue.TYPE_DIMENSION) {
                mSplitterPosition = a.getDimensionPixelSize(R.styleable.SplitPaneLayout_splitterPosition, Integer.MIN_VALUE);
            } else if (value.type == TypedValue.TYPE_FRACTION) {
                mSplitterPositionPercent = a.getFraction(R.styleable.SplitPaneLayout_splitterPosition, 100, 100, 50) * 0.01f;
            }
        } else {
            mSplitterPosition = Integer.MIN_VALUE;
            mSplitterPositionPercent = 0.5f;
        }
        value = a.peekValue(R.styleable.SplitPaneLayout_splitterBackground);
        if (value != null) {
            if (isStringOrReferenceResource(value)) {
                mSplitterDrawable = a.getDrawable(R.styleable.SplitPaneLayout_splitterBackground);
            } else if (isColorResource(value)) {
                mSplitterDrawable = new PaintDrawable(a.getColor(R.styleable.SplitPaneLayout_splitterBackground, DEFAULT_COLOR));
            }
        }
        value = a.peekValue(R.styleable.SplitPaneLayout_splitterDraggingBackground);
        if (value != null) {
            if (isStringOrReferenceResource(value)) {
                mSplitterDraggingDrawable = a.getDrawable(R.styleable.SplitPaneLayout_splitterDraggingBackground);
            } else if (isColorResource(value)) {
                mSplitterDraggingDrawable = new PaintDrawable(a.getColor(R.styleable.SplitPaneLayout_splitterDraggingBackground, DEFAULT_DRAGGING_COLOR));
            }
        } else {
            mSplitterDraggingDrawable = new PaintDrawable(DEFAULT_DRAGGING_COLOR);
        }

        value = a.peekValue(R.styleable.SplitPaneLayout_handleBackground);
        if (value != null) {
            if (isStringOrReferenceResource(value)) {
                mHandleDrawable = a.getDrawable(R.styleable.SplitPaneLayout_handleBackground);
            } else if (isColorResource(value)) {
                ((ShapeDrawable) mHandleDrawable).getPaint().setColor(a.getColor(R.styleable.SplitPaneLayout_handleBackground, DEFAULT_COLOR));
                ((ShapeDrawable) mHandleDrawable).getPaint().setStrokeWidth(mSplitterSize);
            }
        }
        value = a.peekValue(R.styleable.SplitPaneLayout_handleDraggingBackground);
        if (value != null) {
            if (isStringOrReferenceResource(value)) {
                mHandleDraggingDrawable = a.getDrawable(R.styleable.SplitPaneLayout_handleDraggingBackground);
            } else if (isColorResource(value)) {
                ((ShapeDrawable) mHandleDraggingDrawable).getPaint()
                        .setColor(a.getColor(R.styleable.SplitPaneLayout_handleDraggingBackground, DEFAULT_DRAGGING_COLOR));
                ((ShapeDrawable) mHandleDraggingDrawable).getPaint().setStrokeWidth(mSplitterSize);
            }
        }
        a.recycle();
    }

    /**
     * Check if value is a string or reference resource
     * 
     * @param value the value
     * @return true if it is a string or reference resource
     */
    private boolean isStringOrReferenceResource(TypedValue value) {
        return value.type == TypedValue.TYPE_REFERENCE || value.type == TypedValue.TYPE_STRING;
    }

    /**
     * Check if value is a color resource
     * 
     * @param value the value
     * @return true if it is a color resource
     */
    private boolean isColorResource(@NonNull TypedValue value) {
        return value.type == TypedValue.TYPE_INT_COLOR_ARGB8 || value.type == TypedValue.TYPE_INT_COLOR_ARGB4 || value.type == TypedValue.TYPE_INT_COLOR_RGB8
                || value.type == TypedValue.TYPE_INT_COLOR_RGB4;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        check();
        if (widthSize <= 0 || heightSize <= 0) {
            return;
        }
        final int halfOfSplitter = mSplitterSize / 2;
        if (mOrientation == ORIENTATION_HORIZONTAL) {
            if (mSplitterPosition == Integer.MIN_VALUE && mSplitterPositionPercent < 0) {
                mSplitterPosition = widthSize / 2;
            } else if (mSplitterPositionPercent >= 0) {
                mSplitterPosition = (int) (widthSize * mSplitterPositionPercent);
            } else if (mSplitterPosition != Integer.MIN_VALUE && mSplitterPositionPercent < 0) {
                calcPercent(mSplitterPosition, widthSize);
            }
            getChildAt(0).measure(MeasureSpec.makeMeasureSpec(mSplitterPosition - halfOfSplitter, widthMode),
                    MeasureSpec.makeMeasureSpec(heightSize, heightMode));
            getChildAt(1).measure(MeasureSpec.makeMeasureSpec(widthSize - halfOfSplitter - mSplitterPosition, widthMode),
                    MeasureSpec.makeMeasureSpec(heightSize, heightMode));
            return;
        }
        // ORIENTATION_VERTICAL
        if (mSplitterPosition == Integer.MIN_VALUE && mSplitterPositionPercent < 0) {
            mSplitterPosition = heightSize / 2;
        } else if (mSplitterPositionPercent >= 0) {
            mSplitterPosition = (int) (heightSize * mSplitterPositionPercent);
        } else if (mSplitterPosition != Integer.MIN_VALUE && mSplitterPositionPercent < 0) {
            calcPercent(mSplitterPosition, heightSize);
        }
        getChildAt(0).measure(MeasureSpec.makeMeasureSpec(widthSize, widthMode), MeasureSpec.makeMeasureSpec(mSplitterPosition - halfOfSplitter, heightMode));
        getChildAt(1).measure(MeasureSpec.makeMeasureSpec(widthSize, widthMode),
                MeasureSpec.makeMeasureSpec(heightSize - halfOfSplitter - mSplitterPosition, heightMode));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        int h = b - t;
        final int halfOfSplitter = mSplitterSize / 2;
        if (mOrientation == ORIENTATION_HORIZONTAL) {
            getChildAt(0).layout(0, 0, mSplitterPosition - halfOfSplitter, h);
            mSplitterRect.set(mSplitterPosition - halfOfSplitter, 0, mSplitterPosition + halfOfSplitter, h);
            getChildAt(1).layout(mSplitterPosition + halfOfSplitter, 0, r, h);
        } else if (mOrientation == ORIENTATION_VERTICAL) {
            getChildAt(0).layout(0, 0, w, mSplitterPosition - halfOfSplitter);
            mSplitterRect.set(0, mSplitterPosition - halfOfSplitter, w, mSplitterPosition + halfOfSplitter);
            getChildAt(1).layout(0, mSplitterPosition + halfOfSplitter, w, h);
        }
        int cX = mSplitterRect.centerX();
        int cY = mSplitterRect.centerY();
        Rect handleBounds = mHandleDrawable.getBounds();
        final int handleW2 = handleBounds.width() / 2;
        final int handleH2 = handleBounds.height() / 2;
        mHandleRect.set(cX - handleW2, cY - handleH2, cX + handleW2, cY + handleH2);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        return mHandleRect.contains(x, y);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mSplitterMovable) {
            return false;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if (mSplitterRect.contains(x, y) || mHandleRect.contains(x, y)) {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                isDragging = true;
                temp.set(mSplitterRect);
                invalidate();
                lastX = x;
                lastY = y;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (isDragging) {
                if (mOrientation == ORIENTATION_HORIZONTAL) {
                    temp.offset((x - lastX), 0);
                } else if (mOrientation == ORIENTATION_VERTICAL) {
                    temp.offset(0, y - lastY);
                }
                lastX = x;
                lastY = y;
                invalidate();
            }
            break;
        case MotionEvent.ACTION_UP:
            if (isDragging) {
                isDragging = false;
                // note that the relative pos has to be set here
                if (mOrientation == ORIENTATION_HORIZONTAL) {
                    calcPercent(x, getWidth());
                } else if (mOrientation == ORIENTATION_VERTICAL) {
                    calcPercent(y, getHeight());
                }
                remeasure();
                requestLayout();
            }
            break;
        default:
            return false;
        }
        return true;
    }

    /**
     * Calculate the relative position
     * 
     * @param absPos the position in pixels
     * @param max the max value it can have
     */
    private void calcPercent(int absPos, int max) {
        mSplitterPosition = Math.min(absPos, max);
        mSplitterPositionPercent = (float) mSplitterPosition / (float) max;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.mSplitterPositionPercent = mSplitterPositionPercent;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setSplitterPositionPercent(ss.mSplitterPositionPercent);
    }

    /**
     * Convenience for calling own measure method.
     */
    private void remeasure() {
        measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
    }

    /**
     * Checks that we have exactly two children.
     */
    private void check() {
        if (getChildCount() != 2) {
            throw new UiStateException("SplitPaneLayout must have exactly two child views.");
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mSplitterDrawable != null) {
            mSplitterDrawable.setBounds(mSplitterRect);
            mSplitterDrawable.draw(canvas);
            int cX = mSplitterRect.centerX();
            int cY = mSplitterRect.centerY();
            int w2 = mHandleDrawable.getIntrinsicWidth() / 2;
            int h2 = mHandleDrawable.getIntrinsicHeight() / 2;
            mHandleDrawable.setBounds(cX - w2, cY - h2, cX + w2, cY + h2);
            mHandleDrawable.draw(canvas);
            mHandleRect.set(mHandleDrawable.getBounds());
        }
        if (isDragging) {
            mSplitterDraggingDrawable.setBounds(temp);
            mSplitterDraggingDrawable.draw(canvas);
            int cX = temp.centerX();
            int cY = temp.centerY();
            int w2 = mHandleDraggingDrawable.getIntrinsicWidth() / 2;
            int h2 = mHandleDraggingDrawable.getIntrinsicHeight() / 2;
            mHandleDraggingDrawable.setBounds(cX - w2, cY - h2, cX + w2, cY + h2);
            mHandleDraggingDrawable.draw(canvas);
        }
    }

    /**
     * Gets the current drawable used for the splitter.
     *
     * @return the drawable used for the splitter
     */
    public Drawable getSplitterDrawable() {
        return mSplitterDrawable;
    }

    /**
     * Sets the drawable used for the splitter.
     *
     * @param splitterDrawable the desired orientation of the layout
     */
    public void setSplitterDrawable(Drawable splitterDrawable) {
        mSplitterDrawable = splitterDrawable;
        if (getChildCount() == 2) {
            remeasure();
        }
    }

    /**
     * Gets the current drawable used for the splitter dragging overlay.
     *
     * @return the drawable used for the splitter
     */
    public Drawable getSplitterDraggingDrawable() {
        return mSplitterDraggingDrawable;
    }

    /**
     * Sets the drawable used for the splitter dragging overlay.
     *
     * @param splitterDraggingDrawable the drawable to use while dragging the splitter
     */
    public void setSplitterDraggingDrawable(Drawable splitterDraggingDrawable) {
        mSplitterDraggingDrawable = splitterDraggingDrawable;
        if (isDragging) {
            invalidate();
        }
    }

    /**
     * Gets the current orientation of the layout.
     *
     * @return the orientation of the layout
     */
    public int getOrientation() {
        return mOrientation;
    }

    /**
     * Sets the orientation of the layout.
     *
     * @param orientation the desired orientation of the layout
     */
    public void setOrientation(int orientation) {
        if (mOrientation != orientation) {
            mOrientation = orientation;
            if (getChildCount() == 2) {
                remeasure();
            }
        }
    }

    /**
     * Gets the current size of the splitter in pixels.
     *
     * @return the size of the splitter
     */
    public int getSplitterSize() {
        return mSplitterSize;
    }

    /**
     * Sets the current size of the splitter in pixels.
     *
     * @param splitterSize the desired size of the splitter
     */
    public void setSplitterSize(int splitterSize) {
        mSplitterSize = splitterSize;
        if (getChildCount() == 2) {
            remeasure();
        }
    }

    /**
     * Gets whether the splitter is movable by the user.
     *
     * @return whether the splitter is movable
     */
    public boolean isSplitterMovable() {
        return mSplitterMovable;
    }

    /**
     * Sets whether the splitter is movable by the user.
     *
     * @param splitterMovable whether the splitter is movable
     */
    public void setSplitterMovable(boolean splitterMovable) {
        mSplitterMovable = splitterMovable;
    }

    /**
     * Gets the current position of the splitter in pixels.
     *
     * @return the position of the splitter
     */
    public int getSplitterPosition() {
        return mSplitterPosition;
    }

    /**
     * Sets the current position of the splitter in pixels.
     *
     * @param position the desired position of the splitter
     */
    public void setSplitterPosition(int position) {
        if (position < 0) {
            position = 0;
        }
        mSplitterPosition = position;
        mSplitterPositionPercent = -1;
        remeasure();
    }

    /**
     * Gets the current position of the splitter as a percent.
     *
     * @return the position of the splitter
     */
    public float getSplitterPositionPercent() {
        return mSplitterPositionPercent;
    }

    /**
     * Sets the current position of the splitter as a percentage of the layout.
     *
     * @param position the desired position of the splitter
     */
    public void setSplitterPositionPercent(float position) {
        if (position < 0) {
            position = 0;
        }
        if (position > 1) {
            position = 1;
        }
        mSplitterPosition = Integer.MIN_VALUE;
        mSplitterPositionPercent = position;
        remeasure();
    }

    /**
     * Holds important values when we need to save instance state.
     */
    public static class SavedState extends BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {

            /**
             * Get a new SavedState object
             * 
             * @param in a Parcel
             * @return a SavedState object
             */
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            /**
             * Get an array for SavedState objects
             * 
             * @param size the size of the Array
             * @return an SavedState array of size size
             */
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        float mSplitterPositionPercent;

        /**
         * Construct a new instance from a Parcelable
         * 
         * @param superState saved state
         */
        SavedState(@NonNull Parcelable superState) {
            super(superState);
        }

        /**
         * Construct a new instance from a Parcel
         * 
         * @param in the Parcel
         */
        private SavedState(@NonNull Parcel in) {
            super(in);
            mSplitterPositionPercent = in.readFloat();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeFloat(mSplitterPositionPercent);
        }
    }
}