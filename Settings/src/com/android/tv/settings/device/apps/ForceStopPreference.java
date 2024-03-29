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
 * limitations under the License
 */

package com.android.tv.settings.device.apps;

import static com.android.tv.settings.util.InstrumentationUtils.logEntrySelected;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.tvsettings.TvSettingsEnums;
import android.apphibernation.AppHibernationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.leanback.widget.GuidanceStylist;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.applications.ApplicationsState;
import com.android.tv.settings.R;

public class ForceStopPreference extends AppActionPreference {

    public ForceStopPreference(Context context, ApplicationsState.AppEntry entry) {
        super(context, entry);
        ConfirmationFragment.prepareArgs(getExtras(), mEntry.info.packageName);
        refresh();

        UserManager userManager = getContext().getSystemService(UserManager.class);
        if (userManager.hasUserRestriction(UserManager.DISALLOW_APPS_CONTROL)) {
            final RestrictedLockUtils.EnforcedAdmin admin =
                    RestrictedLockUtilsInternal.checkIfRestrictionEnforced(context,
                            UserManager.DISALLOW_APPS_CONTROL, UserHandle.myUserId());
            if (admin != null) {
                setDisabledByAdmin(admin);
            } else {
                setEnabled(false);
            }
        }
    }

    public void refresh() {
        setTitle(R.string.device_apps_app_management_force_stop);
        DevicePolicyManager dpm = (DevicePolicyManager) getContext().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        AppHibernationManager ahm = getContext().getSystemService(
                AppHibernationManager.class);
        boolean isPackageHibernated = ahm.isHibernatingForUser(mEntry.info.packageName);
        if (dpm.packageHasActiveAdmins(mEntry.info.packageName)) {
            // User can't force stop device admin.
            setVisible(false);
        } else if (isPackageHibernated) {
            // Hibernated apps are always stopped.
            setVisible(false);
        } else if ((mEntry.info.flags & ApplicationInfo.FLAG_STOPPED) == 0) {
            // If the app isn't explicitly stopped, then always show the
            // force stop action.
            setVisible(true);
        } else {
            Intent intent = new Intent(Intent.ACTION_QUERY_PACKAGE_RESTART,
                    Uri.fromParts("package", mEntry.info.packageName, null));
            intent.putExtra(Intent.EXTRA_PACKAGES, new String[] {
                    mEntry.info.packageName });
            intent.putExtra(Intent.EXTRA_UID, mEntry.info.uid);
            intent.putExtra(Intent.EXTRA_USER_HANDLE, UserHandle.getUserId(mEntry.info.uid));
            getContext().sendOrderedBroadcast(intent,
                    android.Manifest.permission.HANDLE_QUERY_PACKAGE_RESTART,
                        new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            setVisible(getResultCode() != Activity.RESULT_CANCELED);
                        }
                    }, null, Activity.RESULT_CANCELED, null, null);
        }
        this.setOnPreferenceClickListener(
                preference -> {
                    logEntrySelected(TvSettingsEnums.APPS_ALL_APPS_APP_ENTRY_FORCE_STOP);
                    return false;
                });
    }

    @Override
    public String getFragment() {
        return ConfirmationFragment.class.getName();
    }

    public static class ConfirmationFragment extends AppActionPreference.ConfirmationFragment {
        private static final String ARG_PACKAGE_NAME = "packageName";

        private static void prepareArgs(@NonNull Bundle args, String packageName) {
            args.putString(ARG_PACKAGE_NAME, packageName);
        }

        @NonNull
        @Override
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            final AppManagementFragment fragment = (AppManagementFragment) getTargetFragment();
            return new GuidanceStylist.Guidance(
                    getString(R.string.device_apps_app_management_force_stop),
                    getString(R.string.device_apps_app_management_force_stop_desc),
                    fragment.getAppName(),
                    fragment.getAppIcon());
        }

        @Override
        public void onOk() {
            String pkgName = getArguments().getString(ARG_PACKAGE_NAME);
            ActivityManager am = (ActivityManager)
                    getActivity().getSystemService(Context.ACTIVITY_SERVICE);
            am.forceStopPackage(pkgName);
        }
    }
}
