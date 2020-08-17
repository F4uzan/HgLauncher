package mono.hg.utils

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
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import mono.hg.R
import mono.hg.adapters.AppAdapter
import mono.hg.helpers.LauncherIconHelper
import mono.hg.helpers.PreferenceHelper
import mono.hg.models.App
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
    fun isAppInstalled(packageManager: PackageManager, packageName: String): Boolean {
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
     * This function internally calls [isAppInstalled], and is a syntactic sugar of
     * said function.
     *
     * @param packageManager PackageManager object to use for checking the requested
     * package's existence.
     * @param componentName  The component name belonging to the application to be checked.
     *
     * @return True if application is installed.
     */
    fun doesComponentExist(packageManager: PackageManager, componentName: String): Boolean {
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
    fun isSystemApp(packageManager: PackageManager, componentName: String): Boolean {
        return try {
            packageManager.getApplicationInfo(getPackageName(componentName), 0).let {
                it.flags and ApplicationInfo.FLAG_SYSTEM == 1
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Utils.sendLog(LogLevel.ERROR, e.toString())
            false
        }
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
     * During pinning, the icon is re-retrieved through [LauncherIconHelper.getIcon],
     * as such, it is necessary that the icon cache has been built,
     * otherwise the pinned app will have no icon to show (but the view will still be drawn).
     *
     * @param activity      Current foreground activity.
     * @param user          User profile where the app originates. 0 if none.
     * @param componentName The package name to load and fetch.
     * @param adapter       Which adapter should we notify update to?
     * @param list          Which List object should be updated?
     */
    fun pinApp(
        activity: Activity, user: Long, componentName: String,
        adapter: AppAdapter, list: MutableList<App>
    ) {
        val icon = LauncherIconHelper.getIcon(activity, componentName, user, false)
        val app = icon?.let { App(it, componentName, user) }.apply {
            this?.userPackageName = if (user != UserUtils(activity).currentSerial) {
                appendUser(user, componentName)
            } else {
                componentName
            }
        }
        app?.let {
            list.add(it)
            adapter.updateDataSet(list, false)
        }
    }

    /**
     * Requests an uninstallation of a package.
     *
     * This function requires a proper Uri so as to launch the uninstallation prompt,
     * which contains a package name by the 'package://' prefix. Note that
     * if it is a component name, [getPackageName] should be invoked first.
     *
     * Currently, we are using the deprecated [Intent.ACTION_UNINSTALL_PACKAGE].
     *
     * @param activity      Current foreground activity.
     * @param packageName   The package name of the app to be uninstalled.
     */
    fun uninstallApp(activity: Activity, packageName: Uri) {
        activity.startActivity(Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageName))
    }

    /**
     * Opens the related Settings page of an app.
     *
     * This function requires a component name (and NOT a package name),
     * as [ComponentName.unflattenFromString] and [getPackageName] will be called
     * to create ComponentName and retrieve package name respectively. Using
     * a package name in place of the component name may cause an unhandled
     * exception.
     *
     * A different method is used to launch the settings page itself.
     * For [Build.VERSION_CODES.LOLLIPOP], this function makes use of
     * [LauncherApps.startAppDetailsActivity], which is unavailable
     * in older API level. As such, the traditional intent launch
     * with [Settings.ACTION_APPLICATION_DETAILS_SETTINGS] is used.
     *
     * @param activity          Current foreground activity.
     * @param componentName     The component name of the app.
     * @param user              The user that the app belongs to.
     */
    fun openAppDetails(activity: Activity, componentName: String, user: Long) {
        if (Utils.atLeastLollipop()) {
            val handle = UserUtils(activity).getUser(user)
            val launcher = activity.getSystemService(
                Context.LAUNCHER_APPS_SERVICE
            ) as LauncherApps

            with(ComponentName.unflattenFromString(componentName)) {
                launcher.startAppDetailsActivity(this, handle, null, null)
            }
        } else {
            with(Uri.fromParts("package", getPackageName(componentName), null)) {
                activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, this))
            }
        }
    }

    /**
     * Launches an application as a new task.
     *
     * This function make use of [LauncherApps.startMainActivity] for
     * system running [Build.VERSION_CODES.LOLLIPOP] and above.
     * For earlier version, [quickLaunch] is used.
     *
     * Before an app is launched, its launch animation is overridden
     * by the selected animation in [PreferenceHelper.launchAnim].
     * This behaviour will not occur if [PreferenceHelper.launchAnim]
     * returns "default" or an invalid value.
     *
     * @param context   Current foreground activity.
     * @param app       App object to launch.
     */
    fun launchApp(context: Context, app: App) {
        // Attempt to catch exceptions instead of crash landing directly to the floor.
        try {
            val activity = context as Activity

            if (Utils.atLeastLollipop()) {
                val handle = UserUtils(activity).getUser(app.user)
                val launcher = activity.getSystemService(
                    Context.LAUNCHER_APPS_SERVICE
                ) as LauncherApps
                with(ComponentName.unflattenFromString(app.packageName)) {
                    launcher.startMainActivity(this, handle, null, null)
                }
            } else {
                quickLaunch(activity, app.packageName)
            }
            when (PreferenceHelper.launchAnim) {
                "pull_up" -> activity.overridePendingTransition(R.anim.pull_up, 0)
                "slide_in" -> activity.overridePendingTransition(
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right
                )
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.err_activity_null, Toast.LENGTH_LONG).show()
            Utils.sendLog(
                LogLevel.ERROR,
                "Cannot start " + app.packageName + "; missing package?"
            )
        } catch (e: SecurityException) {
            Toast.makeText(context, R.string.err_activity_null, Toast.LENGTH_LONG).show()
            Utils.sendLog(LogLevel.ERROR, "Cannot start " + app.packageName + "; invalid user?")
        } catch (e: ClassCastException) {
            // Whatever happened.
            Utils.sendLog(
                LogLevel.ERROR,
                "Received ClassCastException when starting " + app.packageName
            )
        }
    }

    /**
     * Launches an activity based on its component name.
     *
     * @param activity      Current foreground activity.
     * @param componentName Component name of the app to be launched.
     */
    @Throws(ActivityNotFoundException::class)
    fun quickLaunch(activity: Activity, componentName: String) {
        // When receiving 'none', it's probably a gesture that hasn't been registered.
        if ("none" == componentName) {
            return
        }
        val component = ComponentName.unflattenFromString(componentName) ?: return

        // Forcibly end if we can't unflatten the string.
        Intent.makeMainActivity(component).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }.also {
            activity.startActivity(it)
        }
    }

    /**
     * Get simple package name from a flattened ComponentName.
     *
     * @param componentName The flattened ComponentName whose package name is to be returned.
     *
     * @return String The package name if not null.
     */
    fun getPackageName(componentName: String): String {
        return ComponentName.unflattenFromString(componentName)?.packageName ?: ""
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
        return try {
            manager.getApplicationInfo(getPackageName(componentName), PackageManager.GET_META_DATA)
                .loadLabel(manager)
                .toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Utils.sendLog(LogLevel.ERROR, "Unable to find label for $componentName")
            ""
        }
    }

    /**
     * Counts the number of installed package in the system. This function leaves out disabled packages.
     *
     * @param packageManager PackageManager to use for counting the list of installed packages.
     *
     * @return The number of installed packages, but without disabled packages.
     */
    fun countInstalledPackage(packageManager: PackageManager): Int {
        return packageManager.getInstalledApplications(0).filter { it.enabled }.count()
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
                packageManager
            )
        ) {
            PreferenceHelper.update("package_count", countInstalledPackage(packageManager))
            return true
        }
        return false
    }

    /**
     * Populates the internal app list. This method must be loaded asynchronously to avoid
     * performance degradation.
     *
     * This function internally calls [loadAppsWithUser] to correctly instantiate
     * user profiles and their respective apps, however if this is not feasible
     * (such as with older APIs without support for user profiles/multi-user),
     * [loadAppsLegacy] is called instead.
     *
     * @param activity      Current foreground activity.
     * @param hideHidden    Should hidden apps be hidden?
     * @param shouldSort    Should the list be sorted?
     *
     * @return List an App List containing the app list itself.
     */
    fun loadApps(activity: Activity, hideHidden: Boolean, shouldSort: Boolean): List<App> {
        return if (Utils.atLeastLollipop()) {
            loadAppsWithUser(activity, hideHidden, shouldSort)
        } else {
            loadAppsLegacy(activity, hideHidden, shouldSort)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun loadAppsWithUser(
        activity: Activity,
        hideHidden: Boolean,
        shouldSort: Boolean
    ): List<App> {
        val appsList: MutableList<App> = ArrayList()
        val userUtils = UserUtils(activity)
        val userManager = activity.getSystemService(Context.USER_SERVICE) as UserManager
        val launcher = activity.getSystemService(
            Context.LAUNCHER_APPS_SERVICE
        ) as LauncherApps
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
                val isHidden = (PreferenceHelper.exclusionList.contains(userPackageName))
                if (! componentName.contains(activity.packageName) && (! isHidden || ! hideHidden)) {
                    val appName = activityInfo.label.toString()
                    val app = App(appName, componentName, false, user)
                    app.hintName = PreferenceHelper.getLabel(userPackageName)
                    app.userPackageName = userPackageName
                    app.icon = LauncherIconHelper.getIcon(
                        activity,
                        componentName,
                        user,
                        PreferenceHelper.shouldHideIcon()
                    )
                    app.isAppHidden = isHidden
                    if (! appsList.contains(app)) {
                        appsList.add(app)
                    }
                }
            }
        }

        sortAppList(appsList, shouldSort)
        return appsList
    }

    private fun loadAppsLegacy(
        activity: Activity,
        hideHidden: Boolean,
        shouldSort: Boolean
    ): List<App> {
        val appsList: MutableList<App> = ArrayList()
        val manager = activity.packageManager
        val userUtils = UserUtils(activity)
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        manager.queryIntentActivities(intent, 0).forEach {
            val packageName = it.activityInfo.packageName
            val componentName = packageName + "/" + it.activityInfo.name
            val isHidden = PreferenceHelper.exclusionList.contains(componentName)
            if (! componentName.contains(activity.packageName) && (! isHidden || ! hideHidden)) {
                val appName = it.loadLabel(manager).toString()
                val app = App(appName, componentName, false, userUtils.currentSerial)
                app.hintName = PreferenceHelper.getLabel(componentName)
                app.userPackageName = componentName
                app.icon = LauncherIconHelper.getIcon(
                    activity, componentName,
                    userUtils.currentSerial, PreferenceHelper.shouldHideIcon()
                )
                app.isAppHidden = isHidden
                if (! appsList.contains(app)) {
                    appsList.add(app)
                }
            }
        }

        sortAppList(appsList, shouldSort)
        return appsList
    }

    /**
     * Sorts a List containing the App object.
     *
     * The sorting will invoke [Collections.reverseOrder]
     * if [PreferenceHelper.isListInverted] is set. This function
     * should preferably be left alone for other app list aside from
     * the ones shown to launch apps.
     *
     * @param list          The list to be sorted.
     * @param applyInverse  Should user-defined sorting be applied?
     */
    private fun sortAppList(list: MutableList<App>, applyInverse: Boolean) {
        list.sortWith(DisplayNameComparator(PreferenceHelper.isListInverted && applyInverse))
    }

    /**
     * Fetch and return shortcuts for a specific app.
     *
     * @param launcherApps  LauncherApps service from an activity.
     * @param componentName The component name to flatten to package name.
     *
     * @return List A list of shortcuts. Null if nonexistent.
     */
    @TargetApi(Build.VERSION_CODES.N_MR1)
    fun getShortcuts(launcherApps: LauncherApps?, componentName: String): List<ShortcutInfo>? {
        // Return nothing if we don't have permission to retrieve shortcuts.
        if (launcherApps == null || ! launcherApps.hasShortcutHostPermission()) {
            return ArrayList(0)
        }

        ShortcutQuery().apply {
            setQueryFlags(
                ShortcutQuery.FLAG_MATCH_DYNAMIC
                        or ShortcutQuery.FLAG_MATCH_MANIFEST
                        or ShortcutQuery.FLAG_MATCH_PINNED
            )
            setPackage(getPackageName(componentName))
        }.also {
            return launcherApps.getShortcuts(it, Process.myUserHandle())
        }
    }

    /**
     * Launches an app shortcut.
     *
     * This function assumes that the launcher has shortcut host permission beforehand.
     * If this assumption is not handled properly, then the launcher will likely crash.
     *
     * @param user          The user owning this [componentName]
     * @param launcherApps  LauncherApps service from an activity
     * @param componentName The component name of the app itself
     * @param id            The unique ID of the shortcut. Must not be null.
     */
    @TargetApi(Build.VERSION_CODES.N_MR1)
    fun launchShortcut(
        user: UserHandle,
        launcherApps: LauncherApps?,
        componentName: String,
        id: String
    ) {
        launcherApps?.startShortcut(getPackageName(componentName), id, null, null, user)
    }
}