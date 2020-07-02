package mono.hg.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import mono.hg.R
import mono.hg.SettingsActivity
import mono.hg.adapters.FileFolderAdapter
import mono.hg.databinding.FragmentBackupRestoreBinding
import mono.hg.models.FileFolder
import mono.hg.utils.BackupRestoreUtils
import mono.hg.utils.BackupRestoreUtils.RestoreBackupTask
import mono.hg.wrappers.BackHandledFragment
import java.io.File
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

class BackupRestoreFragment : BackHandledFragment() {
    private var binding: FragmentBackupRestoreBinding? = null
    private val fileFoldersList = ArrayList<FileFolder>()
    private var fileFolderAdapter: FileFolderAdapter? = null
    private var currentPath: File? = null
    private var isInRestore = false
    private lateinit var backupNameField: EditText

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentBackupRestoreBinding.inflate(inflater, container, false)
        return binding!!.root
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
        val actionBar = (requireActivity() as SettingsActivity).supportActionBar
        if (actionBar != null) {
            if (isInRestore) {
                actionBar.setTitle(R.string.pref_header_restore)
            } else {
                actionBar.setTitle(R.string.pref_header_backup)
            }
        }
        setHasOptionsMenu(true)
        fileFolderAdapter = context?.let { FileFolderAdapter(fileFoldersList, it) }
        val fileInputContainer = binding!!.fileInputContainer
        val fileFolders = binding!!.filesList
        backupNameField = binding!!.fileInputEntry
        fileFolders.adapter = fileFolderAdapter

        // If we are called to restore, then hide the input field.
        if (isInRestore) {
            fileInputContainer.visibility = View.GONE
        }

        // Check for storage permission if we're in Marshmallow and up.
        currentPath = Environment.getExternalStorageDirectory()
        traverseStorage(currentPath)
        fileFolders.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            // Open and traverse to directory.
            if (fileFoldersList[position].isFolder) {
                currentPath = File(
                        currentPath.toString() + File.separator + fileFoldersList[position].name)
                traverseStorage(currentPath)
            } else {
                // Restore backup when clicking a file, but only do so in restore mode.
                val possibleBackup = "file://" + currentPath + File.separator + fileFoldersList[position].name
                if (isInRestore && fileFoldersList[position].name.indexOf('.') > 0) {
                    RestoreBackupTask(requireActivity() as SettingsActivity, possibleBackup).execute()
                }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        return if (currentPath?.path != Environment.getExternalStorageDirectory().path) {
            currentPath = File(currentPath?.parent)
            traverseStorage(currentPath)
            true
        } else {
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        menu.add(0, 1, 100, getString(R.string.action_backup))
        menu.getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.getItem(0).isVisible = !isInRestore
        super.onCreateOptionsMenu(menu, inflater)
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
                    val backupPath = "file://" + currentPath + File.separator + backupNameField.text.toString() + ".xml"
                    val backupName = File(backupPath)
                    if (backupName.exists() && backupName.isFile) {
                        val overwriteDialog = AlertDialog.Builder(
                                requireActivity())
                        overwriteDialog.setTitle(getString(R.string.pref_header_backup))
                        overwriteDialog.setMessage(getString(R.string.backup_exist))
                        overwriteDialog.setNegativeButton(getString(R.string.backup_exist_cancel),
                                null)
                        overwriteDialog.setPositiveButton(
                                getString(R.string.backup_exist_overwrite)
                        ) { _, _ ->
                            BackupRestoreUtils.saveBackup(requireActivity(), backupPath)
                            traverseStorage(currentPath)
                        }
                        overwriteDialog.show()
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
        fileFolderAdapter!!.notifyDataSetInvalidated()
        val contents: Array<File>?
        contents = if (isInRestore) {
            path!!.listFiles { dir ->
                (dir.name.toLowerCase(Locale.getDefault()).endsWith(".xml")
                        || dir.isDirectory && !dir.isFile)
            }
        } else {
            path!!.listFiles()
        }
        if (contents != null && contents.isNotEmpty()) {
            contents.forEach {
                // Don't show hidden (.dot) files/folders.
                if (!it.isHidden) {
                    fileFoldersList.add(FileFolder(it.name, it.isDirectory))
                }
            }
            fileFolderAdapter!!.notifyDataSetChanged()
        }

        fileFoldersList.sortWith(Comparator { f1, f2 ->
            if (f1.isFolder && !f2.isFolder) {
                -1
            } else if (!f1.isFolder && f2.isFolder) {
                1
            } else {
                f1.name.compareTo(f2.name, ignoreCase = true)
            }
        })
    }
}