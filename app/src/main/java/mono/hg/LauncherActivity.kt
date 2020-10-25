package mono.hg

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.SparseArray
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener
import eu.davidea.flexibleadapter.FlexibleAdapter.OnItemMoveListener
import mono.hg.adapters.AppAdapter
import mono.hg.adapters.PageAdapter
import mono.hg.databinding.ActivityLauncherspaceBinding
import mono.hg.databinding.DialogStartHintBinding
import mono.hg.helpers.LauncherIconHelper
import mono.hg.helpers.PreferenceHelper
import mono.hg.listeners.GestureListener
import mono.hg.models.App
import mono.hg.receivers.PackageChangesReceiver
import mono.hg.utils.ActivityServiceUtils
import mono.hg.utils.AppUtils
import mono.hg.utils.UserUtils
import mono.hg.utils.Utils
import mono.hg.utils.ViewUtils
import mono.hg.views.DagashiBar
import mono.hg.wrappers.TextSpectator
import java.net.URLEncoder

/**
 * The launcher itself.
 *
 * Contains all the very things seen when the user starts and interacts with the launcher.
 */
class LauncherActivity : AppCompatActivity() {
    /*
     * Binding for this activity.
     */
    private lateinit var binding: ActivityLauncherspaceBinding

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
     * Whether a Page has requested a panel lock.
     */
    private var panelLockRequested = true

    /*
     * Animation duration; fetched from system's duration.
     */
    private var animateDuration = 0

    /*
     * List containing pinned apps.
     */
    private val pinnedAppList = ArrayList<App>()

    /*
     * Adapter for pinned apps.
     */
    private val pinnedAppsAdapter = AppAdapter(pinnedAppList)

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
     * ViewPager used to handle Pages.
     */
    private lateinit var viewPager: ViewPager2

    /*
     * Adapter handling Pages, used by viewPager.
     */
    private lateinit var viewPagerAdapter: PageAdapter

    /*
     * Receiver used to listen to installed/uninstalled packages.
     */
    private val packageReceiver = PackageChangesReceiver()
    private var launcherApps: LauncherApps? = null
    private var userUtils: UserUtils? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load preferences before setting layout.
        loadPref()

        super.onCreate(savedInstanceState)

        binding = ActivityLauncherspaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (requestedOrientation != PreferenceHelper.orientation) {
            requestedOrientation = PreferenceHelper.orientation
        }

        if (Utils.atLeastLollipop()) {
            launcherApps = this.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        }
        userUtils = UserUtils(this)

        val pinnedAppsManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        appsListContainer = binding.appListContainer
        searchContainer = binding.searchContainer.searchContainer
        pinnedAppsContainer = binding.pinnedAppsContainer
        searchBar = binding.searchContainer.search
        slidingHome = binding.slideHome
        touchReceiver = binding.touchReceiver
        pinnedAppsRecyclerView = binding.pinnedAppsList
        searchContext = binding.searchContainer.searchContextButton
        viewPager = binding.pager
        animateDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

        // Let the launcher handle state of the sliding panel.
        slidingHome.apply {
            disallowHiding(true)
            alwaysResetState(true)
            anchorPoint = 0f
            setDragView(searchContainer)
        }

        pinnedAppsRecyclerView.apply {
            adapter = pinnedAppsAdapter
            layoutManager = pinnedAppsManager
            itemAnimator = null
        }

        pinnedAppsAdapter.isLongPressDragEnabled = true
        pinnedAppsAdapter.itemTouchHelperCallback.setMoveThreshold(1f)

        // The pager adapter, which provides the pages to the view pager widget.
        viewPagerAdapter = PageAdapter(this, viewPager)
        viewPager.adapter = viewPagerAdapter
        viewPager.setCurrentItem(1, false)

        // Start initialising listeners.
        addSearchBarTextListener()
        addSearchBarEditorListener()
        addGestureListener()
        addAdapterListener()
        addPanelListener()
        registerForContextMenu(touchReceiver)

        // Start pinning apps.
        updatePinnedApps(true)
        applyPrefToViews()

