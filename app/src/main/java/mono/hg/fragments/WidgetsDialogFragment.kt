package mono.hg.fragments

import android.app.Activity
import android.app.Dialog
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import mono.hg.R
import mono.hg.appwidget.LauncherAppWidgetHost
import mono.hg.databinding.FragmentWidgetsDialogBinding
import mono.hg.helpers.PreferenceHelper
import mono.hg.utils.Utils
import java.util.*

class WidgetsDialogFragment : DialogFragment() {
    /*
     * Used to handle and add widgets to widgetContainer.
     */
    private var appWidgetManager: AppWidgetManager? = null
    private var appWidgetHost: LauncherAppWidgetHost? = null
    private var appWidgetContainer: LinearLayout? = null

    /*
     * List containing widgets ID.
     */
    private var widgetsList: ArrayList<String?>? = null

    /*
     * View calling the context menu.
     */
    private var callingView: AppWidgetHostView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetManager = AppWidgetManager.getInstance(requireContext())
        appWidgetHost = LauncherAppWidgetHost(requireContext(), WIDGET_HOST_ID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragmentWidgetsDialogBinding.inflate(requireActivity().layoutInflater)
        val builder = AlertDialog.Builder(requireActivity(),
                R.style.WidgetDialogStyle)
        widgetsList = ArrayList(PreferenceHelper.widgetList)
        appWidgetContainer = binding.widgetContainer
        if (widgetsList!!.isNotEmpty()) {
            for (widgets in PreferenceHelper.widgetList) {
                val widgetIntent = Intent()
                widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgets.toInt())
                addWidget(widgetIntent, appWidgetContainer!!.childCount, false)
            }
        }
        builder.setView(binding.root)
        builder.setTitle(R.string.dialog_title_widgets)
        builder.setNegativeButton(R.string.dialog_action_close, null)
        builder.setPositiveButton(R.string.dialog_action_add, null)
        val widgetDialog = builder.create()
        if (widgetDialog.window != null) {
            widgetDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }
        widgetDialog.setOnShowListener {
            val button = widgetDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
                pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        appWidgetHost!!.allocateAppWidgetId())
                startActivityForResult(pickIntent, WIDGET_CONFIG_START_CODE)
            }
        }
        return widgetDialog
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost?.stopListening()
        PreferenceHelper.applyWidgetsUpdate()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val widgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    WIDGET_CONFIG_DEFAULT_CODE)
            val appWidgetInfo = appWidgetManager!!.getAppWidgetInfo(widgetId)
            if (requestCode != WIDGET_CONFIG_RETURN_CODE && appWidgetInfo.configure != null) {
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                intent.component = appWidgetInfo.configure
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                startActivityForResult(intent, WIDGET_CONFIG_RETURN_CODE)
            } else {
                addWidget(data, widgetsList!!.size, true)
            }
        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            val widgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    WIDGET_CONFIG_DEFAULT_CODE)
            if (widgetId != WIDGET_CONFIG_DEFAULT_CODE) {
                appWidgetHost!!.deleteAppWidgetId(widgetId)
            }
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        // Set the calling view.
        callingView = v as AppWidgetHostView
        val index = appWidgetContainer!!.indexOfChild(v)

        // Workaround for DialogFragment issue with context menu.
        // Taken from: https://stackoverflow.com/a/18853634
        val listener = MenuItem.OnMenuItemClickListener { item ->
            onContextItemSelected(item)
            true
        }

        // Generate menu.
        // TODO: Maybe a more robust and automated way can be done for this.
        menu.clear()
        menu.add(1, 0, 100, getString(R.string.action_remove_widget))
        menu.add(1, 1, 100, getString(R.string.action_up_widget))
        menu.add(1, 2, 100, getString(R.string.action_down_widget))
        menu.getItem(0).setOnMenuItemClickListener(listener)

        // Move actions should only be added when there is more than one widget.
        menu.getItem(1).isVisible = appWidgetContainer!!.childCount > 1 && index > 0
        menu.getItem(2).isVisible = appWidgetContainer!!.childCount != index + 1
        if (appWidgetContainer!!.childCount > 1) {
            if (index > 0) {
                menu.getItem(1).setOnMenuItemClickListener(listener)
            }
            if (index + 1 != appWidgetContainer!!.childCount) {
                menu.getItem(2).setOnMenuItemClickListener(listener)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val index = appWidgetContainer!!.indexOfChild(callingView)
        return when (item.itemId) {
            0 -> {
                removeWidget(callingView, callingView!!.appWidgetId)
                true
            }
            1 -> {
                swapWidget(index, index - 1)
                true
            }
            2 -> {
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
        val widgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                WIDGET_CONFIG_DEFAULT_CODE)
        val appWidgetInfo = appWidgetManager!!.getAppWidgetInfo(widgetId)
        val appWidgetHostView = appWidgetHost!!.createView(
                requireActivity().applicationContext,
                widgetId, appWidgetInfo)

        // Prevents crashing when the widget info can't be found.
        // https://github.com/Neamar/KISS/commit/f81ae32ef5ff5c8befe0888e6ff818a41d8dedb4
        if (appWidgetInfo == null) {
            removeWidget(appWidgetHostView, widgetId)
        } else {
            // Notify widget of the available minimum space.
            appWidgetHostView.minimumHeight = appWidgetInfo.minHeight
            appWidgetHostView.setAppWidget(widgetId, appWidgetInfo)
            if (Utils.sdkIsAround(16)) {
                appWidgetHostView.updateAppWidgetSize(null, appWidgetInfo.minWidth,
                        appWidgetInfo.minHeight, appWidgetInfo.minWidth, appWidgetInfo.minHeight)
            }

            // Remove existing widget if any and then add the new widget.
            appWidgetContainer!!.addView(appWidgetHostView, index)

            // Immediately listens for the widget.
            appWidgetHost!!.startListening()
            addWidgetActionListener(index)
            registerForContextMenu(appWidgetContainer!!.getChildAt(index))
            if (newWidget) {
                // Update our list.
                widgetsList!!.add(widgetId.toString())

                // Apply preference changes.
                PreferenceHelper.updateWidgets(widgetsList)
            }
        }
    }

    /**
     * Removes widget from the desktop and resets the configuration
     * relating to widgets.
     */
    private fun removeWidget(view: View?, id: Int) {
        unregisterForContextMenu(requireView())
        appWidgetContainer!!.removeView(view)

        // Remove the widget from the list.
        widgetsList!!.remove(id.toString())

        // Update the preference by having the new list on it.
        PreferenceHelper.updateWidgets(widgetsList)
    }

    private fun swapWidget(one: Int, two: Int) {
        val top = appWidgetContainer!!.getChildAt(one)
        val bottom = appWidgetContainer!!.getChildAt(two)

        // Swap the list and update preferences.
        Collections.swap(widgetsList, one, two)
        PreferenceHelper.updateWidgets(widgetsList)

        // Update our views.
        appWidgetContainer!!.removeView(top)
        appWidgetContainer!!.addView(top, two)
        appWidgetContainer!!.removeView(bottom)
        appWidgetContainer!!.addView(bottom, one)
    }

    /**
     * Adds a long press action to widgets.
     * TODO: Remove this once we figure out ways to resize the widgets.
     */
    private fun addWidgetActionListener(index: Int) {
        appWidgetContainer!!.getChildAt(index).setOnLongClickListener { view ->
            view.showContextMenu()
            true
        }
    }

    companion object {
        private const val WIDGET_CONFIG_START_CODE = 1
        private const val WIDGET_CONFIG_RETURN_CODE = 2
        private const val WIDGET_CONFIG_DEFAULT_CODE = -1
        private const val WIDGET_HOST_ID = 314
    }
}