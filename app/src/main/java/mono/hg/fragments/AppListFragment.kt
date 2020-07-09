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
import mono.hg.models.PinnedApp
import mono.hg.tasks.FetchAppsTask
import mono.hg.utils.AppUtils
import mono.hg.utils.UserUtils
import mono.hg.utils.Utils
import mono.hg.utils.ViewUtils
import mono.hg.views.CustomGridLayoutManager
import mono.hg.views.IndeterminateMaterialProgressBar
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
     * Progress bar shown when populating app list.
     */
    private lateinit var loadProgress: IndeterminateMaterialProgressBar

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAppListBinding.inflate(inflater, container, false)

        // Get a list of our hidden apps, default to null if there aren't any.
        excludedAppsList.addAll(PreferenceHelper.exclusionList)

        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (fetchAppsTask != null && fetchAppsTask!!.status == AsyncTask.Status.RUNNING) {
            fetchAppsTask!!.cancel(true)
        }

        unregisterBroadcast()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        manager = requireActivity().packageManager

        if (Utils.atLeastLollipop()) {
            launcherApps = requireActivity().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        }
        userUtils = UserUtils(requireContext())

        registerBroadcast()

        appsLayoutManager = if (PreferenceHelper.useGrid()) {
            CustomGridLayoutManager(requireContext(), resources.getInteger(R.integer.column_default_size))
        } else {
            TogglingLinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
        }
        val itemDecoration = ItemOffsetDecoration(requireContext(), R.dimen.item_offset)

        appsRecyclerView = binding!!.appsList
        loadProgress = binding!!.loadProgress

        appsRecyclerView.isDrawingCacheEnabled = true
        appsRecyclerView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_LOW
        appsRecyclerView.setHasFixedSize(true)
        appsRecyclerView.setThumbColor(PreferenceHelper.darkAccent)
        appsRecyclerView.setThumbInactiveColor(PreferenceHelper.accent)
        appsRecyclerView.setPopupBgColor(PreferenceHelper.darkerAccent)
        appsRecyclerView.adapter = appsAdapter
        appsRecyclerView.layoutManager = appsLayoutManager
        appsRecyclerView.itemAnimator = DefaultItemAnimator()
        if (PreferenceHelper.useGrid()) {
            appsRecyclerView.addItemDecoration(itemDecoration)
        }

        // Add item click action to app list.
        appsAdapter.addListener(FlexibleAdapter.OnItemClickListener { _, position ->
            appsAdapter.getItem(position)?.let { AppUtils.launchApp(requireActivity(), it) }
            true
        })

        // Add long click listener to apps in the apps list.
        // This shows a menu to manage the selected app.
        appsAdapter.addListener(FlexibleAdapter.OnItemLongClickListener { position ->
            val app = appsAdapter.getItem(position)

            // We need to rely on the LayoutManager here
            // because app list is populated asynchronously,
            // and will throw nulls if we try to directly ask RecyclerView for its child.
            appsRecyclerView.layoutManager!!.findViewByPosition(position)?.let { createAppMenu(it, app) }
        })

        appsAdapter.addListener(FlexibleAdapter.OnUpdateListener { size ->
            if (size > 0 && !appsAdapter.isEmpty) {
                loadProgress.visibility = View.GONE
                loadProgress.invalidate()
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
            if (fetchAppsTask != null) {
                fetchAppsTask?.cancel(true)
            }
            fetchAppsTask = FetchAppsTask(requireActivity(), appsAdapter, appsList)
            fetchAppsTask?.execute()
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
        return if (!appsAdapter.isEmpty) {
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
        val packageName = app!!.packageName
        val componentName = ComponentName.unflattenFromString(packageName)
        val pinApp = PinnedApp(app.packageName, app.user)
        val user = app.user

        val packageNameUri = Uri.fromParts("package", AppUtils.getPackageName(packageName), null)
        val shortcutMap = SparseArray<String>()
        val position = appsAdapter.getGlobalPositionOf(app)

        // Inflate the app menu.
        appMenu = PopupMenu(requireContext(), view)
        appMenu!!.menuInflater.inflate(R.menu.menu_app, appMenu!!.menu)
        appMenu!!.menu.addSubMenu(1, SHORTCUT_MENU_GROUP, 0, R.string.action_shortcuts)

        // Inflate app shortcuts.
        if (Utils.sdkIsAround(25)) {
            var menuId = SHORTCUT_MENU_GROUP
            AppUtils.getShortcuts(launcherApps, packageName)?.forEach {
                shortcutMap.put(menuId, it.id)
                appMenu!!.menu
                        .findItem(SHORTCUT_MENU_GROUP)
                        .subMenu
                        .add(SHORTCUT_MENU_GROUP, menuId, Menu.NONE, it.shortLabel)
                menuId++
            }
            if (shortcutMap.size() == 0) {
                appMenu!!.menu.getItem(0).isVisible = false
            }
        } else {
            appMenu!!.menu.getItem(0).isVisible = false
        }

        // Hide 'pin' if the app is already pinned or isPinned is set.
        appMenu!!.menu.findItem(R.id.action_pin).isVisible = !getLauncherActivity().isPinned(pinApp)

        // Only show the 'unpin' option if isPinned is set.
        appMenu!!.menu.findItem(R.id.action_unpin).isVisible = false

        // Show uninstall menu if the app is not a system app.
        appMenu!!.menu.findItem(R.id.action_uninstall).isVisible = (!AppUtils.isSystemApp(manager, packageName)
                && app.user == userUtils!!.currentSerial)

        appMenu!!.show()

        appMenu!!.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_pin -> {
                    getLauncherActivity().pinAppHere(app.userPackageName, user)
                }
                R.id.action_info -> if (Utils.atLeastLollipop()) {
                    launcherApps!!.startAppDetailsActivity(componentName,
                            userUtils!!.getUser(app.user), null, null)
                } else {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            packageNameUri))
                }
                R.id.action_uninstall -> startActivity(Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageNameUri))
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
                    if (item.groupId == SHORTCUT_MENU_GROUP && Utils.sdkIsAround(25)) {
                        userUtils!!.getUser(user)?.let {
                            launcherApps?.startShortcut(AppUtils.getPackageName(packageName),
                                    shortcutMap[item.itemId],
                                    null, null, it)
                        }
                    }
            }
            true
        }
    }

    private fun registerBroadcast() {
        // We want this activity to receive the package change broadcast,
        // since otherwise it won't be notified when there are changes to that.
        val filter = IntentFilter()
        filter.addAction("mono.hg.PACKAGE_CHANGE_BROADCAST")

        packageBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                fetchAppsTask!!.cancel(true)
                fetchAppsTask = FetchAppsTask(requireActivity(), appsAdapter, appsList)
                fetchAppsTask!!.execute()
            }
        }

        requireActivity().registerReceiver(packageBroadcastReceiver, filter)
    }

    private fun unregisterBroadcast() {
        if (packageBroadcastReceiver != null) {
            requireActivity().unregisterReceiver(packageBroadcastReceiver)
            packageBroadcastReceiver = null
        } else {
            Utils.sendLog(Utils.LogLevel.VERBOSE, "unregisterBroadcast() was called to a null receiver.")
        }
    }

    /**
     * Creates a dialog to set an app's shorthand.
     *
     * @param packageName The package name of the app.
     * @param position    Adapter position of the app.
     */
    private fun makeRenameDialog(packageName: String, position: Int) {
        val builder = AlertDialog.Builder(requireContext())
        val binding = LayoutRenameDialogBinding.inflate(layoutInflater)
        val renameField = binding.renameField
        ViewCompat.setBackgroundTintList(renameField, ColorStateList.valueOf(PreferenceHelper.accent))
        renameField.hint = PreferenceHelper.getLabel(packageName)
        builder.setView(binding.root)
        builder.setNegativeButton(android.R.string.cancel, null)
                .setTitle(R.string.dialog_title_shorthand)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val newLabel = renameField.text
                            .toString()
                            .replace("\\|".toRegex(), "")
                            .trim { it <= ' ' }

                    // Unset shorthand if it is empty.
                    PreferenceHelper.updateLabel(packageName, newLabel, newLabel.isEmpty())

                    // Update the specified item.
                    val app = appsAdapter.getItem(position)
                    if (app != null) {
                        app.hintName = newLabel
                    }
                }

        val themedDialog = builder.create()
        themedDialog.show()

        themedDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(PreferenceHelper.darkAccent)
        themedDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(PreferenceHelper.darkAccent)
    }

    companion object {
        private const val SHORTCUT_MENU_GROUP = 247
    }
}