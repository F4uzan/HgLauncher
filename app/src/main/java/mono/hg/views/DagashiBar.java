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

package mono.hg.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.R;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.SnackbarContentLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

/**
 * DagashiBar is a Snackbar clone tailored for use in HgLauncher.
 * <p>
 * Due to Snackbar being entirely limited, no class can extend from it.
 * DagashiBar is based off of (read: copied from) Google's Snackbar code.
 */
public class DagashiBar extends BaseTransientBottomBar<DagashiBar> {

    /**
     * Show the DagashiBar indefinitely.
     */
    public static final int LENGTH_INDEFINITE = BaseTransientBottomBar.LENGTH_INDEFINITE;
    /**
     * Show the DagashiBar for a short period of time.
     */
    public static final int LENGTH_SHORT = BaseTransientBottomBar.LENGTH_SHORT;
    /**
     * Show the DagashiBar for a long period of time.
     */
    public static final int LENGTH_LONG = BaseTransientBottomBar.LENGTH_LONG;
    private static final int[] SNACKBAR_BUTTON_STYLE_ATTR = new int[]{R.attr.snackbarButtonStyle};
    private final AccessibilityManager accessibilityManager;
    private boolean hasAction;

    private DagashiBar(
            ViewGroup parent,
            View content,
            com.google.android.material.snackbar.ContentViewCallback contentViewCallback) {
        super(parent, content, contentViewCallback);
        accessibilityManager =
                (AccessibilityManager) parent.getContext()
                                             .getSystemService(Context.ACCESSIBILITY_SERVICE);
    }

    /**
     * Make a DagashiBar to display a message
     *
     * @param view     The view to find a parent from.
     * @param text     The text to show. Can be formatted text.
     * @param duration How long to display the message. Can be {@link #LENGTH_SHORT}, {@link
     *                 #LENGTH_LONG}, {@link #LENGTH_INDEFINITE}, or a custom duration in milliseconds.
     */
    @NonNull
    public static DagashiBar make(
            @NonNull View view, @NonNull CharSequence text, @Duration int duration) {
        final ViewGroup parent = findSuitableParent(view);
        if (parent == null) {
            throw new IllegalArgumentException(
                    "No suitable parent found from the given view. Please provide a valid view.");
        }

        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final SnackbarContentLayout content =
                (SnackbarContentLayout)
                        inflater.inflate(
                                hasSnackbarButtonStyleAttr(parent.getContext())
                                        ? R.layout.mtrl_layout_snackbar_include
                                        : R.layout.design_layout_snackbar_include,
                                parent,
                                false);
        final DagashiBar snackbar = new DagashiBar(parent, content, content);
        snackbar.setText(text);
        snackbar.setDuration(duration);
        return snackbar;
    }

    /**
     * {@link DagashiBar}s should still work with AppCompat themes, which don't specify a {@code
     * snackbarButtonStyle}. This method helps to check if a valid {@code snackbarButtonStyle} is set
     * within the current context, so that we know whether we can use the attribute.
     */
    protected static boolean hasSnackbarButtonStyleAttr(Context context) {
        TypedArray a = context.obtainStyledAttributes(SNACKBAR_BUTTON_STYLE_ATTR);
        int snackbarButtonStyleResId = a.getResourceId(0, -1);
        a.recycle();
        return snackbarButtonStyleResId != -1;
    }

    /**
     * Make a DagashiBar to display a message.
     *
     * @param view     The view to find a parent from.
     * @param resId    The resource id of the string resource to use. Can be formatted text.
     * @param duration How long to display the message. Can be {@link #LENGTH_SHORT}, {@link
     *                 #LENGTH_LONG}, {@link #LENGTH_INDEFINITE}, or a custom duration in milliseconds.
     */
    @NonNull
    public static DagashiBar make(@NonNull View view, @StringRes int resId, @Duration int duration) {
        return make(view, view.getResources().getText(resId), duration);
    }

