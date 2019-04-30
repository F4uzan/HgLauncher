package mono.hg.views;

import android.content.Context;

import androidx.recyclerview.widget.LinearLayoutManager;

/**
 * A LinearLayoutManager class with scroll-blocking methods.
 */
public class TogglingLinearLayoutManager extends LinearLayoutManager {
    private static boolean mVerticalScrollEnabled = true;

    public TogglingLinearLayoutManager(Context context, int orientation, boolean stackFromEnd) {
        super(context, orientation, stackFromEnd);

        if (stackFromEnd) {
            setStackFromEnd(true);
            setReverseLayout(false);
        }
    }

    public void setVerticalScrollEnabled(boolean flag) {
        mVerticalScrollEnabled = flag;
    }

    @Override public boolean canScrollVertically() {
        return mVerticalScrollEnabled && super.canScrollVertically();
    }
}
