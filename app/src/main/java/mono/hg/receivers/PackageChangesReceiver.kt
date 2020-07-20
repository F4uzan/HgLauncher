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
            val packageName = intent.data !!.encodedSchemeSpecificPart
            val requireBroadcast = intent.action == "android.intent.action.PACKAGE_ADDED" ||
                    intent.action == "android.intent.action.PACKAGE_REMOVED" ||
                    intent.action == "android.intent.action.PACKAGE_CHANGED" ||
                    intent.action == "android.intent.action.PACKAGE_REPLACED"

            // Receive intent from broadcast and let the Pages know they may need refresh.
            if (requireBroadcast && ! packageName.contains(context.packageName)) {
                val mainIntent = Intent().apply {
                    putExtra("action", intent.action)
                    putExtra("package", packageName)
                    action = "mono.hg.PACKAGE_CHANGE_BROADCAST"
                }.also {
                    context.sendBroadcast(it)
                }
            }
        }
    }
}