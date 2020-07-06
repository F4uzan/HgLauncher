package mono.hg

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentTransaction
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import mono.hg.helpers.PreferenceHelper
import mono.hg.preferences.BasePreference
import mono.hg.utils.ActivityServiceUtils
import mono.hg.utils.Utils
import mono.hg.utils.ViewUtils
import mono.hg.wrappers.BackHandledFragment
import mono.hg.wrappers.BackHandledFragment.BackHandlerInterface

/**
 * Activity hosting all of preference fragments.
 *
 * This activity can be called through 'Additional setting' in the System settings as well.
 */
class SettingsActivity : AppCompatActivity(), BackHandlerInterface, PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private var selectedFragment: BackHandledFragment? = null
    private var fragmentTitle: CharSequence? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        if (!PreferenceHelper.hasEditor()) {
            PreferenceHelper.initPreference(this)
        }
        PreferenceHelper.fetchPreference()
        if (PreferenceHelper.providerList.isEmpty()) {
            Utils.setDefaultProviders(resources)
        }

        // Check the caller of this activity.
        // If it's coming from the launcher itself, it will always have a calling activity.
        checkCaller()
        when (PreferenceHelper.appTheme()) {
            "auto" -> if (Utils.atLeastQ()) {
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            } else {
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            }
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                setTheme(R.style.AppTheme_Dark)
            }
            "black" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> if (Utils.atLeastQ()) {
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            } else {
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            }
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (requestedOrientation != PreferenceHelper.orientation) {
            requestedOrientation = PreferenceHelper.orientation
        }
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        if (savedInstanceState == null) {
            ViewUtils.setFragment(supportFragmentManager, BasePreference(), "settings")
        } else {
            fragmentTitle = savedInstanceState.getCharSequence("title")
            if (supportActionBar != null) {
                supportActionBar!!.title = fragmentTitle
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence("title", fragmentTitle)
    }

    override fun onBackPressed() {
        if (selectedFragment == null || !selectedFragment!!.onBackPressed()) {
            // Selected fragment did not consume the back press event.
            if (supportActionBar != null) {
                if (supportFragmentManager.findFragmentByTag(
                                "settings") is BasePreference) {
                    supportActionBar!!.title = getString(R.string.title_activity_settings)
                } else {
                    supportActionBar!!.title = fragmentTitle
                }
            }
            super.onBackPressed()
        }
    }

    override fun setSelectedFragment(backHandledFragment: BackHandledFragment?) {
        this.selectedFragment = backHandledFragment
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (supportActionBar != null) {
                if (supportFragmentManager.findFragmentByTag(
                                "settings") is BasePreference) {
                    supportActionBar!!.title = getString(R.string.title_activity_settings)
                } else {
                    supportActionBar!!.title = fragmentTitle
                }
            }
            super.onBackPressed()
            ActivityServiceUtils.hideSoftKeyboard(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkCaller() {
        // If this activity is called from anywhere else but the launcher,
        // then the launcher needs to be informed of the changes made that it may not be aware of.
        if (callingActivity == null && !PreferenceHelper.wasAlien()) {
            PreferenceHelper.isAlien(true)
        }
    }

    /**
     * Called when the activity needs to be restarted (i.e when a theme change occurs).
     * Allows for smooth transition between recreation.
     */
    fun restartActivity() {
        val intent = intent
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityCompat.finishAfterTransition(this)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        startActivity(intent)
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        // Instantiate the new Fragment
        val fragment = supportFragmentManager
                .fragmentFactory.instantiate(classLoader, pref.fragment)
        fragment.setTargetFragment(caller, 0)
        fragmentTitle = pref.title

        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.fragment_container, fragment, pref.key)
                .addToBackStack(pref.key)
                .commit()

        // Update the Activity's action bar
        if (supportActionBar != null) {
            supportActionBar!!.title = fragmentTitle
        }
        return true
    }
}