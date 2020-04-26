package mono.hg.views;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

    @Override
    public boolean requestChildRectangleOnScreen(RecyclerView parent, View child, Rect rect, boolean immediate) {
        return false;
    }
}
