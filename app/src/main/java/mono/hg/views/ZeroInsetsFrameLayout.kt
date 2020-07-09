package mono.hg.views

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.WindowInsets
import android.widget.FrameLayout

/**
 * FrameLayout workaround for transparency issue in Android 4.4 and above.
 *
 * Huge thanks and credits to Hogun (https://stackoverflow.com/a/41448403) for the class.
 */
class ZeroInsetsFrameLayout : FrameLayout {
    private val insets = IntArray(4)

    constructor(context: Context?) : super(context !!) {
        // Unused. Used only in XML initialisation.
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context !!, attrs) {
        // Unused. Used only in XML initialisation.
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context !!,
        attrs,
        defStyle
    ) {
        // Unused. Used only in XML initialisation.
    }

    override fun computeSystemWindowInsets(`in`: WindowInsets, outLocalInsets: Rect): WindowInsets {
        outLocalInsets.left = 0
        outLocalInsets.top = 0
        outLocalInsets.right = 0
        return super.computeSystemWindowInsets(`in`, outLocalInsets)
    }

    override fun fitSystemWindows(windowInsets: Rect): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Intentionally do not modify the bottom inset. For some reason,
            // if the bottom inset is modified, window resizing stops working.
            // TODO: Figure out why.
            insets[0] = windowInsets.left
            insets[1] = windowInsets.top
            insets[2] = windowInsets.right
            windowInsets.left = 0
            windowInsets.top = 0
            windowInsets.right = 0
        }
        return super.fitSystemWindows(windowInsets)
    }
}