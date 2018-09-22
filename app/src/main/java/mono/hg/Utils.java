package mono.hg;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import mono.hg.helpers.LauncherIconHelper;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.items.AppDetail;
import mono.hg.items.PinnedAppDetail;

public class Utils {

    /**
     * Fetch statusbar height from system's dimension.
     *
     * @param resource Resources object used to fetch the statusbar height.
     *
     * @return int Size of the statusbar. Returns the fallback value of 24dp if the
     *         associated dimen value cannot be found.
     */
    public static int getStatusBarHeight(Resources resource) {
        int idStatusBarHeight = resource.getIdentifier("status_bar_height", "dimen", "android");
        if (idStatusBarHeight > 0) {
            return resource.getDimensionPixelSize(idStatusBarHeight);
        } else {
            // Return fallback size if we can't get the value from the system.
            return resource.getDimensionPixelSize(R.dimen.status_bar_height_fallback);
        }
    }

    /**
     * Sends log using a predefined tag. This is used to better debug or to catch errors.
     * Logging should always use sendLog to coalesce logs into one single place.
     *
     * @param level Urgency level of the log to send. 3 is the ceiling and will
     *              send errors. Defaults to debug message when the level is invalid.
     *
     * @param message The message to send to logcat.
     */
    public static void sendLog(int level, String message) {
        String tag = "HgLogger";
        switch (level) {
            default:
            case 0:
                Log.d(tag, message);
                break;
            case 1:
                Log.v(tag, message);
                break;
            case 2:
                Log.w(tag, message);
                break;
            case 3:
                Log.e(tag, message);
                break;
        }
    }

    /**
     * Checks if an object is null; throws an IllegalArgumentException if it is.
     *
     * @param obj The object to check for.
     *
     * @param <T> The type of the referenced object.
     *
     * @return Object if not null.
     */
    public static <T> T requireNonNull(T obj) {
        if (obj == null)
            throw new IllegalArgumentException();
        return obj;
    }

    /**
     * Disable snackbar swipe behaviour.
     *
     * @param snackbar Snackbar whose behaviour is to be modified.
     */
    public static void disableSnackbarSwipe(final Snackbar snackbar) {
        snackbar.getView().getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                snackbar.getView().getViewTreeObserver().removeOnPreDrawListener(this);
                ((CoordinatorLayout.LayoutParams) snackbar.getView().getLayoutParams()).setBehavior(null);
                return true;
            }
        });
    }

    /**
     * Hides the software keyboard.
     *
     * @param activity The activity where the keyboard focus is being achieved.
     */
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && activity.getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * Opens a URL/link from a string object.
     *
     * @param context Context object for use with startActivity.
     *
     * @param link The link to be opened.
     */
    public static void openLink(Context context, String link) {
        Intent linkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        context.startActivity(linkIntent);
    }

    /**
     * Checks if a certain application is installed, regardless of their launch intent.
     *
     * @param packageManager PackageManager object to use for checking the requested
     *                       package's existence.
     *
     * @param packageName Application package name to check.
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
     *
     * @param packageName Application package name to check against.
     *
     * @return boolean True if the application is a system app, false if otherwise.
     */
    public static boolean isSystemApp(PackageManager packageManager, String packageName) {
        try {
            ApplicationInfo appFlags = packageManager.getApplicationInfo(packageName, 0);
            if ((appFlags.flags & ApplicationInfo.FLAG_SYSTEM) == 1)
                return true;
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
     *
     * @param packageName The package name to load and fetch.
     *
     * @param adapter Which adapter should we notify update to?
     *
     * @param list Which List object should be updated?
     *
     */
    public static void pinApp(PackageManager packageManager, String packageName,
                              FlexibleAdapter<PinnedAppDetail> adapter, List<PinnedAppDetail> list) {
        if (!adapter.contains(new PinnedAppDetail(null, packageName))) {
            try {
                Drawable icon;
                Drawable getIcon = null;
                if (!PreferenceHelper.getIconPackName().equals("default"))
                    getIcon = new LauncherIconHelper().getIconDrawable(packageManager, packageName);
                if (getIcon == null) {
                    icon = packageManager.getApplicationIcon(packageName);
                } else {
                    icon = getIcon;
                }
                PinnedAppDetail app = new PinnedAppDetail(icon, packageName);
                list.add(app);
                adapter.updateDataSet(list);
            } catch (PackageManager.NameNotFoundException e) {
                sendLog(3, e.toString());
            }
        }
    }

    /**
     * Loads an app to a list hosting the AppDetail object. The loaded app will have
     * a custom icon loaded if it's available at all, and the list associated with it
     * can have the order arranged.
     *
     * This is deprecated for now.
     *
     * @param packageManager PackageManager object used to fetch information regarding
     *                       the app that is being loaded.
     *
     * @param packageName The package name to load and fetch.
     *
     * @param adapter Which adapter should we notify update to?
     *
     * @param list Which List object should be updated?
     *
     * @param forFavourites Is this app used for the favourites panel?
     */
    @Deprecated
    public static void loadSingleApp(PackageManager packageManager, String packageName,
                                     RecyclerView.Adapter adapter, List<AppDetail> list, Boolean forFavourites) {
        ApplicationInfo applicationInfo;

        if (packageManager.getLaunchIntentForPackage(packageName) != null &&
                !list.contains(new AppDetail(null, null, packageName, false))) {
            try {
                applicationInfo = packageManager.getApplicationInfo(packageName, 0);
                String appName = packageManager.getApplicationLabel(applicationInfo).toString();
                Drawable icon = null;
                Drawable getIcon = null;
                if (PreferenceHelper.shouldHideIcon() || forFavourites) {
                    if (!PreferenceHelper.getIconPackName().equals("default"))
                        getIcon = new LauncherIconHelper().getIconDrawable(packageManager, packageName);
                    if (getIcon == null) {
                        icon = packageManager.getApplicationIcon(packageName);
                    } else {
                        icon = getIcon;
                    }
                }
                AppDetail app = new AppDetail(icon, appName, packageName, false);
                list.add(app);
                if (forFavourites) {
                    adapter.notifyDataSetChanged();
                } else {
                    adapter.notifyItemInserted(list.size());
                }
            } catch (PackageManager.NameNotFoundException e) {
                sendLog(3, e.toString());
            }

            if (!forFavourites) {
                if (PreferenceHelper.isListInverted()) {
                    Collections.sort(list, new Comparator<AppDetail>() {
                        @Override
                        public int compare(AppDetail nameL, AppDetail nameR) {
                            return nameR.getAppName().compareToIgnoreCase(nameL.getAppName());
                        }
                    });
                } else {
                    Collections.sort(list, Collections.reverseOrder(new Comparator<AppDetail>() {
                        @Override
                        public int compare(AppDetail nameL, AppDetail nameR) {
                            return nameR.getAppName().compareToIgnoreCase(nameL.getAppName());
                        }
                    }));
                }
            }
        }
    }
}
