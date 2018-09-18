package mono.hg.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import mono.hg.Utils;

public class PackageChangesReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

        // Receive intent from broadcast and simply let the launcher know it needs a refresh.
        if (Utils.requireNonNull(intent.getAction()).equals("android.intent.action.PACKAGE_ADDED")
                || intent.getAction().equals("android.intent.action.PACKAGE_CHANGED")
                && !Utils.requireNonNull(intent.getDataString()).contains("f4.hg")) {
            editor.putBoolean("addApp", true).apply();
            editor.putString("added_app", intent.getDataString().replace("package:", "")).apply();
        }

        if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")
                && !intent.getDataString().contains("f4.hg")) {
            editor.putBoolean("removedApp", true).apply();
            editor.putString("removed_app", intent.getDataString().replace("package:", "")).apply();
        }

        // HACK: Unregister immediately because we don't need the receiver anymore.
        // We shouldn't need this, but I don't like the log spam that comes with _not_ doing this.
        context.unregisterReceiver(this);
    }
}
