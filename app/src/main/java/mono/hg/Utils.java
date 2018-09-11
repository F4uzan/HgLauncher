package mono.hg;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mono.hg.helpers.IconPackHelper;

//TODO: Documentations?

public class Utils {
    public static int getStatusBarHeight(Context context, Resources resources) {
        int idStatusBarHeight = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (idStatusBarHeight > 0) {
            return context.getResources().getDimensionPixelSize(idStatusBarHeight);
        } else {
            // Return fallback size if we can't get the value from the system.
            return context.getResources().getDimensionPixelSize(R.dimen.status_bar_height_fallback);
        }
    }

    public static void sendLog(int level, String message) {
        String tag = "HgLogger";
        switch (level) {
            default:
            case 0:
                Log.d(tag, message);
                break;
            case 1:
                Log.v(tag, message);
                break;
            case 2:
                Log.w(tag, message);
                break;
            case 3:
                Log.e(tag, message);
                break;
        }
    }

    public static void openLink(Context context, String link) {
        Intent linkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        context.startActivity(linkIntent);
    }

    public static boolean isAppInstalled(PackageManager packageManager, String packageName) {
        try {
            // Get application info while handling exception spawning from it.
            packageManager.getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            // No, it's not installed.
            return false;
        }
    }

    public static boolean isSystemApp(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        try {
            ApplicationInfo appFlags = manager.getApplicationInfo(packageName, 0);
            if ((appFlags.flags & ApplicationInfo.FLAG_SYSTEM) == 1)
                return true;
        } catch (PackageManager.NameNotFoundException e) {
            Utils.sendLog(3, e.toString());
            return false;
        }
        return false;
    }

    public static void loadSingleApp(Context context, String packageName,
                                     RecyclerView.Adapter adapter, List<AppDetail> list, Boolean forFavourites) {
        PackageManager manager = context.getPackageManager();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        ApplicationInfo applicationInfo;

        if (manager.getLaunchIntentForPackage(packageName) != null &&
                !list.contains(new AppDetail(null, null, packageName, false))) {
            try {
                applicationInfo = manager.getApplicationInfo(packageName, 0);
                String appName = manager.getApplicationLabel(applicationInfo).toString();
                Drawable icon = null;
                Drawable getIcon = null;
                if (!prefs.getBoolean("icon_hide", false) || forFavourites) {
                    if (!prefs.getString("icon_pack", "default").equals("default"))
                        getIcon = new IconPackHelper().getIconDrawable(context, packageName);
                    if (getIcon == null) {
                        icon = manager.getApplicationIcon(packageName);
                    } else {
                        icon = getIcon;
                    }
                }
                AppDetail app = new AppDetail(icon, appName, packageName, false);
                list.add(app);
                if (forFavourites) {
                    adapter.notifyDataSetChanged();
                } else {
                    adapter.notifyItemInserted(list.size());
                }
            } catch (PackageManager.NameNotFoundException e) {
                sendLog(3, e.toString());
            }

            if (!forFavourites) {
                if (!prefs.getBoolean("list_order", false)) {
                    Collections.sort(list, new Comparator<AppDetail>() {
                        @Override
                        public int compare(AppDetail nameL, AppDetail nameR) {
                            return nameR.getAppName().compareToIgnoreCase(nameL.getAppName());
                        }
                    });
                } else {
                    Collections.sort(list, Collections.reverseOrder(new Comparator<AppDetail>() {
                        @Override
                        public int compare(AppDetail nameL, AppDetail nameR) {
                            return nameR.getAppName().compareToIgnoreCase(nameL.getAppName());
                        }
                    }));
                }
            }
        }
    }
}
