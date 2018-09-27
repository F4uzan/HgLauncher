package mono.hg.models;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFilterable;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;
import mono.hg.R;
import mono.hg.helpers.KissFuzzySearch;

public class AppDetail extends AbstractFlexibleItem<AppDetail.ViewHolder> implements IFilterable<String> {
    private String appName, packageName;
    private Boolean isAppHidden;
    private Drawable icon;

    public AppDetail(Drawable icon, String appName, @NonNull String packageName, Boolean isAppHidden) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
        this.isAppHidden = isAppHidden;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isAppHidden() {
        return isAppHidden;
    }

    public void setAppHidden(Boolean hidden) {
        isAppHidden = hidden;
    }

    public boolean equals(Object object) {
        AppDetail alt = (AppDetail) object;
        return this == object || getClass() != object.getClass() || getPackageName().equals(
                alt.getPackageName());
    }

    @Override public int hashCode() {
        return packageName.hashCode();
    }

    @Override public int getLayoutRes() {
        return R.layout.app_list;
    }

    @Override public AppDetail.ViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new ViewHolder(view, adapter);
    }

    @Override public void bindViewHolder(FlexibleAdapter<IFlexible> adapter,
            AppDetail.ViewHolder holder, int position,
            List<Object> payloads) {
        holder.name.setText(getAppName());
        holder.icon.setImageDrawable(getIcon());
    }

    @Override public boolean filter(String constraint) {
        int fuzzyScore = KissFuzzySearch.doFuzzy(getAppName(), constraint);
        return fuzzyScore >= 30;
    }

    protected class ViewHolder extends FlexibleViewHolder {
        protected TextView name;
        protected ImageView icon;

        ViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            name = view.findViewById(R.id.item_app_name);
            icon = view.findViewById(R.id.item_app_icon);
        }
    }
}
