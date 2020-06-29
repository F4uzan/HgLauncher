package mono.hg.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.widget.EditText;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.appcompat.app.AppCompatActivity;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.util.ArrayList;

import mono.hg.LauncherActivity;
import mono.hg.R;
import mono.hg.fragments.WidgetsDialogFragment;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.models.WebSearchProvider;
import mono.hg.receivers.PackageChangesReceiver;

import static java.lang.annotation.RetentionPolicy.SOURCE;

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
            case LogLevel.DEBUG:
                Log.d(tag, message);
                break;
            case LogLevel.VERBOSE:
                Log.v(tag, message);
                break;
            case LogLevel.WARNING:
                Log.w(tag, message);
                break;
            case LogLevel.ERROR:
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
     * Checks whether the system SDK version is below a specified version.
     *
     * @param version Version expected to compare against the system's SDK.
     *
     * @return True if system SDK version is below the specified version.
     */
    public static boolean sdkIsBelow(int version) {
        return Build.VERSION.SDK_INT < version;
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
     * Checks whether the system is at least Q.
     *
     * @return True when the system SDK is equal to or more than 29 (Q).
     */
    public static boolean atLeastQ() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
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
     * Parses a web provider URL then replaces its placeholder with the query.
     *
     * @param context  Context object for use with startActivity.
     * @param provider The URL of the provider
     * @param query    The query itself
     */
    public static void doWebSearch(Context context, String provider, String query) {
        openLink(context, provider.replace("%s", query));
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
     * Returns a color from an attribute reference.
     *
     * @param context Pass the activity context, not the application context
     * @param attr    The attribute reference to be resolved
     *
     * @return int array of color value
     */
    @ColorInt
    public static int getColorFromAttr(Context context, @AttrRes int attr) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attr, typedValue, true);

        return typedValue.data;
    }

    /**
     * Registers a PackageChangesReceiver on an activity.
     *
     * @param activity        The activity where PackageChangesReceiver is to be registered.
     * @param packageReceiver The receiver itself.
     */
    public static void registerPackageReceiver(AppCompatActivity activity, PackageChangesReceiver packageReceiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addDataScheme("package");
        activity.registerReceiver(packageReceiver, intentFilter);
    }

    /**
     * Unregisters a PackageChangesReceiver from an activity.
     *
     * @param activity        The activity where PackageChangesReceiver is to be registered.
     * @param packageReceiver The receiver itself.
     */
    public static void unregisterPackageReceiver(AppCompatActivity activity, PackageChangesReceiver packageReceiver) {
        try {
            activity.unregisterReceiver(packageReceiver);
        } catch (IllegalArgumentException w) {
            Utils.sendLog(LogLevel.DEBUG, "Failed to remove receiver!");
        }
    }

    /**
     * Handles gesture actions based on the direction of the gesture.
     *
     * @param activity  The activity where the gesture is performed.
     * @param direction The direction of the gesture.
     *
     * @see Gesture Valid directions for the gestures.
     */
    public static void handleGestureActions(AppCompatActivity activity, int direction) {
        UserUtils userUtils = new UserUtils(activity);

        switch (PreferenceHelper.getGestureForDirection(direction)) {
            case "handler":
                if (PreferenceHelper.getGestureHandler() != null) {
                    Intent handlerIntent = new Intent("mono.hg.GESTURE_HANDLER");
                    handlerIntent.setComponent(PreferenceHelper.getGestureHandler());
                    handlerIntent.setType("text/plain");
                    handlerIntent.putExtra("direction", direction);
                    activity.startActivity(handlerIntent);
                }
                break;
            case "widget":
                WidgetsDialogFragment widgetFragment = new WidgetsDialogFragment();
                widgetFragment.show(activity.getSupportFragmentManager(), "Widgets Dialog");
                break;
            case "status":
                ActivityServiceUtils.expandStatusBar(activity);
                break;
            case "panel":
                ActivityServiceUtils.expandSettingsPanel(activity);
                break;
            case "list":
                // TODO: Maybe make this call less reliant on LauncherActivity?
                ((LauncherActivity) activity).doThis("show_panel");
                break;
            case "none":
            default:
                AppUtils.quickLaunch(activity, PreferenceHelper.getGestureForDirection(direction));
                break;
        }
    }

    /**
     * Handles common input shortcut from an EditText.
     *
     * @param activity The activity to reference for copying and pasting.
     * @param editText The EditText where the text is being copied/pasted.
     * @param keyCode  Keycode to handle.
     *
     * @return True if key is handled.
     */
    public static boolean handleInputShortcut(AppCompatActivity activity, EditText editText, int keyCode) {
        // Get selected text for cut and copy.
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        final String text = editText.getText().toString().substring(start, end);

        switch (keyCode) {
            case KeyEvent.KEYCODE_A:
                editText.selectAll();
                return true;
            case KeyEvent.KEYCODE_X:
                editText.setText(editText.getText().toString().replace(text, ""));
                return true;
            case KeyEvent.KEYCODE_C:
                ActivityServiceUtils.copyToClipboard(activity, text);
                return true;
            case KeyEvent.KEYCODE_V:
                editText.setText(
                        editText.getText().replace(Math.min(start, end), Math.max(start, end),
                                ActivityServiceUtils.pasteFromClipboard(activity), 0,
                                ActivityServiceUtils.pasteFromClipboard(activity).length()));
                return true;
            default:
                // Do nothing.
                return false;
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

    /**
     * The importance (level) of log message.
     */
    @IntDef({LogLevel.DEBUG,
            LogLevel.VERBOSE,
            LogLevel.WARNING,
            LogLevel.ERROR})
    @Retention(SOURCE)
    public @interface LogLevel {
        int DEBUG = 0;
        int VERBOSE = 1;
        int WARNING = 2;
        int ERROR = 3;
    }

    /**
     * Directions of gesture.
     */
    @IntDef({Gesture.LEFT,
            Gesture.RIGHT,
            Gesture.UP,
            Gesture.DOWN,
            Gesture.TAP,
            Gesture.DOUBLE_TAP})
    @Retention(SOURCE)
    public @interface Gesture {
        int LEFT = 0;
        int RIGHT = 1;
        int UP = 10;
        int DOWN = 11;
        int TAP = 100;
        int DOUBLE_TAP = 101;
    }
}
