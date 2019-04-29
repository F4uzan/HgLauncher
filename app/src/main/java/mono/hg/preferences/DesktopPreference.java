package mono.hg.preferences;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import mono.hg.R;
import mono.hg.utils.Utils;

public class DesktopPreference extends PreferenceFragmentCompat {
    private Preference.OnPreferenceChangeListener RotatingListListener = new Preference.OnPreferenceChangeListener() {
        @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
            requireActivity().setRequestedOrientation(Integer.parseInt((String) newValue));
            return true;
        }
    };

    @Override public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_desktop, rootKey);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListPreference orientationMode = findPreference("orientation_mode");

        orientationMode.setOnPreferenceChangeListener(RotatingListListener);

        // Window bar hiding works only reliably in KitKat and above.
        if (Utils.atLeastKitKat()) {
            findPreference("windowbar_mode").setVisible(true);
        } else {
            findPreference("windowbar_status_switch").setVisible(true);
        }
    }
}
