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

package com.danvelazco.android.wear.emergencyalert.util;

import android.content.Context;
import android.telephony.SmsManager;
import android.text.format.DateUtils;

/**
 * @author Daniel Velazco <velazcod@gmail.com>
 * @since 9/18/14
 */
public class SMSUtil {

    /**
     * Use the default {@link SmsManager} to send a message to phone number
     *
     * @param number
     *         {@link String}
     * @param message
     *         {@link String}
     */
    public static void sendSms(String number, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(number, null, message, null, null);
    }

    /**
     * Get a formatted timestamp to be included in the SMS
     *
     * @param context
     *         {@link Context}
     * @param timestamp
     *         {@link long}
     * @return {@link String}
     */
    public static String getFormattedTimestamp(Context context, long timestamp) {
        return DateUtils.formatDateTime(context, timestamp,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
                        | DateUtils.FORMAT_SHOW_TIME
        );
    }

}
