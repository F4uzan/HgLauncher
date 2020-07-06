package mono.hg.utils

import mono.hg.R
import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Process
import android.os.UserManager
import android.widget.Toast
import eu.davidea.flexibleadapter.FlexibleAdapter
import mono.hg.BuildConfig
import mono.hg.helpers.LauncherIconHelper
import mono.hg.helpers.PreferenceHelper
import mono.hg.models.App
import mono.hg.models.PinnedApp
import mono.hg.utils.Utils.LogLevel
import mono.hg.wrappers.DisplayNameComparator
import java.util.*

/**
 * Utils class that handles operations related to applications and the app list.
 */
object AppUtils {
    /**
     * Checks if a certain application is installed, regardless of their launch intent.
     *
     * @param packageManager PackageManager object to use for checking the requested
     * package's existence.
     * @param packageName    Application package name to check.
     *
     * @return boolean True or false depending on the existence of the package.
     */
    fun isAppInstalled(packageManager: PackageManager, packageName: String?): Boolean {
        return try {
            // Get application info while handling exception spawning from it.
            packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            // No, it's not installed.
            false
        }
    }

    /**
     * Checks if a certain application is installed based off of its flattened component name.
     *
     * @param packageManager PackageManager object to use for checking the requested
     * package's existence.
     * @param componentName  The component name belonging to the application to be checked.
     *
     * @return True if application is installed.
     */
    fun doesComponentExist(packageManager: PackageManager, componentName: String?): Boolean {
        return isAppInstalled(packageManager, getPackageName(componentName))
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
    fun isSystemApp(packageManager: PackageManager, componentName: String?): Boolean {
        try {
            val appFlags = packageManager.getApplicationInfo(
                    getPackageName(componentName), 0)
            if (appFlags.flags and ApplicationInfo.FLAG_SYSTEM == 1) {
                return true
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Utils.sendLog(LogLevel.ERROR, e.toString())
            return false
        }
        return false
    }

    /**
     * Appends user serial to a component name.
     *
     * @param user          The user serial to prefix the component name.
     * @param componentName The component name itself.
     *
     * @return String with a hyphen-appended user serial.
     */
    fun appendUser(user: Long, componentName: String): String {
        return "$user-$componentName"
    }

    /**
     * Pins an app to the favourites panel.
     *
     * @param activity      Current foreground activity.
     * @param user          User profile where the app originates.
     * @param componentName The package name to load and fetch.
     * @param adapter       Which adapter should we notify update to?
     * @param list          Which List object should be updated?
     */
    fun pinApp(activity: Activity, user: Long, componentName: String,
               adapter: FlexibleAdapter<PinnedApp?>, list: MutableList<PinnedApp?>) {
        val icon = LauncherIconHelper.getIcon(activity, componentName, user, false)
        val app = PinnedApp(icon, componentName, user)
        list.add(app)
        adapter.updateDataSet(list, false)
    }

    /**
     * Launches an app as a new task.
     *
     * @param activity Current foreground activity.
     * @param app      App object to launch.
     */
    fun launchApp(activity: Activity, app: App) {
        // Attempt to catch exceptions instead of crash landing directly to the floor.
        try {
            if (Utils.atLeastLollipop()) {
                val userUtils = UserUtils(activity)
                val launcher = activity.getSystemService(
                        Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val componentName = ComponentName.unflattenFromString(app.packageName)
                launcher.startMainActivity(componentName, userUtils.getUser(app.user), null, null)
            } else {
                quickLaunch(activity, app.packageName)
            }
            when (PreferenceHelper.launchAnim) {
                "pull_up" -> activity.overridePendingTransition(R.anim.pull_up, 0)
                "slide_in" -> activity.overridePendingTransition(android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right)
                "default" -> {
                }
                else -> {
                }
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.err_activity_null, Toast.LENGTH_LONG).show()
            Utils.sendLog(LogLevel.ERROR,
                    "Cannot start " + app.packageName + "; missing package?")
        } catch (e: SecurityException) {
            Toast.makeText(activity, R.string.err_activity_null, Toast.LENGTH_LONG).show()
            Utils.sendLog(LogLevel.ERROR, "Cannot start " + app.packageName + "; invalid user?")
        }
    }

    /**
     * Launches an activity based on its component name.
     *
     * @param activity      Current foreground activity.
     * @param componentName Component name of the app to be launched.
     */
    @Throws(ActivityNotFoundException::class)
    fun quickLaunch(activity: Activity, componentName: String?) {
        // When receiving 'none', it's probably a gesture that hasn't been registered.
        if ("none" == componentName) {
            return
        }
        val component = ComponentName.unflattenFromString(componentName!!) ?: return

        // Forcibly end if we can't unflatten the string.
        val intent = Intent.makeMainActivity(component)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        activity.startActivity(intent)
    }

    /**
     * Get simple package name from a flattened ComponentName.
     *
     * @param componentName The flattened ComponentName whose package name is to be returned.
     *
     * @return String The package name if not null.
     */
    fun getPackageName(componentName: String?): String {
        val unflattened = ComponentName.unflattenFromString(componentName!!)
        return unflattened?.packageName ?: ""
    }

    /**
     * Retrieve a component name label.
     *
     * @param manager       PackageManager used to retrieve the component label.
     * @param componentName The component name whose name is to be retrieved.
     *
     * @return The label of the component name if no exception is thrown.
     */
    fun getPackageLabel(manager: PackageManager, componentName: String): String? {
        var label = ""
        try {
            label = manager.getApplicationLabel(
                    manager.getApplicationInfo(getPackageName(componentName),
                            PackageManager.GET_META_DATA)) as String
        } catch (e: PackageManager.NameNotFoundException) {
            Utils.sendLog(LogLevel.ERROR, "Unable to find label for $componentName")
        }
        return label
    }

    /**
     * Counts the number of installed package in the system. This function leaves out disabled packages.
     *
     * @param packageManager PackageManager to use for counting the list of installed packages.
     *
     * @return The number of installed packages, but without disabled packages.
     */
    fun countInstalledPackage(packageManager: PackageManager): Int {
        var count = 0
        packageManager.getInstalledApplications(0).forEach {
            if (it.enabled) {
                count++
            }
        }
        return count
    }

    /**
     * Checks if there has been a new package installed into the device.
     *
     * @param packageManager PackageManager used to count the installed packages, which are then
     * compared to the internal count saved by the launcher during its onCreate.
     *
     * @return boolean True if there is a change in number.
     */
    fun hasNewPackage(packageManager: PackageManager): Boolean {
        if (PreferenceHelper.preference.getInt("package_count", 0) != countInstalledPackage(
                        packageManager)) {
            PreferenceHelper.update("package_count", countInstalledPackage(packageManager))
            return true
        }
        return false
    }

    /**
     * Populates the internal app list. This method must be loaded asynchronously to avoid
     * performance degradation.
     *
     * @param activity Current foreground activity.
     *
     * @return List an App List containing the app list itself.
     */
    fun loadApps(activity: Activity, hideHidden: Boolean): List<App> {
        val appsList: MutableList<App> = ArrayList()
        val manager = activity.packageManager
        val userUtils = UserUtils(activity)
        if (Utils.atLeastLollipop()) {
            val userManager = activity.getSystemService(Context.USER_SERVICE) as UserManager
            val launcher = activity.getSystemService(
                    Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            userManager.userProfiles.forEach { profile ->
                launcher.getActivityList(null, profile).forEach { activityInfo ->
                    val componentName = activityInfo.componentName.flattenToString()
                    val userPackageName: String
                    val user = userUtils.getSerial(profile)
                    userPackageName = if (user != userUtils.currentSerial) {
                        appendUser(user, componentName)
                    } else {
                        componentName
                    }
                    val isHidden = (PreferenceHelper.exclusionList.contains(componentName)
                            || componentName.contains(BuildConfig.APPLICATION_ID))
                    if (!hideHidden || !isHidden) {
                        val appName = activityInfo.label.toString()
                        val app = App(appName, componentName, false, user)
                        app.hintName = PreferenceHelper.getLabel(userPackageName)
                        app.userPackageName = userPackageName
                        app.icon = LauncherIconHelper.getIcon(activity, componentName, user, PreferenceHelper.shouldHideIcon())
                        app.isAppHidden = isHidden
                        if (!appsList.contains(app)) {
                            appsList.add(app)
                        }
                    }
                }
            }
        } else {
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            manager.queryIntentActivities(intent, 0).forEach {
                val packageName = it.activityInfo.packageName
                val componentName = packageName + "/" + it.activityInfo.name
                val isHidden = (PreferenceHelper.exclusionList.contains(componentName)
                        || componentName.contains(BuildConfig.APPLICATION_ID))
                if (!hideHidden || !isHidden) {
                    val appName = it.loadLabel(manager).toString()
                    val app = App(appName, componentName, false, userUtils.currentSerial)
                    app.hintName = PreferenceHelper.getLabel(componentName)
                    app.userPackageName = componentName
                    app.icon = LauncherIconHelper.getIcon(activity, componentName,
                            userUtils.currentSerial, PreferenceHelper.shouldHideIcon())
                    app.isAppHidden = isHidden
                    if (!appsList.contains(app)) {
                        appsList.add(app)
                    }
                }
            }
        }
        sortAppList(appsList)
        return appsList
    }

    /**
     * Sorts a List containing the App object.
     *
     * @param list The list to be sorted.
     */
    private fun sortAppList(list: List<App>) {
        if (PreferenceHelper.isListInverted) {
            Collections.sort(list, Collections.reverseOrder(DisplayNameComparator()))
        } else {
            Collections.sort(list, DisplayNameComparator())
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
    fun getShortcuts(launcherApps: LauncherApps?, componentName: String?): List<ShortcutInfo>? {
        // Return nothing if we don't have permission to retrieve shortcuts.
        if (launcherApps == null || !launcherApps.hasShortcutHostPermission()) {
            return ArrayList(0)
        }
        val shortcutQuery = ShortcutQuery()
        shortcutQuery.setQueryFlags(ShortcutQuery.FLAG_MATCH_DYNAMIC
                or ShortcutQuery.FLAG_MATCH_MANIFEST
                or ShortcutQuery.FLAG_MATCH_PINNED)
        shortcutQuery.setPackage(getPackageName(componentName))
        return launcherApps.getShortcuts(shortcutQuery, Process.myUserHandle())
    }
}