package mono.hg.adapters

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import mono.hg.fragments.AppListFragment
import mono.hg.fragments.GenericPageFragment
import mono.hg.fragments.WidgetListFragment

/**
 * The adapter used to hold Pages such as [WidgetListFragment] and [AppListFragment].
 *
 * This adapter is to be used for [ViewPager2].
 */
class PageAdapter(fragment: FragmentActivity, viewPager: ViewPager2) :
    FragmentStateAdapter(fragment) {
    private val currentFragment = fragment
    private val currentViewPager = viewPager

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): GenericPageFragment {
        return if (position == 0) {
            WidgetListFragment()
        } else {
            AppListFragment()
        }
    }

    /**
     * Helper function to retrieve currently-viewed Page.
     * Uses a hack available in ViewPager2 fragment tagging.
     */
    fun getCurrentPage(): GenericPageFragment? {
        return currentFragment.supportFragmentManager.findFragmentByTag("f" + currentViewPager.currentItem) as GenericPageFragment?
    }
}