package mono.hg;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import mono.hg.helpers.PreferenceHelper;
import mono.hg.preferences.BasePreference;
import mono.hg.utils.ActivityServiceUtils;
import mono.hg.utils.Utils;
import mono.hg.utils.ViewUtils;
import mono.hg.wrappers.BackHandledFragment;

public class SettingsActivity extends AppCompatActivity
        implements BackHandledFragment.BackHandlerInterface, PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private BackHandledFragment selectedFragment;
    private CharSequence fragmentTitle;

    @Override public void onCreate(Bundle savedInstanceState) {
        if (!PreferenceHelper.hasEditor()) {
            PreferenceHelper.initPreference(this);
        }
        PreferenceHelper.fetchPreference();

        if (PreferenceHelper.getProviderList().isEmpty()) {
            Utils.setDefaultProviders(getResources());
        }

        // Check the caller of this activity.
        // If it's coming from the launcher itself, it will always have a calling activity.
        checkCaller();

        // Load the appropriate theme.
        switch (PreferenceHelper.appTheme()) {
            default:
            case "auto":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                break;
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                setTheme(R.style.AppTheme_Dark);
                break;
            case "black":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getRequestedOrientation() != PreferenceHelper.getOrientation()) {
            setRequestedOrientation(PreferenceHelper.getOrientation());
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            ViewUtils.setFragment(getSupportFragmentManager(), new BasePreference(), "settings");
        } else {
            fragmentTitle = savedInstanceState.getCharSequence("title");
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(fragmentTitle);
            }
        }
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence("title", fragmentTitle);
    }

    @Override public void onBackPressed() {
        if (selectedFragment == null || !selectedFragment.onBackPressed()) {
            // Selected fragment did not consume the back press event.
            if (getSupportActionBar() != null) {
                if (getSupportFragmentManager().findFragmentByTag(
                        "settings") instanceof BasePreference) {
                    getSupportActionBar().setTitle(getString(R.string.title_activity_settings));
                } else {
                    getSupportActionBar().setTitle(fragmentTitle);
                }
            }
            super.onBackPressed();
        }
    }

    @Override public void setSelectedFragment(BackHandledFragment selectedFragment) {
        this.selectedFragment = selectedFragment;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (getSupportActionBar() != null) {
                if (getSupportFragmentManager().findFragmentByTag(
                        "settings") instanceof BasePreference) {
                    getSupportActionBar().setTitle(getString(R.string.title_activity_settings));
                } else {
                    getSupportActionBar().setTitle(fragmentTitle);
                }
            }
            super.onBackPressed();
            ActivityServiceUtils.hideSoftKeyboard(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void checkCaller() {
        // If this activity is called from anywhere else but the launcher,
        // then the launcher needs to be informed of the changes made that it may not be aware of.
        if (getCallingActivity() == null && !PreferenceHelper.wasAlien()) {
            PreferenceHelper.isAlien(true);
        }
    }

    // Called when the activity needs to be restarted (i.e when a theme change occurs).
    // Allows for smooth transition between recreation.
    public void restartActivity() {
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityCompat.finishAfterTransition(this);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        startActivity(intent);
    }

    @Override public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Fragment fragment = getSupportFragmentManager()
                .getFragmentFactory().instantiate(getClassLoader(), pref.getFragment());
        fragment.setTargetFragment(caller, 0);
        fragmentTitle = pref.getTitle();

        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                                   .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                   .replace(R.id.fragment_container, fragment, pref.getKey())
                                   .addToBackStack(pref.getKey())
                                   .commit();

        // Update the Activity's action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fragmentTitle);
        }
        return true;
    }
}
