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

package com.android.tv.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.robolectric.shadow.api.Shadow.extract;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.icu.text.MessageFormat;
import android.telephony.SignalStrength;

import androidx.preference.Preference;

import com.android.tv.settings.connectivity.ConnectivityListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAccountManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class MainFragmentTest {

    @Spy
    private MainFragment mMainFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(RuntimeEnvironment.application).when(mMainFragment).getContext();
    }

    @Test
    public void testUpdateConnectivity_NoNetwork() {
        final Preference networkPref = mock(Preference.class);
        doReturn(networkPref).when(mMainFragment).findPreference(MainFragment.KEY_NETWORK);
        final ConnectivityListener listener = mock(ConnectivityListener.class);
        mMainFragment.mConnectivityListenerOptional = Optional.of(listener);

        doReturn(false).when(listener).isEthernetAvailable();
        doReturn(false).when(listener).isCellConnected();
        doReturn(false).when(listener).isEthernetConnected();
        doReturn(false).when(listener).isWifiEnabledOrEnabling();

        mMainFragment.updateConnectivity();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_signal_off_white);
    }

    @Test
    public void testUpdateConnectivity_hasEthernet() {
        final Preference networkPref = mock(Preference.class);
        doReturn(networkPref).when(mMainFragment).findPreference(MainFragment.KEY_NETWORK);
        final ConnectivityListener listener = mock(ConnectivityListener.class);
        mMainFragment.mConnectivityListenerOptional = Optional.of(listener);

        doReturn(true).when(listener).isEthernetAvailable();
        doReturn(false).when(listener).isCellConnected();
        doReturn(false).when(listener).isEthernetConnected();
        doReturn(false).when(listener).isWifiEnabledOrEnabling();

        mMainFragment.updateConnectivity();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_signal_off_white);
    }

    @Test
    public void testUpdateConnectivity_hasEthernetConnected() {
        final Preference networkPref = mock(Preference.class);
        doReturn(networkPref).when(mMainFragment).findPreference(MainFragment.KEY_NETWORK);
        final ConnectivityListener listener = mock(ConnectivityListener.class);
        mMainFragment.mConnectivityListenerOptional = Optional.of(listener);

        doReturn(true).when(listener).isEthernetAvailable();
        doReturn(false).when(listener).isCellConnected();
        doReturn(true).when(listener).isEthernetConnected();
        doReturn(false).when(listener).isWifiEnabledOrEnabling();

        mMainFragment.updateConnectivity();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_ethernet_white);
    }

    @Test
    public void testUpdateConnectivity_wifiSignal() {
        final Preference networkPref = mock(Preference.class);
        doReturn(networkPref).when(mMainFragment).findPreference(MainFragment.KEY_NETWORK);
        final ConnectivityListener listener = mock(ConnectivityListener.class);
        mMainFragment.mConnectivityListenerOptional = Optional.of(listener);

        doReturn(false).when(listener).isEthernetAvailable();
        doReturn(false).when(listener).isCellConnected();
        doReturn(false).when(listener).isEthernetConnected();
        doReturn(true).when(listener).isWifiEnabledOrEnabling();
        doReturn(true).when(listener).isWifiConnected();
        doReturn(0).when(listener).getWifiSignalStrength(anyInt());

        mMainFragment.updateConnectivity();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_signal_0_white);

        doReturn(1).when(listener).getWifiSignalStrength(anyInt());

        mMainFragment.updateConnectivity();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_signal_1_white);

        doReturn(2).when(listener).getWifiSignalStrength(anyInt());

        mMainFragment.updateConnectivity();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_signal_2_white);

        doReturn(3).when(listener).getWifiSignalStrength(anyInt());

        mMainFragment.updateConnectivity();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_signal_3_white);

        doReturn(4).when(listener).getWifiSignalStrength(anyInt());

        mMainFragment.updateConnectivity();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_signal_4_white);

        doReturn(false).when(listener).isWifiConnected();

        mMainFragment.updateConnectivity();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_not_connected);
    }

    @Test
    public void testUpdateConnectivity_notConnected() {
        final Preference networkPref = mock(Preference.class);
        doReturn(networkPref).when(mMainFragment).findPreference(MainFragment.KEY_NETWORK);
        final ConnectivityListener listener = mock(ConnectivityListener.class);
        mMainFragment.mConnectivityListenerOptional = Optional.of(listener);

        doReturn(false).when(listener).isEthernetAvailable();
        doReturn(false).when(listener).isCellConnected();
        doReturn(false).when(listener).isEthernetConnected();
        doReturn(false).when(listener).isWifiEnabledOrEnabling();

        mMainFragment.updateConnectivity();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_wifi_signal_off_white);
    }

    @Test
    public void testUpdateConnectivity_cellSignal() {
        final Preference networkPref = mock(Preference.class);
        doReturn(networkPref).when(mMainFragment).findPreference(MainFragment.KEY_NETWORK);
        final ConnectivityListener listener = mock(ConnectivityListener.class);
        mMainFragment.mConnectivityListenerOptional = Optional.of(listener);

        doReturn(false).when(listener).isEthernetAvailable();
        doReturn(true).when(listener).isCellConnected();
        doReturn(false).when(listener).isEthernetConnected();
        doReturn(false).when(listener).isWifiEnabledOrEnabling();
        doReturn(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN)
                .when(listener).getCellSignalStrength();

        mMainFragment.updateConnectivity();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_cell_signal_0_white);

        doReturn(SignalStrength.SIGNAL_STRENGTH_POOR)
                .when(listener).getCellSignalStrength();

        mMainFragment.updateConnectivity();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_cell_signal_1_white);

        doReturn(SignalStrength.SIGNAL_STRENGTH_MODERATE)
                .when(listener).getCellSignalStrength();

        mMainFragment.updateConnectivity();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_cell_signal_2_white);

        doReturn(SignalStrength.SIGNAL_STRENGTH_GOOD)
                .when(listener).getCellSignalStrength();

        mMainFragment.updateConnectivity();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_cell_signal_3_white);

        doReturn(SignalStrength.SIGNAL_STRENGTH_GREAT)
                .when(listener).getCellSignalStrength();

        mMainFragment.updateConnectivity();

        verify(networkPref, atLeastOnce()).setIcon(R.drawable.ic_cell_signal_4_white);
    }

    @Test
    public void testUpdateAccountPrefInfo_hasOneAccount() {
        final Preference accountsPref = mock(Preference.class);
        doReturn(accountsPref).when(mMainFragment)
                    .findPreference(MainFragment.KEY_ACCOUNTS_AND_SIGN_IN);
        doReturn(true).when(accountsPref).isVisible();
        ShadowAccountManager am = extract(AccountManager.get(RuntimeEnvironment.application));
        am.addAccount(new Account("test", "test"));

        mMainFragment.updateAccountPrefInfo();

        verify(accountsPref, atLeastOnce()).setIcon(R.drawable.ic_accounts_and_sign_in);
        verify(accountsPref, atLeastOnce()).setSummary("test");
        assertTrue(mMainFragment.mHasAccounts);
    }

    @Test
    public void testUpdateAccountPrefInfo_hasNoAccount() {
        final Preference accountsPref = mock(Preference.class);
        doReturn(accountsPref).when(mMainFragment)
                    .findPreference(MainFragment.KEY_ACCOUNTS_AND_SIGN_IN);
        doReturn(true).when(accountsPref).isVisible();

        mMainFragment.updateAccountPrefInfo();

        verify(accountsPref, atLeastOnce()).setIcon(R.drawable.ic_add_an_account);
        verify(accountsPref, atLeastOnce())
                .setSummary(R.string.accounts_category_summary_no_account);
        assertFalse(mMainFragment.mHasAccounts);
    }

    @Test
    public void testUpdateAccountPrefInfo_hasMoreThanOneAccount() {
        final Preference accountsPref = mock(Preference.class);
        doReturn(RuntimeEnvironment.application.getResources()).when(mMainFragment).getResources();
        doReturn(accountsPref).when(mMainFragment)
                .findPreference(MainFragment.KEY_ACCOUNTS_AND_SIGN_IN);
        doReturn(true).when(accountsPref).isVisible();
        ShadowAccountManager am = extract(AccountManager.get(RuntimeEnvironment.application));
        am.addAccount(new Account("test", "test"));
        am.addAccount(new Account("test2", "test2"));

        mMainFragment.updateAccountPrefInfo();

        verify(accountsPref, atLeastOnce()).setIcon(R.drawable.ic_accounts_and_sign_in);
        MessageFormat msgFormat = new MessageFormat(
                RuntimeEnvironment.application.getResources().getString(
                        R.string.accounts_category_summary),
                Locale.getDefault());
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("count", 2);
        String summary = msgFormat.format(arguments);
        verify(accountsPref, atLeastOnce()).setSummary(summary);
        assertTrue(mMainFragment.mHasAccounts);
    }

    @Test
    public void testUpdateAccessoryPref_hasNoAccessory() {
        final Preference accessoryPref = mock(Preference.class);
        doReturn(accessoryPref).when(mMainFragment)
                .findPreference(MainFragment.KEY_ACCESSORIES);
        mMainFragment.mBtAdapter = mock(BluetoothAdapter.class);
        Set<BluetoothDevice> set = new HashSet<>();
        doReturn(set).when(mMainFragment.mBtAdapter).getBondedDevices();

        mMainFragment.updateAccessoryPref();

        assertFalse(mMainFragment.mHasBtAccessories);
    }

    @Test
    public void testUpdateAccessoryPref_hasAccessories() {
        final Preference accessoryPref = mock(Preference.class);
        doReturn(accessoryPref).when(mMainFragment)
                .findPreference(MainFragment.KEY_ACCESSORIES);
        mMainFragment.mBtAdapter = mock(BluetoothAdapter.class);
        Set<BluetoothDevice> set = new HashSet<>();
        BluetoothDevice device = mock(BluetoothDevice.class);
        doReturn("testDevice").when(device).getAlias();
        set.add(device);
        doReturn(set).when(mMainFragment.mBtAdapter).getBondedDevices();

        mMainFragment.updateAccessoryPref();

        assertTrue(mMainFragment.mHasBtAccessories);
    }

    @Test
    public void
            updateConnectivity_givenConnectivityListenerNull_thenNetworkPreferenceIsNotUpdated() {
        /*
        Currently this is only a unit test that verifies that MainFragment
        does not try to update network preference when ConnectivityListener
        is null.
        TODO:
        Add test to verify that ConnectivityListener is null when wifi scan
        optimisation is enabled.
         */
        final Preference networkPreference = mock(Preference.class);
        doReturn(networkPreference).when(mMainFragment).findPreference(MainFragment.KEY_NETWORK);
        mMainFragment.mConnectivityListenerOptional = Optional.empty();

        mMainFragment.updateConnectivity();

        verifyNoInteractions(networkPreference);
    }
}
