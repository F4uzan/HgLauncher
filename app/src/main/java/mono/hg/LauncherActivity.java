package mono.hg;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import mono.hg.adapters.AppAdapter;
import mono.hg.fragments.WidgetsDialogFragment;
import mono.hg.helpers.LauncherIconHelper;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.listeners.GestureListener;
import mono.hg.listeners.SimpleScrollListener;
import mono.hg.models.App;
import mono.hg.models.PinnedApp;
import mono.hg.receivers.PackageChangesReceiver;
import mono.hg.tasks.FetchAppsTask;
import mono.hg.utils.ActivityServiceUtils;
import mono.hg.utils.AppUtils;
import mono.hg.utils.UserUtils;
import mono.hg.utils.Utils;
import mono.hg.utils.ViewUtils;
import mono.hg.views.DagashiBar;
import mono.hg.views.IndeterminateMaterialProgressBar;
import mono.hg.views.TogglingLinearLayoutManager;
import mono.hg.wrappers.TextSpectator;

public class LauncherActivity extends AppCompatActivity {

    private static int SETTINGS_RETURN_CODE = 12;
    private static int SHORTCUT_MENU_GROUP = 247;
    /*
     * List containing installed apps.
     */
    private ArrayList<App> appsList = new ArrayList<>();
    /*
     * Adapter for installed apps.
     */
    private AppAdapter appsAdapter = new AppAdapter(appsList);
    /*
     * RecyclerView for app list.
     */
    private FastScrollRecyclerView appsRecyclerView;
    /*
     * Progress bar shown when populating app list.
     */
    private IndeterminateMaterialProgressBar loadProgress;
    /*
     * Are we resuming this activity?
     */
    private boolean isResuming = false;
    /*
     * Visibility of the favourites panel.
     */
    private boolean isFavouritesVisible;
    /*
     * Visibility of the contextual button of search bar.
     */
    private boolean isContextVisible = false;
    /*
     * Animation duration; fetched from system's duration.
     */
    private int animateDuration;
    /*
     * List containing pinned apps.
     */
    private ArrayList<PinnedApp> pinnedAppList = new ArrayList<>();
    /*
     * String containing pinned apps. Delimited by a semicolon (;).
     */
    private String pinnedAppString;
    /*
     * Adapter for pinned apps.
     */
    private FlexibleAdapter<PinnedApp> pinnedAppsAdapter = new FlexibleAdapter<>(
            pinnedAppList);
    /*
     * List of excluded apps. These will not be shown in the app list.
     */
    private HashSet<String> excludedAppsList = new HashSet<>();
    /*
     * Package manager; casted through getPackageManager().
     */
    private PackageManager manager;
    /*
     * LinearLayoutManager used in appsRecyclerView.
     */
    private TogglingLinearLayoutManager appsLayoutManager;
    /*
     * RecyclerView for pinned apps; shown in favourites panel.
     */
    private RecyclerView pinnedAppsRecyclerView;
    /*
     * Parent layout containing search bar.
     */
    private LinearLayout searchContainer;
    /*
     * Parent layout of pinned apps' RecyclerView.
     */
    private FrameLayout pinnedAppsContainer;
    /*
     * The search bar. Contained in searchContainer.
     */
    private EditText searchBar;
    /*
     * Sliding up panel. Shows the app list when pulled down and
     * a parent to the other containers.
     */
    private SlidingUpPanelLayout slidingHome;
    /*
     * CoordinatorLayout hosting the view visible when slidingHome is pulled down.
     */
    private CoordinatorLayout appsListContainer;
    /*
     * Contextual button that changes depending on the availability of search text.
     */
    private ImageButton searchContext;
    /*
     * A view used to intercept gestures and taps in the desktop.
     */
    private View touchReceiver;
    /*
     * Menu shown when long-pressing apps.
     */
    private PopupMenu appMenu;
    /*
     * Receiver used to listen to installed/uninstalled packages.
     */
    private PackageChangesReceiver packageReceiver = new PackageChangesReceiver();

    private LauncherApps launcherApps;
    private UserUtils userUtils;

    private FetchAppsTask fetchAppsTask;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load preferences before setting layout.
        loadPref();

        setContentView(R.layout.activity_launcherspace);

        if (getRequestedOrientation() != PreferenceHelper.getOrientation()) {
            setRequestedOrientation(PreferenceHelper.getOrientation());
        }

        manager = getPackageManager();

