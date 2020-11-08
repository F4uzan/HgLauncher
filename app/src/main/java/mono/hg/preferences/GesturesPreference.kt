package mono.hg.preferences

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.View
import androidx.annotation.Keep
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            with(preference as ListPreference) {
                when (newValue as String) {
                    "app" -> {
                        // Create the Bundle to pass to AppSelectionPreferenceDialog.
                        val appListBundle = Bundle()
                        appListBundle.putString("key", key)
                        appListBundle.putCharSequenceArray("entries", appListEntries)
                        appListBundle.putCharSequenceArray("entryValues", appListEntryValues)

                        // Call and create AppSelectionPreferenceDialog.
                        val appList = AppSelectionPreferenceDialog()
                        appList.setTargetFragment(this@GesturesPreference, APPLICATION_DIALOG_CODE)
                        appList.arguments = appListBundle
                        appList.show(requireActivity().supportFragmentManager, "AppSelectionDialog")
                    }
                    else -> {
                        value = newValue.toString()
                        summary = entry
                    }
                }
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

        // Load the app list required for the 'launch app' menu.
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            getAppList
        }

        // We can safely iterate through all the preferences and assume they're ListPreference
        // because GesturesPreference has nothing else aside from that.
        for (i in 0 until prefCount) {
            prefScreen.getPreference(i).apply {
                setNestedListSummary(this as ListPreference)
                this.onPreferenceChangeListener = NestingListListener
            }
        }

        setGestureHandlerList(findPreference("gesture_handler"))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == APPLICATION_DIALOG_CODE && data != null) {
            if (resultCode == Activity.RESULT_CANCELED) {
                data.getStringExtra("key")?.apply {
                    findPreference<ListPreference>(this)?.let {
                        it.setSummary(R.string.gesture_action_default)
                        it.value = getString(R.string.gesture_action_default_value)
                    }
                }
            } else if (resultCode == Activity.RESULT_OK) {
                val key = data.getStringExtra("key")
                val app = data.getStringExtra("app")?.let {
                    AppUtils.getPackageLabel(requireActivity().packageManager, it)
                }

                key?.let { findPreference<ListPreference>(it)?.summary = app }
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

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.Default) {
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
            }

            list?.apply {
                this.entries = entries.toTypedArray()
                this.entryValues = entryValues.toTypedArray()
                list.summary = if (list.value == "none") {
                    getString(R.string.gesture_handler_default)
                } else {
                    AppUtils.getPackageLabel(requireActivity().packageManager, list.value)
                }
            }
        }
    }

    // Fetch apps and feed it into our list.
    private val getAppList: Unit
        get() {
            val manager = requireActivity().packageManager
            val entries: MutableList<String> = ArrayList()
            val entryValues: MutableList<String> = ArrayList()

            // Get default value.
            entries.add(getString(R.string.gesture_action_default))
            entryValues.add(getString(R.string.gesture_action_default_value))

            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            manager.queryIntentActivities(intent, 0)
                .sortedWith(ResolveInfo.DisplayNameComparator(manager)).forEach {
                    // Fetch apps and feed it into our list.
                    val appName = it.loadLabel(manager).toString()
                    val packageName =
                        it.activityInfo.packageName + "/" + it.activityInfo.name
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