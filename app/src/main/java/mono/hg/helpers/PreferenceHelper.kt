package mono.hg.helpers

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.graphics.Color
import androidx.preference.PreferenceManager
import androidx.core.graphics.ColorUtils
import mono.hg.models.WebSearchProvider
import mono.hg.utils.Utils
import java.util.*

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
    private var web_search_long_press = false
    private var use_grid_mode = false
    var isTesting = false
        private set
    private var was_alien = false
    private val label_list: MutableMap<String, String> = HashMap()
    private val provider_list: MutableMap<String?, String?> = HashMap()
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
    private var gesture_left_action: String? = null
    private var gesture_right_action: String? = null
    private var gesture_up_action: String? = null
    private var gesture_down_action: String? = null
    private var gesture_single_tap_action: String? = null
    private var gesture_double_tap_action: String? = null
    private var gesture_pinch_action: String? = null
    var windowBarMode: String? = null
        private set
    private var widgets_list: String? = null
    lateinit var preference: SharedPreferences
    private set
    var editor: Editor? = null
        private set
    var gestureHandler: ComponentName? = null
        private set

    val providerList: Map<String?, String?>
        get() = provider_list

    fun shouldHideIcon(): Boolean {
        return icon_hide
    }

    fun shadeAdaptiveIcon(): Boolean {
        return adaptive_shade
    }

    val isListInverted: Boolean
        get() = !list_order

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
        return !static_favourites_panel
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

    fun wasAlien(): Boolean {
        return was_alien
    }

    fun isAlien(alien: Boolean) {
        was_alien = alien
    }

    fun getGestureForDirection(direction: Int): String? {
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

    val searchProvider: String?
        get() = if ("none" == search_provider_set) {
            "none"
        } else {
            getProvider(search_provider_set)
        }

    fun getDefaultProvider(provider_id: String?): String {
        return when (provider_id) {
            "google" -> "https://www.google.com/search?q=%s"
            "ddg" -> "https://www.duckduckgo.com/?q=%s"
            "searx" -> "https://www.searx.me/?q=%s"
            "startpage" -> "https://www.startpage.com/do/search?query=%s"
            else ->                 // We can't go here. Return an empty string just in case.
                ""
        }
    }

    fun hasEditor(): Boolean {
        return editor != null
    }

    fun initPreference(context: Context?) {
        preference = PreferenceManager.getDefaultSharedPreferences(context)
        editor = preference.edit()
    }

    private fun parseProviders(set: HashSet<String>?) {
        var toParse: Array<String?>
        for (parse in set!!) {
            toParse = parse.split("\\|".toRegex()).toTypedArray()
            provider_list[toParse[0]] = toParse[1]
        }
    }

    fun updateProvider(list: ArrayList<WebSearchProvider>) {
        val tempList = HashSet<String>()
        for (provider in list) {
            tempList.add(provider.name + "|" + provider.url)
        }
        update("provider_list", tempList)

        // Clear and update our Map.
        provider_list.clear()
        parseProviders(tempList)
    }

    fun getProvider(id: String?): String? {
        return if (providerList.containsKey(id)) {
            provider_list[id]
        } else {
            // Whoops.
            "none"
        }
    }

    private fun fetchLabels() {
        var splitPackage: Array<String>
        for (packageName in label_list_set) {
            splitPackage = packageName.split("\\|".toRegex()).toTypedArray()
            label_list[splitPackage[0]] = splitPackage[1]
        }
    }

    val widgetList: ArrayList<String>
        get() {
            val tempList = ArrayList<String>()
            if ("" != widgets_list) {
                Collections.addAll(tempList, *widgets_list!!.split(";".toRegex()).toTypedArray())
            }
            return tempList
        }

    private fun updateSeparatedSet(pref_id: String, map: Map<String, String>, set: HashSet<String>) {
        for ((key, value) in map) {
            set.add("$key|$value")
        }
        update(pref_id, set)
    }

    fun getLabel(packageName: String): String? {
        return label_list[packageName]
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

    fun updateWidgets(list: ArrayList<String?>?) {
        var tempList = ""
        for (widgets in list!!) {
            if ("" != widgets) {
                tempList = "$tempList$widgets;"
            }
        }
        update("widgets_list", tempList)
    }

    fun applyWidgetsUpdate() {
        widgets_list = preference.getString("widgets_list", "")
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

    fun fetchPreference() {
        isTesting = preference.getBoolean("is_grandma", false)
        isNewUser = preference.getBoolean("is_new_user", true)
        launchAnim = preference.getString("launch_anim", "default")
        orientation = preference.getString("orientation_mode", "-1")!!.toInt()
        icon_hide = preference.getBoolean("icon_hide_switch", false)
        iconPackName = preference.getString("icon_pack", "default")
        list_order = (preference.getString("list_order", "alphabetical")
                == "invertedAlphabetical")
        listBackground = preference.getString("list_bg", "theme")
        shade_view = preference.getBoolean("shade_view_switch", false)
        keyboard_focus = preference.getBoolean("keyboard_focus", false)
        app_theme = preference.getString("app_theme", "light")
        accent = preference.getInt("app_accent", -49023) // The default accent in Int.
        darkAccent = ColorUtils.blendARGB(accent, Color.BLACK, 0.25f)
        darkerAccent = ColorUtils.blendARGB(accent, Color.BLACK, 0.4f)
        web_search_enabled = preference.getBoolean("web_search_enabled", true)
        web_search_long_press = preference.getBoolean("web_search_long_press", false)
        search_provider_set = preference.getString("search_provider", "none")
        static_favourites_panel = preference.getBoolean("static_favourites_panel_switch",
                false)
        static_app_list = preference.getBoolean("static_app_list_switch", false)
        keep_last_search = preference.getBoolean("keep_last_search_switch", false)
        adaptive_shade = preference.getBoolean("adaptive_shade_switch", false)
        windowbar_status_switch = preference.getBoolean("windowbar_status_switch", false)
        windowBarMode = preference.getString("windowbar_mode", "none")
        use_grid_mode = preference.getString("app_list_mode", "list") == "grid"
        gesture_left_action = preference.getString("gesture_left", "none")
        gesture_right_action = preference.getString("gesture_right", "none")
        gesture_up_action = preference.getString("gesture_up", "none")
        gesture_down_action = preference.getString("gesture_down", "none")
        gesture_single_tap_action = preference.getString("gesture_single_tap", "list")
        gesture_double_tap_action = preference.getString("gesture_double_tap", "none")
        gesture_pinch_action = preference.getString("gesture_pinch", "none")
        gestureHandler = ComponentName.unflattenFromString(
                preference.getString("gesture_handler", "none")!!)
        widgets_list = preference.getString("widgets_list", "")
        exclusionList = preference.getStringSet("hidden_apps", HashSet()) as HashSet<String>
        val temp_label_list = preference.getStringSet("label_list", HashSet()) as HashSet<String>
        parseProviders(preference.getStringSet("provider_list", HashSet()) as HashSet<String>?)
        label_list_set.addAll(temp_label_list)
        fetchLabels()
    }
}