        appsLayoutManager = new TogglingLinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, true);

        final LinearLayoutManager pinnedAppsManager = new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false);

        appsListContainer = findViewById(R.id.app_list_container);
        searchContainer = findViewById(R.id.search_container);
        pinnedAppsContainer = findViewById(R.id.pinned_apps_container);
        searchBar = findViewById(R.id.search);
        slidingHome = findViewById(R.id.slide_home);
        touchReceiver = findViewById(R.id.touch_receiver);
        appsRecyclerView = findViewById(R.id.apps_list);
        pinnedAppsRecyclerView = findViewById(R.id.pinned_apps_list);
        searchContext = findViewById(R.id.search_context_button);
        loadProgress = findViewById(R.id.load_progress);

        if (Utils.atLeastLollipop()) {
            launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        }

        userUtils = new UserUtils(this);

        animateDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        // Let the launcher handle state of the sliding panel.
        slidingHome.disallowHiding(true);
        slidingHome.alwaysResetState(true);
        slidingHome.setAnchorPoint(0f);
        slidingHome.setDragView(searchContainer);

        appsRecyclerView.setDrawingCacheEnabled(true);
        appsRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        appsRecyclerView.setHasFixedSize(true);
        appsRecyclerView.setThumbColor(PreferenceHelper.getDarkAccent());
        appsRecyclerView.setThumbInactiveColor(PreferenceHelper.getAccent());
        appsRecyclerView.setPopupBgColor(PreferenceHelper.getDarkerAccent());

        appsRecyclerView.setAdapter(appsAdapter);
        appsRecyclerView.setLayoutManager(appsLayoutManager);
        appsRecyclerView.setItemAnimator(new DefaultItemAnimator());

        pinnedAppsRecyclerView.setAdapter(pinnedAppsAdapter);
        pinnedAppsRecyclerView.setLayoutManager(pinnedAppsManager);
        pinnedAppsRecyclerView.setItemAnimator(null);

        pinnedAppsAdapter.setLongPressDragEnabled(true);

        // Get icons from icon pack.
        if (!"default".equals(PreferenceHelper.getIconPackName()) &&
                LauncherIconHelper.loadIconPack(manager) == 0) {
            PreferenceHelper.getEditor().putString("icon_pack", "default").apply();
        }

        // Start initialising listeners.
        addSearchBarTextListener();
        addSearchBarEditorListener();
        addGestureListener();
        addAdapterListener();
        addListListeners();
        addPanelListener();

        registerForContextMenu(touchReceiver);

        PreferenceHelper.update("package_count", AppUtils.countInstalledPackage(manager));

        // Start pinning apps.
        updatePinnedApps(true);

        applyPrefToViews();

        // Show the app list once all the views are set up.
        if (PreferenceHelper.keepAppList()) {
            doThis("show_panel");
        }

        if (PreferenceHelper.isNewUser()) {
            showStartDialog();
        }
    }

    @Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
    }

    @Override public boolean onContextItemSelected(@NonNull MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class),
                        SETTINGS_RETURN_CODE);
                return true;
            case R.id.action_force_refresh:
                recreate();
                return true;
            case R.id.action_view_widgets:
                WidgetsDialogFragment widgetFragment = new WidgetsDialogFragment();
                widgetFragment.show(getSupportFragmentManager(), "Widgets Dialog");
                return true;
            case R.id.update_wallpaper:
                Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                startActivity(Intent.createChooser(intent, getString(R.string.action_wallpaper)));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onBackPressed() {
        // Don't call super.onBackPressed because we don't want the launcher to close.

        // Hides the panel if back is pressed.
        doThis("dismiss_panel");
    }

    @Override public void onPause() {
        super.onPause();

        // Dismiss any visible menu as well as the app panel when it is not needed.
        doThis("dismiss_menu");

        if (!PreferenceHelper.keepAppList()) {
            doThis("dismiss_panel");
        } else {
            // Clear the search bar text if app list is set to be kept open
            // unless keepLastSearch setting indicates maintain last search
            if (!PreferenceHelper.keepLastSearch()) {
                clearSearch(searchBar);
            }
        }

        Utils.unregisterPackageReceiver(this, packageReceiver);
    }

    @Override public void onResume() {
        super.onResume();

        // See if user has changed icon pack. Clear cache if true.
        if (PreferenceHelper.getPreference().getBoolean("require_refresh", false) ||
                !PreferenceHelper.getPreference()
                                 .getString("icon_pack", "default")
                                 .equals(PreferenceHelper.getIconPackName())) {
            LauncherIconHelper.refreshIcons();
        }

        // Get pinned apps.
        pinnedAppString = PreferenceHelper.getPreference().getString("pinned_apps_list", "");

        // Refresh app list and pinned apps if there is a change in package count.
        if (AppUtils.hasNewPackage(
                manager) || (appsAdapter.hasFinishedLoading() && appsAdapter.isEmpty())) {
            updatePinnedApps(true);
            fetchAppsTask.cancel(true);
            fetchAppsTask = new FetchAppsTask(this, appsAdapter, appsList);
            fetchAppsTask.execute();
        }

        Utils.registerPackageReceiver(this, packageReceiver);

        // Show the app list when needed.
        if (PreferenceHelper.keepAppList()) {
            doThis("show_panel");
            searchContainer.setVisibility(View.VISIBLE);
        } else if (Utils.sdkIsBelow(21) || ActivityServiceUtils.isPowerSaving(this)) {
            // HACK: For some reason, KitKat and below is always late setting visibility.
            // Manually set it here to make sure it's invisible.
            searchContainer.setVisibility(View.INVISIBLE);
        }

        // Toggle back the refresh switch.
        PreferenceHelper.update("require_refresh", false);

        isResuming = true;
    }

    @Override public void onStart() {
        super.onStart();

        // Restart the launcher in case of an alien call.
        if (PreferenceHelper.wasAlien()) {
            PreferenceHelper.isAlien(false);
            recreate();
        }

        if (fetchAppsTask == null && appsAdapter.isEmpty()) {
            fetchAppsTask = new FetchAppsTask(this, appsAdapter, appsList);
            fetchAppsTask.execute();
        }

        // Reset the app list filter.
        appsAdapter.resetFilter();
    }

    @Override protected void onDestroy() {
        super.onDestroy();

        if (fetchAppsTask != null && fetchAppsTask.getStatus() == AsyncTask.Status.RUNNING) {
            fetchAppsTask.cancel(true);
        }
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        // See if any of the system bars needed hiding.
        if (Utils.atLeastKitKat()) {
            getWindow().getDecorView()
                       .setSystemUiVisibility(
                               ViewUtils.setWindowbarMode(PreferenceHelper.getWindowBarMode()));
        } else if (Utils.sdkIsBelow(19) && PreferenceHelper.shouldHideStatusBar()) {
            getWindow().getDecorView().setSystemUiVisibility(ViewUtils.setWindowbarMode("status"));
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Handle preference change. Refresh when necessary.
        if (requestCode == SETTINGS_RETURN_CODE && !PreferenceHelper.wasAlien()) {
            recreate();
        }

        // Call super to handle anything else not handled here.
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.isCtrlPressed()) {
            return Utils.handleInputShortcut(this, searchBar, keyCode);
        } else {
            if (keyCode == KeyEvent.KEYCODE_SPACE) {
                if (getWindow().getCurrentFocus() != searchBar) {
                    doThis("show_panel");
                }
                return true;
            } else {
                return super.onKeyUp(keyCode, event);
            }
        }
    }

    /**
     * A shorthand for various toggles and visibility checks/sets.
     *
     * @param action What to do?
     */
    public void doThis(String action) {
        switch (action) {
            default:
                // Don't do anything.
                break;
            case "dismiss_menu":
                if (appMenu != null) {
                    if (appMenu.getMenu().findItem(R.id.action_app_actions) != null) {
                        appMenu.getMenu().findItem(R.id.action_app_actions).getSubMenu().close();
                    }

                    if (appMenu.getMenu().findItem(SHORTCUT_MENU_GROUP) != null) {
                        appMenu.getMenu().findItem(SHORTCUT_MENU_GROUP).getSubMenu().close();
                    }

                    appMenu.dismiss();
                }
                break;
            case "show_panel":
                slidingHome.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED,
                        ActivityServiceUtils.isPowerSaving(this));
                break;
            case "dismiss_panel":
                slidingHome.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED,
                        ActivityServiceUtils.isPowerSaving(this));
                break;
            case "show_favourites":
                pinnedAppsContainer.animate()
                                   .translationY(0f)
                                   .setInterpolator(new LinearOutSlowInInterpolator())
                                   .setDuration(225)
                                   .setListener(new AnimatorListenerAdapter() {
                                       @Override public void onAnimationEnd(Animator animation) {
                                           isFavouritesVisible = true;
                                       }

                                       @Override public void onAnimationCancel(Animator animation) {
                                           isFavouritesVisible = false;
                                       }
                                   });
                break;
            case "hide_favourites":
                pinnedAppsContainer.animate()
                                   .translationY(pinnedAppsContainer.getMeasuredHeight())
                                   .setInterpolator(new FastOutLinearInInterpolator())
                                   .setDuration(175)
                                   .setListener(new AnimatorListenerAdapter() {
                                       @Override public void onAnimationEnd(Animator animation) {
                                           isFavouritesVisible = false;
                                       }
                                   });
                break;
            case "show_context_button":
                searchContext.animate()
                             .translationX(0f)
                             .setInterpolator(new LinearOutSlowInInterpolator())
                             .setDuration(200)
                             .setListener(new AnimatorListenerAdapter() {
                                 @Override public void onAnimationEnd(Animator animation) {
                                     isContextVisible = true;
                                 }

                                 @Override public void onAnimationCancel(Animator animation) {
                                     isContextVisible = false;
                                 }
                             });
                break;
            case "hide_context_button":
                searchContext.animate()
                             .translationX(searchContext.getMeasuredWidth())
                             .setInterpolator(new FastOutLinearInInterpolator())
                             .setDuration(150)
                             .setListener(new AnimatorListenerAdapter() {
                                 @Override public void onAnimationEnd(Animator animation) {
                                     isContextVisible = false;
                                 }
                             });

                break;
        }
    }

    /**
     * Modifies various views parameters and visibility based on the user preferences.
     */
    private void applyPrefToViews() {
        // Workaround v21+ status bar transparency issue.
        // This is disabled if the status bar is hidden.
        if (Utils.atLeastLollipop()
                && (PreferenceHelper.getWindowBarMode().equals("none")
                || PreferenceHelper.getWindowBarMode().equals("nav"))) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            ViewGroup.MarginLayoutParams homeParams = (ViewGroup.MarginLayoutParams) slidingHome
                    .getLayoutParams();
            homeParams.topMargin = ViewUtils.getStatusBarHeight();
        }

        slidingHome.post(new Runnable() {
            @Override public void run() {
                // Hide the favourites panel when there's nothing to show.
                if (pinnedAppsAdapter.isEmpty()) {
                    pinnedAppsContainer.setTranslationY(pinnedAppsContainer.getMeasuredHeight());
                    isFavouritesVisible = false;
                } else {
                    isFavouritesVisible = true;
                }
            }
        });

        // Switch on wallpaper shade.
        if (PreferenceHelper.useWallpaperShade()) {
            View wallpaperShade = findViewById(R.id.wallpaper_shade);
            // Tints the navigation bar with a semi-transparent shade.
            if (Utils.atLeastLollipop()) {
                getWindow().setNavigationBarColor(
                        getResources().getColor(R.color.navigationBarShade));
            }
            wallpaperShade.setBackgroundResource(R.drawable.image_inner_shadow);
        }

        if ("transparent".equals(PreferenceHelper.getListBackground())) {
            appsListContainer.setBackgroundColor(
                    Utils.getColorFromAttr(this, R.attr.backgroundColorAlt));
        } else if ("none".equals(PreferenceHelper.getListBackground())) {
            appsListContainer.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    /**
     * Loads available preferences and updates PreferenceHelpers.
     */
    private void loadPref() {
        if (!PreferenceHelper.hasEditor()) {
            PreferenceHelper.initPreference(this);
        }
        PreferenceHelper.fetchPreference();

        // Get pinned apps.
        pinnedAppString = PreferenceHelper.getPreference().getString("pinned_apps_list", "");

        // Get a list of our hidden apps, default to null if there aren't any.
        excludedAppsList.addAll(PreferenceHelper.getExclusionList());

        // Get the default providers list if it's empty.
        if (PreferenceHelper.getProviderList().isEmpty()) {
            Utils.setDefaultProviders(getResources());
        }

        // Set the app theme!
        switch (PreferenceHelper.appTheme()) {
            default:
            case "auto":
                if (Utils.atLeastQ()) {
                    AppCompatDelegate.setDefaultNightMode(
                            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                } else {
                    AppCompatDelegate.setDefaultNightMode(
                            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                }
                break;
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                setTheme(R.style.LauncherTheme_Dark);
                break;
            case "black":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }

    /**
     * Creates a PopupMenu to use in a long-pressed app object.
     *
     * @param view     View for the PopupMenu to anchor to.
     * @param isPinned Is this a pinned app?
     * @param app      App object selected from the list.
     */
    private void createAppMenu(View view, boolean isPinned, final App app) {
        final String packageName = app.getPackageName();
        final ComponentName componentName = ComponentName.unflattenFromString(packageName);
        final long user = app.getUser();
        PinnedApp pinApp = new PinnedApp(app.getPackageName(), app.getUser());
        final Uri packageNameUri = Uri.fromParts("package", AppUtils.getPackageName(packageName),
                null);
        final SparseArray<String> shortcutMap = new SparseArray<>();

        int position;
        if (isPinned) {
            position = pinnedAppsAdapter.getGlobalPositionOf(app);
        } else {
            position = appsAdapter.getGlobalPositionOf(app);
        }

        // Inflate the app menu.
        appMenu = new PopupMenu(LauncherActivity.this, view);
        appMenu.getMenuInflater().inflate(R.menu.menu_app, appMenu.getMenu());
        appMenu.getMenu().addSubMenu(1, SHORTCUT_MENU_GROUP, 0, R.string.action_shortcuts);

        // Inflate app shortcuts.
        if (Utils.sdkIsAround(25)) {
            int menu_id = SHORTCUT_MENU_GROUP;

            for (ShortcutInfo shortcutInfo : AppUtils.getShortcuts(launcherApps, packageName)) {
                shortcutMap.put(menu_id, shortcutInfo.getId());
                appMenu.getMenu()
                       .findItem(SHORTCUT_MENU_GROUP)
                       .getSubMenu()
                       .add(SHORTCUT_MENU_GROUP, menu_id, Menu.NONE, shortcutInfo.getShortLabel());
                menu_id++;
            }

            if (shortcutMap.size() == 0) {
                appMenu.getMenu().getItem(0).setVisible(false);
            }
        } else {
            appMenu.getMenu().getItem(0).setVisible(false);
        }

        // Hide 'pin' if the app is already pinned or isPinned is set.
        appMenu.getMenu().findItem(R.id.action_pin)
               .setVisible(!isPinned
                       && !pinnedAppsAdapter.contains(pinApp));

        // We can't hide an app from the favourites panel.
        appMenu.getMenu().findItem(R.id.action_hide).setVisible(!isPinned);

        appMenu.getMenu().findItem(R.id.action_shorthand).setVisible(!isPinned);

        // Only show the 'unpin' option if isPinned is set.
        appMenu.getMenu().findItem(R.id.action_unpin).setVisible(isPinned);

        // Show uninstall menu if the app is not a system app.
        appMenu.getMenu().findItem(R.id.action_uninstall)
               .setVisible(!AppUtils.isSystemApp(manager, packageName)
                       && app.getUser() == userUtils.getCurrentSerial());

        appMenu.show();

        final int finalPosition = position;
        appMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_pin:
                        AppUtils.pinApp(LauncherActivity.this, user, packageName, pinnedAppsAdapter,
                                pinnedAppList);
                        pinnedAppString = pinnedAppString.concat(app.getUserPackageName() + ";");
                        PreferenceHelper.update("pinned_apps_list", pinnedAppString);
                        break;
                    case R.id.action_unpin:
                        pinnedAppList.remove(pinnedAppsAdapter.getItem(finalPosition));
                        pinnedAppsAdapter.removeItem(finalPosition);
                        pinnedAppString = pinnedAppString.replace(app.getUserPackageName() + ";",
                                "");
                        PreferenceHelper.update("pinned_apps_list", pinnedAppString);
                        if (pinnedAppsAdapter.isEmpty()) {
                            doThis("hide_favourites");
                        }
                        break;
                    case R.id.action_info:
                        if (Utils.atLeastLollipop()) {
                            launcherApps.startAppDetailsActivity(componentName,
                                    userUtils.getUser(app.getUser()), null, null);
                        } else {
                            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    packageNameUri));
                        }
                        break;
                    case R.id.action_uninstall:
                        startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageNameUri));
                        break;
                    case R.id.action_shorthand:
                        makeRenameDialog(app.getUserPackageName(), finalPosition);
                        break;
                    case R.id.action_hide:
                        // Add the app's package name to the exclusion list.
                        excludedAppsList.add(app.getUserPackageName());

                        PreferenceHelper.update("hidden_apps", excludedAppsList);

                        // Reload the app list!
                        appsList.remove(appsAdapter.getItem(finalPosition));
                        appsAdapter.removeItem(finalPosition);
                        break;
                    default:
                        // Catch click actions from the shortcut menu group.
                        if (item.getGroupId() == SHORTCUT_MENU_GROUP && Utils.sdkIsAround(25)) {
                            launcherApps.startShortcut(AppUtils.getPackageName(packageName),
                                    Utils.requireNonNull(shortcutMap.get(item.getItemId())),
                                    null, null,
                                    userUtils.getUser(user));
                        }
                        break;
                }
                return true;
            }
        });
    }

    /**
     * Listeners for touch receivers.
     */
    private void addGestureListener() {
        // Handle touch events in touchReceiver.
        touchReceiver.setOnTouchListener(new GestureListener(this) {
            @Override
            public void onGesture(int direction) {
                if (slidingHome.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    Utils.handleGestureActions(LauncherActivity.this, direction);
                }
            }

            @Override
            public void onLongPress() {
                // Show context menu when touchReceiver is long pressed when the panel is expanded.
                if (slidingHome.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    touchReceiver.showContextMenu();
                }
            }
        });
    }

    /**
     * Listeners for the search bar query.
     */
    private void addSearchBarTextListener() {
        // Implement listener for the search bar.
        searchBar.addTextChangedListener(new TextSpectator(searchBar) {
            String searchHint;

            DagashiBar searchSnack = DagashiBar.make(appsListContainer, searchHint,
                    DagashiBar.LENGTH_INDEFINITE, false).setTextColor(PreferenceHelper.getAccent());

            @Override public void whenTimerTicked() {
                super.whenTimerTicked();

                if (getTrimmedInputText().isEmpty()) {
                    // HACK: Hide the view stub.
                    if (pinnedAppsAdapter.isEmpty()) {
                        doThis("hide_favourites");
                    }
                    if (isContextVisible) {
                        doThis("hide_context_button");
                    }

                    appsAdapter.resetFilter();
                    searchSnack.dismiss();
                    stopTimer();
                } else {
                    // Begin filtering our list.
                    appsAdapter.setFilter(getTrimmedInputText());
                    appsAdapter.filterItems();
                }
            }

            @Override public void whenChanged(CharSequence s, int start, int before, int count) {
                super.whenChanged(s, start, before, count);

                // Text used for searchSnack.
                searchHint = String.format(getResources().getString(R.string.search_web_hint),
                        getInputText());
            }

            @Override public void afterChanged(Editable s) {
                super.afterChanged(s);

                startTimer();

                if (!getTrimmedInputText().isEmpty() && PreferenceHelper.promptSearch()) {
                    // HACK: Show a view stub to make sure app list anchors properly.
                    if (pinnedAppsAdapter.isEmpty()) {
                        doThis("show_favourites");
                    }

                    // Update the snackbar text.
                    searchSnack.setText(searchHint);

                    if (!isContextVisible) {
                        doThis("show_context_button");
                    }

                    String searchSnackAction;

                    if (PreferenceHelper.getSearchProvider().equals("none")) {
                        searchSnackAction = getString(R.string.search_web_button_prompt);
                    } else {
                        searchSnackAction = getString(R.string.search_web_button);
                    }

                    // Prompt user if they want to search their query online.
                    searchSnack.setNonDismissAction(searchSnackAction, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!PreferenceHelper.getSearchProvider().equals("none")) {
                                Utils.doWebSearch(LauncherActivity.this,
                                        PreferenceHelper.getSearchProvider(),
                                        URLEncoder.encode(getTrimmedInputText()));
                                searchSnack.dismiss();
                            } else {
                                appMenu = new PopupMenu(LauncherActivity.this, view);
                                ViewUtils.createSearchMenu(LauncherActivity.this, appMenu,
                                        URLEncoder.encode(getTrimmedInputText()));
                            }
                        }
                    }).show();

                    if (PreferenceHelper.extendedSearchMenu() && !PreferenceHelper.getSearchProvider()
                                                                                  .equals("none")) {
                        searchSnack.setLongPressAction(new View.OnLongClickListener() {
                            @Override public boolean onLongClick(View view) {
                                appMenu = new PopupMenu(LauncherActivity.this, view);
                                ViewUtils.createSearchMenu(LauncherActivity.this, appMenu,
                                        URLEncoder.encode(getTrimmedInputText()));
                                return true;
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * Listener for search bar editor (keyboard) action.
     */
    private void addSearchBarEditorListener() {
        searchBar.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((searchBar.getText().length() > 0)
                        && (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_NULL)) {
                    if (!appsAdapter.isEmpty()) {
                        ViewUtils.keyboardLaunchApp(LauncherActivity.this, appsRecyclerView,
                                appsAdapter);
                    } else if (PreferenceHelper.promptSearch()
                            && !PreferenceHelper.getSearchProvider().equals("none")) {
                        Utils.doWebSearch(LauncherActivity.this,
                                PreferenceHelper.getSearchProvider(),
                                searchBar.getText().toString());
                    }
                }
                return true;
            }
        });
    }

    /**
     * Listeners for the app list.
     */
    private void addListListeners() {
        // Listen for app list scroll to hide/show favourites panel.
        // Only do this when the user has favourites panel enabled.
        appsRecyclerView.addOnScrollListener(new SimpleScrollListener(48) {
            @Override
            public void onScrollUp() {
                if (!pinnedAppsAdapter.isEmpty()
                        && isFavouritesVisible
                        && PreferenceHelper.favouritesAcceptScroll()) {
                    doThis("hide_favourites");
                }
            }

            @Override
            public void onScroll() {
                doThis("dismiss_menu");
            }

            @Override
            public void onEnd() {
                if (!pinnedAppsAdapter.isEmpty()
                        && !isFavouritesVisible
                        && PreferenceHelper.favouritesAcceptScroll()) {
                    doThis("show_favourites");
                }
            }
        });

        // Add item click action to app list.
        appsAdapter.addListener(new FlexibleAdapter.OnItemClickListener() {
            @Override public boolean onItemClick(View view, int position) {
                AppUtils.launchApp(LauncherActivity.this,
                        Utils.requireNonNull(appsAdapter.getItem(position)));
                return true;
            }
        });

        // Add item click action to the favourites panel.
        pinnedAppsAdapter.addListener(new FlexibleAdapter.OnItemClickListener() {
            @Override public boolean onItemClick(View view, int position) {
                AppUtils.launchApp(LauncherActivity.this,
                        Utils.requireNonNull(pinnedAppsAdapter.getItem(position)));
                return true;
            }
        });

        // Add long click listener to apps in the apps list.
        // This shows a menu to manage the selected app.
        appsAdapter.addListener(new FlexibleAdapter.OnItemLongClickListener() {
            @Override public void onItemLongClick(int position) {
                App app = Utils.requireNonNull(appsAdapter.getItem(position));

                // We need to rely on the LayoutManager here
                // because app list is populated asynchronously,
                // and will throw nulls if we try to directly ask RecyclerView for its child.
                createAppMenu(Utils.requireNonNull(appsRecyclerView.getLayoutManager())
                                   .findViewByPosition(position), false, app);
            }
        });

        // Also add a similar long click action for the favourites panel.
        pinnedAppsAdapter.addListener(new FlexibleAdapter.OnItemLongClickListener() {
            @Override public void onItemLongClick(int position) {
                App app = Utils.requireNonNull(pinnedAppsAdapter.getItem(position));

                // Use LayoutManager method to get the view,
                // as RecyclerView will happily return null if it can.
                createAppMenu(Utils.requireNonNull(pinnedAppsRecyclerView.getLayoutManager())
                                   .findViewByPosition(position), true, app);
            }
        });

        appsAdapter.addListener(new FlexibleAdapter.OnUpdateListener() {
            @Override public void onUpdateEmptyView(int size) {
                if (size > 0 && !appsAdapter.isEmpty()) {
                    loadProgress.setVisibility(View.GONE);
                    loadProgress.invalidate();
                }
            }
        });
    }

    /**
     * Listener for adapters.
     * TODO: Maybe this can be moved to ListListener (or that can go here instead)?
     */
    private void addAdapterListener() {
        pinnedAppsAdapter.addListener(new FlexibleAdapter.OnItemMoveListener() {
            @Override public boolean shouldMoveItem(int fromPosition, int toPosition) {
                return true;
            }

            @Override public void onItemMove(int fromPosition, int toPosition) {
                // Close app menu when we're dragging.
                doThis("dismiss_menu");

                // Shuffle our apps around.
                pinnedAppsAdapter.swapItems(pinnedAppList, fromPosition, toPosition);
                pinnedAppsAdapter.notifyItemMoved(fromPosition, toPosition);
            }

            @Override public void onActionStateChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                updatePinnedApps(false);
            }
        });
    }

    /**
     * Listeners for the app panel.
     */
    private void addPanelListener() {
        slidingHome.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override public void onPanelSlide(View view, float v) {
                // Hide the keyboard at slide.
                ActivityServiceUtils.hideSoftKeyboard(LauncherActivity.this);

                // Dismiss any visible menu.
                doThis("dismiss_menu");
            }

            @Override public void onPanelStateChanged(View panel, int previousState, int newState) {
                appsLayoutManager.setVerticalScrollEnabled(
                        newState == SlidingUpPanelLayout.PanelState.COLLAPSED);
                searchBar.setClickable(newState == SlidingUpPanelLayout.PanelState.COLLAPSED);
                searchBar.setLongClickable(newState == SlidingUpPanelLayout.PanelState.COLLAPSED);

                switch (newState) {
                    case SlidingUpPanelLayout.PanelState.DRAGGING:
                        // Empty out search bar text

                        // Clear the search bar text if app list is set to be kept open
                        // unless keepLastSearch setting indicates maintain last search
                        if (!PreferenceHelper.keepLastSearch()) {
                            clearSearch(searchBar);
                        }

                        // Preemptive attempt at showing the keyboard.
                        if (PreferenceHelper.shouldFocusKeyboard()) {
                            ActivityServiceUtils.showSoftKeyboard(LauncherActivity.this, searchBar);
                        }

                        // Animate search container entering the view.
                        if (!ActivityServiceUtils.isPowerSaving(LauncherActivity.this)) {
                            searchContainer.animate().alpha(1f).setDuration(animateDuration)
                                           .setListener(new AnimatorListenerAdapter() {
                                               @Override
                                               public void onAnimationStart(Animator animation) {
                                                   searchContainer.setVisibility(View.VISIBLE);
                                               }

                                               @Override
                                               public void onAnimationEnd(Animator animation) {
                                                   searchContainer.clearAnimation();
                                               }
                                           });
                        } else {
                            searchContainer.setVisibility(View.VISIBLE);
                        }
                        break;
                    case SlidingUpPanelLayout.PanelState.COLLAPSED:
                        // Show the keyboard.
                        if (PreferenceHelper.shouldFocusKeyboard()
                                && previousState == SlidingUpPanelLayout.PanelState.DRAGGING) {
                            ActivityServiceUtils.showSoftKeyboard(LauncherActivity.this, searchBar);
                        }
                        break;
                    case SlidingUpPanelLayout.PanelState.EXPANDED:
                        // Hide keyboard if container is invisible.
                        ActivityServiceUtils.hideSoftKeyboard(LauncherActivity.this);

                        // Stop scrolling, the panel is being dismissed.
                        appsRecyclerView.stopScroll();

                        searchContainer.setVisibility(View.INVISIBLE);

                        // Animate the container.
                        if (!isResuming && !ActivityServiceUtils.isPowerSaving(
                                LauncherActivity.this)) {
                            searchContainer.animate().alpha(0f).setDuration(animateDuration);
                        } else {
                            isResuming = false;
                        }
                        break;
                    default:
                        // No-op.
                        break;
                }
            }
        });
    }

    /**
     * Updates the favourites panel.
     *
     * @param restart Should a complete adapter & list re-initialisation be done?
     */
    private void updatePinnedApps(boolean restart) {
        String newAppString = "";

        if (!pinnedAppString.isEmpty() && restart) {
            pinnedAppList.clear();
            pinnedAppsAdapter.updateDataSet(pinnedAppList, false);

            for (String pinnedApp : pinnedAppString.split(";")) {
                String componentName = pinnedApp;
                long user = userUtils.getCurrentSerial();

                // Handle pinned apps coming from another user.
                String[] userSplit = pinnedApp.split("-");
                if (userSplit.length == 2) {
                    user = Long.parseLong(userSplit[0]);
                    componentName = userSplit[1];
                }

                if (AppUtils.doesComponentExist(manager, componentName)) {
                    AppUtils.pinApp(this, user, componentName, pinnedAppsAdapter, pinnedAppList);
                }
            }
        }

        // Iterate through the list to get package name of each pinned apps, then stringify them.
        for (PinnedApp app : pinnedAppList) {
            newAppString = newAppString.concat(app.getUserPackageName() + ";");
        }

        // Update the saved pinned apps.
        PreferenceHelper.update("pinned_apps_list", newAppString);

        pinnedAppString = newAppString;
    }

    public void clearSearch(View v) {
        // Clear the search bar text if app list is set to be kept open
        searchBar.setText("");
    }

    public void showStartDialog() {
        final BottomSheetDialog startDialog = new BottomSheetDialog(this);
        startDialog.setContentView(R.layout.dialog_start_hint);
        startDialog.setCancelable(false);
        startDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        Button startDismiss = startDialog.findViewById(R.id.dismiss);
        if (startDismiss != null) {
            startDismiss.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    startDialog.dismiss();
                    PreferenceHelper.update("is_new_user", false);
                }
            });
        }
        startDialog.show();
    }

    /**
     * Creates a dialog to set an app's shorthand.
     *
     * @param packageName The package name of the app.
     * @param position    Adapter position of the app.
     */
    private void makeRenameDialog(final String packageName, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = View.inflate(this, R.layout.layout_rename_dialog, null);

        final EditText renameField = view.findViewById(R.id.rename_field);
        renameField.setHint(PreferenceHelper.getLabel(packageName));
        builder.setView(view);

        builder.setNegativeButton(android.R.string.cancel, null)
               .setTitle(R.string.dialog_title_shorthand)
               .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                   @Override public void onClick(DialogInterface dialogInterface, int i) {
                       String newLabel = renameField.getText()
                                                    .toString()
                                                    .replaceAll("\\|", "")
                                                    .trim();

                       // Unset shorthand if it is empty.
                       PreferenceHelper.updateLabel(packageName, newLabel, newLabel.isEmpty());

                       // Update the specified item.
                       App app = appsAdapter.getItem(position);

                       if (app != null) {
                           app.setHintName(newLabel);
                       }
                   }
               }).show();
    }
}
