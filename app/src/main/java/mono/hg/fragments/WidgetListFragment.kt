package mono.hg.fragments

import android.app.Activity
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.NestedScrollView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import mono.hg.R
import mono.hg.appwidget.LauncherAppWidgetHost
import mono.hg.databinding.FragmentWidgetListBinding
import mono.hg.helpers.PreferenceHelper
import mono.hg.utils.Utils
import java.util.*

/**
 * Page displaying a widget list.
 * This is the generic implementation of a widget list that also handles its scrolling events.
 */
class WidgetListFragment : GenericPageFragment() {
    /*
    * Used to handle and add widgets to widgetContainer.
    */
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: LauncherAppWidgetHost
    private lateinit var appWidgetContainer: LinearLayout

    /*
     * List containing widgets ID.
     */
    private lateinit var widgetsList: ArrayList<String>

    /*
     * View calling the context menu.
     */
    private var callingView: AppWidgetHostView? = null

    private var binding: FragmentWidgetListBinding? = null

    private var isFavouritesShowing: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWidgetListBinding.inflate(inflater, container, false)

        appWidgetManager = AppWidgetManager.getInstance(requireContext())
        appWidgetHost = LauncherAppWidgetHost(requireContext(), WIDGET_HOST_ID)

        widgetsList = PreferenceHelper.widgetList()
        return binding !!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Workaround to prevent widgets from being stuck (not updating).
        // https://github.com/Neamar/KISS/commit/3d5410307b8a8dc29b1fdc48d9f7c6ea1864dcd6
        if (Utils.atLeastOreo()) {
            appWidgetHost.stopListening()
        }
        PreferenceHelper.updateWidgets(widgetsList)

        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var scrollYPosition = 0

        appWidgetContainer = binding !!.widgetContainer
        val widgetScroller: NestedScrollView = binding !!.widgetScroller
        val addWidget: FloatingActionButton = binding !!.addWidget

        addWidget.backgroundTintList = ColorStateList.valueOf(PreferenceHelper.accent)

        if (widgetsList.isNotEmpty()) {
            PreferenceHelper.widgetList().forEachIndexed { index, widgets ->
                if (widgets.isNotEmpty()) {
                    val widgetIntent = Intent()
                    widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgets.toInt())

                    // Don't add ALL the widgets at once.
                    // TODO: Handle this a bit better, because not all devices are made equally.
                    Handler().postDelayed({
                        addWidget(widgetIntent, index, false)
                    }, 300)
                }
            }

