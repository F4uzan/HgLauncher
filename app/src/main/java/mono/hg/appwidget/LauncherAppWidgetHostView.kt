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
package mono.hg.appwidget

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration

/**
 * Modification of the [AppWidgetHostView] class that allows intercepting touch events
 * to gather long press and other touch events.
 */
class LauncherAppWidgetHostView(context: Context?) : AppWidgetHostView(context) {
    private var longClickListener: OnLongClickListener? = null
    private var downTime: Long = 0

    override fun setOnLongClickListener(listener: OnLongClickListener?) {
        longClickListener = listener
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        downTime = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> event.eventTime
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> 0
            MotionEvent.ACTION_MOVE -> {
                val isLongPressing = event.eventTime - downTime > LONG_PRESS_DURATION
                return if (isLongPressing) {
                    longClickListener?.onLongClick(this)
                    true
                } else {
                    false
                }
            }
            else -> return false // Let the input fall through.
        }
        return super.onInterceptTouchEvent(event)
    }

    companion object {
        // Default duration before long press is triggered.
        private val LONG_PRESS_DURATION = ViewConfiguration.getLongPressTimeout()
    }
}