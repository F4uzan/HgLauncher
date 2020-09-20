package mono.hg.helpers

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.preference.PreferenceManager
import mono.hg.models.WebSearchProvider
import mono.hg.utils.Utils
import java.util.*

/**
 * The class that performs CRUD towards preferences.
 *
 * It is preferable that any call to PreferenceManager and SharedPreferences
 * are directed here to unify and simplify their calls.
 */
object PreferenceHelper {
    var orientation = 0
        private set
    var accent = 0
        private set
    var darkAccent = 0
        private set
    var darkerAccent = 0
        private set
    var isNewUser = false
        private set
    private var icon_hide = false
    private var list_order = false
    private var shade_view = false
    private var keyboard_focus = false
    private var web_search_enabled = false
    private var static_favourites_panel = false
    private var static_app_list = false
    private var keep_last_search = false
    private var adaptive_shade = false
    private var windowbar_status_switch = false
    private var widget_space_visible = true
    private var web_search_long_press = false
    private var use_grid_mode = false
    var isTesting = false
        private set
    private var was_alien = false
    private lateinit var pinned_apps_list : String
    private val label_list: MutableMap<String, String> = HashMap()
    private val provider_list: MutableMap<String, String> = HashMap()
    private val label_list_set = HashSet<String>()
    var exclusionList: HashSet<String> = HashSet()
    var launchAnim: String? = null
        private set
    private var app_theme: String? = null
    private var search_provider_set: String? = null
    var iconPackName: String? = null
        private set
    var listBackground: String? = null
        private set
    private lateinit var gesture_left_action: String
    private lateinit var gesture_right_action: String
    private lateinit var gesture_up_action: String
    private lateinit var gesture_down_action: String
    private lateinit var gesture_single_tap_action: String
    private lateinit var gesture_double_tap_action: String
    private lateinit var gesture_pinch_action: String
    var windowBarMode: String? = null
        private set
    private var widgets_list: ArrayList<String> = ArrayList()
    lateinit var preference: SharedPreferences
        private set
    var editor: Editor? = null
        private set
    var gestureHandler: ComponentName? = null
        private set

    val providerList: Map<String, String>
        get() = provider_list

    fun shouldHideIcon(): Boolean {
        return icon_hide
    }

    fun shadeAdaptiveIcon(): Boolean {
        return adaptive_shade
    }

    val isListInverted: Boolean
        get() = ! list_order

    fun useWallpaperShade(): Boolean {
        return shade_view
    }

    fun shouldFocusKeyboard(): Boolean {
        return keyboard_focus
    }

    fun shouldHideStatusBar(): Boolean {
        return windowbar_status_switch
    }

    fun appTheme(): String? {
        return app_theme
    }

    fun promptSearch(): Boolean {
        return web_search_enabled
    }

    fun extendedSearchMenu(): Boolean {
        return web_search_long_press
    }

    fun favouritesAcceptScroll(): Boolean {
        return ! static_favourites_panel
    }

    fun keepAppList(): Boolean {
        return static_app_list
    }

    fun keepLastSearch(): Boolean {
        return keep_last_search
    }

    fun useGrid(): Boolean {
        return use_grid_mode
    }

    fun widgetSpaceVisible(): Boolean {
        return widget_space_visible
    }

    fun wasAlien(): Boolean {
        return was_alien
    }

    fun isAlien(alien: Boolean) {
        was_alien = alien
    }

    fun getGestureForDirection(direction: Int): String {
        return when (direction) {
            Utils.Gesture.LEFT -> gesture_left_action
            Utils.Gesture.RIGHT -> gesture_right_action
            Utils.Gesture.UP -> gesture_up_action
            Utils.Gesture.DOWN -> gesture_down_action
            Utils.Gesture.TAP -> gesture_single_tap_action
            Utils.Gesture.DOUBLE_TAP -> gesture_double_tap_action
            Utils.Gesture.PINCH -> gesture_pinch_action
            else -> ""
        }
    }

    fun widgetList(): ArrayList<String> {
        return widgets_list
    }

    val searchProvider: String?
        get() = if ("none" == search_provider_set) "none" else getProvider(search_provider_set)

    fun getDefaultProvider(provider_id: String?): String {
        return when (provider_id) {
            "google" -> "https://www.google.com/search?q=%s"
            "ddg" -> "https://www.duckduckgo.com/?q=%s"
            "searx" -> "https://www.searx.me/?q=%s"
            "startpage" -> "https://www.startpage.com/do/search?query=%s"
            else -> "" // We can't go here. Return an empty string just in case.
        }
    }

    fun hasEditor(): Boolean {
        return editor != null
    }

    @SuppressLint("CommitPrefEdits")
    fun initPreference(context: Context?) {
        preference = PreferenceManager.getDefaultSharedPreferences(context)
        editor = preference.edit()

        // Initialise widgets early on.
        preference.getString("widgets_list", "")?.split(";")
            ?.filterTo(widgets_list) { it.isNotEmpty() }
    }

    private fun parseDelimitedSet(set: HashSet<String>, map: MutableMap<String, String>) {
        set.forEach { it.split("|").apply { map[this[0]] = this[1] } }
    }

