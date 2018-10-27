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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class LauncherAppWidgetHostView extends AppWidgetHostView {
    private final GestureDetector gestureDetector;
    //private LayoutInflater mInflater;

    public LauncherAppWidgetHostView(Context context) {
        super(context);
        gestureDetector = new GestureDetector(context, new GestureListener());
        //mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override protected View getErrorView() {
        //return mInflater.inflate(R.layout.appwidget_error, this, false);
        return null;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    @Override public void cancelLongPress() {
        super.cancelLongPress();
    }

    @Override public int getDescendantFocusability() {
        return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return performClick();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            performLongClick();
        }
    }
}
