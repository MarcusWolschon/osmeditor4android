package de.blau.android.views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

/**
 * This is an ugly hack providing something like a flow layout.
 * The children are arranged in rows of horizontal linear layouts,
 * which are in turn arranged in a vertical linear layout (this class).
 * 
 * The actual children can be read and set using {@link #getWrappedChildren()}
 * and {@link #setWrappedChildren(ArrayList)}.
 * Accessing this view's children directly, e.g. via {@link #getChildAt(int)},
 * will yield the horizontal linear layouts that may be replaced at any time.
 * 
 * This layout should be usable via inflation from XML - the original children
 * are loaded into this class, wrapped in liear layouts and then re-inserted.
 * However, advanced attributes need to be set in the code.
 * 
 * @author Jan Schejbal
 *
 */
public class WrappingLayout extends LinearLayout {
	
	private boolean needsRelayout = false;
	private boolean relayoutInProgress = false;
	private boolean isWrapped = false;
	
	private final LayoutWrapper wrapper;
	
	private ArrayList<View> children;
	
	public WrappingLayout(Context context) {
		super(context);
		wrapper = new LayoutWrapper(context);
	}

	public WrappingLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		wrapper = new LayoutWrapper(context);
	}
	
	/**
	 * @return he list of child views that are being line-wrapped 
	 */
	public ArrayList<View> getWrappedChildren() {
		eatChildrenIfNecessary();
		return children;
	}

	/**
	 * Sets the list of child views that should be line-wrapped 
	 * @param children the children to line-wrap
	 */
	public void setWrappedChildren(ArrayList<View> children) {
		this.children = children;
		requestLayout();
	}

	/**
	 * (Re)does the line-breaking. Use e.g. if you change the size of child elements.
	 */
	public void triggerRelayout() {
		needsRelayout = true;
		requestLayout();
	}
	
	/**
	 * Sets the row gravity, i.e. the alignment of items inside rows
	 * @param gravity the {@link Gravity} to set
	 * @return LayoutWrapper object (for chaining)
	 */
	public LayoutWrapper setRowGravity(int gravity) {
		return wrapper.setRowGravity(gravity);
	}

	/**
	 * Sets whether children will be added from left to right (default, false) or from right to the left (true).
	 * Most useful with {@link #setRowGravity(int)} set to {@link Gravity#RIGHT}.
	 * @param rightToLeft
	 * @return LayoutWrapper object (for chaining)
	 */
	public LayoutWrapper setRightToLeft(boolean rightToLeft) {
		return wrapper.setRightToLeft(rightToLeft);
	}

	/**
	 * Sets the vertical spacing in pixels between rows of elements
	 * @param pixel spacing 
	 * @return LayoutWrapper object (for chaining)
	 */
	public LayoutWrapper setVerticalSpacing(int pixel) {
		return wrapper.setVerticalSpacing(pixel);
	}

	/**
	 * Sets the horizontal spacing in pixels between elements
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
	 * Removes children that belong directly to the WrappingLayout,
	 * re-adding them as wrapped children.
	 * This allows to set the WrappingLayout contents from XML  
	 */
	private void eatChildren() {
		ArrayList<View> tmpChildren = new ArrayList<View>();
		
		int count = getChildCount();
		for (int i = 0; i < count; i++) {
			tmpChildren.add(getChildAt(i));
		}
				
		setWrappedChildren(tmpChildren);
	}
	
	private void eatChildrenIfNecessary() {
		if (children == null) eatChildren();
	}
	

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (w != oldw) triggerRelayout();
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (needsRelayout) {
			performRelayout();
		} else {
			if (!relayoutInProgress) super.onLayout(changed, l, t, r, b);
		}
	}

	/**
	 * (Re)does the line wrapping if necessary
	 */
	private void performRelayout() {
		if (!needsRelayout) return;
		
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
		    public void run() {
		        requestLayout();
		        invalidate();
		    }
		});

	}	

	
	/**
	 * Helper class performing the actual line-wrapping of elements into a LinearLayout 
	 * @author Jan
	 */
	public static class LayoutWrapper {
		
		private final static String LOGTAG = LayoutWrapper.class.getSimpleName();
		
		private Context context;

		private int rowGravity;
		private boolean rightToLeft;

		private int hspace = 0;
		private int vspace = 0;
		
		private static final int MEASURE_SPEC_UNSPECIFIED = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

		
		public LayoutWrapper(Context context) {
			this.context = context;
		}
		
		public LayoutWrapper setRowGravity(int gravity) {
			rowGravity = gravity;
			return this;
		}
		
		public LayoutWrapper setRightToLeft(boolean rightToLeft) {
			this.rightToLeft = rightToLeft;
			return this;
		}
		
		public LayoutWrapper setVerticalSpacing(int pixel) {
			vspace = pixel;
			return this;
		}
		
		public LayoutWrapper setHorizontalSpacing(int pixel) {
			hspace = pixel;
			return this;
		}
		
		
		/**
		 * Line-Wraps the children into the container. The container must have a width assigned,
		 * i.e. the container layout should be finished
		 * @param children the views to layout
		 * @param container the LinearLayout that should contain the children
		 */
		public void wrap(List<View> children, LinearLayout container) {
			container.setOrientation(LinearLayout.VERTICAL);
			
			LayoutParams innerLayoutParams =
				new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			
			final int availableSpace = container.getWidth() - container.getPaddingLeft() - container.getPaddingRight();
			int usedSpace = 0;
			
			LinearLayout inner = new LinearLayout(context);
			inner.setGravity(rowGravity);
			inner.setOrientation(LinearLayout.HORIZONTAL);
			container.addView(inner, new LayoutParams(innerLayoutParams));
			
			// For new rows, set margin
			innerLayoutParams.bottomMargin = vspace;
			
			
			if (availableSpace == 0) {
				Log.e(LOGTAG, "No width information - read documentation!");
			}
			
			for (View child : children) {
				int childWidth = getViewWidth(child);
				if (inner.getChildCount() > 0) { // if row is empty, no space checking is done
					if ((usedSpace + hspace + childWidth) <= availableSpace) {
						// adding to current row
						
						// add horizontal spacing if necessary
						if (hspace > 0) { 
							if (rightToLeft) {
								inner.addView(new SpacerView(context, hspace, 0), 0);
							} else {
								inner.addView(new SpacerView(context, hspace, 0));
							}
							usedSpace += hspace;
						}
					} else {
						// did not fit, create new row
						inner = new LinearLayout(context);
						inner.setOrientation(LinearLayout.HORIZONTAL);
						inner.setGravity(rowGravity);
						container.addView(inner, new LayoutParams(innerLayoutParams));
						usedSpace = 0;
					}
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
		 * The children are cleanly removed from the row layouts so that they can be re-attached elsewhere.
		 * Then, the row layouts themselves are removed from the linear layout.
		 *  
		 * @param wrappedLayout a {@link LinearLayout} wrapped with {@link #wrap(List, LinearLayout)}
		 */
		public void unwrap(LinearLayout wrappedLayout) {
			int count = wrappedLayout.getChildCount();
			for (int i = 0; i < count; i++) {
				LinearLayout row = (LinearLayout)wrappedLayout.getChildAt(i);
				row.removeAllViews();
			}
			wrappedLayout.removeAllViews();
		}

		/**
		 * Measures a view to get its width
		 * @param view the view to measure
		 * @return the width including margins
		 */
		private int getViewWidth(View view) {
			view.measure(MEASURE_SPEC_UNSPECIFIED, MEASURE_SPEC_UNSPECIFIED);
			int width = view.getMeasuredWidth(); // includes padding, does not include margins
			LayoutParams params = (LayoutParams)(view.getLayoutParams());
			if (params != null) {
				width += params.leftMargin + params.rightMargin;
			}
			return width;
		}

		private static class SpacerView extends View {
				private SpacerView(Context ctx, int width, int height) {
					super(ctx);
					setLayoutParams(new LayoutParams(width, height));
				}
			}	
		}

	}
