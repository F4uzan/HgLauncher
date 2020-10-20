package mono.hg.wrappers

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPreferenceCompat
import com.jaredrummler.android.colorpicker.ColorShape
import mono.hg.R
import mono.hg.helpers.PreferenceHelper
import mono.hg.utils.applyAccent

/**
 * A [ColorPreferenceCompat] that follows the launcher's accent theme.
 */
class ThemeableColorPreference(context: Context?, attrs: AttributeSet?) :
    ColorPreferenceCompat(context, attrs) {

    override fun onClick() {
        // TODO: Better strings for production release. At the moment we're scraping with what we have.
        ColorPickerDialog.newBuilder()
            .setDialogType(ColorPickerDialog.TYPE_PRESETS)
            .setDialogTitle(R.string.app_theme_accent_dialog)
            .setColorShape(ColorShape.CIRCLE)
            .setPresets(presets)
            .setAllowPresets(true)
            .setAllowCustom(true)
            .setShowAlphaSlider(false)
            .setShowColorShades(true)
            .setColor(getPersistedInt(PreferenceHelper.accent))
            .setPresetsButtonText(R.string.icon_pack_default)
            .setCustomButtonText(R.string.dialog_action_edit)
            .setSelectedButtonText(R.string.dialog_ok)
            .create().apply {
                setColorPickerDialogListener(this@ThemeableColorPreference)
            }.also {
                activity.supportFragmentManager.run {
                    beginTransaction().add(it, fragmentTag).commitAllowingStateLoss()

                    // Invoke executePendingTransactions to immediately show the dialog.
                    // FIXME: This might be a hack. Will need to investigate further.
                    executePendingTransactions()
                }

                (it.dialog as AlertDialog).apply {
                    applyAccent()
                }
            }
    }

}