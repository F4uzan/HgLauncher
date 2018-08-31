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
        View view = inflater.inflate(R.layout.files_folder_list, parent, false);

        ImageView folder = view.findViewById(R.id.item_folder);
        ImageView file = view.findViewById(R.id.item_file);
        TextView name = view.findViewById(R.id.item_content_name);

        if (files.get(position).isFolder()) {
            folder.setVisibility(View.VISIBLE);
        } else {
            file.setVisibility(View.VISIBLE);
        }

        name.setText(files.get(position).getName());

        return view;
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
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