    fun updateProvider(list: ArrayList<WebSearchProvider>) {
        val tempList = HashSet<String>()
        list.mapTo(tempList) { "${it.name}|${it.url}" }
        update("provider_list", tempList)

        // Clear and update our Map.
        provider_list.clear()
        parseDelimitedSet(tempList, provider_list)
    }

    fun getPinnedApps(): String {
        return pinned_apps_list
    }

    fun getProvider(id: String?): String? {
        return provider_list[id] ?: "none"
    }

    private fun updateSeparatedSet(
        pref_id: String,
        map: Map<String, String>,
        set: HashSet<String>
    ) {
        map.mapTo(set) { "${it.key}|${it.value}" }
        update(pref_id, set)
    }

    fun getLabel(packageName: String): String {
        return label_list[packageName] ?: ""
    }

    fun updateLabel(packageName: String, newLabel: String, remove: Boolean) {
        if (remove) {
            label_list.remove(packageName)
        } else {
            label_list[packageName] = newLabel
        }

        // Clear then add the set.
        label_list_set.clear()
        updateSeparatedSet("label_list", label_list, label_list_set)
    }

    fun updateWidgets(list: ArrayList<String>) {
        update("widgets_list", list.filter { it.isNotEmpty() }.joinToString(";"))
    }

    fun update(id: String?, stringSet: HashSet<String>?) {
        editor?.putStringSet(id, stringSet)?.apply()
    }

    fun update(id: String?, string: String?) {
        editor?.putString(id, string)?.apply()
    }

    fun update(id: String?, state: Boolean) {
        editor?.putBoolean(id, state)?.apply()
    }

    fun update(id: String?, integer: Int) {
        editor?.putInt(id, integer)?.apply()
    }

    fun reset() {
        editor?.clear()?.apply()

        // We have to individually clear the collections here.
        widgets_list.clear()
        provider_list.clear()
        label_list_set.clear()
        label_list.clear()
    }

    fun fetchPreference() {
        isTesting = preference.getBoolean("is_grandma", false)
        isNewUser = preference.getBoolean("is_new_user", true)
        launchAnim = preference.getString("launch_anim", "default")
        orientation = preference.getString("orientation_mode", "-1")?.toInt() ?: - 1
        icon_hide = preference.getBoolean("icon_hide_switch", false)
        iconPackName = preference.getString("icon_pack", "default")
        list_order = (preference.getString("list_order", "alphabetical")
                == "invertedAlphabetical")
        listBackground = preference.getString("list_bg", "theme")
        shade_view = preference.getBoolean("shade_view_switch", false)
        keyboard_focus = preference.getBoolean("keyboard_focus", false)
        app_theme = preference.getString("app_theme", "light")
        accent = preference.getInt("app_accent", - 49023) // The default accent in Int.
        darkAccent = ColorUtils.blendARGB(accent, Color.BLACK, 0.25f)
        darkerAccent = ColorUtils.blendARGB(accent, Color.BLACK, 0.4f)
        widget_space_visible = preference.getBoolean("widget_space_visible", true)
        web_search_enabled = preference.getBoolean("web_search_enabled", true)
        web_search_long_press = preference.getBoolean("web_search_long_press", false)
        search_provider_set = preference.getString("search_provider", "none")
        static_favourites_panel = preference.getBoolean(
            "static_favourites_panel_switch",
            false
        )
        static_app_list = preference.getBoolean("static_app_list_switch", false)
        keep_last_search = preference.getBoolean("keep_last_search_switch", false)
        adaptive_shade = preference.getBoolean("adaptive_shade_switch", false)
        windowbar_status_switch = preference.getBoolean("windowbar_status_switch", false)
        windowBarMode = preference.getString("windowbar_mode", "none")
        use_grid_mode = preference.getString("app_list_mode", "list") == "grid"
        gesture_left_action = preference.getString("gesture_left", "none") ?: "none"
        gesture_right_action = preference.getString("gesture_right", "none") ?: "none"
        gesture_up_action = preference.getString("gesture_up", "none") ?: "none"
        gesture_down_action = preference.getString("gesture_down", "none") ?: "none"
        gesture_single_tap_action = preference.getString("gesture_single_tap", "list") ?: "list"
        gesture_double_tap_action = preference.getString("gesture_double_tap", "none") ?: "none"
        gesture_pinch_action = preference.getString("gesture_pinch", "none") ?: "none"
        gestureHandler = ComponentName.unflattenFromString(
            preference.getString("gesture_handler", "none") ?: "none"
        )
        pinned_apps_list = preference.getString("pinned_apps_list", "") ?: ""
        exclusionList = preference.getStringSet("hidden_apps", HashSet()) as HashSet<String>
        val tempLabelList = preference.getStringSet("label_list", HashSet()) as HashSet<String>
        parseDelimitedSet(
            preference.getStringSet("provider_list", HashSet()) as HashSet<String>,
            provider_list
        )
        label_list_set.addAll(tempLabelList)
        parseDelimitedSet(label_list_set, label_list)
    }
}