package f4.hubby.receivers;

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
                || intent.getAction().equals("android.intent.action.PACKAGE_CHANGED")) {
            editor.putBoolean("addApp", true).apply();
            editor.putString("added_app", intent.getData().toString().replace("package:", ""));
        }

        if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")) {
            editor.putBoolean("removedApp", true).apply();
            editor.putString("removed_app", intent.getData().toString().replace("package:", ""));
        }
    }
}
