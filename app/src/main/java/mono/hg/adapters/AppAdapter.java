package mono.hg.adapters;

import android.view.KeyEvent;
import android.view.View;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import mono.hg.models.AppDetail;
import mono.hg.utils.Utils;

public class AppAdapter extends FlexibleAdapter<AppDetail>
        implements FastScrollRecyclerView.SectionedAdapter {
    private int mSelectedItem = 0;
    private RecyclerView mRecyclerView;
    private boolean finishedLoading = false;

    public AppAdapter(List<AppDetail> apps) {
        super(apps);
    }

    private static boolean isConfirmButton(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_ENTER:
                return true;
            default:
                return false;
        }
    }

    private static boolean isUp(int keycode) {
        switch (keycode) {
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP:
            case KeyEvent.KEYCODE_DPAD_UP:
                return true;
            default:
                return false;
        }
    }

    private static boolean isDown(int keycode) {
        switch (keycode) {
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return true;
            default:
                return false;
        }
    }

    public void resetFilter() {
        setFilter("");
        filterItems();
    }

    @Override public void filterItems() {
        if (hasFinishedLoading()) {
            super.filterItems();
        }
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        return Utils.requireNonNull(getItem(position)).getAppName().substring(0, 1).toUpperCase();
    }

    /**
     * Keyboard navigation code is taken from the adapter code by zevektor/Vektor (https://github.com/zevektor/KeyboardRecyclerView)
     */
    @Override public void onAttachedToRecyclerView(@NonNull final RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        mRecyclerView = recyclerView;

        // Handle key up and key down and attempt to move selection.
        // This is unnecessary for newer API.
        if (Utils.sdkIsBelow(21)) {
            recyclerView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    // Return false if scrolled to the bounds and allow focus to move off the list.
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (isConfirmButton(event)) {
                            if ((event.getFlags() & KeyEvent.FLAG_LONG_PRESS) == KeyEvent.FLAG_LONG_PRESS) {
                                Utils.requireNonNull(
                                        mRecyclerView.findViewHolderForAdapterPosition(
                                                mSelectedItem))
                                        .itemView.performLongClick();
                            } else {
                                event.startTracking();
                            }
                            return true;
                        } else {
                            if (isDown(keyCode)) {
                                return tryMoveSelection(1);
                            } else if (isUp(keyCode)) {
                                return tryMoveSelection(-1);
                            }
                        }
                    } else if (event.getAction() == KeyEvent.ACTION_UP && isConfirmButton(event)
                            && ((event.getFlags() & KeyEvent.FLAG_LONG_PRESS) != KeyEvent.FLAG_LONG_PRESS)
                            && mSelectedItem != -1) {
                        Utils.requireNonNull(
                                mRecyclerView.findViewHolderForAdapterPosition(mSelectedItem))
                                .itemView.performClick();
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    /**
     * Checks if the adapter has finished loading its data.
     * The adapter itself does not set this flag; and the flag
     * does not actually do anything internally.
     *
     * @return true if the adapter has been notified that it has finished loading.
     */
    public boolean hasFinishedLoading() {
        return finishedLoading;
    }

    /**
     * Notifies the adapter that its data has been loaded.
     *
     * @param finished the new state of the adapter.
     */
    public void finishedLoading(boolean finished) {
        finishedLoading = finished;
    }

    private boolean tryMoveSelection(int direction) {
        int nextSelectItem = mSelectedItem + direction;

        // If still within valid bounds, move the selection, notify to redraw, and scroll.
        if (nextSelectItem >= 0 && nextSelectItem < getItemCount()) {
            notifyItemChanged(mSelectedItem);
            mSelectedItem = nextSelectItem;
            notifyItemChanged(mSelectedItem);
            mRecyclerView.smoothScrollToPosition(mSelectedItem);
            return true;
        }
        return false;
    }
}
