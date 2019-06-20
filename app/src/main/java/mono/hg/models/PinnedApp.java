package mono.hg.models;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;
import mono.hg.R;
import mono.hg.utils.AppUtils;

public class PinnedApp extends App {

    public PinnedApp(Drawable icon, @NonNull String packageName, long user) {
        super(null, packageName, false, user);
        setIcon(icon);
        setUserPackageName(AppUtils.appendUser(user, packageName));
    }

    public PinnedApp(String packageName, long user) {
        super(packageName, user);
        setUserPackageName(AppUtils.appendUser(user, packageName));
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
