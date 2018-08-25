package f4.hubby.wrappers;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Detects touch events across a view.
 * Based on OnTouchListener originally written by Edward Brey at StackOverflow,
 * (https://stackoverflow.com/a/19506010)
 *
 * Modified to add swipe up, swipe down, single tap, and long press events.
 */
public class OnTouchListener implements View.OnTouchListener {

    private final GestureDetector gestureDetector;
    private float reportedX;
    private float reportedY;

    public OnTouchListener(Context context) {
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public void onSwipeLeft() {
    }

    public void onSwipeRight() {
    }

    public void onSwipeUp() {
    }

    public void onSwipeDown() {
    }

    public void onLongPress() {
    }

    public void onClick() {
    }

    public float getReportedX() {
        return reportedX;
    }

    public float getReportedY() {
        return reportedY;
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
            reportedX = e2.getX();
            reportedY = e2.getY();

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