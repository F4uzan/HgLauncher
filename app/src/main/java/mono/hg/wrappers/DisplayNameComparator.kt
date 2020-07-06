package mono.hg.wrappers

import mono.hg.models.App
import java.text.Collator
import java.util.*

/**
 * A simple [Comparator] that does direct comparison between two appName.
 */
class DisplayNameComparator : Comparator<App> {
    private val collator = Collator.getInstance()
    override fun compare(a: App, b: App): Int {
        return collator.compare(a.appName, b.appName)
    }
}