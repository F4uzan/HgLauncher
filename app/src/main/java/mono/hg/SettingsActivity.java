package mono.hg;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.core.app.ActivityCompat;
import mono.hg.fragments.CustomizePreferenceFragment;
import mono.hg.helpers.PreferenceHelper;
import mono.hg.utils.ActivityServiceUtils;
import mono.hg.wrappers.BackHandledFragment;

public class SettingsActivity extends com.fnp.materialpreferences.PreferenceActivity
        implements BackHandledFragment.BackHandlerInterface {
    private BackHandledFragment selectedFragment;

    @Override public void onCreate(Bundle savedInstanceState) {
        if (PreferenceHelper.getPreference() == null) {
            PreferenceHelper.initPreference(this);
        }
        PreferenceHelper.fetchPreference();

        // Load the appropriate theme.
        switch (PreferenceHelper.appTheme()) {
            default:
            case "light":
                setTheme(R.style.SettingTheme);
                break;
            case "dark":
                setTheme(R.style.SettingTheme_Gray);
                break;
            case "black":
                setTheme(R.style.SettingTheme_Dark);
                break;
        }

        super.onCreate(savedInstanceState);
        setPreferenceFragment(new CustomizePreferenceFragment());
    }

    @Override public void onBackPressed() {
        if (selectedFragment == null || !selectedFragment.onBackPressed()) {
            // Selected fragment did not consume the back press event.
            super.onBackPressed();
        }
    }

    @Override public void setSelectedFragment(BackHandledFragment selectedFragment) {
        this.selectedFragment = selectedFragment;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            ActivityServiceUtils.hideSoftKeyboard(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Called when the activity needs to be restarted (i.e when a theme change occurs).
    // Allows for smooth transition between recreation.
    public void restartActivity() {
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        ActivityCompat.finishAfterTransition(this);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        startActivity(intent);
    }
}
