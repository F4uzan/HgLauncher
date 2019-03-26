package mono.hg.models;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFilterable;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;
import mono.hg.R;
import mono.hg.helpers.KissFuzzySearch;

public class App extends AbstractFlexibleItem<App.ViewHolder>
        implements IFilterable<String> {
    private int HINT_MATCH_SCORE = 30;
    private int NAME_MATCH_SCORE = 25;

    private String appName, packageName, hintName;
    private Boolean isAppHidden;
    private Drawable icon;

    public App(Drawable icon, String appName, @NonNull String packageName, String hintName, Boolean isAppHidden) {
        this.packageName = packageName;
        this.appName = appName;
        this.hintName = hintName;
        this.icon = icon;
        this.isAppHidden = isAppHidden;
    }

    public App(@NonNull String packageName) {
        this.packageName = packageName;
        this.isAppHidden = false;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getHintName() {
        return hintName;
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

    public boolean hasHintName() {
        return hintName != null;
    }

    public void setHintMatchScore(int newScore) {
        HINT_MATCH_SCORE = newScore;
    }

    public void setNameMatchScore(int newScore) {
        NAME_MATCH_SCORE = newScore;
    }

    public boolean equals(Object object) {
        App alt = (App) object;
        return this == object || getClass() != object.getClass() || getPackageName().equals(
                alt.getPackageName());
    }

    @Override public int hashCode() {
        return packageName.hashCode();
    }

    @Override public int getLayoutRes() {
        return R.layout.list_generic_item;
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

        ViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            name = view.findViewById(R.id.item_name);
            icon = view.findViewById(R.id.item_icon);
        }
    }
}
