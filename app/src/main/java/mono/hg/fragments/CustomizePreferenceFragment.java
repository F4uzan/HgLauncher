package mono.hg.fragments;

import android.Manifest;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import mono.hg.R;
import mono.hg.SettingsActivity;
import mono.hg.helpers.PreferenceHelper;

public class CustomizePreferenceFragment extends com.fnp.materialpreferences.PreferenceFragment {

    @Override
    public int addPreferencesFromResource() {
        return R.xml.pref_customization;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        ListPreference appTheme = (ListPreference) findPreference("app_theme");
        final ListPreference iconList = (ListPreference) findPreference("icon_pack");

        // Adaptive icon is not available before Android O/API 26.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            PreferenceCategory appListPreference = (PreferenceCategory) findPreference("icon_prefs");
            Preference adaptiveShadePreference = findPreference("adaptive_shade_switch");
            appListPreference.removePreference(adaptiveShadePreference);
        }

        setIconList(iconList);

        appTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ((SettingsActivity) getActivity()).restartActivity();
                return true;
            }
        });

        addVersionCounterListener();
        addFragmentListener();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // Let the activity handle the back press.
            getActivity().onBackPressed();
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
        List<ResolveInfo> info = manager.queryIntentActivities(intent,
                PackageManager.GET_META_DATA);
        for (ResolveInfo resolveInfo : info) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            String packageName = activityInfo.packageName;
            String appName = activityInfo.loadLabel(manager).toString();
            entries.add(appName);
            entryValues.add(packageName);
        }

        CharSequence[] finalEntries = entries.toArray(new CharSequence[entries.size()]);
        CharSequence[] finalEntryValues = entryValues.toArray(new CharSequence[entryValues.size()]);

        list.setEntries(finalEntries);
        list.setEntryValues(finalEntryValues);
    }

    private void addVersionCounterListener() {
        PreferenceHelper.initPreference(getActivity());
        final Preference versionMenu = findPreference("version_key");

        if (!PreferenceHelper.getPreference().getBoolean("is_grandma", false)) {
            versionMenu.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                int counter = 9;
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                        getActivity()).edit();
                Toast counterToast;

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (counter > 1) {
                        if (counterToast != null) {
                            counterToast.cancel();
                        }

                        if (counter < 8) {
                            counterToast = Toast.makeText(getActivity(),
                                    String.format(getString(R.string.version_key_toast_plural),
                                            counter), Toast.LENGTH_SHORT);
                            counterToast.show();
                        }

                        counter--;
                    } else if (counter == 0) {
                        editor.putBoolean("is_grandma", true).apply();
                        versionMenu.setTitle(R.string.version_key_name);
                    } else if (counter == 1) {
                        if (counterToast != null) {
                            counterToast.cancel();
                        }

                        counterToast = Toast.makeText(getActivity(), R.string.version_key_toast,
                                Toast.LENGTH_SHORT);
                        counterToast.show();
                    }
                    return false;
                }
            });
        } else {
            versionMenu.setTitle(R.string.version_key_name);
        }
    }

    private void addFragmentListener() {
        final Preference librariesDialogue = findPreference("about_libraries");
        Preference restoreMenu = findPreference("restore");
        Preference hiddenAppsMenu = findPreference("hidden_apps_menu");
        final Preference backupMenu = findPreference("backup");

        librariesDialogue.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                DialogFragment librariesInfo = new LibraryInfoFragment();
                librariesInfo.show(getActivity().getFragmentManager(), "LibrariesInfo");
                return false;
            }
        });

        hiddenAppsMenu.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                replaceFragment(new HiddenAppsFragment(), "HiddenApps");
                return false;
            }
        });

        backupMenu.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (hasStoragePermission()) {
                    BackupRestoreFragment backupRestoreFragment = new BackupRestoreFragment();
                    Bundle fragmentBundle = new Bundle();
                    fragmentBundle.putBoolean("isRestore", false);
                    backupRestoreFragment.setArguments(fragmentBundle);
                    replaceFragment(backupRestoreFragment, "BackupRestore");
                    return false;
                } else {
                    return false;
                }
            }
        });

        restoreMenu.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (hasStoragePermission()) {
                    BackupRestoreFragment backupRestoreFragment = new BackupRestoreFragment();
                    Bundle fragmentBundle = new Bundle();
                    fragmentBundle.putBoolean("isRestore", true);
                    backupRestoreFragment.setArguments(fragmentBundle);
                    replaceFragment(backupRestoreFragment, "BackupRestore");
                    return false;
                } else {
                    return false;
                }
            }
        });
    }

    // Replace the view with a fragment.
    private void replaceFragment(Fragment fragment, String tag) {
        getActivity().getFragmentManager().beginTransaction()
                     .replace(android.R.id.content, fragment, tag)
                     .addToBackStack(null)
                     .commit();
    }

    // Used to check for storage permission.
    // Throws true when API is less than M.
    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getContext().checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4200);
            } else {
                return true;
            }
        } else {
            return true;
        }
        return false;
    }
}