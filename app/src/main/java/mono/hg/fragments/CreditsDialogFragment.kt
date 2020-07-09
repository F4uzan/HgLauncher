package mono.hg.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.util.Linkify
import androidx.appcompat.app.AlertDialog
import androidx.core.text.util.LinkifyCompat
import androidx.fragment.app.DialogFragment
import mono.hg.R
import mono.hg.databinding.FragmentCreditsDialogBinding
import mono.hg.helpers.PreferenceHelper
import mono.hg.utils.Utils
import mono.hg.utils.Utils.LogLevel
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * A DialogFragment that reads from assets/credits.txt, returning its content to a TextView.
 */
class CreditsDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragmentCreditsDialogBinding.inflate(requireActivity().layoutInflater)
        val builder = AlertDialog.Builder(requireActivity())
        val stringBuilder = StringBuilder()
        var br: BufferedReader? = null
        builder.setTitle(R.string.about_credits_dialog_title)
        builder.setView(binding.root)
        builder.setPositiveButton(R.string.dialog_action_close, null)
        val creditsText = binding.creditsPlaceholder
        creditsText.highlightColor = PreferenceHelper.darkAccent
        creditsText.setLinkTextColor(PreferenceHelper.accent)
        try {
            br = BufferedReader(
                InputStreamReader(requireActivity().assets.open("credits.txt"))
            )
            var line: String?
            while (br.readLine().also { line = it } != null) {
                stringBuilder.append(line)
                stringBuilder.append('\n')
            }
        } catch (e: IOException) {
            Utils.sendLog(
                LogLevel.ERROR,
                "Exception in reading credits file: $e"
            )
        } finally {
            Utils.closeStream(br)
        }
        creditsText.text = stringBuilder.toString()
        LinkifyCompat.addLinks(creditsText, Linkify.WEB_URLS)

        val themedDialog = builder.create()
        themedDialog.show()

        themedDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            .setTextColor(PreferenceHelper.darkAccent)
        themedDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            .setTextColor(PreferenceHelper.darkAccent)

        return themedDialog
    }
}