package mono.hg

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.util.SparseArray
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.FlexibleAdapter.OnItemMoveListener
import eu.davidea.flexibleadapter.FlexibleAdapter.OnUpdateListener
import mono.hg.adapters.AppAdapter
import mono.hg.databinding.ActivityLauncherspaceBinding
import mono.hg.databinding.DialogStartHintBinding
import mono.hg.databinding.LayoutRenameDialogBinding
import mono.hg.fragments.WidgetsDialogFragment
import mono.hg.helpers.LauncherIconHelper
import mono.hg.helpers.PreferenceHelper
import mono.hg.listeners.GestureListener
import mono.hg.listeners.SimpleScrollListener
import mono.hg.models.App
import mono.hg.models.PinnedApp
import mono.hg.receivers.PackageChangesReceiver
import mono.hg.tasks.FetchAppsTask
import mono.hg.utils.*
import mono.hg.views.CustomGridLayoutManager
import mono.hg.views.DagashiBar
import mono.hg.views.IndeterminateMaterialProgressBar
import mono.hg.views.TogglingLinearLayoutManager
import mono.hg.wrappers.ItemOffsetDecoration
import mono.hg.wrappers.TextSpectator
import java.net.URLEncoder
import java.util.*

class LauncherActivity : AppCompatActivity() {
    /*
     * Binding for this activity.
     */
    private lateinit var binding: ActivityLauncherspaceBinding

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
    private var loadProgress: IndeterminateMaterialProgressBar? = null

    /*
     * Are we resuming this activity?
     */
    private var isResuming = false

    /*
     * Visibility of the favourites panel.
     */
    private var isFavouritesVisible = false

    /*
     * Visibility of the contextual button of search bar.
     */
    private var isContextVisible = false

    /*
     * Animation duration; fetched from system's duration.
     */
    private var animateDuration = 0

    /*
     * List containing pinned apps.
     */
    private val pinnedAppList = ArrayList<PinnedApp?>()

    /*
     * Adapter for pinned apps.
     */
    private val pinnedAppsAdapter = FlexibleAdapter(
            pinnedAppList)

    /*
     * List of excluded apps. These will not be shown in the app list.
     */
    private val excludedAppsList = HashSet<String>()

    /*
     * Package manager; casted through getPackageManager()``.
     */
    private lateinit var manager: PackageManager

    /*
     * LinearLayoutManager used in appsRecyclerView.
     */
    private lateinit var appsLayoutManager: RecyclerView.LayoutManager

    /*
     * RecyclerView for pinned apps; shown in favourites panel.
     */
    private lateinit var pinnedAppsRecyclerView: RecyclerView

    /*
     * Parent layout containing search bar.
     */
    private lateinit var searchContainer: LinearLayout

    /*
     * Parent layout of pinned apps' RecyclerView.
     */
    private lateinit var pinnedAppsContainer: FrameLayout

    /*
     * The search bar. Contained in searchContainer.
     */
    private lateinit var searchBar: EditText

    /*
     * Sliding up panel. Shows the app list when pulled down and
     * a parent to the other containers.
     */
    private lateinit var slidingHome: SlidingUpPanelLayout

    /*
     * CoordinatorLayout hosting the view visible when slidingHome is pulled down.
     */
    private lateinit var appsListContainer: CoordinatorLayout

    /*
     * Contextual button that changes depending on the availability of search text.
     */
    private lateinit var searchContext: ImageButton

    /*
     * A view used to intercept gestures and taps in the desktop.
     */
    private lateinit var touchReceiver: View

    /*
     * Menu shown when long-pressing apps.
     */
    private var appMenu: PopupMenu? = null

