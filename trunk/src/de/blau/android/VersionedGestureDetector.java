package de.blau.android;

import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * From: http://android-developers.blogspot.com/2010/07/how-to-have-your-cupcake-and-eat-it-too.html
 * @author Adam Powell, modified by Andrew Gregory for Vespucci
 */
public abstract class VersionedGestureDetector {
	OnGestureListener mListener;
	
	public abstract boolean onTouchEvent(View v, MotionEvent ev);
	
	public interface OnGestureListener {
		public void onClick(View v, float x, float y);
		public void onDrag(View v, float x, float y, float dx, float dy);
		public void onScale(View v, float scaleFactor, float prevSpan, float curSpan);
	}
	
	public static VersionedGestureDetector newInstance(Context context, OnGestureListener listener) {
		final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
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
	
	private static class CupcakeDetector extends VersionedGestureDetector {
		float mFirstTouchX;
		float mFirstTouchY;
		float mLastTouchX;
		float mLastTouchY;
		boolean hasDragged;
		private static final float dragThreshold = 20f;
		
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
				hasDragged = false;
				break;
			case MotionEvent.ACTION_MOVE:
				{
					final float x = getActiveX(ev);
					final float y = getActiveY(ev);
					if (shouldDrag()) {
						if (Math.abs(x - mFirstTouchX) > dragThreshold || Math.abs(y - mFirstTouchY) > dragThreshold) {
							hasDragged = true;
						}
						if (hasDragged) {
							mListener.onDrag(v, x, y, x - mLastTouchX, y - mLastTouchY);
						}
					}
					mLastTouchX = x;
					mLastTouchY = y;
				}
				break;
			case MotionEvent.ACTION_UP:
				if (!hasDragged) {
					mListener.onClick(v, getActiveX(ev), getActiveY(ev));
				}
				break;
			}
			return true;
		}
	}
	
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
			if ((action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP)
			{
				mActivePointerId = INVALID_POINTER_ID;
				mActivePointerIndex = ev.findPointerIndex(mActivePointerId);
			}
			return ret;
		}
	}
	
	private static class FroyoDetector extends EclairDetector {
		private ScaleGestureDetector mDetector;
		private View v;
		
		public FroyoDetector(Context context) {
			mDetector = new ScaleGestureDetector(context,
					new ScaleGestureDetector.SimpleOnScaleGestureListener() {
				@Override public boolean onScale(ScaleGestureDetector detector) {
					mListener.onScale(v, detector.getScaleFactor(), detector.getPreviousSpan(), detector.getCurrentSpan());
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
			mDetector.onTouchEvent(ev);
			return super.onTouchEvent(v, ev);
		}
	}
	
}
