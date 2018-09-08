package mono.hg;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import mono.hg.fragments.BackupRestoreFragment;
import mono.hg.fragments.CustomizePreferenceFragment;
import mono.hg.fragments.HiddenAppsFragment;
import mono.hg.wrappers.BackHandledFragment;

public class SettingsActivity extends com.fnp.materialpreferences.PreferenceActivity implements BackHandledFragment.BackHandlerInterface {
    private BackHandledFragment selectedFragment;

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
        String app_theme = prefs.getString("app_theme", "light");
        switch (app_theme) {
            case "light":
                setTheme(R.style.SettingTheme);
                break;
            case "dark":
                setTheme(R.style.SettingTheme_Gray);
                break;
            case "black":
                setTheme(R.style.SettingTheme_Dark);
                break;
        }

        super.onCreate(savedInstanceState);
        setTitle(R.string.title_activity_settings);
        setPreferenceFragment(new CustomizePreferenceFragment());
    }

    @Override
    public void onBackPressed() {
        if (selectedFragment == null || !selectedFragment.onBackPressed()) {
            // Selected fragment did not consume the back press event.
            super.onBackPressed();
        }
    }

    @Override
    public void setSelectedFragment(BackHandledFragment selectedFragment) {
        this.selectedFragment = selectedFragment;
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
                || CustomizePreferenceFragment.class.getName().equals(fragmentName)
                || HiddenAppsFragment.class.getName().equals(fragmentName)
                || BackupRestoreFragment.class.getName().equals(fragmentName);
    }

    // Called when the activity needs to be restarted (i.e when a theme change occurs).
    // Allows for smooth transition between recreation.
    public void restartActivity() {
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        startActivity(intent);
    }
}
