package mono.hg.wrappers

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView

/**
 * A [NestedScrollView] that does not perform interception
 * on touch events. This class should be used when its child
 * are expected to consume a scroll event, and that a regular
 * [NestedScrollView] will otherwise consume said event. Otherwise,
 * its use are discouraged.
 */
class NoInterceptScrollView(context: Context, attrs: AttributeSet?) :
    NestedScrollView(context, attrs) {

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return false
    }
}