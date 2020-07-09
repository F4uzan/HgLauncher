package mono.hg.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import mono.hg.R
import mono.hg.helpers.PreferenceHelper
import mono.hg.models.App
import java.util.*

/**
 * Adapter handling display of hidden apps. Only used in preferences.
 */
class HiddenAppAdapter(private val hiddenAppsList: ArrayList<App>, private val context: Context) :
    BaseAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val appHolder: ViewHolder
        var view = convertView

        if (view == null) {
            view = View.inflate(context, R.layout.list_generic_item, null)
            appHolder = ViewHolder()
            appHolder.icon = view.findViewById(R.id.item_icon)
            appHolder.name = view.findViewById(R.id.item_name)
            view.tag = appHolder
        } else {
            appHolder = view.tag as ViewHolder
        }

        appHolder.name?.text = hiddenAppsList[position].appName
        if (hiddenAppsList[position].isAppHidden) {
            appHolder.icon?.setImageResource(R.drawable.ic_check)
            appHolder.icon?.let {
                ImageViewCompat.setImageTintList(
                    it,
                    ColorStateList.valueOf(PreferenceHelper.accent)
                )
            }
            appHolder.name?.typeface = Typeface.DEFAULT_BOLD
        } else {
            appHolder.icon?.setImageDrawable(hiddenAppsList[position].icon)
            appHolder.icon?.let { ImageViewCompat.setImageTintList(it, null) }
            appHolder.name?.typeface = Typeface.DEFAULT
        }
        return view
    }

    override fun getCount(): Int {
        return hiddenAppsList.size
    }

    override fun getItem(position: Int): Any {
        return hiddenAppsList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    private class ViewHolder {
        internal var icon: ImageView? = null
        internal var name: TextView? = null
    }

}