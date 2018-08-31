package f4.hubby.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.List;

import f4.hubby.AppDetail;
import f4.hubby.R;
import f4.hubby.helpers.KissFuzzySearch;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> implements Filterable,
        FastScrollRecyclerView.SectionedAdapter {
    private List<AppDetail> apps;
    private AppFilter filter;
    private Boolean updateFilter = false;

    public AppAdapter(List<AppDetail> apps) {
        this.apps = apps;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        AppDetail app;
        TextView name;
        ImageView icon;

        void setItem(AppDetail app) {
            this.app = app;
            name.setText(app.getAppName());
            icon.setImageDrawable(app.getIcon());
        }

        ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.item_app_name);
            icon = view.findViewById(R.id.item_app_icon);
        }
    }

    @NonNull
    @Override
    public AppAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_list, parent, false);
        return new AppAdapter.ViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull AppAdapter.ViewHolder holder, int position) {
        final AppDetail fetchItem = apps.get(position);
        holder.setItem(fetchItem);
    }

    @Override
    public Filter getFilter() {
        if (filter == null || updateFilter) {
            filter = new AppFilter(apps);
        }
        return filter;
    }

    public Boolean shouldUpdateFilter() {
        return updateFilter;
    }

    public void setUpdateFilter(Boolean shouldUpdate) {
        this.updateFilter = shouldUpdate;
    }

    @Override
    public int getItemViewType(int pos) {
        return pos;
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    @Override
    public long getItemId(int position) {
        return apps.get(position).hashCode();
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        return apps.get(position).getAppName().substring(0, 1).toUpperCase();
    }

    // Basic filter class
    private class AppFilter extends Filter {
        private final ArrayList<AppDetail> originalList;
        private final ArrayList<AppDetail> filteredList;

        private AppFilter(List<AppDetail> originalList) {
            super();
            this.originalList = new ArrayList<>(originalList);
            this.filteredList = new ArrayList<>();
        }

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            filteredList.clear();
            final FilterResults results = new FilterResults();

            if (charSequence == null || charSequence.length() == 0 || updateFilter) {
                filteredList.addAll(originalList);
            } else {
                final String filterPattern = charSequence.toString();
                for (AppDetail item : originalList) {
                    // Do a fuzzy comparison instead of checking for absolute match.
                    if (KissFuzzySearch.doFuzzy(item.getAppName(), filterPattern) >= 30) {
                        filteredList.add(item);
                    }
                }
            }

            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            if (filterResults.values != null) {
                apps.clear();
                apps.addAll((ArrayList<AppDetail>) filterResults.values);
                notifyDataSetChanged();
            }
        }
    }
}
