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

            // Receive intent from broadcast and let the Pages know they may need refresh.
            if ((intent.action == "android.intent.action.PACKAGE_ADDED" || intent.action == "android.intent.action.PACKAGE_REMOVED" || intent.action == "android.intent.action.PACKAGE_CHANGED" || intent.action == "android.intent.action.PACKAGE_REPLACED") && ! packageName.contains(
                    context.packageName
                )
            ) {
                val mainIntent = Intent()
                mainIntent.action = "mono.hg.PACKAGE_CHANGE_BROADCAST"
                context.sendBroadcast(mainIntent)
            }
        }
    }
}