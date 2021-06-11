/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.tv.settings.library.device.apps;

import android.content.Context;
import android.text.format.Formatter;

import com.android.tv.settings.library.PreferenceCompat;
import com.android.tv.settings.library.UIUpdateCallback;
import com.android.tv.settings.library.data.PreferenceCompatManager;
import com.android.tv.settings.library.util.ResourcesUtil;

/** Preference controller to handle clear cache preference. */
public class ClearCachePreferenceController extends AppActionPreferenceController {
    private static final String KEY_CLEAR_CACHE = "clearCache";
    private boolean mClearingCache;
    private PreferenceCompat mClearDataPreference;

    public ClearCachePreferenceController(Context context,
            UIUpdateCallback callback, int stateIdentifier,
            ApplicationsState.AppEntry appEntry) {
        super(context, callback, stateIdentifier, appEntry);
    }

    @Override
    public void displayPreference(PreferenceCompatManager screen) {
        mClearDataPreference = screen.getOrCreatePrefCompat(getPreferenceKey());
        super.displayPreference(screen);
    }

    public void setClearingCache(boolean clearingCache) {
        mClearingCache = clearingCache;
        refresh();
    }

    @Override
    void refresh() {
        if (mAppEntry == null) {
            return;
        }
        mClearDataPreference.setTitle(ResourcesUtil.getString(
                mContext, "device_apps_app_management_clear_cache"));
        mClearDataPreference.setSummary(mClearingCache
                ? ResourcesUtil.getString(mContext, "computing_size")
                : Formatter.formatFileSize(mContext,
                        mAppEntry.cacheSize + mAppEntry.externalCacheSize));
        mClearDataPreference.setEnabled(!mClearingCache && mAppEntry.cacheSize > 0);
        mUIUpdateCallback.notifyUpdate(mStateIdentifier, mClearDataPreference);
    }

    @Override
    public String[] getPreferenceKey() {
        return new String[]{KEY_CLEAR_CACHE};
    }
}