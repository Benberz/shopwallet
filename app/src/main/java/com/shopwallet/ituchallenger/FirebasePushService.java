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

/**
 * FirebasePushService handles incoming Firebase Cloud Messaging (FCM) messages.
 * It processes authentication requests, performs local authentication, and manages token registration.
 */
public class FirebasePushService extends FirebaseMessagingService {

    private static final String TAG = "FirebasePushService";
    private static RemoteMessage currentRemoteMessage;
    private static final String ACTION_AUTHENTICATE = "com.shopwallet.ituchallenger.ACTION_AUTHENTICATE";
    private static final HashSet<String> processedMessages = new HashSet<>();
    private static final long BROADCAST_DELAY_MS = 5000; // 5 seconds delay to throttle broadcasts

    // Time of the last broadcast to throttle messages
    private long lastBroadcastTime = 0;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        currentRemoteMessage = remoteMessage;

        String messageId = remoteMessage.getMessageId();
        if (messageId != null && processedMessages.contains(messageId)) {
            // Skip processing if the message has already been processed
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

    /**
     * Performs local authentication based on the provided authentication type.
     * Sends a broadcast if the authentication type requires it.
     *
     * @param authType The type of authentication to perform.
     */
    private void performLocalAuth(String authType) {
        switch (authType) {
            case "3":
            case "4":
                // Throttle broadcasts to avoid sending multiple in quick succession
                if (System.currentTimeMillis() - lastBroadcastTime > BROADCAST_DELAY_MS) {
                    lastBroadcastTime = System.currentTimeMillis();
                    // Create and send a broadcast intent to trigger authentication
                    Intent authIntent = new Intent(ACTION_AUTHENTICATE);
                    authIntent.putExtra("authType", authType);
                    authIntent.putExtra("broadcastId", String.valueOf(System.currentTimeMillis())); // Unique identifier for the broadcast
                    sendBroadcast(authIntent);
                    Log.e(TAG, "Broadcast Sent: " + authType);
                    Log.e(TAG, "Broadcast ID: " + System.currentTimeMillis());
                } else {
                    Log.d(TAG, "Broadcast throttled: " + authType);
                }
                break;
            default:
                // Handle other authentication types if necessary
                Log.d(TAG, "Unknown authType: " + authType);
                break;
        }
    }

    /**
     * Shows a Snackbar message to the user by sending a broadcast.
     *
     * @param message The message to display in the Snackbar.
     */
    private void showSnackbar(String message) {
        Intent intent = new Intent(Dashboard.ACTION_SHOW_SNACKBAR);
        intent.putExtra(Dashboard.EXTRA_SNACKBAR_MESSAGE, message);
        sendBroadcast(intent);
    }

    /**
     * Called when authentication is completed successfully.
     * This method is a placeholder and can be expanded as needed.
     */
    public static void completeAuth() {
        if (currentRemoteMessage != null) {
            Log.d(TAG, "Authentication is completed successfully");
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // Register the new token with the BSA SDK
        registerPushToken(token);
    }

    /**
     * Registers the new push token with the BSA SDK.
     *
     * @param token The new push token to register.
     */
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