/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.Manifest;
import android.app.AppOpsManager;
import android.app.tvsettings.TvSettingsEnums;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.applications.ApplicationsState;
import com.android.tv.settings.R;

/**
 * Fragment for controlling if apps can install other apps
 */
@Keep
public class ExternalSources extends ManageAppOp {
    private AppOpsManager mAppOpsManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mAppOpsManager = getContext().getSystemService(AppOpsManager.class);
        super.onCreate(savedInstanceState);
    }

    @Override
    public int getAppOpsOpCode() {
        return AppOpsManager.OP_REQUEST_INSTALL_PACKAGES;
    }

    @Override
    public String getPermission() {
        return Manifest.permission.REQUEST_INSTALL_PACKAGES;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.manage_external_sources, null);
    }

    @NonNull
    @Override
    public Preference createAppPreference() {
        return new RestrictedSwitchPreference(getPreferenceManager().getContext());
    }

    @NonNull
    @Override
    public Preference bindPreference(@NonNull Preference preference,
            ApplicationsState.AppEntry entry) {
        final RestrictedSwitchPreference switchPref = (RestrictedSwitchPreference) preference;
        switchPref.setTitle(entry.label);
        switchPref.setKey(entry.info.packageName);
        switchPref.setIcon(entry.icon);
        switchPref.setOnPreferenceChangeListener((pref, newValue) -> {
            findEntriesUsingPackageName(entry.info.packageName)
                    .forEach(packageEntry -> setCanInstallApps(packageEntry, (Boolean) newValue));
            return true;
        });

        PermissionState state = (PermissionState) entry.extraInfo;
        switchPref.setChecked(state.isAllowed());

        EnforcedAdmin admin = checkIfRestrictionEnforcedByAdmin(entry);
        if (admin != null) {
            switchPref.setDisabledByAdmin(admin);
        } else {
            switchPref.setEnabled(canChange(entry));
            switchPref.setSummary(getPreferenceSummary(entry));
        }

        return switchPref;
    }

    private EnforcedAdmin checkIfRestrictionEnforcedByAdmin(ApplicationsState.AppEntry entry) {
        int userId = UserHandle.getUserId(entry.info.uid);
        EnforcedAdmin admin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(getContext(),
                UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, userId);

        if (admin != null) {
            return admin;
        }

        return RestrictedLockUtilsInternal.checkIfRestrictionEnforced(getContext(),
                UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY, userId);
    }

    private boolean canChange(ApplicationsState.AppEntry entry) {
        final UserHandle userHandle = UserHandle.getUserHandleForUid(entry.info.uid);
        final UserManager um = UserManager.get(getContext());
        return !um.hasBaseUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, userHandle)
                && !um.hasBaseUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY,
                        userHandle);
    }

    private CharSequence getPreferenceSummary(ApplicationsState.AppEntry entry) {
        if (!canChange(entry)) {
            return getContext().getString(R.string.disabled);
        }

        return getContext().getString(((PermissionState) entry.extraInfo).isAllowed()
                ? R.string.external_source_trusted
                : R.string.external_source_untrusted);
    }

    private void setCanInstallApps(ApplicationsState.AppEntry entry, boolean newState) {
        mAppOpsManager.setMode(AppOpsManager.OP_REQUEST_INSTALL_PACKAGES,
                entry.info.uid, entry.info.packageName,
                newState ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_ERRORED);
        updateAppList();
    }

    @NonNull
    @Override
    public PreferenceGroup getAppPreferenceGroup() {
        return getPreferenceScreen();
    }

    @Override
    protected int getPageId() {
        return TvSettingsEnums.APPS_SECURITY_RESTRICTIONS_UNKNOWN_SOURCES;
    }
}
