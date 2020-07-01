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
import mono.hg.utils.Utils
import java.util.*

@Keep
class AppListPreference : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_app_list, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val iconList = findPreference<ListPreference>("icon_pack")
        setIconList(iconList)

        // Adaptive icon is not available before Android O/API 26.
        if (Utils.atLeastOreo()) {
            findPreference<Preference>("adaptive_shade_switch")!!.isVisible = true
        }
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
        val info = manager.queryIntentActivities(intent,
                PackageManager.GET_META_DATA)
        for (resolveInfo in info) {
            val activityInfo = resolveInfo.activityInfo
            val packageName = activityInfo.packageName
            val appName = activityInfo.loadLabel(manager).toString()
            entries.add(appName)
            entryValues.add(packageName)
        }
        val finalEntries = entries.toTypedArray<CharSequence>()
        val finalEntryValues = entryValues.toTypedArray<CharSequence>()
        list!!.entries = finalEntries
        list.entryValues = finalEntryValues
    }
}