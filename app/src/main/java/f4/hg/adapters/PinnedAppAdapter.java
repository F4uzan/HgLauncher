package f4.hg.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

import f4.hg.AppDetail;
import f4.hubby.R;

public class PinnedAppAdapter extends RecyclerView.Adapter<PinnedAppAdapter.ViewHolder> {
    private List<AppDetail> apps;

    public PinnedAppAdapter(List<AppDetail> apps) {
        this.apps = apps;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        AppDetail app;
        ImageView icon;

        void setItem(AppDetail app) {
            this.app = app;
            icon.setImageDrawable(app.getIcon());
        }

        ViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.item_app_icon);
        }
    }

    @NonNull
    @Override
    public PinnedAppAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.pinned_app_list, parent, false);
        return new PinnedAppAdapter.ViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull PinnedAppAdapter.ViewHolder holder, int position) {
        final AppDetail fetchItem = apps.get(position);
        holder.setItem(fetchItem);
    }

    @Override
    public int getItemViewType(int pos) {
        return pos;
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }
}