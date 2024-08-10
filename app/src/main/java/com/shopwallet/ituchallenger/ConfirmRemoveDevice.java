package com.shopwallet.ituchallenger;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fnsv.bsa.sdk.BsaSdk;
import com.fnsv.bsa.sdk.callback.SdkAuthResponseCallback;
import com.fnsv.bsa.sdk.callback.SdkResponseCallback;
import com.fnsv.bsa.sdk.common.SdkUtil;
import com.fnsv.bsa.sdk.response.AuthResultResponse;
import com.fnsv.bsa.sdk.response.ErrorResult;
import com.fnsv.bsa.sdk.response.UnRegisterDeviceResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.shopwallet.ituchallenger.util.SecureStorageUtil;
import com.shopwallet.ituchallenger.util.SessionManager;

import java.util.HashMap;

public class ConfirmRemoveDevice extends AppCompatActivity {

    private static final String TAG = "PersonalInfoClass";
    private HashMap<String, Object> storedInputData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_remove_device);

        storedInputData = SecureStorageUtil.retrieveDataFromKeystore(this, "inputData");

        // Set the Confirm button functionality
        Button confirmButton = findViewById(R.id.confirmButton);

        confirmButton.setOnClickListener(view -> {
            // Show the de-register device dialog
            showRemoveDeviceDialog();
        });
    }

    private void showRemoveDeviceDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_remove_device, findViewById(android.R.id.content), false);
        dialog.setContentView(view);

        Button buttonYes = dialog.findViewById(R.id.deleteAccountYesDialogButton);
        Button buttonCancel = dialog.findViewById(R.id.deleteAccountCancelDialogButton);

        assert buttonYes != null;
        buttonYes.setOnClickListener(v -> {
            // Handle Yes button click
            unregisterDevice();
            dialog.dismiss();
        });

        assert buttonCancel != null;
        buttonCancel.setOnClickListener(v -> {
            Intent intent = new Intent(ConfirmRemoveDevice.this, PersonalInfo.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Finish the current activity
            dialog.dismiss();
        });
        dialog.show();
    }

    private void unregisterDevice() {
        // Retrieve the userKey
        String userKey = (String) storedInputData.get("userKey");

        if (userKey != null) {
            // Check if the access token is available
            if (!SdkUtil.getAccessToken().isEmpty()) {
                // Call the unregister device API
                BsaSdk.getInstance().getSdkService().unRegisterDevice(userKey, new SdkResponseCallback<>() {
                    @Override
                    public void onSuccess(UnRegisterDeviceResponse response) {
                        Log.d(TAG, "Device unregistered successfully: " + response);
                        logOutFirebaseAndNavigateToSignIn();
                    }

                    @Override
                    public void onFailed(ErrorResult errorResult) {
                        if (errorResult.getErrorCode() == 2105) {
                            callInAppAuthenticator(userKey);
                        } else {
                            Log.e(TAG, "Failed to unregister device: " + errorResult.getErrorMessage());
                            runOnUiThread(() -> Toast.makeText(ConfirmRemoveDevice.this, "Failed to unregister device: " + errorResult.getErrorMessage(), Toast.LENGTH_LONG).show());
                            Intent personalInfoActivity = new Intent(ConfirmRemoveDevice.this, PersonalInfo.class);
                            startActivity(personalInfoActivity);
                        }
                    }
                });
            } else {
                // Show dialog to inform the user to sign in again
                runOnUiThread(() -> new androidx.appcompat.app.AlertDialog.Builder(ConfirmRemoveDevice.this)
                        .setTitle("Sign In Required")
                        .setMessage("Your session has expired. Please sign in again to continue.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            // Redirect to SignIn activity
                            Intent intent = new Intent(ConfirmRemoveDevice.this, SignIn.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish(); // Finish the current activity
                        })
                        .setCancelable(false)
                        .show());
            }
        } else {
            runOnUiThread(() -> Toast.makeText(ConfirmRemoveDevice.this, "User key is missing", Toast.LENGTH_LONG).show());
            Log.e(TAG, "User key is null");
            Intent profileActivity = new Intent(ConfirmRemoveDevice.this, Profile.class);
            startActivity(profileActivity);
        }
    }

    private void logOutFirebaseAndNavigateToSignIn() {
        FirebaseAuth.getInstance().signOut();

        // Display SnackBar
        runOnUiThread(() -> {
            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Device removed", Snackbar.LENGTH_LONG);
            snackbar.setAnchorView(findViewById(R.id.dashboardBottomNavigation)); // Adjust if you have a BottomNavigationView
            snackbar.show();

            // Delay the navigation to allow Snackbar to be displayed
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(ConfirmRemoveDevice.this, SignIn.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                // Log out from the session
                SessionManager.getInstance(getApplicationContext()).logout();
                finish(); // Finish the current activity
            }, Snackbar.LENGTH_LONG + 2000); // Delay in milliseconds
        });
    }

    private void callInAppAuthenticator(String userKey) {
        BsaSdk.getInstance().getSdkService().appAuthenticator(userKey, true, this, new SdkAuthResponseCallback<>() {
            @Override
            public void onSuccess(AuthResultResponse authResultResponse) {
                Log.d(TAG, "In-app authentication successful: code: " + authResultResponse.getRtCode());
                Log.d(TAG, "AccessToken: " + SdkUtil.getAccessToken());
                unregisterDevice();
            }

            @Override
            public void onProcess(boolean b, String s) {
                Log.d(TAG, "In-app authentication processing...: s: " + s);
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                Log.e(TAG, "In-app authentication failed: " + errorResult.getErrorMessage());
                Snackbar.make(findViewById(android.R.id.content), "Authentication failed: " + errorResult.getErrorMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}