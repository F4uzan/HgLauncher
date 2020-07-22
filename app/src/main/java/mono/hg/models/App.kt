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
import mono.hg.utils.AppUtils

/**
 * The app object. Holds information such as package name, shorthands, icons, and visibility state.
 * Implements [IFilterable] to allow searching in the app list.
 */
class App : AbstractFlexibleItem<App.ViewHolder>, IFilterable<String> {
    private var HINT_MATCH_SCORE = 30
    private var NAME_MATCH_SCORE = 25
    private var layoutType = 0
    var appName: String? = null
        private set
    var packageName: String
        private set
    lateinit var userPackageName: String
    var hintName: String? = null
    var isAppHidden = false
    var icon: Drawable? = null
    var user: Long = 0
        private set

    constructor(appName: String?, packageName: String, isAppHidden: Boolean, user: Long) {
        this.packageName = packageName
        this.appName = appName
        this.isAppHidden = isAppHidden
        this.user = user
    }

    constructor(icon: Drawable, packageName: String, user: Long) {
        this.layoutType = 1
        this.icon = icon
        this.packageName = packageName
        this.userPackageName = AppUtils.appendUser(user, packageName)
    }

    constructor(packageName: String, user: Long) {
        this.packageName = packageName
        isAppHidden = false
        this.user = user
    }

    private fun hasHintName(): Boolean {
        return hintName != null
    }

    override fun equals(other: Any?): Boolean {
        if (javaClass != other !!.javaClass) {
            return false
        }
        val alt = other as App?
        return this === other || userPackageName == alt?.userPackageName
    }

    override fun hashCode(): Int {
        return userPackageName.hashCode()
    }

    override fun getLayoutRes(): Int {
        return when (itemViewType) {
            GENERIC_APP_TYPE -> if (PreferenceHelper.useGrid()) {
                R.layout.grid_generic_item
            } else {
                R.layout.list_generic_item
            }
            PINNED_APP_TYPE -> R.layout.list_pinned_item
            else -> R.layout.list_generic_item
        }
    }

    override fun getItemViewType(): Int {
        return layoutType
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<*>?>?
    ): ViewHolder {
        return ViewHolder(view, adapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<*>?>?,
        holder: ViewHolder, position: Int,
        payloads: List<Any>
    ) {
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

    /**
     * The ViewHolder that stores the app name and icon.
     *
     * Uses [R.layout.list_generic_item] as its layout,
     * but this can be overriden in [App.getLayoutRes].
     */
    class ViewHolder internal constructor(view: View, adapter: FlexibleAdapter<IFlexible<*>?>?) :
        FlexibleViewHolder(view, adapter) {
        var name: TextView? = view.findViewById(R.id.item_name)
        var icon: ImageView = view.findViewById(R.id.item_icon)
    }

    companion object {
        const val GENERIC_APP_TYPE = 0
        const val PINNED_APP_TYPE = 1
    }
}