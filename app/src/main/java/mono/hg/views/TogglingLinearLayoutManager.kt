package mono.hg.views

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A LinearLayoutManager class with scroll-blocking methods.
 */
class TogglingLinearLayoutManager(context: Context?, orientation: Int, stackFromEnd: Boolean) :
    LinearLayoutManager(context, orientation, stackFromEnd) {
    override fun requestChildRectangleOnScreen(
        parent: RecyclerView,
        child: View,
        rect: Rect,
        immediate: Boolean
    ): Boolean {
        return false
    }

    init {
        setStackFromEnd(stackFromEnd)
        reverseLayout = !stackFromEnd
    }
}