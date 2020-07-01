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
import mono.hg.R
import mono.hg.utils.AppUtils
import mono.hg.wrappers.AppSelectionPreferenceDialog
import java.util.*

@Keep
class GesturesPreference : PreferenceFragmentCompat() {
    private lateinit var appListEntries: Array<CharSequence>
    private lateinit var appListEntryValues: Array<CharSequence>
    private val NestingListListener = Preference.OnPreferenceChangeListener { preference, newValue ->
        val list = preference as ListPreference
        val value = newValue as String
        when (value) {
            "handler" -> list.setSummary(R.string.gesture_action_handler)
            "widget" -> list.setSummary(R.string.gesture_action_widget)
            "status" -> list.setSummary(R.string.gesture_action_status)
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
                appList.show(requireFragmentManager(), "AppSelectionDialog")
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
        appList
        val gestureLeftList = findPreference<ListPreference>("gesture_left")
        val gestureRightList = findPreference<ListPreference>("gesture_right")
        val gestureDownList = findPreference<ListPreference>("gesture_down")
        val gestureUpList = findPreference<ListPreference>("gesture_up")
        val gestureTapList = findPreference<ListPreference>("gesture_single_tap")
        val gestureDoubleTapList = findPreference<ListPreference>("gesture_double_tap")
        val gesturePinchList = findPreference<ListPreference>("gesture_pinch")
        val gestureHandlerList = findPreference<ListPreference>("gesture_handler")
        setNestedListSummary(gestureLeftList)
        setNestedListSummary(gestureRightList)
        setNestedListSummary(gestureDownList)
        setNestedListSummary(gestureUpList)
        setNestedListSummary(gestureTapList)
        setNestedListSummary(gestureDoubleTapList)
        setNestedListSummary(gesturePinchList)
        setGestureHandlerList(gestureHandlerList)
        gestureLeftList!!.onPreferenceChangeListener = NestingListListener
        gestureRightList!!.onPreferenceChangeListener = NestingListListener
        gestureDownList!!.onPreferenceChangeListener = NestingListListener
        gestureUpList!!.onPreferenceChangeListener = NestingListListener
        gestureDoubleTapList!!.onPreferenceChangeListener = NestingListListener
        gestureTapList!!.onPreferenceChangeListener = NestingListListener
        gesturePinchList!!.onPreferenceChangeListener = NestingListListener
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == APPLICATION_DIALOG_CODE && data != null) {
            if (resultCode == Activity.RESULT_CANCELED) {
                val key = data.getStringExtra("key")
                val preference = findPreference<ListPreference>(key)
                preference?.setSummary(R.string.gesture_action_default)
                preference?.value = getString(R.string.gesture_action_default_value)
            } else if (resultCode == Activity.RESULT_OK) {
                val key = data.getStringExtra("key")
                val app = AppUtils.getPackageLabel(requireActivity().packageManager,
                        data.getStringExtra("app"))
                val preference = findPreference<ListPreference>(key)
                preference!!.summary = app
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
        val info = manager.queryIntentActivities(intent,
                PackageManager.GET_RESOLVED_FILTER)
        for (resolveInfo in info) {
            val activityInfo = resolveInfo.activityInfo
            val className = activityInfo.name
            val packageName = activityInfo.packageName
            val componentName = "$packageName/$className"
            val appName = activityInfo.loadLabel(manager).toString()
            entries.add(appName)
            entryValues.add(componentName)
        }
        val finalEntries = entries.toTypedArray<CharSequence>()
        val finalEntryValues = entryValues.toTypedArray<CharSequence>()
        list!!.entries = finalEntries
        list.entryValues = finalEntryValues
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
            for (resolveInfo in availableActivities) {
                val appName = resolveInfo.loadLabel(manager).toString()
                val packageName = resolveInfo.activityInfo.packageName + "/" + resolveInfo.activityInfo.name
                entries.add(appName)
                entryValues.add(packageName)
            }
            appListEntries = entries.toTypedArray()
            appListEntryValues = entryValues.toTypedArray()
        }

    private fun setNestedListSummary(list: ListPreference?) {
        if (list!!.value.contains("/")) {
            list.summary = AppUtils.getPackageLabel(requireActivity().packageManager,
                    list.value)
        }
    }

    companion object {
        private const val APPLICATION_DIALOG_CODE = 300
    }
}