package mono.hg.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import mono.hg.AppDetail;
import mono.hg.R;
import mono.hg.SettingsActivity;
import mono.hg.Utils;
import mono.hg.adapters.HiddenAppAdapter;

public class HiddenAppsFragment extends Fragment {
    private ArrayList<AppDetail> appList = new ArrayList<>();
    private HiddenAppAdapter apps;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editPrefs;
    private PackageManager manager;
    private HashSet<String> excludedAppList = new HashSet<>();
    private ListView list;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hidden_apps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        super.onCreate(savedInstanceState);

        // Make a toolbar when the preference library doesn't give us anything.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            ((SettingsActivity) getActivity()).setSupportActionBar(toolbar);
            toolbar.setVisibility(View.VISIBLE);
            toolbar.setTitle(R.string.pref_header_hidden_apps);
        } else {
            ActionBar actionBar = ((SettingsActivity) getActivity()).getSupportActionBar();
            if (actionBar != null)
                actionBar.setTitle(R.string.pref_header_hidden_apps);
        }

        setHasOptionsMenu(true);

        manager = getActivity().getPackageManager();
        editPrefs = prefs.edit();

        list = getActivity().findViewById(R.id.ex_apps_list);
        apps = new HiddenAppAdapter(appList, getActivity());

        list.setAdapter(apps);

        // Get our app list.
        excludedAppList.addAll(prefs.getStringSet("hidden_apps", excludedAppList));
        loadApps();

        addListeners();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (prefs.getBoolean("dummy_restore", false)) {
            editPrefs.remove("dummy_restore").apply();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        if (excludedAppList.size() > 0) {
            menu.add(0, 1, 100, getString(R.string.action_hidden_app_reset));
            menu.getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        } else if (id == 1) {
            excludedAppList.clear();
            editPrefs.putStringSet("hidden_apps", excludedAppList).apply();
            editPrefs.putBoolean("dummy_restore", true).apply();

            // Recreate the toolbar menu to hide the 'restore all' button.
            getActivity().invalidateOptionsMenu();

            // Reload the list.
            loadApps();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadApps() {
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> availableActivities = manager.queryIntentActivities(i, 0);

        Collections.sort(availableActivities, new ResolveInfo.DisplayNameComparator(manager));
        // Clear the list to make sure that we aren't just adding over an existing list.
        appList.clear();
        apps.notifyDataSetChanged();

        // Fetch and add every app into our list, but ignore those that are in the exclusion list.
        for (ResolveInfo ri : availableActivities) {
            String packageName = ri.activityInfo.packageName;
            Boolean isHidden = false;
            String appName = ri.loadLabel(manager).toString();
            Drawable icon = ri.activityInfo.loadIcon(manager);
            if (excludedAppList.contains(packageName))
                isHidden = true;
            AppDetail app = new AppDetail(icon, appName, packageName, isHidden);
            appList.add(app);
            apps.notifyDataSetChanged();
        }
    }

    private void toggleHiddenState(int position) {
        String packageName = appList.get(position).getPackageName();
        // Check if package is already in exclusion.
        if (excludedAppList.contains(packageName)) {
            excludedAppList.remove(packageName);
            editPrefs.putStringSet("hidden_apps", excludedAppList).apply();
        } else {
            excludedAppList.add(packageName);
            editPrefs.putStringSet("hidden_apps", excludedAppList).apply();
        }
        editPrefs.putBoolean("dummy_restore", true).apply();
        // Reload the app list!
        if (excludedAppList.contains(packageName)) {
            appList.get(position).setHidden(true);
        } else {
            appList.get(position).setHidden(false);
        }
        apps.notifyDataSetChanged();

        // Toggle the state of the 'restore all' button.
        getActivity().invalidateOptionsMenu();
    }

    private void addListeners() {
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                toggleHiddenState(position);
            }
        });

        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                // Parse package URI for use in uninstallation and package info call.
                final String packageName = appList.get(position).getPackageName();
                final Uri packageNameUri = Uri.parse("package:" + packageName);

                // Inflate the app menu.
                PopupMenu appMenu = new PopupMenu(getActivity(), view);
                appMenu.getMenuInflater().inflate(R.menu.menu_hidden_app, appMenu.getMenu());

                // Don't show hide action if the app is already hidden.
                if (appList.get(position).isHidden()) {
                    appMenu.getMenu().removeItem(R.id.action_hide);
                } else {
                    appMenu.getMenu().removeItem(R.id.action_show);
                }

                // Remove uninstall menu if the app is a system app.
                if (Utils.isSystemApp(getActivity().getPackageManager(), packageName)) {
                    appMenu.getMenu().removeItem(R.id.action_uninstall);
                }

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
                            case R.id.action_hide:
                            case R.id.action_show:
                                toggleHiddenState(position);
                                break;
                            default:
                                // Don't do anything.
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
