package mono.hg.fragments

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import mono.hg.R
import mono.hg.adapters.AppAdapter
import mono.hg.databinding.FragmentAppListBinding
import mono.hg.databinding.LayoutRenameDialogBinding
import mono.hg.helpers.PreferenceHelper
import mono.hg.listeners.SimpleScrollListener
import mono.hg.models.App
import mono.hg.tasks.FetchAppsTask
import mono.hg.utils.AppUtils
import mono.hg.utils.UserUtils
import mono.hg.utils.Utils
import mono.hg.utils.ViewUtils
import mono.hg.views.CustomGridLayoutManager
import mono.hg.views.TogglingLinearLayoutManager
import mono.hg.wrappers.ItemOffsetDecoration
import java.util.*

/**
 * Page displaying an app list.
 * This is the generic implementation of an app list that handles the required features.
 */
class AppListFragment : GenericPageFragment() {
    /*
     * List containing installed apps.
     */
    private val appsList = ArrayList<App?>()

    /*
     * Adapter for installed apps.
     */
    private val appsAdapter = AppAdapter(appsList)

    /*
     * RecyclerView for app list.
     */
    private lateinit var appsRecyclerView: FastScrollRecyclerView

    /*
    * List of excluded apps. These will not be shown in the app list.
    */
    private val excludedAppsList = HashSet<String>()

    /*
     * Package manager; casted through getPackageManager().
     */
    private lateinit var manager: PackageManager

    /*
     * LinearLayoutManager used in appsRecyclerView.
     */
    private lateinit var appsLayoutManager: RecyclerView.LayoutManager

    /*
    * Menu shown when long-pressing apps.
    */
    private var appMenu: PopupMenu? = null

    /*
     * BroadcastReceiver used to receive package changes notification from LauncherActivity.
     */
    private var packageBroadcastReceiver: BroadcastReceiver? = null

    private var launcherApps: LauncherApps? = null
    private var userUtils: UserUtils? = null
    private var fetchAppsTask: FetchAppsTask? = null
    private var binding: FragmentAppListBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAppListBinding.inflate(inflater, container, false)

        // Get a list of our hidden apps, default to null if there aren't any.
        excludedAppsList.addAll(PreferenceHelper.exclusionList)

        return binding !!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (fetchAppsTask != null && fetchAppsTask?.status == AsyncTask.Status.RUNNING) {
            fetchAppsTask?.cancel(true)
            fetchAppsTask = null
        }

        unregisterBroadcast()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        manager = requireActivity().packageManager

        if (Utils.atLeastLollipop()) {
            launcherApps =
                requireActivity().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        }
        userUtils = UserUtils(requireContext())

        registerBroadcast()

        appsLayoutManager = if (PreferenceHelper.useGrid()) {
            CustomGridLayoutManager(
                requireContext(),
                resources.getInteger(R.integer.column_default_size)
            )
        } else {
            TogglingLinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
        }
        val itemDecoration = ItemOffsetDecoration(requireContext(), R.dimen.item_offset)

        appsRecyclerView = binding !!.appsList.apply {
            isDrawingCacheEnabled = true
            drawingCacheQuality = View.DRAWING_CACHE_QUALITY_LOW
            setHasFixedSize(true)
            setThumbColor(PreferenceHelper.darkAccent)
            setThumbInactiveColor(PreferenceHelper.accent)
            setPopupBgColor(PreferenceHelper.darkerAccent)
            adapter = appsAdapter
            layoutManager = appsLayoutManager
            itemAnimator = DefaultItemAnimator()
            if (PreferenceHelper.useGrid()) {
                addItemDecoration(itemDecoration)
            }
        }

        // Add long click listener to apps in the apps list.
        // This shows a menu to manage the selected app.
        appsAdapter.addListener(FlexibleAdapter.OnItemLongClickListener { position ->
            val app = appsAdapter.getItem(position)

            appsRecyclerView.findViewHolderForLayoutPosition(position)?.itemView?.let {
                createAppMenu(
                    it,
                    app
                )
            }
        })

