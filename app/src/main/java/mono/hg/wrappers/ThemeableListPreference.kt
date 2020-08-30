package mono.hg.wrappers

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import mono.hg.R
import mono.hg.helpers.PreferenceHelper
import mono.hg.utils.Utils
import mono.hg.utils.applyAccent

/**
 * A ListPreference with custom style and dialogue buttons that follows [PreferenceHelper.accent].
 */
class ThemeableListPreference(context: Context?, attrs: AttributeSet?) :
    ListPreference(context, attrs) {
    override fun onClick() {
        val theme: Int =
            if (Utils.sdkIsBelow(17) &&
                (PreferenceHelper.appTheme() == "dark" || PreferenceHelper.appTheme() == "black")
            ) {
                R.style.PreferenceList_Night_NoRadio
            } else {
                R.style.PreferenceList_NoRadio
            }

        with(AlertDialog.Builder(context, theme)) {
            setTitle(title)
            setMessage(dialogMessage)
            setSingleChoiceItems(entries, entries.indexOf(entry)) { it, index ->
                if (callChangeListener(entryValues[index].toString())) {
                    setValueIndex(index)
                }
                it.dismiss()
            }
            setNegativeButton(R.string.dialog_cancel) { it, _ ->
                it.dismiss()
            }

            create().apply {
                show()
                applyAccent()
            }
        }
    }
}