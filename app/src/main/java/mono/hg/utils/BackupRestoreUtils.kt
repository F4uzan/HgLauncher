package mono.hg.utils

import android.app.ProgressDialog
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
            Utils.sendLog(LogLevel.Companion.ERROR, e.toString())
        } catch (e: IOException) {
            Utils.sendLog(LogLevel.ERROR, e.toString())
        } finally {
            try {
                if (out != null) {
                    out.close()
                    out.flush()
                }
            } catch (e: IOException) {
                Utils.sendLog(LogLevel.Companion.ERROR, e.toString())
            }
            Toast.makeText(context, R.string.backup_complete, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Restores a local backup and exports all the preferences stored.
     *
     * @param uri The Uri representing the file itself.
     */
    private fun restoreBackup(context: Context, uri: Uri) {
        var input: ObjectInputStream? = null
        try {
            input = ObjectInputStream(context.contentResolver.openInputStream(uri))
            PreferenceHelper.editor?.clear()
            val entries = input.readObject() as Map<String, *>
            for ((key, value) in entries) {
                val v = value!!
                if (v is Boolean) {
                    PreferenceHelper.editor?.putBoolean(key, v)
                } else if (v is Float) {
                    PreferenceHelper.editor?.putFloat(key, v)
                } else if (v is Int) {
                    PreferenceHelper.editor?.putInt(key, v)
                } else if (v is Long) {
                    PreferenceHelper.editor?.putLong(key, v)
                } else if (v is Set<*>) {
                    PreferenceHelper.editor?.putStringSet(key, v as Set<String?>)
                } else if (v is String) {
                    PreferenceHelper.editor?.putString(key, v)
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

    class RestoreBackupTask(activity: SettingsActivity, path: String?) : AsyncTask<Void?, Void?, Void?>() {
        private val fragmentRef: WeakReference<SettingsActivity> = WeakReference(activity)
        private val uri: Uri = Uri.parse(path)
        private var progress: ProgressDialog? = null
        override fun onPreExecute() {
            super.onPreExecute()
            progress = ProgressDialog(fragmentRef.get())
            progress!!.setMessage(fragmentRef.get()!!.getString(R.string.backup_restore_dialog))
            progress!!.isIndeterminate = false
            progress!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            progress!!.show()
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
                progress!!.dismiss()
                fragment.restartActivity()
                Toast.makeText(fragmentRef.get(), R.string.restore_complete,
                        Toast.LENGTH_LONG).show()
            }
        }

    }
}