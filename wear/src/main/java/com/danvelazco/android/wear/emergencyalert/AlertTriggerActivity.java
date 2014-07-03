package com.danvelazco.android.wear.emergencyalert;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WatchViewStub;
import android.util.DisplayMetrics;
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

public class AlertTriggerActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // Constants
    private static final String TAG = "AlertTriggerActivity";
    private static final int REQUEST_ID = 0x1001;
    public final static String SEND_EMERGENCY_ALERT_SMS_PATH = "/start/sendEmergencyAlert";

    // Members
    private GoogleApiClient mGoogleApiClient = null;
    private TextView mTvTitle;
    private TextView mTvQuestion;
    private TextView mTvStatus;
    private CircledImageView mBtnConfirm;
    private CircledImageView mBtnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_trigger);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {

                mTvTitle = (TextView) stub.findViewById(R.id.tv_title);
                mTvQuestion = (TextView) stub.findViewById(R.id.tv_are_you_sure);
                mTvStatus = (TextView) stub.findViewById(R.id.tv_status);

                mBtnConfirm = (CircledImageView) stub.findViewById(R.id.btn_confirm);
                mBtnConfirm.setImageResource(R.drawable.ic_navigation_accept);
                mBtnConfirm.setCircleHidden(false);
                mBtnConfirm.setCircleRadius(dpToPx(32));
                mBtnConfirm.setCircleRadiusPressed(dpToPx(36));
                mBtnConfirm.setCircleColor(getResources().getColor(R.color.green));
                mBtnConfirm.setCircleBorderColor(getResources().getColor(R.color.green_dark));
                mBtnConfirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmEmergency();
                    }
                });

                mBtnCancel = (CircledImageView) stub.findViewById(R.id.btn_cancel);
                mBtnCancel.setImageResource(R.drawable.ic_navigation_cancel);
                mBtnCancel.setCircleHidden(false);
                mBtnCancel.setCircleRadius(dpToPx(32));
                mBtnCancel.setCircleRadiusPressed(dpToPx(36));
                mBtnCancel.setCircleColor(getResources().getColor(R.color.grey));
                mBtnCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });

                // Handle the google api client connection in case it's already connected by the time this is called
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    mBtnConfirm.setEnabled(true);
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

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ID) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mBtnConfirm != null) {
            mBtnConfirm.setEnabled(true);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // TODO: show error?

        if (mBtnConfirm != null) {
            mBtnConfirm.setEnabled(false);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // TODO: show error?

        if (mBtnConfirm != null) {
            mBtnConfirm.setEnabled(false);
        }
    }

    private void confirmEmergency() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            (new MessageAlertTask()).execute();
        }
    }

    private Collection<String> getNodes() {
        HashSet<String> results= new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }

    private class MessageAlertTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            mTvTitle.setVisibility(View.GONE);
            mTvQuestion.setVisibility(View.GONE);
            mBtnConfirm.setVisibility(View.GONE);
            mBtnCancel.setVisibility(View.GONE);
            mTvStatus.setText("Sending alert...");
            mTvStatus.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node,
                        SEND_EMERGENCY_ALERT_SMS_PATH, null).await();
                if (!result.getStatus().isSuccess()) {
                    Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                    return false;
                }
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                mTvStatus.setVisibility(View.GONE);

                Intent intent = new Intent(AlertTriggerActivity.this, ConfirmationActivity.class);
                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
                startActivityForResult(intent, REQUEST_ID);
            } else {
                mTvStatus.setText("An error occurred");
                mTvStatus.setVisibility(View.VISIBLE);
            }
        }
    }

}
