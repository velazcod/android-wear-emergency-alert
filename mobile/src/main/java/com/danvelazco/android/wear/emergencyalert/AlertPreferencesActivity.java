/*
 * Copyright (C) 2014 Daniel Velazco
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

package com.danvelazco.android.wear.emergencyalert;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import com.danvelazco.android.wear.emergencyalert.data.PreferencesData;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * @author Daniel Velazco <velazcod@gmail.com>
 * @since 7/2/14
 */
public class AlertPreferencesActivity extends AppCompatActivity {

    // Preference Keys
    public static final String PREF_KEY_SMS_NUMBER = "_contact_phone_number";
    public static final String PREF_KEY_SMS_MESSAGE = "_sms_emergency_message";
    public static final String PREF_KEY_SMS_MESSAGE_LOCATION = "_sms_send_location";
    public static final String PREF_KEY_SHOW_NOTIFICATION = "_show_notification";
    public static final String PREF_KEY_USE_CONFIRMATION_BTN = "_use_confirmation_button";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_preferences);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, AlertsPreferenceFragment.newInstance())
                    .commit();
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class AlertsPreferenceFragment extends PreferenceFragment implements
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        // Members
        private GoogleApiClient mGoogleApiClient = null;

        // Preferences
        private CheckBoxPreference mmPrefUseConfirmationBtn = null;
        private EditTextPreference mmPrefSmsNumber = null;
        private EditTextPreference mmPrefSmsMessage = null;

        /**
         * Create a new instance of this fragment
         *
         * @return {@link AlertsPreferenceFragment}
         */
        public static AlertsPreferenceFragment newInstance() {
            return new AlertsPreferenceFragment();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs_alert_config);

            mmPrefUseConfirmationBtn = (CheckBoxPreference) findPreference(
                    PREF_KEY_USE_CONFIRMATION_BTN);
            mmPrefUseConfirmationBtn.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    setUseConfirmationButtonPreference((Boolean) newValue);
                    return true;
                }
            });

            mmPrefSmsNumber = (EditTextPreference) findPreference(PREF_KEY_SMS_NUMBER);
            if (mmPrefSmsNumber != null) {
                if (!TextUtils.isEmpty(mmPrefSmsNumber.getText())) {
                    mmPrefSmsNumber.setSummary(mmPrefSmsNumber.getText());
                }
                mmPrefSmsNumber.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String newProxyHostValue = (String) newValue;
                        mmPrefSmsNumber.setSummary(newProxyHostValue);
                        return true;
                    }
                });
            }

            mmPrefSmsMessage = (EditTextPreference) findPreference(PREF_KEY_SMS_MESSAGE);
            if (mmPrefSmsMessage != null) {
                if (!TextUtils.isEmpty(mmPrefSmsMessage.getText())) {
                    mmPrefSmsMessage.setSummary(mmPrefSmsMessage.getText());
                }
                mmPrefSmsMessage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String newProxyPortValue = (String) newValue;
                        mmPrefSmsMessage.setSummary(newProxyPortValue);
                        return true;
                    }
                });
            }

            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onResume() {
            super.onResume();

            if (mmPrefSmsNumber != null) {
                if (!TextUtils.isEmpty(mmPrefSmsNumber.getText())) {
                    mmPrefSmsNumber.setSummary(mmPrefSmsNumber.getText());
                }
            }
            if (mmPrefSmsMessage != null) {
                if (!TextUtils.isEmpty(mmPrefSmsMessage.getText())) {
                    mmPrefSmsMessage.setSummary(mmPrefSmsMessage.getText());
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onDestroy() {
            if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
            super.onDestroy();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onConnected(Bundle bundle) {
            if (mmPrefUseConfirmationBtn != null) {
                setUseConfirmationButtonPreference(mmPrefUseConfirmationBtn.isChecked());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onConnectionSuspended(int i) {
            // Not used
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            // Not used
        }

        /**
         * Set the new value for the preference for using a confirmation button on the watch app
         *
         * @param newValue
         *         {@link boolean}
         */
        private void setUseConfirmationButtonPreference(boolean newValue) {
            (new SetPreferenceTask()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, newValue);
        }

        private class SetPreferenceTask extends AsyncTask<Boolean, Void, Boolean> {

            /**
             * {@inheritDoc}
             */
            @Override
            protected Boolean doInBackground(Boolean... params) {
                if (params.length != 1) {
                    throw new IllegalArgumentException("At least one boolean value must be set, but only one.");
                }

                if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {
                    PutDataMapRequest dataMap = PreferencesData.toDataMap(params[0]);
                    PutDataRequest request = dataMap.asPutDataRequest();
                    Wearable.DataApi.putDataItem(mGoogleApiClient, request);
                    return true;
                }
                return false;
            }
        }

    }

}
