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
        View view = inflater.inflate(R.layout.hidden_app_list, parent, false);

        ImageView icon = view.findViewById(R.id.item_app_icon);
        TextView name = view.findViewById(R.id.item_app_name);
        AppCompatCheckBox hiddenState = view.findViewById(R.id.item_app_hidden_state);

        if (apps.get(position).isHidden()) {
            hiddenState.setChecked(true);
        } else {
            hiddenState.setChecked(false);
        }

        icon.setImageDrawable(apps.get(position).getIcon());
        name.setText(apps.get(position).getAppName());

        return view;
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
