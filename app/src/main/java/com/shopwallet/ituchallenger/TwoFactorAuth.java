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

/**
 * The TwoFactorAuth class handles the selection and registration of two-factor authentication methods
 * (Biometric or PIN/Pattern) for the ShopWallet application. It integrates with the BsaSdk to register
 * biometric data or authenticate using a device PIN/Pattern.
 */
@SuppressWarnings("ALL")
public class TwoFactorAuth extends AppCompatActivity {

    private static final String TAG = "TwoFactorAuthClass"; // Tag for logging

    private CardView selectBiometricCardView;
    private CardView selectPinPatternCardView;
    private Button nextTwoFAButton;
    private ProgressBar progressBar;

    private boolean isBiometricSelected = false; // Flag to track selected authentication method
    private HashMap<String, Object> inputData;   // Data passed between activities

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_factor_auth);

        // Retrieve the inputData from the Intent
        Intent intent = getIntent();
        inputData = (HashMap<String, Object>) intent.getSerializableExtra("inputData");

        // Initialize UI components
        selectBiometricCardView = findViewById(R.id.selectBiometricCardView);
        selectPinPatternCardView = findViewById(R.id.selectPinPatternCardView);
        nextTwoFAButton = findViewById(R.id.selectAuthTypeNextButton);
        progressBar = findViewById(R.id.twoFactorProgressBar);
        progressBar.setVisibility(View.GONE); // Hide the progress bar initially

        // Set up click listeners for the authentication options
        selectBiometricCardView.setOnClickListener(v -> {
            handleSelection(selectBiometricCardView, "Biometric");
            isBiometricSelected = true; // Set flag to indicate biometric is selected
            setupBiometricPrompt(); // Set up biometric prompt
        });

        selectPinPatternCardView.setOnClickListener(v -> {
            handleSelection(selectPinPatternCardView, "PIN/Pattern");
            isBiometricSelected = false; // Set flag to indicate PIN/Pattern is selected
            checkPinPatternSetup(); // Check if device PIN/Pattern is set up
        });

        // Set up click listener for the Next button
        nextTwoFAButton.setOnClickListener(view -> {
            progressBar.setVisibility(View.VISIBLE); // Show progress bar
            if (isBiometricSelected) {
                registerBiometric(); // Register biometric authentication
            } else {
                registerPinPattern(); // Register PIN/Pattern authentication
            }
        });

        nextTwoFAButton.setEnabled(false); // Disable Next button initially
    }

    /**
     * Handles the UI changes when a user selects an authentication method.
     *
     * @param selectedCardView The CardView that was selected.
     * @param selectionType The type of authentication selected (Biometric or PIN/Pattern).
     */
    private void handleSelection(CardView selectedCardView, String selectionType) {
        resetSelections(); // Reset other selections
        selectedCardView.setCardBackgroundColor(Color.LTGRAY); // Highlight the selected card
        Toast.makeText(this, selectionType + " selected", Toast.LENGTH_SHORT).show(); // Show selection toast
    }

    /**
     * Resets the background color of both authentication option CardViews.
     */
    private void resetSelections() {
        selectBiometricCardView.setCardBackgroundColor(Color.WHITE);
        selectPinPatternCardView.setCardBackgroundColor(Color.WHITE);
    }

    /**
     * Registers the biometric authentication method using the BsaSdk.
     */
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
                    saveInputDataAndNavigate(); // Save input data and navigate to the next screen
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

    /**
     * Registers the PIN/Pattern authentication method using the BsaSdk.
     */
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
                    saveInputDataAndNavigate(); // Save input data and navigate to the next screen
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

    /**
     * Saves the input data securely and navigates to the appropriate next screen.
     */
    private void saveInputDataAndNavigate() {
        progressBar.setVisibility(View.GONE); // Hide progress bar
        Intent intent = getIntent();
        if ("signIn".equals(intent.getStringExtra("from"))) {
            // Retrieve inputData securely before navigating to Dashboard
            HashMap<String, Object> storedInputData = SecureStorageUtil.saveDataToKeystore(getApplicationContext(), "inputData", inputData);
            Log.d(TAG, "Auth Type: " + inputData.get("authType"));
            Intent dashboardIntent = new Intent(TwoFactorAuth.this, DeviceReRegistration.class);
            dashboardIntent.putExtra("inputData", storedInputData);
            startActivity(dashboardIntent);
            finish(); // Finish current activity
        } else {
            Intent agreementsIntent = new Intent(TwoFactorAuth.this, Agreements.class);
            agreementsIntent.putExtra("inputData", inputData);
            startActivity(agreementsIntent);
            finish(); // Finish current activity
        }
    }

    /**
     * Sets up the BiometricPrompt for biometric authentication.
     */
    @SuppressLint("SwitchIntDef")
    private void setupBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this); // Use main thread executor
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
                nextTwoFAButton.setEnabled(true); // Enable Next button on success
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
                Toast.makeText(this, "Your device does not have biometric authentication", Toast.LENGTH_LONG).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.e(TAG, "Biometric features are currently unavailable.");
                Toast.makeText(this, "Biometric authentication is currently unavailable", Toast.LENGTH_LONG).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Log.e(TAG, "User has not associated any biometric credentials with their account.");
                Toast.makeText(this, "No biometric credentials enrolled", Toast.LENGTH_LONG).show();
                break;
        }
    }

    /**
     * Checks if a device PIN, pattern, or password is set up.
     */
    private void checkPinPatternSetup() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager.isKeyguardSecure()) {
            Toast.makeText(this, "Device PIN/Pattern/Password is set up", Toast.LENGTH_LONG).show();
            nextTwoFAButton.setEnabled(true); // Enable Next button if secure
        } else {
            Toast.makeText(this, "Please set up a PIN/Pattern/Password in device settings", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            startActivity(intent);
        }
    }
}