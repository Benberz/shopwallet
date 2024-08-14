package com.shopwallet.ituchallenger;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
 * Activity that allows users to select their authentication method (Biometric or PIN/Pattern).
 * It handles the setup and registration of the selected authentication type.
 */
public class AuthenticationType extends AppCompatActivity {
    private static final String TAG = "AuthenticationTypeClass";

    private CardView selectBiometricCardView;
    private CardView selectPinPatternCardView;
    private ImageView biometricCheckmark;
    private ImageView pinPatternCheckmark;
    private HashMap<String, Object> storedInputData;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication_type);

        // Retrieve stored input data
        storedInputData = SecureStorageUtil.retrieveDataFromKeystore(this, "inputData");

        // Set up the toolbar with navigation and title
        Toolbar toolbar = findViewById(R.id.otpToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.authentication_type_text);
        }

        // Initialize UI elements
        selectBiometricCardView = findViewById(R.id.biometricCardView);
        selectPinPatternCardView = findViewById(R.id.pinPatternCardView);
        biometricCheckmark = findViewById(R.id.biometricCheckmark);
        pinPatternCheckmark = findViewById(R.id.pinPatternCheckmark);
        progressBar = findViewById(R.id.authTypeProgressBar);

        // Hide checkmarks and progress bar initially
        biometricCheckmark.setVisibility(View.GONE);
        pinPatternCheckmark.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        // Set up click listeners for authentication type selection
        selectBiometricCardView.setOnClickListener(v -> {
            handleSelection(selectBiometricCardView, "Biometric");
            setupBiometricPrompt(); // Set up biometric authentication
        });

        selectPinPatternCardView.setOnClickListener(v -> {
            handleSelection(selectPinPatternCardView, "PIN/Pattern");
            checkPinPatternSetup(); // Check PIN/Pattern setup
        });

        // Set up cancel button to navigate back to Profile activity
        Button cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            Intent agreementsIntent = new Intent(AuthenticationType.this, Profile.class);
            startActivity(agreementsIntent);
            finish();
        });

        // Check and display the selected authentication type
        checkAuthType();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button click to navigate to Profile activity
            startActivity(new Intent(this, Profile.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles the selection of an authentication type.
     * @param selectedCardView The selected CardView
     * @param selectionType The type of authentication selected (Biometric or PIN/Pattern)
     */
    private void handleSelection(CardView selectedCardView, String selectionType) {
        resetSelections(); // Reset all selections
        selectedCardView.setCardBackgroundColor(Color.LTGRAY); // Highlight the selected card
        Toast.makeText(this, selectionType + " selected", Toast.LENGTH_SHORT).show();
    }

    /**
     * Resets the UI selections by setting default colors and hiding checkmarks.
     */
    private void resetSelections() {
        selectBiometricCardView.setCardBackgroundColor(Color.WHITE);
        selectPinPatternCardView.setCardBackgroundColor(Color.WHITE);
        biometricCheckmark.setVisibility(View.GONE);
        pinPatternCheckmark.setVisibility(View.GONE);
    }

    /**
     * Sets up and starts the biometric authentication process.
     */
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
                registerBiometric(); // Proceed to register biometric authentication
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Log.e(TAG, "Authentication failed");
                runOnUiThread(() -> Toast.makeText(AuthenticationType.this, "Authentication failed", Toast.LENGTH_SHORT).show());
            }
        });

        // Create and configure the BiometricPrompt dialog
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
                // Prompt user to enroll biometric credentials
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

    /**
     * Checks if a PIN/Pattern is set up on the device.
     */
    @SuppressLint("SwitchIntDef")
    private void checkPinPatternSetup() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager.isDeviceSecure()) {
            Log.d(TAG, "Device is secure with PIN/Pattern/Password.");
            registerPinPattern(); // Proceed to register PIN/Pattern authentication
        } else {
            Log.e(TAG, "No PIN/Pattern/Password set up.");
            Toast.makeText(this, "No PIN/Pattern/Password set up.", Toast.LENGTH_SHORT).show();
            // Prompt user to set up security settings
            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        }
    }

    /**
     * Registers the selected biometric authentication method with the SDK.
     */
    private void registerBiometric() {
        BsaSdk.getInstance().getSdkService().registerBiometric(this, new SdkResponseCallback<>() {
            @Override
            public void onSuccess(AuthBiometricResponse authBiometric) {
                Log.d(TAG, "Biometric registration successful");
                // Update inputData and navigate on the main thread
                runOnUiThread(() -> {
                    Toast.makeText(AuthenticationType.this, "Biometric registration successful", Toast.LENGTH_LONG).show();
                    storedInputData.put("authType", "3"); // Update authType to Biometric
                    biometricCheckmark.setVisibility(View.VISIBLE); // Show checkmark
                    saveInputDataAndNavigate(); // Save data and navigate
                });
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                Log.e(TAG, "Biometric registration failed: " + errorResult.getErrorMessage());
                // Show failure message on the main thread
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AuthenticationType.this, "Biometric registration failed: " + errorResult.getErrorMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Registers the selected PIN/Pattern authentication method with the SDK.
     */
    private void registerPinPattern() {
        BsaSdk.getInstance().getSdkService().authDeviceCredential(AuthenticationType.this, new SdkResponseCallback<>() {
            @Override
            public void onSuccess(AuthBiometricResponse authBiometric) {
                Log.d(TAG, "PIN/Pattern authentication successful");
                // Update inputData and navigate on the main thread
                runOnUiThread(() -> {
                    Toast.makeText(AuthenticationType.this, "PIN/Pattern authentication successful", Toast.LENGTH_LONG).show();
                    storedInputData.put("authType", "4"); // Update authType to PIN/Pattern
                    pinPatternCheckmark.setVisibility(View.VISIBLE); // Show checkmark
                    saveInputDataAndNavigate(); // Save data and navigate
                });
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                Log.e(TAG, "PIN/Pattern authentication failed: " + errorResult.getErrorMessage());
                // Show failure message on the main thread
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AuthenticationType.this, "PIN/Pattern authentication failed: " + errorResult.getErrorMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Saves the input data securely and hides the progress bar.
     */
    private void saveInputDataAndNavigate() {
        // Save inputData securely
        SecureStorageUtil.saveDataToKeystore(this, "inputData", storedInputData);

        progressBar.setVisibility(View.GONE);
    }

    /**
     * Checks the previously selected authentication type and updates UI accordingly.
     */
    private void checkAuthType() {
        String authType = (String) storedInputData.get("authType");
        if (authType != null) {
            switch (authType) {
                case "3":
                    handleSelection(selectBiometricCardView, "Biometric");
                    biometricCheckmark.setVisibility(View.VISIBLE);
                    break;
                case "4":
                    handleSelection(selectPinPatternCardView, "PIN/Pattern");
                    pinPatternCheckmark.setVisibility(View.VISIBLE);
                    break;
                default:
                    resetSelections();
                    break;
            }
        }
    }
}
