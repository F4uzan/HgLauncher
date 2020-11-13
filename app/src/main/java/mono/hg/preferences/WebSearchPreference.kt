package mono.hg.preferences

import android.os.Bundle
import androidx.annotation.Keep
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import mono.hg.R
import mono.hg.SettingsActivity
import mono.hg.helpers.PreferenceHelper
import java.util.*

/**
 * Preferences for web search settings. This preference does not host the web search list,
 * see [WebSearchProviderPreference] for that.
 */
@Keep
class WebSearchPreference : PreferenceFragmentCompat() {
    private var providerList: ListPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_web, rootKey)

        providerList = findPreference("search_provider")
        setProviderList(providerList)
    }

    override fun onResume() {
        super.onResume()

        // Set this here to make sure its values are updated every time we return to it.
        setProviderList(providerList)

        // Update the action bar title.
        (requireActivity() as SettingsActivity).supportActionBar?.setTitle(R.string.pref_header_web)
    }

    private fun setProviderList(list: ListPreference?) {
        val entries: MutableList<String?> = ArrayList()
        val entryValues: MutableList<String?> = ArrayList()

        entries.add(getString(R.string.search_provider_none))
        entryValues.add(getString(R.string.gesture_action_default_value))

        // We only need the key as the value is stored in PreferenceHelper's Map.
        PreferenceHelper.providerList.forEach {
            entries.add(it.key)
            entryValues.add(it.key)
        }

        list?.entries = entries.toTypedArray()
        list?.entryValues = entryValues.toTypedArray()
    }
}