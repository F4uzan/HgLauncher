package mono.hg.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;

public class PreferenceHelper {
    private static boolean icon_hide;
    private static boolean list_order;
    private static boolean shade_view;
    private static boolean keyboard_focus;
    private static boolean web_search_enabled;
    private static boolean comfy_padding;
    private static boolean tap_to_drawer;
    private static boolean favourites_panel;
    private static boolean static_favourites_panel;
    private static boolean static_app_list;
    private static boolean adaptive_shade;
    private static boolean has_widget;
    private static boolean is_testing;
    private static boolean was_alien;
    private static HashSet<String> exclusion_list;
    private static String launch_anim;
    private static String app_theme;
    private static String search_provider_set;
    private static String icon_pack;
    private static String gesture_left_action;
    private static String gesture_right_action;
    private static String windowbar_mode;

    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;

    public static boolean isTesting() {
        return is_testing;
    }

    public static boolean hasWidget() {
        return has_widget;
    }

    public static HashSet<String> getExclusionList() {
        return exclusion_list;
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

    public static boolean keepAppList() {
        return static_app_list;
    }

    public static boolean wasAlien() {
        return was_alien;
    }

    public static void isAlien(boolean alien) {
        was_alien = alien;
    }

    public static String doSwipeRight() {
        return gesture_right_action;
    }

    public static String doSwipeLeft() {
        return gesture_left_action;
    }

    public static String getWindowBarMode() {
        return windowbar_mode;
    }

    public static String getSearchProvider() {
        String search_provider;

        switch (search_provider_set) {
            case "google":
                search_provider = "https://www.google.com/search?q=";
                break;
            case "ddg":
                search_provider = "https://www.duckduckgo.com/?q=";
                break;
            case "searx":
                search_provider = "https://www.searx.me/?q=";
                break;
            default:
            case "none":
                search_provider = "none";
                break;
        }
        return search_provider;
    }

    public static String getSearchProvider(String provider_id) {
        String search_provider = "";

        switch (provider_id) {
            case "google":
                search_provider = "https://www.google.com/search?q=";
                break;
            case "ddg":
                search_provider = "https://www.duckduckgo.com/?q=";
                break;
            case "searx":
                search_provider = "https://www.searx.me/?q=";
                break;
            default:
                // No-op;
        }
        return search_provider;
    }

    public static SharedPreferences getPreference() {
        return preferences;
    }

    public static SharedPreferences.Editor getEditor() {
        return editor;
    }

    public static boolean hasEditor() {
        return editor != null;
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
        tap_to_drawer = preferences.getBoolean("tap_to_drawer", true);
        app_theme = preferences.getString("app_theme", "light");
        web_search_enabled = preferences.getBoolean("web_search_enabled", true);
        search_provider_set = preferences.getString("search_provider", "google");
        favourites_panel = preferences.getBoolean("favourites_panel_switch", true);
        static_favourites_panel = preferences.getBoolean("static_favourites_panel_switch", false);
        static_app_list = preferences.getBoolean("static_app_list_switch", false);
        adaptive_shade = preferences.getBoolean("adaptive_shade_switch", false);
        windowbar_mode = preferences.getString("windowbar_mode", "none");
        gesture_left_action = preferences.getString("gesture_left", "none");
        gesture_right_action = preferences.getString("gesture_right", "none");

        exclusion_list = (HashSet<String>) preferences.getStringSet("hidden_apps", new HashSet<String>());
    }
}