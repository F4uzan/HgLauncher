package mono.hg.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceHelper {
    private static boolean icon_hide, list_order, shade_view,
            keyboard_focus, web_search_enabled, comfy_padding,
            tap_to_drawer, favourites_panel, dismiss_panel;
    private static String launch_anim, search_provider, app_theme, search_provider_set;

    public static String getLaunchAnim() {
        return launch_anim;
    }

    public static boolean shouldHideIcon() {
        return icon_hide;
    }

    public static boolean isListInverted() {
        return list_order;
    }

    public static boolean useWallpaperShade() {
        return shade_view;
    }

    public static boolean shouldFocusKeyboard() {
        return keyboard_focus;
    }

    public static boolean usesComfyPadding() {
        return comfy_padding;
    }

    public static boolean shouldDismissOnLeave() {
        return dismiss_panel;
    }

    public static boolean allowTapToOpen() {
        return tap_to_drawer;
    }

    public static String appTheme() {
        return app_theme;
    }

    public static boolean promptSearch() {
        return web_search_enabled;
    }

    public static boolean isFavouritesEnabled() {
        return favourites_panel;
    }

    public static String getSearchProvider() {
        switch (search_provider_set) {
            default:
            case "google":
                search_provider = "https://www.google.com/search?q=";
                break;
            case "ddg":
                search_provider = "https://www.duckduckgo.com/?q=";
                break;
            case "searx":
                search_provider = "https://www.searx.me/?q=";
                break;
        }
        return search_provider;
    }

    public static void fetchPreference(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        launch_anim = prefs.getString("launch_anim", "default");
        icon_hide = prefs.getBoolean("icon_hide_switch", false);
        list_order = prefs.getString("list_order", "alphabetical").equals("invertedAlphabetical");
        shade_view = prefs.getBoolean("shade_view_switch", false);
        keyboard_focus = prefs.getBoolean("keyboard_focus", false);
        comfy_padding = prefs.getBoolean("comfy_padding", false);
        dismiss_panel = prefs.getBoolean("dismiss_panel", true);
        tap_to_drawer = prefs.getBoolean("tap_to_drawer", true);
        app_theme = prefs.getString("app_theme", "light");
        web_search_enabled = prefs.getBoolean("web_search_enabled", true);
        search_provider_set = prefs.getString("search_provider", "google");
        favourites_panel = prefs.getBoolean("favourites_panel_switch", true);
    }

}
