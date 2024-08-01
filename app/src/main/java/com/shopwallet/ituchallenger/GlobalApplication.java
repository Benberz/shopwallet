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

public class GlobalApplication extends Application {

    private static final String CLIENT_KEY = BuildConfig.CLIENT_KEY;
    private static final String API_SERVER_URL = BuildConfig.API_SERVER_URL;
    private static final String TAG = "GlobalApplication";
    private static Handler mainHandler;
    private static AppLifecycleTracker lifecycleTracker;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the BsaSdk
        BsaSdk.getInstance().init(getApplicationContext(), CLIENT_KEY, API_SERVER_URL);

        // Initialize Firebase
        FirebaseApp.initializeApp(getApplicationContext());

        // Initialize handler
        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize and register the lifecycle tracker
        lifecycleTracker = new AppLifecycleTracker();
        registerActivityLifecycleCallbacks(lifecycleTracker);

        // Check for internet connection on app start
        checkInternetConnection();

        // get the current FCM token
        registerFCMPushToken();
    }

    private void checkInternetConnection() {
        if (!NetworkUtil.isInternetAvailable(getApplicationContext())) {
            // Show snackbar on the main thread
            mainHandler.post(() -> {
                if (lifecycleTracker.getCurrentActivity() != null) {
                    View rootView = lifecycleTracker.getCurrentActivity().findViewById(android.R.id.content);
                    Snackbar snackbar = Snackbar.make(rootView, "No internet connection. Please check your connection.", Snackbar.LENGTH_INDEFINITE)
                            .setAction("Retry", view -> checkInternetConnection())
                            .setAction("Dismiss", view -> {
                                // Dismiss action, no additional code needed
                            });
                    snackbar.show();
                }
            });
        }
    }

    private void registerFCMPushToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                Log.d(TAG, "Fetching FCM registration token : " + token);
                // registerFCMPushTokenToBSA(token);
            } else {
                Log.e(TAG, "Fetching FCM registration token failed", task.getException());
            }
        });
    }
}