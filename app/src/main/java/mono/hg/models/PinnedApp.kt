package mono.hg.models

import android.graphics.drawable.Drawable
import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import mono.hg.R
import mono.hg.utils.AppUtils

/**
 * An extension of the [App] class that doesn't extracts app name.
 * This class is used in the favourites panel.
 */
class PinnedApp : App {
    constructor(icon: Drawable?, packageName: String, user: Long) : super(
        null,
        packageName,
        false,
        user
    ) {
        this.icon = icon
        userPackageName = AppUtils.appendUser(user, packageName)
    }

    constructor(packageName: String, user: Long) : super(packageName, user) {
        userPackageName = AppUtils.appendUser(user, packageName)
    }

    override fun getLayoutRes(): Int {
        return R.layout.list_pinned_item
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
        holder.icon.setImageDrawable(icon)
    }
}