    /*
     * Receiver used to listen to installed/uninstalled packages.
     */
    private val packageReceiver = PackageChangesReceiver()
    private var launcherApps: LauncherApps? = null
    private var userUtils: UserUtils? = null
    private var fetchAppsTask: FetchAppsTask? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load preferences before setting layout.
        loadPref()
        binding = ActivityLauncherspaceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (requestedOrientation != PreferenceHelper.orientation) {
            requestedOrientation = PreferenceHelper.orientation
        }
        manager = packageManager
        appsLayoutManager = if (PreferenceHelper.useGrid()) {
            CustomGridLayoutManager(this,
                    resources.getInteger(R.integer.column_default_size))
        } else {
            TogglingLinearLayoutManager(this, LinearLayoutManager.VERTICAL,
                    true)
        }
        val itemDecoration = ItemOffsetDecoration(this, R.dimen.item_offset)
        val pinnedAppsManager = LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false)
        appsListContainer = binding.appListContainer
        searchContainer = binding.searchContainer.searchContainer
        pinnedAppsContainer = binding.pinnedAppsContainer
        searchBar = binding.searchContainer.search
        slidingHome = binding.slideHome
        touchReceiver = binding.touchReceiver
        appsRecyclerView = binding.appsList
        pinnedAppsRecyclerView = binding.pinnedAppsList
        searchContext = binding.searchContainer.searchContextButton
        loadProgress = binding.loadProgress
        if (Utils.atLeastLollipop()) {
            launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        }
        userUtils = UserUtils(this)
        animateDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

        // Let the launcher handle state of the sliding panel.
        slidingHome.disallowHiding(true)
        slidingHome.alwaysResetState(true)
        slidingHome.anchorPoint = 0f
        slidingHome.setDragView(searchContainer)
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
        pinnedAppsRecyclerView.adapter = pinnedAppsAdapter
        pinnedAppsRecyclerView.layoutManager = pinnedAppsManager
        pinnedAppsRecyclerView.itemAnimator = null
        pinnedAppsAdapter.isLongPressDragEnabled = true
        pinnedAppsAdapter.itemTouchHelperCallback.setMoveThreshold(1f)

        // Get icons from icon pack.
        if ("default" != PreferenceHelper.iconPackName &&
                LauncherIconHelper.loadIconPack(manager) == 0) {
            PreferenceHelper.editor?.putString("icon_pack", "default")?.apply()
        }

        // Start initialising listeners.
        addSearchBarTextListener()
        addSearchBarEditorListener()
        addGestureListener()
        addAdapterListener()
        addListListeners()
        addPanelListener()
        registerForContextMenu(touchReceiver)
        PreferenceHelper.update("package_count", AppUtils.countInstalledPackage(manager))

        // Start pinning apps.
        updatePinnedApps(true)
        applyPrefToViews()

        // Show the app list once all the views are set up.
        if (PreferenceHelper.keepAppList()) {
            doThis("show_panel")
        }
        if (PreferenceHelper.isNewUser) {
            showStartDialog()
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        menuInflater.inflate(R.menu.menu_main, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return onOptionsItemSelected(item)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivityForResult(Intent(this, SettingsActivity::class.java),
                        SETTINGS_RETURN_CODE)
                true
            }
            R.id.action_force_refresh -> {
                recreate()
                true
            }
            R.id.action_view_widgets -> {
                val widgetFragment = WidgetsDialogFragment()
                widgetFragment.show(supportFragmentManager, "Widgets Dialog")
                true
            }
            R.id.update_wallpaper -> {
                val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                startActivity(Intent.createChooser(intent, getString(R.string.action_wallpaper)))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        // Don't call super.onBackPressed because we don't want the launcher to close.

        // Hides the panel if back is pressed.
        doThis("dismiss_panel")
    }

    public override fun onPause() {
        super.onPause()

        // Dismiss any visible menu as well as the app panel when it is not needed.
        doThis("dismiss_menu")
        if (!PreferenceHelper.keepAppList()) {
            doThis("dismiss_panel")
        } else {
            // Clear the search bar text if app list is set to be kept open
            // unless keepLastSearch setting indicates maintain last search
            if (!PreferenceHelper.keepLastSearch()) {
                clearSearch()
            }
        }
        Utils.unregisterPackageReceiver(this, packageReceiver)
    }

    public override fun onResume() {
        super.onResume()

        // See if user has changed icon pack. Clear cache if true.
        if (PreferenceHelper.preference.getBoolean("require_refresh", false) ||
                PreferenceHelper.preference
                        .getString("icon_pack", "default") != PreferenceHelper.iconPackName) {
            LauncherIconHelper.refreshIcons()
        }

        // Refresh app list and pinned apps if there is a change in package count.
        if (AppUtils.hasNewPackage(
                        manager) || appsAdapter.hasFinishedLoading() && appsAdapter.isEmpty) {
            updatePinnedApps(true)
            fetchAppsTask!!.cancel(true)
            fetchAppsTask = FetchAppsTask(this, appsAdapter, appsList)
            fetchAppsTask!!.execute()
        }
        Utils.registerPackageReceiver(this, packageReceiver)

        // Show the app list when needed.
        if (PreferenceHelper.keepAppList()) {
            doThis("show_panel")
            searchContainer.visibility = View.VISIBLE
        } else if (Utils.sdkIsBelow(21) || ActivityServiceUtils.isPowerSaving(this)) {
            // HACK: For some reason, KitKat and below is always late setting visibility.
            // Manually set it here to make sure it's invisible.
            searchContainer.visibility = View.INVISIBLE
        }

        // Toggle back the refresh switch.
        PreferenceHelper.update("require_refresh", false)
        isResuming = true
    }

    public override fun onStart() {
        super.onStart()

        // Restart the launcher in case of an alien call.
        if (PreferenceHelper.wasAlien()) {
            PreferenceHelper.isAlien(false)
            recreate()
        }
        if (fetchAppsTask == null && appsAdapter.isEmpty) {
            fetchAppsTask = FetchAppsTask(this, appsAdapter, appsList)
            fetchAppsTask!!.execute()
        }

        // Reset the app list filter.
        appsAdapter.resetFilter()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (fetchAppsTask != null && fetchAppsTask!!.status == AsyncTask.Status.RUNNING) {
            fetchAppsTask!!.cancel(true)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        // See if any of the system bars needed hiding.
        if (Utils.atLeastKitKat()) {
            window.decorView.systemUiVisibility = ViewUtils.setWindowbarMode(PreferenceHelper.windowBarMode)
        } else if (Utils.sdkIsBelow(19) && PreferenceHelper.shouldHideStatusBar()) {
            window.decorView.systemUiVisibility = ViewUtils.setWindowbarMode("status")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Handle preference change. Refresh when necessary.
        if (requestCode == SETTINGS_RETURN_CODE && !PreferenceHelper.wasAlien()) {
            recreate()
        }

        // Call super to handle anything else not handled here.
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return if (event.action == KeyEvent.ACTION_DOWN && event.isCtrlPressed) {
            searchBar.let { Utils.handleInputShortcut(this, it, keyCode) }!!
        } else {
            if (keyCode == KeyEvent.KEYCODE_SPACE) {
                if (window.currentFocus !== searchBar) {
                    doThis("show_panel")
                }
                true
            } else {
                super.onKeyUp(keyCode, event)
            }
        }
    }

    /**
     * A shorthand for various toggles and visibility checks/sets.
     *
     * @param action What to do?
     */
    fun doThis(action: String?) {
        when (action) {
            "dismiss_menu" -> if (appMenu != null) {
                if (appMenu!!.menu.findItem(R.id.action_app_actions) != null) {
                    appMenu!!.menu.findItem(R.id.action_app_actions).subMenu.close()
                }
                if (appMenu!!.menu.findItem(SHORTCUT_MENU_GROUP) != null) {
                    appMenu!!.menu.findItem(SHORTCUT_MENU_GROUP).subMenu.close()
                }
                appMenu!!.dismiss()
            }
            "show_panel" -> slidingHome.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED,
                    ActivityServiceUtils.isPowerSaving(this))
            "dismiss_panel" -> slidingHome.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED,
                    ActivityServiceUtils.isPowerSaving(this))
            "show_favourites" -> pinnedAppsContainer.animate()
                    .translationY(0f)
                    .setInterpolator(LinearOutSlowInInterpolator())
                    .setDuration(225)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            isFavouritesVisible = true
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            isFavouritesVisible = false
                        }
                    })
            "hide_favourites" -> pinnedAppsContainer.animate()
                    .translationY(pinnedAppsContainer.measuredHeight.toFloat())
                    .setInterpolator(FastOutLinearInInterpolator())
                    .setDuration(175)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            isFavouritesVisible = false
                        }
                    })
            "show_context_button" -> searchContext.animate()
                    .translationX(0f)
                    .setInterpolator(LinearOutSlowInInterpolator())
                    .setDuration(200)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            isContextVisible = true
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            isContextVisible = false
                        }
                    })
            "hide_context_button" -> searchContext.animate()
                    .translationX(searchContext.measuredWidth.toFloat())
                    .setInterpolator(FastOutLinearInInterpolator())
                    .setDuration(150)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            isContextVisible = false
                        }
                    })
            else -> {
            }
        }
    }

    /**
     * Modifies various views parameters and visibility based on the user preferences.
     */
    private fun applyPrefToViews() {
        // Workaround v21+ status bar transparency issue.
        // This is disabled if the status bar is hidden.
        if (Utils.atLeastLollipop()
                && (PreferenceHelper.windowBarMode == "none" || PreferenceHelper.windowBarMode == "nav")) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            val homeParams = slidingHome.layoutParams as MarginLayoutParams
            homeParams.topMargin = ViewUtils.statusBarHeight
        }
        slidingHome.post { // Hide the favourites panel when there's nothing to show.
            if (pinnedAppsAdapter.isEmpty) {
                pinnedAppsContainer.translationY = pinnedAppsContainer.measuredHeight.toFloat()
                isFavouritesVisible = false
            } else {
                isFavouritesVisible = true
            }
        }

        // Switch on wallpaper shade.
        if (PreferenceHelper.useWallpaperShade()) {
            // Tints the navigation bar with a semi-transparent shade.
            if (Utils.atLeastLollipop()) {
                window.navigationBarColor = resources.getColor(R.color.navigationBarShade)
            }
            binding.wallpaperShade.setBackgroundResource(R.drawable.image_inner_shadow)
        }
        if ("transparent" == PreferenceHelper.listBackground) {
            appsListContainer.setBackgroundColor(
                    Utils.getColorFromAttr(this, R.attr.backgroundColorAlt))
        } else if ("none" == PreferenceHelper.listBackground) {
            appsListContainer.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    /**
     * Loads available preferences and updates PreferenceHelpers.
     */
    private fun loadPref() {
        if (!PreferenceHelper.hasEditor()) {
            PreferenceHelper.initPreference(this)
        }
        PreferenceHelper.fetchPreference()

        // Get pinned apps.
        pinnedAppString = PreferenceHelper.preference.getString("pinned_apps_list", "").toString()

        // Get a list of our hidden apps, default to null if there aren't any.
        excludedAppsList.addAll(PreferenceHelper.exclusionList)

        // Get the default providers list if it's empty.
        if (PreferenceHelper.providerList.isEmpty()) {
            Utils.setDefaultProviders(resources)
        }
        when (PreferenceHelper.appTheme()) {
            "auto" -> if (Utils.atLeastQ()) {
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            } else {
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            }
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                setTheme(R.style.LauncherTheme_Dark)
            }
            "black" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> if (Utils.atLeastQ()) {
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            } else {
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            }
        }
    }

    /**
     * Creates a PopupMenu to use in a long-pressed app object.
     *
     * @param view     View for the PopupMenu to anchor to.
     * @param isPinned Is this a pinned app?
     * @param app      App object selected from the list.
     */
    private fun createAppMenu(view: View?, isPinned: Boolean, app: App?) {
        val packageName = app!!.packageName
        val componentName = ComponentName.unflattenFromString(packageName)
        val user = app.user
        val pinApp = PinnedApp(app.packageName, app.user)
        val packageNameUri = Uri.fromParts("package", AppUtils.getPackageName(packageName),
                null)
        val shortcutMap = SparseArray<String>()
        val position: Int = if (isPinned) {
            pinnedAppsAdapter.getGlobalPositionOf(app)
        } else {
            appsAdapter.getGlobalPositionOf(app)
        }

        // Inflate the app menu.
        appMenu = PopupMenu(this@LauncherActivity, view!!)
        appMenu!!.menuInflater.inflate(R.menu.menu_app, appMenu!!.menu)
        appMenu!!.menu.addSubMenu(1, SHORTCUT_MENU_GROUP, 0, R.string.action_shortcuts)

        // Inflate app shortcuts.
        if (Utils.sdkIsAround(25)) {
            var menuId = SHORTCUT_MENU_GROUP
            for (shortcutInfo in AppUtils.getShortcuts(launcherApps, packageName)!!) {
                shortcutMap.put(menuId, shortcutInfo.id)
                appMenu!!.menu
                        .findItem(SHORTCUT_MENU_GROUP)
                        .subMenu
                        .add(SHORTCUT_MENU_GROUP, menuId, Menu.NONE, shortcutInfo.shortLabel)
                menuId++
            }
            if (shortcutMap.size() == 0) {
                appMenu!!.menu.getItem(0).isVisible = false
            }
        } else {
            appMenu!!.menu.getItem(0).isVisible = false
        }

        // Hide 'pin' if the app is already pinned or isPinned is set.
        appMenu!!.menu.findItem(R.id.action_pin).isVisible = (!isPinned
                && !pinnedAppsAdapter.contains(pinApp))

        // We can't hide an app from the favourites panel.
        appMenu!!.menu.findItem(R.id.action_hide).isVisible = !isPinned
        appMenu!!.menu.findItem(R.id.action_shorthand).isVisible = !isPinned

        // Only show the 'unpin' option if isPinned is set.
        appMenu!!.menu.findItem(R.id.action_unpin).isVisible = isPinned

        // Show uninstall menu if the app is not a system app.
        appMenu!!.menu.findItem(R.id.action_uninstall).isVisible = (!AppUtils.isSystemApp(manager, packageName)
                && app.user == userUtils!!.currentSerial)
        appMenu!!.show()
        appMenu!!.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_pin -> {
                    AppUtils.pinApp(this@LauncherActivity, user, packageName, pinnedAppsAdapter,
                            pinnedAppList)
                    pinnedAppString = pinnedAppString + app.userPackageName + ";"
                    PreferenceHelper.update("pinned_apps_list", pinnedAppString)
                }
                R.id.action_unpin -> {
                    pinnedAppList.remove(pinnedAppsAdapter.getItem(position))
                    pinnedAppsAdapter.removeItem(position)
                    pinnedAppString = pinnedAppString.replace(app.userPackageName + ";",
                            "")
                    PreferenceHelper.update("pinned_apps_list", pinnedAppString)
                    if (pinnedAppsAdapter.isEmpty) {
                        doThis("hide_favourites")
                    }
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

    /**
     * Listeners for touch receivers.
     */
    private fun addGestureListener() {
        // Handle touch events in touchReceiver.
        touchReceiver.setOnTouchListener(object : GestureListener(this@LauncherActivity) {
            override fun onGesture(direction: Int) {
                if (slidingHome.panelState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    Utils.handleGestureActions(this@LauncherActivity, direction)
                }
            }

            override fun onLongPress() {
                // Show context menu when touchReceiver is long pressed when the panel is expanded.
                if (slidingHome.panelState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    touchReceiver.showContextMenu()
                }
            }
        })
    }

    /**
     * Listeners for the search bar query.
     */
    private fun addSearchBarTextListener() {
        // Implement listener for the search bar.
        searchBar.addTextChangedListener(object : TextSpectator(searchBar) {
            var searchHint: String = ""
            var searchSnack = DagashiBar.make(appsListContainer, searchHint,
                    DagashiBar.LENGTH_INDEFINITE, false).setTextColor(PreferenceHelper.accent)

            override fun whenTimerTicked() {
                super.whenTimerTicked()
                if (trimmedInputText.isEmpty()) {
                    // HACK: Hide the view stub.
                    if (pinnedAppsAdapter.isEmpty) {
                        doThis("hide_favourites")
                    }
                    if (isContextVisible) {
                        doThis("hide_context_button")
                    }
                    appsAdapter.resetFilter()
                    searchSnack.dismiss()
                    stopTimer()
                } else {
                    // Begin filtering our list.
                    appsAdapter.setFilter(trimmedInputText)
                    appsAdapter.filterItems()
                }
            }

            override fun whenChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                super.whenChanged(s, start, before, count)

                // Text used for searchSnack.
                searchHint = String.format(resources.getString(R.string.search_web_hint),
                        inputText)
            }

            override fun afterChanged(s: Editable?) {
                super.afterChanged(s)
                startTimer()
                if (trimmedInputText.isNotEmpty() && PreferenceHelper.promptSearch()) {
                    // HACK: Show a view stub to make sure app list anchors properly.
                    doThis("show_favourites")

                    // Update the snackbar text.
                    searchSnack.setText(searchHint)
                    if (!isContextVisible) {
                        doThis("show_context_button")
                    }
                    val searchSnackAction: String = if (PreferenceHelper.searchProvider == "none") {
                        getString(R.string.search_web_button_prompt)
                    } else {
                        getString(R.string.search_web_button)
                    }

                    // Prompt user if they want to search their query online.
                    searchSnack.setNonDismissAction(searchSnackAction, View.OnClickListener {
                        if (PreferenceHelper.searchProvider != "none") {
                            Utils.doWebSearch(this@LauncherActivity,
                                    PreferenceHelper.searchProvider,
                                    URLEncoder.encode(trimmedInputText))
                            searchSnack.dismiss()
                        } else {
                            appMenu = PopupMenu(this@LauncherActivity, it)
                            ViewUtils.createSearchMenu(this@LauncherActivity, appMenu!!,
                                    URLEncoder.encode(trimmedInputText))
                        }
                    })
                    if (PreferenceHelper.extendedSearchMenu() && PreferenceHelper.searchProvider != "none") {
                        searchSnack.setLongPressAction(View.OnLongClickListener {
                            appMenu = PopupMenu(this@LauncherActivity, it)
                            ViewUtils.createSearchMenu(this@LauncherActivity, appMenu!!,
                                    URLEncoder.encode(trimmedInputText))
                            true
                        })
                    }
                }
            }
        })
    }

    /**
     * Listener for search bar editor (keyboard) action.
     */
    private fun addSearchBarEditorListener() {
        searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (searchBar.text.isNotEmpty()
                    && (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_NULL)) {
                if (!appsAdapter.isEmpty) {
                    appsRecyclerView.let {
                        ViewUtils.keyboardLaunchApp(this@LauncherActivity, it, appsAdapter)
                    }
                } else if (PreferenceHelper.promptSearch()
                        && PreferenceHelper.searchProvider != "none") {
                    Utils.doWebSearch(this@LauncherActivity,
                            PreferenceHelper.searchProvider,
                            searchBar.text.toString())
                }
            }
            true
        }
    }

    /**
     * Listeners for the app list.
     */
    private fun addListListeners() {
        // Listen for app list scroll to hide/show favourites panel.
        // Only do this when the user has favourites panel enabled.
        appsRecyclerView.addOnScrollListener(object : SimpleScrollListener(48) {
            override fun onScrollUp() {
                if (!pinnedAppsAdapter.isEmpty
                        && isFavouritesVisible
                        && PreferenceHelper.favouritesAcceptScroll()
                        && searchBar.text.toString().isEmpty()) {
                    doThis("hide_favourites")
                }
            }

            override fun onScroll() {
                doThis("dismiss_menu")
            }

            override fun onEnd() {
                if (!pinnedAppsAdapter.isEmpty
                        && !isFavouritesVisible
                        && PreferenceHelper.favouritesAcceptScroll()) {
                    doThis("show_favourites")
                }
            }
        })

        // Add item click action to app list.
        appsAdapter.addListener(FlexibleAdapter.OnItemClickListener { _, position ->
            appsAdapter.getItem(position)?.let { AppUtils.launchApp(this@LauncherActivity, it) }
            true
        })

        // Add item click action to the favourites panel.
        pinnedAppsAdapter.addListener(FlexibleAdapter.OnItemClickListener { _, position ->
            pinnedAppsAdapter.getItem(position)?.let { AppUtils.launchApp(this@LauncherActivity, it) }
            true
        })

        // Add long click listener to apps in the apps list.
        // This shows a menu to manage the selected app.
        appsAdapter.addListener(FlexibleAdapter.OnItemLongClickListener { position ->
            val app = appsAdapter.getItem(position)

            // We need to rely on the LayoutManager here
            // because app list is populated asynchronously,
            // and will throw nulls if we try to directly ask RecyclerView for its child.
            createAppMenu(appsRecyclerView.layoutManager!!.findViewByPosition(position), false, app)
        })
        appsAdapter.addListener(OnUpdateListener { size ->
            if (size > 0 && !appsAdapter.isEmpty) {
                loadProgress!!.visibility = View.GONE
                loadProgress!!.invalidate()
            }
        })
    }

    /**
     * Listener for adapters.
     * TODO: Maybe this can be moved to ListListener (or that can go here instead)?
     */
    private fun addAdapterListener() {
        pinnedAppsAdapter.addListener(object : OnItemMoveListener {
            var newState = 0
            var startTime: Long = 0
            override fun shouldMoveItem(fromPosition: Int, toPosition: Int): Boolean {
                return true
            }

            override fun onItemMove(fromPosition: Int, toPosition: Int) {
                startTime = System.currentTimeMillis()

                // Close app menu when we're dragging.
                doThis("dismiss_menu")

                // Shuffle our apps around.
                pinnedAppsAdapter.swapItems(pinnedAppList, fromPosition, toPosition)
                pinnedAppsAdapter.notifyItemMoved(fromPosition, toPosition)
            }

            override fun onActionStateChanged(viewHolder: RecyclerView.ViewHolder, actionState: Int) {
                // FIXME: Work out a better touch detection.
                // No movement occurred, this is a long press.
                if (newState != ItemTouchHelper.ACTION_STATE_DRAG && System.currentTimeMillis() - startTime == System
                                .currentTimeMillis()) {
                    val app: App? = pinnedAppsAdapter.getItem(viewHolder.absoluteAdapterPosition)

                    // Use LayoutManager method to get the view,
                    // as RecyclerView will happily return null if it can.
                    createAppMenu(pinnedAppsRecyclerView.layoutManager!!.findViewByPosition(viewHolder.absoluteAdapterPosition), true, app)
                } else {
                    // Reset startTime and update the pinned apps, we were swiping.
                    startTime = 0
                    updatePinnedApps(false)
                }
                newState = actionState
            }
        })
    }

    /**
     * Listeners for the app panel.
     */
    private fun addPanelListener() {
        slidingHome.addPanelSlideListener(object : PanelSlideListener {
            override fun onPanelSlide(view: View, v: Float) {
                // Dismiss any visible menu.
                doThis("dismiss_menu")
            }

            override fun onPanelStateChanged(panel: View, previousState: Int, newState: Int) {
                searchBar.isClickable = newState == SlidingUpPanelLayout.PanelState.COLLAPSED
                searchBar.isLongClickable = newState == SlidingUpPanelLayout.PanelState.COLLAPSED
                when (newState) {
                    SlidingUpPanelLayout.PanelState.DRAGGING -> {
                        // Empty out search bar text
                        // Clear the search bar text if app list is set to be kept open
                        // unless keepLastSearch setting indicates maintain last search
                        if (!PreferenceHelper.keepLastSearch()) {
                            clearSearch()
                        }

                        // Animate search container entering the view.
                        if (!ActivityServiceUtils.isPowerSaving(this@LauncherActivity)) {
                            searchContainer.animate().alpha(1f).setDuration(animateDuration.toLong())
                                    .setListener(object : AnimatorListenerAdapter() {
                                        override fun onAnimationStart(animation: Animator) {
                                            searchContainer.visibility = View.VISIBLE
                                        }

                                        override fun onAnimationEnd(animation: Animator) {
                                            searchContainer.clearAnimation()
                                        }
                                    })
                        } else {
                            searchContainer.visibility = View.VISIBLE
                        }
                    }
                    SlidingUpPanelLayout.PanelState.COLLAPSED ->                         // Show the keyboard.
                        if (PreferenceHelper.shouldFocusKeyboard()
                                && previousState == SlidingUpPanelLayout.PanelState.DRAGGING) {
                            ActivityServiceUtils.showSoftKeyboard(this@LauncherActivity, searchBar)
                        }
                    SlidingUpPanelLayout.PanelState.EXPANDED -> {
                        // Hide keyboard if container is invisible.
                        ActivityServiceUtils.hideSoftKeyboard(this@LauncherActivity)

                        // Stop scrolling, the panel is being dismissed.
                        appsRecyclerView.stopScroll()
                        searchContainer.visibility = View.INVISIBLE

                        // Animate the container.
                        if (!isResuming && !ActivityServiceUtils.isPowerSaving(this@LauncherActivity)) {
                            searchContainer.animate().alpha(0f).duration = animateDuration.toLong()
                        } else {
                            isResuming = false
                        }
                    }
                    SlidingUpPanelLayout.PanelState.ANCHORED ->                         // Please don't anchor, we don't want it.
                        if (previousState != SlidingUpPanelLayout.PanelState.DRAGGING) {
                            slidingHome.setPanelState(previousState,
                                    ActivityServiceUtils.isPowerSaving(this@LauncherActivity))
                        } else {
                            slidingHome.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED,
                                    ActivityServiceUtils.isPowerSaving(this@LauncherActivity))
                        }
                    else -> {
                    }
                }
            }
        })
    }

    /**
     * Updates the favourites panel.
     *
     * @param restart Should a complete adapter & list re-initialisation be done?
     */
    private fun updatePinnedApps(restart: Boolean) {
        var newAppString = ""
        if (pinnedAppString.isNotEmpty() && restart) {
            pinnedAppList.clear()
            pinnedAppsAdapter.updateDataSet(pinnedAppList, false)
            for (pinnedApp in pinnedAppString.split(";".toRegex()).toTypedArray()) {
                var componentName = pinnedApp
                var user = userUtils!!.currentSerial

                // Handle pinned apps coming from another user.
                val userSplit = pinnedApp.split("-".toRegex()).toTypedArray()
                if (userSplit.size == 2) {
                    user = userSplit[0].toLong()
                    componentName = userSplit[1]
                }
                if (AppUtils.doesComponentExist(manager, componentName)) {
                    AppUtils.pinApp(this, user, componentName, pinnedAppsAdapter, pinnedAppList)
                }
            }
        }

        // Iterate through the list to get package name of each pinned apps, then stringify them.
        for (app in pinnedAppList) {
            newAppString = newAppString + app!!.userPackageName + ";"
        }

        // Update the saved pinned apps.
        PreferenceHelper.update("pinned_apps_list", newAppString)
        pinnedAppString = newAppString
    }

    fun clearSearch() {
        // Clear the search bar text if app list is set to be kept open
        searchBar.setText("")
    }

    private fun showStartDialog() {
        val binding = DialogStartHintBinding.inflate(layoutInflater)
        val startDialog = BottomSheetDialog(this)
        startDialog.setContentView(binding.root)
        startDialog.setCancelable(false)
        startDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        val startDismiss = binding.dismiss
        startDismiss.setOnClickListener {
            startDialog.dismiss()
            PreferenceHelper.update("is_new_user", false)
        }
        startDialog.show()
    }

    /**
     * Creates a dialog to set an app's shorthand.
     *
     * @param packageName The package name of the app.
     * @param position    Adapter position of the app.
     */
    private fun makeRenameDialog(packageName: String, position: Int) {
        val builder = AlertDialog.Builder(this)
        val binding = LayoutRenameDialogBinding.inflate(layoutInflater)
        val renameField = binding.renameField
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
                }.show()
    }

    companion object {
        private const val SETTINGS_RETURN_CODE = 12
        private const val SHORTCUT_MENU_GROUP = 247

        /*
         * String containing pinned apps. Delimited by a semicolon (;).
         */
        private lateinit var pinnedAppString: String
    }
}