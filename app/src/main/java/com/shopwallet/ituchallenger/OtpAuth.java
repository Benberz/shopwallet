package com.shopwallet.ituchallenger;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.fnsv.bsa.sdk.BsaSdk;
import com.fnsv.bsa.sdk.callback.SdkAuthResponseCallback;
import com.fnsv.bsa.sdk.callback.SdkResponseCallback;
import com.fnsv.bsa.sdk.common.SdkUtil;
import com.fnsv.bsa.sdk.response.AuthCompleteResponse;
import com.fnsv.bsa.sdk.response.AuthResultResponse;
import com.fnsv.bsa.sdk.response.ErrorResult;
import com.fnsv.bsa.sdk.response.OtpCancelResponse;
import com.fnsv.bsa.sdk.response.OtpGenerateResponse;
import com.google.android.material.snackbar.Snackbar;
import com.shopwallet.ituchallenger.util.SecureStorageUtil;

import java.util.HashMap;
import java.util.concurrent.Executor;


@SuppressWarnings("ALL")
public class OtpAuth extends AppCompatActivity {

    private static final String TAG = "OtpAuthClass";

    private String otpCode;
    private TextView otpTextView;
    private ProgressBar progressBar;
    private String userKey;

    public static final String ACTION_AUTHENTICATE = "com.shopwallet.ituchallenger.ACTION_AUTHENTICATE";
    private boolean isReceiverRegistered = false;
    // private String channelKey;

    public static final String ACTION_SHOW_SNACKBAR = "com.shopwallet.ituchallenger.ACTION_SHOW_SNACKBAR";
    public static final String EXTRA_SNACKBAR_MESSAGE = "com.shopwallet.ituchallenger.EXTRA_SNACKBAR_MESSAGE";

