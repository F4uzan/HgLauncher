package mono.hg.wrappers

import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * A fragment class meant to handle back button press.
 * Taken from http://vinsol.com/blog/2014/10/01/handling-back-button-press-inside-fragments/
 */
abstract class BackHandledFragment : Fragment() {
    private var backHandlerInterface: BackHandlerInterface? = null

    /**
     * Action done when back button is pressed
     *
     * @return Boolean Whether the back press event is consumed at all.
     */
    abstract fun onBackPressed(): Boolean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backHandlerInterface = if (activity !is BackHandlerInterface) {
            throw ClassCastException("Hosting activity must implement BackHandlerInterface")
        } else {
            activity as BackHandlerInterface?
        }
    }

    override fun onStart() {
        super.onStart()

        // Mark this fragment as the selected Fragment.
        backHandlerInterface!!.setSelectedFragment(this)
    }

    /**
     * The interface used to intercept back button press events on a selected fragment.
     */
    interface BackHandlerInterface {
        /**
         * Set a fragment to be the main focus (selected) fragment.
         *
         * @param backHandledFragment The fragment itself. Must implement [BackHandlerInterface].
         */
        fun setSelectedFragment(backHandledFragment: BackHandledFragment?)
    }
}