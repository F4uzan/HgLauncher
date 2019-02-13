package mono.hg;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import mono.hg.adapters.AppAdapter;
import mono.hg.appwidget.LauncherAppWidgetHost;
import mono.hg.appwidget.LauncherAppWidgetHostView;
import mono.hg.helpers.LauncherIconHelper;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.models.AppDetail;
import mono.hg.models.PinnedAppDetail;
import mono.hg.receivers.PackageChangesReceiver;
import mono.hg.utils.ActivityServiceUtils;
import mono.hg.utils.AppUtils;
import mono.hg.utils.Utils;
import mono.hg.utils.ViewUtils;
import mono.hg.views.IndeterminateMaterialProgressBar;
import mono.hg.views.TogglingLinearLayoutManager;
import mono.hg.wrappers.OnTouchListener;
import mono.hg.wrappers.SimpleScrollListener;

public class MainActivity extends AppCompatActivity {

    /*
     * Should the favourites panel listen for scroll?
     */
    private boolean shouldShowFavourites = true;

    /*
     * Animation duration; fetched from system's duration.
     */
    private int animateDuration;

    /*
     * List containing installed apps.
     */
    private ArrayList<AppDetail> appsList = new ArrayList<>();

    /*
     * Adapter for installed apps.
     */
    private AppAdapter appsAdapter = new AppAdapter(appsList);

    /*
     * List containing pinned apps.
     */
    private ArrayList<PinnedAppDetail> pinnedAppList = new ArrayList<>();

    /*
     * String containing pinned apps. Delimited by a semicolon (;).
     */
    private String pinnedAppString;

    /*
     * Adapter for pinned apps.
     */
    private FlexibleAdapter<PinnedAppDetail> pinnedAppsAdapter = new FlexibleAdapter<>(
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
     * RecyclerView for app list.
     */
    private RecyclerView appsRecyclerView;

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
    private FrameLayout searchContainer;

    /*
     * Parent layout of pinned apps' RecyclerView.
     */
    private FrameLayout pinnedAppsContainer;

    /*
     * Parent layout for installed app list.
     */
    private RelativeLayout appListContainer;

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
     * CoordinatorLayout hosting the search snackbar.
     */
    private View snackHolder;

    /*
     * A view used to intercept gestures and taps in the desktop.
     */
    private View touchReceiver;

    /*
     * View containing widget in the desktop.
     */
    private FrameLayout appWidgetContainer;

    /*
     * Progress bar shown when populating app list.
     */
    private IndeterminateMaterialProgressBar loadProgress;

    /*
     * Menu shown when long-pressing apps.
     */
    private PopupMenu appMenu;

    /*
     * Receiver used to listen to installed/uninstalled packages.
     */
    private PackageChangesReceiver packageReceiver = new PackageChangesReceiver();

    /*
     * Used to handle and add widgets to widgetContainer.
     */
    private AppWidgetManager appWidgetManager;
    private LauncherAppWidgetHost appWidgetHost;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load preferences before setting layout to allow for quick theme change.
        loadPref(true);

        setContentView(R.layout.activity_main);

        manager = getPackageManager();

        appsLayoutManager = new TogglingLinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);

        appsLayoutManager.setStackFromEnd(true);

