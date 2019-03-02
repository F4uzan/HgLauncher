package mono.hg.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import mono.hg.models.WebSearchProvider;

public class PreferenceHelper {
    private static boolean icon_hide;
    private static boolean list_order;
    private static boolean shade_view;
    private static boolean keyboard_focus;
    private static boolean web_search_enabled;
    private static boolean tap_to_drawer;
    private static boolean static_favourites_panel;
    private static boolean static_app_list;
    private static boolean adaptive_shade;
    private static boolean windowbar_status_switch;
    private static boolean is_testing;
    private static boolean was_alien;
    private static Map<String, String> label_list = new HashMap<>();
    private static Map<String, String> provider_list = new HashMap<>();
    private static HashSet<String> label_list_set = new HashSet<>();
    private static HashSet<String> exclusion_list;
    private static String launch_anim;
    private static String app_theme;
    private static String search_provider_set;
    private static String icon_pack;
    private static String gesture_left_action;
    private static String gesture_right_action;
    private static String gesture_up_action;
    private static String windowbar_mode;
    private static String widgets_list;

    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;

    public static boolean isTesting() {
        return is_testing;
    }

    public static HashSet<String> getExclusionList() {
        return exclusion_list;
    }

    public static Map<String, String> getProviderList() {
        return provider_list;
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

    public static boolean allowTapToOpen() {
        return tap_to_drawer;
    }

    public static boolean shouldHideStatusBar() {
        return windowbar_status_switch;
    }

    public static String appTheme() {
        return app_theme;
    }

    public static boolean promptSearch() {
        return web_search_enabled;
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

    public static String doSwipeUp() {
        return gesture_up_action;
    }

    public static String getWindowBarMode() {
        return windowbar_mode;
    }

    public static String getSearchProvider() {
        if ("none".equals(search_provider_set)) {
            return "none";
        } else {
            return getProvider(search_provider_set);
        }
    }

    public static String getDefaultProvider(String provider_id) {
        switch (provider_id) {
            case "google":
                return "https://www.google.com/search?q=";
            case "ddg":
                return "https://www.duckduckgo.com/?q=";
            case "searx":
                return "https://www.searx.me/?q=";
            default:
                // We can't go here. Return an empty string just in case.
                return "";
        }
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

    private static void parseProviders(HashSet<String> set) {
        String toParse[];
        for (String parse : set) {
            toParse = parse.split("\\|");
            provider_list.put(toParse[0], toParse[1]);
        }
    }

    public static void updateProvider(ArrayList<WebSearchProvider> list) {
        HashSet<String> tempList = new HashSet<>();

        for (WebSearchProvider provider : list) {
            tempList.add(provider.getName() + "|" + provider.getUrl());
        }

        update("provider_list", tempList);

        // Clear and update our Map.
        provider_list.clear();
        parseProviders(tempList);
    }

    public static String getProvider(String id) {
        if (getProviderList().containsKey(id)) {
            return provider_list.get(id);
        } else {
            // Whoops.
            return "none";
        }
    }

    private static void fetchLabels() {
        String splitPackage[];
        for (String packageName : label_list_set) {
            splitPackage = packageName.split("\\|");
            label_list.put(splitPackage[0], splitPackage[1]);
        }
    }

    public static ArrayList<String> getWidgetList() {
        ArrayList<String> tempList = new ArrayList<>();
        if (!"".equals(widgets_list)) {
            Collections.addAll(tempList, widgets_list.split(";"));
        }
        return tempList;
    }

    private static void updateSeparatedSet(String pref_id, Map<String, String> map, HashSet<String> set) {
        for (Map.Entry<String, String> newItem : map.entrySet()) {
            set.add(newItem.getKey() + "|" + newItem.getValue());
        }
        update(pref_id, set);
    }

    public static String getLabel(String packageName) {
        return label_list.get(packageName);
    }

    public static void updateLabel(String packageName, String newLabel, boolean remove) {
        if (remove) {
            label_list.remove(packageName);
        } else {
            label_list.put(packageName, newLabel);
        }

        // Clear then add the set.
        label_list_set.clear();
        updateSeparatedSet("label_list", label_list, label_list_set);
    }

    public static void updateWidgets(ArrayList<String> list) {
        String tempList = "";
        for (String widgets : list) {
            if (!"".equals(widgets)) {
                tempList = tempList.concat(widgets + ";");
            }
        }
        update("widgets_list", tempList);
    }

    public static void update(String id, HashSet<String> stringSet) {
        getEditor().putStringSet(id, stringSet).apply();
    }

    public static void update(String id, String string) {
        getEditor().putString(id, string).apply();
    }

    public static void update(String id, int integer) {
        getEditor().putInt(id, integer).apply();
    }

    public static void fetchPreference() {
        is_testing = preferences.getBoolean("is_grandma", false);
        launch_anim = preferences.getString("launch_anim", "default");
        icon_hide = preferences.getBoolean("icon_hide_switch", false);
        icon_pack = preferences.getString("icon_pack", "default");
        list_order = preferences.getString("list_order", "alphabetical")
                                .equals("invertedAlphabetical");
        shade_view = preferences.getBoolean("shade_view_switch", false);
        keyboard_focus = preferences.getBoolean("keyboard_focus", false);
        tap_to_drawer = preferences.getBoolean("tap_to_drawer", true);
        app_theme = preferences.getString("app_theme", "light");
        web_search_enabled = preferences.getBoolean("web_search_enabled", true);
        search_provider_set = preferences.getString("search_provider", "none");
        static_favourites_panel = preferences.getBoolean("static_favourites_panel_switch", false);
        static_app_list = preferences.getBoolean("static_app_list_switch", false);
        adaptive_shade = preferences.getBoolean("adaptive_shade_switch", false);
        windowbar_status_switch = preferences.getBoolean("windowbar_status_switch", false);
        windowbar_mode = preferences.getString("windowbar_mode", "none");
        gesture_left_action = preferences.getString("gesture_left", "none");
        gesture_right_action = preferences.getString("gesture_right", "none");
        gesture_up_action = preferences.getString("gesture_up", "none");
        widgets_list = preferences.getString("widgets_list", "");

        exclusion_list = (HashSet<String>) preferences.getStringSet("hidden_apps",
                new HashSet<String>());
        HashSet<String> temp_label_list = (HashSet<String>) preferences.getStringSet("label_list",
                new HashSet<String>());
        parseProviders(
                (HashSet<String>) preferences.getStringSet("provider_list", new HashSet<String>()));

        label_list_set.addAll(temp_label_list);
        fetchLabels();
    }
}