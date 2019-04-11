package mono.hg.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import mono.hg.R;
import mono.hg.SettingsActivity;
import mono.hg.adapters.WebProviderAdapter;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.models.WebSearchProvider;
import mono.hg.wrappers.BackHandledFragment;

public class WebProviderFragment extends BackHandledFragment {
    private ArrayList<WebSearchProvider> providerList = new ArrayList<>();
    private WebProviderAdapter providerAdapter;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_web_provider, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ActionBar actionBar = ((SettingsActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.pref_header_web_provider_short);
        }
        setHasOptionsMenu(true);

        ListView providerListView = requireActivity().findViewById(R.id.web_provider_list);
        providerAdapter = new WebProviderAdapter(requireContext(), providerList);

        providerListView.setAdapter(providerAdapter);
        providerListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
                final String name = providerList.get(i).getName();
                final String url = providerList.get(i).getUrl();

                PopupMenu popupMenu = new PopupMenu(requireActivity(), view);
                popupMenu.getMenuInflater().inflate(R.menu.menu_web_provider, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_web_provider_remove:
                                providerList.remove(i);
                                providerAdapter.notifyDataSetChanged();
                                PreferenceHelper.updateProvider(providerList);
                                return true;
                            case R.id.action_web_provider_edit:
                                makeEditMenu(name, url, true, i);
                                return true;
                            default:
                                return true;
                        }
                    }
                });

                popupMenu.show();
                return true;
            }
        });

        addProviders();

        // Add defaults if we don't have any provider.
        if (providerList.isEmpty()) {
            addDefaults();
        }
    }

    @Override public boolean onBackPressed() {
        return false;
    }

    @Override public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        menu.add(1, 0, 100, getString(R.string.action_web_provider_add));
        menu.add(1, 1, 100, getString(R.string.action_web_provider_reset));
        menu.getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.getItem(1).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                requireActivity().onBackPressed();
                return true;
            case 0:
                makeEditMenu();
                return true;
            case 1:
                providerList.clear();
                addDefaults();
                PreferenceHelper.updateProvider(providerList);
                providerAdapter.notifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addDefaults() {
        String[] defaultProvider = getResources().getStringArray(
                R.array.pref_search_provider_title);
        String[] defaultProviderId = getResources().getStringArray(
                R.array.pref_search_provider_values);

        // defaultProvider will always be the same size as defaultProviderUrl.
        // However, we start at 1 to ignore the 'Always ask' option.
        for (int i = 1; i < defaultProvider.length; i++) {
            providerList.add(new WebSearchProvider(defaultProvider[i],
                    PreferenceHelper.getDefaultProvider(defaultProviderId[i]),
                    defaultProvider[i]));
        }
    }

    private void addProviders() {
        for (Map.Entry<String, String> provider : PreferenceHelper.getProviderList().entrySet()) {
            providerList.add(new WebSearchProvider(provider.getKey(), provider.getValue()));
        }
    }

    private void makeEditMenu() {
        makeEditMenu("", "", false, 0);
    }

    private void makeEditMenu(String name, String url, final boolean isEditing, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = View.inflate(requireContext(), R.layout.layout_web_provider_edit_dialog, null);

        String title;

        final EditText nameField = view.findViewById(R.id.provider_edit_name);
        final EditText urlField = view.findViewById(R.id.provider_edit_url);

        if (!name.isEmpty()) {
            nameField.setText(name);
        }

        if (!url.isEmpty()) {
            urlField.setText(url);
        }

        if (isEditing) {
            title = getString(R.string.dialog_title_edit_provider);
        } else {
            title = getString(R.string.dialog_title_add_provider);
        }

        builder.setView(view);

        builder.setTitle(title)
               .setNegativeButton(android.R.string.cancel, null)
               .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                   @Override public void onClick(DialogInterface dialogInterface, int i) {
                       String name = nameField.getText().toString().replaceAll("\\|", "").trim();
                       String url = urlField.getText().toString().trim();

                       // Strip out %s as it triggers the matcher.
                       // We won't use this URL, but we still need to check if the URL overall is valid.
                       String safeUrl = url.replaceAll("%s", "+s");

                       if (!Patterns.WEB_URL.matcher(safeUrl).matches()) {
                           // This is an invalid URL, cancel.
                           Toast.makeText(requireContext(), R.string.err_invalid_url, Toast.LENGTH_SHORT).show();
                           return;
                       }

                       if (!"none".equals(PreferenceHelper.getProvider(name)) && !isEditing) {
                           // We already have that provider and/or we aren't editing.
                           Toast.makeText(requireContext(), R.string.err_provider_exists, Toast.LENGTH_SHORT).show();
                           return;
                       }

                       if (isEditing) {
                           providerList.set(position, new WebSearchProvider(name, url));
                       } else {
                           providerList.add(new WebSearchProvider(name, url));
                       }

                       PreferenceHelper.updateProvider(providerList);

                       providerAdapter.notifyDataSetChanged();
                   }
               }).show();
    }
}
