package f4.hubby;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import f4.hubby.wrappers.AppCompatPreferenceActivity;

public class SettingsActivity extends AppCompatPreferenceActivity {

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Load appropriate theme before creating the activity.
        if (prefs.getBoolean("dark_theme", false) && prefs.getBoolean("dark_theme_black", true)) {
            setTheme(R.style.AppTheme_Dark);
        } else if (!prefs.getBoolean("dark_theme_black", true)) {
            setTheme(R.style.AppTheme_Gray);
        } else {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    // Set up action bar
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || CustomizePreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * It is used when the activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class CustomizePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_customization);
            setHasOptionsMenu(true);

            Preference hiddenAppsMenu = findPreference("hidden_apps_menu");
            hiddenAppsMenu.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), HiddenAppsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return false;
                }
            });

            final ListPreference iconList = (ListPreference) findPreference("icon_pack");
            setIconList(iconList);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        protected void setIconList(ListPreference list) {
            PackageManager manager = getActivity().getPackageManager();
            List<String> entries = new ArrayList<>();
            List<String> entryValues = new ArrayList<>();

            // Get default value.
            entries.add(getString(R.string.icon_pack_default));
            entryValues.add(getString(R.string.icon_pack_default_value));

            // Fetch all available icon pack.
            Intent intent = new Intent("org.adw.launcher.THEMES");
            List<ResolveInfo> infos = manager.queryIntentActivities (intent,
                    PackageManager.GET_META_DATA);
            for (ResolveInfo info : infos) {
                ActivityInfo activityInfo = info.activityInfo;
                String packageName = activityInfo.packageName;
                String appName = activityInfo.loadLabel(manager).toString();
                entries.add(appName);
                entryValues.add(packageName);
            }

            CharSequence[] finalEntries = entries.toArray(new CharSequence[entries.size()]);
            CharSequence[] finalEntryValues = entryValues.toArray(new CharSequence[entryValues.size()]);

            list.setEntries(finalEntries);
            list.setDefaultValue(getString(R.string.icon_pack_default_value));
            list.setEntryValues(finalEntryValues);
            list.setTitle(getString(R.string.icon_pack));
            list.setKey("icon_pack");
        }
    }
}
