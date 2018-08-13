package f4.hubby;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import f4.hubby.helpers.RecyclerClick;

public class SettingsActivity extends AppCompatPreferenceActivity {

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary is updated to reflect the value.
     * The summary is also immediately updated upon calling this method.
     * The exact display format is dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Load appropriate theme before creating the activity.
        if (prefs.getBoolean("dark_theme", false) && prefs.getBoolean("dark_theme_black", true)) {
            setTheme(R.style.AppTheme_Dark);
        } else if (!prefs.getBoolean("dark_theme_black", true)) {
            setTheme(R.style.AppTheme_Gray);
        } else {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    // Set up action bar
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Detect if we're on an extra large tablet and use multipanel if it is true
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    // Load headers for multipanel
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * Stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || CustomizePreferenceFragment.class.getName().equals(fragmentName)
                || AboutPreferenceFragment.class.getName().equals(fragmentName)
                || HiddenAppFragment.class.getName().equals(fragmentName);
    }

    /**
     * It is used when the activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class CustomizePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_customization);
            setHasOptionsMenu(true);

            // Bind the summaries of references to their values.
            // When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            //bindPreferenceSummaryToValue(findPreference("title_text"));

            // Preference anim = findPreference("anim_switch");
            // Preference icon = findPreference("icon_hide_switch");
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AboutPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_about);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class HiddenAppFragment extends PreferenceFragment {
        ArrayList<AppDetail> appList = new ArrayList<>();
        Set<String> excludedAppList = new ArraySet<>();
        AppAdapter apps = new AppAdapter(appList);
        RecyclerView list;
        SharedPreferences prefs;
        SharedPreferences.Editor editPrefs;

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_hidden_apps, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            editPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity(),
                    LinearLayoutManager.VERTICAL, false);

            list = getActivity().findViewById(R.id.ex_apps_list);

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
        public void onDetach() {
            if (prefs.getBoolean("recreateAppFragment", false)) {
                editPrefs.putBoolean("recreateAppFragment", false).apply();
                getActivity().recreate();
            }
            super.onDetach();
        }

        @Override
        public void onResume() {
            editPrefs.putBoolean("recreateAppFragment", true).apply();
            super.onResume();
        }

        private void loadHiddenApps() {
            PackageManager manager = getActivity().getPackageManager();
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
                    PopupMenu appMenu = new PopupMenu(getActivity(), v);
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
}
