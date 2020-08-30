package mono.hg.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import mono.hg.LauncherActivity
import mono.hg.R
import mono.hg.helpers.PreferenceHelper
import mono.hg.models.WebSearchProvider
import mono.hg.receivers.PackageChangesReceiver

/**
 * A misc. utils class for other various helpers and utilities functions.
 */
object Utils {
    /**
     * Sends log using a predefined tag. This is used to better debug or to catch errors.
     * Logging should always use sendLog to coalesce logs into one single source.
     *
     * @param level   Urgency level of the log to send. 3 is the ceiling and will
     * send errors. Defaults to debug message when the level is invalid.
     * @param message The message to send to logcat.
     */
    fun sendLog(level: Int, message: String) {
        val tag = "HgLogger"
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.VERBOSE -> Log.v(tag, message)
            LogLevel.WARNING -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
            else -> Log.d(tag, message)
        }
    }

    /**
     * Checks whether the system SDK version is around the specified version.
     *
     * @param version Version expected to compare against the system's SDK.
     *
     * @return True if system SDK version is equal to or more than the specified version.
     */
    fun sdkIsAround(version: Int): Boolean {
        return Build.VERSION.SDK_INT >= version
    }

    /**
     * Checks whether the system SDK version is below a specified version.
     *
     * @param version Version expected to compare against the system's SDK.
     *
     * @return True if system SDK version is below the specified version.
     */
    fun sdkIsBelow(version: Int): Boolean {
        return Build.VERSION.SDK_INT < version
    }

    /**
     * Checks whether the system is at least KitKat.
     *
     * @return True when system SDK version is equal to or more than 19 (KitKat).
     */
    fun atLeastKitKat(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
    }

    /**
     * Checks whether the system is at least Lollipop.
     *
     * @return True when the system SDK is equal to or more than 21 (L).
     */
    fun atLeastLollipop(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

    /**
     * Checks whether the system is at least Marshmallow.
     *
     * @return True when the system SDK is equal to or more than 23 (M).
     */
    fun atLeastMarshmallow(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    /**
     * Checks whether the system is at least Oreo.
     *
     * @return True when the system SDK is equal to or more than 26 (O).
     */
    fun atLeastOreo(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    /**
     * Checks whether the system is at least Q.
     *
     * @return True when the system SDK is equal to or more than 29 (Q).
     */
    fun atLeastQ(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    /**
     * Checks whether the system is at least R.
     *
     * @return True when the system SDK is equal to or more than 30 (R).
     */
    fun atLeastR(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    /**
     * Parses a web provider URL then replaces its placeholder with the query.
     *
     * @param context  Context object for use with startActivity.
     * @param provider The URL of the provider
     * @param query    The query itself
     */
    fun doWebSearch(context: Context, provider: String, query: String) {
        with(Intent(Intent.ACTION_VIEW, Uri.parse(provider.replace("%s", query)))) {
            context.startActivity(this)
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
    fun getColorFromAttr(context: Context, @AttrRes attr: Int): Int {
        return with(TypedValue()) {
            context.theme.resolveAttribute(attr, this, true)
            this.data
        }
    }

    /**
     * Registers a PackageChangesReceiver on an activity.
     *
     * This function does not check the availability or the
     * nullity of the package receiver. It is recommended that before
     * calling this function, a check is performed first.
     *
     * @param activity        The activity where PackageChangesReceiver is to be registered.
     * @param packageReceiver The receiver itself.
     */
    fun registerPackageReceiver(
        activity: AppCompatActivity,
        packageReceiver: PackageChangesReceiver?
    ) {
        IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }.also {
            activity.registerReceiver(packageReceiver, it)
        }
    }

    /**
     * Unregisters a PackageChangesReceiver from an activity.
     *
     * This function should be called when the receiver in question
     * exists and have not been destroyed/removed by the system. Otherwise,
     * there is a risk of exceptions arising, not handled by this function.
     *
     * @param activity        The activity where PackageChangesReceiver is to be registered.
     * @param packageReceiver The receiver itself.
     */
    fun unregisterPackageReceiver(
        activity: AppCompatActivity,
        packageReceiver: PackageChangesReceiver?
    ) {
        try {
            activity.unregisterReceiver(packageReceiver)
        } catch (w: IllegalArgumentException) {
            sendLog(LogLevel.DEBUG, "Failed to remove receiver!")
        }
    }

    /**
     * Handles gesture actions based on the direction of the gesture.
     *
     * @param activity  Instance of LauncheActivity.
     * @param direction The direction of the gesture.
     *
     * @see Gesture Valid directions for the gestures.
     */
    fun handleGestureActions(activity: LauncherActivity, direction: Int) {
        when (PreferenceHelper.getGestureForDirection(direction)) {
            "handler" -> PreferenceHelper.gestureHandler?.apply {
                with(Intent("mono.hg.GESTURE_HANDLER")) {
                    component = this@apply
                    type = "text/plain"
                    putExtra("direction", direction)
                }.also {
                    activity.startActivity(it)
                }
            }
            "widget" -> activity.doThis("open_widgets")
            "status" -> ActivityServiceUtils.expandStatusBar(activity)
            "panel" -> ActivityServiceUtils.expandSettingsPanel(activity)
            "list" -> activity.doThis("show_panel")
            "none" -> {
            }
            else -> try {
                AppUtils.quickLaunch(
                    activity,
                    PreferenceHelper.getGestureForDirection(direction)
                )
            } catch (w: ActivityNotFoundException) {
                // Maybe the user had an old configuration, but otherwise, this is harmless.
                // We should still notify them though.
                Toast.makeText(
                    activity,
                    activity.getString(R.string.err_activity_null),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Handles common input shortcuts from an EditText.
     *
     * The input shortcut currently defined are:
     * * Select All (CTRL+A)
     * * Copy (CTRL+C)
     * * Paste (CTRL+V)
     * * Cut (CTRL+X)
     *
     * The shortcuts here may be overridden by the system, as such
     * it is best if there is a check for such cases.
     *
     * @param activity The activity to reference for copying and pasting.
     * @param editText The EditText where the text is being copied/pasted.
     * @param keyCode  Keycode to handle.
     *
     * @return Boolean True if key is handled.
     */
    fun handleInputShortcut(
        activity: AppCompatActivity,
        editText: EditText,
        keyCode: Int
    ): Boolean {
        // Get selected text for cut and copy.
        with(editText) {
            val text = editText.text.toString().substring(selectionStart, selectionEnd)
            return when (keyCode) {
                KeyEvent.KEYCODE_A -> {
                    selectAll()
                    true
                }
                KeyEvent.KEYCODE_X -> {
                    ActivityServiceUtils.copyToClipboard(activity, text)
                    setText(this.text.toString().replace(text, ""))
                    true
                }
                KeyEvent.KEYCODE_C -> {
                    ActivityServiceUtils.copyToClipboard(activity, text)
                    true
                }
                KeyEvent.KEYCODE_V -> {
                    this.text = this.text.replace(
                        selectionStart.coerceAtMost(selectionEnd),
                        selectionStart.coerceAtLeast(selectionEnd),
                        ActivityServiceUtils.pasteFromClipboard(activity), 0,
                        ActivityServiceUtils.pasteFromClipboard(activity).length
                    )
                    true
                }
                else -> false // Do nothing.
            }
        }
    }

    /**
     * Set default (starting) providers for indeterminate search.
     *
     * This function is always called when the provider list is empty
     * to prevent the user from seeing an empty provider selection.
     *
     * @see [R.array.pref_search_provider_title]
     *
     * @param resources The resources object, used to retrieve the default array.
     */
    fun setDefaultProviders(
        resources: Resources,
        list: ArrayList<WebSearchProvider>
    ): ArrayList<WebSearchProvider> {
        val defaultProvider = resources.getStringArray(
            R.array.pref_search_provider_title
        )
        val defaultProviderId = resources.getStringArray(
            R.array.pref_search_provider_values
        )

        // defaultProvider will always be the same size as defaultProviderUrl.
        // However, we start at 1 to ignore the 'Always ask' option.
        defaultProvider.forEachIndexed { index, _ ->
            if (index > 0) {
                list.add(
                    WebSearchProvider(
                        defaultProvider[index],
                        PreferenceHelper.getDefaultProvider(defaultProviderId[index]),
                        defaultProvider[index]
                    )
                )
            }
        }

        return list
    }

    /**
     * The importance (level) of log message.
     */
    @IntDef(LogLevel.DEBUG, LogLevel.VERBOSE, LogLevel.WARNING, LogLevel.ERROR)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class LogLevel {
        companion object {
            const val DEBUG = 0
            const val VERBOSE = 1
            const val WARNING = 2
            const val ERROR = 3
        }
    }

    /**
     * Directions of gesture.
     */
    @IntDef(
        Gesture.LEFT,
        Gesture.RIGHT,
        Gesture.UP,
        Gesture.DOWN,
        Gesture.TAP,
        Gesture.DOUBLE_TAP,
        Gesture.PINCH
    )
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class Gesture {
        companion object {
            const val LEFT = 0
            const val RIGHT = 1
            const val UP = 10
            const val DOWN = 11
            const val TAP = 100
            const val DOUBLE_TAP = 101
            const val PINCH = 111
        }
    }
}