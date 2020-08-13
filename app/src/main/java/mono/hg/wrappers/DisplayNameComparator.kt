package mono.hg.wrappers

import mono.hg.models.App
import java.text.Collator
import java.util.*

/**
 * A simple [Comparator] that does direct comparison between two appName.
 *
 * @param descending Whether to use descending order when sorting the list.
 */
class DisplayNameComparator(private var descending: Boolean) : Comparator<App> {
    private val collator = Collator.getInstance()
    override fun compare(a: App, b: App): Int {
        return if (descending) {
            collator.compare(a.appName, b.appName) * (-1)
        } else {
            collator.compare(a.appName, b.appName)
        }
    }
}