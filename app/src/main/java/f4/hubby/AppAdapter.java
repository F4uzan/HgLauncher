package f4.hubby;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
    private List<AppDetail> apps;

    AppAdapter(List<AppDetail> apps) {
        this.apps = apps;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        AppDetail app;
        TextView name;
        ImageView icon;

        void setItem(AppDetail app) {
            this.app = app;
            name.setText(app.getLabel());
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
    public int getItemViewType(int pos) {
        return pos;
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

}
