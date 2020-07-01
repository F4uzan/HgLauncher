package mono.hg.wrappers

import mono.hg.R
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import mono.hg.helpers.PreferenceHelper

/**
 * A DialogFragment class that loads a list of apps, in style of ListPreference.
 */
class AppSelectionPreferenceDialog : DialogFragment() {
    private var preference: String? = null
    private var mValue: String? = null
    private var mEntries: Array<CharSequence>? = null
    private var mEntryValues: Array<CharSequence>? = null
    private var mClickedDialogEntryIndex = 0
    private val selectItemListener = DialogInterface.OnClickListener { dialog, which ->
        if (mClickedDialogEntryIndex != which) {
            mClickedDialogEntryIndex = which
            mValue = mEntryValues!![mClickedDialogEntryIndex].toString()
            PreferenceHelper.update(preference, mValue)
        }
        dialog.dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            preference = arguments?.getString("key")
            mEntries = arguments?.getCharSequenceArray("entries")
            mEntryValues = arguments?.getCharSequenceArray("entryValues")
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (mValue == null) {
            val i = Intent().putExtra("key", preference)
            targetFragment!!.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED,
                    i)
            PreferenceHelper.update(preference, "none")
        } else {
            val i = Intent().putExtra("key", preference).putExtra("app", mValue)
            targetFragment!!.onActivityResult(targetRequestCode, Activity.RESULT_OK, i)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AlertDialog.Builder(requireActivity())
        dialog.setNegativeButton(android.R.string.cancel, null)
        dialog.setTitle(R.string.gesture_action_app_dialog_title)
        dialog.setSingleChoiceItems(mEntries, mClickedDialogEntryIndex, selectItemListener)
        return dialog.create()
    }
}