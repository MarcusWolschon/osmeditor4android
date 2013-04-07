package de.blau.android;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * From: http://android-developers.blogspot.com/2010/07/how-to-have-your-cupcake-and-eat-it-too.html
 * @author Adam Powell, modified by Andrew Gregory for Vespucci
 * @author Jan Schejbal added long-click detection
 */
public abstract class VersionedGestureDetector {
	private static final float DRAG_THRESHOLD = 20f;
	public static final long LONG_PRESS_DELAY = 500;
	OnGestureListener mListener;
	
	public abstract boolean onTouchEvent(View v, MotionEvent ev);
	
	public interface OnGestureListener {
		public void onDown(View v, float x, float y);
		public void onClick(View v, float x, float y);
		public void onUp(View v, float x, float y);
		/** @return true if long click events are handled, false if they should be ignored */
		public boolean onLongClick(View v, float x, float y);
		public void onDrag(View v, float x, float y, float dx, float dy);
		public void onScale(View v, float scaleFactor, float prevSpan, float curSpan);
	}
	
	public static VersionedGestureDetector newInstance(Context context, OnGestureListener listener) {
		final int sdkVersion = Build.VERSION.SDK_INT;
		VersionedGestureDetector detector = null;
		if (sdkVersion < Build.VERSION_CODES.ECLAIR) {
			detector = new CupcakeDetector();
		} else if (sdkVersion < Build.VERSION_CODES.FROYO) {
			detector = new EclairDetector();
		} else {
			detector = new FroyoDetector(context);
		}
		
		detector.mListener = listener;
		
		return detector;
	}
	
	@TargetApi(3)
	private static class CupcakeDetector extends VersionedGestureDetector {
		float mFirstTouchX;
		float mFirstTouchY;
		float mLastTouchX;
		float mLastTouchY;
		boolean hasDragged;
		boolean hasScaled;
		/** true if a long press has occurred since the last ACTION_DOWN, i.e. the ACTION_UP may need to be ignored */
		boolean hasLongPressed;
		LongPressTrigger longPressTrigger;
		
		float getActiveX(MotionEvent ev) {
			return ev.getX();
		}
		
		float getActiveY(MotionEvent ev) {
			return ev.getY();
		}
		
		boolean shouldDrag() {
			return true;
		}
		
		@Override
		public boolean onTouchEvent(View v, MotionEvent ev) {
			switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mFirstTouchX = mLastTouchX = getActiveX(ev);
				mFirstTouchY = mLastTouchY = getActiveY(ev);
				mListener.onDown(v, mFirstTouchX, mFirstTouchY);
				hasDragged = false;
				hasScaled = false;
				hasLongPressed = false;
				startLongPress(v, ev);
				break;
			case MotionEvent.ACTION_MOVE:
				{
					if (hasLongPressed) {
						break;
					}
					final float x = getActiveX(ev);
					final float y = getActiveY(ev);
					if (shouldDrag()) {
						if (Math.abs(x - mFirstTouchX) > DRAG_THRESHOLD || Math.abs(y - mFirstTouchY) > DRAG_THRESHOLD) {
							hasDragged = true;
						}
						if (hasDragged) {
							mListener.onDrag(v, x, y, x - mLastTouchX, y - mLastTouchY);
						}
					}
					mLastTouchX = x;
					mLastTouchY = y;
					
					if (hasDragged || hasScaled) {
						stopLongPress();
					} else {
						// Update long press position
						if (longPressTrigger != null) {
							longPressTrigger.x = x;
							longPressTrigger.y = y;
						}
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				{
					final float x = getActiveX(ev);
					final float y = getActiveY(ev);
					mListener.onUp(v, x, y);
					if (hasLongPressed) {
						break;
					}
					stopLongPress();
					if (!hasDragged && !hasScaled) {
						mListener.onClick(v, x, y);
					}
				}
				break;
			}
			return true;
		}
		
		/**
		 * Called when ACTION_DOWN is received.
		 * Schedules a long-click check for execution.
		 * 
		 * @param v the view from which the ACTION_DOWN onTouch event came 
		 * @param ev the onTouch event starting the (possible) long click
		 */
		private void startLongPress(View v, MotionEvent ev) {
			stopLongPress();
			longPressTrigger = new LongPressTrigger(v, getActiveX(ev), getActiveY(ev));
			Handler h = new Handler();
			h.postDelayed(longPressTrigger, LONG_PRESS_DELAY);
		}
		
		/**
		 * If a long-click check is scheduled, marks it as canceled.
		 * Call this when the click turned out not to be a long-click
		 * (e.g. finger released, or drag/scale detected) 
		 */
		private void stopLongPress() {
			if (longPressTrigger != null) {
				longPressTrigger.cancel();
				longPressTrigger = null;
			}
		}
		
		/**
		 * Runnable checking for a long-press.
		 * Will be scheduled for execution by startLongPress.
		 * 
		 * If not cancelled, will notify the listener of the long-press event
		 * and set hasLongPressed. 
		 *
		 */
		private class LongPressTrigger implements Runnable {
			private View v;
			private float x;
			private float y;
			private boolean canceled = false;
			
			LongPressTrigger(View v, float x, float y) {
				this.v = v;
				this.x = x;
				this.y = y;
			}

			@Override
			public void run() {
				if (canceled) return;
				if (mListener.onLongClick(v, x, y)) {
					hasLongPressed = true;
				}
			}
			
			/**
			 * If this press turned out to be anything else than a long-press,
			 * call this to disable this LongPressTrigger
			 */
			public void cancel() {
				canceled = true;
			}
		}
		
		
	}
	
