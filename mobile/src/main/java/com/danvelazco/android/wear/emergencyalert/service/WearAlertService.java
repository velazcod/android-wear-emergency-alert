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

package com.danvelazco.android.wear.emergencyalert.service;

import android.accessibilityservice.AccessibilityService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import com.danvelazco.android.wear.emergencyalert.AlertPreferencesActivity;
import com.danvelazco.android.wear.emergencyalert.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

/**
 * @author Daniel Velazco <velazcod@gmail.com>
 * @since 7/2/14
 */
public class WearAlertService extends AccessibilityService implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {

    // Constants
    private final static int ALERT_NOTIFICATION_ID = 0x6001;
    private final static int MSG_TRIGGER_ALERT = 0x5001;
    public final static String SEND_EMERGENCY_ALERT_SMS_PATH = "/start/sendEmergencyAlert";

    // Members
    private GoogleApiClient mGoogleApiClient = null;
    private SharedPreferences mSharedPrefs = null;
    private NotificationManager mNotificationManager = null;
    private LocationManager mLocationManager;
    private String mTempSmsNumberToSendLocationTo = null;

    @Override
    public void onCreate() {
        super.onCreate();

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Create the GoogleApiClient object we'll be using to connect in order to use the Wear API
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        Toast.makeText(this, "Service started", Toast.LENGTH_LONG).show();

    }

    @Override
    public void onDestroy() {
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No implementation
    }

    @Override
    public void onInterrupt() {
        // No implementation
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }

        if (location != null && mTempSmsNumberToSendLocationTo != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            String message = String.format(getString(R.string.message_current_location),
                    Double.toString(latitude), Double.toString(longitude),
                    getFormattedTimestamp(location.getTime()));
            sendSms(mTempSmsNumberToSendLocationTo, message);
            mTempSmsNumberToSendLocationTo = null;
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // No implementation
    }

    @Override
    public void onProviderEnabled(String provider) {
        // No implementation
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // No implementation
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // No implementation
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(SEND_EMERGENCY_ALERT_SMS_PATH)) {
            mMessageHandler.sendEmptyMessage(MSG_TRIGGER_ALERT);
        }
    }

    private String getFormattedTimestamp(long timestamp) {
        return DateUtils.formatDateTime(this, timestamp,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
                        | DateUtils.FORMAT_SHOW_TIME
        );
    }

    private void triggerAlert() {
        final String smsNumber = mSharedPrefs.getString(AlertPreferencesActivity.PREF_KEY_SMS_NUMBER, null);
        final String smsMessage = mSharedPrefs.getString(AlertPreferencesActivity.PREF_KEY_SMS_MESSAGE,
                getString(R.string.default_emergency_message));
        boolean sendLocation = mSharedPrefs.getBoolean(AlertPreferencesActivity.PREF_KEY_SMS_MESSAGE_LOCATION,
                true);
        boolean showNotification = mSharedPrefs.getBoolean(AlertPreferencesActivity.PREF_KEY_SHOW_NOTIFICATION,
                true);

        if (!TextUtils.isEmpty(smsNumber) && !TextUtils.isEmpty(smsMessage)) {
            if (showNotification) {
                showNotification();
            }

            sendSms(smsNumber, smsMessage);

            if (sendLocation) {
                Location location = mLocationManager
                        .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    String message = String.format(getString(R.string.message_last_location),
                            Double.toString(latitude), Double.toString(longitude),
                            getFormattedTimestamp(location.getTime()));
                    sendSms(smsNumber, message);
                }

                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                String locationProvider = mLocationManager.getBestProvider(criteria, true);
                if (locationProvider != null) {
                    mTempSmsNumberToSendLocationTo = smsNumber;
                    mLocationManager.requestLocationUpdates(locationProvider,
                            300000,     /** time intervals in ms (5 mins) **/
                            10,         /** min distance in meters **/
                            WearAlertService.this);
                }
            }
        }
    }

    private void sendSms(String number, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(number, null, message, null, null);
    }

    private void showNotification() {
        removeNotification();

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(getString(R.string.notification_title))
                        .setContentText(getString(R.string.notification_content))
                        .setAutoCancel(true)
                        //.setVisibility(NotificationCompat.VISIBILITY_SECRET)
                        .setLocalOnly(true);

        Intent notificationIntent = new Intent(this, AlertPreferencesActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        // Add as notification
        if (mNotificationManager != null) {
            mNotificationManager.notify(ALERT_NOTIFICATION_ID, builder.build());
        }
    }

    private void removeNotification() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(ALERT_NOTIFICATION_ID);
        }
    }

    private Handler mMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_TRIGGER_ALERT:
                    triggerAlert();
            }
        }
    };

}
