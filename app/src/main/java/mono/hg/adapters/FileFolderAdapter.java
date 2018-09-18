package mono.hg.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import mono.hg.FileFolder;
import mono.hg.R;
import mono.hg.Utils;

public class FileFolderAdapter extends BaseAdapter {
    private ArrayList<FileFolder> files;
    private Context context;

    public FileFolderAdapter(ArrayList<FileFolder> list, Context context) {
        this.files = list;
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ViewHolder holder;

        if (convertView == null) {
            convertView = Utils.requireNonNull(inflater).inflate(R.layout.files_folder_list, parent, false);

            holder = new ViewHolder();
            holder.content = convertView.findViewById(R.id.item_content);
            holder.name = convertView.findViewById(R.id.item_content_name);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (files.get(position).isFolder()) {
            holder.content.setImageResource(R.drawable.ic_folder);
        } else {
            holder.content.setImageResource(R.drawable.ic_file);
        }

        holder.name.setText(files.get(position).getName());

        return convertView;
    }

    private static class ViewHolder {
        private ImageView content;
        private TextView name;
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public Object getItem(int position) {
        return files.get(position);
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
