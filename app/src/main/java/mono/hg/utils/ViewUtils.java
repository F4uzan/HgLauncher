package mono.hg.utils;

import android.content.res.Resources;
import android.view.ViewTreeObserver;

import com.google.android.material.snackbar.Snackbar;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import mono.hg.R;

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
}
