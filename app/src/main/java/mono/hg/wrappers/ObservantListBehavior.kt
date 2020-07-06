package mono.hg.wrappers

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Keep
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import kotlin.math.roundToInt

/**
 * CoordinatorLayout.Behavior that pushes the view upwards from its anchor.
 */
@Keep
class ObservantListBehavior : CoordinatorLayout.Behavior<View> {
    constructor() : super() {
        // Left empty. Used for XML initialisation.
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        // Left empty. Used for XML initialisation.
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        return dependency is SnackbarLayout
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        val translationY = 0f.coerceAtMost(dependency.translationY - dependency.height)
        child.animate().cancel()
        child.translationY = translationY
        child.setPadding(0, -translationY.roundToInt(), 0, 0)
        return true
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: View, dependency: View) {
        child.setPadding(0, 0, 0, 0)
        child.animate().translationY(0f).start()
    }
}