package mono.hg.preferences;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import mono.hg.R;
import mono.hg.SettingsActivity;
import mono.hg.fragments.BackupRestoreFragment;
import mono.hg.fragments.CreditsDialogFragment;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.utils.BackupRestoreUtils;
import mono.hg.utils.Utils;
import mono.hg.utils.ViewUtils;
import mono.hg.wrappers.SpinnerPreference;

public class BasePreference extends PreferenceFragmentCompat {
    private final int RESTORE_STORAGE_CODE = 3600;
    private final int BACKUP_STORAGE_CODE = 3200;
    private static final int PERMISSION_STORAGE_CODE = 4200;
    private boolean isRestore = false;
    private Preference versionMenu;

    private Preference.OnPreferenceChangeListener RestartingListListener = new Preference.OnPreferenceChangeListener() {
        @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((SettingsActivity) requireActivity()).restartActivity();
            return true;
        }
    };

    @Override public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_base, rootKey);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SpinnerPreference appTheme = findPreference("app_theme");
        versionMenu = findPreference("version_key");

        appTheme.setOnPreferenceChangeListener(RestartingListListener);

        addVersionCounterListener();
        addFragmentListener();
    }

    private void addVersionCounterListener() {
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
                        PreferenceHelper.update("is_grandma", true);
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
                                     PreferenceHelper.update("require_refresh", true);
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

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_STORAGE_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openBackupRestore(isRestore);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
            Intent resultData) {
        Uri uri = null;

        if (resultCode == Activity.RESULT_OK && resultData != null) {
            uri = resultData.getData();
            if (requestCode == RESTORE_STORAGE_CODE) {
                new BackupRestoreUtils.RestoreBackupTask((SettingsActivity) requireActivity(), uri.toString()).execute();
            }  else if (requestCode == BACKUP_STORAGE_CODE) {
                BackupRestoreUtils.saveBackup(requireActivity(), uri.toString());
            }
        }
    }

    /**
     * Opens the backup & restore fragment.
     *
     * @param isRestore Are we calling the fragment to restore a backup?
     */
    private void openBackupRestore(boolean isRestore) {
        if (Utils.atLeastKitKat()) {
            Intent intent;

            if (isRestore) {
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            } else {
                intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            }

            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/xml");

            if (isRestore) {
                startActivityForResult(intent, RESTORE_STORAGE_CODE);
            } else {
                startActivityForResult(intent, BACKUP_STORAGE_CODE);
            }
        } else {
            BackupRestoreFragment backupRestoreFragment = new BackupRestoreFragment();
            Bundle fragmentBundle = new Bundle();
            fragmentBundle.putBoolean("isRestore", isRestore);
            backupRestoreFragment.setArguments(fragmentBundle);
            ViewUtils.replaceFragment(requireFragmentManager(), backupRestoreFragment,
                    "backup_restore");
        }
    }
}
