package mono.hg.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;

import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import mono.hg.models.WebSearchProvider;
import mono.hg.utils.Utils.Gesture;

public class PreferenceHelper {
    private static int orientation_mode;
    private static int app_accent;
    private static int app_accent_dark;
    private static int app_accent_darker;
    private static boolean is_new_user;
    private static boolean icon_hide;
    private static boolean list_order;
    private static boolean shade_view;
    private static boolean keyboard_focus;
    private static boolean web_search_enabled;
    private static boolean static_favourites_panel;
    private static boolean static_app_list;
    private static boolean keep_last_search;
    private static boolean adaptive_shade;
    private static boolean windowbar_status_switch;
    private static boolean web_search_long_press;
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
    private static String list_bg;
    private static String gesture_left_action;
    private static String gesture_right_action;
    private static String gesture_up_action;
    private static String gesture_down_action;
    private static String gesture_single_tap_action;
    private static String gesture_double_tap_action;
    private static String windowbar_mode;
    private static String widgets_list;

    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;

    private static ComponentName gesture_handler;

    public static boolean isTesting() {
        return is_testing;
    }

    public static boolean isNewUser() {
        return is_new_user;
    }

    public static int getOrientation() {
        return orientation_mode;
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

    public static String getListBackground() {
        return list_bg;
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

    public static boolean shouldHideStatusBar() {
        return windowbar_status_switch;
    }

    public static String appTheme() {
        return app_theme;
    }

    public static int getAccent() {
        return app_accent;
    }

    public static int getDarkAccent() {
        return app_accent_dark;
    }

    public static int getDarkerAccent() {
        return app_accent_darker;
    }

    public static boolean promptSearch() {
        return web_search_enabled;
    }

    public static boolean extendedSearchMenu() {
        return web_search_long_press;
    }

    public static boolean favouritesAcceptScroll() {
        return !static_favourites_panel;
    }

    public static boolean keepAppList() {
        return static_app_list;
    }

    public static boolean keepLastSearch() {
        return keep_last_search;
    }

    public static boolean wasAlien() {
        return was_alien;
    }

    public static void isAlien(boolean alien) {
        was_alien = alien;
    }

    public static String getGestureForDirection(int direction) {
        switch (direction) {
            case Gesture.LEFT:
                return gesture_left_action;
            case Gesture.RIGHT:
                return gesture_right_action;
            case Gesture.UP:
                return gesture_up_action;
            case Gesture.DOWN:
                return gesture_down_action;
            case Gesture.TAP:
                return gesture_single_tap_action;
            case Gesture.DOUBLE_TAP:
                return gesture_double_tap_action;
            default:
                return "";
        }
    }

    public static ComponentName getGestureHandler() {
        return gesture_handler;
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
                return "https://www.google.com/search?q=%s";
            case "ddg":
                return "https://www.duckduckgo.com/?q=%s";
            case "searx":
                return "https://www.searx.me/?q=%s";
            case "startpage":
                return "https://www.startpage.com/do/search?query=%s";
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
        String[] toParse;
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
        String[] splitPackage;
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

    public static void applyWidgetsUpdate() {
        widgets_list = getPreference().getString("widgets_list", "");
    }

    public static void update(String id, HashSet<String> stringSet) {
        getEditor().putStringSet(id, stringSet).apply();
    }

    public static void update(String id, String string) {
        getEditor().putString(id, string).apply();
    }

    public static void update(String id, boolean state) {
        getEditor().putBoolean(id, state).apply();
    }

    public static void update(String id, int integer) {
        getEditor().putInt(id, integer).apply();
    }

    public static void fetchPreference() {
        is_testing = getPreference().getBoolean("is_grandma", false);
        is_new_user = preferences.getBoolean("is_new_user", true);
        launch_anim = getPreference().getString("launch_anim", "default");
        orientation_mode = Integer.parseInt(getPreference().getString("orientation_mode", "-1"));
        icon_hide = getPreference().getBoolean("icon_hide_switch", false);
        icon_pack = getPreference().getString("icon_pack", "default");
        list_order = getPreference().getString("list_order", "alphabetical")
                                    .equals("invertedAlphabetical");
        list_bg = getPreference().getString("list_bg", "theme");
        shade_view = getPreference().getBoolean("shade_view_switch", false);
        keyboard_focus = getPreference().getBoolean("keyboard_focus", false);
        app_theme = getPreference().getString("app_theme", "light");
        app_accent = getPreference().getInt("app_accent", -49023); // The default accent in Int.
        app_accent_dark = ColorUtils.blendARGB(app_accent, Color.BLACK, 0.1f);
        app_accent_darker = ColorUtils.blendARGB(app_accent, Color.BLACK, 0.4f);
        web_search_enabled = getPreference().getBoolean("web_search_enabled", true);
        web_search_long_press = getPreference().getBoolean("web_search_long_press", false);
        search_provider_set = getPreference().getString("search_provider", "none");
        static_favourites_panel = getPreference().getBoolean("static_favourites_panel_switch",
                false);
        static_app_list = getPreference().getBoolean("static_app_list_switch", false);
        keep_last_search = getPreference().getBoolean("keep_last_search_switch", false);
        adaptive_shade = getPreference().getBoolean("adaptive_shade_switch", false);
        windowbar_status_switch = getPreference().getBoolean("windowbar_status_switch", false);
        windowbar_mode = getPreference().getString("windowbar_mode", "none");
        gesture_left_action = getPreference().getString("gesture_left", "none");
        gesture_right_action = getPreference().getString("gesture_right", "none");
        gesture_up_action = getPreference().getString("gesture_up", "none");
        gesture_down_action = getPreference().getString("gesture_down", "none");
        gesture_single_tap_action = getPreference().getString("gesture_single_tap", "list");
        gesture_double_tap_action = getPreference().getString("gesture_double_tap", "none");
        gesture_handler = ComponentName.unflattenFromString(
                getPreference().getString("gesture_handler", "none"));
        widgets_list = getPreference().getString("widgets_list", "");

        exclusion_list = (HashSet<String>) getPreference().getStringSet("hidden_apps",
                new HashSet<String>());
        HashSet<String> temp_label_list = (HashSet<String>) getPreference().getStringSet(
                "label_list",
                new HashSet<String>());
        parseProviders(
                (HashSet<String>) getPreference().getStringSet("provider_list",
                        new HashSet<String>()));

        label_list_set.addAll(temp_label_list);
        fetchLabels();
    }
}