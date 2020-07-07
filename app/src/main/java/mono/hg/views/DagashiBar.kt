/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mono.hg.views

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.SnackbarContentLayout
import mono.hg.R

/**
 * DagashiBar is a Snackbar clone tailored for use in HgLauncher.
 *
 * Due to Snackbar being entirely limited, no class can extend from it.
 * DagashiBar is based off of (read: copied from) Google's Snackbar code.
 */
open class DagashiBar private constructor(
        parent: ViewGroup,
        content: View,
        contentViewCallback: com.google.android.material.snackbar.ContentViewCallback) : BaseTransientBottomBar<DagashiBar?>(parent, content, contentViewCallback) {
    private val accessibilityManager: AccessibilityManager = parent.context
            .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    private var hasAction = false

    /**
     * Update the text in this [DagashiBar].
     *
     * @param message The new text for this [BaseTransientBottomBar].
     */
    @SuppressLint("RestrictedApi")
    fun setText(message: CharSequence): DagashiBar {
        val contentLayout = view.getChildAt(0) as SnackbarContentLayout
        val tv = contentLayout.messageView
        tv.text = message
        return this
    }

    /**
     * Update the text in this [DagashiBar].
     *
     * @param resId The new text for this [BaseTransientBottomBar].
     */
    fun setText(@StringRes resId: Int): DagashiBar {
        return setText(context.getText(resId))
    }

    /**
     * Set the action to be displayed in this [BaseTransientBottomBar].
     *
     * @param resId    String resource to display for the action
     * @param listener callback to be invoked when the action is clicked
     */
    fun setAction(@StringRes resId: Int, listener: View.OnClickListener?): DagashiBar {
        return setAction(context.getText(resId), listener, true)
    }

    /**
     * Set the text color of the action in this [BaseTransientBottomBar]
     *
     * @param color The color to use for the action.
     */
    @SuppressLint("RestrictedApi")
    fun setTextColor(@ColorInt color: Int): DagashiBar {
        val contentLayout = view.getChildAt(0) as SnackbarContentLayout
        val tv: TextView = contentLayout.actionView
        tv.setTextColor(color)
        return this
    }

    /**
     * Set the action to be displayed. The action will not dismiss this [BaseTransientBottomBar]
     *
     * @param text     Text to display for the action
     * @param listener callback to be invoked when the action is clicked
     */
    fun setNonDismissAction(text: CharSequence?, listener: View.OnClickListener?): DagashiBar {
        return setAction(text, listener, false)
    }

    /**
     * Set the action to be displayed in this [BaseTransientBottomBar].
     *
     * @param text     Text to display for the action
     * @param listener callback to be invoked when the action is clicked
     * @param dismiss  Whether the action should dismiss the bar
     */
    @SuppressLint("RestrictedApi")
    fun setAction(text: CharSequence?, listener: View.OnClickListener?, dismiss: Boolean): DagashiBar {
        val contentLayout = view.getChildAt(0) as SnackbarContentLayout
        val tv: TextView = contentLayout.actionView
        if (TextUtils.isEmpty(text) || listener == null) {
            tv.visibility = View.GONE
            tv.setOnClickListener(null)
            hasAction = false
        } else {
            hasAction = true
            tv.visibility = View.VISIBLE
            tv.text = text
            tv.setOnClickListener { view ->
                listener.onClick(view)
                // Dismiss when requested.
                if (dismiss) {
                    dispatchDismiss(BaseCallback.DISMISS_EVENT_ACTION)
                }
            }
        }
        return this
    }

    /**
     * Set the long press action that is called when the user taps on the main action
     * of this [BaseTransientBottomBar]
     *
     * @param listener callback to be invoked when the action is clicked
     */
    @SuppressLint("RestrictedApi")
    fun setLongPressAction(listener: OnLongClickListener) {
        val contentLayout = view.getChildAt(0) as SnackbarContentLayout
        val tv: TextView = contentLayout.actionView
        tv.setOnLongClickListener { view ->
            listener.onLongClick(view)
            true
        }
    }

    /**
     * Disables swipe-to-dismiss behavior.
     */
    fun setSwipeDisabled() {
        behavior = object : Behavior() {
            override fun canSwipeDismissView(child: View): Boolean {
                return false
            }
        }
    }

    override fun getDuration(): Int {
        // If touch exploration is enabled override duration to give people chance to interact.
        return if (hasAction && accessibilityManager.isTouchExplorationEnabled) BaseTransientBottomBar.LENGTH_INDEFINITE else super.getDuration()
    }

    /**
     * The duration length constants, see [DagashiBar.Companion] for the constant values.
     */
    @IntDef(LENGTH_INDEFINITE, LENGTH_SHORT, LENGTH_LONG)
    @IntRange(from = 1)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class Duration

    companion object {
        /**
         * Show the DagashiBar indefinitely.
         */
        const val LENGTH_INDEFINITE = BaseTransientBottomBar.LENGTH_INDEFINITE

        /**
         * Show the DagashiBar for a short period of time.
         */
        const val LENGTH_SHORT = BaseTransientBottomBar.LENGTH_SHORT

        /**
         * Show the DagashiBar for a long period of time.
         */
        const val LENGTH_LONG = BaseTransientBottomBar.LENGTH_LONG
        private val SNACKBAR_BUTTON_STYLE_ATTR = intArrayOf(R.attr.snackbarButtonStyle)

        /**
         * Make a DagashiBar to display a message
         *
         * @param view      The view to find a parent from.
         * @param text      The text to show. Can be formatted text.
         * @param duration  How long to display the message. Can be [.LENGTH_SHORT], [                  ][.LENGTH_LONG], [.LENGTH_INDEFINITE], or a custom duration in milliseconds.
         * @param swipeable Whether swipe-to-dismiss is allowed.
         */
        @SuppressLint("PrivateResource")
        fun make(
                view: View, text: CharSequence, @Duration duration: Int, swipeable: Boolean): DagashiBar {
            val parent = findSuitableParent(view)
                    ?: throw IllegalArgumentException(
                            "No suitable parent found from the given view. Please provide a valid view.")
            val inflater = LayoutInflater.from(parent.context)
            val content = inflater.inflate(
                    if (hasSnackbarButtonStyleAttr(parent.context)) R.layout.mtrl_layout_snackbar_include else R.layout.design_layout_snackbar_include,
                    parent,
                    false) as SnackbarContentLayout
            val snackbar = DagashiBar(parent, content, content)
            snackbar.setText(text)
            snackbar.duration = duration
            if (!swipeable) {
                snackbar.setSwipeDisabled()
            }
            return snackbar
        }

        /**
         * [DagashiBar]s should still work with AppCompat themes, which don't specify a `snackbarButtonStyle`. This method helps to check if a valid `snackbarButtonStyle` is set
         * within the current context, so that we know whether we can use the attribute.
         */
        protected fun hasSnackbarButtonStyleAttr(context: Context): Boolean {
            val a = context.obtainStyledAttributes(SNACKBAR_BUTTON_STYLE_ATTR)
            val snackbarButtonStyleResId = a.getResourceId(0, -1)
            a.recycle()
            return snackbarButtonStyleResId != -1
        }

        private fun findSuitableParent(attachView: View): ViewGroup? {
            var fallback: ViewGroup? = null
            var view: View? = attachView
            do {
                if (view is CoordinatorLayout) {
                    // We've found a CoordinatorLayout, use it
                    return view
                } else if (view is FrameLayout) {
                    fallback = if (view.getId() == R.id.content) {
                        // If we've hit the decor content view, then we didn't find a CoL in the
                        // hierarchy, so use it.
                        return view
                    } else {
                        // It's not the content view but we'll use it as our fallback
                        view
                    }
                }
                if (view != null) {
                    // Else, we will loop and crawl up the view hierarchy and try to find a parent
                    val parent = view.parent
                    view = if (parent is View) parent else null
                }
            } while (view != null)

            // If we reach here then we didn't find a CoL or a suitable content view so we'll fallback
            return fallback
        }
    }

}