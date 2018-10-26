package mono.hg.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import mono.hg.helpers.PreferenceHelper;

public class PackageChangesReceiver extends BroadcastReceiver {

    @Override public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getData() != null) {
            String packageName = intent.getData().getEncodedSchemeSpecificPart();
            boolean isReplacing = false;

            if (intent.getAction().equals("android.intent.action.PACKAGE_CHANGED")
                    && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                isReplacing = true;
            }

            // Receive intent from broadcast and simply let the launcher know it needs a refresh.
            if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")
                    || intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")
                    && !packageName.contains(context.getPackageName()) && !isReplacing) {
                PreferenceHelper.getEditor().putBoolean("refreshList", true).apply();
            }
        }
    }
}
