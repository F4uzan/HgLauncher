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
     * Saves preferences to a local file.
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
     * @param uri The Uri representing the file itself.
     */
    fun restoreBackup(context: Context, uri: Uri) {
        var input: ObjectInputStream? = null
        try {
            input = ObjectInputStream(context.contentResolver.openInputStream(uri))
            PreferenceHelper.editor?.clear()
            val entries = input.readObject() as Map<String, *>
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
            PreferenceHelper.editor?.apply()
        } catch (e: FileNotFoundException) {
            Utils.sendLog(LogLevel.ERROR, e.toString())
        } catch (e: ClassNotFoundException) {
            Utils.sendLog(LogLevel.ERROR, e.toString())
        } catch (e: IOException) {
            Utils.sendLog(LogLevel.ERROR, e.toString())
        } finally {
            Utils.closeStream(input)
        }
    }

    /**
     * An AsyncTask tied to [SettingsActivity], used to read a backup file (.xml)
     * and attempts to restore it.
     */
    class RestoreBackupTask(activity: SettingsActivity, path: String?) :
        AsyncTask<Void?, Void?, Void?>() {
        private val fragmentRef: WeakReference<SettingsActivity> = WeakReference(activity)
        private val uri: Uri = Uri.parse(path)
        override fun onPreExecute() {
            super.onPreExecute()
            val fragment = fragmentRef.get()
            fragment?.progressBar?.show()
        }

        override fun doInBackground(vararg params: Void?): Void? {
            val fragment = fragmentRef.get()
            fragment?.let { restoreBackup(it, uri) }
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            val fragment = fragmentRef.get()
            if (fragment != null) {
                fragment.progressBar.hide()
                fragment.restartActivity()
                Toast.makeText(
                    fragmentRef.get(), R.string.restore_complete,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    }
}