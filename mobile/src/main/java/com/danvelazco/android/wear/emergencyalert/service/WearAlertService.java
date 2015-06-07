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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import com.danvelazco.android.wear.emergencyalert.AlertPreferencesActivity;
import com.danvelazco.android.wear.emergencyalert.R;
import com.danvelazco.android.wear.emergencyalert.util.SMSUtil;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.lang.ref.WeakReference;

/**
 * Sicne a {@link WearableListenerService} is short-lived and is quickly killed, we try to delegate
 * finding an accurate {@link Location} to the {@link FineLocationSMSIntentService} from here, but
 * only if the option to send location data is enabled by the user.
 *
 * @author Daniel Velazco <velazcod@gmail.com>
 * @since 7/2/14
 */
public class WearAlertService extends WearableListenerService {
    
    // Constants
    private final static int ALERT_NOTIFICATION_ID = 0x6001;
    private final static int MSG_TRIGGER_ALERT = 0x5001;
    private final static int DELAY_TRIGGER_ALERT = 500;
    public final static String SEND_EMERGENCY_ALERT_SMS_PATH = "/start/sendEmergencyAlert";

    // Members
    private SharedPreferences mSharedPrefs = null;
    private NotificationManager mNotificationManager = null;
    private LocationManager mLocationManager;
    private ViewHandler mMessageHandler = new ViewHandler(this);

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(SEND_EMERGENCY_ALERT_SMS_PATH)) {
            // Don't allow multiple consecutive messages firing right next to another
            // to trigger an alert when they are too close together
            mMessageHandler.removeMessages(MSG_TRIGGER_ALERT);
            mMessageHandler.sendEmptyMessageDelayed(MSG_TRIGGER_ALERT, DELAY_TRIGGER_ALERT);
        }
    }

    /**
     * Trigger the alert by sending an SMS with a pre-configured message to a pre-configured phone number. Here we also
     * take user's preferences into consideration. A {@link Notification} is posted, only if the user has that option
     * enabled, and the last known location is sent immediately, only if the user has that option enabled.
     * <p/>
     * Finally, if the option to send the location is enabled, the {@link FineLocationSMSIntentService} will be started
     * and the phone number will be passed as an {@link Intent} extra. The {@link FineLocationSMSIntentService} will
     * make sure to fetch a more accurate and current location point, then send it to the phone number passed when the
     * service was started, and then it will kill it self.
     */
    private void triggerAlert() {
        // Get the user preferences
        final String smsNumber = mSharedPrefs.getString(AlertPreferencesActivity.PREF_KEY_SMS_NUMBER, null);
        final String smsMessage = mSharedPrefs.getString(AlertPreferencesActivity.PREF_KEY_SMS_MESSAGE,
                getString(R.string.default_emergency_message));
        boolean sendLocation = mSharedPrefs.getBoolean(AlertPreferencesActivity.PREF_KEY_SMS_MESSAGE_LOCATION,
                true);
        boolean showNotification = mSharedPrefs.getBoolean(AlertPreferencesActivity.PREF_KEY_SHOW_NOTIFICATION,
                true);

        // Only keep going if we have a valid message and phone number
        if (!TextUtils.isEmpty(smsNumber) && !TextUtils.isEmpty(smsMessage)) {
            if (showNotification) {
                // Only post the notification if the option is enabled
                showNotification();
            }

            // Send the SMS with the specific messaged to the specific phone number set by the user
            SMSUtil.sendSms(smsNumber, smsMessage);

            // Only send the phone's location if the option is enabled
            if (sendLocation) {
                // Get the last known location and send it to the specific phone number
                Location location = mLocationManager
                        .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    // Format the message with specific coordinates
                    String message = String.format(getString(R.string.message_last_location),
                            Double.toString(latitude), Double.toString(longitude),
                            SMSUtil.getFormattedTimestamp(this, location.getTime()));

                    // Send the SMS with the last known location
                    SMSUtil.sendSms(smsNumber, message);
                }

                // Start the location service and pass the phone number as an extra
                Intent locationService = new Intent(this, FineLocationSMSIntentService.class);
                locationService.putExtra(FineLocationSMSIntentService.KEY_SMS_PHONE_NUMBER, smsNumber);
                startService(locationService);
            }
        }
    }

    /**
     * Post the notification to the {@link NotificationManager}
     */
    private void showNotification() {
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

        if (mNotificationManager != null) {
            mNotificationManager.notify(ALERT_NOTIFICATION_ID, builder.build());
        }
    }

    private static class ViewHandler extends Handler {

        // Members
        private WeakReference<WearAlertService> mmParent;

        /**
         * Constructor.
         *
         * @param parent
         *         {@link WearAlertService}
         */
        private ViewHandler(WearAlertService parent) {
            mmParent = new WeakReference<>(parent);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleMessage(Message msg) {
            WearAlertService parent = mmParent.get();
            switch (msg.what) {
                case MSG_TRIGGER_ALERT:
                    if (parent != null) {
                        parent.triggerAlert();
                    }
                    break;
            }
        }
    }

}
