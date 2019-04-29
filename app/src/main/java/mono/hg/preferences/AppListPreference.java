package mono.hg.preferences;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.List;

import mono.hg.R;
import mono.hg.utils.Utils;

public class AppListPreference extends PreferenceFragmentCompat {
    @Override public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_app_list, rootKey);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListPreference iconList = findPreference("icon_pack");

        setIconList(iconList);

        // Adaptive icon is not available before Android O/API 26.
        if (Utils.atLeastOreo()) {
            findPreference("adaptive_shade_switch").setVisible(true);
        }
    }

    private void setIconList(ListPreference list) {
        PackageManager manager = requireActivity().getPackageManager();
        List<String> entries = new ArrayList<>();
        List<String> entryValues = new ArrayList<>();

        // Get default value.
        entries.add(getString(R.string.icon_pack_default));
        entryValues.add(getString(R.string.icon_pack_default_value));

        // Fetch all available icon pack.
        Intent intent = new Intent("org.adw.launcher.THEMES");
        List<ResolveInfo> info = manager.queryIntentActivities(intent,
                PackageManager.GET_META_DATA);
        for (ResolveInfo resolveInfo : info) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            String packageName = activityInfo.packageName;
            String appName = activityInfo.loadLabel(manager).toString();
            entries.add(appName);
            entryValues.add(packageName);
        }

        CharSequence[] finalEntries = entries.toArray(new CharSequence[0]);
        CharSequence[] finalEntryValues = entryValues.toArray(new CharSequence[0]);

        list.setEntries(finalEntries);
        list.setEntryValues(finalEntryValues);
    }
}
