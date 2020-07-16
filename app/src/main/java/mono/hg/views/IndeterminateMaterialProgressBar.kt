package mono.hg.views

import android.content.Context
import android.util.AttributeSet
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
    companion object {
        private const val WIDTH_DP = 4
    }

    init {
        val metrics = resources.displayMetrics
        metrics.density.let {
            val drawable = CircularProgressDrawable(context).apply {
                setColorSchemeColors(PreferenceHelper.accent)
                strokeWidth = WIDTH_DP * it
                indeterminateDrawable = this
            }
        }
    }
}