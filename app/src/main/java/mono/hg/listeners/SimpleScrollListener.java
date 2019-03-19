package mono.hg.listeners;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class SimpleScrollListener extends RecyclerView.OnScrollListener {

    private int HIDE_THRESHOLD;
    private int mScrolledDistance = 0;

    protected SimpleScrollListener(int threshold) {
        this.HIDE_THRESHOLD = threshold;
    }

    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        onScroll();

        if (!recyclerView.canScrollVertically(RecyclerView.FOCUS_DOWN)) {
            onEnd();
            resetScrollDistance();
        } else {
            if (mScrolledDistance < -HIDE_THRESHOLD) {
                onScrollUp();
                resetScrollDistance();
            }
        }

        if ((dy < 0)) {
            mScrolledDistance += dy;
        }
    }

    private void resetScrollDistance() {
        mScrolledDistance = 0;
    }

    public void onEnd() {
        // No-op. Overridden when needed.
    }

    public void onScrollUp() {
        // No-op. Overridden when needed.
    }

    public abstract void onScroll();
}