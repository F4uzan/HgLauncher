package mono.hg.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import mono.hg.R;
import mono.hg.models.AppDetail;
import mono.hg.utils.Utils;

public class HiddenAppAdapter extends BaseAdapter {
    private ArrayList<AppDetail> hiddenAppsList;
    private Context context;

    public HiddenAppAdapter(ArrayList<AppDetail> hiddenApps, Context context) {
        this.hiddenAppsList = hiddenApps;
        this.context = context;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ViewHolder appHolder;
        View view = convertView;

        if (view == null) {
            view = Utils.requireNonNull(inflater).inflate(R.layout.list_generic_item, parent, false);

            appHolder = new ViewHolder();

            appHolder.icon = view.findViewById(R.id.item_icon);
            appHolder.name = view.findViewById(R.id.item_name);

            view.setTag(appHolder);
        } else {
            appHolder = (ViewHolder) view.getTag();
        }

        appHolder.name.setText(hiddenAppsList.get(position).getAppName());

        if (hiddenAppsList.get(position).isAppHidden()) {
            appHolder.icon.setImageResource(R.drawable.ic_check);
            appHolder.name.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            appHolder.icon.setImageDrawable(hiddenAppsList.get(position).getIcon());
            appHolder.name.setTypeface(Typeface.DEFAULT);
        }

        return view;
    }

    @Override public int getCount() {
        return hiddenAppsList.size();
    }

    @Override public Object getItem(int position) {
        return hiddenAppsList.get(position);
    }

    @Override public long getItemId(int position) {
        return position;
    }

    @Override public boolean hasStableIds() {
        return true;
    }

    private static class ViewHolder {
        private ImageView icon;
        private TextView name;
    }
}
