package mono.hg.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import mono.hg.R;
import mono.hg.utils.Utils;

public class LibraryInfoFragment extends DialogFragment {

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = View.inflate(getActivity(), R.layout.fragment_library_dialogue, null);

        builder.setTitle(R.string.about_libraries_dialogue_title);
        builder.setView(view);
        ListView libsListView = view.findViewById(R.id.libs_list);

        libsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        // SlidingUpPanel.
                        Utils.openLink(getActivity(), getString(R.string.lib_slidinguppanel_url));
                        break;
                    case 1:
                        // RecyclerView-FastScroll.
                        Utils.openLink(getActivity(),
                                getString(R.string.lib_recyclerview_fastscroll_url));
                        break;
                    case 2:
                        // material-preferences.
                        Utils.openLink(getActivity(),
                                getString(R.string.lib_material_preferences_url));
                        break;
                    case 3:
                        // material-preferences.
                        Utils.openLink(getActivity(), getString(R.string.lib_flexibleadapter_url));
                        break;
                    default:
                        // Nada.
                        break;
                }
            }
        });

        return builder.create();
    }
}
