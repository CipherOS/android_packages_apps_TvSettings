/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.tv.settings.device.apps.specialaccess;

import android.app.AppOpsManager;
import android.app.tvsettings.TvSettingsEnums;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.applications.ApplicationsState;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.widget.SwitchWithSoundPreference;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Fragment for managing which apps are granted PIP access
 */
@Keep
public class PictureInPicture extends SettingsPreferenceFragment
        implements ManageApplicationsController.Callback {
    private static final String TAG = "PictureInPicture";

    private ManageApplicationsController mManageApplicationsController;
    private AppOpsManager mAppOpsManager;

    private final ApplicationsState.AppFilter mFilter =
            new ApplicationsState.CompoundFilter(
                    new ApplicationsState.CompoundFilter(
                            ApplicationsState.FILTER_WITHOUT_DISABLED_UNTIL_USED,
                            ApplicationsState.FILTER_ALL_ENABLED),

                    new ApplicationsState.AppFilter() {
                        @Override
                        public void init() {}

                        @Override
                        public boolean filterApp(ApplicationsState.AppEntry info) {
                            info.extraInfo = mAppOpsManager.checkOpNoThrow(
                                    AppOpsManager.OP_PICTURE_IN_PICTURE,
                                    info.info.uid,
                                    info.info.packageName) == AppOpsManager.MODE_ALLOWED;
                            return !ManageAppOp.shouldIgnorePackage(
                                    getContext(), info.info.packageName, 0)
                                    && checkPackageHasPipActivities(info.info.packageName);
                        }
                    });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppOpsManager = getContext().getSystemService(AppOpsManager.class);
        mManageApplicationsController = new ManageApplicationsController(getContext(), this,
                getLifecycle(), mFilter, ApplicationsState.ALPHA_COMPARATOR);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.picture_in_picture, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        mManageApplicationsController.updateAppList();
    }

    private boolean checkPackageHasPipActivities(String packageName) {
        try {
            final PackageInfo packageInfo = getContext().getPackageManager().getPackageInfo(
                    packageName, PackageManager.GET_ACTIVITIES);
            if (packageInfo.activities == null) {
                return false;
            }
            for (ActivityInfo info : packageInfo.activities) {
                if (info.supportsPictureInPicture()) {
                    return true;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Exception while fetching package info for " + packageName, e);
            return false;
        }
        return false;
    }

    @NonNull
    @Override
    public Preference bindPreference(@NonNull Preference preference,
            ApplicationsState.AppEntry entry) {
        final TwoStatePreference switchPref = (SwitchWithSoundPreference) preference;
        switchPref.setTitle(entry.label);
        switchPref.setKey(entry.info.packageName);
        switchPref.setIcon(entry.icon);
        switchPref.setChecked((Boolean) entry.extraInfo);
        switchPref.setOnPreferenceChangeListener((pref, newValue) -> {
            findEntriesUsingPackageName(entry.info.packageName)
                    .forEach(packageEntry -> setPiPMode(entry, (Boolean) newValue));
            return true;
        });
        switchPref.setSummaryOn(R.string.app_permission_summary_allowed);
        switchPref.setSummaryOff(R.string.app_permission_summary_not_allowed);
        return switchPref;
    }

    private void setPiPMode(ApplicationsState.AppEntry entry, boolean newValue) {
        mAppOpsManager.setMode(AppOpsManager.OP_PICTURE_IN_PICTURE,
                entry.info.uid,
                entry.info.packageName,
                newValue ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_ERRORED);
    }

    private List<ApplicationsState.AppEntry> findEntriesUsingPackageName(String packageName) {
        return mManageApplicationsController.getApps().stream()
                .filter(entry -> entry.info.packageName.equals(packageName))
                .collect(Collectors.toList());
    }

    @NonNull
    @Override
    public Preference createAppPreference() {
        return new SwitchWithSoundPreference(getPreferenceManager().getContext());
    }

    @NonNull
    @Override
    public Preference getEmptyPreference() {
        final Preference empty = new Preference(getPreferenceManager().getContext());
        empty.setKey("empty");
        empty.setTitle(R.string.picture_in_picture_empty_text);
        empty.setEnabled(false);
        return empty;
    }

    @NonNull
    @Override
    public PreferenceGroup getAppPreferenceGroup() {
        return getPreferenceScreen();
    }

    @Override
    protected int getPageId() {
        return TvSettingsEnums.APPS_SPECIAL_APP_ACCESS_PICTURE_IN_PICTURE;
    }
}
