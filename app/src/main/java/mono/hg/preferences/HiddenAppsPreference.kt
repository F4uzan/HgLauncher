package mono.hg.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import androidx.annotation.Keep
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mono.hg.R
import mono.hg.SettingsActivity
import mono.hg.adapters.HiddenAppAdapter
import mono.hg.databinding.FragmentHiddenAppsBinding
import mono.hg.databinding.UiLoadProgressBinding
import mono.hg.helpers.PreferenceHelper
import mono.hg.models.App
import mono.hg.utils.AppUtils
import java.util.*

/**
 * Preferences for the hidden apps list.
 */
@Keep
class HiddenAppsPreference : PreferenceFragmentCompat() {
    private var binding: FragmentHiddenAppsBinding? = null
    private var loaderBinding: UiLoadProgressBinding? = null
    private val appList = ArrayList<App>()
    private var hiddenAppAdapter: HiddenAppAdapter? = null
    private var excludedAppList = HashSet<String>()
    private lateinit var appsListView: ListView

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // No-op.
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHiddenAppsBinding.inflate(inflater, container, false)
        return binding !!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        excludedAppList =
            PreferenceHelper.preference.getStringSet("hidden_apps", HashSet()) as HashSet<String>

        appsListView = binding !!.hiddenAppsList
        hiddenAppAdapter = context?.let { HiddenAppAdapter(appList, it) }
        appsListView.adapter = hiddenAppAdapter

        // Get our app list.
        loadApps()
        addListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        PreferenceHelper.update("hidden_apps", excludedAppList)

        // We have been sent back. Set the action bar title accordingly.
        val actionBar = (requireActivity() as SettingsActivity).supportActionBar
        actionBar?.setTitle(R.string.pref_header_list)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        with(menu) {
            clear()
            add(0, 1, 100, getString(R.string.action_hidden_app_reset))
            getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            getItem(0).isVisible = excludedAppList.isNotEmpty()
            super.onCreateOptionsMenu(this, inflater)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.home -> {
                requireActivity().onBackPressed()
                true
            }
            1 -> {
                excludedAppList.clear()
                PreferenceHelper.update("hidden_apps", HashSet())

                // Recreate the toolbar menu to hide the 'restore all' button.
                requireActivity().invalidateOptionsMenu()

                // Reload the list.
                loadApps()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadApps() {
        CoroutineScope(Dispatchers.Main).launch {
            (requireActivity() as SettingsActivity).progressBar.show()
            withContext(Dispatchers.Default) {
                appList.clear()
                appList.addAll(AppUtils.loadApps(requireActivity(),
                    hideHidden = false,
                    shouldSort = false
                ))
            }
            hiddenAppAdapter?.notifyDataSetInvalidated()
            hiddenAppAdapter?.notifyDataSetChanged()
            (requireActivity() as SettingsActivity).progressBar.hide()
        }
    }

    private fun toggleHiddenState(position: Int) {
        val packageName = appList[position].userPackageName

        // Check if package is already in exclusion.
        if (excludedAppList.contains(packageName)) {
            excludedAppList.remove(packageName)
            PreferenceHelper.update("hidden_apps", excludedAppList)
        } else {
            excludedAppList.add(packageName)
            PreferenceHelper.update("hidden_apps", excludedAppList)
        }
        appList[position].isAppHidden = excludedAppList.contains(packageName)

        // Reload the app list!
        hiddenAppAdapter?.notifyDataSetChanged()

        // Toggle the state of the 'restore all' button.
        requireActivity().invalidateOptionsMenu()
    }

    private fun addListeners() {
        appsListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> toggleHiddenState(position) }
    }
}