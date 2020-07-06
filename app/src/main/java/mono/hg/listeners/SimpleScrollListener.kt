package mono.hg.listeners

import androidx.recyclerview.widget.RecyclerView

abstract class SimpleScrollListener protected constructor(private val HIDE_THRESHOLD: Int) : RecyclerView.OnScrollListener() {
    private var mScrolledDistance = 0

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        onScroll()
        if (!recyclerView.canScrollVertically(RecyclerView.FOCUS_DOWN)) {
            onEnd()
            resetScrollDistance()
        } else {
            if (mScrolledDistance < -HIDE_THRESHOLD) {
                onScrollUp()
                resetScrollDistance()
            }
        }
        if (dy < 0) {
            mScrolledDistance += dy
        }
    }

    private fun resetScrollDistance() {
        mScrolledDistance = 0
    }

    /**
     * Called when the RecyclerView has reached the bottom (end), and cannot scroll any further.
     */
    open fun onEnd() {
        // No-op. Overridden when needed.
    }

    /**
     * Called when the RecyclerView is scrolling upwards.
     * This function does not check whether the RecyclerView can or cannot scroll further.
     */
    open fun onScrollUp() {
        // No-op. Overridden when needed.
    }

    /**
     * Called when the RecyclerView has begun scrolling, or detects a motion of scrolling.
     */
    abstract fun onScroll()

}