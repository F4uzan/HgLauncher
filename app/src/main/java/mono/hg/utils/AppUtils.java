package mono.hg.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;
import android.os.UserManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import mono.hg.BuildConfig;
import mono.hg.R;
import mono.hg.helpers.LauncherIconHelper;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.models.App;
import mono.hg.models.PinnedApp;
import mono.hg.wrappers.DisplayNameComparator;

public class AppUtils {
    /**
     * Checks if a certain application is installed, regardless of their launch intent.
     *
     * @param packageManager PackageManager object to use for checking the requested
     *                       package's existence.
     * @param packageName    Application package name to check.
     *
     * @return boolean True or false depending on the existence of the package.
     */
    public static boolean isAppInstalled(PackageManager packageManager, String packageName) {
        try {
            // Get application info while handling exception spawning from it.
            packageManager.getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            // No, it's not installed.
            return false;
        }
    }

    /**
     * Checks if a certain application is installed based off of its flattened component name.
     *
     * @param packageManager PackageManager object to use for checking the requested
     *                       package's existence.
     * @param componentName  The component name belonging to the application to be checked.
     *
     * @return True if application is installed.
     */
    public static boolean doesComponentExist(PackageManager packageManager, String componentName) {
        return isAppInstalled(packageManager, getPackageName(componentName));
    }

