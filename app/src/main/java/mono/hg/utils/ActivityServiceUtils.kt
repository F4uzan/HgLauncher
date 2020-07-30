package mono.hg.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import mono.hg.R
import mono.hg.utils.Utils.LogLevel
import java.lang.reflect.Method

/**
 * Utils class that handles calls to SystemService.
 */
object ActivityServiceUtils {
    /**
     * Hides the software keyboard.
     *
     * @param activity The activity where the keyboard focus is being achieved.
     */
    fun hideSoftKeyboard(activity: Activity) {
        val inputMethodManager = activity.getSystemService(
            Activity.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        if (activity.currentFocus != null) {
            if (Utils.atLeastR()) {
                activity.currentFocus?.windowInsetsController?.hide(WindowInsets.Type.ime())
            } else {
                inputMethodManager.hideSoftInputFromWindow(
                    activity.currentFocus !!.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
                activity.currentFocus !!.clearFocus()
            }
        }
    }

    /**
     * Show the software keyboard and request focus to a certain input field.
     *
     * @param activity The activity hosting the view to be in focus.
     * @param view     The view requesting focus.
     */
    fun showSoftKeyboard(activity: Activity, view: View) {
        val inputMethodManager = activity.getSystemService(
            Activity.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        if (Utils.atLeastR()) {
            view.windowInsetsController?.show(WindowInsets.Type.ime())
        } else {
            if (! view.isFocused) {
                view.requestFocus()
            }
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
        }
    }

    /**
     * Checks if the system is currently in battery saver mode.
     *
     * @param activity The activity where getSystemService can be received.
     *
     * @return false if battery saver is not enabled.
     */
    fun isPowerSaving(activity: Activity): Boolean {
        val powerManager = activity
            .getSystemService(Context.POWER_SERVICE) as PowerManager
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && powerManager.isPowerSaveMode
    }

    /**
     * Copies text to keyboard.
     *
     * @param activity Activity where CLIPBOARD_SERVICE can be fetched.
     * @param text     Text to copy.
     */
    fun copyToClipboard(activity: Activity, text: String?) {
        val clipboardManager = activity
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(null, text)
        clipboardManager.setPrimaryClip(clipData)
    }

    /**
     * Pastes the first clip item containing the mimetype text.
     *
     * @param activity Activity where CLIPBOARD_SERVICE can be fetched.
     *
     * @return CharSequence text from clipboard, empty if there isn't any.
     */
    fun pasteFromClipboard(activity: Activity): CharSequence {
        val clipboardManager = activity
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return if (clipboardManager.hasPrimaryClip() && clipboardManager.primaryClipDescription !!.hasMimeType(
                ClipDescription.MIMETYPE_TEXT_PLAIN
            )
        ) {
            clipboardManager.primaryClip !!.getItemAt(0).text
        } else {
            ""
        }
    }

    /**
     * Expands the status bar.
     *
     * @param activity The activity where status bar service can be retrieved.
     */
    @SuppressLint("WrongConstant", "PrivateApi")
    fun expandStatusBar(activity: Activity) {
        try {
            val service = activity.getSystemService("statusbar")
            val statusbarManager = Class.forName("android.app.StatusBarManager")
            val showStatusbar: Method
            showStatusbar = if (Utils.sdkIsAround(17)) {
                statusbarManager.getMethod("expandNotificationsPanel")
            } else {
                statusbarManager.getMethod("expand")
            }
            showStatusbar.invoke(service)
        } catch (w: Exception) {
            Utils.sendLog(LogLevel.WARNING, "Exception in expandStatusBar: $w")
        }
    }

    /**
     * Expands the quick settings panel.
     *
     * @param activity The activity where status bar service can be retrieved.
     */
    @SuppressLint("WrongConstant", "PrivateApi")
    fun expandSettingsPanel(activity: Activity) {
        try {
            val service = activity.getSystemService("statusbar")
            val statusbarManager = Class.forName("android.app.StatusBarManager")
            val showPanel: Method
            if (Utils.sdkIsAround(17)) {
                showPanel = statusbarManager.getMethod("expandSettingsPanel")
                showPanel.invoke(service)
            } else {
                Toast.makeText(activity, R.string.err_no_method_panel, Toast.LENGTH_SHORT).show()
            }
        } catch (w: Exception) {
            Utils.sendLog(LogLevel.WARNING, "Exception in expandSettingsPanel: $w")
        }
    }
}