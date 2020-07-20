package mono.hg.preferences

import android.os.Bundle
import android.view.View
import androidx.annotation.Keep
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import mono.hg.R
import mono.hg.utils.Utils

/**
 * Preferences for the desktop/main view of the launcher.
 */
@Keep
class DesktopPreference : PreferenceFragmentCompat() {
    private val RotatingListListener = Preference.OnPreferenceChangeListener { _, newValue ->
        requireActivity().requestedOrientation = Integer.parseInt(newValue as String)
        true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_desktop, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findPreference<ListPreference>("orientation_mode")?.onPreferenceChangeListener =
            RotatingListListener

        // Window bar hiding works only reliably in KitKat and above.
        if (Utils.atLeastKitKat()) {
            findPreference<Preference>("windowbar_mode")?.isVisible = true
        } else {
            findPreference<Preference>("windowbar_status_switch")?.isVisible = true
        }
    }
}