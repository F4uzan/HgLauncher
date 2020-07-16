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

        with(AlertDialog.Builder(requireActivity())) {
            setTitle(R.string.about_credits_dialog_title)
            setView(binding.root)
            setPositiveButton(R.string.dialog_action_close, null)

            binding.creditsPlaceholder.apply {
                highlightColor = PreferenceHelper.darkAccent
                setLinkTextColor(PreferenceHelper.accent)
                text = readCredits()
                LinkifyCompat.addLinks(this, Linkify.WEB_URLS)
            }

            create().apply {
                show()
                getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(PreferenceHelper.darkAccent)
                getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(PreferenceHelper.darkAccent)
                return this
            }
        }
    }

    private fun readCredits() : String {
        val stringBuilder = StringBuilder()
        var br: BufferedReader? = null

        try {
            br = BufferedReader(
                InputStreamReader(requireActivity().assets.open("credits.txt"))
            )
            var line: String?
            while (br.readLine().also { line = it } != null) {
                stringBuilder.append(line).append('\n')
            }
        } catch (e: IOException) {
            Utils.sendLog(
                LogLevel.ERROR,
                "Exception in reading credits file: $e"
            )
        } finally {
            Utils.closeStream(br)
        }

        return stringBuilder.toString()
    }
}