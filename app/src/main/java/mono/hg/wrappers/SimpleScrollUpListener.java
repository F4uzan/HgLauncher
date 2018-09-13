package mono.hg.wrappers;

import android.support.v7.widget.RecyclerView;

public abstract class SimpleScrollUpListener extends RecyclerView.OnScrollListener {

    private int HIDE_THRESHOLD;
    private int mScrolledDistance = 0;

    protected SimpleScrollUpListener(int threshold) {
        this.HIDE_THRESHOLD = threshold;
    }

    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

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

    public abstract void onEnd();

    public abstract void onScrollUp();
}