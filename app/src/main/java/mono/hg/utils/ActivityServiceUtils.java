package mono.hg.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class ActivityServiceUtils {

    /**
     * Hides the software keyboard.
     *
     * @param activity The activity where the keyboard focus is being achieved.
     */
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(
                Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && activity.getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(),
                    0);
        }
    }

    /**
     * Show the software keyboard and request focus to a certain input field.
     *
     * @param activity The activity hosting the view to be in focus.
     * @param view     The view requesting focus.
     */
    public static void showSoftKeyboard(Activity activity, View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(
                Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && view.isFocusable()) {
            inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            view.requestFocus();
        }
    }

    /**
     * Checks if the system is currently in battery saver mode.
     *
     * @param activity The activity where getSystemService can be received.
     * @return false if battery saver is not enabled.
     */
    public static boolean isPowerSaving(Activity activity) {
        PowerManager powerManager = (PowerManager) activity
                .getSystemService(Context.POWER_SERVICE);
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && powerManager != null && powerManager.isPowerSaveMode();
    }

    /**
     * Copies text to keyboard.
     *
     * @param activity Activity where CLIPBOARD_SERVICE can be fetched.
     * @param text     Text to copy.
     */
    public static void copyToClipboard(Activity activity, String text) {
        ClipboardManager clipboardManager = (ClipboardManager) activity
                .getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData clipData = ClipData.newPlainText(null, text);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(clipData);
        }
    }

    /**
     * Pastes the first clip item containing the mimetype text.
     *
     * @param activity Activity where CLIPBOARD_SERVICE can be fetched.
     *
     * @return CharSequence text from clipboard, empty if there isn't any.
     */
    public static CharSequence pasteFromClipboard(Activity activity) {
        ClipboardManager clipboardManager = (ClipboardManager) activity
                .getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboardManager != null && clipboardManager.hasPrimaryClip()
                && clipboardManager.getPrimaryClipDescription()
                                   .hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            return clipboardManager.getPrimaryClip().getItemAt(0).getText();
        } else {
            return "";
        }
    }

}
