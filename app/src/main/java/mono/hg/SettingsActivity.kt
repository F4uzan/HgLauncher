package mono.hg

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentTransaction
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.progressindicator.ProgressIndicator
import mono.hg.databinding.ActivitySettingsBinding
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
class SettingsActivity : AppCompatActivity(), BackHandlerInterface,
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private var selectedFragment: BackHandledFragment? = null
    private var fragmentTitle: CharSequence? = null
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var toolbar: Toolbar
    lateinit var progressBar: ProgressIndicator

    public override fun onCreate(savedInstanceState: Bundle?) {
        if (! PreferenceHelper.hasEditor()) {
            PreferenceHelper.initPreference(this)
        }

        PreferenceHelper.fetchPreference()
        if (PreferenceHelper.providerList.isEmpty()) {
            PreferenceHelper.updateProvider(Utils.setDefaultProviders(resources, ArrayList()))
        }

        // Check the caller of this activity.
        // If it's coming from the launcher itself, it will always have a calling activity.
        checkCaller()
        setActivityTheme()

        if (requestedOrientation != PreferenceHelper.orientation) {
            requestedOrientation = PreferenceHelper.orientation
        }

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        super.onCreate(savedInstanceState)

        toolbar = binding.toolbar
        progressBar = binding.progressBar.apply {
            indicatorColors = IntArray(1) { PreferenceHelper.darkerAccent }
            trackColor = PreferenceHelper.accent
            compatHide()
        }

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportActionBar !!.title = getString(R.string.title_activity_settings)
            ViewUtils.setFragment(supportFragmentManager, BasePreference(), "settings")
        } else {
            fragmentTitle = savedInstanceState.getCharSequence("title")
                ?: getString(R.string.title_activity_settings)
            supportActionBar?.title = fragmentTitle
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence("title", fragmentTitle)
    }

    override fun onBackPressed() {
        if (selectedFragment == null || ! selectedFragment !!.onBackPressed()) {
            // Selected fragment did not consume the back press event.
            if (supportFragmentManager.findFragmentByTag("settings") is BasePreference) {
                supportActionBar?.title = getString(R.string.title_activity_settings)
            } else {
                supportActionBar?.title = fragmentTitle
            }
            super.onBackPressed()
        }
    }

    override fun setSelectedFragment(backHandledFragment: BackHandledFragment?) {
        this.selectedFragment = backHandledFragment
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (supportFragmentManager.findFragmentByTag("settings") is BasePreference) {
                supportActionBar?.title = getString(R.string.title_activity_settings)
            } else {
                supportActionBar?.title = fragmentTitle
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
        if (callingActivity == null && ! PreferenceHelper.wasAlien()) {
            PreferenceHelper.isAlien(true)
        }
    }

    private fun setActivityTheme() {
        ViewUtils.switchTheme(this, false)
        if (Utils.atLeastLollipop() && resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_NO
        ) {
            window.statusBarColor = PreferenceHelper.darkerAccent
        }
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        val fragment =
            supportFragmentManager.fragmentFactory.instantiate(classLoader, pref.fragment)
        fragment.setTargetFragment(caller, 0)
        fragmentTitle = pref.title

        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.fragment_container, fragment, pref.key)
            .addToBackStack(pref.key)
            .commit()

        // Update the Activity's action bar
        supportActionBar?.title = fragmentTitle
        return true
    }
}

/**
 * Extension function for [ProgressIndicator]
 * that handles hiding for API levels lower than 17.
 */
fun ProgressIndicator.compatHide() {
    if (Utils.sdkIsAround(17)) {
        hide()
    } else {
        visibility = View.INVISIBLE
    }
}

/**
 * Extension function for [ProgressIndicator]
 * that handles showing the ProgressIndicator for API levels lower than 17.
 */
fun ProgressIndicator.compatShow() {
    if (Utils.sdkIsAround(17)) {
        show()
    } else {
        visibility = View.VISIBLE
    }
}
