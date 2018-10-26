package mono.hg.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.Log;

import mono.hg.receivers.PackageChangesReceiver;

public class Utils {

    /**
     * Sends log using a predefined tag. This is used to better debug or to catch errors.
     * Logging should always use sendLog to coalesce logs into one single place.
     *
     * @param level   Urgency level of the log to send. 3 is the ceiling and will
     *                send errors. Defaults to debug message when the level is invalid.
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
     * @param <T> The type of the referenced object.
     *
     * @return Object if not null.
     */
    public static <T> T requireNonNull(T obj) {
        if (obj == null) {
            throw new IllegalArgumentException();
        }
        return obj;
    }

    /**
     * Opens a URL/link from a string object.
     *
     * @param context Context object for use with startActivity.
     * @param link    The link to be opened.
     */
    public static void openLink(Context context, String link) {
        Intent linkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        context.startActivity(linkIntent);
    }

    /**
     * Registers a PackageChangesReceiver that monitors installed/removed apps.
     *
     * @param activity        The activity where the receiver should be used.
     * @param packageReceiver The PackageChangesReceiver to be registered.
     */
    public static void registerPackageReceiver(Activity activity, PackageChangesReceiver packageReceiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addDataScheme("package");
        activity.registerReceiver(packageReceiver, intentFilter);
    }

    /**
     * Unregister a PackageChangesReceiver while (safely) trying to ignore any IllegalArgumentsException.
     *
     * @param activity        The activity where the receiver should be used.
     * @param packageReceiver The PackageChangesReceiver to be registered.
     */
    public static void unregisterPackageReceiver(Activity activity, PackageChangesReceiver packageReceiver) {
        try {
            activity.unregisterReceiver(packageReceiver);
        } catch (IllegalArgumentException ignored) {
            // FIXME: Don't ignore this please.
        }
    }
}
