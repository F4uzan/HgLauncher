package mono.hg.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

import mono.hg.FileFolder;
import mono.hg.R;
import mono.hg.Utils;
import mono.hg.adapters.FileFolderAdapter;
import mono.hg.wrappers.BackHandledFragment;

public class BackupRestoreFragment extends BackHandledFragment {
    private ArrayList<FileFolder> fileFoldersList = new ArrayList<>();
    private FileFolderAdapter fileFolderAdapter;
    private File path;
    private Boolean isInRestore = false;
    private SharedPreferences prefs;
    private EditText backupNameField;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_backup_restore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        setHasOptionsMenu(true);

        super.onCreate(savedInstanceState);

        fileFolderAdapter = new FileFolderAdapter(fileFoldersList, getActivity());
        FrameLayout fileInputContainer = getActivity().findViewById(R.id.file_input_container);
        ListView fileFolders = getActivity().findViewById(R.id.files_list);
        backupNameField = getActivity().findViewById(R.id.file_input_entry);

        fileFolders.setAdapter(fileFolderAdapter);

        // If we are called to restore, then hide the input field.
        // Also set appropriate action bar title here.
        if (this.getArguments().getBoolean("isRestore", false)) {
            getActivity().setTitle(R.string.pref_header_restore);
            fileInputContainer.setVisibility(View.GONE);
            isInRestore = true;
        } else {
            getActivity().setTitle(R.string.pref_header_backup);
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
                        new restoreBackupTask(BackupRestoreFragment.this, possibleBackup).execute();
                    }
                }
            }
        });
    }

    @Override
    public boolean onBackPressed() {
        if (!path.getPath().equals(Environment.getExternalStorageDirectory().getPath())) {
            path = new File(path.getParent());
            traverseStorage(path);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        // Hide 'Backup' menu item in restore mode.
        if (!isInRestore) {
            menu.add(0, 1, 100, getString(R.string.pref_header_backup));
            menu.getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
            case 1:
                // Send our backup signal!
                if (!backupNameField.getText().toString().equals("")) {
                    final File backupName = new File(path + File.separator + backupNameField.getText().toString() + ".xml");
                    if (backupName.exists() && backupName.isFile()) {
                        final AlertDialog.Builder overwriteDialog = new AlertDialog.Builder(getActivity());
                        overwriteDialog.setTitle(getString(R.string.pref_header_backup));
                        overwriteDialog.setMessage(getString(R.string.backup_exist));
                        overwriteDialog.setNegativeButton(getString(R.string.backup_exist_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing.
                            }
                        });
                        overwriteDialog.setPositiveButton(getString(R.string.backup_exist_overwrite), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                saveBackup(backupName);
                            }
                        });
                        overwriteDialog.show();
                    } else {
                        saveBackup(backupName);
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.backup_empty, Toast.LENGTH_SHORT).show();
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
            Utils.sendLog(3, e.toString());
        } catch (IOException e) {
            Utils.sendLog(3, e.toString());
        } finally {
            try {
                if (out != null) {
                    out.close();
                    out.flush();
                }
            } catch (IOException e) {
                Utils.sendLog(3, e.toString());
            }
            traverseStorage(this.path);
            Toast.makeText(getActivity(), R.string.backup_complete, Toast.LENGTH_SHORT).show();
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
            Utils.sendLog(3, e.toString());
        } catch (IOException e) {
            Utils.sendLog(3, e.toString());
        } catch (ClassNotFoundException e) {
            Utils.sendLog(3, e.toString());
        } finally {
            try {
                if (input != null)
                    input.close();
            } catch (IOException e) {
                Utils.sendLog(3, e.toString());
            }
        }
    }

    private static class restoreBackupTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<BackupRestoreFragment> fragmentRef;
        private File path;
        private ProgressDialog progress;

        restoreBackupTask(BackupRestoreFragment context, File path) {
            fragmentRef = new WeakReference<>(context);
            this.path = path;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = new ProgressDialog(fragmentRef.get().getActivity());
            progress.setMessage(fragmentRef.get().getString(R.string.backup_restore_dialogue));
            progress.setIndeterminate(false);
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            BackupRestoreFragment fragment = fragmentRef.get();
            fragment.restoreBackup(path);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            BackupRestoreFragment fragment = fragmentRef.get();
            super.onPostExecute(result);
            progress.dismiss();
            fragment.getActivity().recreate();
            Toast.makeText(fragmentRef.get().getActivity(), R.string.restore_complete, Toast.LENGTH_LONG).show();
        }
    }
}
