package mono.hg.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import mono.hg.R;
import mono.hg.Utils;
import mono.hg.models.FileFolder;

public class FileFolderAdapter extends BaseAdapter {
    private ArrayList<FileFolder> filesList;
    private Context context;

    public FileFolderAdapter(ArrayList<FileFolder> files, Context context) {
        this.filesList = files;
        this.context = context;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ViewHolder holder;
        View view = convertView;

        if (view == null) {
            view = Utils.requireNonNull(inflater)
                        .inflate(R.layout.files_folder_list, parent, false);

            holder = new ViewHolder();
            holder.content = view.findViewById(R.id.item_content);
            holder.name = view.findViewById(R.id.item_content_name);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        if (filesList.get(position).isFolder()) {
            holder.content.setImageResource(R.drawable.ic_folder);
        } else {
            holder.content.setImageResource(R.drawable.ic_file);
        }

        holder.name.setText(filesList.get(position).getName());

        return view;
    }

    @Override public int getCount() {
        return filesList.size();
    }

    @Override public Object getItem(int position) {
        return filesList.get(position);
    }

    @Override public long getItemId(int position) {
        return position;
    }

    @Override public boolean hasStableIds() {
        return true;
    }

    private static class ViewHolder {
        private ImageView content;
        private TextView name;
    }
}