        appsAdapter.addListener(FlexibleAdapter.OnUpdateListener { size ->
            if (size > 0 && ! appsAdapter.isEmpty) {
                binding !!.loadProgress.hide()
            } else {
                binding !!.loadProgress.show()
            }
        })

        // Listen for app list scroll to hide/show favourites panel.
        // Only do this when the user has favourites panel enabled.
        appsRecyclerView.addOnScrollListener(object : SimpleScrollListener(48) {
            override fun onScrollUp() {
                getLauncherActivity().hidePinnedApps()
            }

            override fun onScroll() {
                appMenu?.menu?.findItem(R.id.action_app_actions)?.subMenu?.close()
                appMenu?.menu?.findItem(SHORTCUT_MENU_GROUP)?.subMenu?.close()
                appMenu?.dismiss()
            }

            override fun onEnd() {
                getLauncherActivity().showPinnedApps()
            }
        })
    }

    override fun onStart() {
        super.onStart()

        if (AppUtils.hasNewPackage(manager) || appsAdapter.isEmpty) {
            fetchApps()
        }

        // Reset the app list filter.
        appsAdapter.resetFilter()
    }

    override fun isAcceptingSearch(): Boolean {
        return true
    }

    override fun commitSearch(query: String) {
        appsAdapter.setFilter(query)
        appsAdapter.filterItems()
    }

    override fun resetSearch() {
        appsAdapter.resetFilter()
    }

    override fun launchPreselection(): Boolean {
        return if (! appsAdapter.isEmpty) {
            appsRecyclerView.let {
                ViewUtils.keyboardLaunchApp(requireActivity(), it, appsAdapter)
            }
            true
        } else {
            false
        }
    }

    /**
     * Creates a PopupMenu to use in a long-pressed app object.
     *
     * @param view     View for the PopupMenu to anchor to.
     * @param app      App object selected from the list.
     */
    private fun createAppMenu(view: View, app: App?) {
        val packageName = app !!.packageName
        val user = app.user

        val packageNameUri = Uri.fromParts("package", AppUtils.getPackageName(packageName), null)
        val shortcutMap = SparseArray<String>()
        val position = appsAdapter.getGlobalPositionOf(app)

        // Inflate the app menu.
        appMenu = PopupMenu(requireContext(), view)
        appMenu !!.menuInflater.inflate(R.menu.menu_app, appMenu !!.menu)
        appMenu !!.menu.addSubMenu(1, SHORTCUT_MENU_GROUP, 0, R.string.action_shortcuts)

        // Hide 'pin' if the app is already pinned or isPinned is set.
        appMenu !!.menu.findItem(R.id.action_pin).isVisible =
            ! getLauncherActivity().isPinned(app)

        // Only show the 'unpin' option if isPinned is set.
        appMenu !!.menu.findItem(R.id.action_unpin).isVisible = false

        // Show uninstall menu if the app is not a system app.
        appMenu !!.menu.findItem(R.id.action_uninstall).isVisible =
            (! AppUtils.isSystemApp(manager, packageName)
                    && app.user == userUtils !!.currentSerial)

        // Inflate app shortcuts.
        if (Utils.sdkIsAround(25)) {
            var menuId = SHORTCUT_MENU_GROUP
            AppUtils.getShortcuts(launcherApps, packageName)?.forEach {
                shortcutMap.put(menuId, it.id)
                appMenu !!.menu
                    .findItem(SHORTCUT_MENU_GROUP)
                    .subMenu
                    .add(SHORTCUT_MENU_GROUP, menuId, Menu.NONE, it.shortLabel)
                menuId ++
            }
            if (shortcutMap.size() == 0) {
                appMenu !!.menu.getItem(0).isVisible = false
            }
        } else {
            appMenu !!.menu.getItem(0).isVisible = false
        }

        appMenu !!.show()

        appMenu !!.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_pin -> {
                    getLauncherActivity().pinAppHere(app.userPackageName, user)
                }
                R.id.action_info -> AppUtils.openAppDetails(requireActivity(), packageName, user)
                R.id.action_uninstall -> AppUtils.uninstallApp(requireActivity(), packageNameUri)
                R.id.action_shorthand -> makeRenameDialog(app.userPackageName, position)
                R.id.action_hide -> {
                    // Add the app's package name to the exclusion list.
                    excludedAppsList.add(app.userPackageName)
                    PreferenceHelper.update("hidden_apps", excludedAppsList)

                    // Reload the app list!
                    appsList.remove(appsAdapter.getItem(position))
                    appsAdapter.removeItem(position)
                }
                else ->                         // Catch click actions from the shortcut menu group.
                    if (item.groupId == SHORTCUT_MENU_GROUP) {
                        userUtils?.getUser(user)?.let {
                            AppUtils.launchShortcut(
                                it,
                                launcherApps,
                                packageName,
                                shortcutMap[item.itemId]
                            )
                        }
                    }
            }
            true
        }
    }

    private fun registerBroadcast() {
        packageBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val isRemoving =
                    intent.getStringExtra("action") == "android.intent.action.PACKAGE_REMOVED"
                val launchIntent = intent.getStringExtra("package")?.let {
                    requireActivity().packageManager.getLaunchIntentForPackage(
                        it
                    )
                }

                if (launchIntent != null) {
                    val hasLauncherCategory = launchIntent.hasCategory(Intent.CATEGORY_LAUNCHER)

                    if (hasLauncherCategory) {
                        fetchApps()
                    }
                } else if (isRemoving) {
                    // Apps being uninstalled will have no launch intent,
                    // therefore it's better if we get the entire list again.
                    fetchApps()
                }

                // We should recount here, regardless of whether we update the list or not.
                PreferenceHelper.update(
                    "package_count",
                    AppUtils.countInstalledPackage(requireActivity().packageManager)
                )
            }
        }

        // We want this fragment to receive the package change broadcast,
        // since otherwise it won't be notified when there are changes to that.
        IntentFilter().apply {
            addAction("mono.hg.PACKAGE_CHANGE_BROADCAST")
        }.also {
            requireActivity().registerReceiver(packageBroadcastReceiver, it)
        }
    }

    private fun unregisterBroadcast() {
        if (packageBroadcastReceiver != null) {
            requireActivity().unregisterReceiver(packageBroadcastReceiver)
            packageBroadcastReceiver = null
        } else {
            Utils.sendLog(
                Utils.LogLevel.VERBOSE,
                "unregisterBroadcast() was called to a null receiver."
            )
        }
    }

    private fun fetchApps() {
        if (fetchAppsTask == null || fetchAppsTask?.status != AsyncTask.Status.RUNNING) {
            fetchAppsTask = FetchAppsTask(requireActivity(), appsAdapter, appsList)
            fetchAppsTask?.execute()
        }
    }

    /**
     * Creates a dialog to set an app's shorthand.
     *
     * @param packageName The package name of the app.
     * @param position    Adapter position of the app.
     */
    private fun makeRenameDialog(packageName: String, position: Int) {
        val binding = LayoutRenameDialogBinding.inflate(layoutInflater)
        val renameField = binding.renameField.apply {
            ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(PreferenceHelper.accent))
            hint = PreferenceHelper.getLabel(packageName)
        }

        with(AlertDialog.Builder(requireContext())) {
            setView(binding.root)
            setNegativeButton(android.R.string.cancel, null)
            setTitle(R.string.dialog_title_shorthand)
            setPositiveButton(android.R.string.ok) { _, _ ->
                val newLabel = renameField.text
                    .toString()
                    .replace("\\|".toRegex(), "")
                    .trim { it <= ' ' }

                // Unset shorthand if it is empty.
                PreferenceHelper.updateLabel(packageName, newLabel, newLabel.isEmpty())

                // Update the specified item.
                appsAdapter.getItem(position).apply {
                    this?.hintName = newLabel
                }
            }

            create().apply {
                show()
                getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(PreferenceHelper.darkAccent)
                getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(PreferenceHelper.darkAccent)
            }
        }
    }

    companion object {
        private const val SHORTCUT_MENU_GROUP = 247
    }
}