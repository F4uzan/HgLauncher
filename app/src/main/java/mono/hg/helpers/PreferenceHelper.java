package mono.hg.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceHelper {
    private static boolean icon_hide, list_order, shade_view,
            keyboard_focus, web_search_enabled, comfy_padding,
            tap_to_drawer, favourites_panel, dismiss_panel,
            static_favourites_panel, adaptive_shade, has_widget,
            is_testing;
    private static String launch_anim;
    private static String app_theme;
    private static String search_provider_set;
    private static String icon_pack;

    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;

    public static boolean isTesting() {
        return is_testing;
    }

    public static boolean hasWidget() {
        return has_widget;
    }

    public static String getLaunchAnim() {
        return launch_anim;
    }

    public static boolean shouldHideIcon() {
        return icon_hide;
    }

    public static boolean shadeAdaptiveIcon() {
        return adaptive_shade;
    }

    public static String getIconPackName() {
        return icon_pack;
    }

    public static boolean isListInverted() {
        return !list_order;
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

    public static boolean favouritesIgnoreScroll() {
        return static_favourites_panel;
    }

    public static String getSearchProvider() {
        String search_provider;

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

    public static SharedPreferences getPreference() {
        return preferences;
    }

    public static SharedPreferences.Editor getEditor() {
        return editor;
    }

    public static void initPreference(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = preferences.edit();
    }

    public static void fetchPreference() {
        is_testing = preferences.getBoolean("is_grandma", false);
        has_widget = preferences.getBoolean("has_widget", false);
        launch_anim = preferences.getString("launch_anim", "default");
        icon_hide = preferences.getBoolean("icon_hide_switch", false);
        icon_pack = preferences.getString("icon_pack", "default");
        list_order = preferences.getString("list_order", "alphabetical")
                                .equals("invertedAlphabetical");
        shade_view = preferences.getBoolean("shade_view_switch", false);
        keyboard_focus = preferences.getBoolean("keyboard_focus", false);
        comfy_padding = preferences.getBoolean("comfy_padding", false);
        dismiss_panel = preferences.getBoolean("dismiss_panel", true);
        tap_to_drawer = preferences.getBoolean("tap_to_drawer", true);
        app_theme = preferences.getString("app_theme", "light");
        web_search_enabled = preferences.getBoolean("web_search_enabled", true);
        search_provider_set = preferences.getString("search_provider", "google");
        favourites_panel = preferences.getBoolean("favourites_panel_switch", true);
        static_favourites_panel = preferences.getBoolean("static_favourites_panel_switch", false);
        adaptive_shade = preferences.getBoolean("adaptive_shade_switch", false);
    }
}