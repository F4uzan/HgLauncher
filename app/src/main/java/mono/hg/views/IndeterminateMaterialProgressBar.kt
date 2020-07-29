package mono.hg.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ProgressBar
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import mono.hg.helpers.PreferenceHelper

/**
 * Indeterminate progress bar, in the style of Material Design.
 *
 * Uses [CircularProgressDrawable] to draw its progress bar.
 */
class IndeterminateMaterialProgressBar(context: Context, attrs: AttributeSet?) :
    ProgressBar(context, attrs) {

    /**
     * Hides and invalidates this [IndeterminateMaterialProgressBar] if it's visibility
     * is not [View.GONE]. Calls [View.invalidate], as such it is best if
     * the ProgressBar is not recalled again once hidden.
     */
    fun hide() {
        if (visibility != View.GONE) {
            this.visibility = View.GONE
            this.invalidate()
        }
    }

    /**
     * Show this [IndeterminateMaterialProgressBar] if it's not already visible.
     */
    fun show() {
        if (visibility != View.VISIBLE) {
            this.visibility = View.VISIBLE
        }
    }

    init {
        resources.displayMetrics.density.let {
            CircularProgressDrawable(context).apply {
                setColorSchemeColors(PreferenceHelper.accent)
                strokeWidth = WIDTH_DP * it
                indeterminateDrawable = this
            }
        }
    }

    companion object {
        private const val WIDTH_DP = 4
    }
}