package mono.hg.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;

import mono.hg.R;
import mono.hg.SettingsActivity;
import mono.hg.helpers.PreferenceHelper;

public class BackupRestoreUtils {
    /**
     * Saves preferences to a local file.
     *
     * @param path Where should the preferences be saved to?
     */
    public static void saveBackup(Context context, String path) {
        ObjectOutputStream out = null;
        Uri uri = Uri.parse(path);

        try {
            out = new ObjectOutputStream(context.getContentResolver().openOutputStream(uri));
            out.writeObject(PreferenceHelper.getPreference().getAll());
        } catch (FileNotFoundException e) {
            Utils.sendLog(Utils.LogLevel.ERROR, e.toString());
        } catch (IOException e) {
            Utils.sendLog(Utils.LogLevel.ERROR, e.toString());
        } finally {
            try {
                if (out != null) {
                    out.close();
                    out.flush();
                }
            } catch (IOException e) {
                Utils.sendLog(Utils.LogLevel.ERROR, e.toString());
            }
            Toast.makeText(context, R.string.backup_complete, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Restores a local backup and exports all the preferences stored.
     *
     * @param uri The Uri representing the file itself.
     */
    @SuppressWarnings("unchecked") private static void restoreBackup(Context context, Uri uri) {
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(context.getContentResolver().openInputStream(uri));
            PreferenceHelper.getEditor().clear();
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean) {
                    PreferenceHelper.getEditor().putBoolean(key, (Boolean) v);
                } else if (v instanceof Float) {
                    PreferenceHelper.getEditor().putFloat(key, (Float) v);
                } else if (v instanceof Integer) {
                    PreferenceHelper.getEditor().putInt(key, (Integer) v);
                } else if (v instanceof Long) {
                    PreferenceHelper.getEditor().putLong(key, (Long) v);
                } else if (v instanceof Set) {
                    PreferenceHelper.getEditor().putStringSet(key, (Set<String>) v);
                } else if (v instanceof String) {
                    PreferenceHelper.getEditor().putString(key, ((String) v));
                }

                PreferenceHelper.update("require_refresh", true);
            }
            PreferenceHelper.getEditor().apply();
        } catch (FileNotFoundException | ClassNotFoundException e) {
            Utils.sendLog(Utils.LogLevel.ERROR, e.toString());
        } catch (IOException e) {
            Utils.sendLog(Utils.LogLevel.ERROR, e.toString());
        } finally {
            Utils.closeStream(input);
        }
    }

    public static class restoreBackupTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<SettingsActivity> fragmentRef;
        private Uri uri;
        private ProgressDialog progress;

        public restoreBackupTask(SettingsActivity activity, String path) {
            fragmentRef = new WeakReference<>(activity);
            this.uri = Uri.parse(path);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = new ProgressDialog(fragmentRef.get());
            progress.setMessage(fragmentRef.get().getString(R.string.backup_restore_dialog));
            progress.setIndeterminate(false);
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            SettingsActivity fragment = fragmentRef.get();
            if (fragment != null) {
                restoreBackup(fragment, uri);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            SettingsActivity fragment = fragmentRef.get();
            if (fragment != null) {
                progress.dismiss();
                ((SettingsActivity) fragment).restartActivity();
                Toast.makeText(fragmentRef.get(), R.string.restore_complete,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
