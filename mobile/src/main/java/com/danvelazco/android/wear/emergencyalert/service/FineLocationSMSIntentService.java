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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import com.danvelazco.android.wear.emergencyalert.R;
import com.danvelazco.android.wear.emergencyalert.util.SMSUtil;

/**
 * This service is only responsible for finding an accurate and current {@link Location} and send it as a SMS message to
 * the specified phone number passed as an {@link Intent} extra on {@link #onStartCommand(Intent, int, int)}
 *
 * @author Daniel Velazco <velazcod@gmail.com>
 * @since 9/18/14
 */
public class FineLocationSMSIntentService extends Service implements LocationListener {

    // Constants
    public static final String LOG_TAG = "FineLocationSMSIntentService";
    private static final int WAKELOCK_TIMEOUT_MS = 600000; // 10 minutes
    public static final String KEY_SMS_PHONE_NUMBER = "_key_phone_number";

    // Members
    private PowerManager.WakeLock mWakeLock = null;
    private LocationManager mLocationManager;
    private String mTempSmsNumberToSendLocationTo = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        super.onCreate();

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }

        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String smsNumber = intent.getStringExtra(KEY_SMS_PHONE_NUMBER);

        if (!TextUtils.isEmpty(smsNumber)) {
            if (mTempSmsNumberToSendLocationTo == null) {
                // Need to fetch a new fine location

                // Acquire a wake lock to make sure we don't die while we wait for a location
                mWakeLock.acquire(WAKELOCK_TIMEOUT_MS);

                // Build a criteria that will be used to find the best location provider
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                criteria.setPowerRequirement(Criteria.POWER_HIGH);

                // Get the name of the best location provider based on the criteria above
                String locationProvider = mLocationManager.getBestProvider(criteria, true);

                if (locationProvider != null) {
                    // If our provider is valid, request a single location update using this provider, then wait
                    mTempSmsNumberToSendLocationTo = smsNumber;
                    mLocationManager.requestSingleUpdate(locationProvider,
                            FineLocationSMSIntentService.this, null);
                }
            } else {
                // If a number is already set, that means we are currently waiting for the
                // location to be found, simply make sure the phone number is updated
                mTempSmsNumberToSendLocationTo = smsNumber;
            }
        }

        return START_FLAG_REDELIVERY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLocationChanged(Location location) {
        if (mLocationManager != null && location != null) {
            // Remove all pending location updates. we got what we needed
            mLocationManager.removeUpdates(this);
        }

        if (location != null && mTempSmsNumberToSendLocationTo != null) {
            // Get the location coordinates
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            // Get the coordinates in a user readable format
            String message = String.format(getString(R.string.message_current_location),
                    Double.toString(latitude), Double.toString(longitude),
                    SMSUtil.getFormattedTimestamp(this, location.getTime()));

            // Send the SMS to the specific number with the fine location
            SMSUtil.sendSms(mTempSmsNumberToSendLocationTo, message);

            // Clear out the phone number variable to avoid duplicates
            mTempSmsNumberToSendLocationTo = null;

            if (mWakeLock.isHeld()) {
                // Release the wake lock
                mWakeLock.release();
            }

            // Message sent, wakelock released, we are done here, kill the service
            stopSelf();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // No implementation
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onProviderEnabled(String provider) {
        // No implementation
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onProviderDisabled(String provider) {
        // No implementation
    }

}
