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

    open fun onEnd() {
        // No-op. Overridden when needed.
    }

    open fun onScrollUp() {
        // No-op. Overridden when needed.
    }

    abstract fun onScroll()

}