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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Daniel Velazco <velazcod@gmail.com>
 * @since 7/2/14
 */
public class AlertTriggerActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DelayedConfirmationView.DelayedConfirmationListener {

    // Constants
    private static final String TAG = "AlertTriggerActivity";
    private static final long CONFIRMATION_DELAY_MS = 3500;
    public final static String SEND_EMERGENCY_ALERT_SMS_PATH = "/start/sendEmergencyAlert";
    public final static String PREF_KEY_USE_CONFIRMATION_BTN = "_pref_use_confirmation_btn";

    // Members
    private SharedPreferences mSharedPrefs = null;
    private GoogleApiClient mGoogleApiClient = null;
    private TextView mTvTitle;
    private TextView mTvQuestion;
    private TextView mTvStatus;
    private TextView mTvConfirmationStatus;
    private DelayedConfirmationView mBtnConfirm;
    private boolean mUseConfirmationButton;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_trigger);

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mUseConfirmationButton = mSharedPrefs.getBoolean(PREF_KEY_USE_CONFIRMATION_BTN, false);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTvTitle = (TextView) stub.findViewById(R.id.tv_title);
                mTvQuestion = (TextView) stub.findViewById(R.id.tv_are_you_sure);
                mTvStatus = (TextView) stub.findViewById(R.id.tv_status);
                mTvConfirmationStatus = (TextView) stub.findViewById(R.id.tv_confirmation_status);

                mBtnConfirm = (DelayedConfirmationView) stub.findViewById(R.id.btn_confirm);
                mBtnConfirm.setListener(AlertTriggerActivity.this);

                if (mUseConfirmationButton) {
                    mTvConfirmationStatus.setText(R.string.lbl_confirm);
                    mBtnConfirm.setImageResource(R.drawable.ic_navigation_accept);
                } else {
                    mTvConfirmationStatus.setText(R.string.lbl_sending);
                    mBtnConfirm.setImageResource(R.drawable.ic_navigation_cancel);
                    mBtnConfirm.setTotalTimeMs(CONFIRMATION_DELAY_MS);
                    mBtnConfirm.start();
                }
            }
        });

        // Create the GoogleApiClient object we'll be using to connect in order to use the Wear API
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart() {
        super.onStart();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (mBtnConfirm != null && mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mBtnConfirm.start();
            mBtnConfirm.setListener(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        if (mBtnConfirm != null) {
            mBtnConfirm.setListener(null);
        }
        super.onStop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnected(Bundle bundle) {
        // No need to do anything here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnectionSuspended(int i) {
        if (mBtnConfirm != null) {
            mBtnConfirm.setListener(null);
        }

        finish();
        Intent intent = new Intent(AlertTriggerActivity.this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.lbl_error_occurred));
        startActivity(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (mBtnConfirm != null) {
            mBtnConfirm.setListener(null);
        }

        finish();
        Intent intent = new Intent(AlertTriggerActivity.this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.lbl_error_occurred));
        startActivity(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTimerFinished(View view) {
        sendAlert();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTimerSelected(View view) {
        if (mUseConfirmationButton) {
            sendAlert();
        } else {
            mBtnConfirm.setListener(null);

            finish();
            Intent intent = new Intent(AlertTriggerActivity.this, ConfirmationActivity.class);
            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
            intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.lbl_alert_cancelled));
            startActivity(intent);
        }
    }

    private void sendAlert() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            (new MessageAlertTask()).execute();
        }
    }

    /**
     * Get a {@link Collection} of connected {@linkplain Node nodes} to which we can send a message to.
     *
     * @return {@link Collection}
     */
    private Collection<String> getNodes() {
        HashSet<String> results= new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }

    /**
     * {@link AsyncTask} used to send a message using the {@link MessageApi} since the method {@link
     * MessageApi#sendMessage(GoogleApiClient, String, String, byte[])} blocks and needs to be called on a background
     * thread.
     */
    private class MessageAlertTask extends AsyncTask<Void, Void, Boolean> {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPreExecute() {
            mTvTitle.setVisibility(View.GONE);
            mTvQuestion.setVisibility(View.GONE);
            mBtnConfirm.setVisibility(View.GONE);
            mTvConfirmationStatus.setVisibility(View.GONE);
            mTvStatus.setText(getString(R.string.lbl_sending));
            mTvStatus.setVisibility(View.VISIBLE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            boolean messageSent = false;
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node,
                        SEND_EMERGENCY_ALERT_SMS_PATH, null).await();
                if (result.getStatus().isSuccess()) {
                    messageSent = true;
                } else {
                    Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                }
            }

            return messageSent;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(Boolean success) {
            finish();
            Intent intent = new Intent(AlertTriggerActivity.this, ConfirmationActivity.class);
            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, success ?
                    ConfirmationActivity.SUCCESS_ANIMATION : ConfirmationActivity.FAILURE_ANIMATION);
            intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, success ? "Success" : "Failure");
            startActivity(intent);
        }
    }

}