    private static ViewGroup findSuitableParent(View attachView) {
        ViewGroup fallback = null;
        View view = attachView;

        do {
            if (view instanceof CoordinatorLayout) {
                // We've found a CoordinatorLayout, use it
                return (ViewGroup) view;
            } else if (view instanceof FrameLayout) {
                if (view.getId() == android.R.id.content) {
                    // If we've hit the decor content view, then we didn't find a CoL in the
                    // hierarchy, so use it.
                    return (ViewGroup) view;
                } else {
                    // It's not the content view but we'll use it as our fallback
                    fallback = (ViewGroup) view;
                }
            }

            if (view != null) {
                // Else, we will loop and crawl up the view hierarchy and try to find a parent
                final ViewParent parent = view.getParent();
                view = parent instanceof View ? (View) parent : null;
            }
        } while (view != null);

        // If we reach here then we didn't find a CoL or a suitable content view so we'll fallback
        return fallback;
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }

    @Override
    public boolean isShown() {
        return super.isShown();
    }

    /**
     * Update the text in this {@link DagashiBar}.
     *
     * @param message The new text for this {@link BaseTransientBottomBar}.
     */
    @NonNull
    @SuppressLint("RestrictedApi")
    public DagashiBar setText(@NonNull CharSequence message) {
        final SnackbarContentLayout contentLayout = (SnackbarContentLayout) view.getChildAt(0);
        final TextView tv = contentLayout.getMessageView();
        tv.setText(message);
        return this;
    }

    /**
     * Update the text in this {@link DagashiBar}.
     *
     * @param resId The new text for this {@link BaseTransientBottomBar}.
     */
    @NonNull
    public DagashiBar setText(@StringRes int resId) {
        return setText(getContext().getText(resId));
    }

    /**
     * Set the action to be displayed in this {@link BaseTransientBottomBar}.
     *
     * @param resId    String resource to display for the action
     * @param listener callback to be invoked when the action is clicked
     */
    @NonNull
    public DagashiBar setAction(@StringRes int resId, View.OnClickListener listener) {
        return setAction(getContext().getText(resId), listener, true);
    }

    /**
     * Set the action to be displayed. The action will not dismiss this {@link BaseTransientBottomBar}
     *
     * @param text     Text to display for the action
     * @param listener callback to be invoked when the action is clicked
     *
     * @return
     */
    public DagashiBar setNonDismissAction(CharSequence text, View.OnClickListener listener) {
        return setAction(text, listener, false);
    }

    /**
     * Set the action to be displayed in this {@link BaseTransientBottomBar}.
     *
     * @param text     Text to display for the action
     * @param listener callback to be invoked when the action is clicked
     * @param dismiss  Whether the action should dismiss the bar
     */
    @NonNull
    @SuppressLint("RestrictedApi")
    public DagashiBar setAction(CharSequence text, final View.OnClickListener listener, final boolean dismiss) {
        final SnackbarContentLayout contentLayout = (SnackbarContentLayout) this.view.getChildAt(0);
        final TextView tv = contentLayout.getActionView();

        if (TextUtils.isEmpty(text) || listener == null) {
            tv.setVisibility(View.GONE);
            tv.setOnClickListener(null);
            hasAction = false;
        } else {
            hasAction = true;
            tv.setVisibility(View.VISIBLE);
            tv.setText(text);
            tv.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            listener.onClick(view);
                            // Dismiss when requested.
                            if (dismiss) {
                                dispatchDismiss(BaseCallback.DISMISS_EVENT_ACTION);
                            }
                        }
                    });
        }
        return this;
    }

    @SuppressLint("RestrictedApi")
    public void setLongPressAction(final View.OnLongClickListener listener) {
        final SnackbarContentLayout contentLayout = (SnackbarContentLayout) this.view.getChildAt(0);
        final TextView tv = contentLayout.getActionView();

        tv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View view) {
                listener.onLongClick(view);
                return true;
            }
        });
    }

    /**
     * Disables swipe-to-dismiss behavior.
     */
    public void setSwipeDisabled() {
        setBehavior(new BaseTransientBottomBar.Behavior() {
            @Override
            public boolean canSwipeDismissView(View child) {
                return false;
            }
        });
    }

    @Override
    public int getDuration() {
        // If touch exploration is enabled override duration to give people chance to interact.
        return hasAction && accessibilityManager.isTouchExplorationEnabled()
                ? BaseTransientBottomBar.LENGTH_INDEFINITE
                : super.getDuration();
    }

    @IntDef({LENGTH_INDEFINITE, LENGTH_SHORT, LENGTH_LONG})
    @IntRange(from = 1)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Duration {
    }
}
