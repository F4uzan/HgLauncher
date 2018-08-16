package f4.hubby;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.util.ArraySet;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import f4.hubby.helpers.RecyclerClick;

public class HiddenAppsActivity extends AppCompatActivity {
    ArrayList<AppDetail> appList = new ArrayList<>();
    Set<String> excludedAppList = new ArraySet<>();
    AppAdapter apps = new AppAdapter(appList);
    RecyclerView list;
    TextView emptyHint;
    SharedPreferences.Editor editPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Load appropriate theme before creating the activity.
        if (prefs.getBoolean("dark_theme", false) && prefs.getBoolean("dark_theme_black", true)) {
            setTheme(R.style.AppTheme_Dark);
        } else if (!prefs.getBoolean("dark_theme_black", true)) {
            setTheme(R.style.AppTheme_Gray);
        } else {
            setTheme(R.style.AppTheme);
        }

        setContentView(R.layout.activity_hidden_apps);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editPrefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);

        list = findViewById(R.id.ex_apps_list);
        emptyHint = findViewById(R.id.empty_hint);

        list.setAdapter(apps);
        list.setLayoutManager(mLayoutManager);
        list.setItemAnimator(new DefaultItemAnimator());

        // Get our app list.
        excludedAppList.addAll(prefs.getStringSet("hidden_apps", excludedAppList));
        loadHiddenApps();

        // Update our view cache size, now that we have got all apps on the list.
        list.setItemViewCacheSize(appList.size() - 1);
        addListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar_hidden_app, menu);
        MenuItem reset = menu.findItem(R.id.action_reset_hidden_apps);

        // Don't show an option to restore entries when there is nothing to restore.
        if (appList.size() == 0) {
            reset.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_reset_hidden_apps) {
            excludedAppList.clear();
            editPrefs.putStringSet("hidden_apps", excludedAppList).apply();
            // Recreate the toolbar menu to hide the 'restore all' button.
            invalidateOptionsMenu();
            // Reload the list.
            loadHiddenApps();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    private void loadHiddenApps() {
        PackageManager manager = getPackageManager();
        appList.clear();
        list.getRecycledViewPool().clear();
        apps.notifyItemRangeChanged(0, 0);

        for (String packageName : excludedAppList) {
            ApplicationInfo appInfo;
            Drawable icon = null;
            try {
                appInfo = manager.getApplicationInfo(packageName, 0);
                icon = manager.getApplicationIcon(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                appInfo = null;
            }
            String appName = manager.getApplicationLabel(appInfo).toString();
            AppDetail app = new AppDetail(icon, appName, packageName);
            appList.add(app);
            apps.notifyItemInserted(appList.size() - 1);
        }

        Collections.sort(appList, new Comparator<AppDetail>() {
            @Override
            public int compare(AppDetail one, AppDetail two) {
                return one.getAppName().compareToIgnoreCase(two.getAppName());
            }
        });

        if (appList.size() == 0) {
            emptyHint.setVisibility(View.VISIBLE);
            // Kill the list since there is nothing to show.
            list.setVisibility(View.GONE);
        }
    }

    private void addListeners() {
        RecyclerClick.addTo(list).setOnItemLongClickListener(new RecyclerClick.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, final int position, View v) {
                // Parse package URI for use in uninstallation and package info call.
                final String packageName = appList.get(position).getPackageName();
                final Uri packageNameUri = Uri.parse("package:" + packageName);

                // Inflate the app menu.
                PopupMenu appMenu = new PopupMenu(HiddenAppsActivity.this, v);
                appMenu.getMenuInflater().inflate(R.menu.menu_hidden_app, appMenu.getMenu());
                appMenu.show();

                // Set listener for the menu.
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
                            case R.id.action_show:
                                // Remove the app's package name from the exclusion list.
                                excludedAppList.remove(packageName);
                                editPrefs.putStringSet("hidden_apps", excludedAppList).apply();
                                // Reload the app list!
                                appList.remove(position);
                                if (appList.size() == 0) {
                                    emptyHint.setVisibility(View.VISIBLE);
                                }
                                apps.notifyItemRemoved(position);
                                break;
                        }
                        return true;
                    }
                });
                return false;
            }
        });
    }
}