    private final BroadcastReceiver authReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_AUTHENTICATE.equals(intent.getAction())) {
                Log.e(TAG, "Broadcast Received from FirebasePushService");
                String authType = intent.getStringExtra("authType");
                // channelKey = intent.getStringExtra("channel_key");

                if ("3".equals(authType)) {
                    performBiometricAuth();
                } else if ("4".equals(authType)) {
                    performPinPatternAuth();
                }
            }
        }
    };

    private final BroadcastReceiver snackbarReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && ACTION_SHOW_SNACKBAR.equals(intent.getAction())) {
                String message = intent.getStringExtra(EXTRA_SNACKBAR_MESSAGE);
                if (message != null && !message.isEmpty()) {
                    showSnackbar(message);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_auth);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.otpToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.otp_code_text);
        }

        otpTextView = findViewById(R.id.otpTextView);
        Button cancelButton = findViewById(R.id.cancelButton);
        Button reGenerateButton = findViewById(R.id.reGenerateButton);
        progressBar = findViewById(R.id.otpAuthProgressBar);

        otpTextView.setVisibility(View.INVISIBLE);

        // Replace with actual user key retrieval
        HashMap<String, Object> inputData = SecureStorageUtil.retrieveDataFromKeystore(getApplicationContext(), "inputData");
        userKey = (String) inputData.get("userKey");

        checkAccessTokenAndRequestOtp();

        cancelButton.setOnClickListener(v -> cancelOtpCode());
        reGenerateButton.setOnClickListener(v -> requestOtpCode());

        // Register the BroadcastReceiver
        if (!isReceiverRegistered) {
            IntentFilter filter = new IntentFilter(ACTION_AUTHENTICATE);
            registerReceiver(authReceiver, filter);
            isReceiverRegistered = true;
            Log.e(TAG, "BroadcastReceiver registered");
        }

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(ACTION_SHOW_SNACKBAR);
        registerReceiver(snackbarReceiver, filter);
    } // end of onCreate

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the BroadcastReceiver
        if (isReceiverRegistered) {
            unregisterReceiver(authReceiver);
            isReceiverRegistered = false;
            Log.e(TAG, "BroadcastReceiver unregistered");
        }

        // Unregister the BroadcastReceiver
        unregisterReceiver(snackbarReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button click
            startActivity(new Intent(this, Dashboard.class)); // to navigate back to MainActivity
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkAccessTokenAndRequestOtp() {
        String accessToken = SdkUtil.getAccessToken();

        if (accessToken != null && !accessToken.isEmpty()) {
            Log.d(TAG, "Access token is available, requesting OTP code");
            requestOtpCode();
        } else {
            Log.d(TAG, "Access token not available, starting in-app authentication");
            callInAppAuthenticator(userKey, this::requestOtpCode);
        }
    }

    private void requestOtpCode() {
        progressBar.setVisibility(View.VISIBLE);
        BsaSdk.getInstance().getSdkService().getAuthOtpCode(new SdkResponseCallback<>() {
            @Override
            public void onSuccess(OtpGenerateResponse result) {
                Log.d(TAG, "onSuccess: getAuthOtpCode");
                progressBar.setVisibility(View.INVISIBLE);
                if (result != null) {
                    otpCode = result.data;
                    Log.d(TAG, "onSuccess: otpCode: " + otpCode);

                    otpTextView.setText(otpCode);
                    otpTextView.setVisibility(View.VISIBLE);
                    otpTextView.setTextColor(Color.BLACK);
                    // authenticateOtp();
                }
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                Log.e(TAG, "onFailed: " + errorResult.getErrorCode());
                progressBar.setVisibility(View.INVISIBLE);
                runOnUiThread(() -> Toast.makeText(OtpAuth.this, "Failed to get OTP code: " + errorResult.getErrorCode() + "\n Message: " + errorResult.getErrorMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void cancelOtpCode() {
        progressBar.setVisibility(View.VISIBLE);
        BsaSdk.getInstance().getSdkService().cancelOtp(otpCode, new SdkResponseCallback<>() {
            @Override
            public void onSuccess(OtpCancelResponse result) {
                Log.d(TAG, "onSuccess: cancelOtp");
                progressBar.setVisibility(View.INVISIBLE);
                runOnUiThread(() -> Toast.makeText(OtpAuth.this, "OTP Cancelled Successfully", Toast.LENGTH_LONG).show());
                startActivity(new Intent(OtpAuth.this, Dashboard.class));
                finish();
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                Log.e(TAG, "onFailed: " + errorResult.toString());
                progressBar.setVisibility(View.INVISIBLE);
                runOnUiThread(() -> Toast.makeText(OtpAuth.this, "Failed to cancel OTP: " + errorResult.getErrorCode(), Toast.LENGTH_LONG).show());
                startActivity(new Intent(OtpAuth.this, Dashboard.class));
                finish();
            }
        });
    }

    private void authenticateOtp() {
        // Show a dialog while processing
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setView(LayoutInflater.from(this).inflate(R.layout.dialog_progress, findViewById(android.R.id.content), false))
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    cancelOtpCode();
                    dialogInterface.dismiss(); // Dismiss the dialog
                })
                .create();
        progressDialog.show();

        progressBar.setVisibility(View.VISIBLE);
        BsaSdk.getInstance().getSdkService().normalAuthenticator(userKey, true, this, new SdkAuthResponseCallback<>() {

            @Override
            public void onSuccess(AuthCompleteResponse authCompleteResponse) {
                Log.d(TAG, "normalAuthenticator | onSuccess");
                progressBar.setVisibility(View.INVISIBLE);
                progressDialog.dismiss();
                runOnUiThread(() -> showAuthenticationResult(true, "Carry on [Web]" + authCompleteResponse.rtMsg));
            }

            @Override
            public void onProcess(boolean b, String s) {
                runOnUiThread(() -> Snackbar.make(findViewById(android.R.id.content), "Authenticating OTP s: " + s, Snackbar.LENGTH_SHORT).show());
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                Log.e(TAG, "otpViewAuthenticator| onFailed: code: " + errorResult.getErrorCode() + " | message: " + errorResult.getErrorMessage());
                progressDialog.dismiss();
                progressBar.setVisibility(View.INVISIBLE);
                runOnUiThread(() -> showAuthenticationResult(false, "Error Code: " + errorResult.getErrorCode()));
            }
        });
    }

    private void showAuthenticationResult(boolean isSuccess, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(OtpAuth.this);
        builder.setTitle(isSuccess ? "OTP Authentication Successful" : "OTP Authentication Failed");
        builder.setMessage(message);

        // Inflate custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_auth_result, null);
        builder.setView(dialogView);

        // Set the icon based on the authentication result
        ImageView resultIcon = dialogView.findViewById(R.id.resultIcon);
        resultIcon.setImageResource(isSuccess ? R.drawable.ic_success : R.drawable.ic_failure);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            dialog.dismiss();
            if (isSuccess) {
                startActivity(new Intent(OtpAuth.this, Dashboard.class));
                finish();
            }
        });
        builder.show();
    }

    private void showSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
    }

    private void performBiometricAuth() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Log.e(TAG, "Biometric authentication error: " + errString);
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    Log.d(TAG, "Biometric authentication succeeded");

                    String accessToken = SdkUtil.getAccessToken();

                    Log.e(TAG, "___ Access Token is set | Access token: " + accessToken);
                    //checkExistingAuthRequest();
                    authenticateOtp();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Log.e(TAG, "Biometric authentication failed");
                }
            });

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric Authentication")
                    .setSubtitle("Authenticate using biometric credentials")
                    .setNegativeButtonText("Cancel")
                    .build();

            biometricPrompt.authenticate(promptInfo);
        } else {
            Log.e(TAG, "Biometric authentication not available or not set up");
        }
    }

    private void performPinPatternAuth() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        if (keyguardManager.isKeyguardSecure()) {
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent("Authentication Required [OTP Auth]", "Please enter your PIN, pattern or password to authenticate");
            if (intent != null) {
                deviceCredentialLauncher2.launch(intent);
            }
        } else {
            Log.e(TAG, "PIN/pattern authentication not set up");
        }
    }

    private final ActivityResultLauncher<Intent> deviceCredentialLauncher2 = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Log.d(TAG, "PIN/pattern authentication succeeded");

                    Log.e(TAG, "userKey: " + userKey);
                    String accessToken = SdkUtil.getAccessToken();

                    Log.e(TAG, "___ Access Token is set, calling callNormalAuthenticator | Access token: " + accessToken);
                    //checkExistingAuthRequest();
                    authenticateOtp();

                } else {
                    Log.e(TAG, "deviceCredentialLauncher2: PIN/pattern authentication failed");
                }
            }
    );

    private void callInAppAuthenticator(String userKey, Runnable success) {

        // Show a dialog while processing
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setView(LayoutInflater.from(this).inflate(R.layout.dialog_progress, findViewById(android.R.id.content), false))
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    // cancelExistingAuth();
                    dialogInterface.dismiss(); // Dismiss the dialog
                })
                .create();
        progressDialog.show();


        BsaSdk.getInstance().getSdkService().appAuthenticator(userKey, true, this, new SdkAuthResponseCallback<>() {
            @Override
            public void onSuccess(AuthResultResponse authResultResponse) {
                Log.d(TAG, "In-app authentication successful: code: " + authResultResponse.getRtCode());
                String accessToken = SdkUtil.getAccessToken();
                Log.e(TAG, "[callInAppAuthenticator]___ Access Token is set, | Access token: " + accessToken);
                progressDialog.dismiss(); // Dismiss the dialog when authentication fails
                success.run();
            }

            @Override
            public void onProcess(boolean b, String s) {
                Log.d(TAG, "In-app authentication processing...: s: " + s);
                Snackbar.make(findViewById(android.R.id.content), "In-app authentication processing...: s: " + s, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                progressDialog.dismiss(); // Dismiss the dialog when authentication fails
                Log.e(TAG, "In-app authentication failed: " + errorResult.getErrorMessage() + " | code: " + errorResult.getErrorCode());
                runOnUiThread(() -> handleAuthError(errorResult.getErrorCode()));
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void handleAuthError(int errorCode) {
        String title = "Authentication Error";
        String description;
        String solution;

        switch (errorCode) {
            case 2004:
                description = "Channel does not exist";
                solution = "Check the URL used for initialization. If it happens constantly, please inquire the person in charge.";
                break;
            case 2010:
                description = "User authentication in-progress";
                solution = "Cancel previous authentication and request for a new one.";
                break;
            case 2100:
            case 2105:
            case 2107:
                description = "Token is expired or not found";
                solution = "Retry to authenticate.";
                break;
            case 2101:
            case 2102:
            case 2103:
                description = "Invalid token";
                solution = "Cancel previous authentication and request for a new one.";
                break;
            case 2106:
                description = "Unmatched signature token";
                solution = "Retry to authenticate.";
                break;
            case 5010:
                description = "Authentication failure";
                solution = "Contact the person in charge to solve this matter.";
                break;
            case 5011:
                description = "User authentication canceled";
                solution = "Make the person in charge solve this matter.";
                break;
            case 5015:
                description = "Failed to create channel";
                solution = "It can occur when the parameters are not enough. If it happens constantly, please inquire the person in charge.";
                break;
            case 5017:
                description = "Failed to send push notification";
                solution = "Problems with FCM (Firebase Cloud Messaging) etc. Also check whether the updated token is the correct one.";
                break;
            case 5022:
                description = "Verification failure";
                solution = "Node verification failed. If it happens constantly, please inquire the person in charge.";
                break;
            case 5000:
                description = "Socket Time Exception - Read timed out";
                solution = "Retry to authenticate.";
                break;
            default:
                description = "Unknown error";
                solution = "Please contact the person in charge.";
                break;
        }
    }
}