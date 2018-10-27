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
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

public class LauncherAppWidgetHostView extends AppWidgetHostView {
    private float oldX = 0;
    private float oldY = 0;
    private float newX = 0;
    private float newY = 0;
    private boolean mHasPerformedLongPress;
    //private LayoutInflater mInflater;

    public LauncherAppWidgetHostView(Context context) {
        super(context);
        //mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override protected View getErrorView() {
        //return mInflater.inflate(R.layout.appwidget_error, this, false);
        return null;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Consume any touch events for ourselves after longpress is triggered
        if (mHasPerformedLongPress) {
            mHasPerformedLongPress = false;
            return true;
        }

        // Watch for longpress events at this level to make sure
        // users can always pick up this widget.
        //
        // In DOWN and MOVE, we save our X and Y values to check whether user is swiping or tapping.
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                oldX = ev.getX();
                oldY = ev.getY();
                postCheckForLongClick();
                break;

            case MotionEvent.ACTION_MOVE:
                newX = ev.getX();
                newY = ev.getY();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelLongPress();
                break;

            default:
                // No-op.
                break;
        }

        // Otherwise continue letting touch events fall through to children
        return false;
    }

    private void postCheckForLongClick() {
        mHasPerformedLongPress = false;
        checkForLongPress();
    }

    @Override public void cancelLongPress() {
        super.cancelLongPress();
        mHasPerformedLongPress = false;
    }

    @Override public int getDescendantFocusability() {
        return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
    }

    private void checkForLongPress() {
        final int maxDistance = 10;
        int timeout = ViewConfiguration.getLongPressTimeout();

        new CountDownTimer(timeout, timeout) {
            public void onTick(long millisUntilFinished) {
                // No-op.
            }

            public void onFinish() {
                if ((Math.abs(newX - oldX) < maxDistance)
                        && (Math.abs(newY - oldY) < maxDistance)
                        && performLongClick() && !mHasPerformedLongPress) {
                    mHasPerformedLongPress = true;
                }
            }
        }.start();
    }
}
