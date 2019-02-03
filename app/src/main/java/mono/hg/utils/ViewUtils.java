package mono.hg.utils;

import android.content.res.Resources;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.PopupMenu;

import com.google.android.material.snackbar.Snackbar;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import mono.hg.R;
import mono.hg.helpers.PreferenceHelper;

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

    /**
     * Disable snackbar swipe behaviour.
     *
     * @param snackbar Snackbar whose behaviour is to be modified.
     */
    public static void disableSnackbarSwipe(final Snackbar snackbar) {
        snackbar.getView()
                .getViewTreeObserver()
                .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        snackbar.getView().getViewTreeObserver().removeOnPreDrawListener(this);
                        ((CoordinatorLayout.LayoutParams) snackbar.getView()
                                                                  .getLayoutParams()).setBehavior(
                                null);
                        return true;
                    }
                });
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
                       .add(R.id.fragment_container, fragment)
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
        popupMenu.getMenuInflater().inflate(R.menu.menu_search, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(
                new PopupMenu.OnMenuItemClickListener() {
                    @Override public boolean onMenuItemClick(MenuItem menuItem) {
                        String provider_id = "";
                        switch (menuItem.getItemId()) {
                            case R.id.provider_google:
                                provider_id = "google";
                                break;
                            case R.id.provider_ddg:
                                provider_id = "ddg";
                                break;
                            case R.id.provider_searx:
                                provider_id = "searx";
                                break;
                            default:
                                // No-op.
                        }
                        Utils.openLink(activity, PreferenceHelper.getSearchProvider(
                                provider_id) + query);
                        return true;
                    }
                });
        popupMenu.show();
    }
}