        // Show the app list once all the views are set up.
        if (PreferenceHelper.keepAppList()) {
            doThis(SHOW_PANEL)
        }

        if (PreferenceHelper.isNewUser) {
            showStartDialog()
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        menuInflater.inflate(R.menu.menu_main, menu)

        // Hide widget-space from the menu if it's disabled by the user.
        menu.getItem(2).isVisible = PreferenceHelper.widgetSpaceVisible()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return onOptionsItemSelected(item)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivityForResult(
                    Intent(this, SettingsActivity::class.java),
                    SETTINGS_RETURN_CODE
                )
                true
            }
            R.id.action_force_refresh -> {
                ViewUtils.restartActivity(this, true)
                true
            }
            R.id.action_view_widgets -> {
                doThis("open_widgets")
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
        doThis(HIDE_PANEL)
    }

    public override fun onPause() {
        super.onPause()

        // Dismiss any visible menu as well as the app panel when it is not needed.
        doThis(CLOSE_MENU)
        if (panelLockRequested || PreferenceHelper.keepAppList()) {
            // Clear the search bar text if app list is set to be kept open
            // unless keepLastSearch setting indicates maintain last search
            if (! PreferenceHelper.keepLastSearch()) {
                clearSearchBar(searchBar)
            }
        } else {
            doThis(HIDE_PANEL)
        }

        Utils.unregisterPackageReceiver(this, packageReceiver)
    }

    public override fun onResume() {
        super.onResume()

        // Refresh app pinned apps if there is a change in package count.
        if (AppUtils.hasNewPackage(packageManager)) {
            updatePinnedApps(true)
        }

        Utils.registerPackageReceiver(this, packageReceiver)

        // Show the app list when needed.
        if (PreferenceHelper.keepAppList()) {
            doThis(SHOW_PANEL)
            searchContainer.visibility = View.VISIBLE
        } else if ((! panelLockRequested && Utils.sdkIsBelow(21)) ||
            ActivityServiceUtils.isPowerSaving(this)
        ) {
            // HACK: For some reason, KitKat and below is always late setting visibility.
            // Manually set it here to make sure it's invisible.
            searchContainer.visibility = View.INVISIBLE
        }

        // Restore last search query if the user requests for it.
        if (PreferenceHelper.keepLastSearch()) {
            doSearch(searchBar.text.toString())
        }

        // Toggle back the refresh switch.
        PreferenceHelper.update("require_refresh", false)

        panelLockRequested = false

        isResuming = true
    }