        final LinearLayoutManager pinnedAppsManager = new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false);

        appListContainer = findViewById(R.id.app_list_container);
        searchContainer = findViewById(R.id.search_container);
        pinnedAppsContainer = findViewById(R.id.pinned_apps_container);
        searchBar = findViewById(R.id.search);
        slidingHome = findViewById(R.id.slide_home);
        touchReceiver = findViewById(R.id.touch_receiver);
        appWidgetContainer = findViewById(R.id.widget_container);
        snackHolder = findViewById(R.id.snack_holder);
        appsRecyclerView = findViewById(R.id.apps_list);
        pinnedAppsRecyclerView = findViewById(R.id.pinned_apps_list);
        loadProgress = findViewById(R.id.load_progress);

        appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        appWidgetHost = new LauncherAppWidgetHost(getApplicationContext(), 0);

        animateDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        // Let the launcher handle state of the sliding panel.
        slidingHome.disallowHiding(true);
        slidingHome.alwaysResetState(true);

        appsRecyclerView.setDrawingCacheEnabled(true);
        appsRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        appsRecyclerView.setHasFixedSize(true);

        appsRecyclerView.setAdapter(appsAdapter);
        appsRecyclerView.setLayoutManager(appsLayoutManager);
        appsRecyclerView.setItemAnimator(new DefaultItemAnimator());

        pinnedAppsRecyclerView.setAdapter(pinnedAppsAdapter);
        pinnedAppsRecyclerView.setLayoutManager(pinnedAppsManager);
        pinnedAppsRecyclerView.setItemAnimator(null);

        pinnedAppsAdapter.setLongPressDragEnabled(true);

        // Restore search bar visibility when panel is pulled down.
        if (savedInstanceState != null && ViewUtils.isPanelVisible(slidingHome)) {
            searchContainer.setVisibility(View.VISIBLE);
        }

        // Get icons from icon pack.
        if (!"default".equals(PreferenceHelper.getIconPackName()) &&
                LauncherIconHelper.loadIconPack(manager) == 0) {
            PreferenceHelper.getEditor().putString("icon_pack", "default").apply();
        }

        // Start loading apps and initialising click listeners.
        new getAppTask(this).execute();
        addSearchBarTextListener();
        addSearchBarEditorListener();
        addGestureListener();
        addAdapterListener();
        addListListeners();
        addPanelListener();

        registerForContextMenu(touchReceiver);

        PreferenceHelper.getEditor()
                        .putInt("package_count", AppUtils.countInstalledPackage(manager)).apply();

        if (!pinnedAppString.isEmpty()) {
            for (String pinnedApp : pinnedAppString.split(";")) {
                if (AppUtils.doesComponentExist(manager, pinnedApp)) {
                    AppUtils.pinApp(manager, pinnedApp, pinnedAppsAdapter, pinnedAppList);
                } else {
                    Utils.sendLog(3, "Not pinning " + pinnedApp + "; is app installed?");
                }
            }
        }

        applyPrefToViews();

        // Show the app list once all the views are set up.
        if (PreferenceHelper.keepAppList()) {
            doThis("show_panel");
        }
    }

    @Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        // Show the widget options when the user is in testing mode.
        // Also show/hide them selectively based on widget availability.
        menu.findItem(R.id.action_add_widget)
            .setVisible(PreferenceHelper.isTesting() && !PreferenceHelper.hasWidget());
        menu.findItem(R.id.action_remove_widget)
            .setVisible(PreferenceHelper.isTesting() && PreferenceHelper.hasWidget());
    }

    @Override public boolean onContextItemSelected(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), 12);
                return true;
            case R.id.action_force_refresh:
                recreate();
                return true;
            case R.id.action_add_widget:
                int appWidgetId = appWidgetHost.allocateAppWidgetId();
                Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
                pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                startActivityForResult(pickIntent, 1);
                return true;
            case R.id.action_remove_widget:
                removeWidget();
                PreferenceHelper.fetchPreference();
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
        doThis("hide_panel");
    }

    @Override public void onPause() {
        super.onPause();

        // Dismiss any visible menu as well as the app panel when it is not needed.
        if (appMenu != null) {
            appMenu.dismiss();
        }
        if (!PreferenceHelper.keepAppList()) {
            doThis("hide_panel");
        }

        Utils.unregisterPackageReceiver(this, packageReceiver);
    }

    @Override public void onResume() {
        super.onResume();

        loadPref(false);

        // Set app list animation duration.
        slidingHome.setPanelDurationMultiplier(Settings.System.getFloat(getContentResolver(),
                Settings.System.TRANSITION_ANIMATION_SCALE, 0));

        if (AppUtils.hasNewPackage(
                manager) || (appsAdapter.hasFinishedLoading() && appsAdapter.isEmpty())) {
            new getAppTask(this).execute();
        }

        Utils.registerPackageReceiver(this, packageReceiver);

        // Show the app list when needed.
        if (PreferenceHelper.keepAppList()) {
            doThis("show_panel");
        }
    }

    @Override public void onStart() {
        super.onStart();

        // Restart the launcher in case of an alien call.
        if (PreferenceHelper.wasAlien()) {
            PreferenceHelper.isAlien(false);
            recreate();
        }

        if (PreferenceHelper.hasWidget()) {
            appWidgetHost.startListening();
        }

        // Reset the app list filter.
        appsAdapter.resetFilter();
    }

    @Override public void onStop() {
        super.onStop();
        if (PreferenceHelper.hasWidget()) {
            appWidgetHost.stopListening();
        }
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        // See if any of the system bars needed hiding.
        switch (PreferenceHelper.getWindowBarMode()) {
            case "status":
                if (Utils.atLeastKitKat()) {
                    getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                break;
            case "nav":
                if (Utils.atLeastKitKat()) {
                    getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                break;
            case "both":
                if (Utils.atLeastKitKat()) {
                    getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                break;
            case "none":
            default:
                // Do nothing.
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Handle preference change. 12 is arbitrary, but it should always be the same as
        // startActivity's requestCode.
        //
        // We also don't restart when an alien caller is detected because a recreate() would already
        // be on its way from onStart.
        if (requestCode == 12 && !PreferenceHelper.wasAlien()) {
            recreate();
        }

        if (resultCode == RESULT_OK && data != null) {
            int widgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(widgetId);

            if (requestCode != 2 && appWidgetInfo.configure != null) {
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                intent.setComponent(appWidgetInfo.configure);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                startActivityForResult(intent, 2);
            } else {
                addWidget(data);
            }
        } else if (resultCode == RESULT_CANCELED && data != null) {
            int widgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (widgetId != -1) {
                appWidgetHost.deleteAppWidgetId(widgetId);
            }
        }
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.isCtrlPressed()) {
            // Get selected text for cut and copy.
            int start = searchBar.getSelectionStart();
            int end = searchBar.getSelectionEnd();
            final String text = searchBar.getText().toString().substring(start, end);

            switch (keyCode) {
                case KeyEvent.KEYCODE_A:
                    searchBar.selectAll();
                    return true;
                case KeyEvent.KEYCODE_X:
                    searchBar.setText(searchBar.getText().toString().replace(text, ""));
                    return true;
                case KeyEvent.KEYCODE_C:
                    ActivityServiceUtils.copyToClipboard(this, text);
                    return true;
                case KeyEvent.KEYCODE_V:
                    searchBar.setText(
                            searchBar.getText().replace(Math.min(start, end), Math.max(start, end),
                                    ActivityServiceUtils.pasteFromClipboard(this), 0,
                                    ActivityServiceUtils.pasteFromClipboard(this).length()));
                    return true;
                default:
                    return super.onKeyUp(keyCode, event);
            }
        } else {
            switch (keyCode) {
                case KeyEvent.KEYCODE_ESCAPE:
                    onBackPressed();
                    return true;
                case KeyEvent.KEYCODE_SPACE:
                    if (!searchBar.hasFocus()) {
                        doThis("show_panel");
                    }
                    return true;
                default:
                    return super.onKeyUp(keyCode, event);
            }
        }
    }

    /**
     * A shorthand for various toggles and visibility checks/sets.
     *
     * @param action What to do?
     */
    private void doThis(String action) {
        switch (action) {
            default:
                // Don't do anything.
                break;
            case "show_panel":
                if (!ViewUtils.isPanelVisible(slidingHome)) {
                    slidingHome.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED,
                            ActivityServiceUtils.isPowerSaving(this));
                }
                break;
            case "hide_panel":
                if (ViewUtils.isPanelVisible(slidingHome)) {
                    slidingHome.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED,
                            ActivityServiceUtils.isPowerSaving(this));
                }
                break;
            case "show_favourites":
                pinnedAppsContainer.animate()
                                   .translationY(0f)
                                   .setInterpolator(new LinearOutSlowInInterpolator())
                                   .setDuration(animateDuration)
                                   .setListener(new AnimatorListenerAdapter() {
                                       @Override
                                       public void onAnimationStart(Animator animator) {
                                           pinnedAppsContainer.setVisibility(View.VISIBLE);
                                       }
                                   });
                break;
            case "hide_favourites":
                appsRecyclerView.animate()
                                .translationY(1f)
                                .setInterpolator(new OvershootInterpolator())
                                .setDuration(animateDuration);
                pinnedAppsContainer.animate()
                                   .translationY(pinnedAppsContainer.getMeasuredHeight())
                                   .setInterpolator(new LinearOutSlowInInterpolator())
                                   .setDuration(animateDuration)
                                   .setListener(new AnimatorListenerAdapter() {
                                       @Override
                                       public void onAnimationEnd(Animator animator) {
                                           pinnedAppsContainer.setVisibility(View.GONE);
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

        // Empty out margins if they are not needed.
        if (!PreferenceHelper.usesComfyPadding()) {
            ViewGroup.MarginLayoutParams searchParams = (ViewGroup.MarginLayoutParams) searchContainer
                    .getLayoutParams();
            ViewGroup.MarginLayoutParams listParams = (ViewGroup.MarginLayoutParams) appListContainer
                    .getLayoutParams();
            searchParams.setMargins(0, 0, 0, 0);
            listParams.setMargins(0, 0, 0, 0);
        }

        // Hide the favourites panel when user chooses to disable it or when there's nothing to show.
        if (!PreferenceHelper.isFavouritesEnabled() || pinnedAppsAdapter.isEmpty()) {
            pinnedAppsContainer.setVisibility(View.GONE);
            shouldShowFavourites = false;
        }

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

        // Load widgets if there are any.
        if (PreferenceHelper.hasWidget()) {
            Intent widgetIntent = new Intent();
            widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    PreferenceHelper.getPreference().getInt("widget_id", -1));
            addWidget(widgetIntent);
        }
    }

    /**
     * Loads available preferences and updates PreferenceHelpers.
     *
     * @param isInit Are we loading for onCreate?
     */
    private void loadPref(Boolean isInit) {
        if (!PreferenceHelper.hasEditor()) {
            PreferenceHelper.initPreference(this);
        }
        PreferenceHelper.fetchPreference();

        // Get pinned apps.
        pinnedAppString = PreferenceHelper.getPreference().getString("pinned_apps_list", "");

        if (isInit) {
            // Get a list of our hidden apps, default to null if there aren't any.
            excludedAppsList.addAll(PreferenceHelper.getExclusionList());

            // Set the app theme!
            switch (PreferenceHelper.appTheme()) {
                default:
                case "light":
                    setTheme(R.style.AppTheme_NoActionBar);
                    break;
                case "dark":
                    setTheme(R.style.AppTheme_Gray_NoActionBar);
                    break;
                case "black":
                    setTheme(R.style.AppTheme_Dark_NoActionBar);
                    break;
            }
        }
    }

    /**
     * Creates a PopupMenu to use in a long-pressed app object.
     *
     * @param view        View for the PopupMenu to anchor to.
     * @param isPinned    Is this a pinned app?
     * @param packageName Package name of the app.
     */
    private void createAppMenu(View view, Boolean isPinned, final String packageName) {
        final Uri packageNameUri = Uri.parse("package:" + AppUtils.getPackageName(packageName));

        int position;
        if (isPinned) {
            PinnedAppDetail selectedPackage = new PinnedAppDetail(null, packageName);
            position = pinnedAppsAdapter.getGlobalPositionOf(selectedPackage);
        } else {
            AppDetail selectedPackage = new AppDetail(null, null, packageName, false);
            position = appsAdapter.getGlobalPositionOf(selectedPackage);
        }

        // Inflate the app menu.
        appMenu = new PopupMenu(MainActivity.this, view);
        appMenu.getMenuInflater().inflate(R.menu.menu_app, appMenu.getMenu());

        // Hide 'pin' if the app is already pinned or isPinned is set.
        appMenu.getMenu().findItem(R.id.action_pin)
               .setVisible(!isPinned && !pinnedAppString.contains(packageName));

        // We can't hide an app from the favourites panel.
        appMenu.getMenu().findItem(R.id.action_hide).setVisible(!isPinned);

        // Only show the 'unpin' option if isPinned is set.
        appMenu.getMenu().findItem(R.id.action_unpin).setVisible(isPinned);

        // Show uninstall menu if the app is not a system app.
        appMenu.getMenu().findItem(R.id.action_uninstall)
               .setVisible(!AppUtils.isSystemApp(manager, packageName));

        appMenu.show();

        final int finalPosition = position;
        appMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_pin:
                        AppUtils.pinApp(manager, packageName, pinnedAppsAdapter, pinnedAppList);
                        pinnedAppString = pinnedAppString.concat(packageName + ";");
                        PreferenceHelper.getEditor()
                                        .putString("pinned_apps_list", pinnedAppString)
                                        .apply();
                        if (!PreferenceHelper.isFavouritesEnabled()) {
                            Toast.makeText(MainActivity.this, R.string.warn_pinning,
                                    Toast.LENGTH_SHORT).show();
                        }
                        if (PreferenceHelper.isFavouritesEnabled() && pinnedAppsAdapter.getItemCount() == 1) {
                            shouldShowFavourites = true;
                        }
                        break;
                    case R.id.action_unpin:
                        pinnedAppList.remove(pinnedAppsAdapter.getItem(finalPosition));
                        pinnedAppsAdapter.removeItem(finalPosition);
                        pinnedAppString = pinnedAppString.replace(packageName + ";", "");
                        PreferenceHelper.getEditor()
                                        .putString("pinned_apps_list", pinnedAppString)
                                        .apply();
                        if (pinnedAppsAdapter.isEmpty()) {
                            doThis("hide_favourites");
                        }
                        break;
                    case R.id.action_info:
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                packageNameUri));
                        break;
                    case R.id.action_uninstall:
                        startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageNameUri));
                        break;
                    case R.id.action_hide:
                        // Add the app's package name to the exclusion list.
                        excludedAppsList.add(packageName);
                        PreferenceHelper.getEditor()
                                        .putStringSet("hidden_apps", excludedAppsList)
                                        .apply();
                        // Reload the app list!
                        appsList.remove(appsAdapter.getItem(finalPosition));
                        appsAdapter.removeItem(finalPosition);
                        break;
                    default:
                        // There is nothing to do.
                        break;
                }
                return true;
            }
        });
    }

    /**
     * Listeners for touch receivers.
     * TODO: Implement more swipe actions and listen for them.
     */
    private void addGestureListener() {
        // Handle touch events in touchReceiver.
        touchReceiver.setOnTouchListener(new OnTouchListener(this) {
            @Override
            public void onSwipeDown() {
                // Show the app panel.
                doThis("show_panel");
            }

            @Override
            public void onSwipeRight() {
                if (!PreferenceHelper.doSwipeRight().equals("none")) {
                    AppUtils.launchApp(MainActivity.this, PreferenceHelper.doSwipeRight());
                }
            }

            @Override
            public void onSwipeLeft() {
                if (!PreferenceHelper.doSwipeLeft().equals("none")) {
                    AppUtils.launchApp(MainActivity.this, PreferenceHelper.doSwipeLeft());
                }
            }

            @Override
            public void onLongPress() {
                // Show context menu when touchReceiver is long pressed.
                touchReceiver.showContextMenu();
            }

            @Override
            public void onClick() {
                // Imitate sliding panel drag view behaviour; show the app panel on click.
                if (PreferenceHelper.allowTapToOpen()) {
                    doThis("show_panel");
                }
            }
        });
    }

    /**
     * Listener(s) for widget long click action.
     */
    private void addWidgetActionListener() {
        appWidgetContainer.getChildAt(0).setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View view) {
                appWidgetContainer.getChildAt(0).showContextMenu();
                return true;
            }
        });
    }

    /**
     * Listeners for the search bar query.
     */
    private void addSearchBarTextListener() {
        // Implement listener for the search bar.
        searchBar.addTextChangedListener(new TextWatcher() {
            String searchBarText, searchHint;
            Snackbar searchSnack = Snackbar.make(snackHolder, searchHint,
                    Snackbar.LENGTH_INDEFINITE);

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Fetch texts for the snackbar.
                searchBarText = searchBar.getText().toString().trim();
                searchHint = String.format(getResources().getString(R.string.search_web_hint),
                        searchBarText);

                // Begin filtering our list.
                if (appsAdapter.hasFinishedLoading()) {
                    appsAdapter.setFilter(searchBarText);
                    appsAdapter.filterItems();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Don't allow spamming of empty spaces.
                if (s.length() > 0 && s.charAt(0) == ' ') {
                    s.delete(0, 1);
                }

                if (s.length() == 0) {
                    // Dismiss the search snackbar.
                    searchSnack.dismiss();
                } else if (s.length() > 0 && PreferenceHelper.promptSearch()) {
                    // Update the snackbar text.
                    searchSnack.setText(searchHint);

                    String searchSnackAction;

                    if (PreferenceHelper.getSearchProvider().equals("none")) {
                        searchSnackAction = getString(R.string.search_web_button_prompt);
                    } else {
                        searchSnackAction = getString(R.string.search_web_button);
                    }

                    // Prompt user if they want to search their query online.
                    searchSnack.setAction(searchSnackAction, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!PreferenceHelper.getSearchProvider().equals("none")) {
                                Utils.openLink(MainActivity.this,
                                        PreferenceHelper.getSearchProvider() + URLEncoder.encode(
                                                searchBarText));
                            } else {
                                appMenu = new PopupMenu(MainActivity.this, view);
                                ViewUtils.createSearchMenu(MainActivity.this, appMenu,
                                        URLEncoder.encode(searchBarText));
                            }
                        }
                    }).show();

                    // Disable search snackbar swipe-to-dismiss.
                    ViewUtils.disableSnackbarSwipe(searchSnack);
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
                if ((!appsAdapter.isEmpty() && searchBar.getText().length() > 0) &&
                        (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_NULL)) {
                    if (!appsRecyclerView.canScrollVertically(RecyclerView.FOCUS_UP)
                            && !appsRecyclerView.canScrollVertically(RecyclerView.FOCUS_DOWN)) {
                        AppUtils.launchApp(MainActivity.this, Utils.requireNonNull(
                                appsAdapter.getItem(appsAdapter.getItemCount() - 1))
                                                                   .getPackageName());
                    } else {
                        AppUtils.launchApp(MainActivity.this, Utils.requireNonNull(
                                appsAdapter.getItem(0)).getPackageName());
                    }
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Listeners for the app list.
     */
    private void addListListeners() {
        // Listen for app list scroll to hide/show favourites panel.
        // Only do this when the user has favourites panel enabled.
        if (PreferenceHelper.isFavouritesEnabled()) {
            appsRecyclerView.addOnScrollListener(new SimpleScrollListener(48) {
                @Override
                public void onScrollUp() {
                    if (shouldShowFavourites && !pinnedAppsAdapter.isEmpty() && !PreferenceHelper.favouritesIgnoreScroll()) {
                        doThis("hide_favourites");
                    }
                }

                @Override
                public void onScroll() {
                    if (appMenu != null) {
                        appMenu.dismiss();
                    }
                }

                @Override
                public void onEnd() {
                    if (shouldShowFavourites && !pinnedAppsAdapter.isEmpty() && !PreferenceHelper.favouritesIgnoreScroll()) {
                        doThis("show_favourites");
                    }
                }
            });
        }

        // Add item click action to app list.
        appsAdapter.addListener(new FlexibleAdapter.OnItemClickListener() {
            @Override public boolean onItemClick(View view, int position) {
                AppUtils.launchApp(MainActivity.this,
                        Utils.requireNonNull(appsAdapter.getItem(position))
                             .getPackageName());
                return true;
            }
        });

        // Add item click action to the favourites panel.
        pinnedAppsAdapter.addListener(new FlexibleAdapter.OnItemClickListener() {
            @Override public boolean onItemClick(View view, int position) {
                AppUtils.launchApp(MainActivity.this,
                        Utils.requireNonNull(pinnedAppsAdapter.getItem(position))
                             .getPackageName());
                return true;
            }
        });

        // Add long click listener to apps in the apps list.
        // This shows a menu to manage the selected app.
        appsAdapter.addListener(new FlexibleAdapter.OnItemLongClickListener() {
            @Override public void onItemLongClick(int position) {
                final String packageName = Utils.requireNonNull(
                        appsAdapter.getItem(position)).getPackageName();

                // We need to rely on the LayoutManager here
                // because app list is populated asynchronously,
                // and will throw nulls if we try to directly ask RecyclerView for its child.
                createAppMenu(Utils.requireNonNull(appsRecyclerView.getLayoutManager())
                                   .findViewByPosition(position), false, packageName);
            }
        });

        // Also add a similar long click action for the favourites panel.
        pinnedAppsAdapter.addListener(new FlexibleAdapter.OnItemLongClickListener() {
            @Override public void onItemLongClick(int position) {
                final String packageName = Utils.requireNonNull(
                        pinnedAppsAdapter.getItem(position)).getPackageName();
                createAppMenu(pinnedAppsRecyclerView.getChildAt(position), true, packageName);
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
                appMenu.dismiss();

                // Shuffle our apps around.
                pinnedAppsAdapter.swapItems(pinnedAppList, fromPosition, toPosition);
                pinnedAppsAdapter.notifyItemMoved(fromPosition, toPosition);
            }

            @Override public void onActionStateChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                String orderedPinnedApps = "";

                // Iterate through the list to get package name of each pinned apps, then stringify them.
                for (AppDetail appDetail : pinnedAppList) {
                    orderedPinnedApps = orderedPinnedApps.concat(appDetail.getPackageName() + ";");
                }

                // Update the saved pinned apps.
                PreferenceHelper.getEditor()
                                .putString("pinned_apps_list", orderedPinnedApps)
                                .apply();

                // Also update pinnedAppString for future references.
                pinnedAppString = orderedPinnedApps;
            }
        });
    }

    /**
     * Listeners for the app panel.
     */
    private void addPanelListener() {
        slidingHome.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override public void onPanelSlide(View view, float v) {
                appsLayoutManager.setVerticalScrollEnabled(false);

                // Hide the keyboard at slide.
                ActivityServiceUtils.hideSoftKeyboard(MainActivity.this);

                // Dismiss any visible menu.
                if (appMenu != null) {
                    appMenu.dismiss();
                }
            }

            @Override public void onPanelStateChanged(View panel,
                    SlidingUpPanelLayout.PanelState previousState,
                    SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED
                        || newState == SlidingUpPanelLayout.PanelState.DRAGGING) {

                    if (newState != SlidingUpPanelLayout.PanelState.DRAGGING) {
                        appsLayoutManager.setVerticalScrollEnabled(true);

                        // Hide the widget when the panel is showing.
                        appWidgetContainer.setVisibility(View.INVISIBLE);
                    }

                    // Unregister context menu for touchReceiver as we don't want
                    // the user to accidentally show it during search.
                    unregisterForContextMenu(touchReceiver);

                    // Empty out search bar text
                    searchBar.setText(null);

                    // Automatically show keyboard when the panel is called.
                    if (PreferenceHelper.shouldFocusKeyboard()
                            && previousState != SlidingUpPanelLayout.PanelState.COLLAPSED) {
                        ActivityServiceUtils.showSoftKeyboard(MainActivity.this, searchBar);
                    }

                    // Animate search container entering the view.
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
                } else if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    appsLayoutManager.setVerticalScrollEnabled(false);

                    // Show back the widget when the panel is showing.
                    appWidgetContainer.setVisibility(View.VISIBLE);

                    // Re-register touchReceiver context menu.
                    registerForContextMenu(touchReceiver);

                    // Hide keyboard if container is invisible.
                    ActivityServiceUtils.hideSoftKeyboard(MainActivity.this);

                    // Stop scrolling, the panel is being dismissed.
                    appsRecyclerView.stopScroll();

                    searchContainer.setVisibility(View.INVISIBLE);

                    // Also animate the container when it's disappearing.
                    searchContainer.animate().alpha(0f).setDuration(animateDuration);
                } else if (newState == SlidingUpPanelLayout.PanelState.ANCHORED) {
                    doThis("show_panel");
                }
            }
        });
    }

    /**
     * Adds a widget to the desktop.
     *
     * @param data Intent used to receive the ID of the widget being added.
     */
    private void addWidget(Intent data) {
        int widgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(widgetId);
        AppWidgetHostView appWidgetHostView = appWidgetHost.createView(getApplicationContext(),
                widgetId, appWidgetInfo);

        // Prevents crashing when the widget info can't be found.
        // https://github.com/Neamar/KISS/commit/f81ae32ef5ff5c8befe0888e6ff818a41d8dedb4
        if (appWidgetInfo == null) {
            removeWidget();
        } else {
            // Notify widget of the available minimum space.
            appWidgetHostView.setMinimumHeight(appWidgetInfo.minHeight);
            appWidgetHostView.setMinimumWidth(appWidgetInfo.minWidth);
            appWidgetHostView.setAppWidget(widgetId, appWidgetInfo);
            if (Utils.sdkIsAround(16)) {
                appWidgetHostView.updateAppWidgetSize(null, appWidgetInfo.minWidth,
                        appWidgetInfo.minHeight, appWidgetInfo.minWidth, appWidgetInfo.minHeight);
            }

            // Remove existing widget if any and then add the new widget.
            appWidgetContainer.removeAllViews();
            appWidgetContainer.addView(appWidgetHostView, 0);

            // Immediately listens for the widget.
            appWidgetHost.startListening();
            registerForContextMenu(appWidgetContainer.getChildAt(0));
            addWidgetActionListener();

            // Apply preference changes.
            PreferenceHelper.getEditor()
                            .putInt("widget_id", widgetId)
                            .putBoolean("has_widget", true)
                            .apply();
        }
    }

    /**
     * Removes widget from the desktop and resets the configuration
     * relating to widgets.
     */
    private void removeWidget() {
        LauncherAppWidgetHostView widget = (LauncherAppWidgetHostView) appWidgetContainer.getChildAt(
                0);
        appWidgetContainer.removeView(widget);
        PreferenceHelper.getEditor().remove("widget_id").putBoolean("has_widget", false).apply();
    }

    /**
     * AsyncTask used to load/populate the app list.
     */
    private static class getAppTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<MainActivity> activityRef;
        private ArrayList<AppDetail> tempList = new ArrayList<>();

        getAppTask(MainActivity context) {
            activityRef = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            MainActivity activity = activityRef.get();

            if (activity != null) {
                // Clear the apps list first so we wouldn't add over an existing list.
                activity.appsList.clear();
                activity.appsAdapter.removeRange(0, activity.appsList.size());

                // Show the progress bar so the list wouldn't look empty.
                activity.loadProgress.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            MainActivity activity = activityRef.get();
            if (activity != null) {
                tempList.addAll(AppUtils.loadApps(activity));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void results) {
            MainActivity activity = activityRef.get();
            if (activity != null) {
                // Remove the progress bar.
                activity.loadProgress.setVisibility(View.GONE);
                activity.loadProgress.invalidate();

                // Add the fetched apps and update item view cache.
                activity.appsList.addAll(tempList);
                activity.appsAdapter.updateDataSet(tempList);
                activity.appsRecyclerView.setItemViewCacheSize(activity
                        .appsAdapter.getItemCount() - 1);

                activity.appsAdapter.finishedLoading(true);
            }
        }
    }
}
