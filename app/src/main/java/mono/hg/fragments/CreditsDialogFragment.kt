package mono.hg.fragments

import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.text.util.Linkify
import androidx.appcompat.app.AlertDialog
import androidx.core.text.util.LinkifyCompat
import mono.hg.R
import mono.hg.databinding.FragmentCreditsDialogBinding
import mono.hg.utils.Utils
import mono.hg.utils.Utils.LogLevel
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class CreditsDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragmentCreditsDialogBinding.inflate(activity.layoutInflater)
        val builder = AlertDialog.Builder(activity)
        val stringBuilder = StringBuilder()
        var br: BufferedReader? = null
        builder.setTitle(R.string.about_credits_dialog_title)
        builder.setView(binding.root)
        builder.setPositiveButton(R.string.dialog_action_close, null)
        val creditsText = binding.creditsPlaceholder
        try {
            br = BufferedReader(
                    InputStreamReader(activity.assets.open("credits.txt")))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                stringBuilder.append(line)
                stringBuilder.append('\n')
            }
        } catch (e: IOException) {
            Utils.sendLog(LogLevel.ERROR,
                    "Exception in reading credits file: $e")
        } finally {
            Utils.closeStream(br)
        }
        creditsText.text = stringBuilder.toString()
        LinkifyCompat.addLinks(creditsText, Linkify.WEB_URLS)
        return builder.create()
    }
}