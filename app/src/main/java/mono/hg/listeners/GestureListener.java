package mono.hg.listeners;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import mono.hg.utils.Utils.Gesture;

/**
 * Detects touch events across a view.
 * Based on GestureListener originally written by Edward Brey at StackOverflow,
 * (https://stackoverflow.com/a/19506010)
 * <p>
 * Modified to add swipe up, swipe down, single tap, and long press events.
 */
public class GestureListener implements View.OnTouchListener {
    private final GestureDetector gestureDetector;
    private final ScaleGestureDetector scaleDetector;

    protected GestureListener(Context context) {
        gestureDetector = new GestureDetector(context, new InternalGestureListener());
        scaleDetector = new ScaleGestureDetector(context, new InternalScaleGestureListener());
    }

    public void onLongPress() {
        // Do long press action.
    }

    public void onGesture(int direction) {
        // Do generic gesture action.
    }

    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || scaleDetector.onTouchEvent(event);
    }

    private final class InternalScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override public boolean onScale(ScaleGestureDetector detector) {
            onGesture(Gesture.PINCH);
            return super.onScale(detector);
        }
    }

    private final class InternalGestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_DISTANCE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            onGesture(Gesture.TAP);
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            // Only call onGesture if it's a DOWN event.
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                onGesture(Gesture.DOUBLE_TAP);
            }
            return true;
        }

        public void onLongPress(MotionEvent e) {
            GestureListener.this.onLongPress();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float distanceX = e2.getX() - e1.getX();
            float distanceY = e2.getY() - e1.getY();

            if (Math.abs(distanceX) > Math.abs(distanceY)
                    && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD
                    && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (distanceX > 0) {
                    onGesture(Gesture.RIGHT);
                } else {
                    onGesture(Gesture.LEFT);
                }
                return true;
            } else if (Math.abs(distanceY) > Math.abs(distanceX)
                    && Math.abs(distanceY) > SWIPE_DISTANCE_THRESHOLD
                    && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (distanceY > 0) {
                    onGesture(Gesture.DOWN);
                } else {
                    onGesture(Gesture.UP);
                }
                return true;
            }
            return false;
        }
    }
}