    public override fun onStart() {
        super.onStart()

        // Restart the launcher in case of an alien call.
        if (PreferenceHelper.wasAlien()) {
            PreferenceHelper.isAlien(false)
            ViewUtils.restartActivity(this, true)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        // See if any of the system bars needed hiding.
        @Suppress("DEPRECATION") // We are already handling the deprecation.
        if (Utils.atLeastR()) {
            ViewUtils.setWindowBarMode(this, PreferenceHelper.windowBarMode)
        } else if (Utils.atLeastKitKat()) {
            window.decorView.systemUiVisibility =
                ViewUtils.setWindowbarMode(PreferenceHelper.windowBarMode)
        } else if (PreferenceHelper.shouldHideStatusBar()) {
            if (Utils.sdkIsAround(16)) {
                window.decorView.systemUiVisibility = ViewUtils.setWindowbarMode("status")
            } else {
                ViewUtils.hideStatusBar(window)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Handle preference change. Refresh when necessary.
        if (requestCode == SETTINGS_RETURN_CODE && ! PreferenceHelper.wasAlien()) {
            ViewUtils.restartActivity(this, true)
        }

        // Call super to handle anything else not handled here.
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return if (event.action == KeyEvent.ACTION_DOWN && event.isCtrlPressed) {
            Utils.handleInputShortcut(this, searchBar, keyCode)
        } else {
            if (keyCode == KeyEvent.KEYCODE_SPACE) {
                if (window.currentFocus !== searchBar) {
                    doThis(SHOW_PANEL)
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
            CLOSE_MENU -> appMenu?.apply {
                this.menu.findItem(R.id.action_app_actions)?.subMenu?.close()
                this.menu.findItem(SHORTCUT_MENU_GROUP)?.subMenu?.close()
                this.dismiss()
            }
            SHOW_PANEL -> slidingHome.setPanelState(
                SlidingUpPanelLayout.PanelState.COLLAPSED,
                ActivityServiceUtils.isPowerSaving(this)
            )
            HIDE_PANEL -> slidingHome.setPanelState(
                SlidingUpPanelLayout.PanelState.EXPANDED,
                ActivityServiceUtils.isPowerSaving(this)
            )
            SHOW_PINNED -> pinnedAppsContainer.animate()
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
            HIDE_PINNED -> pinnedAppsContainer.animate()
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
            "open_widgets" -> {
                viewPager.currentItem = 0
                doThis(SHOW_PANEL)
            }
            else -> {
            }
        }
    }

    /**
     * Modifies various views parameters and visibility based on the user preferences.
     */
    private fun applyPrefToViews() {
        slidingHome.post {
            // Hide the favourites panel when there's nothing to show.
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
                window.navigationBarColor = ContextCompat.getColor(this, R.color.navigationBarShade)
            }

            binding.root.setBackgroundResource(R.drawable.image_inner_shadow)
        }
        if ("transparent" == PreferenceHelper.listBackground) {
            appsListContainer.setBackgroundColor(
                Utils.getColorFromAttr(this, R.attr.backgroundColorAlt)
            )
        } else if ("none" == PreferenceHelper.listBackground) {
            appsListContainer.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    /**
     * Loads available preferences and updates PreferenceHelpers.
     */
    private fun loadPref() {
        if (! PreferenceHelper.hasEditor()) {
            PreferenceHelper.initPreference(this)
        }
        PreferenceHelper.fetchPreference()

        // Get pinned apps.
        pinnedAppString = PreferenceHelper.getPinnedApps()

        // Get the default providers list if it's empty.
        if (PreferenceHelper.providerList.isEmpty()) {
            PreferenceHelper.updateProvider(Utils.setDefaultProviders(resources, ArrayList()))
        }

        ViewUtils.switchTheme(this, true)

        // Clear icon pack cache if we receive the flag to hard refresh.
        if (PreferenceHelper.preference.getBoolean("require_refresh", false)) {
            LauncherIconHelper.refreshIcons()
        }

        // Get icons from icon pack.
        if ("default" != PreferenceHelper.iconPackName &&
            LauncherIconHelper.loadIconPack(packageManager) == 0
        ) {
            PreferenceHelper.editor?.putString("icon_pack", "default")?.apply()
        }
    }

    /**
     * Creates a PopupMenu to use in a long-pressed app object.
     *
     * @param view     View for the PopupMenu to anchor to.
     * @param app      App object selected from the list.
     */
    private fun createAppMenu(view: View, app: App) {
        val packageName = app.packageName
        val user = app.user
        val packageNameUri = Uri.fromParts(
            "package", AppUtils.getPackageName(packageName),
            null
        )
        val shortcutMap = SparseArray<String>()
        val position = pinnedAppsAdapter.getGlobalPositionOf(app)

        appMenu = ViewUtils.createAppMenu(this, view, app).apply {
            // Inflate app shortcuts.
            menu.getItem(0).isVisible = if (Utils.sdkIsAround(25)) {
                var menuId = SHORTCUT_MENU_GROUP
                AppUtils.getShortcuts(launcherApps, packageName)?.forEach {
                    shortcutMap.put(menuId, it.id)
                    menu
                        .findItem(SHORTCUT_MENU_GROUP)
                        .subMenu
                        .add(SHORTCUT_MENU_GROUP, menuId, Menu.NONE, it.shortLabel)
                    menuId ++
                }

                shortcutMap.size() > 0 // Only show the menu if there's a shortcut.
            } else {
                false // API level older than 25 doesn't have support for shortcuts.
            }

            show()
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_unpin -> unpinApp(position)
                    R.id.action_info -> AppUtils.openAppDetails(
                        this@LauncherActivity,
                        packageName,
                        user
                    )
                    R.id.action_uninstall -> AppUtils.uninstallApp(
                        this@LauncherActivity,
                        packageNameUri
                    )
                    else -> {
                        // Catch click actions from the shortcut menu group.
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
                }
                true
            }
        }
    }

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

    private fun addSearchBarTextListener() {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // Pages should be informed about the new query as soon as they are selected.
                doSearch(searchBar.text.toString())
            }
        })

        searchBar.addTextChangedListener(object : TextSpectator(searchBar) {
            var searchHint: String = ""
            var searchSnack = DagashiBar.make(
                appsListContainer, searchHint,
                DagashiBar.LENGTH_INDEFINITE, false
            ).setTextColor(PreferenceHelper.accent)

            override fun whenTimerTicked() {
                super.whenTimerTicked()
                if (trimmedInputText.isEmpty()) {
                    // HACK: Hide the view stub.
                    if (pinnedAppsAdapter.isEmpty) {
                        doThis(HIDE_PINNED)
                    }
                    if (isContextVisible) {
                        doThis("hide_context_button")
                    }
                    searchSnack.dismiss()
                    stopTimer()
                }
            }

            override fun whenChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                super.whenChanged(s, start, before, count)

                // Filter the list.
                doSearch(trimmedInputText)

                // Text used for searchSnack.
                searchHint = String.format(resources.getString(R.string.search_web_hint), inputText)

                startTimer()
                if (trimmedInputText.isNotEmpty() && PreferenceHelper.promptSearch()) {
                    // HACK: Show a view stub to make sure app list anchors properly.
                    doThis(SHOW_PINNED)

                    // Update the snackbar text.
                    searchSnack.setText(searchHint)

                    // Show the context/clear-all button.
                    if (! isContextVisible) {
                        doThis("show_context_button")
                    }

                    val searchSnackAction: String = if (PreferenceHelper.searchProvider == "none") {
                        getString(R.string.search_web_button_prompt)
                    } else {
                        getString(R.string.search_web_button)
                    }

                    // Prompt user if they want to search their query online.
                    searchSnack.setNonDismissAction(searchSnackAction) {
                        if (PreferenceHelper.searchProvider != "none") {
                            PreferenceHelper.searchProvider?.let { provider ->
                                Utils.doWebSearch(
                                    this@LauncherActivity,
                                    provider,
                                    URLEncoder.encode(trimmedInputText, Charsets.UTF_8.name())
                                )
                            }
                            searchSnack.dismiss()
                        } else {
                            appMenu = PopupMenu(this@LauncherActivity, it).apply {
                                ViewUtils.createSearchMenu(
                                    this@LauncherActivity, this,
                                    URLEncoder.encode(trimmedInputText, Charsets.UTF_8.name())
                                )
                            }
                        }
                    }.show()
                    if (PreferenceHelper.extendedSearchMenu() && PreferenceHelper.searchProvider != "none") {
                        searchSnack.setLongPressAction {
                            appMenu = PopupMenu(this@LauncherActivity, it).apply {
                                ViewUtils.createSearchMenu(
                                    this@LauncherActivity, this,
                                    URLEncoder.encode(trimmedInputText, Charsets.UTF_8.name())
                                )
                            }
                            true
                        }
                    }
                }
            }
        })
    }

    private fun addSearchBarEditorListener() {
        searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (searchBar.text.isNotEmpty()
                && (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_NULL)
            ) {
                val pageAvailable = viewPagerAdapter.getCurrentPage()?.launchPreselection() ?: false
                if (! pageAvailable && PreferenceHelper.promptSearch() && PreferenceHelper.searchProvider != "none") {
                    PreferenceHelper.searchProvider?.let {
                        Utils.doWebSearch(
                            this@LauncherActivity, it,
                            URLEncoder.encode(searchBar.text.toString(), Charsets.UTF_8.name())
                        )
                    }
                }
            }
            true
        }
    }

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
                doThis(CLOSE_MENU)

