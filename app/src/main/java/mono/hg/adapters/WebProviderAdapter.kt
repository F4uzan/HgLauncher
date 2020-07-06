package mono.hg.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import mono.hg.R
import mono.hg.models.WebSearchProvider
import java.util.*

/**
 * Adapter used to handle display web providers. Used only in preferences.
 */
class WebProviderAdapter(private val itemList: ArrayList<WebSearchProvider>, private val context: Context) : BaseAdapter() {
    override fun getCount(): Int {
        return itemList.size
    }

    override fun getItem(i: Int): Any {
        return itemList[i]
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, convertView: View?, parent: ViewGroup): View? {
        val viewHolder: ViewHolder
        var view = convertView

        if (view == null) {
            view = View.inflate(context, R.layout.list_web_provider_item, null)
            viewHolder = ViewHolder()
            viewHolder.title = view.findViewById(R.id.provider_title)
            viewHolder.url = view.findViewById(R.id.provider_url)
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }

        viewHolder.title!!.text = itemList[i].name
        viewHolder.url!!.text = itemList[i].url

        return view
    }

    private class ViewHolder {
        internal var title: TextView? = null
        internal var url: TextView? = null
    }
}