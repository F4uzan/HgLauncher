package mono.hg.preferences

import android.os.Bundle
import androidx.annotation.Keep
import androidx.preference.PreferenceFragmentCompat
import mono.hg.R
import mono.hg.SettingsActivity

/**
 * Preferences for pages and drawer.
 */
@Keep
class PagesPreference : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_pages, rootKey)
    }

    override fun onResume() {
        super.onResume()

        // Update the action bar title.
        (requireActivity() as SettingsActivity).supportActionBar?.setTitle(R.string.pref_header_pages)
    }
}