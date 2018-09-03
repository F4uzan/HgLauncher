package mono.hg.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import mono.hg.AppDetail;
import mono.hg.R;
import mono.hg.adapters.AppAdapter;
import mono.hg.helpers.RecyclerClick;

public class HiddenAppsFragment extends Fragment {
    ArrayList<AppDetail> appList = new ArrayList<>();
    Set<String> excludedAppList = new ArraySet<>();
    AppAdapter apps = new AppAdapter(appList);
    RecyclerView list;
    TextView emptyHint;
    SharedPreferences prefs;
    SharedPreferences.Editor editPrefs;
    PackageManager manager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_hidden_apps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        manager = getActivity().getPackageManager();

        editPrefs = prefs.edit();
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity(),
                LinearLayoutManager.VERTICAL, false);

        list = getActivity().findViewById(R.id.ex_apps_list);
        emptyHint = getActivity().findViewById(R.id.empty_hint);

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.menu_toolbar_hidden_app, menu);
        MenuItem reset = menu.findItem(R.id.action_reset_hidden_apps);

        // Don't show an option to restore entries when there is nothing to restore.
        if (appList.size() == 0) {
            reset.setVisible(false);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        } else if (id == R.id.action_reset_hidden_apps) {
            excludedAppList.clear();
            editPrefs.putStringSet("hidden_apps", excludedAppList).apply();
            if (prefs.getBoolean("dummy_restore", true)) {
                editPrefs.putBoolean("dummy_restore", false).apply();
            } else {
                editPrefs.putBoolean("dummy_restore", true).apply();
            }
            // Recreate the toolbar menu to hide the 'restore all' button.
            getActivity().invalidateOptionsMenu();
            // Reload the list.
            loadHiddenApps();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadHiddenApps() {
        appList.clear();
        list.getRecycledViewPool().clear();
        apps.notifyItemRangeChanged(0, 0);

        for (String packageName : excludedAppList) {
            ApplicationInfo appInfo;
            Drawable icon;
            try {
                appInfo = manager.getApplicationInfo(packageName, 0);
                icon = manager.getApplicationIcon(packageName);
                String appName = manager.getApplicationLabel(appInfo).toString();
                AppDetail app = new AppDetail(icon, appName, packageName);
                appList.add(app);
                apps.notifyItemInserted(appList.size() - 1);
            } catch (PackageManager.NameNotFoundException e) {
                // Don't do anything if package manager throws this.
            }
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
                PopupMenu appMenu = new PopupMenu(getActivity(), v);
                appMenu.getMenuInflater().inflate(R.menu.menu_hidden_app, appMenu.getMenu());

                // Remove uninstall menu if the app is a system app.
                try {
                    ApplicationInfo appFlags = manager.getApplicationInfo(packageName, 0);
                    if ((appFlags.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                        appMenu.getMenu().removeItem(R.id.action_uninstall);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("Hubby", e.toString());
                } finally {
                    // Show the menu.
                    appMenu.show();
                }

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
                                if (prefs.getBoolean("dummy_restore", true)) {
                                    editPrefs.putBoolean("dummy_restore", false).apply();
                                } else {
                                    editPrefs.putBoolean("dummy_restore", true).apply();
                                }
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
