package com.shopwallet.ituchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fnsv.bsa.sdk.BsaSdk;
import com.shopwallet.ituchallenger.util.SecureStorageUtil;

import java.util.HashMap;

/**
 * TotpAuth class handles the generation and display of Time-based One-Time Password (TOTP) codes for the user.
 * It retrieves a secret key stored securely, generates a TOTP code using the BSA SDK, and displays it to the user.
 * The activity also includes error handling for key retrieval and TOTP generation.
 */
public class TotpAuth extends AppCompatActivity {

    private static final String TAG = "TotpAuthClass";  // Tag for logging purposes

    private TextView totpTextView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_totp_auth);

        // Set up the toolbar with back navigation and title
        Toolbar toolbar = findViewById(R.id.tOtpToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.totp_code);
        }

        // Initialize UI components
        totpTextView = findViewById(R.id.totpTextView);
        Button closeButton = findViewById(R.id.closeButton);
        progressBar = findViewById(R.id.tOtpProgressBar);
        progressBar.setVisibility(View.INVISIBLE); // Hide the progress bar initially

        // Retrieve user key and secret key from secure storage
        HashMap<String, Object> inputData = SecureStorageUtil.retrieveDataFromKeystore(getApplicationContext(), "inputData");
        String userKey = (String) inputData.get("userKey");

        String secretKey = SecureStorageUtil.retrieveSecretKeyFromKeystore(getApplicationContext());
        Log.e(TAG, "###### user Key: " + userKey);
        Log.e(TAG, "###### secretKey: " + secretKey);

        // If secret key is not available, show an error message
        if (secretKey.isEmpty()) {
            showToast("No Secret Key Available, Delete & Create Account again");
            totpTextView.setText(R.string.no_secret_key);
        } else {
            // Request TOTP code if the secret key is available
            requestTotpCode(secretKey);
        }

        // Set up the close button to navigate back to the dashboard
        closeButton.setOnClickListener(v -> {
            Intent intent = new Intent(TotpAuth.this, Dashboard.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle the back button in the toolbar
            startActivity(new Intent(this, Dashboard.class)); // Navigate back to the dashboard
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Requests the TOTP code from the BSA SDK using the provided user key.
     * Displays the TOTP code or an error message based on the result.
     *
     * @param userKey The user's unique key used to generate the TOTP code.
     */
    private void requestTotpCode(String userKey) {
        progressBar.setVisibility(View.VISIBLE); // Show the progress bar while loading

        // Run the TOTP code request in a background thread
        new Thread(() -> {
            try {
                // Generate the TOTP code using the BSA SDK
                String totpCode = BsaSdk.getInstance().getSdkService().getTotpCode(userKey);
                Log.e(TAG, "totpCode: " + totpCode);

                // Update the UI with the TOTP code on the main thread
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (totpCode != null) {
                        totpTextView.setText(formatTotpCode(totpCode)); // Format and display the TOTP code
                        totpTextView.setVisibility(View.VISIBLE);
                        totpTextView.setTextColor(Color.BLACK);
                    } else {
                        showToast("Failed to get TOTP code");
                    }
                });
            } catch (IllegalArgumentException e) {
                // Handle base32 decoding error
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Base32 decoding error: " + e.getMessage(), e);
                    showToast("Base32 decoding error: " + e.getMessage());
                });
            } catch (Exception e) {
                // Handle any other errors that occur during TOTP generation
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Failed to get TOTP code: " + e.getMessage(), e);
                    showToast("Failed to get TOTP code: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Formats the TOTP code by adding a space in the middle for better readability.
     *
     * @param totpCode The TOTP code to be formatted.
     * @return The formatted TOTP code with a space in the middle.
     */
    private String formatTotpCode(String totpCode) {
        if (totpCode == null || totpCode.length() != 6) {
            return totpCode;
        }
        return totpCode.substring(0, 3) + " " + totpCode.substring(3); // Format the TOTP code
    }

    /**
     * Shows a toast message on the screen.
     *
     * @param message The message to be displayed in the toast.
     */
    private void showToast(String message) {
        Toast.makeText(TotpAuth.this, message, Toast.LENGTH_LONG).show(); // Display a long toast message
    }
}