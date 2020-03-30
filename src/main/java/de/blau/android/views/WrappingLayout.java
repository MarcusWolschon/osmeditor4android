package de.blau.android.views;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This is an ugly hack providing something like a flow layout. The children are arranged in rows of horizontal linear
 * layouts, which are in turn arranged in a vertical linear layout (this class).
 * 
 * The actual children can be read and set using {@link #getWrappedChildren()} and
 * {@link #setWrappedChildren(ArrayList)}. Accessing this view's children directly, e.g. via {@link #getChildAt(int)},
 * will yield the horizontal linear layouts that may be replaced at any time.
 * 
 * This layout should be usable via inflation from XML - the original children are loaded into this class, wrapped in
 * linear layouts and then re-inserted. However, advanced attributes need to be set in the code.
 * 
 * @author Jan Schejbal
 *
 */
public class WrappingLayout extends LinearLayout {

    private boolean needsRelayout      = false;
    private boolean relayoutInProgress = false;
    private boolean isWrapped          = false;

    private final LayoutWrapper wrapper;

    private List<View> children;

    /**
     * Construct a new WrappingLayout
     * 
     * @param context an Android Context
     */
    public WrappingLayout(@NonNull Context context) {
        super(context);
        wrapper = new LayoutWrapper(context);
    }

    /**
     * Construct a new WrappingLayout
     * 
     * @param context an Android Context
     * @param attrs an AtrributeSet
     */
    public WrappingLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        wrapper = new LayoutWrapper(context);
    }

    /**
     * Construct a new WrappingLayout
     * 
     * @param context an Android Context
     * @param attrs an AtrributeSet
     * @param defStyle a resource id for a default style
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public WrappingLayout(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        wrapper = new LayoutWrapper(context);
    }

    /**
     * @return he list of child views that are being line-wrapped
     */
    public List<View> getWrappedChildren() {
        eatChildrenIfNecessary();
        return children;
    }

    /**
     * Sets the list of child views that should be line-wrapped
     * 
     * @param childViews the children to line-wrap
     */
    public void setWrappedChildren(List<View> childViews) {
        this.children = childViews;
        requestLayout();
    }

    /**
     * (Re)does the line-breaking. Use e.g. if you change the size of child elements.
     */
    private void triggerRelayout() {
        needsRelayout = true;
        requestLayout();
    }

    /**
     * Sets the row gravity, i.e. the alignment of items inside rows
     * 
     * @param gravity the {@link Gravity} to set
     * @return LayoutWrapper object (for chaining)
     */
    public LayoutWrapper setRowGravity(int gravity) {
        return wrapper.setRowGravity(gravity);
    }

    /**
     * Sets whether children will be added from left to right (default, false) or from right to the left (true). Most
     * useful with {@link #setRowGravity(int)} set to {@link Gravity#RIGHT}.
     * 
     * @param rightToLeft if true, layout right to left
     * @return LayoutWrapper object (for chaining)
     */
    public LayoutWrapper setRightToLeft(boolean rightToLeft) {
        return wrapper.setRightToLeft(rightToLeft);
    }

    /**
     * Sets the vertical spacing in pixels between rows of elements
     * 
     * @param pixel spacing
     * @return LayoutWrapper object (for chaining)
     */
    public LayoutWrapper setVerticalSpacing(int pixel) {
        return wrapper.setVerticalSpacing(pixel);
    }

    /**
     * Sets the horizontal spacing in pixels between elements
     * 
     * @param pixel spacing
     * @return LayoutWrapper object (for chaining)
     */
    public LayoutWrapper setHorizontalSpacing(int pixel) {
        return wrapper.setHorizontalSpacing(pixel);
    }

    /**
     * After inflating from XML, calls {@link #eatChildren()} to properly line-wrap children
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        eatChildren();
    }

    /**
     * Removes children that belong directly to the WrappingLayout, re-adding them as wrapped children. This allows to
     * set the WrappingLayout contents from XML
     */
    private void eatChildren() {
        ArrayList<View> tmpChildren = new ArrayList<>();

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            tmpChildren.add(getChildAt(i));
        }

        setWrappedChildren(tmpChildren);
    }

    /**
     * Calls eatChildren if required
     */
    private void eatChildrenIfNecessary() {
        if (children == null) {
            eatChildren();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw) {
            triggerRelayout();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (needsRelayout) {
            performRelayout();
        } else {
            if (!relayoutInProgress) {
                super.onLayout(changed, l, t, r, b);
            }
        }
    }

    /**
     * (Re)does the line wrapping if necessary
     */
    private void performRelayout() {
        if (!needsRelayout) {
            return;
        }

        needsRelayout = false;
        relayoutInProgress = true;

        if (isWrapped) {
            wrapper.unwrap(this);
        }
        removeAllViews();
        wrapper.wrap(children, this);
        isWrapped = true;

        relayoutInProgress = false;
        post(new Runnable() {
            @Override
            public void run() {
                requestLayout();
                invalidate();
            }
        });
    }

    /**
     * Helper class performing the actual line-wrapping of elements into a LinearLayout
     * 
     * @author Jan
     */
    public static class LayoutWrapper {

        private static final String DEBUG_TAG = "WrappingLayout";

        private static final String LOGTAG = LayoutWrapper.class.getSimpleName();

        private Context context;

        private int     rowGravity;
        private boolean rightToLeft;

        private int hspace = 0;
        private int vspace = 0;

        private boolean widthAdjustmentDone = false;
        private int     newWidth            = 0;

        private static final int MEASURE_SPEC_UNSPECIFIED = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        /**
         * Constrzct a new LayoutWrapper
         * 
         * @param context an Android Context
         */
        public LayoutWrapper(@NonNull Context context) {
            this.context = context;
        }

        /**
         * Set the gravity
         * 
         * @param gravity the Gravity value to set
         * @return this instance for chaining
         */
        public LayoutWrapper setRowGravity(int gravity) {
            rowGravity = gravity;
            return this;
        }

        /**
         * Set the layout direction
         * 
         * @param rightToLeft if true the layout will be rtl
         * @return this instance for chaining
         */
        public LayoutWrapper setRightToLeft(boolean rightToLeft) {
            this.rightToLeft = rightToLeft;
            return this;
        }

        /**
         * Set the vertical spacing
         * 
         * @param pixel vertical spacing in pixel
         * @return this instance for chaining
         */
        public LayoutWrapper setVerticalSpacing(int pixel) {
            vspace = pixel;
            return this;
        }

        /**
         * Set the horizontal spacing
         * 
         * @param pixel horizontal spacing in pixel
         * @return this instance for chaining
         */
        public LayoutWrapper setHorizontalSpacing(int pixel) {
            hspace = pixel;
            return this;
        }

        /**
         * Line-Wraps the children into the container. The container must have a width assigned, i.e. the container
         * layout should be finished
         * 
         * @param children the views to layout
         * @param container the LinearLayout that should contain the children
         */
        public void wrap(List<View> children, LinearLayout container) {
            container.setOrientation(LinearLayout.VERTICAL);

            if (children == null) {
                Log.e(DEBUG_TAG, "wrap: childern null");
                return;
            }

            LayoutParams innerLayoutParams = new LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

            final int availableSpace = container.getWidth() - container.getPaddingLeft() - container.getPaddingRight();
            int usedSpace = 0;

            if (!children.isEmpty() && !widthAdjustmentDone) {
                int childWidth = getViewWidth(children.get(0));
                int times = (int) Math.max(1, (availableSpace - hspace) / (float) (childWidth + hspace));
                newWidth = (availableSpace - ((times + 1) * hspace)) / times;
                widthAdjustmentDone = true;
            }

            LinearLayout inner = new LinearLayout(context);
            inner.setGravity(rowGravity);
            inner.setOrientation(LinearLayout.HORIZONTAL);
            // not only For new rows, set margin
            innerLayoutParams.topMargin = vspace;
            inner.setSaveEnabled(container.isSaveEnabled());
            container.addView(inner, new LayoutParams((android.view.ViewGroup.MarginLayoutParams) innerLayoutParams));

            if (availableSpace == 0) {
                Log.e(LOGTAG, "No width information - read documentation!");
            }

            for (View child : children) {
                int childWidth = getViewWidth(child);
                if (newWidth > childWidth && child instanceof TextView) { // TODO this will fail with non square
                                                                          // children views
                    android.view.ViewGroup.LayoutParams childLayout = ((TextView) child).getLayoutParams();
                    if (childLayout != null) {
                        childLayout.width = newWidth;
                        childLayout.height = newWidth;
                        ((TextView) child).setLayoutParams(childLayout);
                    } else {
                        Log.e(DEBUG_TAG, "child view layout params null");
                    }
                }
                childWidth = getViewWidth(child);

                if ((usedSpace + hspace + childWidth) > availableSpace) {
                    // did not fit, create new row
                    inner = new LinearLayout(context);
                    inner.setOrientation(LinearLayout.HORIZONTAL);
                    inner.setGravity(rowGravity);
                    inner.setSaveEnabled(container.isSaveEnabled());
                    container.addView(inner, new LayoutParams((android.view.ViewGroup.MarginLayoutParams) innerLayoutParams));
                    usedSpace = 0;
                }
                // adding to current row
                // add horizontal spacing if necessary
                if (hspace > 0) {
                    SpacerView spacer = new SpacerView(context, hspace, 0);
                    spacer.setSaveEnabled(container.isSaveEnabled());
                    if (rightToLeft) {
                        inner.addView(spacer, 0);
                    } else {
                        inner.addView(spacer);
                    }
                    usedSpace += hspace;
                }

                // add to whatever is the current row now
                if (rightToLeft) {
                    inner.addView(child, 0);
                } else {
                    inner.addView(child);
                }
                usedSpace += childWidth;
            }
        }

        /**
         * Fully unwraps the given layout.
         * 
         * The children are cleanly removed from the row layouts so that they can be re-attached elsewhere. Then, the
         * row layouts themselves are removed from the linear layout.
         * 
         * @param wrappedLayout a {@link LinearLayout} wrapped with {@link #wrap(List, LinearLayout)}
         */
        public void unwrap(LinearLayout wrappedLayout) {
            int count = wrappedLayout.getChildCount();
            for (int i = 0; i < count; i++) {
                LinearLayout row = (LinearLayout) wrappedLayout.getChildAt(i);
                row.removeAllViews();
            }
            wrappedLayout.removeAllViews();
        }

        /**
         * Measures a view to get its width
         * 
         * @param view the view to measure
         * @return the width including margins
         */
        private int getViewWidth(View view) {
            if (view.getLayoutParams() == null) {
                // protect against https://issuetracker.google.com/issues/37003658
                Log.e(DEBUG_TAG, "Don't know what to do with " + view.getClass().getName());
                return 0;
            }
            view.measure(MEASURE_SPEC_UNSPECIFIED, MEASURE_SPEC_UNSPECIFIED);
            int width = view.getMeasuredWidth(); // includes padding, does not include margins
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) view.getLayoutParams();
            if (params != null) {
                width += params.leftMargin + params.rightMargin;
            }
            return width;
        }

        private static final class SpacerView extends View {

            /**
             * Construct an empty View that can be used as a spacer
             * 
             * @param ctx an Android Context
             * @param width width of the SpacerView
             * @param height height of the SpacerView
             */
            private SpacerView(@NonNull Context ctx, int width, int height) {
                super(ctx);
                setLayoutParams(new LayoutParams(width, height));
            }
        }
    }
}
