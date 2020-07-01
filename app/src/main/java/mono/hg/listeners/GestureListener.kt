package mono.hg.listeners

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.View.OnTouchListener
import mono.hg.utils.Utils
import kotlin.math.abs

/**
 * Detects touch events across a view.
 * Based on GestureListener originally written by Edward Brey at StackOverflow,
 * (https://stackoverflow.com/a/19506010)
 *
 *
 * Modified to add swipe up, swipe down, single tap, and long press events.
 */
open class GestureListener protected constructor(context: Context?) : OnTouchListener {
    private val gestureDetector: GestureDetector
    private val scaleDetector: ScaleGestureDetector

    open fun onLongPress() {
        // Do long press action.
    }

    open fun onGesture(direction: Int) {
        // Do generic gesture action.
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || scaleDetector.onTouchEvent(event)
    }

    private inner class InternalScaleGestureListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            onGesture(Utils.Gesture.PINCH)
            return super.onScale(detector)
        }
    }

    private inner class InternalGestureListener : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
            onGesture(Utils.Gesture.TAP)
            return true
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            // Only call onGesture if it's a DOWN event.
            if (e.action == MotionEvent.ACTION_DOWN) {
                onGesture(Utils.Gesture.DOUBLE_TAP)
            }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            this@GestureListener.onLongPress()
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val distanceX = e2.x - e1.x
            val distanceY = e2.y - e1.y
            if (abs(distanceX) > abs(distanceY) && abs(distanceX) > Companion.SWIPE_DISTANCE_THRESHOLD && abs(velocityX) > Companion.SWIPE_VELOCITY_THRESHOLD) {
                if (distanceX > 0) {
                    onGesture(Utils.Gesture.RIGHT)
                } else {
                    onGesture(Utils.Gesture.LEFT)
                }
                return true
            } else if (abs(distanceY) > abs(distanceX) && abs(distanceY) > Companion.SWIPE_DISTANCE_THRESHOLD && abs(velocityX) > Companion.SWIPE_VELOCITY_THRESHOLD) {
                if (distanceY > 0) {
                    onGesture(Utils.Gesture.DOWN)
                } else {
                    onGesture(Utils.Gesture.UP)
                }
                return true
            }
            return false
        }
    }

    companion object {
        private const val SWIPE_DISTANCE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    init {
        gestureDetector = GestureDetector(context, InternalGestureListener())
        scaleDetector = ScaleGestureDetector(context, InternalScaleGestureListener())
    }
}