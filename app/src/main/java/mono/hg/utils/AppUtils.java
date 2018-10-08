package mono.hg.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import mono.hg.R;
import mono.hg.helpers.LauncherIconHelper;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.models.PinnedAppDetail;

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
     * Checks with its package name, if an application is a system app, or is the app
     * is installed as a system app.
     *
     * @param packageManager PackageManager object used to receive application info.
     * @param packageName    Application package name to check against.
     *
     * @return boolean True if the application is a system app, false if otherwise.
     */
    public static boolean isSystemApp(PackageManager packageManager, String packageName) {
        try {
            ApplicationInfo appFlags = packageManager.getApplicationInfo(packageName, 0);
            if ((appFlags.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Utils.sendLog(3, e.toString());
            return false;
        }
        return false;
    }

    /**
     * Pins an app to the favourites panel.
     *
     * @param packageManager PackageManager object used to fetch information regarding
     *                       the app that is being loaded.
     * @param packageName    The package name to load and fetch.
     * @param adapter        Which adapter should we notify update to?
     * @param list           Which List object should be updated?
     */
    public static void pinApp(PackageManager packageManager, String packageName,
            FlexibleAdapter<PinnedAppDetail> adapter, List<PinnedAppDetail> list) {
        if (!adapter.contains(new PinnedAppDetail(null, packageName))) {
            try {
                Drawable icon;
                Drawable getIcon = null;
                String componentPackage = getPackageName(packageName);

                if (!PreferenceHelper.getIconPackName().equals("default")) {
                    getIcon = new LauncherIconHelper().getIconDrawable(packageManager, packageName);
                }
                if (getIcon == null) {
                    icon = packageManager.getApplicationIcon(componentPackage);
                } else {
                    icon = getIcon;
                }

                PinnedAppDetail app = new PinnedAppDetail(icon, packageName);
                list.add(app);
                adapter.updateDataSet(list, false);
            } catch (PackageManager.NameNotFoundException e) {
                Utils.sendLog(3, e.toString());
            }
        }
    }


    /**
     * Launches an app as a new task.
     *
     * @param packageName The package name of the app.
     */
    public static void launchApp(Activity activity, String packageName) {
        ComponentName componentName = ComponentName.unflattenFromString(packageName);

        // Attempt to catch exceptions instead of crash landing directly to the floor.
        try {
            activity.startActivity(Intent.makeMainActivity(componentName));

            // Override app launch animation when needed.
            switch (PreferenceHelper.getLaunchAnim()) {
                case "pull_up":
                    activity.overridePendingTransition(R.anim.pull_up, 0);
                    break;
                case "slide_in":
                    activity.overridePendingTransition(R.anim.slide_in, 0);
                    break;
                default:
                case "default":
                    // Don't override when we have the default value.
                    break;
            }
        } catch (ActivityNotFoundException | NullPointerException e) {
            Toast.makeText(activity, R.string.err_activity_null, Toast.LENGTH_LONG).show();
            Utils.sendLog(3, "Cannot start " + packageName + "; missing package?");
        }
    }

    /**
     * Get simple package name from a flattened ComponentName.
     *
     * @param componentName The flattened ComponentName whose package name is to be returned.
     *
     * @return String the package name if not null.
     */
    public static String getPackageName(String componentName) {
        return Utils.requireNonNull(ComponentName.unflattenFromString(componentName))
                    .getPackageName();
    }
}
