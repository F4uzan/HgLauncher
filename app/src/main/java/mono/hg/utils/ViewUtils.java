package mono.hg.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.view.MenuItem;
import android.view.View;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import mono.hg.R;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.models.App;

public class ViewUtils {

    /**
     * Fetch statusbar height from system's dimension.
     *
     * @return int Size of the statusbar. Returns the fallback value of 24dp if the
     * associated dimen value cannot be found.
     */
    public static int getStatusBarHeight() {
        int idStatusBarHeight = Resources.getSystem()
                                         .getIdentifier("status_bar_height", "dimen", "android");
        if (idStatusBarHeight > 0) {
            return Resources.getSystem().getDimensionPixelSize(idStatusBarHeight);
        } else {
            // Return fallback size if we can't get the value from the system.
            return Resources.getSystem().getDimensionPixelSize(R.dimen.status_bar_height_fallback);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static int setWindowbarMode(String mode) {
        int baseLayout;
        if (Utils.sdkIsAround(19)) {
            baseLayout = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        } else {
            baseLayout = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        }
        int noStatusLayout = baseLayout
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        int noNavLayout = baseLayout
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

        switch (mode) {
            case "status":
                return noStatusLayout;
            case "nav":
                return noNavLayout;
            case "both":
                return noStatusLayout | noNavLayout;
            case "none":
            default:
                return View.SYSTEM_UI_LAYOUT_FLAGS;
        }
    }

    /**
     * Checks if a SlidingUpPanelLayout is visible within view.
     *
     * @param panel SlidingUpPanelLayout whose visibility is to be checked.
     *
     * @return true if panel is neither collapsed, dragging, or anchored.
     */
    public static boolean isPanelVisible(SlidingUpPanelLayout panel) {
        return panel.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED
                || panel.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED;
    }

    /**
     * Launches an app based on RecyclerView scroll state.
     *
     * @param activity     The activity for context reference.
     * @param recyclerView The RecyclerView itself.
     * @param adapter      A FlexibleAdapter with App items.
     */
    public static void keyboardLaunchApp(Activity activity, RecyclerView recyclerView, FlexibleAdapter<App> adapter) {
        if (!recyclerView.canScrollVertically(RecyclerView.FOCUS_UP)
                && !recyclerView.canScrollVertically(RecyclerView.FOCUS_DOWN)) {
            AppUtils.launchApp(activity, Utils.requireNonNull(
                    adapter.getItem(adapter.getItemCount() - 1))
                                              .getPackageName());
        } else {
            AppUtils.launchApp(activity, Utils.requireNonNull(
                    adapter.getItem(0)).getPackageName());
        }
    }

    /**
     * Sets initial fragment. This fragment is not added to the backstack.
     *
     * @param activity The activity where the fragment is being contained.
     * @param fragment The fragment to use.
     */
    public static void setFragment(AppCompatActivity activity, Fragment fragment) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        fragmentManager.beginTransaction()
                       .replace(R.id.fragment_container, fragment)
                       .commit();
    }

    /**
     * Replace existing fragment with another. This adds the fragment to the back stack.
     *
     * @param activity The activity where the fragment is being contained.
     * @param fragment The fragment to use.
     * @param tag      The tag used for the fragment.
     */
    public static void replaceFragment(AppCompatActivity activity, Fragment fragment, String tag) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        fragmentManager.beginTransaction()
                       .replace(R.id.fragment_container, fragment)
                       .addToBackStack(tag)
                       .commit();
    }

    /**
     * Creates a PopupMenu containing available search provider.
     *
     * @param activity  Activity where the PopupMenu resides.
     * @param popupMenu The PopupMenu to populate and show.
     * @param query     Search query to launch when a provider is selected.
     */
    public static void createSearchMenu(final AppCompatActivity activity, PopupMenu popupMenu, final String query) {

        for (Map.Entry<String, String> provider : PreferenceHelper.getProviderList().entrySet()) {
            popupMenu.getMenu().add(provider.getKey());
        }

        popupMenu.setOnMenuItemClickListener(
                new PopupMenu.OnMenuItemClickListener() {
                    @Override public boolean onMenuItemClick(MenuItem menuItem) {
                        Utils.doWebSearch(activity,
                                PreferenceHelper.getProvider(menuItem.getTitle().toString()),
                                query);
                        return true;
                    }
                });
        popupMenu.show();
    }
}