    /**
     * Checks with its package name, if an application is a system app, or is the app
     * is installed as a system app.
     *
     * @param packageManager PackageManager object used to receive application info.
     * @param componentName  Application package name to check against.
     *
     * @return boolean True if the application is a system app, false if otherwise.
     */
    public static boolean isSystemApp(PackageManager packageManager, String componentName) {
        try {
            ApplicationInfo appFlags = packageManager.getApplicationInfo(
                    getPackageName(componentName), 0);
            if ((appFlags.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Utils.sendLog(Utils.LogLevel.ERROR, e.toString());
            return false;
        }
        return false;
    }

    /**
     * Pins an app to the favourites panel.
     *
     * @param packageManager PackageManager object used to fetch information regarding
     *                       the app that is being loaded.
     * @param componentName  The package name to load and fetch.
     * @param adapter        Which adapter should we notify update to?
     * @param list           Which List object should be updated?
     */
    public static void pinApp(PackageManager packageManager, String componentName,
            FlexibleAdapter<PinnedApp> adapter, List<PinnedApp> list) {
        if (!adapter.contains(new PinnedApp(componentName))) {
            ComponentName iconComponent = ComponentName.unflattenFromString(componentName);

            try {
                Drawable icon;
                Drawable getIcon = null;

                if (!PreferenceHelper.getIconPackName().equals("default")) {
                    getIcon = LauncherIconHelper.getIconDrawable(packageManager,
                            componentName);
                }
                if (getIcon == null) {
                    icon = packageManager.getActivityIcon(iconComponent);
                } else {
                    icon = getIcon;
                }

                PinnedApp app = new PinnedApp(icon, componentName);
                list.add(app);
                adapter.updateDataSet(list, false);
            } catch (PackageManager.NameNotFoundException e) {
                Utils.sendLog(Utils.LogLevel.ERROR,
                        "Unable to pin " + componentName + ";missing package?");
            }
        }
    }

    /**
     * Launches an app as a new task.
     *
     * @param componentName The component name of the app.
     */
    public static void launchApp(Activity activity, String componentName) {
        // When receiving 'none', it's probably a gesture that hasn't been registered.
        if ("none".equals(componentName)) {
            return;
        }

        ComponentName component = ComponentName.unflattenFromString(componentName);

        // Forcibly end if we can't unflatten the string.
        if (component == null) {
            return;
        }

        Intent intent = Intent.makeMainActivity(component);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        // Attempt to catch exceptions instead of crash landing directly to the floor.
        try {
            activity.startActivity(intent);

            // Override app launch animation when needed.
            switch (PreferenceHelper.getLaunchAnim()) {
                case "pull_up":
                    activity.overridePendingTransition(R.anim.pull_up, 0);
                    break;
                case "slide_in":
                    activity.overridePendingTransition(android.R.anim.slide_in_left,
                            android.R.anim.slide_out_right);
                    break;
                default:
                case "default":
                    // Don't override when we have the default value.
                    break;
            }
        } catch (ActivityNotFoundException | NullPointerException e) {
            Toast.makeText(activity, R.string.err_activity_null, Toast.LENGTH_LONG).show();
            Utils.sendLog(Utils.LogLevel.ERROR,
                    "Cannot start " + componentName + "; missing package?");
        }
    }

    /**
     * Get simple package name from a flattened ComponentName.
     *
     * @param componentName The flattened ComponentName whose package name is to be returned.
     *
     * @return String The package name if not null.
     */
    public static String getPackageName(String componentName) {
        ComponentName unflattened = ComponentName.unflattenFromString(componentName);
        if (unflattened != null) {
            return unflattened.getPackageName();
        } else {
            return "";
        }
    }

    /**
     * Retrieve a component name label.
     *
     * @param manager       PackageManager used to retrieve the component label.
     * @param componentName The component name whose name is to be retrieved.
     *
     * @return The label of the component name if no exception is thrown.
     */
    public static String getPackageLabel(PackageManager manager, String componentName) {
        String label = "";
        try {
            label = (String) manager.getApplicationLabel(
                    manager.getApplicationInfo(getPackageName(componentName),
                            PackageManager.GET_META_DATA));
        } catch (PackageManager.NameNotFoundException e) {
            Utils.sendLog(Utils.LogLevel.ERROR, "Unable to find label for " + componentName);
        }
        return label;
    }

    /**
     * Counts the number of installed package in the system. This function leaves out disabled packages.
     *
     * @param packageManager PackageManager to use for counting the list of installed packages.
     *
     * @return The number of installed packages, but without disabled packages.
     */
    public static int countInstalledPackage(PackageManager packageManager) {
        int count = 0;
        for (ApplicationInfo applicationInfo : packageManager.getInstalledApplications(0)) {
            if (applicationInfo.enabled) {
                count++;
            }
        }
        return count;
    }

    /**
     * Checks if there has been a new package installed into the device.
     *
     * @param packageManager PackageManager used to count the installed packages, which are then
     *                       compared to the internal count saved by the launcher during its onCreate.
     *
     * @return boolean True if there is a change in number.
     */
    public static boolean hasNewPackage(PackageManager packageManager) {
        if (PreferenceHelper.getPreference().getInt("package_count", 0) != countInstalledPackage(
                packageManager)) {
            PreferenceHelper.update("package_count", countInstalledPackage(packageManager));
            return true;
        }
        return false;
    }

    /**
     * Populates the internal app list. This method must be loaded asynchronously to avoid
     * performance degradation.
     *
     * @param activity Current foreground activity.
     *
     * @return List an App List containing the app list itself.
     */
    public static List<App> loadApps(Activity activity) {
        List<App> appsList = new ArrayList<>();
        PackageManager manager = activity.getPackageManager();

        if (Utils.atLeastLollipop()) {
            UserManager userManager = (UserManager) activity.getSystemService(Context.USER_SERVICE);
            LauncherApps launcher = (LauncherApps) activity.getSystemService(
                    Context.LAUNCHER_APPS_SERVICE);

            for (android.os.UserHandle profile : Utils.requireNonNull(userManager)
                                                      .getUserProfiles()) {
                for (LauncherActivityInfo activityInfo : Utils.requireNonNull(launcher)
                                                              .getActivityList(null, profile)) {
                    String componentName = activityInfo.getComponentName().flattenToString();

                    if (!PreferenceHelper.getExclusionList().contains(componentName) && !componentName.equals(
                            BuildConfig.APPLICATION_ID)) {
                        String appName = activityInfo.getLabel().toString();
                        App app = new App(appName, componentName, false);

                        app.setHintName(PreferenceHelper.getLabel(componentName));
                        app.setIcon(LauncherIconHelper.getIcon(manager, componentName));
                        appsList.add(app);
                    }
                }
            }
        } else {
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            for (ResolveInfo ri : manager.queryIntentActivities(intent, 0)) {
                String packageName = ri.activityInfo.packageName;
                String componentName = packageName + "/" + ri.activityInfo.name;
                if (!PreferenceHelper.getExclusionList().contains(componentName) && !packageName.equals(
                        BuildConfig.APPLICATION_ID)) {
                    String appName = ri.loadLabel(manager).toString();
                    App app = new App(appName, componentName, false);

                    app.setHintName(PreferenceHelper.getLabel(componentName));
                    app.setIcon(LauncherIconHelper.getIcon(manager, componentName));
                    appsList.add(app);
                }
            }
        }

        sortAppList(appsList);
        return appsList;
    }

    /**
     * Sorts a List containing the App object.
     *
     * @param list The list to be sorted.
     */
    private static void sortAppList(List<App> list) {
        if (PreferenceHelper.isListInverted()) {
            Collections.sort(list, Collections.reverseOrder(new DisplayNameComparator()));
        } else {
            Collections.sort(list, new DisplayNameComparator());
        }
    }

    /**
     * Fetch and return shortcuts for a specific app.
     *
     * @param launcherApps  LauncherApps service from an activity.
     * @param componentName The component name to flatten to package name.
     *
     * @return A list of shortcut. Null if nonexistent.
     */
    @TargetApi(Build.VERSION_CODES.N_MR1)
    public static List<ShortcutInfo> getShortcuts(LauncherApps launcherApps, String componentName) {
        // Return nothing if we don't have permission to retrieve shortcuts.
        if (launcherApps == null || !launcherApps.hasShortcutHostPermission()) {
            return new ArrayList<>(0);
        }

        LauncherApps.ShortcutQuery shortcutQuery = new LauncherApps.ShortcutQuery();
        shortcutQuery.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                | LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
        shortcutQuery.setPackage(getPackageName(componentName));

        return launcherApps.getShortcuts(shortcutQuery, Process.myUserHandle());
    }
}
