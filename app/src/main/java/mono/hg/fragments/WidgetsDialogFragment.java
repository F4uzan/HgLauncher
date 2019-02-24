package mono.hg.fragments;

import android.app.Dialog;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.DialogFragment;
import mono.hg.R;
import mono.hg.appwidget.LauncherAppWidgetHost;
import mono.hg.appwidget.LauncherAppWidgetHostView;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.utils.Utils;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class WidgetsDialogFragment extends DialogFragment {

    private static int WIDGET_CONFIG_START_CODE = 1;
    private static int WIDGET_CONFIG_RETURN_CODE = 2;
    private static int WIDGET_CONFIG_DEFAULT_CODE = -1;
    private static int WIDGET_HOST_ID = 314;

    /*
     * Used to handle and add widgets to widgetContainer.
     */
    private AppWidgetManager appWidgetManager;
    private LauncherAppWidgetHost appWidgetHost;
    private FrameLayout appWidgetContainer;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appWidgetManager = AppWidgetManager.getInstance(requireActivity().getApplicationContext());
        appWidgetHost = new LauncherAppWidgetHost(requireActivity().getApplicationContext(), WIDGET_HOST_ID);
    }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        View view = View.inflate(requireContext(), R.layout.fragment_widgets_dialog, null);

        appWidgetContainer = view.findViewById(R.id.widget_container);

        if (PreferenceHelper.hasWidget()) {
            Intent widgetIntent = new Intent();
            widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    PreferenceHelper.getPreference().getInt("widget_id", WIDGET_CONFIG_DEFAULT_CODE));
            addWidget(widgetIntent);
        }

        builder.setView(view);
        builder.setTitle(R.string.dialogue_title_widgets);
        builder.setNegativeButton(R.string.dialogue_action_close, null);
        builder.setPositiveButton(R.string.dialogue_action_add, null);

        final AlertDialog widgetDialog = builder.create();

        if (widgetDialog.getWindow() != null) {
            widgetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        widgetDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface dialogInterface) {

                Button button = widgetDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
                        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,  appWidgetHost.allocateAppWidgetId());
                        startActivityForResult(pickIntent, WIDGET_CONFIG_START_CODE);
                    }
                });
            }
        });

        return widgetDialog;
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            int widgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    WIDGET_CONFIG_DEFAULT_CODE);
            AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(widgetId);

            if (requestCode != WIDGET_CONFIG_RETURN_CODE && appWidgetInfo.configure != null) {
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                intent.setComponent(appWidgetInfo.configure);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                startActivityForResult(intent, WIDGET_CONFIG_RETURN_CODE);
            } else {
                addWidget(data);
            }
        } else if (resultCode == RESULT_CANCELED && data != null) {
            int widgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, WIDGET_CONFIG_DEFAULT_CODE);
            if (widgetId != WIDGET_CONFIG_DEFAULT_CODE) {
                appWidgetHost.deleteAppWidgetId(widgetId);
            }
        }
    }

    @Override public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View view, ContextMenu.ContextMenuInfo menuInfo) {
        requireActivity().getMenuInflater().inflate(R.menu.menu_fragment_dialog, menu);
    }

    @Override public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_remove_widget:
                removeWidget();
                PreferenceHelper.fetchPreference();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Adds a widget to the desktop.
     *
     * @param data Intent used to receive the ID of the widget being added.
     */
    private void addWidget(Intent data) {
        int widgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, WIDGET_CONFIG_DEFAULT_CODE);
        AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(widgetId);
        AppWidgetHostView appWidgetHostView = appWidgetHost.createView(requireActivity().getApplicationContext(),
                widgetId, appWidgetInfo);

        // Prevents crashing when the widget info can't be found.
        // https://github.com/Neamar/KISS/commit/f81ae32ef5ff5c8befe0888e6ff818a41d8dedb4
        if (appWidgetInfo == null) {
            removeWidget();
        } else {
            // Notify widget of the available minimum space.
            appWidgetHostView.setMinimumHeight(appWidgetInfo.minHeight);
            appWidgetHostView.setAppWidget(widgetId, appWidgetInfo);
            if (Utils.sdkIsAround(16)) {
                appWidgetHostView.updateAppWidgetSize(null, appWidgetInfo.minWidth,
                        appWidgetInfo.minHeight, appWidgetInfo.minWidth, appWidgetInfo.minHeight);
            }

            // Remove existing widget if any and then add the new widget.
            appWidgetContainer.removeAllViews();
            appWidgetContainer.addView(appWidgetHostView, 0);

            // Immediately listens for the widget.
            appWidgetHost.startListening();
            addWidgetActionListener();

            // Apply preference changes.
            PreferenceHelper.getEditor()
                            .putInt("widget_id", widgetId)
                            .putBoolean("has_widget", true)
                            .apply();
        }
    }

    /**
     * Removes widget from the desktop and resets the configuration
     * relating to widgets.
     */
    private void removeWidget() {
        LauncherAppWidgetHostView widget = (LauncherAppWidgetHostView) appWidgetContainer.getChildAt(0);
        appWidgetContainer.removeView(widget);
        PreferenceHelper.getEditor().remove("widget_id").putBoolean("has_widget", false).apply();
    }

    private void addWidgetActionListener() {
        appWidgetContainer.getChildAt(0).setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View view) {
                PopupMenu popupMenu = new PopupMenu(requireActivity(), view, Gravity.END, 0, R.style.WidgetPopup);
                popupMenu.getMenuInflater().inflate(R.menu.menu_fragment_dialog, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override public boolean onMenuItemClick(MenuItem item) {
                        onContextItemSelected(item);
                        return false;
                    }
                });
                popupMenu.show();
                return true;
            }
        });
    }
}
