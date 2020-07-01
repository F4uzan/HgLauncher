package mono.hg.wrappers

import android.os.Bundle
import androidx.fragment.app.Fragment

/*
 * A fragment class meant to handle back button press.
 * Taken from http://vinsol.com/blog/2014/10/01/handling-back-button-press-inside-fragments/
 */
abstract class BackHandledFragment : Fragment() {
    private var backHandlerInterface: BackHandlerInterface? = null
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

    interface BackHandlerInterface {
        fun setSelectedFragment(backHandledFragment: BackHandledFragment?)
    }
}