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
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.preference.PreferenceFragmentCompat
import mono.hg.R
import mono.hg.SettingsActivity
import mono.hg.adapters.WebProviderAdapter
import mono.hg.databinding.FragmentWebProviderBinding
import mono.hg.databinding.LayoutWebProviderEditDialogBinding
import mono.hg.helpers.PreferenceHelper
import mono.hg.models.WebSearchProvider
import java.util.*


/**
 * PreferenceFragment that hosts the list of available and editable web search provider.
 */
@Keep
class WebSearchProviderPreference : PreferenceFragmentCompat() {
    private var binding: FragmentWebProviderBinding? = null
    private val providerList = ArrayList<WebSearchProvider>()
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
        providerListView.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { _, _, i, _ ->
                val name = providerList[i].name
                val url = providerList[i].url
                val popupMenu = PopupMenu(requireActivity(), providerListView.getChildAt(i))
                popupMenu.menuInflater.inflate(R.menu.menu_web_provider, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_web_provider_remove -> {
                            providerList.removeAt(i)
                            providerAdapter !!.notifyDataSetChanged()
                            PreferenceHelper.updateProvider(providerList)
                            true
                        }
                        R.id.action_web_provider_edit -> {
                            makeEditMenu(name, url, true, i)
                            true
                        }
                        else -> true
                    }
                }
                popupMenu.show()
                true
            }

        PreferenceHelper.providerList.forEach {
            providerList.add(
                WebSearchProvider(
                    it.key,
                    it.value
                )
            )
        }

        // Add defaults if we don't have any provider.
        if (providerList.isEmpty()) {
            addDefaults()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // We have been sent back. Set the action bar title accordingly.
        val actionBar = (requireActivity() as SettingsActivity).supportActionBar
        actionBar?.setTitle(R.string.pref_header_web)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        menu.add(1, 0, 100, getString(R.string.action_web_provider_add))
        menu.add(1, 1, 100, getString(R.string.action_web_provider_reset))
        menu.getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.getItem(1).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        super.onCreateOptionsMenu(menu, inflater)
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
                addDefaults()
                PreferenceHelper.updateProvider(providerList)
                providerAdapter !!.notifyDataSetChanged()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun addDefaults() {
        val defaultProvider = resources.getStringArray(
            R.array.pref_search_provider_title
        )
        val defaultProviderId = resources.getStringArray(
            R.array.pref_search_provider_values
        )

        // defaultProvider will always be the same size as defaultProviderUrl.
        // However, we start at 1 to ignore the 'Always ask' option.
        for (i in 1 until defaultProvider.size) {
            providerList.add(
                WebSearchProvider(
                    defaultProvider[i],
                    PreferenceHelper.getDefaultProvider(defaultProviderId[i]),
                    defaultProvider[i]
                )
            )
        }
    }

    private fun makeEditMenu(
        name: String? = "",
        url: String? = "",
        isEditing: Boolean = false,
        position: Int = 0
    ) {
        val binding = LayoutWebProviderEditDialogBinding.inflate(requireActivity().layoutInflater)
        val builder = AlertDialog.Builder(requireActivity())
        val nameField = binding.providerEditName
        val urlField = binding.providerEditUrl

        ViewCompat.setBackgroundTintList(nameField, ColorStateList.valueOf(PreferenceHelper.accent))
        ViewCompat.setBackgroundTintList(urlField, ColorStateList.valueOf(PreferenceHelper.accent))

        if (name !!.isNotEmpty()) {
            nameField.setText(name)
        }
        if (url !!.isNotEmpty()) {
            urlField.setText(url)
        }
        val title: String = if (isEditing) {
            getString(R.string.dialog_title_edit_provider)
        } else {
            getString(R.string.dialog_title_add_provider)
        }
        builder.setView(binding.root)
        builder.setTitle(title)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { _, _ ->
                val name = nameField.text.toString().replace("\\|".toRegex(), "").trim { it <= ' ' }
                val url = urlField.text.toString().trim { it <= ' ' }

                // Strip out %s as it triggers the matcher.
                // We won't use this URL, but we still need to check if the URL overall is valid.
                val safeUrl = url.replace("%s".toRegex(), "+s")
                if (! Patterns.WEB_URL.matcher(safeUrl).matches()) {
                    // This is an invalid URL, cancel.
                    Toast.makeText(
                        requireContext(), R.string.err_invalid_url,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@OnClickListener
                }
                if ("none" != PreferenceHelper.getProvider(name) && ! isEditing) {
                    // We already have that provider and/or we aren't editing.
                    Toast.makeText(
                        requireContext(), R.string.err_provider_exists,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@OnClickListener
                }
                if (isEditing) {
                    providerList[position] = WebSearchProvider(name, url)
                } else {
                    providerList.add(WebSearchProvider(name, url))
                }
                PreferenceHelper.updateProvider(providerList)
                providerAdapter !!.notifyDataSetChanged()
            })

        val themedDialog = builder.create()
        themedDialog.show()

        themedDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            .setTextColor(PreferenceHelper.darkAccent)
        themedDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            .setTextColor(PreferenceHelper.darkAccent)
    }
}