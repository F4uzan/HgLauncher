package mono.hg.views

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Small modification to [GridLayoutManager] that blocks requestChildRectangleOnScreen
 * to prevent crashing when toggling a PopupMenu over its child.
 */
class CustomGridLayoutManager(context: Context?, spanCount: Int) : GridLayoutManager(context, spanCount) {
    override fun requestChildRectangleOnScreen(parent: RecyclerView, child: View, rect: Rect, immediate: Boolean): Boolean {
        return false
    }
}