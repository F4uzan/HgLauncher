package mono.hg.utils

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.widget.Toast
import mono.hg.R
import mono.hg.SettingsActivity
import mono.hg.helpers.PreferenceHelper
import mono.hg.utils.Utils.LogLevel
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.ref.WeakReference

/**
 * Utils class that handles both backup and restore.
 */
object BackupRestoreUtils {
    /**
     * Naively saves preferences to a local file.
     *
     * This function does not perform checks for which preferences
     * to keep and will not perform any check for the resulting
     * file.
     *
     * TODO: Selectively pick preferences instead of dumping everything.
     *
     * @param path Where should the preferences be saved to?
     */
    fun saveBackup(context: Context, path: String?) {
        var out: ObjectOutputStream? = null
        val uri = Uri.parse(path)
        try {
            out = ObjectOutputStream(context.contentResolver.openOutputStream(uri))
            out.writeObject(PreferenceHelper.preference.all)
        } catch (e: FileNotFoundException) {
            Utils.sendLog(LogLevel.ERROR, e.toString())
        } catch (e: IOException) {
            Utils.sendLog(LogLevel.ERROR, e.toString())
        } finally {
            try {
                if (out != null) {
                    out.close()
                    out.flush()
                }
            } catch (e: IOException) {
                Utils.sendLog(LogLevel.ERROR, e.toString())
            }
            Toast.makeText(context, R.string.backup_complete, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Restores a local backup and exports all the preferences stored.
     *
     * This function clears all the existing preferences before restoring
     * the backup, and [PreferenceHelper.fetchPreference] will also be called
     * to clear the cached values.
     *
     * @param uri The Uri representing the file itself.
     */
    fun restoreBackup(context: Context, uri: Uri) {
        ObjectInputStream(context.contentResolver.openInputStream(uri)).use {
            // We have to reset the leftover preferences to make sure they don't linger.
            PreferenceHelper.editor?.clear()?.apply()

            val entries = it.readObject() as Map<String, *>
            entries.forEach {
                when (val v = it.value !!) {
                    is Boolean -> {
                        PreferenceHelper.editor?.putBoolean(it.key, v)
                    }
                    is Float -> {
                        PreferenceHelper.editor?.putFloat(it.key, v)
                    }
                    is Int -> {
                        PreferenceHelper.editor?.putInt(it.key, v)
                    }
                    is Long -> {
                        PreferenceHelper.editor?.putLong(it.key, v)
                    }
                    is Set<*> -> {
                        PreferenceHelper.editor?.putStringSet(it.key, v as Set<String?>)
                    }
                    is String -> {
                        PreferenceHelper.editor?.putString(it.key, v)
                    }
                }
                PreferenceHelper.update("require_refresh", true)
            }

            // Fetch again.
            PreferenceHelper.editor?.apply()
            PreferenceHelper.fetchPreference()
        }
    }

    /**
     * An AsyncTask tied to [SettingsActivity], used to read a backup file (.xml)
     * and attempts to restore it.
     *
     * @param activity  The SettingsActivity itself.
     * @param path      Path to the backup file. This path will be parsed by [Uri.parse], therefore
     *                  it must be a valid Uri, prefixed by "file://".
     */
    class RestoreBackupTask(activity: SettingsActivity, path: String?) :
        AsyncTask<Void?, Void?, Void?>() {
        private val fragmentRef: WeakReference<SettingsActivity> = WeakReference(activity)
        private val uri: Uri = Uri.parse(path)
        override fun onPreExecute() {
            super.onPreExecute()
            with (fragmentRef.get()) {
                this?.progressBar?.show()
            }
        }

        override fun doInBackground(vararg params: Void?): Void? {
            with (fragmentRef.get()) {
                this?.let { restoreBackup(it, uri) }
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            with (fragmentRef.get()) {
                this?.progressBar?.hide()
                this?.let { ViewUtils.restartActivity(it, false) }
                Toast.makeText(this, R.string.restore_complete, Toast.LENGTH_LONG).show()
            }
        }

    }
}