                // Shuffle our apps around.
                pinnedAppsAdapter.swapItems(pinnedAppList, fromPosition, toPosition)
                pinnedAppsAdapter.notifyItemMoved(fromPosition, toPosition)
            }

            override fun onActionStateChanged(
                viewHolder: RecyclerView.ViewHolder?,
                actionState: Int
            ) {
                // FIXME: Work out a better touch detection.
                // No movement occurred, this is a long press.
                if (newState != ItemTouchHelper.ACTION_STATE_DRAG && System.currentTimeMillis() - startTime == System
                        .currentTimeMillis()
                ) {
                    viewHolder?.apply {
                        val app: App? = pinnedAppsAdapter.getItem(this.absoluteAdapterPosition)
                        app?.let { createAppMenu(this.itemView, it) }
                    }
                } else {
                    // Reset startTime and update the pinned apps, we were swiping.
                    startTime = 0
                    updatePinnedApps(false)
                }
                newState = actionState
            }
        })
    }

    private fun addPanelListener() {
        slidingHome.addPanelSlideListener(object : PanelSlideListener {
            override fun onPanelSlide(view: View, v: Float) {
                // Dismiss any visible menu.
                doThis(CLOSE_MENU)
            }

            override fun onPanelStateChanged(panel: View, previousState: Int, newState: Int) {
                with(searchBar) {
                    isClickable = newState == SlidingUpPanelLayout.PanelState.COLLAPSED
                    isLongClickable = newState == SlidingUpPanelLayout.PanelState.COLLAPSED
                }
                when (newState) {
                    SlidingUpPanelLayout.PanelState.DRAGGING -> {
                        // Empty out search bar text
                        // Clear the search bar text if app list is set to be kept open
                        // unless keepLastSearch setting indicates maintain last search
                        if (! PreferenceHelper.keepLastSearch()) {
                            clearSearchBar(searchBar)
                        }

                        // Animate search container entering the view.
                        if (! ActivityServiceUtils.isPowerSaving(this@LauncherActivity)) {
                            searchContainer.animate().alpha(1f)
                                .setDuration(animateDuration.toLong())
                                .setListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationStart(animation: Animator) {
                                        searchContainer.visibility = View.VISIBLE
                                    }
                                })
                        } else {
                            searchContainer.animate().alpha(1f)
                            searchContainer.visibility = View.VISIBLE
                        }
                    }
                    SlidingUpPanelLayout.PanelState.COLLAPSED ->
                        // Show the keyboard.
                        if (PreferenceHelper.shouldFocusKeyboard()
                            && previousState == SlidingUpPanelLayout.PanelState.DRAGGING
                        ) {
                            ActivityServiceUtils.showSoftKeyboard(this@LauncherActivity, searchBar)
                        }
                    SlidingUpPanelLayout.PanelState.EXPANDED -> {
                        // Hide keyboard if container is invisible.
                        ActivityServiceUtils.hideSoftKeyboard(this@LauncherActivity)

                        // Toggle the visibility early.
                        searchContainer.visibility = View.INVISIBLE

                        if (! isResuming && ! ActivityServiceUtils.isPowerSaving(this@LauncherActivity)) {
                            // Animate the container.
                            searchContainer.animate().alpha(0f)
                                .setDuration(animateDuration.toLong())
                                .setListener(null)
                        } else {
                            isResuming = false
                        }
                    }
                    SlidingUpPanelLayout.PanelState.ANCHORED ->
                        // Please don't anchor, we don't want it.
                        if (previousState != SlidingUpPanelLayout.PanelState.DRAGGING) {
                            slidingHome.setPanelState(
                                previousState,
                                ActivityServiceUtils.isPowerSaving(this@LauncherActivity)
                            )
                        } else {
                            slidingHome.setPanelState(
                                SlidingUpPanelLayout.PanelState.COLLAPSED,
                                ActivityServiceUtils.isPowerSaving(this@LauncherActivity)
                            )
                        }
                    else -> {
                    }
                }
            }
        })
    }

    /**
     * Updates the favourites panel.
     * @param restart Should a complete adapter & list re-initialisation be done?
     */
    private fun updatePinnedApps(restart: Boolean) {
        if (pinnedAppString.isNotEmpty() && restart) {
            pinnedAppList.clear()
            pinnedAppsAdapter.updateDataSet(pinnedAppList)
            pinnedAppString.split(";").forEach {
                // Handle pinned apps coming from another user.
                val userSplit = it.split("-")
                val componentName = if (userSplit.size == 2) userSplit[1] else it
                val user =
                    if (userSplit.size == 2) userSplit[0].toLong() else userUtils?.currentSerial
                        ?: 0

                if (AppUtils.doesComponentExist(packageManager, componentName)) {
                    AppUtils.pinApp(this, user, componentName, pinnedAppsAdapter, pinnedAppList)
                }
            }
        }

        // Iterate through the list to get package name of each pinned apps, then stringify them.
        pinnedAppString = pinnedAppList.joinToString(";") { it.userPackageName }

        // Update the saved pinned apps.
        PreferenceHelper.update("pinned_apps_list", pinnedAppString)
    }

    private fun doSearch(query: String) {
        viewPagerAdapter.getCurrentPage()?.apply {
            if (this.isAcceptingSearch()) {
                this.commitSearch(query)
            }
        }
    }

    fun clearSearchBar(view: View) {
        // Clear the search bar text if app list is set to be kept open
        searchBar.setText("")
    }

    /**
     * Pin an app to the favourites panel.
     *
     * @param packageName The package name of the app.
     * @param user The user eligible to launch the app.
     */
    fun pinAppHere(packageName: String, user: Long) {
        // We need to make sure that an app from another user can be pinned.
        val userSplit = packageName.split("-")
        val componentName = if (userSplit.size == 2) userSplit[1] else packageName

        AppUtils.pinApp(this, user, componentName, pinnedAppsAdapter, pinnedAppList)
        updatePinnedApps(false)
    }

    private fun unpinApp(positionInAdapter: Int) {
        pinnedAppList.remove(pinnedAppsAdapter.getItem(positionInAdapter))
        pinnedAppsAdapter.removeItem(positionInAdapter)
        updatePinnedApps(false)
        if (pinnedAppsAdapter.isEmpty) {
            doThis(HIDE_PINNED)
        }
    }

    /**
     * Hide the favourites panel.
     * This function is provided for Pages to toggle favourites panel visibility.
     */
    fun hidePinnedApps() {
        if (! pinnedAppsAdapter.isEmpty
            && isFavouritesVisible
            && PreferenceHelper.favouritesAcceptScroll()
            && searchBar.text.toString().isEmpty()
        ) {
            doThis(HIDE_PINNED)
        }
    }

    /**
     * Show the favourites panel.
     * This function is provided for Pages to toggle favourites panel visibility.
     */
    fun showPinnedApps() {
        if (! pinnedAppsAdapter.isEmpty
            && ! isFavouritesVisible
            && PreferenceHelper.favouritesAcceptScroll()
        ) {
            doThis(SHOW_PINNED)
        }
    }

    /**
     * Locks the panel, preventing it from being pulled up when pausing the launcher.
     * The lock is released upon the second pause.
     */
    fun requestPanelLock() {
        panelLockRequested = true
    }

    private fun showStartDialog() {
        val binding = DialogStartHintBinding.inflate(layoutInflater)

        BottomSheetDialog(this).apply {
            setContentView(binding.root)
            setCancelable(false)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }.also { dialog ->
            with(binding.dismiss) {
                setOnClickListener {
                    dialog.dismiss()
                    PreferenceHelper.update("is_new_user", false)
                }
            }
            dialog.show()
        }

    }

    companion object {
        private const val SETTINGS_RETURN_CODE = 12
        private const val SHORTCUT_MENU_GROUP = 247

        /*
         * String containing pinned apps. Delimited by a semicolon (;).
         */
        private var pinnedAppString: String = ""

        // Constants used for doThis()
        private const val SHOW_PINNED = "show_favourites"
        private const val HIDE_PINNED = "hide_favourites"
        private const val SHOW_PANEL = "show_panel"
        private const val HIDE_PANEL = "hide_panel"
        private const val CLOSE_MENU = "dismiss_menu"
    }
}