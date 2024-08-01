package com.shopwallet.ituchallenger;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.fnsv.bsa.sdk.BsaSdk;
import com.fnsv.bsa.sdk.callback.SdkResponseCallback;
import com.fnsv.bsa.sdk.response.ErrorResult;
import com.fnsv.bsa.sdk.response.TokenResponse;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.shopwallet.ituchallenger.util.SecureStorageUtil;

import java.util.HashMap;
import java.util.HashSet;

public class FirebasePushService extends FirebaseMessagingService {

    private static final String TAG = "FirebasePushService";
    private static RemoteMessage currentRemoteMessage;
    private static final String ACTION_AUTHENTICATE = "com.shopwallet.ituchallenger.ACTION_AUTHENTICATE";
    private static final HashSet<String> processedMessages = new HashSet<>();
    private static final long BROADCAST_DELAY_MS = 5000; // 5 seconds delay

    // Declare and initialize lastBroadcastTime
    private long lastBroadcastTime = 0;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        currentRemoteMessage = remoteMessage;

        String messageId = remoteMessage.getMessageId();
        if (messageId != null && processedMessages.contains(messageId)) {
            Log.d(TAG, "Duplicate message received, ignoring: " + messageId);
            return;
        }

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            BsaSdk.getInstance().getSdkService().responsePushMessage(remoteMessage.getData());

            // Extract the channel_key from the data payload
            String channelKey = remoteMessage.getData().get("channel_key");
            if (channelKey != null && !channelKey.isEmpty()) {
                // Retrieve inputData from SecureStorageUtil
                HashMap<String, Object> inputData = SecureStorageUtil.retrieveDataFromKeystore(this, "inputData");
                String authType = (String) inputData.get("authType");
                if (authType != null) {
                    performLocalAuth(authType);
                } else {
                    // Show Snackbar asking the user to re-authenticate
                    showSnackbar("Re-Authentication Required. Please re-authenticate on the mobile app.");
                }
            }

            String isChannelKey = remoteMessage.getData().get("is_channel_key");
            if ("Y".equals(isChannelKey)) {
                showSnackbar("Authentication is completed successfully.");
            }
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        if (messageId != null) {
            processedMessages.add(messageId);
        }
    }

    private void performLocalAuth(String authType) {
        switch (authType) {
            case "3":
            case "4":
                // Throttle broadcasts
                if (System.currentTimeMillis() - lastBroadcastTime > BROADCAST_DELAY_MS) {
                    lastBroadcastTime = System.currentTimeMillis();
                    // Send broadcast to trigger authentication in an activity
                    Intent authIntent = new Intent(ACTION_AUTHENTICATE);
                    authIntent.putExtra("authType", authType);
                    sendBroadcast(authIntent);
                    Log.e(TAG, "Broadcast Sent: " + authType);
                } else {
                    Log.d(TAG, "Broadcast throttled: " + authType);
                }
                break;
            default:
                // Handle other cases if necessary
                Log.d(TAG, "Unknown authType: " + authType);
                break;
        }
    }

    private void showSnackbar(String message) {
        Intent intent = new Intent(Dashboard.ACTION_SHOW_SNACKBAR);
        intent.putExtra(Dashboard.EXTRA_SNACKBAR_MESSAGE, message);
        sendBroadcast(intent);
    }

    public static void completeAuth() {
        // Method to be called when authentication is completed successfully
        if (currentRemoteMessage != null) {
            Log.d(TAG, "authentication is completed successfully");
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // Register the new token with the BSA SDK
        registerPushToken(token);
    }

    private void registerPushToken(String token) {
        try {
            BsaSdk.getInstance().getSdkService().registerPushToken(token, new SdkResponseCallback<>() {
                @Override
                public void onSuccess(TokenResponse tokenResponse) {
                    if (tokenResponse != null) {
                        Log.d(TAG, "registerPushToken onSuccess: Result code: " + tokenResponse.getRtCode());
                    }
                }

                @Override
                public void onFailed(ErrorResult errorResult) {
                    if (errorResult != null) {
                        Log.d(TAG, "registerPushToken onFailed: Error code: " + errorResult.getErrorCode());
                        Log.d(TAG, "registerPushToken onFailed: Error message: " + errorResult.getErrorMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in registerPushToken: " + e.getMessage());
        }
    }
}