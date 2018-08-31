package mono.hg;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
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
        switch (level) {
            default:
            case 0:
                Log.d("Hubby", message);
                break;
            case 1:
                Log.v("Hubby", message);
                break;
            case 2:
                Log.w("Hubby", message);
                break;
            case 3:
                Log.e("Hubby", message);
                break;
        }
    }

    public static void loadSingleApp(Context context, String packageName,
                                     RecyclerView.Adapter adapter, List<AppDetail> list, Boolean forFavourites) {
        PackageManager manager = context.getPackageManager();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        ApplicationInfo applicationInfo;

        if (manager.getLaunchIntentForPackage(packageName) != null &&
                !list.contains(new AppDetail(null, null, packageName))) {
            try {
                applicationInfo = manager.getApplicationInfo(packageName, 0);
                String appName = manager.getApplicationLabel(applicationInfo).toString();
                Drawable icon = null;
                Drawable getIcon = null;
                if (!prefs.getBoolean("icon_hide", false) || forFavourites) {
                    if (!prefs.getString("icon_pack", "default").equals("default")) {
                        getIcon = new IconPackHelper().getIconDrawable(context, packageName);
                    }
                    if (getIcon == null) {
                        icon = manager.getApplicationIcon(packageName);
                    } else {
                        icon = getIcon;
                    }
                }
                AppDetail app = new AppDetail(icon, appName, packageName);
                list.add(app);
                adapter.notifyItemInserted(list.size());
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("Hubby", e.toString());
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
