package mono.hg.wrappers

import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import mono.hg.R
import mono.hg.helpers.PreferenceHelper

/**
 * A ListPreference with custom style and dialogue buttons that follows [PreferenceHelper.accent].
 */
class ThemeableListPreference(context: Context?, attrs: AttributeSet?) :
    ListPreference(context, attrs) {
    override fun onClick() {
        val dialog = AlertDialog.Builder(context, R.style.PreferenceList_NoRadio)

        dialog.setTitle(title)
        dialog.setMessage(dialogMessage)
        dialog.setSingleChoiceItems(entries, entries.indexOf(entry)) { it, index ->
            if (callChangeListener(entryValues[index].toString())) {
                setValueIndex(index)
            }
            it.dismiss()
        }
        dialog.setNegativeButton(android.R.string.cancel) { it, _ ->
            it.dismiss()
        }

        val postDialog = dialog.create()
        postDialog.show()

        postDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
            .setTextColor(PreferenceHelper.darkAccent)
        postDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            .setTextColor(PreferenceHelper.darkAccent)
        postDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            .setTextColor(PreferenceHelper.darkAccent)
    }
}