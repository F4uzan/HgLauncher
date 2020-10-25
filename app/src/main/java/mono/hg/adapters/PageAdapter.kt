package mono.hg.adapters

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import mono.hg.pages.AppsListPage
import mono.hg.pages.GenericPage
import mono.hg.pages.WidgetsListPage
import mono.hg.helpers.PreferenceHelper

/**
 * The adapter used to hold Pages such as [WidgetsListPage] and [AppsListPage].
 *
 * This adapter is to be used for [ViewPager2].
 */
class PageAdapter(fragment: FragmentActivity, viewPager: ViewPager2) :
    FragmentStateAdapter(fragment) {
    private val currentFragment = fragment
    private val currentViewPager = viewPager

    override fun getItemCount(): Int = if (PreferenceHelper.widgetSpaceVisible()) 2 else 1

    override fun createFragment(position: Int): GenericPage {
        when (position) {
            0 -> return if (PreferenceHelper.widgetSpaceVisible()) WidgetsListPage() else AppsListPage()
            1 -> if (PreferenceHelper.widgetSpaceVisible()) return AppsListPage()
        }
        return GenericPage() // Return a generic page as a fallback.
    }

    /**
     * Helper function to retrieve currently-viewed Page.
     * Uses a hack available in ViewPager2 fragment tagging.
     */
    fun getCurrentPage(): GenericPage? {
        return currentFragment.supportFragmentManager.findFragmentByTag("f" + currentViewPager.currentItem) as GenericPage?
    }
}