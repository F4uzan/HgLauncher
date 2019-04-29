package mono.hg.preferences;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mono.hg.R;
import mono.hg.helpers.PreferenceHelper;

public class WebSearchPreference extends PreferenceFragmentCompat {
    private ListPreference providerList;

    @Override public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_web, rootKey);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        providerList = findPreference("search_provider");
        setProviderList(providerList);
    }

    @Override public void onResume() {
        super.onResume();

        // Set this here to make sure its values are updated every time we return to it.
        setProviderList(providerList);
    }

    private void setProviderList(ListPreference list) {
        List<String> entries = new ArrayList<>();
        List<String> entryValues = new ArrayList<>();

        entries.add(getString(R.string.search_provider_none));
        entryValues.add(getString(R.string.gesture_action_default_value));

        // We only need the key as the value is stored in PreferenceHelper's Map.
        for (Map.Entry<String, String> provider : PreferenceHelper
                .getProviderList().entrySet()) {
            entries.add(provider.getKey());
            entryValues.add(provider.getKey());
        }

        CharSequence[] finalEntries = entries.toArray(new CharSequence[0]);
        CharSequence[] finalEntryValues = entryValues.toArray(new CharSequence[0]);

        list.setEntries(finalEntries);
        list.setEntryValues(finalEntryValues);
    }
}
