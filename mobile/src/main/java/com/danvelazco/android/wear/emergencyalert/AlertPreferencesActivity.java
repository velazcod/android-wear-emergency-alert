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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import java.util.List;

public class AlertPreferencesActivity extends Activity {

    // Constants
    private static final String LOG_TAG = "AlertPreferencesActivity";
    private static final String ACCESSIBILITY_SERVICE_COMPONENT_NAME =
            "com.danvelazco.android.wear.emergencyalert/.service.WearAlertService";

    // Preference Keys
    public static final String PREF_KEY_ASKED_USER_ENABLE_SERVICE = "_asked_user_enable_service";
    public static final String PREF_KEY_ENABLE_SERVICE = "_enable_service";
    public static final String PREF_KEY_SMS_NUMBER = "_contact_phone_number";
    public static final String PREF_KEY_SMS_MESSAGE = "_sms_emergency_message";
    public static final String PREF_KEY_SMS_MESSAGE_LOCATION = "_sms_send_location";
    public static final String PREF_KEY_SHOW_NOTIFICATION = "_show_notification";

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
    public static class AlertsPreferenceFragment extends PreferenceFragment
            implements Preference.OnPreferenceClickListener {

        // Members
        private SharedPreferences mmSharedPreferences = null;

        // Preferences
        private PreferenceScreen mmPrefEnableService = null;
        private EditTextPreference mmPrefSmsNumber = null;
        private EditTextPreference mmPrefSmsMessage = null;

        public static AlertsPreferenceFragment newInstance() {
            return new AlertsPreferenceFragment();
        }

        public AlertsPreferenceFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs_alert_config);

            mmSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

            mmPrefEnableService = (PreferenceScreen) findPreference(PREF_KEY_ENABLE_SERVICE);
            if (mmPrefEnableService != null) {
                mmPrefEnableService.setOnPreferenceClickListener(this);
            }

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
        }

        @Override
        public void onResume() {
            super.onResume();

            boolean askedUserToEnableService = mmSharedPreferences.getBoolean(PREF_KEY_ASKED_USER_ENABLE_SERVICE, false);
            boolean accessibilityEnabled = isAccessibilityEnabled(getActivity(), ACCESSIBILITY_SERVICE_COMPONENT_NAME);
            if (!askedUserToEnableService && !accessibilityEnabled) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getString(R.string.app_name));
                builder.setIcon(R.drawable.ic_launcher);
                builder.setMessage(getString(R.string.enable_service_msg));
                builder.setPositiveButton(R.string.enable_service_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefEnableService();

                        SharedPreferences.Editor editor = mmSharedPreferences.edit();
                        editor.putBoolean(PREF_KEY_ASKED_USER_ENABLE_SERVICE, true);
                        editor.apply();

                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(R.string.enable_service_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }

            if (mmPrefEnableService != null) {
                if (!accessibilityEnabled) {
                    mmPrefEnableService.setSummary(getString(R.string.pref_enable_service_sry_disabled));
                } else {
                    mmPrefEnableService.setSummary(getString(R.string.pref_enable_service_sry_enabled));
                }
            }

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

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String prefKey = preference.getKey();
            if (PREF_KEY_ENABLE_SERVICE.equals(prefKey)) {
                prefEnableService();
                return true;
            }

            return false;
        }

        public void prefEnableService() {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            Toast.makeText(getActivity(), String.format(getString(R.string.toast_enable_service_msg),
                    getString(R.string.app_name)), Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Check whether or not a specific accessibility service is enabled.
     *
     * @param context {@link Context}
     * @param id {@link String}
     * @return {@link boolean}
     */
    public static boolean isAccessibilityEnabled(Context context, String id) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> runningServices = am
                .getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
        for (AccessibilityServiceInfo service : runningServices) {
            if (id.equalsIgnoreCase(service.getId())) {
                return true;
            }
        }
        return false;
    }

}
