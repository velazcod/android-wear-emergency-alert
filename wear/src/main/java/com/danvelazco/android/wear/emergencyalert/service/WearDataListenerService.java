package com.danvelazco.android.wear.emergencyalert.service;

import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import com.danvelazco.android.wear.emergencyalert.AlertTriggerActivity;
import com.danvelazco.android.wear.emergencyalert.data.PreferencesData;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

/**
 * @since 6/7/15
 */
public class WearDataListenerService extends WearableListenerService {

    // Constants
    private static final String TAG = "WearDataListener";

    // Members
    private SharedPreferences mSharedPrefs = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged()");
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        for (DataEvent event : events) {
            Uri uri = event.getDataItem().getUri();
            if (PreferencesData.URI_PATH.equals(uri.getPath())) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    Log.d(TAG, PreferencesData.URI_PATH + " data TYPE_CHANGED");
                    DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    SharedPreferences.Editor editor = mSharedPrefs.edit();
                    editor.putBoolean(AlertTriggerActivity.PREF_KEY_USE_CONFIRMATION_BTN,
                            PreferencesData.getUseButtonConfirmation(dataMap));
                    editor.apply();
                }
            }
        }
    }

}
