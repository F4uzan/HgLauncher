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
        val screenDensity = metrics.density
        val drawable = CircularProgressDrawable(context)
        drawable.setColorSchemeColors(PreferenceHelper.accent)
        drawable.strokeWidth = WIDTH_DP * screenDensity
        indeterminateDrawable = drawable
    }
}