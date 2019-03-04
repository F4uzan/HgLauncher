package mono.hg.fragments;

import android.Manifest;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import mono.hg.R;
import mono.hg.SettingsActivity;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.utils.Utils;
import mono.hg.utils.ViewUtils;

public class PreferenceFragment extends PreferenceFragmentCompat {

    private static final int PERMISSION_STORAGE_CODE = 4200;
    private CharSequence[] appListEntries;
    private CharSequence[] appListEntriesValue;
    private boolean isRestore = false;
    private ListPreference providerList;

    @Override public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_customization);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ActionBar actionBar = ((SettingsActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_activity_settings);
        }

        getAppList();

        ListPreference appTheme = (ListPreference) findPreference("app_theme");
        final ListPreference iconList = (ListPreference) findPreference("icon_pack");
        providerList = (ListPreference) findPreference("search_provider");
        ListPreference gestureLeftList = (ListPreference) findPreference("gesture_left");
        ListPreference gestureRightList = (ListPreference) findPreference("gesture_right");
        ListPreference gestureUpList = (ListPreference) findPreference("gesture_up");
        ListPreference gestureDoubleTapList = (ListPreference) findPreference("gesture_double_tap");

        // Adaptive icon is not available before Android O/API 26.
        if (Utils.atLeastOreo()) {
            findPreference("adaptive_shade_switch").setVisible(true);
        }

        // Window bar hiding works only reliably in KitKat and above.
        if (Utils.atLeastKitKat()) {
            findPreference("windowbar_mode").setVisible(true);
        }

        if (Utils.sdkIsBelow(19)) {
            findPreference("windowbar_status_switch").setVisible(true);
        }

        setIconList(iconList);
        setProviderList(providerList);
        setAppList(gestureLeftList);
        setAppList(gestureRightList);
        setAppList(gestureUpList);
        setAppList(gestureDoubleTapList);

        appTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ((SettingsActivity) requireActivity()).restartActivity();
                return true;
            }
        });

        addVersionCounterListener();
        addFragmentListener();
    }

    @Override public void onResume() {
        super.onResume();

        // Set this here to make sure its values are updated every time we return to it.
        setProviderList(providerList);
    }

    private void setProviderList(ListPreference list) {
        List<String> entries = new ArrayList<>();
        List<String> entryValues = new ArrayList<>();

        entries.add(getString(R.string.search_provider_none));
        entryValues.add(getString(R.string.gestures_default_value));

        // We only need the key as the value is stored in PreferenceHelper's Map.
        for (Map.Entry<String, String> provider : PreferenceHelper
                .getProviderList().entrySet()) {
            entries.add(provider.getKey());
            entryValues.add(provider.getKey());
        }

        CharSequence[] finalEntries = entries.toArray(new CharSequence[0]);
        CharSequence[] finalEntryValues = entryValues.toArray(new CharSequence[0]);

        list.setEntries(finalEntries);
        list.setEntryValues(finalEntryValues);
    }

    private void setAppList(ListPreference list) {
        list.setEntries(appListEntries);
        list.setEntryValues(appListEntriesValue);
    }

    private void getAppList() {
        PackageManager manager = requireActivity().getPackageManager();
        List<String> entries = new ArrayList<>();
        List<String> entryValues = new ArrayList<>();

        // Get default value.
        entries.add(getString(R.string.gestures_default));
        entryValues.add(getString(R.string.gestures_default_value));

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
        appListEntriesValue = entryValues.toArray(new CharSequence[0]);
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
                            counterToast = Toast.makeText(requireActivity(),
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

                        counterToast = Toast.makeText(requireActivity(), R.string.version_key_toast,
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
        final Preference credits = findPreference("about_credits");
        Preference restoreMenu = findPreference("restore");
        Preference hiddenAppsMenu = findPreference("hidden_apps_menu");
        Preference webProviderMenu = findPreference("web_provider");
        final Preference backupMenu = findPreference("backup");
        Preference resetMenu = findPreference("reset");

        credits.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                DialogFragment creditsInfo = new CreditsDialogFragment();
                creditsInfo.show(requireActivity().getFragmentManager(), "CreditsDialog");
                return false;
            }
        });

        hiddenAppsMenu.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ViewUtils.replaceFragment((SettingsActivity) requireActivity(),
                        new HiddenAppsFragment(), "hidden_apps");
                return false;
            }
        });

        webProviderMenu.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ViewUtils.replaceFragment((SettingsActivity) requireActivity(),
                        new WebProviderFragment(), "WebProvider");
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

        resetMenu.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder alert = new AlertDialog.Builder(requireContext());
                alert.setTitle(getString(R.string.reset_preference))
                     .setMessage(getString(R.string.reset_preference_warn))
                     .setNegativeButton(getString(android.R.string.cancel), null)
                     .setPositiveButton(R.string.reset_preference_positive,
                             new DialogInterface.OnClickListener() {
                                 @Override
                                 public void onClick(DialogInterface dialog, int which) {
                                     PreferenceHelper.getEditor().clear().apply();
                                     ((SettingsActivity) requireActivity()).restartActivity();
                                     Toast.makeText(requireContext(),
                                             R.string.reset_preference_toast, Toast.LENGTH_LONG)
                                          .show();
                                 }
                             }).show();
                return false;
            }
        });
    }

    // Used to check for storage permission.
    // Throws true when API is less than M.
    private boolean hasStoragePermission() {
        if (Utils.atLeastMarshmallow()) {
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
        ViewUtils.replaceFragment((SettingsActivity) requireActivity(), backupRestoreFragment,
                "backup_restore");
    }
}
