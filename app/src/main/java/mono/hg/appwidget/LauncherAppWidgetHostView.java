/*
 * Copyright (C) 2009 The Android Open Source Project
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

package mono.hg.appwidget;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

public class LauncherAppWidgetHostView extends AppWidgetHostView {
    private OnLongClickListener longClickListener;
    private long downTime;

    // Default duration before long press is triggered.
    private static long LONG_PRESS_DURATION = 300L;

    public LauncherAppWidgetHostView(Context context) {
        super(context);
    }

    @Override public void setOnLongClickListener(@Nullable OnLongClickListener listener) {
        this.longClickListener = listener;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_UP:
                boolean isLongPressing = System.currentTimeMillis() - downTime > LONG_PRESS_DURATION;

                if (isLongPressing) {
                    longClickListener.onLongClick(this);
                    return true;
                } else {
                    return false;
                }
            default:
                // Let the input fall through.
                return false;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override public int getDescendantFocusability() {
        return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
    }
}
