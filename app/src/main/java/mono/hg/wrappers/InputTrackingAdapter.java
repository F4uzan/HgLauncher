package mono.hg.wrappers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;

import java.util.Objects;

import mono.hg.Utils;

/**
 * A RecyclerView adapter that scrolls when receiving keyboard up/down press.
 *
 * Based on adapter code by zevektor/Vektor at https://github.com/zevektor/KeyboardRecyclerView
 */
public abstract class InputTrackingAdapter<V extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<V>{
    private Context mContext;
    private int mSelectedItem = 0;
    private RecyclerView mRecyclerView;

    public InputTrackingAdapter(Context context) {
        mContext = context;
    }

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
                            return tryMoveSelection(RecyclerView.FOCUS_DOWN);
                        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            return tryMoveSelection(RecyclerView.FOCUS_UP);
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

    public Context getContext() {
        return mContext;
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