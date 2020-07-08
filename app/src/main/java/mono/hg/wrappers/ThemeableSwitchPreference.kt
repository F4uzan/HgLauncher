package mono.hg.wrappers

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import mono.hg.helpers.PreferenceHelper

class ThemeableSwitchPreference(context: Context, attrs: AttributeSet?) : SwitchPreferenceCompat(context, attrs) {
    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        val switch : SwitchCompat? = holder?.itemView?.findViewById(androidx.preference.R.id.switchWidget)

        val thumbStates = ColorStateList(arrayOf(intArrayOf(- android.R.attr.state_enabled), intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(
                PreferenceHelper.accent,
                PreferenceHelper.accent,
                Color.LTGRAY
        ))

        val trackStates = ColorStateList(arrayOf(intArrayOf(- android.R.attr.state_enabled), intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(
                Color.LTGRAY,
                PreferenceHelper.darkerAccent,
                Color.GRAY
        ))

        switch?.thumbTintList = thumbStates
        switch?.trackTintList = trackStates
        switch?.trackTintMode = PorterDuff.Mode.SRC_IN
    }
}