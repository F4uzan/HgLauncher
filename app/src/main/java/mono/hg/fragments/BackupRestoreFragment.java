package mono.hg.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import mono.hg.R;
import mono.hg.SettingsActivity;
import mono.hg.adapters.FileFolderAdapter;
import mono.hg.models.FileFolder;
import mono.hg.utils.BackupRestoreUtils;
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
        if (getArguments() != null) {
            isInRestore = getArguments().getBoolean("isRestore", false);
        }

        super.onCreate(savedInstanceState);

        /*
         * Show our own toolbar if we are running below Lollipop.
         * This is needed because the preference library does not supply toolbars
         * for fragments that it isn't managing.
         */
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
                    String possibleBackup = "file://" + currentPath + File.separator + fileFoldersList.get(position).getName();
                    if (isInRestore && fileFoldersList.get(position).getName().indexOf('.') > 0) {
                        new BackupRestoreUtils.restoreBackupTask((SettingsActivity) requireActivity(), possibleBackup).execute();
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
                    final String backupPath = "file://" + currentPath + File.separator + backupNameField.getText().toString() + ".xml";
                    final File backupName = new File(backupPath);
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
                                        BackupRestoreUtils.saveBackup(requireActivity(), backupPath);
                                        traverseStorage(currentPath);
                                    }
                                });
                        overwriteDialog.show();
                    } else {
                        BackupRestoreUtils.saveBackup(requireActivity(), backupPath);
                        traverseStorage(this.currentPath);
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
}
