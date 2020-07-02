package mono.hg.models

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFilterable
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import mono.hg.R
import mono.hg.helpers.KissFuzzySearch
import mono.hg.helpers.PreferenceHelper

open class App : AbstractFlexibleItem<App.ViewHolder>, IFilterable<String> {
    private var HINT_MATCH_SCORE = 30
    private var NAME_MATCH_SCORE = 25
    var appName: String? = null
        private set
    var packageName: String
        private set
    lateinit var userPackageName: String
    var hintName: String? = null
    var isAppHidden: Boolean
    var icon: Drawable? = null
    var user: Long
        private set

    constructor(appName: String?, packageName: String, isAppHidden: Boolean, user: Long) {
        this.packageName = packageName
        this.appName = appName
        this.isAppHidden = isAppHidden
        this.user = user
    }

    constructor(packageName: String, user: Long) {
        this.packageName = packageName
        isAppHidden = false
        this.user = user
    }

    private fun hasHintName(): Boolean {
        return hintName != null
    }

    fun setHintMatchScore(newScore: Int) {
        HINT_MATCH_SCORE = newScore
    }

    fun setNameMatchScore(newScore: Int) {
        NAME_MATCH_SCORE = newScore
    }

    override fun equals(other: Any?): Boolean {
        if (javaClass != other!!.javaClass) {
            return false
        }
        val alt = other as App?
        return this === other || userPackageName == alt?.userPackageName
    }

    override fun hashCode(): Int {
        return userPackageName.hashCode()
    }

    override fun getLayoutRes(): Int {
        return if (PreferenceHelper.useGrid()) {
            R.layout.grid_generic_item
        } else {
            R.layout.list_generic_item
        }
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<*>?>?): ViewHolder {
        return ViewHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<*>?>?,
                                holder: ViewHolder, position: Int,
                                payloads: List<Any>) {
        holder.name?.text = appName
        holder.icon.setImageDrawable(icon)
    }

    override fun filter(constraint: String): Boolean {
        var fuzzyScore = 0

        // See if we can match by hint names.
        if (hasHintName()) {
            fuzzyScore = KissFuzzySearch.doFuzzy(hintName, constraint)
        }

        // Is the hint name strong enough?
        return if (fuzzyScore >= HINT_MATCH_SCORE) {
            true
        } else {
            // Fall back to app name matching if it isn't.
            fuzzyScore = KissFuzzySearch.doFuzzy(appName, constraint)
            fuzzyScore >= NAME_MATCH_SCORE
        }
    }

    class ViewHolder internal constructor(view: View, adapter: FlexibleAdapter<IFlexible<*>?>?) : FlexibleViewHolder(view, adapter) {
        var name: TextView? = view.findViewById(R.id.item_name)
        var icon: ImageView = view.findViewById(R.id.item_icon)
    }
}