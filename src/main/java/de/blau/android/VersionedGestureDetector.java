package de.blau.android;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * From: http://android-developers.blogspot.com/2010/07/how-to-have-your-cupcake-and-eat-it-too.html
 * 
 * @author Adam Powell, modified by Andrew Gregory for Vespucci
 * @author Jan Schejbal added long-click detection
 * @author Simon Poole removed code for very old (pre-API 8) versions, double tap support
 */
public abstract class VersionedGestureDetector {

    OnGestureListener mListener;

    public abstract boolean onTouchEvent(View v, MotionEvent ev);

    public interface OnGestureListener {
        void onDown(View v, float x, float y);

        void onClick(View v, float x, float y);

        void onUp(View v, float x, float y);

        /** @return true if long click events are handled, false if they should be ignored */
        boolean onLongClick(View v, float x, float y);

        void onDrag(View v, float x, float y, float dx, float dy);

        void onScale(View v, float scaleFactor, float prevSpan, float curSpan, float focusX, float focusY);

        boolean onDoubleTap(View v, float x, float y);
    }

    public static VersionedGestureDetector newInstance(Context context, OnGestureListener listener) {
        VersionedGestureDetector detector = null;
        detector = new FroyoDetector(context);

        detector.mListener = listener;

        return detector;
    }

    private static class FroyoDetector extends VersionedGestureDetector {
        private ScaleGestureDetector mScaleDetector;
        private GestureDetector      mGestureDetector;
        private View                 v;

        public FroyoDetector(Context context) {
            mScaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    mListener.onScale(v, detector.getScaleFactor(), detector.getPreviousSpan(), detector.getCurrentSpan(), detector.getFocusX(), detector.getFocusY());
                    return true;
                }
            });

            mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    float x = e.getX();
                    float y = e.getY();

                    mListener.onDoubleTap(v, x, y);
                    return true;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    float x = e.getX();
                    float y = e.getY();

                    mListener.onClick(v, x, y);
                    return true;
                }

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    float x = e2.getX();
                    float y = e2.getY();

                    mListener.onDrag(v, x, y, -distanceX, -distanceY);
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    float x = e.getX();
                    float y = e.getY();

                    mListener.onLongClick(v, x, y);
                }

                @Override
                public boolean onDown(MotionEvent e) {
                    float x = e.getX();
                    float y = e.getY();

                    mListener.onDown(v, x, y);
                    return true;
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    float x = e.getX();
                    float y = e.getY();

                    mListener.onUp(v, x, y);
                    return true;
                }
            });
        }

        @Override
        /**
         * This used to call through to the non-froyo versions. Replaced by calls to the respective android methods.
         */
        public boolean onTouchEvent(View v, MotionEvent ev) {
            this.v = v;

            mScaleDetector.onTouchEvent(ev);
            mGestureDetector.onTouchEvent(ev);

            return true;
        }
    }
}
