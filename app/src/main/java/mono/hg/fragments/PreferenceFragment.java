package mono.hg.fragments;

import android.Manifest;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import mono.hg.R;
import mono.hg.SettingsActivity;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.utils.ViewUtils;

public class PreferenceFragment extends PreferenceFragmentCompat {

    private static final int PERMISSION_STORAGE_CODE = 4200;
    private boolean isRestore = false;

    @Override public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_customization);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ActionBar actionBar = ((SettingsActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_activity_settings);
        }

        ListPreference appTheme = (ListPreference) findPreference("app_theme");
        final ListPreference iconList = (ListPreference) findPreference("icon_pack");

        // Adaptive icon is not available before Android O/API 26.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            PreferenceCategory appListPreference = (PreferenceCategory) findPreference(
                    "icon_prefs");
            Preference adaptiveShadePreference = findPreference("adaptive_shade_switch");
            appListPreference.removePreference(adaptiveShadePreference);
        }

        // Window bar hiding works only reliably in KitKat and above.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            PreferenceCategory desktopPreference = (PreferenceCategory) findPreference(
                    "desktop");
            Preference windowBarPreference = findPreference("windowbar_mode");
            desktopPreference.removePreference(windowBarPreference);
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

        CharSequence[] finalEntries = entries.toArray(new CharSequence[0]);
        CharSequence[] finalEntryValues = entryValues.toArray(new CharSequence[0]);

        list.setEntries(finalEntries);
        list.setEntryValues(finalEntryValues);
    }

    private void addVersionCounterListener() {
        final Preference versionMenu = findPreference("version_key");

        if (!PreferenceHelper.getPreference().getBoolean("is_grandma", false)) {
            versionMenu.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                int counter = 9;
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
                        PreferenceHelper.getEditor().putBoolean("is_grandma", true).apply();
                        versionMenu.setTitle(R.string.version_key_name);
                    } else if (counter == 1) {
                        if (counterToast != null) {
                            counterToast.cancel();
                        }

                        counterToast = Toast.makeText(getActivity(), R.string.version_key_toast,
                                Toast.LENGTH_SHORT);
                        counterToast.show();

                        counter--;
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
                ViewUtils.replaceFragment((SettingsActivity) getActivity(), new HiddenAppsFragment(), "hidden_apps");
                return false;
            }
        });

        backupMenu.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                isRestore = false;
                return hasStoragePermission();
            }
        });

        restoreMenu.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                isRestore = true;
                return hasStoragePermission();
            }
        });
    }

    // Used to check for storage permission.
    // Throws true when API is less than M.
    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_STORAGE_CODE);
        } else {
            openBackupRestore(isRestore);
            return true;
        }
        return false;
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
            @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_STORAGE_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openBackupRestore(isRestore);
                }
                break;
            default:
                // No-op.
                break;
        }
    }

    /**
     * Opens the backup & restore fragment.
     *
     * @param isRestore Are we calling the fragment to restore a backup?
     */
    private void openBackupRestore(boolean isRestore) {
        BackupRestoreFragment backupRestoreFragment = new BackupRestoreFragment();
        Bundle fragmentBundle = new Bundle();
        fragmentBundle.putBoolean("isRestore", isRestore);
        backupRestoreFragment.setArguments(fragmentBundle);
        ViewUtils.replaceFragment((SettingsActivity) getActivity(), backupRestoreFragment, "backup_restore");
    }

}
