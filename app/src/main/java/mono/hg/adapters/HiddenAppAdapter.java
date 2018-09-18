package mono.hg.adapters;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import mono.hg.AppDetail;
import mono.hg.R;
import mono.hg.Utils;

public class HiddenAppAdapter extends BaseAdapter {
    private ArrayList<AppDetail> apps;
    private Context context;

    public HiddenAppAdapter(ArrayList<AppDetail> apps, Context context) {
        this.apps = apps;
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ViewHolder appHolder;
        View view = convertView;

        if (view == null) {
            view = Utils.requireNonNull(inflater).inflate(R.layout.hidden_app_list, parent, false);

            appHolder = new ViewHolder();

            appHolder.icon = view.findViewById(R.id.item_app_icon);
            appHolder.name = view.findViewById(R.id.item_app_name);
            appHolder.hiddenState = view.findViewById(R.id.item_app_hidden_state);

            view.setTag(appHolder);
        } else {
            appHolder = (ViewHolder) view.getTag();
        }

        if (apps.get(position).isHidden()) {
            appHolder.hiddenState.setChecked(true);
        } else {
            appHolder.hiddenState.setChecked(false);
        }

        appHolder.icon.setImageDrawable(apps.get(position).getIcon());
        appHolder.name.setText(apps.get(position).getAppName());

        return view;
    }

    private static class ViewHolder {
        private ImageView icon;
        private TextView name;
        private AppCompatCheckBox hiddenState;
    }

    @Override
    public int getCount() {
        return apps.size();
    }

    @Override
    public Object getItem(int position) {
        return apps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
