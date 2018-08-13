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
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.Set;

import f4.hubby.helpers.RecyclerClick;

public class HiddenAppsActivity extends AppCompatActivity {
    ArrayList<AppDetail> appList = new ArrayList<>();
    Set<String> excludedAppList = new ArraySet<>();
    AppAdapter apps = new AppAdapter(appList);
    RecyclerView list;
    SharedPreferences prefs;
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
    public void onBackPressed() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadHiddenApps() {
        PackageManager manager = getPackageManager();
        appList.clear();
        apps.notifyDataSetChanged();

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
    }

    private void addListeners() {
        RecyclerClick.addTo(list).setOnItemLongClickListener(new RecyclerClick.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, int position, View v) {
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
                                // Add the app's package name to the exclusion list.
                                excludedAppList.remove(packageName);
                                editPrefs.putStringSet("hidden_apps", excludedAppList).apply();
                                // Reload the app list!
                                loadHiddenApps();
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
