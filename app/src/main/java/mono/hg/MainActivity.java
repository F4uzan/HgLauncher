package mono.hg;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.util.ArraySet;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mono.hg.adapters.AppAdapter;
import mono.hg.adapters.PinnedAppAdapter;
import mono.hg.helpers.IconPackHelper;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.helpers.RecyclerClick;
import mono.hg.receivers.PackageChangesReceiver;
import mono.hg.wrappers.OnTouchListener;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private boolean shouldShowFavourites;
    private Integer app_count, animateTime;
    private ArrayList<AppDetail> appList = new ArrayList<>();
    private ArrayList<AppDetail> pinnedAppList = new ArrayList<>();
    private Set<String> excludedAppList = new ArraySet<>();
    private Set<String> pinnedAppSet;
    private PackageManager manager;
    private PinnedAppAdapter pinnedApps = new PinnedAppAdapter(pinnedAppList);
    private AppAdapter apps = new AppAdapter(appList);
    private RecyclerView list, pinned_list;
    private FrameLayout searchContainer, pinnedAppsContainer;
    private RelativeLayout appListContainer;
    private EditText searchBar;
    private SlidingUpPanelLayout slidingHome;
    private View snackHolder, touchReceiver;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editPrefs;
    private PopupMenu appMenu;

    private PackageChangesReceiver packageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load preferences before setting layout to allow for quick theme change.
        loadPref(true);

        setContentView(R.layout.activity_main);

        manager = getPackageManager();

        LinearLayoutManager appListManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);

        final LinearLayoutManager pinnedAppsManager = new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false);

        appListContainer = findViewById(R.id.app_list_container);
        searchContainer = findViewById(R.id.search_container);
        pinnedAppsContainer = findViewById(R.id.pinned_apps_container);
        searchBar = findViewById(R.id.search);
        slidingHome = findViewById(R.id.slide_home);
        touchReceiver = findViewById(R.id.touch_receiver);
        snackHolder = findViewById(R.id.snack_holder);
        list = findViewById(R.id.apps_list);
        pinned_list = findViewById(R.id.pinned_apps_list);

        animateTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        apps.setHasStableIds(true);
        pinnedApps.setHasStableIds(true);

        list.setDrawingCacheEnabled(true);
        list.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        list.setHasFixedSize(true);

        list.setAdapter(apps);
        list.setLayoutManager(appListManager);
        list.setItemAnimator(new DefaultItemAnimator());

        pinned_list.setAdapter(pinnedApps);
        pinned_list.setLayoutManager(pinnedAppsManager);
        pinned_list.setHasFixedSize(true);

        // Restore search bar visibility when available.
        if (savedInstanceState != null) {
            // The search bar shouldn't be invisible when the panel is pulled down,
            // and it shouldn't be visible when the panel isn't visible.
            if (savedInstanceState.getInt("searchVisibility") == View.INVISIBLE
                    && slidingHome.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                searchContainer.setVisibility(View.VISIBLE);
            } else if (savedInstanceState.getInt("searchVisibility") == View.VISIBLE
                    && slidingHome.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                searchContainer.setVisibility(View.INVISIBLE);
            } else if (savedInstanceState.getInt("searchVisibility") == View.GONE) {
                // This can happen and we don't want it.
                if (slidingHome.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    searchContainer.setVisibility(View.VISIBLE);
                } else if (slidingHome.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    searchContainer.setVisibility(View.INVISIBLE);
                }
            }
        }

        // Get icons from icon pack.
        if (!"default".equals(PreferenceHelper.getIconPackName())) {
            if (Utils.isAppInstalled(getPackageManager(), PreferenceHelper.getIconPackName())) {
                new getIconTask(this).execute();
            } else {
                // We can't find the icon pack, so revert back to the default pack.
                editPrefs.putString("icon_pack", "default").apply();
            }
        }

        // Start loading apps and initialising click listeners.
        loadApps();
        addSearchBarListener();
        addGestureListener();
        addListListeners();
        addPanelListener();

        registerForContextMenu(touchReceiver);

        if (packageReceiver == null)
            registerPackageReceiver();

        // Get pinned apps.
        pinnedAppSet = new HashSet<>(prefs.getStringSet("pinned_apps", new HashSet<String>()));
        for (String pinnedApp : pinnedAppSet) {
            Utils.loadSingleApp(manager, pinnedApp, pinnedApps, pinnedAppList, true);
        }

        applyPrefToViews();

        // Save our current app count.
        //TODO: There are better ways to accomplish this.
        app_count = appList.size() - 1;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_force_refresh:
                recreate();
                return true;
            case R.id.update_wallpaper:
                intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                startActivity(Intent.createChooser(intent, getString(R.string.action_wallpaper)));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        switch (key) {
            default:
                // No-op.
                break;
            case "app_theme":
            case "shade_view_switch":
            case "comfy_padding":
            case "dummy_restore":
            case "favourites_panel_switch":
            case "icon_hide_switch":
            case "list_order":
                recreate();
                break;
            case "icon_pack":
                IconPackHelper.clearDrawableCache();
                recreate();
                break;
            case "removedApp":
                editPrefs.putBoolean("removedApp", false).apply();
                editPrefs.remove("removed_app").apply();
                parseAction("panel_up", null);
                // HACK: Recreate, recreate.
                // Sometimes we receive inconsistent result, so just kick the bucket here.
                recreate();
                break;
            case "addApp":
                editPrefs.putBoolean("addApp", false).apply();
                editPrefs.remove("added_app").apply();
                parseAction("panel_up", null);
                // HACK: Recreate after receiving installation.
                // A workaround for app list getting stuck in search result due to filters.
                recreate();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        // Hides the panel if back is pressed.
        parseAction("panel_up", null);
    }

    @Override
    public void onDestroy() {
        try {
            if (packageReceiver != null) {
                unregisterReceiver(packageReceiver);
            }
        } catch (IllegalArgumentException e) {
            Utils.sendLog(3, e.toString());
        }
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();

        try {
            if (packageReceiver != null) {
                this.unregisterReceiver(packageReceiver);
            }
        } catch (IllegalArgumentException e) {
            Utils.sendLog(3, e.toString());
        }

        // You shouldn't be visible.
        if (appMenu != null)
            appMenu.dismiss();
        if (!searchBar.getText().toString().isEmpty())
            apps.getFilter().filter(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPref(false);
        registerPackageReceiver();

        if (PreferenceHelper.shouldDismissOnLeave())
            parseAction("panel_up", null);

        searchBar.setText(null);
        apps.setUpdateFilter(true);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Utils.sendLog(3, "KeyUp received");
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.isCtrlPressed()) {
            // Get selected text for cut and copy.
            int start = searchBar.getSelectionStart();
            int end = searchBar.getSelectionEnd();
            final String text = searchBar.getText().toString().substring(start, end);

            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            switch (keyCode) {
                case KeyEvent.KEYCODE_A:
                    searchBar.selectAll();
                    return true;
                case KeyEvent.KEYCODE_X:
                    searchBar.setText(searchBar.getText().toString().replace(text, ""));
                    return true;
                case KeyEvent.KEYCODE_C:
                    ClipData clipData = ClipData.newPlainText(null, text);
                    if (clipboardManager != null) {
                        clipboardManager.setPrimaryClip(clipData);
                    }
                    return true;
                case KeyEvent.KEYCODE_V:
                    if (clipboardManager != null && clipboardManager.hasPrimaryClip()
                            && clipboardManager.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                        CharSequence textToPaste = clipboardManager.getPrimaryClip().getItemAt(0).getText();
                        searchBar.setText(searchBar.getText().replace(Math.min(start, end), Math.max(start, end),
                                textToPaste, 0, textToPaste.length()));
                    }
                    return true;
                default:
                    return super.onKeyUp(keyCode, event);
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save search bar visibility state.
        savedInstanceState.putInt("searchVisibility", searchContainer.getVisibility());
        super.onSaveInstanceState(savedInstanceState);
    }

    private void loadApps() {
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> availableActivities = manager.queryIntentActivities(i, 0);

        if (!PreferenceHelper.isListInverted()) {
            Collections.sort(availableActivities, Collections
                    .reverseOrder(new ResolveInfo.DisplayNameComparator(manager)));
        } else {
            Collections.sort(availableActivities, new ResolveInfo.DisplayNameComparator(manager));
        }
        // Clear the list to make sure that we aren't just adding over an existing list.
        appList.clear();
        apps.notifyItemRangeChanged(0, 0);

        // Fetch and add every app into our list, but ignore those that are in the exclusion list.
        for (ResolveInfo ri : availableActivities) {
            String packageName = ri.activityInfo.packageName;
            if (!excludedAppList.contains(packageName)) {
                String appName = ri.loadLabel(manager).toString();
                Drawable icon = null;
                Drawable getIcon = null;
                // Only show icons if user chooses so.
                if (!PreferenceHelper.shouldHideIcon()) {
                    if (!PreferenceHelper.getIconPackName().equals("default"))
                        getIcon = new IconPackHelper().getIconDrawable(manager, packageName);
                    if (getIcon == null) {
                        icon = ri.activityInfo.loadIcon(manager);
                    } else {
                        icon = getIcon;
                    }
                }
                AppDetail app = new AppDetail(icon, appName, packageName,false);
                appList.add(app);
                apps.notifyItemInserted(appList.size());
            }
        }

        // Update our view cache size, now that we have got all apps on the list
        list.setItemViewCacheSize(appList.size() - 1);
    }

    // A method to launch an app based on package name.
    private void launchApp(String packageName) {
        Intent i = manager.getLaunchIntentForPackage(packageName);
        // Attempt to catch exceptions instead of crash landing directly to the floor.
        try {
            // Override app launch animation when needed.
            startActivity(i);
            switch (PreferenceHelper.getLaunchAnim()) {
                case "pull_up":
                    overridePendingTransition(R.anim.pull_up, 0);
                    break;
                case "slide_in":
                    overridePendingTransition(R.anim.slide_in, 0);
                    break;
                default:
                case "default":
                    // Don't override when we have the default value.
                    break;
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(MainActivity.this, R.string.err_activity_not_found, Toast.LENGTH_LONG).show();
        } catch (NullPointerException e) {
            Toast.makeText(MainActivity.this, R.string.err_activity_null, Toast.LENGTH_LONG).show();
        }
    }

    private static class getIconTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<MainActivity> activityRef;

        getIconTask(MainActivity context) {
            activityRef = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... params) {
            MainActivity activity = activityRef.get();
            if (activity != null)
                new IconPackHelper().loadIconPack(activity.getPackageManager());
            return null;
        }
    }

    private void parseAction(String action, @Nullable View actionContext) {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        switch (action) {
            default:
                // Don't do anything.
                break;
            case "panel_down":
                if (slidingHome.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED)
                    slidingHome.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                break;
            case "panel_up":
                if (slidingHome.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED
                        || slidingHome.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)
                    slidingHome.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                break;
            case "hide_keyboard":
                if (inputManager != null && actionContext != null)
                    inputManager.hideSoftInputFromWindow(actionContext.getWindowToken(), 0);
                break;
            case "show_keyboard":
                if (inputManager != null && actionContext != null) {
                    inputManager.showSoftInput(actionContext, InputMethodManager.SHOW_IMPLICIT);
                    actionContext.requestFocus();
                }
                break;
            case "show_favourites_animate":
                pinnedAppsContainer.animate().cancel();

                if (PreferenceHelper.isFavouritesEnabled() && pinnedAppList.size() > 0) {
                    pinnedAppsContainer.animate()
                            .translationY(0f)
                            .setInterpolator(new FastOutSlowInInterpolator())
                            .setDuration(animateTime)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationStart(Animator animator) {
                                    pinnedAppsContainer.setVisibility(View.VISIBLE);
                                }
                            });
                }
                break;
            case "hide_favourites_animate":
                pinnedAppsContainer.animate().cancel();

                pinnedAppsContainer.animate()
                        .translationY(pinnedAppsContainer.getHeight())
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .setDuration(animateTime)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animator) {
                                pinnedAppsContainer.setVisibility(View.GONE);
                            }
                        });
                break;
            case "show_favourites":
                pinnedAppsContainer.setVisibility(View.VISIBLE);
                break;
            case "hide_favourites":
                pinnedAppsContainer.setVisibility(View.GONE);
                break;
        }
    }

    private void applyPrefToViews() {
        // Workaround v21+ statusbar transparency issue.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            ViewGroup.MarginLayoutParams homeParams = (ViewGroup.MarginLayoutParams) slidingHome.getLayoutParams();
            homeParams.topMargin = Utils.getStatusBarHeight(getResources());
        }

        // Empty out margins if they are not needed.
        if (!PreferenceHelper.usesComfyPadding()) {
            ViewGroup.MarginLayoutParams searchParams = (ViewGroup.MarginLayoutParams) searchContainer.getLayoutParams();
            ViewGroup.MarginLayoutParams listParams = (ViewGroup.MarginLayoutParams) appListContainer.getLayoutParams();
            searchParams.setMargins(0, 0, 0, 0);
            listParams.setMargins(0, 0, 0, 0);
        }

        // Hide the favourites panel when user chooses to disable it or when there's nothing to show.
        if (!PreferenceHelper.isFavouritesEnabled() || pinnedAppList.size() == 0)
            parseAction("hide_favourites", null);

        // Switch on wallpaper shade.
        if (PreferenceHelper.useWallpaperShade()) {
            View wallpaperShade = findViewById(R.id.wallpaper_shade);
            // Tints the navigation bar with a semi-transparent shade.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setNavigationBarColor(getResources().getColor(R.color.navigationBarShade));
            }
            wallpaperShade.setBackgroundResource(R.drawable.image_inner_shadow);
        }
    }

    // Load available preferences.
    private void loadPref(Boolean isInit) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editPrefs = prefs.edit();

        PreferenceHelper.fetchPreference(prefs);

        if (isInit) {
            prefs.registerOnSharedPreferenceChangeListener(this);

            // Get a list of our hidden apps, default to null if there aren't any.
            excludedAppList.addAll(prefs.getStringSet("hidden_apps", excludedAppList));

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

    private void registerPackageReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addDataScheme("package");
        packageReceiver = new PackageChangesReceiver();
        registerReceiver(packageReceiver, intentFilter);
    }

    private void createAppMenu(View v, Boolean isPinned, final String packageName) {
        AppDetail selectedPackage = new AppDetail(null, null, packageName, false);
        final Uri packageNameUri = Uri.parse("package:" + packageName);

        int position;
        if (isPinned) {
            position = pinnedAppList.indexOf(selectedPackage);
        } else {
            position = appList.indexOf(selectedPackage);
        }

        // Inflate the app menu.
        appMenu = new PopupMenu(MainActivity.this, v);
        appMenu.getMenuInflater().inflate(R.menu.menu_app, appMenu.getMenu());

        if (isPinned) {
            appMenu.getMenu().removeItem(R.id.action_pin);
            appMenu.getMenu().removeItem(R.id.action_hide);
        } else {
            // Don't show the 'pin' action when the app is already pinned.
            if (pinnedAppList.contains(selectedPackage))
                appMenu.getMenu().removeItem(R.id.action_pin);
            appMenu.getMenu().removeItem(R.id.action_unpin);
        }

        // Remove uninstall menu if the app is a system app.
        if (Utils.isSystemApp(getPackageManager(), packageName)) {
            appMenu.getMenu().removeItem(R.id.action_uninstall);
        }

        appMenu.show();

        final int finalPosition = position;
        appMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_pin:
                        Utils.loadSingleApp(getPackageManager(), packageName, pinnedApps, pinnedAppList, true);
                        pinnedAppSet.add(packageName);
                        editPrefs.putStringSet("pinned_apps", pinnedAppSet).apply();
                        if (!PreferenceHelper.isFavouritesEnabled())
                            Toast.makeText(MainActivity.this, R.string.warn_pinning, Toast.LENGTH_SHORT).show();
                        if (PreferenceHelper.isFavouritesEnabled() && pinnedAppList.size() == 1) {
                            parseAction("show_favourites_animate", null);
                            shouldShowFavourites = true;
                        }
                        break;
                    case R.id.action_unpin:
                        pinnedAppList.remove(finalPosition);
                        pinnedApps.notifyItemRemoved(finalPosition);
                        pinnedAppSet.remove(packageName);
                        editPrefs.putStringSet("pinned_apps", pinnedAppSet).apply();
                        if (pinnedAppList.size() == 0)
                            parseAction("hide_favourites_animate", null);
                        break;
                    case R.id.action_info:
                        startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                packageNameUri));
                        break;
                    case R.id.action_uninstall:
                        startActivity(new Intent(Intent.ACTION_DELETE, packageNameUri));
                        break;
                    case R.id.action_hide:
                        // Add the app's package name to the exclusion list.
                        excludedAppList.add(packageName);
                        editPrefs.putStringSet("hidden_apps", excludedAppList).apply();
                        // Reload the app list!
                        appList.remove(new AppDetail(null, null, packageName, false));
                        apps.notifyItemRemoved(finalPosition);
                        if (searchBar.getText().toString().equals("")) {
                            apps.setUpdateFilter(true);
                        } else {
                            //TODO: Remove this when loadApps become less of a behemoth.
                            recreate();
                        }
                        break;
                    default:
                        // There is nothing to do.
                        break;
                }
                return true;
            }
        });
    }

    private void addGestureListener() {
        // Handle touch events in touchReceiver.
        touchReceiver.setOnTouchListener(new OnTouchListener(this) {
            @Override
            public void onSwipeDown() {
                // Show the app panel.
                parseAction("panel_down", null);
            }

            @Override
            public void onLongPress() {
                // Show context menu when touchReceiver is long pressed.
                touchReceiver.showContextMenu();
            }

            @Override
            public void onClick() {
                // Imitate sliding panel drag view behaviour; show the app panel on click.
                if (PreferenceHelper.allowTapToOpen())
                    parseAction("panel_down", null);
            }
        });
    }

    private void addSearchBarListener() {
        // Implement listener for the search bar.
        searchBar.addTextChangedListener(new TextWatcher() {
            String searchBarText, searchHint;
            Snackbar searchSnack = Snackbar.make(snackHolder, searchHint, Snackbar.LENGTH_INDEFINITE);

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Fetch texts for the snackbar.
                searchBarText = searchBar.getText().toString().trim();
                searchHint = String.format(getResources().getString(R.string.search_web_hint), searchBarText);

                // Begin filtering our list.
                apps.getFilter().filter(s);
                if (apps.shouldUpdateFilter())
                    apps.setUpdateFilter(false);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Don't allow spamming of empty spaces.
                if (s.length() > 0 && s.charAt(0) == ' ')
                    s.delete(0, 1);

                if (s.length() == 0) {
                    // Scroll back down to the start of the list if search query is empty.
                    list.getLayoutManager().scrollToPosition(app_count);

                    // Dismiss the search snackbar.
                    searchSnack.dismiss();

                    // Summon our favourites panel back.
                    if (!PreferenceHelper.isFavouritesEnabled() || pinnedAppList.size() == 0) {
                        parseAction("hide_favourites", null);
                    } else {
                        shouldShowFavourites = true;
                        parseAction("show_favourites_animate", null);
                    }
                } else if (s.length() > 0 && PreferenceHelper.promptSearch()) {
                    // Update the snackbar text.
                    searchSnack.setText(searchHint);

                    // Prompt user if they want to search their query online.
                    searchSnack.setAction(R.string.search_web_button, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            synchronized (this) {
                                Utils.openLink(MainActivity.this, PreferenceHelper.getSearchProvider() + searchBarText);
                                apps.getFilter().filter(null);
                            }
                        }
                    }).show();

                    // Disable search snackbar swipe-to-dismiss.
                    Utils.disableSnackbarSwipe(searchSnack);
                }
            }
        });

        // Listen for keyboard enter/search key input.
        searchBar.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_NULL) {
                    if (!appList.isEmpty() && searchBar.getText().length() > 0) {
                        launchApp(appList.get(0).getPackageName());
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void addListListeners() {
        // If favourites panel is enabled and it has pinned apps, we listen for its layout change.
        // If not, then we rely of the app list to supply its own listener.
        if (PreferenceHelper.isFavouritesEnabled() && pinnedAppList.size() > 0) {
            pinnedAppsContainer.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    // Scroll app list down when favourites panel is being pushed by the keyboard.
                    list.scrollToPosition(list.getAdapter().getItemCount() - 1);
                }
            });
        } else {
            list.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    // Scroll app list down when being pushed by the keyboard.
                    list.scrollToPosition(list.getAdapter().getItemCount() - 1);
                }
            });
        }

        // Listen for app list scroll to hide/show favourites panel.
        // Only do this when the user has favourites panel enabled.
        if (PreferenceHelper.isFavouritesEnabled()) {
            list.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    // When the favourites panel is replaced/swapped out,
                    // we should not be calling it until we are told to.
                    if (pinnedAppList.size() > 0) {
                        if (!recyclerView.canScrollVertically(RecyclerView.FOCUS_DOWN) && shouldShowFavourites) {
                            parseAction("show_favourites", null);
                        } else if (recyclerView.canScrollVertically(RecyclerView.FOCUS_UP) && shouldShowFavourites) {
                            parseAction("hide_favourites", null);
                        }
                    }
                }
            });
        }

        // Add short click/click listener to the app list.
        RecyclerClick.addTo(list).setOnItemClickListener(new RecyclerClick.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                synchronized (this) {
                    launchApp(appList.get(position).getPackageName());
                    apps.getFilter().filter(null);
                }
            }
        });

        // Add long click action to app list. Long click shows a menu to manage selected app.
        RecyclerClick.addTo(list).setOnItemLongClickListener(new RecyclerClick.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, final int position, View v) {
                // Parse package URI for use in uninstallation and package info call.
                final String packageName = appList.get(position).getPackageName();
                createAppMenu(v, false, packageName);
                return false;
            }
        });

        // Add long click action to pinned apps.
        RecyclerClick.addTo(pinned_list).setOnItemLongClickListener(new RecyclerClick.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, final int position, View v) {
                // Parse package URI for use in uninstallation and package info call.
                final String packageName = pinnedAppList.get(position).getPackageName();
                createAppMenu(v, true, packageName);
                return false;
            }
        });

        RecyclerClick.addTo(pinned_list).setOnItemClickListener(new RecyclerClick.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                launchApp(pinnedAppList.get(position).getPackageName());
            }
        });
    }

    private void addPanelListener() {
        slidingHome.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View view, float v) {
                // Hide the keyboard at slide.
                parseAction("hide_keyboard", searchBar);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED
                        || newState == SlidingUpPanelLayout.PanelState.DRAGGING) {
                    // Empty out search bar text
                    searchBar.setText(null);

                    // Automatically show keyboard when the panel is called.
                    if (PreferenceHelper.shouldFocusKeyboard()
                            && previousState != SlidingUpPanelLayout.PanelState.COLLAPSED) {
                        parseAction("show_keyboard", searchBar);
                    }
                    // Animate search container entering the view.
                    searchContainer.animate().alpha(1.0f).setDuration(animateTime)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                    searchContainer.setVisibility(View.VISIBLE);
                                }
                            });
                } else if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    // Hide keyboard if container is invisible.
                    parseAction("hide_keyboard", searchBar);

                    // Also animate the container when it's disappearing.
                    searchContainer.animate().alpha(0.0f).setDuration(animateTime)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    searchContainer.setVisibility(View.INVISIBLE);
                                }
                            });
                } else if (newState == SlidingUpPanelLayout.PanelState.ANCHORED) {
                    slidingHome.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                }
            }
        });
    }
}