            if (widgetsList.size > appWidgetContainer.childCount) {
                // This shouldn't happen, but will occur in a corrupted backup/restore.
                // We don't want to add or iterate over this corrupted list,
                // so it's better to clear and redo.
                widgetsList.clear()
                PreferenceHelper.updateWidgets(widgetsList)
            }
        }

        addWidget.setOnClickListener {
            // Don't pull the panel just yet.
            getLauncherActivity().requestPanelLock()

            Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetHost.allocateAppWidgetId())
            }.also {
                startActivityForResult(it, WIDGET_CONFIG_START_CODE)
            }
        }

        widgetScroller.setOnScrollChangeListener { _: NestedScrollView?, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->
            val scrollYDelta = scrollY - oldScrollY
            val bottomDelta: Int =
                widgetScroller.getChildAt(0).bottom + widgetScroller.paddingBottom - (widgetScroller.height + widgetScroller.scrollY)

            if (bottomDelta == 0) {
                if (! isFavouritesShowing) {
                    getLauncherActivity().showPinnedApps()
                    addWidget.hide()
                    isFavouritesShowing = true
                }
                scrollYPosition = 0
            } else if (scrollYPosition < - 48) {
                if (isFavouritesShowing) {
                    getLauncherActivity().hidePinnedApps()
                    addWidget.show()
                    isFavouritesShowing = false
                }
                scrollYPosition = 0
            }

            if (scrollYDelta < 0) {
                scrollYPosition += scrollYDelta
            }
        }
    }

    override fun isAcceptingSearch(): Boolean {
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val widgetId =
                data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, WIDGET_CONFIG_DEFAULT_CODE)
            val appWidgetInfo = appWidgetManager.getAppWidgetInfo(widgetId)
            if (requestCode != WIDGET_CONFIG_RETURN_CODE && appWidgetInfo.configure != null) {
                Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = appWidgetInfo.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }.also {
                    startActivityForResult(it, WIDGET_CONFIG_RETURN_CODE)
                }
            } else {
                addWidget(data, appWidgetContainer.childCount, true)
            }
        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            val widgetId =
                data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, WIDGET_CONFIG_DEFAULT_CODE)
            if (widgetId != WIDGET_CONFIG_DEFAULT_CODE) {
                appWidgetHost.deleteAppWidgetId(widgetId)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val index = appWidgetContainer.indexOfChild(callingView)
        return when (item.itemId) {
            0 -> {
                // Don't pull the panel just yet.
                getLauncherActivity().requestPanelLock()

                Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                    putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        appWidgetHost.allocateAppWidgetId()
                    )
                }.also {
                    startActivityForResult(it, WIDGET_CONFIG_START_CODE)
                }
                true
            }
            1 -> {
                callingView?.let { removeWidget(it, callingView !!.appWidgetId) }
                true
            }
            2 -> {
                swapWidget(index, index - 1)
                true
            }
            3 -> {
                swapWidget(index, index + 1)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    /**
     * Adds a widget to the desktop.
     *
     * @param data Intent used to receive the ID of the widget being added.
     */
    private fun addWidget(data: Intent, index: Int, newWidget: Boolean) {
        if (! isAdded || activity == null) {
            // Nope. Not doing anything.
            return
        }

        if (index > appWidgetContainer.childCount) {
            // This might be caused by a broken restore.
            // We don't want this to happen, so don't do anything.
            return
        }

        val widgetId =
            data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, WIDGET_CONFIG_DEFAULT_CODE)
        val appWidgetInfo = appWidgetManager.getAppWidgetInfo(widgetId)

        if (appWidgetInfo != null) {
            val appWidgetHostView = appWidgetHost.createView(
                requireActivity().applicationContext,
                widgetId, appWidgetInfo
            ).apply {
                // Notify widget of the available minimum space.
                minimumHeight = appWidgetInfo.minHeight
                setAppWidget(widgetId, appWidgetInfo)
                if (Utils.sdkIsAround(16)) {
                    updateAppWidgetSize(
                        null, appWidgetInfo.minWidth,
                        appWidgetInfo.minHeight, appWidgetInfo.minWidth, appWidgetInfo.minHeight
                    )
                }
            }

            // Add the widget.
            appWidgetContainer.addView(appWidgetHostView, index)

            // Immediately listens for the widget.
            appWidgetHost.startListening()
            addWidgetActionListener(index)
            registerForContextMenu(appWidgetContainer.getChildAt(index))
            if (newWidget) {
                // Update our list.
                widgetsList.add(widgetId.toString())

                // Apply preference changes.
                PreferenceHelper.updateWidgets(widgetsList)
            }
        }
    }

    /**
     * Removes widget from the desktop and resets the configuration
     * relating to widgets.
     */
    private fun removeWidget(view: View, id: Int) {
        unregisterForContextMenu(view)
        appWidgetContainer.removeView(view)

        // Remove the widget from the list.
        widgetsList.remove(id.toString())

        // Update the preference by having the new list on it.
        PreferenceHelper.updateWidgets(widgetsList)
    }

    private fun swapWidget(one: Int, two: Int) {
        val top = appWidgetContainer.getChildAt(one)
        val bottom = appWidgetContainer.getChildAt(two)

        // Swap the list and update preferences.
        Collections.swap(widgetsList, one, two)
        PreferenceHelper.updateWidgets(widgetsList)

        // Update our views.
        with(appWidgetContainer) {
            removeView(top)
            addView(top, two)
            removeView(bottom)
            addView(bottom, one)
        }

        addWidgetActionListener(one)
        addWidgetActionListener(two)
    }

    /**
     * Adds a long press action to widgets.
     * TODO: Remove this once we figure out ways to resize the widgets.
     */
    private fun addWidgetActionListener(index: Int) {
        appWidgetContainer.getChildAt(index)?.setOnLongClickListener { view ->
            // Set the calling view.
            callingView = view as AppWidgetHostView
            val popupMenu = PopupMenu(requireContext(), view)

            with(popupMenu.menu) {
                // Generate menu.
                // TODO: Maybe a more robust and automated way can be done for this.
                clear()
                add(1, 0, 100, getString(R.string.dialog_action_add))
                add(1, 1, 100, getString(R.string.action_remove_widget))
                add(1, 2, 100, getString(R.string.action_up_widget))
                add(1, 3, 100, getString(R.string.action_down_widget))

                // Move actions should only be added when there is more than one widget.
                getItem(2).isVisible = appWidgetContainer.childCount > 1 && index > 0
                getItem(3).isVisible = appWidgetContainer.childCount != index + 1
            }

            popupMenu.setOnMenuItemClickListener {
                onContextItemSelected(it)
            }

            popupMenu.show()
            true
        }
    }

    companion object {
        private const val WIDGET_CONFIG_START_CODE = 1
        private const val WIDGET_CONFIG_RETURN_CODE = 2
        private const val WIDGET_CONFIG_DEFAULT_CODE = - 1
        private const val WIDGET_HOST_ID = 314
    }
}