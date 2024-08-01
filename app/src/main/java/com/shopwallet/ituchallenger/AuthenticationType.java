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

        storedInputData = SecureStorageUtil.retrieveDataFromKeystore(this, "inputData");

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.otpToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.authentication_type_text);
        }

        selectBiometricCardView = findViewById(R.id.biometricCardView);
        selectPinPatternCardView = findViewById(R.id.pinPatternCardView);
        biometricCheckmark = findViewById(R.id.biometricCheckmark);
        pinPatternCheckmark = findViewById(R.id.pinPatternCheckmark);

        biometricCheckmark.setVisibility(View.GONE);
        pinPatternCheckmark.setVisibility(View.GONE);

        progressBar = findViewById(R.id.authTypeProgressBar);
        progressBar.setVisibility(View.GONE);

        selectBiometricCardView.setOnClickListener(v -> {
            handleSelection(selectBiometricCardView, "Biometric");
            setupBiometricPrompt();
        });

        selectPinPatternCardView.setOnClickListener(v -> {
            handleSelection(selectPinPatternCardView, "PIN/Pattern");
            checkPinPatternSetup();
        });

        // set the cancel button functionality
        Button cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            Intent agreementsIntent = new Intent(AuthenticationType.this, Profile.class);
            startActivity(agreementsIntent);
            finish();
        });

        // Check and set the selected authentication type
        checkAuthType();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button click
            startActivity(new Intent(this, Profile.class)); // to navigate back to MainActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleSelection(CardView selectedCardView, String selectionType) {
        resetSelections();
        selectedCardView.setCardBackgroundColor(Color.LTGRAY); // Highlight the selected card
        Toast.makeText(this, selectionType + " selected", Toast.LENGTH_SHORT).show();
    }

    private void resetSelections() {
        selectBiometricCardView.setCardBackgroundColor(Color.WHITE);
        selectPinPatternCardView.setCardBackgroundColor(Color.WHITE);
        biometricCheckmark.setVisibility(View.GONE);
        pinPatternCheckmark.setVisibility(View.GONE);
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
                registerBiometric();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Log.e(TAG, "Authentication failed");
                runOnUiThread(() -> Toast.makeText(AuthenticationType.this, "Authentication failed", Toast.LENGTH_SHORT).show());
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
            registerPinPattern();
        } else {
            Log.e(TAG, "No PIN/Pattern/Password set up.");
            Toast.makeText(this, "No PIN/Pattern/Password set up.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        }
    }

    private void registerBiometric() {
        BsaSdk.getInstance().getSdkService().registerBiometric(this, new SdkResponseCallback<>() {
            @Override
            public void onSuccess(AuthBiometricResponse authBiometric) {
                Log.d(TAG, "Biometric registration successful");
                // Update inputData and navigate on the main thread
                runOnUiThread(() -> {
                    Toast.makeText(AuthenticationType.this, "Biometric registration successful", Toast.LENGTH_LONG).show();
                    storedInputData.put("authType", "3");
                    biometricCheckmark.setVisibility(View.VISIBLE); // Show checkmark
                    saveInputDataAndNavigate();
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

    private void registerPinPattern() {
        BsaSdk.getInstance().getSdkService().authDeviceCredential(AuthenticationType.this, new SdkResponseCallback<>() {
            @Override
            public void onSuccess(AuthBiometricResponse authBiometric) {
                Log.d(TAG, "PIN/Pattern authentication successful");
                // Update inputData and navigate on the main thread
                runOnUiThread(() -> {
                    Toast.makeText(AuthenticationType.this, "PIN/Pattern authentication successful", Toast.LENGTH_LONG).show();
                    storedInputData.put("authType", "4");
                    pinPatternCheckmark.setVisibility(View.VISIBLE); // Show checkmark
                    saveInputDataAndNavigate();
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

    private void saveInputDataAndNavigate() {
        // Save inputData securely
        SecureStorageUtil.saveDataToKeystore(this, "inputData", storedInputData);

        progressBar.setVisibility(View.GONE);
    }

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