package com.shopwallet.ituchallenger;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.fnsv.bsa.sdk.BsaSdk;
import com.fnsv.bsa.sdk.callback.SdkResponseCallback;
import com.fnsv.bsa.sdk.response.AuthBiometricResponse;
import com.fnsv.bsa.sdk.response.ErrorResult;
import com.shopwallet.ituchallenger.util.SecureStorageUtil;

import java.util.HashMap;
import java.util.concurrent.Executor;

@SuppressWarnings("ALL")
public class TwoFactorAuth extends AppCompatActivity {

    private static final String TAG = "TwoFactorAuthClass";

    private CardView selectBiometricCardView;
    private CardView selectPinPatternCardView;
    private Button nextTwoFAButton;
    private ProgressBar progressBar;

    private boolean isBiometricSelected = false;
    private HashMap<String, Object> inputData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_factor_auth);

        // Retrieve the inputData from the Intent
        Intent intent = getIntent();
        inputData = (HashMap<String, Object>) intent.getSerializableExtra("inputData");

        selectBiometricCardView = findViewById(R.id.selectBiometricCardView);
        selectPinPatternCardView = findViewById(R.id.selectPinPatternCardView);
        nextTwoFAButton = findViewById(R.id.selectAuthTypeNextButton);
        progressBar = findViewById(R.id.twoFactorProgressBar);
        progressBar.setVisibility(View.GONE);

        selectBiometricCardView.setOnClickListener(v -> {
            handleSelection(selectBiometricCardView, "Biometric");
            isBiometricSelected = true;
            setupBiometricPrompt();
        });

        selectPinPatternCardView.setOnClickListener(v -> {
            handleSelection(selectPinPatternCardView, "PIN/Pattern");
            isBiometricSelected = false;
            checkPinPatternSetup();
        });

        nextTwoFAButton.setOnClickListener(view -> {
            progressBar.setVisibility(View.VISIBLE);
            if (isBiometricSelected) {
                registerBiometric();
            } else {
                registerPinPattern();
            }
        });

        nextTwoFAButton.setEnabled(false);
    }

    private void handleSelection(CardView selectedCardView, String selectionType) {
        resetSelections();
        selectedCardView.setCardBackgroundColor(Color.LTGRAY); // Highlight the selected card
        Toast.makeText(this, selectionType + " selected", Toast.LENGTH_SHORT).show();
    }

    private void resetSelections() {
        selectBiometricCardView.setCardBackgroundColor(Color.WHITE);
        selectPinPatternCardView.setCardBackgroundColor(Color.WHITE);
    }

    private void registerBiometric() {
        BsaSdk.getInstance().getSdkService().registerBiometric(this, new SdkResponseCallback<>() {
            @Override
            public void onSuccess(AuthBiometricResponse authBiometric) {
                Log.d(TAG, "Biometric registration successful");
                // Update inputData and navigate on the main thread
                runOnUiThread(() -> {
                    Toast.makeText(TwoFactorAuth.this, "Biometric registration successful", Toast.LENGTH_LONG).show();
                    inputData.put("authType", "3");
                    inputData.put("otpType", "email");
                    saveInputDataAndNavigate();
                });
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                Log.e(TAG, "Biometric registration failed: " + errorResult.getErrorMessage());
                // Show failure message on the main thread
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(TwoFactorAuth.this, "Biometric registration failed: " + errorResult.getErrorMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void registerPinPattern() {
        BsaSdk.getInstance().getSdkService().authDeviceCredential(TwoFactorAuth.this, new SdkResponseCallback<>() {
            @Override
            public void onSuccess(AuthBiometricResponse authBiometric) {
                Log.d(TAG, "PIN/Pattern authentication successful");
                // Update inputData and navigate on the main thread
                runOnUiThread(() -> {
                    Toast.makeText(TwoFactorAuth.this, "PIN/Pattern authentication successful", Toast.LENGTH_LONG).show();
                    inputData.put("authType", "4");
                    inputData.put("otpType", "email");
                    saveInputDataAndNavigate();
                });
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                Log.e(TAG, "PIN/Pattern authentication failed: " + errorResult.getErrorMessage());
                // Show failure message on the main thread
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(TwoFactorAuth.this, "PIN/Pattern authentication failed: " + errorResult.getErrorMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void saveInputDataAndNavigate() {
        progressBar.setVisibility(View.GONE);
        Intent intent = getIntent();
        if ("signIn".equals(intent.getStringExtra("from"))) {
            // Retrieve inputData securely before navigating to Dashboard
            HashMap<String, Object> storedInputData = SecureStorageUtil.saveDataToKeystore(getApplicationContext(), "inputData", inputData);
            Log.d(TAG, "Auth Type: " + inputData.get("authType"));
            Intent dashboardIntent = new Intent(TwoFactorAuth.this, DeviceReRegistration.class);
            dashboardIntent.putExtra("inputData", storedInputData);
            startActivity(dashboardIntent);
            finish();
        } else {
            Intent agreementsIntent = new Intent(TwoFactorAuth.this, Agreements.class);
            agreementsIntent.putExtra("inputData", inputData);
            startActivity(agreementsIntent);
            finish();
        }
    }

    @SuppressLint("SwitchIntDef")
    private void setupBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Log.e(TAG, "Authentication error: " + errString);
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Log.d(TAG, "Authentication succeeded!");
                nextTwoFAButton.setEnabled(true); // Enable Next button
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Log.e(TAG, "Authentication failed");
                runOnUiThread(() -> Toast.makeText(TwoFactorAuth.this, "Authentication failed", Toast.LENGTH_SHORT).show());
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Select Biometric")
                .setSubtitle("Authenticate to Select Biometric")
                .setNegativeButtonText("Cancel")
                .build();

        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d(TAG, "App can authenticate using biometrics.");
                biometricPrompt.authenticate(promptInfo);
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.e(TAG, "No biometric features available on this device.");
                Toast.makeText(this, "No biometric features available on this device.", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.e(TAG, "Biometric features are currently unavailable.");
                Toast.makeText(this, "Biometric features are currently unavailable.", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Log.e(TAG, "The user hasn't associated any biometric credentials with their account.");
                Toast.makeText(this, "No biometric credentials found.", Toast.LENGTH_SHORT).show();
                Intent enrollIntent = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    enrollIntent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
                }
                startActivity(enrollIntent);
                break;
            default:
                Log.e(TAG, "Unknown error occurred.");
                Toast.makeText(this, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @SuppressLint("SwitchIntDef")
    private void checkPinPatternSetup() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager.isDeviceSecure()) {
            Log.d(TAG, "Device is secure with PIN/Pattern/Password.");
            nextTwoFAButton.setEnabled(true); // Enable Next button
        } else {
            Log.e(TAG, "No PIN/Pattern/Password set up.");
            Toast.makeText(this, "No PIN/Pattern/Password set up.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        }
    }
}