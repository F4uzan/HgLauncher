package mono.hg.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mono.hg.R
import mono.hg.SettingsActivity
import mono.hg.adapters.FileFolderAdapter
import mono.hg.databinding.FragmentBackupRestoreBinding
import mono.hg.models.FileFolder
import mono.hg.utils.BackupRestoreUtils
import mono.hg.wrappers.BackHandledFragment
import java.io.File
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

/**
 * A Fragment that displays both backup and restore options.
 */
class BackupRestoreFragment : BackHandledFragment() {
    private var binding: FragmentBackupRestoreBinding? = null
    private val fileFoldersList = ArrayList<FileFolder>()
    private var fileFolderAdapter: FileFolderAdapter? = null
    private var currentPath: File? = null
    private var isInRestore = false
    private lateinit var backupNameField: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBackupRestoreBinding.inflate(inflater, container, false)
        return binding !!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (arguments != null) {
            isInRestore = requireArguments().getBoolean("isRestore", false)
        }
        super.onCreate(savedInstanceState)

        /*
         * Show our own toolbar if we are running below Lollipop.
         * This is needed because the preference library does not supply toolbars
         * for fragments that it isn't managing.
         */
        (requireActivity() as SettingsActivity).supportActionBar.apply {
            if (isInRestore) {
                this?.setTitle(R.string.pref_header_restore)
            } else {
                this?.setTitle(R.string.pref_header_backup)
            }
        }

        setHasOptionsMenu(true)
        fileFolderAdapter = FileFolderAdapter(fileFoldersList, requireContext())
        val fileFolders = binding !!.filesList
        backupNameField = binding !!.fileInputEntry
        fileFolders.adapter = fileFolderAdapter

        // If we are called to restore, then hide the input field.
        if (isInRestore) {
            binding !!.fileInputContainer.visibility = View.GONE
        }

        // This fragment is only used for API levels that does not specifically
        // need scoped storage. These API levels will likely not require
        // any change, and the deprecation warning is pointless.
        @Suppress("DEPRECATION")
        currentPath = Environment.getExternalStorageDirectory()
        traverseStorage(currentPath)
        fileFolders.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            // Open and traverse to directory.
            if (fileFoldersList[position].isFolder) {
                currentPath = File(
                    currentPath.toString() + File.separator + fileFoldersList[position].name
                )
                traverseStorage(currentPath)
            } else {
                // Restore backup when clicking a file, but only do so in restore mode.
                val possibleBackup =
                    "file://" + currentPath + File.separator + fileFoldersList[position].name
                if (isInRestore && fileFoldersList[position].name.indexOf('.') > 0) {
                    CoroutineScope(Dispatchers.Main).launch {
                        BackupRestoreUtils.restoreBackup(
                            requireActivity() as SettingsActivity,
                            possibleBackup
                        )
                    }
                }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        @Suppress("DEPRECATION")
        return if (currentPath?.path != Environment.getExternalStorageDirectory().path) {
            currentPath = File(currentPath?.parent)
            traverseStorage(currentPath)
            true
        } else {
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        with(menu) {
            clear()
            add(0, 1, 100, getString(R.string.action_backup))
            getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            getItem(0).isVisible = ! isInRestore
            super.onCreateOptionsMenu(this, inflater)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> {
                requireActivity().onBackPressed()
                return true
            }
            1 -> {
                // Send our backup signal!
                if (backupNameField.text.toString() != "") {
                    val backupPath =
                        "file://" + currentPath + File.separator + backupNameField.text.toString() + ".xml"
                    val backupName = File(backupPath)
                    if (backupName.exists() && backupName.isFile) {
                        with(AlertDialog.Builder(requireActivity())) {
                            setTitle(getString(R.string.pref_header_backup))
                            setMessage(getString(R.string.backup_exist))
                            setNegativeButton(getString(R.string.dialog_cancel), null)
                            setPositiveButton(getString(R.string.backup_exist_overwrite)) { _, _ ->
                                BackupRestoreUtils.saveBackup(requireActivity(), backupPath)
                                traverseStorage(currentPath)
                            }
                            show()
                        }
                    } else {
                        BackupRestoreUtils.saveBackup(requireActivity(), backupPath)
                        traverseStorage(currentPath)
                    }
                } else {
                    Toast.makeText(requireActivity(), R.string.backup_empty, Toast.LENGTH_SHORT)
                        .show()
                }
                return true
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // Open a directory and refresh fileFoldersList.
    private fun traverseStorage(path: File?) {
        fileFoldersList.clear()
        fileFolderAdapter?.notifyDataSetInvalidated()
        val contents: Array<File>?
        contents = if (isInRestore) {
            path?.listFiles { dir ->
                (dir.name.toLowerCase(Locale.getDefault()).endsWith(".xml")
                        || dir.isDirectory && ! dir.isFile)
            }
        } else {
            path !!.listFiles()
        }
        if (contents != null && contents.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                withContext(Dispatchers.Default) {
                    contents.filter { ! it.isHidden }
                        .forEach { fileFoldersList.add(FileFolder(it.name, it.isDirectory)) }
                }
                fileFoldersList.sortWith(Comparator { f1, f2 ->
                    if (f1.isFolder && ! f2.isFolder) {
                        - 1
                    } else if (! f1.isFolder && f2.isFolder) {
                        1
                    } else {
                        f1.name.compareTo(f2.name, ignoreCase = true)
                    }
                })
                (requireActivity() as SettingsActivity).progressBar.show()
                fileFolderAdapter?.notifyDataSetChanged()
                (requireActivity() as SettingsActivity).progressBar.hide()
            }
        }
    }
}