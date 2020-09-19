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
            var packageAction = -1
            val packageName = intent.data?.encodedSchemeSpecificPart ?: ""

            packageAction = when (intent.action) {
                "android.intent.action.PACKAGE_REPLACED" -> PACKAGE_UPDATED
                "android.intent.action.PACKAGE_ADDED" -> PACKAGE_INSTALLED
                "android.intent.action.PACKAGE_REMOVED", "android.intent.action.PACKAGE_FULLY_REMOVED" -> PACKAGE_REMOVED
                "android.intent.action.PACKAGE_CHANGED" -> PACKAGE_MISC
                else -> -1
            }

            // Receive intent from broadcast and let the Pages know they may need refresh.
            if (packageAction != -1 && ! packageName.contains(context.packageName)) {
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