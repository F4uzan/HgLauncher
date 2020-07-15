package mono.hg.utils

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.view.View
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
     * Fetch statusbar height from system's dimension.
     *
     * @return int Size of the statusbar. Returns the fallback value of 24dp if the
     * associated dimen value cannot be found.
     */
    val statusBarHeight: Int
        get() {
            val idStatusBarHeight = Resources.getSystem()
                .getIdentifier("status_bar_height", "dimen", "android")
            return if (idStatusBarHeight > 0) {
                Resources.getSystem().getDimensionPixelSize(idStatusBarHeight)
            } else {
                // Return fallback size if we can't get the value from the system.
                Resources.getSystem().getDimensionPixelSize(R.dimen.status_bar_height_fallback)
            }
        }

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
     * Launches an app based on RecyclerView scroll state.
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
     * Sets initial fragment. This fragment is not added to the backstack.
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
     * Replace existing fragment with another. This adds the fragment to the back stack.
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
     * Creates a PopupMenu containing available search provider.
     *
     * @param activity  Activity where the PopupMenu resides.
     * @param popupMenu The PopupMenu to populate and show.
     * @param query     Search query to launch when a provider is selected.
     */
    fun createSearchMenu(activity: AppCompatActivity, popupMenu: PopupMenu, query: String?) {
        PreferenceHelper.providerList.forEach { popupMenu.menu.add(it.key) }
        popupMenu.setOnMenuItemClickListener { menuItem ->
            Utils.doWebSearch(
                activity,
                PreferenceHelper.getProvider(menuItem.title.toString()),
                query
            )
            true
        }
        popupMenu.show()
    }

    /**
     * Called when the activity needs to be restarted (i.e when a theme change occurs).
     * Allows for smooth transition between recreation.
     */
    fun restartActivity(activity: AppCompatActivity, clearTask: Boolean) {
        val intent = Intent(activity.intent).apply {
            this.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            if (clearTask) {
                this.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            } else {
                this.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }.also {
            activity.startActivity(it)
            activity.finish()
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

    }

}