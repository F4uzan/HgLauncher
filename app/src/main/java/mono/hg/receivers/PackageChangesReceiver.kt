package mono.hg.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * A receiver called when the launcher receives any changes in packages (apps).
 *
 * This receiver is only used in LauncherActivity and it is not called when the launcher is paused.
 */
open class PackageChangesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != null && intent.data != null) {
            var packageAction = - 1
            val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            val packageName = intent.data?.encodedSchemeSpecificPart ?: ""

            packageAction = if (isReplacing) {
                // If Intent.EXTRA_REPLACING is detected, then the action
                // should be updating, so don't use PACKAGE_REMOVED.
                // https://developer.android.com/reference/android/content/Intent.html#EXTRA_REPLACING
                PACKAGE_UPDATED
            } else when (intent.action) {
                Intent.ACTION_PACKAGE_REPLACED -> PACKAGE_UPDATED
                Intent.ACTION_PACKAGE_ADDED -> PACKAGE_INSTALLED
                Intent.ACTION_PACKAGE_REMOVED, Intent.ACTION_PACKAGE_FULLY_REMOVED -> PACKAGE_REMOVED
                Intent.ACTION_PACKAGE_CHANGED -> PACKAGE_MISC
                else -> - 1
            }

            // Receive intent from broadcast and let the Pages know they may need refresh.
            if (packageAction != - 1 && ! packageName.contains(context.packageName)) {
                Intent().apply {
                    putExtra("action", packageAction)
                    putExtra("package", packageName)
                    action = "mono.hg.PACKAGE_CHANGE_BROADCAST"
                }.also {
                    context.sendBroadcast(it)
                }
            }
        }
    }

    companion object {
        const val PACKAGE_REMOVED = 0
        const val PACKAGE_INSTALLED = 1
        const val PACKAGE_UPDATED = 2
        const val PACKAGE_MISC = 42
    }
}