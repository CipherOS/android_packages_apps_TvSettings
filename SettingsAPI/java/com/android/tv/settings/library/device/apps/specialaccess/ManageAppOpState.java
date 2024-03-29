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

package com.android.tv.settings.library.device.apps.specialaccess;

import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.util.ArrayUtils;
import com.android.tv.settings.library.PreferenceCompat;
import com.android.tv.settings.library.UIUpdateCallback;
import com.android.tv.settings.library.data.PreferenceControllerState;
import com.android.tv.settings.library.device.apps.ApplicationsState;
import com.android.tv.settings.library.util.AbstractPreferenceController;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Base {@link State} for special app access settings. */
public abstract class ManageAppOpState extends PreferenceControllerState implements
        ManageApplicationsController.Callback {
    private static final String TAG = "ManageAppOps";

    private IPackageManager mIPackageManager;
    private AppOpsManager mAppOpsManager;

    private ManageApplicationsController mManageApplicationsController;

    public ManageAppOpState(Context context,
            UIUpdateCallback callback) {
        super(context, callback);
    }

    @Override
    public void onAttach() {
        super.onAttach();
        mManageApplicationsController = new ManageApplicationsController(mContext,
                getStateIdentifier(), getLifecycle(), getAppFilter(), getAppComparator(), this,
                mUIUpdateCallback);
    }

    @Override
    public void onCreate(Bundle extras) {
        super.onCreate(extras);
        mIPackageManager = ActivityThread.getPackageManager();
        mAppOpsManager = mContext.getSystemService(AppOpsManager.class);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    protected List<AbstractPreferenceController> onCreatePreferenceControllers(Context context) {
        return null;
    }

    /**
     * Subclasses may override this to provide an alternate app filter. The default filter inserts
     * {@link PermissionState} objects into the {@link ApplicationsState.AppEntry#extraInfo} field.
     *
     * @return {@link ApplicationsState.AppFilter}
     */
    @NonNull
    public ApplicationsState.AppFilter getAppFilter() {
        return new ApplicationsState.AppFilter() {
            @Override
            public void init() {
            }

            @Override
            public boolean filterApp(ApplicationsState.AppEntry entry) {
                entry.extraInfo = createPermissionStateFor(entry.info.packageName, entry.info.uid);
                return !shouldIgnorePackage(
                        mContext, entry.info.packageName, customizedIgnoredPackagesArray())
                        && ((PermissionState) entry.extraInfo).isPermissible();
            }
        };
    }

    /** Provide array resource id for customized ignored packages */
    public int customizedIgnoredPackagesArray() {
        return 0;
    }

    /**
     * @return AppOps code
     */
    public abstract int getAppOpsOpCode();

    /**
     * @return Manifest permission string
     */
    public abstract String getPermission();

    private PermissionState createPermissionStateFor(String packageName, int uid) {
        return new PermissionState(
                hasRequestedAppOpPermission(
                        getPermission(), packageName, UserHandle.getUserId(uid)),
                hasPermission(uid),
                getAppOpMode(uid, packageName));
    }

    private boolean hasRequestedAppOpPermission(String permission, String packageName, int userId) {
        try {
            String[] packages = mIPackageManager.getAppOpPermissionPackages(permission, userId);
            return ArrayUtils.contains(packages, packageName);
        } catch (RemoteException exc) {
            Log.e(TAG, "PackageManager dead. Cannot get permission info");
            return false;
        }
    }

    private boolean hasPermission(int uid) {
        try {
            int result = mIPackageManager.checkUidPermission(getPermission(), uid);
            return result == PackageManager.PERMISSION_GRANTED;
        } catch (RemoteException e) {
            Log.e(TAG, "PackageManager dead. Cannot get permission info");
            return false;
        }
    }

    private int getAppOpMode(int uid, String packageName) {
        return mAppOpsManager.checkOpNoThrow(getAppOpsOpCode(), uid, packageName);
    }

    /**
     * Checks for packages that should be ignored for further processing
     */
    static boolean shouldIgnorePackage(Context context, String packageName,
            int customizedIgnoredPackagesArray) {
        if (context == null) {
            return true;
        }
        Set<String> ignoredPackageNames = null;
        if (customizedIgnoredPackagesArray != 0) {
            ignoredPackageNames = Arrays.stream(context.getResources()
                    .getStringArray(customizedIgnoredPackagesArray)).collect(Collectors.toSet());

        }
        return packageName.equals("android")
                || packageName.equals("com.android.systemui")
                || packageName.equals(context.getPackageName())
                || (ignoredPackageNames != null && ignoredPackageNames.contains(packageName));
    }

    /**
     * Subclasses may override this to provide an alternate comparator for sorting apps
     *
     * @return {@link Comparator} for {@link ApplicationsState.AppEntry} objects.
     */
    @Nullable
    public Comparator<ApplicationsState.AppEntry> getAppComparator() {
        return ApplicationsState.ALPHA_COMPARATOR;
    }

    /**
     * Collection of information to be used as {@link ApplicationsState.AppEntry#extraInfo} objects
     */
    public static class PermissionState {
        public final boolean permissionRequested;
        public final boolean permissionGranted;
        public final int appOpMode;

        private PermissionState(boolean permissionRequested, boolean permissionGranted,
                int appOpMode) {
            this.permissionRequested = permissionRequested;
            this.permissionGranted = permissionGranted;
            this.appOpMode = appOpMode;
        }

        /**
         * @return True if the permission is granted
         */
        public boolean isAllowed() {
            if (appOpMode == AppOpsManager.MODE_DEFAULT) {
                return permissionGranted;
            } else {
                return appOpMode == AppOpsManager.MODE_ALLOWED;
            }
        }

        /**
         * @return True if the permission is relevant
         */
        public boolean isPermissible() {
            return appOpMode != AppOpsManager.MODE_DEFAULT || permissionRequested;
        }

        @Override
        public String toString() {
            return "[permissionGranted: " + permissionGranted
                    + ", permissionRequested: " + permissionRequested
                    + ", appOpMode: " + appOpMode
                    + "]";
        }
    }

    /**
     * Call to trigger the app list to update
     */
    public void updateAppList() {
        mManageApplicationsController.updateAppList();
    }

    @NonNull
    @Override
    public PreferenceCompat createAppPreference(
            com.android.tv.settings.library.device.apps.ApplicationsState.AppEntry entry) {
        return null;
    }

    @NonNull
    @Override
    public PreferenceCompat getEmptyPreference() {
        return null;
    }
}
