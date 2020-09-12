package mono.hg.preferences

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.jaredrummler.android.colorpicker.ColorPreferenceCompat
import kotlinx.coroutines.launch
import mono.hg.R
import mono.hg.SettingsActivity
import mono.hg.fragments.BackupRestoreFragment
import mono.hg.fragments.CreditsDialogFragment
import mono.hg.helpers.PreferenceHelper
import mono.hg.utils.BackupRestoreUtils
import mono.hg.utils.Utils
import mono.hg.utils.ViewUtils
import mono.hg.utils.applyAccent
import mono.hg.wrappers.SpinnerPreference

/**
 * The main preference menu. This PreferenceFragment is used
 * as a hub for other preferences, and it hosts main-level preferences.
 */
class BasePreference : PreferenceFragmentCompat() {
    private var isRestore = false

    private val RestartingListListener = Preference.OnPreferenceChangeListener { _, _ ->
        ViewUtils.restartActivity(requireActivity() as AppCompatActivity, false)
        true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_base, rootKey)
    }

    override fun onResume() {
        super.onResume()

        // Update the action bar title.
        (requireActivity() as SettingsActivity).supportActionBar?.setTitle(R.string.action_settings)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        RestartingListListener.apply {
            findPreference<SpinnerPreference>("app_theme")?.onPreferenceChangeListener = this
            findPreference<ColorPreferenceCompat>("app_accent")?.onPreferenceChangeListener = this
        }
        addVersionCounterListener()
        addFragmentListener()
    }

    private fun addVersionCounterListener() {
        val versionMenu = findPreference<Preference>("version_key")

        if (! PreferenceHelper.preference.getBoolean("is_grandma", false)) {
            versionMenu?.onPreferenceClickListener = object : Preference.OnPreferenceClickListener {
                var counter = 9
                var counterToast: Toast? = null
                override fun onPreferenceClick(preference: Preference): Boolean {
                    counterToast?.cancel()
                    when (counter) {
                        in 2 .. 7 -> {
                            counterToast = Toast.makeText(
                                requireActivity(), String.format(
                                    getString(R.string.version_key_toast_plural),
                                    counter
                                ), Toast.LENGTH_SHORT
                            ).apply { show() }
                        }
                        1 -> {
                            counterToast = Toast.makeText(
                                requireActivity(), R.string.version_key_toast,
                                Toast.LENGTH_SHORT
                            ).apply { show() }
                        }
                        0 -> {
                            PreferenceHelper.update("is_grandma", true)
                            versionMenu?.setTitle(R.string.version_key_name)
                        }
                    }
                    counter --
                    return false
                }
            }
        } else {
            versionMenu?.setTitle(R.string.version_key_name)
        }
    }

    private fun addFragmentListener() {
        val credits = findPreference<Preference>("about_credits")
        val restoreMenu = findPreference<Preference>("restore")
        val backupMenu = findPreference<Preference>("backup")
        val resetMenu = findPreference<Preference>("reset")
        credits?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            CreditsDialogFragment().show(requireActivity().supportFragmentManager, "CreditsDialog")
            false
        }
        backupMenu?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            isRestore = false
            hasStoragePermission()
        }
        restoreMenu?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            isRestore = true
            hasStoragePermission()
        }
        resetMenu?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            with(AlertDialog.Builder(requireContext())) {
                setTitle(getString(R.string.reset_preference))
                setMessage(getString(R.string.reset_preference_warn))
                setNegativeButton(getString(R.string.dialog_cancel), null)
                setPositiveButton(R.string.dialog_ok) { _, _ ->
                    // We have to reset the leftover preferences to make sure they don't linger.
                    PreferenceHelper.editor?.clear()?.apply()
                    PreferenceHelper.fetchPreference()

                    PreferenceHelper.update("require_refresh", true)
                    ViewUtils.restartActivity(requireActivity() as AppCompatActivity, false)
                    Toast.makeText(
                        requireContext(),
                        R.string.reset_preference_toast,
                        Toast.LENGTH_LONG
                    ).show()
                }

                create().apply {
                    show()
                    applyAccent()
                }
            }
            false
        }
    }

    // Used to check for storage permission.
    // Throws true when API is less than M.
    private fun hasStoragePermission(): Boolean {
        return if (Utils.atLeastMarshmallow()) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_STORAGE_CODE
            )
            false
        } else {
            openBackupRestore(isRestore)
            return true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_STORAGE_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openBackupRestore(isRestore)
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int,
        resultData: Intent?
    ) {
        val uri: Uri?
        if (resultCode == Activity.RESULT_OK && resultData != null) {
            uri = resultData.data
            if (requestCode == RESTORE_STORAGE_CODE) {
                lifecycleScope.launch {
                    BackupRestoreUtils.restoreBackup(
                        requireActivity() as SettingsActivity,
                        uri.toString()
                    )
                }
            } else if (requestCode == BACKUP_STORAGE_CODE) {
                BackupRestoreUtils.saveBackup(requireActivity(), uri.toString())
            }
        }
    }

    /**
     * Opens the backup & restore fragment.
     *
     * @param isRestore Are we calling the fragment to restore a backup?
     */
    private fun openBackupRestore(isRestore: Boolean) {
        if (Utils.atLeastKitKat()) {
            if (isRestore) {
                Intent(Intent.ACTION_OPEN_DOCUMENT)
            } else {
                Intent(Intent.ACTION_CREATE_DOCUMENT)
            }.apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/xml"
            }.also {
                if (isRestore) {
                    startActivityForResult(it, RESTORE_STORAGE_CODE)
                } else {
                    startActivityForResult(it, BACKUP_STORAGE_CODE)
                }
            }
        } else {
            Bundle().apply { putBoolean("isRestore", isRestore) }.also {
                ViewUtils.replaceFragment(
                    requireActivity().supportFragmentManager,
                    BackupRestoreFragment().apply { arguments = it },
                    "backup_restore"
                )
            }
        }
    }

    companion object {
        private const val PERMISSION_STORAGE_CODE = 4200
        private const val RESTORE_STORAGE_CODE = 3600
        private const val BACKUP_STORAGE_CODE = 3200
    }
}