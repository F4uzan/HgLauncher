package mono.hg.utils

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import mono.hg.R
import mono.hg.adapters.AppAdapter
import mono.hg.helpers.PreferenceHelper

/**
 * Utils class handling transformation of views relating to the launcher.
 *
 * Generally, most misc. view-handling is also stored here.
 */
object ViewUtils {
    /**
     * Configures the status bar and navigation bar mode according to the
     * user's preference.
     *
     * @param mode  Between "status", "nav", "both", or "none". The parameter used
     *              set the mode of the system bars.
     *
     * @see R.array.pref_windowbar_values
     */
    @Suppress("DEPRECATION") // This is meant for older APIs.
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun setWindowbarMode(mode: String?): Int {
        val baseLayout: Int = if (Utils.sdkIsAround(19)) {
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        } else {
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        val noStatusLayout = (baseLayout
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        val noNavLayout = (baseLayout
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        return when (mode) {
            "status" -> noStatusLayout
            "nav" -> noNavLayout
            "both" -> noStatusLayout or noNavLayout
            "none" -> View.SYSTEM_UI_LAYOUT_FLAGS
            else -> View.SYSTEM_UI_LAYOUT_FLAGS
        }
    }

    /**
     * Configures the status bar and navigation bar mode according to the
     * user's preference.
     *
     * @param mode  Between "status", "nav", "both", or "none". The parameter used
     *              set the mode of the system bars.
     *
     * @see R.array.pref_windowbar_values
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun setWindowBarMode(activity: AppCompatActivity, mode: String?) {
        activity.window.setDecorFitsSystemWindows(false)

        val insetsController = activity.window.insetsController
        insetsController?.systemBarsBehavior =
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        when (mode) {
            "status" -> insetsController?.hide(WindowInsets.Type.statusBars())
            "nav" -> insetsController?.hide(WindowInsets.Type.navigationBars())
            "both" -> insetsController?.hide(WindowInsets.Type.systemBars())
        }
    }

    /**
     * Launches an app based on RecyclerView scroll state.
     *
     * When the RecyclerView is unable to scroll upwards, it will
     * call the topmost item. Otherwise, the reverse applies when
     * the RecyclerView can't scroll downwards.
     *
     * @param activity     The activity for context reference.
     * @param recyclerView The RecyclerView itself.
     * @param adapter      A FlexibleAdapter with App items.
     */
    fun keyboardLaunchApp(
        activity: Activity,
        recyclerView: RecyclerView,
        adapter: AppAdapter
    ) {
        if (recyclerView.canScrollVertically(RecyclerView.FOCUS_UP)) {
            adapter.getItem(0)?.let { AppUtils.launchApp(activity, it) }
        } else if (! recyclerView.canScrollVertically(RecyclerView.FOCUS_DOWN)) {
            adapter.getItem(adapter.itemCount - 1)?.let { AppUtils.launchApp(activity, it) }
        }
    }

    /**
     * Sets initial/starting fragment.
     *
     * This function should be called early, when the activity has no other
     * fragments to present, as this fragment will not be added to back stack.
     *
     * @param fragmentManager The fragment manager in the current activity.
     * @param fragment        The fragment to use.
     */
    fun setFragment(fragmentManager: FragmentManager, fragment: Fragment?, tag: String?) {
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment !!, tag)
            .commit()
    }

    /**
     * Replace existing fragment with another fragment.
     *
     * This function adds the fragment to the back stack, allowing for calls
     * to [Activity.onBackPressed] to proceed. The fragment and activity
     * should account for this.
     *
     * @param fragmentManager The fragment manager in the current activity.
     * @param fragment        The fragment to use.
     * @param tag             The tag used for the fragment.
     */
    fun replaceFragment(fragmentManager: FragmentManager, fragment: Fragment?, tag: String?) {
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment !!)
            .addToBackStack(tag)
            .commit()
    }

    /**
     * Creates a PopupMenu containing all available search provider.
     *
     * @param activity  Activity where the PopupMenu resides.
     * @param popupMenu The PopupMenu to populate and show.
     * @param query     Search query to launch when a provider is selected.
     */
    fun createSearchMenu(activity: AppCompatActivity, popupMenu: PopupMenu, query: String) {
        PreferenceHelper.providerList.forEach { popupMenu.menu.add(it.key) }
        popupMenu.setOnMenuItemClickListener { menuItem ->
            PreferenceHelper.getProvider(menuItem.title.toString())?.let {
                Utils.doWebSearch(
                    activity,
                    it,
                    query
                )
            }
            true
        }
        popupMenu.show()
    }

    /**
     * Called when the activity needs to be restarted (i.e when a theme change occurs).
     * Allows for smooth transition between recreation, without the flicker associated
     * with calling [Activity.recreate].
     *
     * @param activity  AppCompatActivity that needs to be restarted.
     * @param clearTask Whether [Intent.FLAG_ACTIVITY_NEW_TASK] and [Intent.FLAG_ACTIVITY_CLEAR_TASK]
     *                  flags should be used. These flags are useful for bottommost activity,
     *                  where the backstack is at its end.
     */
    fun restartActivity(activity: AppCompatActivity, clearTask: Boolean) {
        val intent = Intent(activity.intent).apply {
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            if (clearTask) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            } else {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }.also {
            activity.startActivity(it)
            activity.finish()
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

    }

}