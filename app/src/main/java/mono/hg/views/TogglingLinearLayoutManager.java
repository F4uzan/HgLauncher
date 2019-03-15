package mono.hg.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.LinearLayoutManager;

/**
 * A LinearLayoutManager class with scroll-blocking methods.
 */
public class TogglingLinearLayoutManager extends LinearLayoutManager {
    private static boolean mVerticalScrollEnabled = true;
    private static boolean mHorizontalScrollEnabled = true;

    public TogglingLinearLayoutManager(Context context) {
        super(context);
    }

    public TogglingLinearLayoutManager(Context context, int orientation, boolean stackFromEnd) {
        super(context, orientation, stackFromEnd);

        if (stackFromEnd) {
            setStackFromEnd(true);
            setReverseLayout(false);
        }
    }

    public TogglingLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setVerticalScrollEnabled(boolean flag) {
        mVerticalScrollEnabled = flag;
    }

    public void setHorizontalScrollEnabled(boolean flag) {
        mHorizontalScrollEnabled = flag;
    }

    public boolean verticalScrollEnabled() {
        return mVerticalScrollEnabled;
    }

    public boolean horizontalScrollEnabled() {
        return mHorizontalScrollEnabled;
    }

    @Override public boolean canScrollVertically() {
        return mVerticalScrollEnabled && super.canScrollVertically();
    }

    @Override public boolean canScrollHorizontally() {
        return mHorizontalScrollEnabled && super.canScrollHorizontally();
    }
}
