package mono.hg.wrappers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;

@Keep
public class ObservantListBehavior extends CoordinatorLayout.Behavior<View> {

    public ObservantListBehavior() {
        super();
    }

    public ObservantListBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull View dependency) {
        float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight());

        child.animate().cancel();
        child.setTranslationY(translationY);
        child.setPadding(0, -Math.round(translationY), 0, 0);

        return true;
    }

    @Override
    public void onDependentViewRemoved(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull View dependency) {
        child.setPadding(0, 0, 0, 0);
        child.animate().translationY(0).start();
    }
}