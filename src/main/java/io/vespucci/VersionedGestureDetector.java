package io.vespucci;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import androidx.annotation.NonNull;

/**
 * From: http://android-developers.blogspot.com/2010/07/how-to-have-your-cupcake-and-eat-it-too.html
 * 
 * All Android versions now support the functionality this provided so it is actually only used as a backwards
 * compatibility shim for our own code
 * 
 * @author Adam Powell, modified by Andrew Gregory for Vespucci
 * @author Jan Schejbal added long-click detection
 * @author Simon Poole removed code for very old (pre-API 8) versions, double tap support
 */
public abstract class VersionedGestureDetector {

    OnGestureListener mListener;

    /**
     * Called for touch events
     * 
     * @param v the View
     * @param ev the MotionEvent
     * @return true if it was consumed
     */
    public abstract boolean onTouchEvent(@NonNull View v, @NonNull MotionEvent ev);

    public interface OnGestureListener {

        /**
         * Start of touch
         * 
         * @param v the View
         * @param x screen x coordinate
         * @param y screen y coordinate
         */
        void onDown(@NonNull View v, float x, float y);

        /**
         * A click
         * 
         * @param v the View
         * @param x screen x coordinate
         * @param y screen y coordinate
         */
        void onClick(@NonNull View v, float x, float y);

        /**
         * End of touch
         * 
         * @param v the View
         * @param x screen x coordinate
         * @param y screen y coordinate
         */
        void onUp(@NonNull View v, float x, float y);

        /**
         * A long click
         * 
         * @param v the View
         * @param x screen x coordinate
         * @param y screen y coordinate
         * @return true if long click events are handled, false if they should be ignored
         */
        boolean onLongClick(@NonNull View v, float x, float y);

        /**
         * A drag
         * 
         * @param v the View
         * @param x screen x coordinate
         * @param y screen y coordinate
         * @param dx delta x in screen coordinates
         * @param dy delta y in screen coordinates
         */
        void onDrag(@NonNull View v, float x, float y, float dx, float dy);

        /**
         * Scale gesture
         * 
         * @param v the View
         * @param scaleFactor the current scaling factor
         * @param prevSpan previous span
         * @param curSpan current span
         * @param focusX center of pinch x in screen coordinates
         * @param focusY center of pinch y in screen coordinates
         */
        void onScale(@NonNull View v, float scaleFactor, float prevSpan, float curSpan, float focusX, float focusY);

        /**
         * 
         * @param v the View
         * @param x screen x coordinate
         * @param y screen y coordinate
         */
        void onDoubleTap(@NonNull View v, float x, float y);
    }

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     * @param listener an OnGestureListener
     * @return a new VersionedGestureDetector instance
     */
    public static VersionedGestureDetector newInstance(@NonNull Context context, @NonNull OnGestureListener listener) {
        VersionedGestureDetector detector = null;
        detector = new FroyoDetector(context);

        detector.mListener = listener;

        return detector;
    }

    private static class FroyoDetector extends VersionedGestureDetector {
        private ScaleGestureDetector mScaleDetector;
        private GestureDetector      mGestureDetector;
        private View                 v;

        /**
         * Construct a Froyo (android 2.2) and later specific instance
         * 
         * @param context an Android Context
         */
        public FroyoDetector(@NonNull Context context) {
            mScaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    mListener.onScale(v, detector.getScaleFactor(), detector.getPreviousSpan(), detector.getCurrentSpan(), detector.getFocusX(),
                            detector.getFocusY());
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
