package mono.hg.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import mono.hg.R;
import mono.hg.models.WebSearchProvider;

public class WebProviderAdapter extends BaseAdapter {
    private ArrayList<WebSearchProvider> itemList;
    private Context context;

    public WebProviderAdapter(Context context, ArrayList<WebSearchProvider> list) {
        this.itemList = list;
        this.context = context;
    }

    @Override public int getCount() {
        return itemList.size();
    }

    @Override public Object getItem(int i) {
        return itemList.get(i);
    }

    @Override public long getItemId(int i) {
        return i;
    }

    @Override public View getView(int i, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        View view = convertView;

        if (view == null) {
            view = View.inflate(context, R.layout.list_web_provider_item, null);

            viewHolder = new ViewHolder();

            viewHolder.title = view.findViewById(R.id.provider_title);
            viewHolder.url = view.findViewById(R.id.provider_url);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        viewHolder.title.setText(itemList.get(i).getName());
        viewHolder.url.setText(itemList.get(i).getUrl());

        return view;
    }

    private static class ViewHolder {
        private TextView title;
        private TextView url;
    }
}
