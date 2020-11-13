package mono.hg.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mono.hg.R
import mono.hg.SettingsActivity
import mono.hg.databinding.FragmentCreditsDialogBinding
import mono.hg.databinding.FragmentCreditsDisplayBinding
import mono.hg.helpers.PreferenceHelper
import mono.hg.utils.compatHide
import mono.hg.utils.compatShow
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * A DialogFragment that reads from assets/credits.txt, returning its content to a TextView.
 */
class CreditsDialogFragment : DialogFragment() {
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragmentCreditsDialogBinding.inflate(requireActivity().layoutInflater)

        with(AlertDialog.Builder(requireActivity())) {
            setTitle(R.string.about_credits_dialog_title)
            setView(binding.root)
            setPositiveButton(R.string.dialog_action_close, null)

            val adapter = CreditsDisplayAdapter(this@CreditsDialogFragment)
            val viewPager = binding.creditsPager
            viewPager.adapter = adapter

            val tabLayout = binding.creditsTab
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                when (position) {
                    0 -> tab.icon =
                        AppCompatResources.getDrawable(requireContext(), R.drawable.ic_credit_license)
                    1 -> tab.icon =
                        AppCompatResources.getDrawable(requireContext(), R.drawable.ic_credit_author)
                    2 -> tab.icon =
                        AppCompatResources.getDrawable(requireContext(), R.drawable.ic_credit_translator)
                }
            }.attach()

            tabLayout.tabIconTint = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_selected),
                    intArrayOf()
                ), intArrayOf(
                    PreferenceHelper.accent,
                    Color.LTGRAY
                )
            )

            tabLayout.setSelectedTabIndicatorColor(PreferenceHelper.darkAccent)

            create().apply {
                show()
                getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(PreferenceHelper.darkAccent)
                getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(PreferenceHelper.darkAccent)
                return this
            }
        }
    }
}

class CreditsDisplayAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CreditsDisplayFragment("license.txt")
            1 -> CreditsDisplayFragment("authors.txt")
            2 -> CreditsDisplayFragment("translators.txt")
            else -> CreditsDisplayFragment("placeholder.txt") // This shouldn't happen.
        }
    }
}

class CreditsDisplayFragment(val file: String) : Fragment() {
    private var binding: FragmentCreditsDisplayBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCreditsDisplayBinding.inflate(inflater, container, false)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.creditsPlaceholder?.apply {
            highlightColor = PreferenceHelper.darkAccent
            setLinkTextColor(PreferenceHelper.accent)
            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                text = withContext(Dispatchers.IO) {
                    HtmlCompat.fromHtml(readCredits(), HtmlCompat.FROM_HTML_MODE_COMPACT)
                }
            }
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private suspend fun readCredits(): String {
        withContext(Dispatchers.Main) {
            (requireActivity() as SettingsActivity).progressBar.compatShow()
        }

        val stringBuilder = StringBuilder()

        BufferedReader(InputStreamReader(requireActivity().assets.open(file))).use {
            var currentLine: String?
            while (it.readLine().also { line -> currentLine = line } != null) {
                stringBuilder.append(currentLine).append('\n')
            }
        }

        withContext(Dispatchers.Main) {
            (requireActivity() as SettingsActivity).progressBar.compatHide()
        }
        return stringBuilder.toString().replace("\n", "<br/>")
    }
}