package mono.hg.models;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;
import mono.hg.R;

public class PinnedApp extends App {

    public PinnedApp(Drawable icon, @NonNull String packageName) {
        super(icon, null, packageName, null, false);
    }

    public PinnedApp(String packageName) {
        super(packageName);
    }

    @Override public int getLayoutRes() {
        return R.layout.list_pinned_item;
    }

    @Override public PinnedApp.ViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new ViewHolder(view, adapter);
    }

    @Override public void bindViewHolder(FlexibleAdapter<IFlexible> adapter,
            PinnedApp.ViewHolder holder, int position,
            List<Object> payloads) {
        holder.icon.setImageDrawable(getIcon());
    }
}
