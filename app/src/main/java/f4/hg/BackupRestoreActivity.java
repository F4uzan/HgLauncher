package f4.hg;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import f4.hg.adapters.FileFolderAdapter;
import f4.hg.R;

public class BackupRestoreActivity extends AppCompatActivity {

    private ArrayList<FileFolder> fileFoldersList = new ArrayList<>();
    private FileFolderAdapter fileFolderAdapter;
    private File path;
    private Boolean isInRestore = false;
    private SharedPreferences prefs;
    private EditText backupNameField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Load appropriate theme before creating the activity.
        if (prefs.getBoolean("dark_theme", false) && prefs.getBoolean("dark_theme_black", true)) {
            setTheme(R.style.AppTheme_Dark);
        } else if (!prefs.getBoolean("dark_theme", false) && !prefs.getBoolean("dark_theme_black", true)) {
            setTheme(R.style.AppTheme);
        } else if (!prefs.getBoolean("dark_theme_black", true)) {
            setTheme(R.style.AppTheme_Gray);
        }

        setContentView(R.layout.activity_backup_restore);

        fileFolderAdapter = new FileFolderAdapter(fileFoldersList, this);
        FrameLayout fileInputContainer = findViewById(R.id.file_input_container);
        ListView fileFolders = findViewById(R.id.files_list);
        backupNameField = findViewById(R.id.file_input_entry);

        fileFolders.setAdapter(fileFolderAdapter);

        // If we are called to restore, then hide the input field.
        // Also set appropriate action bar title here.
        if (getIntent().getBooleanExtra("isRestore", false)) {
            this.setTitle(R.string.pref_header_restore);
            fileInputContainer.setVisibility(View.GONE);
            isInRestore = true;
        } else {
            this.setTitle(R.string.pref_header_backup);
        }

        // Check for storage permission if we're in Marshmallow and up.
        path = Environment.getExternalStorageDirectory();
        traverseStorage(path);

        fileFolders.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Open and traverse to directory.
                if (fileFoldersList.get(position).isFolder()) {
                    path = new File(path + File.separator + fileFoldersList.get(position).getName());
                    traverseStorage(path);
                } else {
                    // Restore backup when clicking a file, but only do so in restore mode.
                    File possibleBackup = new File(path + File.separator + fileFoldersList.get(position).getName());
                    if (isInRestore && possibleBackup.isFile() && fileFoldersList.get(position).getName().indexOf('.') > 0) {
                        new restoreBackupTask(BackupRestoreActivity.this, possibleBackup).execute();
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Go back when a parent directory is available, if not then just trigger back pressed.
        if (!path.getPath().equals(Environment.getExternalStorageDirectory().getPath())) {
            path = new File(path.getParent());
            traverseStorage(path);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        // Hide 'Backup' menu item in restore mode.
        if (!isInRestore) {
            menu.add(0, 1, 100, getString(R.string.pref_header_backup));
            menu.getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        } else if (id == 1) {
            // Send our backup signal!
            if (!backupNameField.getText().toString().equals("")) {
                saveBackup(new File(path + File.separator + backupNameField.getText().toString() + ".xml"));
            } else {
                Toast.makeText(this, R.string.backup_empty, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Open a directory and refresh fileFoldersList.
    public void traverseStorage(File path) {
        fileFoldersList.clear();
        fileFolderAdapter.notifyDataSetInvalidated();
        File contents[];
        if (isInRestore) {
            contents = path.listFiles(new FileFilter() {
                public boolean accept(File dir) {
                    return dir.getName().toLowerCase().endsWith(".xml") || dir.isDirectory() && !dir.isFile();
                }
            });
        } else {
            contents = path.listFiles();
        }

        if (contents.length > 0) {
            for (File availableContents : contents) {
                // Don't show hidden (.hidden) files/folders.
                if (!availableContents.isHidden()) {
                    if (availableContents.isDirectory()) {
                        fileFoldersList.add(new FileFolder(availableContents.getName(), true));
                    } else if (availableContents.isFile()) {
                        fileFoldersList.add(new FileFolder(availableContents.getName(), false));
                    }
                }
            }
            fileFolderAdapter.notifyDataSetChanged();
        }

        Collections.sort(fileFoldersList, new Comparator<FileFolder>() {
            @Override
            public int compare(FileFolder f1, FileFolder f2) {
                if (f1.isFolder() && !f2.isFolder()) {
                    return -1;
                } else if (!f1.isFolder() && f2.isFolder()) {
                    return 1;
                } else {
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            }
        });
    }

    // Save backup to XML.
    // Resulting backup may be unreadable and jumbled.
    public void saveBackup(File path) {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(path));
            out.writeObject(prefs.getAll());
        } catch (FileNotFoundException e) {
            Log.e("Hubby", e.toString());
        } catch (IOException e) {
            Log.e("Hubby", e.toString());
        } finally {
            try {
                if (out != null) {
                    out.close();
                    out.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(BackupRestoreActivity.this, R.string.backup_complete, Toast.LENGTH_SHORT).show();
        }
    }

    // Restore backup from a specified file.
    @SuppressWarnings("unchecked")
    public void restoreBackup(File path) {
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(new FileInputStream(path));
            SharedPreferences.Editor edit = prefs.edit();
            edit.clear();
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean) {
                    edit.putBoolean(key, (Boolean) v);
                } else if (v instanceof Float) {
                    edit.putFloat(key, (Float) v);
                } else if (v instanceof Integer) {
                    edit.putInt(key, (Integer) v);
                } else if (v instanceof Long) {
                    edit.putLong(key, (Long) v);
                } else if (v instanceof Set){
                    edit.putStringSet(key, (Set<String>) v);
                } else if (v instanceof String) {
                    edit.putString(key, ((String) v));
                }
            }
            edit.apply();
        } catch (FileNotFoundException e) {
            Log.e("Hubby", e.toString());
        } catch (IOException e) {
            Log.e("Hubby", e.toString());
        } catch (ClassNotFoundException e) {
            Log.e("Hubby", e.toString());
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                Log.e("Hubby", e.toString());
            }
        }
    }

    private static class restoreBackupTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<BackupRestoreActivity> activityRef;
        private File path;
        private ProgressDialog progress;

        restoreBackupTask(BackupRestoreActivity context, File path) {
            activityRef = new WeakReference<>(context);
            this.path = path;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = new ProgressDialog(activityRef.get());
            progress.setMessage(activityRef.get().getString(R.string.backup_restore_dialogue));
            progress.setIndeterminate(false);
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            BackupRestoreActivity activity = activityRef.get();
            if (activity != null) {
                activity.restoreBackup(path);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            progress.dismiss();
            Toast.makeText(activityRef.get(), R.string.restore_complete, Toast.LENGTH_LONG).show();
        }
    }
}
