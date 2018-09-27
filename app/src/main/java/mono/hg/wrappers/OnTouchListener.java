package mono.hg.wrappers;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Detects touch events across a view.
 * Based on OnTouchListener originally written by Edward Brey at StackOverflow,
 * (https://stackoverflow.com/a/19506010)
 * <p>
 * Modified to add swipe up, swipe down, single tap, and long press events.
 */
public class OnTouchListener implements View.OnTouchListener {

    private final GestureDetector gestureDetector;
    private float startX;
    private float startY;
    private float endX;
    private float endY;

    protected OnTouchListener(Context context) {
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public void onSwipeLeft() {
        // Do swipe left action.
    }

    public void onSwipeRight() {
        // Do swipe right action.
    }

    public void onSwipeUp() {
        // Do swipe up action.
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
        return gestureDetector.onTouchEvent(event);
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

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

        public void onLongPress(MotionEvent e) {
            OnTouchListener.this.onLongPress();
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
                    onSwipeRight();
                } else {
                    onSwipeLeft();
                }
                return true;
            } else if (Math.abs(distanceY) > Math.abs(distanceX)
                    && Math.abs(distanceY) > SWIPE_DISTANCE_THRESHOLD
                    && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (distanceY > 0) {
                    onSwipeDown();
                } else {
                    onSwipeUp();
                }
            }
            return false;
        }
    }
}