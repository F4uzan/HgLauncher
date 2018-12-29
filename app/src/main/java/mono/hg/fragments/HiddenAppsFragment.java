package mono.hg.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import mono.hg.R;
import mono.hg.adapters.HiddenAppAdapter;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.models.AppDetail;

public class HiddenAppsFragment extends Fragment {
    private ArrayList<AppDetail> appList = new ArrayList<>();
    private HiddenAppAdapter hiddenAppAdapter;
    private PackageManager manager;
    private HashSet<String> excludedAppList = new HashSet<>();
    private ListView appsListView;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hidden_apps, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * TODO: We probably won't need all this, but rewrite it just in case.
        // Make a toolbar when the preference library doesn't give us anything.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            ((SettingsActivity) getActivity()).setSupportActionBar(toolbar);
            toolbar.setVisibility(View.VISIBLE);
            toolbar.setTitle(R.string.pref_header_hidden_apps);
        } else {
            ActionBar actionBar = ((SettingsActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(R.string.pref_header_hidden_apps);
            }
        } */

        setHasOptionsMenu(true);

        manager = getActivity().getPackageManager();

        appsListView = getActivity().findViewById(R.id.hidden_apps_list);
        hiddenAppAdapter = new HiddenAppAdapter(appList, getActivity());

        appsListView.setAdapter(hiddenAppAdapter);

        // Get our app list.
        excludedAppList.addAll(
                PreferenceHelper.getPreference().getStringSet("hidden_apps", excludedAppList));
        loadApps();

        addListeners();
    }

    @Override public void onDetach() {
        super.onDetach();
        if (PreferenceHelper.getPreference().getBoolean("dummy_restore", false)) {
            PreferenceHelper.getEditor().remove("dummy_restore").apply();
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        menu.add(0, 1, 100, getString(R.string.action_hidden_app_reset));
        menu.getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.getItem(0).setVisible(!excludedAppList.isEmpty());
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
            case 1:
                excludedAppList.clear();
                PreferenceHelper.getEditor()
                                .putStringSet("hidden_apps", new HashSet<String>())
                                .apply();
                PreferenceHelper.getEditor().putBoolean("dummy_restore", true).apply();

                // Recreate the toolbar menu to hide the 'restore all' button.
                getActivity().invalidateOptionsMenu();

                // Reload the list.
                loadApps();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadApps() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> availableActivities = manager.queryIntentActivities(intent, 0);

        Collections.sort(availableActivities, new ResolveInfo.DisplayNameComparator(manager));

        // Clear the list to make sure that we aren't just adding over an existing list.
        appList.clear();
        hiddenAppAdapter.notifyDataSetInvalidated();

        // Fetch and add every app into our list, but ignore those that are in the exclusion list.
        for (ResolveInfo ri : availableActivities) {
            String packageName = ri.activityInfo.packageName + "/" + ri.activityInfo.name;
            if (!packageName.equals(getActivity().getPackageName())) {
                String appName = ri.loadLabel(manager).toString();
                Drawable icon = ri.activityInfo.loadIcon(manager);
                boolean isHidden = excludedAppList.contains(packageName);
                AppDetail app = new AppDetail(icon, appName, packageName, isHidden);
                appList.add(app);
            }
        }

        hiddenAppAdapter.notifyDataSetChanged();
    }

    private void toggleHiddenState(int position) {
        String packageName = appList.get(position).getPackageName();

        // Check if package is already in exclusion.
        if (excludedAppList.contains(packageName)) {
            excludedAppList.remove(packageName);
            PreferenceHelper.getEditor().putStringSet("hidden_apps", excludedAppList).apply();
        } else {
            excludedAppList.add(packageName);
            PreferenceHelper.getEditor().putStringSet("hidden_apps", excludedAppList).apply();
        }
        PreferenceHelper.getEditor().putBoolean("dummy_restore", true).apply();
        appList.get(position).setAppHidden(excludedAppList.contains(packageName));

        // Reload the app list!
        hiddenAppAdapter.notifyDataSetChanged();

        // Toggle the state of the 'restore all' button.
        getActivity().invalidateOptionsMenu();
    }

    private void addListeners() {
        appsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                toggleHiddenState(position);
            }
        });
    }
}
