package mono.hg.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import mono.hg.R
import mono.hg.helpers.PreferenceHelper
import mono.hg.models.FileFolder
import java.util.*

/**
 * Adapter used to handle displaying over files and folders.
 */
class FileFolderAdapter(private val filesList: ArrayList<FileFolder>, private val context: Context) : BaseAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val holder: ViewHolder
        var view = convertView

        if (view == null) {
            view = View.inflate(context, R.layout.list_generic_item, null)
            holder = ViewHolder()
            holder.content = view.findViewById(R.id.item_icon)
            holder.name = view.findViewById(R.id.item_name)
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }

        if (filesList[position].isFolder) {
            holder.content!!.setImageResource(R.drawable.ic_folder)
        } else {
            holder.content!!.setImageResource(R.drawable.ic_file)
        }

        ImageViewCompat.setImageTintList(holder.content!!, ColorStateList.valueOf(PreferenceHelper.accent))
        holder.name!!.text = filesList[position].name

        return view
    }

    override fun getCount(): Int {
        return filesList.size
    }

    override fun getItem(position: Int): Any {
        return filesList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    private class ViewHolder {
        internal var content: ImageView? = null
        internal var name: TextView? = null
    }

}