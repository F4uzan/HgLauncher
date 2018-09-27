package mono.hg.views;

import android.content.Context;
import android.support.v4.widget.CircularProgressDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ProgressBar;

import mono.hg.R;

public class IndeterminateMaterialProgressBar extends ProgressBar {
    private static final int WIDTH_DP = 6;

    public IndeterminateMaterialProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        final float screenDensity = metrics.density;

        CircularProgressDrawable drawable = new CircularProgressDrawable(context);
        drawable.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        drawable.setStrokeWidth(WIDTH_DP * screenDensity);
        setIndeterminateDrawable(drawable);
    }
}
