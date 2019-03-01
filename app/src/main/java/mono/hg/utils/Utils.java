package mono.hg.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;

import mono.hg.R;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.models.WebSearchProvider;
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
     * Checks whether the system SDK version is around the specified version.
     *
     * @param version Version expected to compare against the system's SDK.
     *
     * @return True if system SDK version is equal to or more than the specified version.
     */
    public static boolean sdkIsAround(int version) {
        return Build.VERSION.SDK_INT >= version;
    }

    /**
     * Checks whether the system is at least KitKat.
     *
     * @return True when system SDK version is equal to or more than 19 (KitKat).
     */
    public static boolean atLeastKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    /**
     * Checks whether the system is at least Lollipop.
     *
     * @return True when the system SDK is equal to or more than 21 (L).
     */
    public static boolean atLeastLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * Checks whether the system is at least Marshmallow.
     *
     * @return True when the system SDK is equal to or more than 23 (M).
     */
    public static boolean atLeastMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * Checks whether the system is at least Oreo.
     *
     * @return True when the system SDK is equal to or more than 26 (O).
     */
    public static boolean atLeastOreo() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
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
     * Closes a Closeable instance if it is not null.
     *
     * @param stream The Closeable instance to close.
     */
    public static void closeStream(Closeable stream) {
        try {
            if (stream != null) {
                stream.close();
            }
        } catch (IOException ignored) {
            // Do nothing.
        }
    }

    /**
     * Registers a PackageChangesReceiver on an activity.
     *
     * @param activity        The activity where PackageChangesReceiver is to be registered.
     * @param packageReceiver The receiver itself.
     */
    public static void registerPackageReceiver(Activity activity, PackageChangesReceiver packageReceiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addDataScheme("package");
        activity.registerReceiver(packageReceiver, intentFilter);
    }

    /**
     * Unregisters a PackageChangesReceiver from an activity.
     *
     * @param activity        The activity where PackageChangesReceiver is to be registered.
     * @param packageReceiver The receiver itself.
     */
    public static void unregisterPackageReceiver(Activity activity, PackageChangesReceiver packageReceiver) {
        try {
            activity.unregisterReceiver(packageReceiver);
        } catch (IllegalArgumentException w) {
            Utils.sendLog(0, "Failed to remove receiver!");
        }
    }


    /**
     * Set default providers for indeterminate search.
     */
    public static void setDefaultProviders(Resources resources) {
        String[] defaultProvider = resources.getStringArray(
                R.array.pref_search_provider_title);
        String[] defaultProviderId = resources.getStringArray(
                R.array.pref_search_provider_values);
        ArrayList<WebSearchProvider> tempList = new ArrayList<>();

        // defaultProvider will always be the same size as defaultProviderUrl.
        // However, we start at 1 to ignore the 'Always ask' option.
        for (int i = 1; i < defaultProvider.length; i++) {
            tempList.add(new WebSearchProvider(defaultProvider[i],
                    PreferenceHelper.getDefaultProvider(defaultProviderId[i]),
                    defaultProvider[i]));
        }

        PreferenceHelper.updateProvider(tempList);
    }
}
