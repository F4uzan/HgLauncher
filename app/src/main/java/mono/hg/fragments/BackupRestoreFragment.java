package mono.hg.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

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

import mono.hg.R;
import mono.hg.SettingsActivity;
import mono.hg.adapters.FileFolderAdapter;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.models.FileFolder;
import mono.hg.utils.Utils;
import mono.hg.wrappers.BackHandledFragment;

public class BackupRestoreFragment extends BackHandledFragment {
    private ArrayList<FileFolder> fileFoldersList = new ArrayList<>();
    private FileFolderAdapter fileFolderAdapter;
    private File currentPath;
    private Boolean isInRestore = false;
    private EditText backupNameField;

    @Override public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_backup_restore, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Show our own toolbar if we are running below Lollipop.
         * This is needed because the preference library does not supply toolbars
         * for fragments that it isn't managing.
         */
        if (getArguments() != null) {
            isInRestore = getArguments().getBoolean("isRestore", false);
        }

        ActionBar actionBar = ((SettingsActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            if (isInRestore) {
                actionBar.setTitle(R.string.pref_header_restore);
            } else {
                actionBar.setTitle(R.string.pref_header_backup);
            }
        }

        setHasOptionsMenu(true);

        fileFolderAdapter = new FileFolderAdapter(fileFoldersList, requireActivity());
        FrameLayout fileInputContainer = requireActivity().findViewById(R.id.file_input_container);
        ListView fileFolders = requireActivity().findViewById(R.id.files_list);
        backupNameField = requireActivity().findViewById(R.id.file_input_entry);

        fileFolders.setAdapter(fileFolderAdapter);

        // If we are called to restore, then hide the input field.
        if (isInRestore) {
            fileInputContainer.setVisibility(View.GONE);
        }

        // Check for storage permission if we're in Marshmallow and up.
        currentPath = Environment.getExternalStorageDirectory();
        traverseStorage(currentPath);
        fileFolders.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Open and traverse to directory.
                if (fileFoldersList.get(position).isFolder()) {
                    currentPath = new File(
                            currentPath + File.separator + fileFoldersList.get(position).getName());
                    traverseStorage(currentPath);
                } else {
                    // Restore backup when clicking a file, but only do so in restore mode.
                    File possibleBackup = new File(
                            currentPath + File.separator + fileFoldersList.get(position).getName());
                    if (isInRestore && possibleBackup.isFile()
                            && fileFoldersList.get(position).getName().indexOf('.') > 0) {
                        new restoreBackupTask(BackupRestoreFragment.this, possibleBackup).execute();
                    }
                }
            }
        });
    }

    @Override public boolean onBackPressed() {
        if (!currentPath.getPath().equals(Environment.getExternalStorageDirectory().getPath())) {
            currentPath = new File(currentPath.getParent());
            traverseStorage(currentPath);
            return true;
        } else {
            return false;
        }
    }

    @Override public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        menu.add(0, 1, 100, getString(R.string.action_backup));
        menu.getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.getItem(0).setVisible(!isInRestore);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                requireActivity().onBackPressed();
                return true;
            case 1:
                // Send our backup signal!
                if (!backupNameField.getText().toString().equals("")) {
                    final File backupName = new File(
                            currentPath + File.separator + backupNameField.getText()
                                                                          .toString() + ".xml");
                    if (backupName.exists() && backupName.isFile()) {
                        final AlertDialog.Builder overwriteDialog = new AlertDialog.Builder(
                                requireActivity());
                        overwriteDialog.setTitle(getString(R.string.pref_header_backup));
                        overwriteDialog.setMessage(getString(R.string.backup_exist));
                        overwriteDialog.setNegativeButton(getString(R.string.backup_exist_cancel),
                                null);
                        overwriteDialog.setPositiveButton(
                                getString(R.string.backup_exist_overwrite),
                                new DialogInterface.OnClickListener() {
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
                    Toast.makeText(requireActivity(), R.string.backup_empty, Toast.LENGTH_SHORT)
                         .show();
                }
                return true;
            default:
                // Do nothing.
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // Open a directory and refresh fileFoldersList.
    private void traverseStorage(File path) {
        fileFoldersList.clear();
        fileFolderAdapter.notifyDataSetInvalidated();
        File[] contents;
        if (isInRestore) {
            contents = path.listFiles(new FileFilter() {
                public boolean accept(File dir) {
                    return dir.getName().toLowerCase().endsWith(".xml")
                            || dir.isDirectory() && !dir.isFile();
                }
            });
        } else {
            contents = path.listFiles();
        }

        if (contents != null && contents.length > 0) {
            for (File availableContents : contents) {
                // Don't show hidden (.dot) files/folders.
                if (!availableContents.isHidden()) {
                    fileFoldersList.add(new FileFolder(availableContents.getName(),
                            availableContents.isDirectory()));
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

    /**
     * Saves preferences to a local file.
     *
     * @param path Where should the preferences be saved to?
     */
    private void saveBackup(File path) {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(path));
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
            traverseStorage(this.currentPath);
            Toast.makeText(requireActivity(), R.string.backup_complete, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Restores a local backup and exports all the preferences stored.
     *
     * @param path Where does the backup reside in?
     */
    @SuppressWarnings("unchecked") private void restoreBackup(File path) {
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(new FileInputStream(path));
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
            progress = new ProgressDialog(fragmentRef.get().requireActivity());
            progress.setMessage(fragmentRef.get().getString(R.string.backup_restore_dialog));
            progress.setIndeterminate(false);
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            BackupRestoreFragment fragment = fragmentRef.get();
            if (fragment != null) {
                fragment.restoreBackup(path);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            BackupRestoreFragment fragment = fragmentRef.get();
            if (fragment != null) {
                progress.dismiss();
                ((SettingsActivity) fragment.requireActivity()).restartActivity();
                Toast.makeText(fragmentRef.get().requireActivity(), R.string.restore_complete,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
