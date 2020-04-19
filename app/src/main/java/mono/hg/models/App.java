package mono.hg.models;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFilterable;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;
import mono.hg.R;
import mono.hg.helpers.KissFuzzySearch;
import mono.hg.helpers.PreferenceHelper;

public class App extends AbstractFlexibleItem<App.ViewHolder>
        implements IFilterable<String> {
    private int HINT_MATCH_SCORE = 30;
    private int NAME_MATCH_SCORE = 25;

    private String appName, packageName, userPackageName, hintName;
    private Boolean isAppHidden;
    private Drawable icon;
    private long user;

    public App(String appName, @NonNull String packageName, Boolean isAppHidden, long user) {
        this.packageName = packageName;
        this.appName = appName;
        this.isAppHidden = isAppHidden;
        this.user = user;
    }

    public App(@NonNull String packageName, long user) {
        this.packageName = packageName;
        this.isAppHidden = false;
        this.user = user;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getUserPackageName() {
        return userPackageName;
    }

    public void setUserPackageName(String userPackageName) {
        this.userPackageName = userPackageName;
    }

    public String getHintName() {
        return hintName;
    }

    public boolean hasHintName() {
        return hintName != null;
    }

    public void setHintName(String newName) {
        hintName = newName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public boolean isAppHidden() {
        return isAppHidden;
    }

    public void setAppHidden(Boolean hidden) {
        isAppHidden = hidden;
    }

    public long getUser() {
        return user;
    }

    public void setHintMatchScore(int newScore) {
        HINT_MATCH_SCORE = newScore;
    }

    public void setNameMatchScore(int newScore) {
        NAME_MATCH_SCORE = newScore;
    }

    public boolean equals(Object object) {
        if (getClass() != object.getClass()) {
            return false;
        }

        App alt = (App) object;
        return this == object || getUserPackageName().equals(alt.getUserPackageName());
    }

    @Override public int hashCode() {
        return userPackageName != null ? userPackageName.hashCode() : packageName.hashCode();
    }

    @Override public int getLayoutRes() {
        if (PreferenceHelper.useGrid()) {
            return R.layout.grid_generic_item;
        } else {
            return R.layout.list_generic_item;
        }
    }

    @Override public App.ViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new ViewHolder(view, adapter);
    }

    @Override public void bindViewHolder(FlexibleAdapter<IFlexible> adapter,
            App.ViewHolder holder, int position,
            List<Object> payloads) {
        holder.name.setText(getAppName());
        holder.icon.setImageDrawable(getIcon());
    }

    @Override public boolean filter(String constraint) {
        int fuzzyScore = 0;

        // See if we can match by hint names.
        if (hasHintName()) {
            fuzzyScore = KissFuzzySearch.doFuzzy(getHintName(), constraint);
        }

        // Is the hint name strong enough?
        if (fuzzyScore >= HINT_MATCH_SCORE) {
            return true;
        } else {
            // Fall back to app name matching if it isn't.
            fuzzyScore = KissFuzzySearch.doFuzzy(appName, constraint);
            return fuzzyScore >= NAME_MATCH_SCORE;
        }
    }

    protected static class ViewHolder extends FlexibleViewHolder {
        protected TextView name;
        protected ImageView icon;

        ViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
            super(view, adapter);
            name = view.findViewById(R.id.item_name);
            icon = view.findViewById(R.id.item_icon);
        }
    }
}
