package mono.hg.preferences

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.preference.PreferenceFragmentCompat
import mono.hg.R
import mono.hg.SettingsActivity
import mono.hg.adapters.WebProviderAdapter
import mono.hg.databinding.FragmentWebProviderBinding
import mono.hg.databinding.LayoutWebProviderEditDialogBinding
import mono.hg.helpers.PreferenceHelper
import mono.hg.models.WebSearchProvider
import mono.hg.utils.Utils
import java.util.*


/**
 * PreferenceFragment that hosts the list of available and editable web search provider.
 */
@Keep
class WebSearchProviderPreference : PreferenceFragmentCompat() {
    private var binding: FragmentWebProviderBinding? = null
    private var providerList = ArrayList<WebSearchProvider>()
    private var providerAdapter: WebProviderAdapter? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // No-op.
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWebProviderBinding.inflate(inflater, container, false)
        return binding !!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        val providerListView = binding !!.webProviderList
        providerAdapter = context?.let { WebProviderAdapter(providerList, it) }

        providerListView.adapter = providerAdapter
        providerListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
            val name = providerList[i].name
            val url = providerList[i].url
            makeEditMenu(name, url, true, i)
        }

        // Add defaults if we don't have any provider.
        with(PreferenceHelper.providerList) {
            if (this.isEmpty()) {
                Utils.setDefaultProviders(requireActivity().resources, providerList)
            } else {
                this.forEach { providerList.add(WebSearchProvider(it.key, it.value)) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // We have been sent back. Set the action bar title accordingly.
        val actionBar = (requireActivity() as SettingsActivity).supportActionBar
        actionBar?.setTitle(R.string.pref_header_web)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        with(menu) {
            clear()
            add(1, 0, 100, getString(R.string.action_web_provider_add))
            add(1, 1, 100, getString(R.string.action_web_provider_reset))
            getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            getItem(1).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            super.onCreateOptionsMenu(this, inflater)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.home -> {
                requireActivity().onBackPressed()
                true
            }
            0 -> {
                makeEditMenu()
                true
            }
            1 -> {
                providerList.clear()
                PreferenceHelper.updateProvider(
                    Utils.setDefaultProviders(requireActivity().resources, providerList)
                )
                providerAdapter?.notifyDataSetChanged()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun makeEditMenu(
        name: String? = "",
        url: String? = "",
        isEditing: Boolean = false,
        position: Int = 0
    ) {
        val binding = LayoutWebProviderEditDialogBinding.inflate(requireActivity().layoutInflater)

        val nameField = binding.providerEditName.apply {
            ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(PreferenceHelper.accent))
            setText(name)
        }

        val urlField = binding.providerEditUrl.apply {
            ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(PreferenceHelper.accent))
            setText(url)
        }

        val title: String = if (isEditing) {
            getString(R.string.dialog_title_edit_provider)
        } else {
            getString(R.string.dialog_title_add_provider)
        }

        with(AlertDialog.Builder(requireActivity())) {
            setView(binding.root)
            setTitle(title)
            setNeutralButton(R.string.action_web_provider_remove) { _, _ ->
                providerList.removeAt(position)
                providerAdapter?.notifyDataSetChanged()
                PreferenceHelper.updateProvider(providerList)
            }
            setNegativeButton(R.string.dialog_cancel, null)
            setPositiveButton(R.string.dialog_ok) { _, _ ->
                val currentName = nameField.text.toString().replace("|", "").trim()
                val currentUrl = urlField.text.toString().trim()

                // Strip out %s as it triggers the matcher.
                // We won't use this URL, but we still need to check if the URL overall is valid.
                val safeUrl = currentUrl.replace("%s".toRegex(), "+s")
                if (! Patterns.WEB_URL.matcher(safeUrl).matches()) {
                    // This is an invalid URL, cancel.
                    Toast.makeText(
                        requireContext(), R.string.err_invalid_url,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                if ("none" != PreferenceHelper.getProvider(currentName) &&
                    currentUrl != PreferenceHelper.providerList[currentName]) {
                    // We already have that provider.
                    Toast.makeText(
                        requireContext(), R.string.err_provider_exists,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                if (isEditing) {
                    providerList[position] = WebSearchProvider(currentName, currentUrl)
                } else {
                    providerList.add(WebSearchProvider(currentName, currentUrl))
                }
                PreferenceHelper.updateProvider(providerList)
                providerAdapter?.notifyDataSetChanged()
            }

            create().apply {
                show()
                with(PreferenceHelper.darkAccent) {
                    getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(this)
                    getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(this)
                    getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(this)
                }
            }
        }
    }
}