package f4.hubby;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import me.xdrop.fuzzywuzzy.FuzzySearch;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> implements Filterable {
    private List<AppDetail> apps;
    private AppFilter filter;

    AppAdapter(List<AppDetail> apps) {
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
        if (filter == null) {
            filter = new AppFilter(this, apps);
        }
        return filter;
    }

    @Override
    public int getItemViewType(int pos) {
        return pos;
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    // Basic filter class
    private class AppFilter extends Filter {
        private final AppAdapter adapter;
        private final ArrayList<AppDetail> originalList;
        private final ArrayList<AppDetail> filteredList;

        private AppFilter(AppAdapter adapter, List<AppDetail> originalList) {
            super();
            this.adapter = adapter;
            this.originalList = new ArrayList<>(originalList);
            this.filteredList = new ArrayList<>();
        }

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            filteredList.clear();
            final FilterResults results = new FilterResults();

            if (charSequence.length() == 0) {
                filteredList.addAll(originalList);
            } else {
                final String filterPattern = charSequence.toString().toLowerCase().trim();
                for (AppDetail item : originalList) {
                    // Do a fuzzy comparison instead of checking for absolute match.
                    if (FuzzySearch.weightedRatio(item.getAppName().toLowerCase(), filterPattern) >= 65) {
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
            apps.clear();
            apps.addAll((ArrayList<AppDetail>) filterResults.values);
            adapter.notifyDataSetChanged();
        }
    }
}
