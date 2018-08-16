package f4.hubby;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import f4.hubby.helpers.RecyclerClick;
import f4.hubby.receivers.PackageChangesReceiver;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    boolean anim, icon_hide, list_order, shade_view,
            keyboard_focus, dark_theme, dark_theme_black, web_search_enabled,
            comfy_padding;
    String launch_anim, search_provider;
    private ArrayList<AppDetail> appList = new ArrayList<>();
    private Set<String> excludedAppList = new ArraySet<>();
    private PackageManager manager;
    private AppAdapter apps = new AppAdapter(appList);
    private RecyclerView list;
    private CardView searchContainer;
    private EditText searchBar;
    private SlidingUpPanelLayout slidingHome;
    private View snackHolder;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load preferences before setting layout to allow for quick theme change.
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editPrefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
        prefs.registerOnSharedPreferenceChangeListener(this);
        loadPref();

        // Set the theme!
        if (dark_theme && !dark_theme_black) {
            setTheme(R.style.AppTheme_Gray_NoActionBar);
        } else if (dark_theme) {
            setTheme(R.style.AppTheme_Dark_NoActionBar);
        } else {
            setTheme(R.style.AppTheme_NoActionBar);
        }

        setContentView(R.layout.activity_main);

        final View touchReceiver = findViewById(R.id.touch_receiver);
        View wallpaperShade = findViewById(R.id.wallpaper_shade);
        registerForContextMenu(touchReceiver);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);

        FrameLayout appListContainer = findViewById(R.id.app_list_container);

        // Workaround v21+ statusbar transparency issue.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            appListContainer.setPadding(0, getResources().getDimensionPixelSize(R.dimen.statusbar_margin), 0, 0);
        }

        searchContainer = findViewById(R.id.search_container);
        searchBar = findViewById(R.id.search);
        slidingHome = findViewById(R.id.slide_home);

        if (!comfy_padding) {
            slidingHome.setPadding(0, 0, 0, 0);
        }

        snackHolder = findViewById(R.id.snackHolder);

        slidingHome.setDragView(touchReceiver);

        apps.setHasStableIds(true);

        list = findViewById(R.id.apps_list);
        list.setDrawingCacheEnabled(true);
        list.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        list.setHasFixedSize(true);
        
        list.setAdapter(apps);
        list.setLayoutManager(mLayoutManager);
        list.setItemAnimator(new DefaultItemAnimator());

        // Get a list of our hidden apps, default to null if there aren't any.
        excludedAppList.addAll(prefs.getStringSet("hidden_apps", excludedAppList));

        // Start loading apps and initialising click listeners.
        loadApps(false);
        addClickListener();

        registerPackageReceiver();

        // Save our current count.
        //TODO: There are better ways to accomplish this.
        final int app_count = appList.size() - 1;
        
        // Show context menu when touchReceiver is long pressed.
        touchReceiver.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                touchReceiver.showContextMenu();
                return true;
            }
        });

        // Switch on wallpaper shade.
        if (shade_view) {
           wallpaperShade.setBackgroundResource(R.drawable.image_inner_shadow);
       }

       // Implement listener for the search bar.
       searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Begin filtering our list.
                apps.getFilter().filter(s);
                if (apps.shouldUpdateFilter()) {
                    apps.setUpdateFilter(false);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                final String searchBarText = searchBar.getText().toString().trim();
                // Scroll back down to the start of the list if search query is empty.
                if (searchBarText.equals("")) {
                    list.getLayoutManager().scrollToPosition(app_count);
                } else if (!searchBarText.equals("") && web_search_enabled) {
                    // Prompt user if they want to search their query online.
                    String searchHint = String.format(getResources().getString(R.string.search_web_hint), searchBarText);
                    Snackbar.make(snackHolder, searchHint, Snackbar.LENGTH_LONG)
                            .setAction(R.string.search_web_button, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent link = new Intent(Intent.ACTION_VIEW,
                                            Uri.parse(search_provider + searchBarText));
                                    startActivity(link);
                                }
                            }).show();
                }
            }
        });

        // Listen for keyboard enter/search key input.
        searchBar.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH && !searchBar.getText().toString().equals("")) {
                    if (!appList.isEmpty()) {
                        launchApp(appList.get(0).getPackageName());
                        return true;
                    }
                }
                return false;
            }
        });

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
            case R.id.update_wallpaper:
                intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                startActivity(Intent.createChooser(intent, getString(R.string.action_wallpaper)));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        switch (key) {
            case "dark_theme":
            case "dark_theme_black":
            case "shade_view_switch":
            case "comfy_padding":
                recreate();
                break;
            case "hidden_apps":
                excludedAppList.clear();
                excludedAppList.addAll(prefs.getStringSet("hidden_apps", excludedAppList));
                loadApps(true);
                apps.setUpdateFilter(true);
                break;
            case "icon_hide_switch":
                icon_hide = prefs.getBoolean("icon_hide_switch", false);
                loadApps(true);
                apps.setUpdateFilter(true);
                break;
            case "list_order":
                list_order = prefs.getString("list_order", "alphabetical").equals("invertedAlphabetical");
                loadApps(true);
                apps.setUpdateFilter(true);
                break;
            case "refreshAppList":
                loadApps(true);
                apps.setUpdateFilter(true);
                editPrefs.putBoolean("refreshAppList", false).apply();
                break;
        }
    }

    // Don't do anything when back is pressed.
    // Fixes the issue of launcher going AWOL.
    @Override
    public void onBackPressed() {}

    @Override
    public void onResume() {
        super.onResume();
        loadPref();
        searchBar.setText(null);
        if (slidingHome.getPanelState() == SlidingUpPanelLayout.PanelState.DRAGGING) {
            slidingHome.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
        registerPackageReceiver();
        if (prefs.getBoolean("refreshAppList", false)) {
            loadApps(true);
            apps.setUpdateFilter(true);
            editPrefs.putBoolean("refreshAppList", false).apply();
        }
    }

    private void loadApps(Boolean shouldForceRefresh) {
        manager = getPackageManager();

        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> availableActivities = manager.queryIntentActivities(i, 0);

        if (!list_order) {
            Collections.sort(availableActivities, Collections
                    .reverseOrder(new ResolveInfo.DisplayNameComparator(manager)));
        } else {
            Collections.sort(availableActivities, new ResolveInfo.DisplayNameComparator(manager));
        }
        // Clear the list to make sure that we aren't just adding over an existing list.
        appList.clear();
        if (shouldForceRefresh) {
            list.getRecycledViewPool().clear();
            apps.notifyDataSetChanged();
        } else {
            apps.notifyItemRangeChanged(0, 0);
        }

        // Fetch and add every app into our list, but ignore those that are in the exclusion list.
        for (ResolveInfo ri : availableActivities) {
            String packageName = ri.activityInfo.packageName;
            if (!excludedAppList.contains(packageName)) {
                String appName = ri.loadLabel(manager).toString();
                Drawable icon = null;
                // Only show icons if user chooses so.
                if (!icon_hide) {
                    icon = ri.activityInfo.loadIcon(manager);
                }
                AppDetail app = new AppDetail(icon, appName, packageName);
                appList.add(app);
                apps.notifyItemInserted(appList.size() - 1);
            }
        }

        // Update our view cache size, now that we have got all apps on the list
        list.setItemViewCacheSize(appList.size() - 1);
    }

    private void addClickListener() {
        // Add short click/click listener to the app list.
        RecyclerClick.addTo(list).setOnItemClickListener(new RecyclerClick.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                launchApp(appList.get(position).getPackageName());
            }
        });

        // Add long click action to app list. Long click shows a menu to manage selected app.
        RecyclerClick.addTo(list).setOnItemLongClickListener(new RecyclerClick.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, final int position, View v) {
                // Parse package URI for use in uninstallation and package info call.
                final String packageName = appList.get(position).getPackageName();
                final Uri packageNameUri = Uri.parse("package:" + packageName);

                // Inflate the app menu.
                PopupMenu appMenu = new PopupMenu(MainActivity.this, v);
                appMenu.getMenuInflater().inflate(R.menu.menu_app, appMenu.getMenu());
                appMenu.show();

                //TODO: Why does this look so hackish.
                appMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
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
                                editPrefs.putStringSet("hidden_apps", excludedAppList).commit();
                                // Reload the app list!
                                appList.remove(packageName);
                                apps.notifyItemRemoved(position);
                                apps.setUpdateFilter(true);
                                break;
                        }
                        return true;
                    }
                });
                return false;
            }
        });

        slidingHome.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            @Override
            public void onPanelSlide(View view, float v) {
                // Do nothing.
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED
                        || newState == SlidingUpPanelLayout.PanelState.DRAGGING) {
                    // Empty out search bar text
                    searchBar.setText(null);

                    // Automatically show keyboard when the panel is called.
                    if (inputManager != null && keyboard_focus) {
                        inputManager.showSoftInput(searchBar, InputMethodManager.SHOW_IMPLICIT);
                        searchBar.requestFocus();
                    }
                    // Animate search container entering the view.
                    searchContainer.animate().alpha(1.0f).setDuration(100)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    searchContainer.setVisibility(View.VISIBLE);
                                }
                            });
                } else if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    // Hide keyboard if container is invisible.
                    if (inputManager != null && inputManager.isAcceptingText()) {
                        inputManager.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                    }

                    // Also animate the container when it's disappearing.
                    searchContainer.animate().alpha(0.0f).setDuration(200)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    searchContainer.setVisibility(View.GONE);
                                }
                            });
                }
            }
        });
    }

    // A method to launch an app based on package name.
    private void launchApp(String packageName) {
        Intent i = manager.getLaunchIntentForPackage(packageName);
        // Attempt to catch exceptions instead of crash landing directly to the floor.
        try {
            // Override app launch animation when needed.
            startActivity(i);
            switch (launch_anim) {
                case "pull_up":
                    overridePendingTransition(R.anim.pull_up, 0);
                    break;
                case "slide_in":
                    overridePendingTransition(R.anim.slide_in, 0);
                    break;
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(MainActivity.this, R.string.err_activity_not_found, Toast.LENGTH_LONG).show();
        } catch (NullPointerException e) {
            Toast.makeText(MainActivity.this, R.string.err_activity_null, Toast.LENGTH_LONG).show();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    public void registerPackageReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
            intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            intentFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
            intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            intentFilter.addDataScheme("package");
            registerReceiver(new PackageChangesReceiver(), intentFilter);
        }
    }

    // Load available preferences.
    //TODO: This is suboptimal. Maybe try coming up with a better hax?
    private void loadPref() {
        launch_anim = prefs.getString("launch_anim", "default");
        icon_hide = prefs.getBoolean("icon_hide_switch", false);
        list_order = prefs.getString("list_order", "alphabetical").equals("invertedAlphabetical");
        shade_view = prefs.getBoolean("shade_view_switch", false);
        keyboard_focus = prefs.getBoolean("keyboard_focus", false);
        comfy_padding = prefs.getBoolean("comfy_padding", false);
        dark_theme = prefs.getBoolean("dark_theme", false);
        dark_theme_black = prefs.getBoolean("dark_theme_black", false);
        web_search_enabled = prefs.getBoolean("web_search_enabled", true);
        String search_provider_set = prefs.getString("search_provider", "google");

        switch (search_provider_set) {
            case "google":
                search_provider = "https://www.google.com/search?q=";
                break;
            case "ddg":
                search_provider = "https://www.duckduckgo.com/?q=";
                break;
            case "searx":
                search_provider = "https://www.searx.me/?q=";
        }
    }
}
