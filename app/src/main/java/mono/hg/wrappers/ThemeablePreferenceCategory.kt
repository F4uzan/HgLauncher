package mono.hg.wrappers

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import mono.hg.helpers.PreferenceHelper

/**
 * A PreferenceCategory that sets its colour from [PreferenceHelper.accent].
 */
class ThemeablePreferenceCategory(context: Context, attrs: AttributeSet?) : PreferenceCategory(context, attrs) {
    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        // TODO: Might be a hack? Investigate further.
        val title : TextView? = holder?.itemView?.findViewById(android.R.id.title)
        title?.setTextColor(PreferenceHelper.accent)
    }
}