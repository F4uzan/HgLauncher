package mono.hg.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import mono.hg.AppDetail;
import mono.hg.Utils;

public class AppAdapter extends FlexibleAdapter<AppDetail> {
    private List<AppDetail> apps;
    private int mSelectedItem = 0;
    private RecyclerView mRecyclerView;

    public AppAdapter(List<AppDetail> apps) {
        super(apps);
        this.apps = apps;
    }

    @Override
    public String onCreateBubbleText(int position) {
        return apps.get(position).getAppName().substring(0, 1).toUpperCase();
    }

    /**
     * Keyboard navigation code is taken from the adapter code by zevektor/Vektor (https://github.com/zevektor/KeyboardRecyclerView)
     */
    @Override
    public void onAttachedToRecyclerView(@NonNull final RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        mRecyclerView = recyclerView;

        // Handle key up and key down and attempt to move selection.
        recyclerView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // Return false if scrolled to the bounds and allow focus to move off the list.
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (isConfirmButton(event)) {
                        if ((event.getFlags() & KeyEvent.FLAG_LONG_PRESS) == KeyEvent.FLAG_LONG_PRESS) {
                            Utils.requireNonNull(mRecyclerView.findViewHolderForAdapterPosition(mSelectedItem)).itemView.performLongClick();
                        } else {
                            event.startTracking();
                        }
                        return true;
                    } else {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            return tryMoveSelection(1);
                        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            return tryMoveSelection(-1);
                        }
                    }
                } else if (event.getAction() == KeyEvent.ACTION_UP && isConfirmButton(event)
                        && ((event.getFlags() & KeyEvent.FLAG_LONG_PRESS) != KeyEvent.FLAG_LONG_PRESS)
                        && mSelectedItem != -1) {
                    Utils.requireNonNull(mRecyclerView.findViewHolderForAdapterPosition(mSelectedItem)).itemView.performClick();
                    return true;
                }
                return false;
            }
        });
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

    private static boolean isConfirmButton(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_ENTER:
                return true;
            default:
                return false;
        }
    }
}
