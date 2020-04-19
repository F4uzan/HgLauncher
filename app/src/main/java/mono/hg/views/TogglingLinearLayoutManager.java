package mono.hg.views;

import android.content.Context;

import androidx.recyclerview.widget.LinearLayoutManager;

/**
 * A LinearLayoutManager class with scroll-blocking methods.
 */
public class TogglingLinearLayoutManager extends LinearLayoutManager {
    public TogglingLinearLayoutManager(Context context, int orientation, boolean stackFromEnd) {
        super(context, orientation, stackFromEnd);

        if (stackFromEnd) {
            setStackFromEnd(true);
            setReverseLayout(false);
        }
    }
}
