package mono.hg;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.DialogInterface;
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
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;

import androidx.appcompat.app.AlertDialog;
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

    private static int SETTINGS_RETURN_CODE = 12;
    private static int WIDGET_CONFIG_RETURN_CODE = 2;
    /*
     * Should the favourites panel listen for scroll?
     */
    private boolean shouldShowFavourites = true;
    /*
     * Are we resuming this activity?
     */
    private boolean isResuming = false;
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

        PreferenceHelper.update("package_count", AppUtils.countInstalledPackage(manager));

        // Start pinning apps.
        updatePinnedApps(true);

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
                startActivityForResult(new Intent(this, SettingsActivity.class),
                        SETTINGS_RETURN_CODE);
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

        // Refresh app list and pinned apps if there is a change in package count.
        if (AppUtils.hasNewPackage(
                manager) || (appsAdapter.hasFinishedLoading() && appsAdapter.isEmpty())) {
            updatePinnedApps(true);
            new getAppTask(this).execute();
        }

        Utils.registerPackageReceiver(this, packageReceiver);

        // Show the app list when needed.
        if (PreferenceHelper.keepAppList()) {
            doThis("show_panel");
        }

        isResuming = true;
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
        if (Utils.atLeastKitKat()) {
            getWindow().getDecorView()
                       .setSystemUiVisibility(
                               ViewUtils.setWindowbarMode(PreferenceHelper.getWindowBarMode()));
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Handle preference change. Refresh when necessary.
        if (requestCode == SETTINGS_RETURN_CODE && !PreferenceHelper.wasAlien()) {
            recreate();
        }

        if (resultCode == RESULT_OK && data != null) {
            int widgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(widgetId);

            if (requestCode != WIDGET_CONFIG_RETURN_CODE && appWidgetInfo.configure != null) {
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                intent.setComponent(appWidgetInfo.configure);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                startActivityForResult(intent, WIDGET_CONFIG_RETURN_CODE);
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
            PinnedAppDetail selectedPackage = new PinnedAppDetail(packageName);
            position = pinnedAppsAdapter.getGlobalPositionOf(selectedPackage);
        } else {
            AppDetail selectedPackage = new AppDetail(packageName);
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

        appMenu.getMenu().findItem(R.id.action_shorthand).setVisible(!isPinned);

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
                        PreferenceHelper.update("pinned_apps_list", pinnedAppString);
                        if (!PreferenceHelper.isFavouritesEnabled()) {
                            Toast.makeText(MainActivity.this, R.string.warn_pinning,
                                    Toast.LENGTH_SHORT).show();
                        }

                        shouldShowFavourites = PreferenceHelper.isFavouritesEnabled() && pinnedAppsAdapter
                                .getItemCount() >= 1;
                        break;
                    case R.id.action_unpin:
                        pinnedAppList.remove(pinnedAppsAdapter.getItem(finalPosition));
                        pinnedAppsAdapter.removeItem(finalPosition);
                        pinnedAppString = pinnedAppString.replace(packageName + ";", "");
                        PreferenceHelper.update("pinned_apps_list", pinnedAppString);
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
                    case R.id.action_shorthand:
                        makeRenameDialog(packageName, finalPosition);
                        break;
                    case R.id.action_hide:
                        // Add the app's package name to the exclusion list.
                        excludedAppsList.add(packageName);

                        PreferenceHelper.update("hidden_apps", excludedAppsList);

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
            public void onSwipeUp() {
                if (!PreferenceHelper.doSwipeUp().equals("none")
                        && slidingHome.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    AppUtils.launchApp(MainActivity.this, PreferenceHelper.doSwipeUp());
                }
            }

            @Override
            public void onLongPress() {
                // Show context menu when touchReceiver is long pressed when the panel is expanded.
                if (slidingHome.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    touchReceiver.showContextMenu();
                }
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

                    // Hide keyboard if container is invisible.
                    ActivityServiceUtils.hideSoftKeyboard(MainActivity.this);

                    // Stop scrolling, the panel is being dismissed.
                    appsRecyclerView.stopScroll();

                    searchContainer.setVisibility(View.INVISIBLE);

                    // Also animate the container only when we are not resuming.
                    if (!isResuming) {
                        searchContainer.animate().alpha(0f).setDuration(animateDuration);
                        isResuming = false;
                    } else if (ActivityServiceUtils.isPowerSaving(MainActivity.this)) {
                        searchContainer.animate().alpha(0).setDuration(animateDuration);
                    }
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
     * Updates the favourites panel.
     *
     * @param restart Should a complete adapter & list re-initialisation be done?
     */
    private void updatePinnedApps(Boolean restart) {
        String newAppString = "";

        if (!pinnedAppString.isEmpty() && restart) {
            pinnedAppList.clear();
            pinnedAppsAdapter.updateDataSet(pinnedAppList, false);

            for (String pinnedApp : pinnedAppString.split(";")) {
                if (AppUtils.doesComponentExist(manager, pinnedApp)) {
                    AppUtils.pinApp(manager, pinnedApp, pinnedAppsAdapter, pinnedAppList);
                }
            }
        }

        // Iterate through the list to get package name of each pinned apps, then stringify them.
        for (AppDetail appDetail : pinnedAppList) {
            newAppString = newAppString.concat(appDetail.getPackageName() + ";");
        }

        // Update the saved pinned apps.
        PreferenceHelper.update("pinned_apps_list", newAppString);

        pinnedAppString = newAppString;
    }

    /**
     * Creates a dialogue to set an app's shorthand.
     *
     * @param packageName The package name of the app.
     * @param position    Adapter position of the app.
     */
    private void makeRenameDialog(final String packageName, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = View.inflate(this, R.layout.layout_rename_dialogue, null);

        final EditText renameField = view.findViewById(R.id.rename_field);
        renameField.setHint(PreferenceHelper.getLabel(packageName));
        builder.setView(view);

        builder.setNegativeButton(android.R.string.cancel, null)
               .setTitle(R.string.dialogue_title_shorthand)
               .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                   @Override public void onClick(DialogInterface dialogInterface, int i) {
                       String newLabel = renameField.getText().toString().replaceAll("\\|", "").trim();

                       // Unset shorthand when it is empty.
                       if (!newLabel.isEmpty()) {
                           PreferenceHelper.updateLabel(packageName, newLabel);
                       } else {
                           PreferenceHelper.deleteLabel(packageName);
                       }

                       // Update the specified item.
                       AppDetail oldItem = appsAdapter.getItem(position);

                       if (oldItem != null) {
                           AppDetail newItem = new AppDetail(oldItem.getIcon(), oldItem.getAppName(),
                                   packageName, newLabel, false);

                           appsList.set(position, newItem);
                           appsAdapter.updateItem(newItem);
                       }
                   }
               }).show();
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
