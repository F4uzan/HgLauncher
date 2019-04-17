package mono.hg.listeners;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
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
    private MotionEvent event;
    private float startX;
    private float startY;
    private float endX;
    private float endY;

    protected GestureListener(Context context) {
        gestureDetector = new GestureDetector(context, new InternalGestureListener());
    }

    public void onSwipeDown() {
        // Do swipe down action.
    }

    public void onLongPress() {
        // Do long press action.
    }

    public void onClick() {
        // Do tap/click action.
    }

    public void onGesture(int direction) {
        // Do generic gesture action.
    }

    public MotionEvent getEvent() {
        return event;
    }

    public float getStartX() {
        return startX;
    }

    public float getStartY() {
        return startY;
    }

    public float getEndX() {
        return endX;
    }

    public float getEndY() {
        return endY;
    }

    public boolean onTouch(View v, MotionEvent event) {
        this.event = event;
        return gestureDetector.onTouchEvent(event);
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
            onClick();
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            onGesture(Gesture.DOUBLE_TAP);
            return true;
        }

        public void onLongPress(MotionEvent e) {
            GestureListener.this.onLongPress();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float distanceX = e2.getX() - e1.getX();
            float distanceY = e2.getY() - e1.getY();
            startX = e1.getX();
            startY = e1.getY();
            endX = e2.getX();
            endY = e2.getY();

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
                    onSwipeDown();
                } else {
                    onGesture(Gesture.UP);
                }
                return true;
            }
            return false;
        }
    }
}