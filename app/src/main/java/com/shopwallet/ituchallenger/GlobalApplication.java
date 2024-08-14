package com.shopwallet.ituchallenger;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.fnsv.bsa.sdk.BsaSdk;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.shopwallet.ituchallenger.util.NetworkUtil;

/**
 * GlobalApplication initializes necessary components when the application starts.
 * It sets up BSA SDK, Firebase, network checking, and FCM token retrieval.
 */
public class GlobalApplication extends Application {

    private static final String CLIENT_KEY = BuildConfig.CLIENT_KEY;
    private static final String API_SERVER_URL = BuildConfig.API_SERVER_URL;
    private static final String TAG = "GlobalApplication";
    private static Handler mainHandler;
    private static AppLifecycleTracker lifecycleTracker;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the BsaSdk with application context, client key, and API server URL
        BsaSdk.getInstance().init(getApplicationContext(), CLIENT_KEY, API_SERVER_URL);

        // Initialize Firebase with application context
        FirebaseApp.initializeApp(getApplicationContext());

        // Initialize the main handler for executing tasks on the main thread
        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize and register the AppLifecycleTracker to monitor activity lifecycle
        lifecycleTracker = new AppLifecycleTracker();
        registerActivityLifecycleCallbacks(lifecycleTracker);

        // Check for internet connection at app start and show snackbar if offline
        checkInternetConnection();

        // Retrieve and log the current FCM token
        registerFCMPushToken();
    }

    /**
     * Checks for an internet connection and shows a Snackbar if no connection is available.
     * Provides options to retry or dismiss the Snackbar.
     */
    private void checkInternetConnection() {
        if (!NetworkUtil.isInternetAvailable(getApplicationContext())) {
            // Show snackbar on the main thread if no internet connection is found
            mainHandler.post(() -> {
                if (lifecycleTracker.getCurrentActivity() != null) {
                    View rootView = lifecycleTracker.getCurrentActivity().findViewById(android.R.id.content);
                    Snackbar snackbar = Snackbar.make(rootView, "No internet connection. Please check your connection.", Snackbar.LENGTH_INDEFINITE)
                            .setAction("Retry", view -> checkInternetConnection()) // Retry internet check
                            .setAction("Dismiss", view -> {
                                // Dismiss action, no additional code needed
                            });
                    snackbar.show();
                }
            });
        }
    }

    /**
     * Retrieves the current Firebase Cloud Messaging (FCM) token and logs it.
     * This token is used for push notifications.
     */
    private void registerFCMPushToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                Log.d(TAG, "Fetching FCM registration token : " + token);
                // Optionally register the FCM token with BSA or another service here
                // registerFCMPushTokenToBSA(token);
            } else {
                Log.e(TAG, "Fetching FCM registration token failed", task.getException());
            }
        });
    }
}