package mono.hg.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import mono.hg.LauncherActivity

class PackageChangesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != null && intent.data != null) {
            val packageName = intent.data!!.encodedSchemeSpecificPart

            // Receive intent from broadcast and simply let the launcher know it needs a refresh.
            if (intent.action == "android.intent.action.PACKAGE_ADDED" || intent.action == "android.intent.action.PACKAGE_REMOVED" || intent.action == "android.intent.action.PACKAGE_CHANGED" || intent.action == "android.intent.action.PACKAGE_REPLACED" && !packageName.contains(context.packageName)) {
                val mainIntent = Intent(context, LauncherActivity::class.java)
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(mainIntent)
            }
        }
    }
}