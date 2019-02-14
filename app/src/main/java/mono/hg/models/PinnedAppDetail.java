package mono.hg.models;

import android.graphics.drawable.Drawable;
import android.view.View;

import java.util.List;

import androidx.annotation.NonNull;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;
import mono.hg.R;

public class PinnedAppDetail extends AppDetail {

    public PinnedAppDetail(Drawable icon, @NonNull String packageName) {
        super(icon, null, packageName, false);
    }

    public PinnedAppDetail(String packageName) {
        super(packageName);
    }

    @Override public int getLayoutRes() {
        return R.layout.list_pinned_item;
    }

    @Override public PinnedAppDetail.ViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new ViewHolder(view, adapter);
    }

    @Override public void bindViewHolder(FlexibleAdapter<IFlexible> adapter,
            PinnedAppDetail.ViewHolder holder, int position,
            List<Object> payloads) {
        holder.icon.setImageDrawable(getIcon());
    }
}
