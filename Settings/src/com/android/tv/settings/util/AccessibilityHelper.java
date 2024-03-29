/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.tv.settings.util;

import android.content.Context;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodManager;

/**
 * Static utility class for common accessibility functions.
 */
public class AccessibilityHelper {

    /**
     * If accessibility manager is enabled, we should force text views to be focusable.
     */
    public static boolean forceFocusableViews(Context context) {
        AccessibilityManager accessMan = (AccessibilityManager) context.
                getSystemService(Context.ACCESSIBILITY_SERVICE);
        return accessMan.isEnabled();
    }

    public static void dismissKeyboard(Context context, View view) {
        InputMethodManager imm = context.getSystemService(InputMethodManager.class);
        imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
    }
}