	@TargetApi(5)
	private static class EclairDetector extends CupcakeDetector {
		private static final int INVALID_POINTER_ID = -1;
		private int mActivePointerId = INVALID_POINTER_ID;
		private int mActivePointerIndex = 0;
		
		@Override
		float getActiveX(MotionEvent ev) {
			return ev.getX(mActivePointerIndex);
		}
		
		@Override
		float getActiveY(MotionEvent ev) {
			return ev.getY(mActivePointerIndex);
		}
		
		@Override
		public boolean onTouchEvent(View v, MotionEvent ev) {
			final int action = ev.getAction();
			switch (action & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_UP:
				mActivePointerId = ev.getPointerId(0);
				break;
			case MotionEvent.ACTION_CANCEL:
				mActivePointerId = INVALID_POINTER_ID;
				break;
			case MotionEvent.ACTION_POINTER_UP:
				final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
				final int pointerId = ev.getPointerId(pointerIndex);
				if (pointerId == mActivePointerId) {
					// This was our active pointer going up. Choose a new
					// active pointer and adjust accordingly.
					final int newPointerIndex = (pointerIndex == 0) ? 1 : 0;
					mActivePointerId = ev.getPointerId(newPointerIndex);
					mLastTouchX = ev.getX(newPointerIndex);
					mLastTouchY = ev.getY(newPointerIndex);
				}
				break;
			}
			
			mActivePointerIndex = ev.findPointerIndex(mActivePointerId);
			boolean ret = super.onTouchEvent(v, ev);
			if ((action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
				mActivePointerId = INVALID_POINTER_ID;
				mActivePointerIndex = ev.findPointerIndex(mActivePointerId);
			}
			return ret;
		}
	}
	
	@TargetApi(8)
	private static class FroyoDetector extends EclairDetector {
		private ScaleGestureDetector mDetector;
		private View v;
		
		public FroyoDetector(Context context) {
			mDetector = new ScaleGestureDetector(context,
					new ScaleGestureDetector.SimpleOnScaleGestureListener() {
				@Override public boolean onScale(ScaleGestureDetector detector) {
					mListener.onScale(v, detector.getScaleFactor(), detector.getPreviousSpan(), detector.getCurrentSpan());
					hasScaled = true;
					return true;
				}
			});
		}
		
		@Override
		boolean shouldDrag() {
			return !mDetector.isInProgress();
		}
		
		@Override
		public boolean onTouchEvent(View v, MotionEvent ev) {
			this.v = v;
			if (!hasLongPressed) {
				mDetector.onTouchEvent(ev);
			}
			return super.onTouchEvent(v, ev);
		}
	}
	
}
