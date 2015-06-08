package com.danvelazco.android.wear.emergencyalert.data;

import android.net.Uri;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;

/**
 * @since 6/7/15
 */
public class PreferencesData {

    /**
     * Constants used for Wear Data API
     */
    public static final String URI_PATH = "/preferencesdata";
    public static final String FIELD_USE_BUTTON_CONFIRMATION = "use_button_confirmation";

    /**
     * Get the full URI for the specific {@link Node}
     *
     * @param nodeId
     *         {@link String} ID of the {@link Node}
     * @return {@link Uri}
     */
    public static Uri getUri(String nodeId) {
        return new Uri.Builder()
                .scheme(PutDataRequest.WEAR_URI_SCHEME)
                .authority(nodeId)
                .path(URI_PATH)
                .build();
    }

    public static boolean getUseButtonConfirmation(DataMap dataMap) {
        return dataMap.getBoolean(FIELD_USE_BUTTON_CONFIRMATION);
    }

    public static PutDataMapRequest toDataMap(boolean useButtonConfirmation) {
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create(URI_PATH);
        dataMapRequest.getDataMap().putBoolean(FIELD_USE_BUTTON_CONFIRMATION, useButtonConfirmation);
        return dataMapRequest;
    }

}
