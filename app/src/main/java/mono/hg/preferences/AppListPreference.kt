package mono.hg.preferences

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.annotation.Keep
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import mono.hg.R
import mono.hg.SettingsActivity
import mono.hg.helpers.PreferenceHelper
import mono.hg.utils.Utils
import java.util.*

/**
 * Preferences for app lists and the view surrounding it.
 */
@Keep
class AppListPreference : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_app_list, rootKey)
    }

    override fun onResume() {
        super.onResume()

        // Update the action bar title.
        (requireActivity() as SettingsActivity).supportActionBar?.setTitle(R.string.pref_header_list)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findPreference<ListPreference>("icon_pack")?.apply {
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                PreferenceHelper.update("require_refresh", true)
                true
            }
            setIconList(this)
        }

        // Adaptive icon is not available before Android O/API 26.
        findPreference<Preference>("adaptive_shade_switch")?.isVisible = Utils.atLeastOreo()
    }

    private fun setIconList(list: ListPreference?) {
        val manager = requireActivity().packageManager
        val entries: MutableList<String> = ArrayList()
        val entryValues: MutableList<String> = ArrayList()

        // Get default value.
        entries.add(getString(R.string.icon_pack_default))
        entryValues.add(getString(R.string.icon_pack_default_value))

        // Fetch all available icon pack.
        val intent = Intent("org.adw.launcher.THEMES")
        manager.queryIntentActivities(intent, PackageManager.GET_META_DATA).forEach {
            with(it.activityInfo) {
                val packageName = this.packageName
                val appName = loadLabel(manager).toString()
                entries.add(appName)
                entryValues.add(packageName)
            }
        }

        list?.entries = entries.toTypedArray()
        list?.entryValues = entryValues.toTypedArray()
    }
}