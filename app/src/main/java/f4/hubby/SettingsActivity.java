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
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends com.fnp.materialpreferences.PreferenceActivity {

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
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Load appropriate theme before creating the activity.
        if (prefs.getBoolean("dark_theme", false) && prefs.getBoolean("dark_theme_black", true)) {
            setTheme(R.style.SettingTheme_Dark);
        } else if (!prefs.getBoolean("dark_theme", false) && !prefs.getBoolean("dark_theme_black", true)) {
            setTheme(R.style.SettingTheme);
        } else if (!prefs.getBoolean("dark_theme_black", true)) {
            setTheme(R.style.SettingTheme_Gray);
        }

        super.onCreate(savedInstanceState);
        setTitle(R.string.title_activity_settings);
        setPreferenceFragment(new CustomizePreferenceFragment());
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
    public static class CustomizePreferenceFragment extends
            com.fnp.materialpreferences.PreferenceFragment {

        @Override
        public int addPreferencesFromResource() {
            return R.xml.pref_customization;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            // HACK: Add padding in KitKat and below.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                ListView lv = getActivity().findViewById(android.R.id.list);
                ViewGroup parent = (ViewGroup) lv.getParent();
                parent.setPadding(getResources().getDimensionPixelOffset(R.dimen.uniform_panel_margin), 0,
                        getResources().getDimensionPixelOffset(R.dimen.uniform_panel_margin), 0);
            }

            final Preference versionMenu = findPreference("version_key");

            if (prefs.getBoolean("is_grandma", false)) {
                versionMenu.setEnabled(false);
                versionMenu.setTitle(R.string.version_key_name);
            }

            versionMenu.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                int counter = 9;
                SharedPreferences.Editor editor =  PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                Toast counterToast;

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (counter > 0) {
                        counter--;
                    } else if (counter == 0) {
                        editor.putBoolean("is_grandma", true).apply();
                        versionMenu.setEnabled(false);
                        versionMenu.setTitle(R.string.version_key_name);
                    }

                    if (counter < 7 && counter > 1) {
                        if (counterToast != null) {
                            counterToast.cancel();
                        }
                        counterToast = Toast.makeText(getActivity(),
                                String.format(getString(R.string.version_key_toast_plural), counter), Toast.LENGTH_SHORT);
                        counterToast.show();
                    } else if (counter == 1) {
                        if (counterToast != null) {
                            counterToast.cancel();
                        }
                        counterToast = Toast.makeText(getActivity(), R.string.version_key_toast, Toast.LENGTH_SHORT);
                        counterToast.show();
                    }
                    return false;
                }
            });

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
