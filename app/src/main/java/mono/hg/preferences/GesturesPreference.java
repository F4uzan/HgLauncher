package mono.hg.preferences;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mono.hg.R;
import mono.hg.utils.AppUtils;
import mono.hg.wrappers.AppSelectionPreferenceDialog;

@Keep
public class GesturesPreference extends PreferenceFragmentCompat {
    private static final int APPLICATION_DIALOG_CODE = 300;
    private CharSequence[] appListEntries;
    private CharSequence[] appListEntryValues;

    private Preference.OnPreferenceChangeListener NestingListListener = new Preference.OnPreferenceChangeListener() {
        @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
            ListPreference list = (ListPreference) preference;
            String value = (String) newValue;

            // Workaround due to the use of setSummary in onCreate.
            switch (value) {
                case "handler":
                    list.setSummary(R.string.gesture_action_handler);
                    break;
                case "widget":
                    list.setSummary(R.string.gesture_action_widget);
                    break;
                case "status":
                    list.setSummary(R.string.gesture_action_status);
                    break;
                case "list":
                    list.setSummary(R.string.gesture_action_list);
                    break;
                case "app":
                    // Create the Bundle to pass to AppSelectionPreferenceDialog.
                    Bundle appListBundle = new Bundle();
                    appListBundle.putString("key", list.getKey());
                    appListBundle.putCharSequenceArray("entries", appListEntries);
                    appListBundle.putCharSequenceArray("entryValues", appListEntryValues);

                    // Call and create AppSelectionPreferenceDialog.
                    AppSelectionPreferenceDialog appList = new AppSelectionPreferenceDialog();
                    appList.setTargetFragment(GesturesPreference.this, APPLICATION_DIALOG_CODE);
                    appList.setArguments(appListBundle);
                    appList.show(requireFragmentManager(), "AppSelectionDialog");
                    break;
                case "none":
                default:
                    list.setSummary(R.string.gesture_action_default);
                    break;
            }
            return true;
        }
    };

    @Override public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_gestures, rootKey);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getAppList();

        ListPreference gestureLeftList = findPreference("gesture_left");
        ListPreference gestureRightList = findPreference("gesture_right");
        ListPreference gestureDownList = findPreference("gesture_down");
        ListPreference gestureUpList = findPreference("gesture_up");
        ListPreference gestureTapList = findPreference("gesture_single_tap");
        ListPreference gestureDoubleTapList = findPreference("gesture_double_tap");
        ListPreference gesturePinchList = findPreference("gesture_pinch");
        ListPreference gestureHandlerList = findPreference("gesture_handler");

        setNestedListSummary(gestureLeftList);
        setNestedListSummary(gestureRightList);
        setNestedListSummary(gestureDownList);
        setNestedListSummary(gestureUpList);
        setNestedListSummary(gestureTapList);
        setNestedListSummary(gestureDoubleTapList);
        setNestedListSummary(gesturePinchList);

        setGestureHandlerList(gestureHandlerList);
        gestureLeftList.setOnPreferenceChangeListener(NestingListListener);
        gestureRightList.setOnPreferenceChangeListener(NestingListListener);
        gestureDownList.setOnPreferenceChangeListener(NestingListListener);
        gestureUpList.setOnPreferenceChangeListener(NestingListListener);
        gestureDoubleTapList.setOnPreferenceChangeListener(NestingListListener);
        gestureTapList.setOnPreferenceChangeListener(NestingListListener);
        gesturePinchList.setOnPreferenceChangeListener(NestingListListener);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == APPLICATION_DIALOG_CODE && data != null) {
            if (resultCode == Activity.RESULT_CANCELED) {
                String key = data.getStringExtra("key");
                ListPreference preference = findPreference(key);
                preference.setSummary(R.string.gesture_action_default);
                preference.setValue(getString(R.string.gesture_action_default_value));
            } else if (resultCode == Activity.RESULT_OK) {
                String key = data.getStringExtra("key");
                String app = AppUtils.getPackageLabel(requireActivity().getPackageManager(),
                        data.getStringExtra("app"));
                ListPreference preference = findPreference(key);
                preference.setSummary(app);
            }
        }
    }

    private void setGestureHandlerList(ListPreference list) {
        PackageManager manager = requireActivity().getPackageManager();
        List<String> entries = new ArrayList<>();
        List<String> entryValues = new ArrayList<>();

        // Get default value.
        entries.add(getString(R.string.gesture_handler_default));
        entryValues.add(getString(R.string.gesture_handler_default_value));

        // Fetch all available icon pack.
        Intent intent = new Intent("mono.hg.GESTURE_HANDLER");
        List<ResolveInfo> info = manager.queryIntentActivities(intent,
                PackageManager.GET_RESOLVED_FILTER);
        for (ResolveInfo resolveInfo : info) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            String className = activityInfo.name;
            String packageName = activityInfo.packageName;
            String componentName = packageName + "/" + className;
            String appName = activityInfo.loadLabel(manager).toString();
            entries.add(appName);
            entryValues.add(componentName);
        }

        CharSequence[] finalEntries = entries.toArray(new CharSequence[0]);
        CharSequence[] finalEntryValues = entryValues.toArray(new CharSequence[0]);

        list.setEntries(finalEntries);
        list.setEntryValues(finalEntryValues);
    }

    private void getAppList() {
        PackageManager manager = requireActivity().getPackageManager();
        List<String> entries = new ArrayList<>();
        List<String> entryValues = new ArrayList<>();

        // Get default value.
        entries.add(getString(R.string.gesture_action_default));
        entryValues.add(getString(R.string.gesture_action_default_value));

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> availableActivities = manager.queryIntentActivities(intent, 0);

        Collections.sort(availableActivities, new ResolveInfo.DisplayNameComparator(manager));

        // Fetch apps and feed it into our list.
        for (ResolveInfo resolveInfo : availableActivities) {
            String appName = resolveInfo.loadLabel(manager).toString();
            String packageName = resolveInfo.activityInfo.packageName + "/" + resolveInfo.activityInfo.name;
            entries.add(appName);
            entryValues.add(packageName);
        }

        appListEntries = entries.toArray(new CharSequence[0]);
        appListEntryValues = entryValues.toArray(new CharSequence[0]);
    }

    private void setNestedListSummary(ListPreference list) {
        if (list.getValue().contains("/")) {
            list.setSummary(AppUtils.getPackageLabel(requireActivity().getPackageManager(),
                    list.getValue()));
        }
    }
}
