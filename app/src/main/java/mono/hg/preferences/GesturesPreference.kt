package mono.hg.preferences

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.View
import androidx.annotation.Keep
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mono.hg.R
import mono.hg.utils.AppUtils
import mono.hg.wrappers.AppSelectionPreferenceDialog
import java.util.*

/**
 * Preferences for gestures and their actions.
 */
@Keep
class GesturesPreference : PreferenceFragmentCompat() {
    private lateinit var appListEntries: Array<CharSequence>
    private lateinit var appListEntryValues: Array<CharSequence>
    private val NestingListListener =
        Preference.OnPreferenceChangeListener { preference, newValue ->
            val list = preference as ListPreference
            when (newValue as String) {
                "handler" -> list.setSummary(R.string.gesture_action_handler)
                "widget" -> list.setSummary(R.string.gesture_action_widget)
                "status" -> list.setSummary(R.string.gesture_action_status)
                "panel" -> list.setSummary(R.string.gesture_action_panel)
                "list" -> list.setSummary(R.string.gesture_action_list)
                "app" -> {
                    // Create the Bundle to pass to AppSelectionPreferenceDialog.
                    val appListBundle = Bundle()
                    appListBundle.putString("key", list.key)
                    appListBundle.putCharSequenceArray("entries", appListEntries)
                    appListBundle.putCharSequenceArray("entryValues", appListEntryValues)

                    // Call and create AppSelectionPreferenceDialog.
                    val appList = AppSelectionPreferenceDialog()
                    appList.setTargetFragment(this@GesturesPreference, APPLICATION_DIALOG_CODE)
                    appList.arguments = appListBundle
                    appList.show(requireActivity().supportFragmentManager, "AppSelectionDialog")
                }
                "none" -> list.setSummary(R.string.gesture_action_default)
                else -> list.setSummary(R.string.gesture_action_default)
            }
            true
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_gestures, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefScreen: PreferenceScreen = preferenceScreen
        val prefCount: Int = prefScreen.preferenceCount

        appList

        // We can safely iterate through all the preferences and assume they're ListPreference
        // because GesturesPreference has nothing else aside from that.
        for (i in 0 until prefCount) {
            val pref = prefScreen.getPreference(i).apply {
                setNestedListSummary(this as ListPreference)
                this.onPreferenceChangeListener = NestingListListener
            }
        }

        setGestureHandlerList(findPreference("gesture_handler"))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == APPLICATION_DIALOG_CODE && data != null) {
            if (resultCode == Activity.RESULT_CANCELED) {
                val key = data.getStringExtra("key")
                if (key !== null) {
                    val preference = findPreference<ListPreference>(key).let {
                        it?.setSummary(R.string.gesture_action_default)
                        it?.value = getString(R.string.gesture_action_default_value)
                    }
                }
            } else if (resultCode == Activity.RESULT_OK) {
                val key = data.getStringExtra("key")
                val app = data.getStringExtra("app")?.let {
                    AppUtils.getPackageLabel(
                        requireActivity().packageManager,
                        it
                    )
                }

                if (key != null) {
                    findPreference<ListPreference>(key)?.summary = app
                }
            }
        }
    }

    private fun setGestureHandlerList(list: ListPreference?) {
        val manager = requireActivity().packageManager
        val entries: MutableList<String> = ArrayList()
        val entryValues: MutableList<String> = ArrayList()

        // Get default value.
        entries.add(getString(R.string.gesture_handler_default))
        entryValues.add(getString(R.string.gesture_handler_default_value))

        // Fetch all available icon pack.
        val intent = Intent("mono.hg.GESTURE_HANDLER")

        CoroutineScope(Dispatchers.Default).launch {
            manager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER)
                .forEach {
                    with(it.activityInfo) {
                        val className = this.name
                        val packageName = this.packageName
                        val componentName = "$packageName/$className"
                        val appName = loadLabel(manager).toString()
                        entries.add(appName)
                        entryValues.add(componentName)
                    }
                }

            list?.entries = entries.toTypedArray()
            list?.entryValues = entryValues.toTypedArray()
        }
    }

    // Fetch apps and feed it into our list.
    private val appList: Unit
        get() {
            val manager = requireActivity().packageManager
            val entries: MutableList<String> = ArrayList()
            val entryValues: MutableList<String> = ArrayList()

            // Get default value.
            entries.add(getString(R.string.gesture_action_default))
            entryValues.add(getString(R.string.gesture_action_default_value))

            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            val availableActivities = manager.queryIntentActivities(intent, 0)
            Collections.sort(availableActivities, ResolveInfo.DisplayNameComparator(manager))

            // Fetch apps and feed it into our list.
            availableActivities.forEach {
                val appName = it.loadLabel(manager).toString()
                val packageName = it.activityInfo.packageName + "/" + it.activityInfo.name
                entries.add(appName)
                entryValues.add(packageName)
            }

            appListEntries = entries.toTypedArray()
            appListEntryValues = entryValues.toTypedArray()
        }

    private fun setNestedListSummary(list: ListPreference) {
        if (list.value.contains("/")) {
            list.summary = AppUtils.getPackageLabel(
                requireActivity().packageManager,
                list.value
            )
        }
    }

    companion object {
        private const val APPLICATION_DIALOG_CODE = 300
    }
}