/*
 *  Android Split Pane Layout.
 *  https://github.com/MobiDevelop/android-split-pane-layout
 *  
 *  Copyright (C) 2012 Justin Shapcott
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package de.blau.android.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewGroup;
import de.blau.android.R;


/**
 * A layout that splits the available space between two child views.
 * <p/>
 * An optionally movable bar exists between the children which allows the user
 * to redistribute the space allocated to each view.
 */
public class SplitPaneLayout extends ViewGroup {
    public static final int ORIENTATION_HORIZONTAL = 0;
    public static final int ORIENTATION_VERTICAL = 1;

    private int mOrientation = 0;
    private int mSplitterSize = 12;
    private boolean mSplitterMovable = true;
    private int mSplitterPosition = Integer.MIN_VALUE;
    private float mSplitterPositionPercent = 0.5f;

    private Drawable mSplitterDrawable;
    private Drawable mSplitterDraggingDrawable;

    private Rect mSplitterRect = new Rect();

    private int lastX;
    private int lastY;
    private Rect temp = new Rect();
    private boolean isDragging = false;

    public SplitPaneLayout(Context context) {
        super(context);
        mSplitterPositionPercent = 0.5f;
        mSplitterDrawable = new PaintDrawable(0x88FFFFFF);
        mSplitterDraggingDrawable = new PaintDrawable(0x88FFFFFF);
    }

