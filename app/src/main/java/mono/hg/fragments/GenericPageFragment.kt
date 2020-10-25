package mono.hg.fragments

import androidx.fragment.app.Fragment
import mono.hg.LauncherActivity

/**
 * A Fragment class that is specifically used to host Pages.
 * Pages must extend from this class, as LauncherActivity expects proper call to handle searches.
 */
open class GenericPageFragment : Fragment() {
    private var acceptsSearch: Boolean = false

    /**
     * Returns the attached LauncherActivity.
     *
     * @return LauncherActivity The attached LauncherActivity.
     */
    fun getLauncherActivity(): LauncherActivity {
        return requireActivity() as LauncherActivity
    }

    /**
     * Applies a search query to the items of this Page.
     *
     * Override if the Page's items can be searched by the search bar.
     *
     * @param query The query used to filter the items.
     */
    open fun commitSearch(query: String) {
        // Override when necessary.
    }

    /**
     * Checks if a Page is accepting search queries.
     *
     * @return Boolean Whether the Page can accept search queries.
     */
    open fun isAcceptingSearch(): Boolean {
        return acceptsSearch
    }

    /**
     * A setter to allow a Page's items to be searched.
     *
     * @param search Does this Page accepts search queries?
     */
    fun acceptsSearch(search: Boolean) {
        this.acceptsSearch = search
    }

    /**
     * When searching, the keyboard 'Search'/'Enter' button
     * has the capability to launch the most relevant result
     * from a Page. If a Page can serve this result, then
     * this function should be used.
     *
     * @return Boolean True if there is a result to be launched,
     *                  otherwise, let the LauncherActivity
     *                  consume the key press.
     */
    open fun launchPreselection(): Boolean {
        return false
    }
}