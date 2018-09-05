package mono.hg.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PackageChangesReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

        // Receive intent from broadcast and simply let the launcher know it needs a refresh.
        if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")
                || intent.getAction().equals("android.intent.action.PACKAGE_CHANGED")
                && !intent.getDataString().contains("f4.hg")) {
            editor.putBoolean("addApp", true).apply();
            editor.putString("added_app", intent.getDataString().replace("package:", "")).apply();
        }

        if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")
                && !intent.getDataString().contains("f4.hg")) {
            editor.putBoolean("removedApp", true).apply();
            editor.putString("removed_app", intent.getDataString().replace("package:", "")).apply();
        }
    }
}