    public SplitPaneLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        extractAttributes(context, attrs);
    }

    public SplitPaneLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        extractAttributes(context, attrs);
    }

    private void extractAttributes(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SplitPaneLayout);
            mOrientation = a.getInt(R.styleable.SplitPaneLayout_orientation, 0);
            mSplitterSize = a.getDimensionPixelSize(R.styleable.SplitPaneLayout_splitterSize, context.getResources().getDimensionPixelSize(R.dimen.spl_default_splitter_size));
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
                if (value.type == TypedValue.TYPE_REFERENCE ||
                        value.type == TypedValue.TYPE_STRING) {
                    mSplitterDrawable = a.getDrawable(R.styleable.SplitPaneLayout_splitterBackground);
                } else if (value.type == TypedValue.TYPE_INT_COLOR_ARGB8 ||
                        value.type == TypedValue.TYPE_INT_COLOR_ARGB4 ||
                        value.type == TypedValue.TYPE_INT_COLOR_RGB8 ||
                        value.type == TypedValue.TYPE_INT_COLOR_RGB4) {
                    mSplitterDrawable = new PaintDrawable(a.getColor(R.styleable.SplitPaneLayout_splitterBackground, 0xFF000000));
                }
            }
            value = a.peekValue(R.styleable.SplitPaneLayout_splitterDraggingBackground);
            if (value != null) {
                if (value.type == TypedValue.TYPE_REFERENCE ||
                        value.type == TypedValue.TYPE_STRING) {
                    mSplitterDraggingDrawable = a.getDrawable(R.styleable.SplitPaneLayout_splitterDraggingBackground);
                } else if (value.type == TypedValue.TYPE_INT_COLOR_ARGB8 ||
                        value.type == TypedValue.TYPE_INT_COLOR_ARGB4 ||
                        value.type == TypedValue.TYPE_INT_COLOR_RGB8 ||
                        value.type == TypedValue.TYPE_INT_COLOR_RGB4) {
                    mSplitterDraggingDrawable = new PaintDrawable(a.getColor(R.styleable.SplitPaneLayout_splitterDraggingBackground, 0x88FFFFFF));
                }
            } else {
                mSplitterDraggingDrawable = new PaintDrawable(0x88FFFFFF);
            }
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        check();

        if (widthSize > 0 && heightSize > 0) {
            switch (mOrientation) {
                case 0: {
                    if (mSplitterPosition == Integer.MIN_VALUE && mSplitterPositionPercent < 0) {
                        mSplitterPosition = widthSize / 2;
                    } else if (mSplitterPosition == Integer.MIN_VALUE && mSplitterPositionPercent >= 0) {
                        mSplitterPosition = (int) (widthSize * mSplitterPositionPercent);
                    } else if (mSplitterPosition != Integer.MIN_VALUE && mSplitterPositionPercent < 0) {
                        mSplitterPositionPercent = (float) mSplitterPosition / (float) widthSize;
                    }
                    getChildAt(0).measure(MeasureSpec.makeMeasureSpec(mSplitterPosition - (mSplitterSize / 2), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
                    getChildAt(1).measure(MeasureSpec.makeMeasureSpec(widthSize - (mSplitterSize / 2) - mSplitterPosition, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
                    break;
                }
                case 1: {
                    if (mSplitterPosition == Integer.MIN_VALUE && mSplitterPositionPercent < 0) {
                        mSplitterPosition = heightSize / 2;
                    } else if (mSplitterPosition == Integer.MIN_VALUE && mSplitterPositionPercent >= 0) {
                        mSplitterPosition = (int) (heightSize * mSplitterPositionPercent);
                    } else if (mSplitterPosition != Integer.MIN_VALUE && mSplitterPositionPercent < 0) {
                        mSplitterPositionPercent = (float) mSplitterPosition / (float) heightSize;
                    }
                    getChildAt(0).measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mSplitterPosition - (mSplitterSize / 2), MeasureSpec.EXACTLY));
                    getChildAt(1).measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize - (mSplitterSize / 2) - mSplitterPosition, MeasureSpec.EXACTLY));
                    break;
                }
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        int h = b - t;
        switch (mOrientation) {
            case 0: {
                getChildAt(0).layout(0, 0, mSplitterPosition - (mSplitterSize / 2), h);
                mSplitterRect.set(mSplitterPosition - (mSplitterSize / 2), 0, mSplitterPosition + (mSplitterSize / 2), h);
                getChildAt(1).layout(mSplitterPosition + (mSplitterSize / 2), 0, r, h);
                break;
            }
            case 1: {
                getChildAt(0).layout(0, 0, w, mSplitterPosition - (mSplitterSize / 2));
                mSplitterRect.set(0, mSplitterPosition - (mSplitterSize / 2), w, mSplitterPosition + (mSplitterSize / 2));
                getChildAt(1).layout(0, mSplitterPosition + (mSplitterSize / 2), w, h);
                break;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mSplitterMovable) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    if (mSplitterRect.contains(x, y)) {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        isDragging = true;
                        temp.set(mSplitterRect);
                        invalidate(temp);
                        lastX = x;
                        lastY = y;
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (isDragging) {
                        switch (mOrientation) {
                            case ORIENTATION_HORIZONTAL: {
                                temp.offset((x - lastX), 0);
                                break;
                            }
                            case ORIENTATION_VERTICAL: {
                                temp.offset(0, (int) (y - lastY));
                                break;
                            }
                        }
                        lastX = x;
                        lastY = y;
                        invalidate();
                    }
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    if (isDragging) {
                        isDragging = false;
                        switch (mOrientation) {
                            case ORIENTATION_HORIZONTAL: {
                                mSplitterPosition = x;
                                break;
                            }
                            case ORIENTATION_VERTICAL: {
                                mSplitterPosition = y;
                                break;
                            }
                        }
                        mSplitterPositionPercent = -1;
                        remeasure();
                        requestLayout();
                    }
                    break;
                }
            }
            return true;
        }
        return false;
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
        measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY)
        );
    }

    /**
     * Checks that we have exactly two children.
     */
    private void check() {
        if (getChildCount() != 2) {
            throw new RuntimeException("SplitPaneLayout must have exactly two child views.");
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mSplitterDrawable != null) {
            mSplitterDrawable.setBounds(mSplitterRect);
            mSplitterDrawable.draw(canvas);
        }
        if (isDragging) {
            mSplitterDraggingDrawable.setBounds(temp);
            mSplitterDraggingDrawable.draw(canvas);
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
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        float mSplitterPositionPercent;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
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