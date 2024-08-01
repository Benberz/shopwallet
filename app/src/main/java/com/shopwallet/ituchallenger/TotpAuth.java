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

public class TotpAuth extends AppCompatActivity {

    private static final String TAG = "TotpAuthClass";

    private TextView totpTextView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_totp_auth);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.tOtpToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.totp_code);
        }

        totpTextView = findViewById(R.id.totpTextView);
        Button closeButton = findViewById(R.id.closeButton);
        progressBar = findViewById(R.id.tOtpProgressBar);
        progressBar.setVisibility(View.INVISIBLE);

        // Replace with actual user key retrieval
         HashMap<String, Object> inputData = SecureStorageUtil.retrieveDataFromKeystore(getApplicationContext(), "inputData");
        String userKey = (String) inputData.get("userKey");

        String secretKey = SecureStorageUtil.retrieveSecretKeyFromKeystore(getApplicationContext());
        Log.e(TAG, "###### user Key: " + userKey);
        Log.e(TAG, "###### secretKey: " + secretKey);

        if (secretKey.isEmpty()) {
            showToast("No Secret Key Available, Delete & Create Account again");
            totpTextView.setText(R.string.no_secret_key);
        } else {
            requestTotpCode(secretKey);
        }
        closeButton.setOnClickListener(v -> {
            Intent intent = new Intent(TotpAuth.this, Dashboard.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button click
            startActivity(new Intent(this, Dashboard.class)); // to navigate back to MainActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestTotpCode(String userKey) {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                String totpCode = BsaSdk.getInstance().getSdkService().getTotpCode(userKey);
                Log.e(TAG, "totpCode: " + totpCode);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (totpCode != null) {
                        totpTextView.setText(formatTotpCode(totpCode));
                        totpTextView.setVisibility(View.VISIBLE);
                        totpTextView.setTextColor(Color.BLACK);
                    } else {
                        showToast("Failed to get TOTP code");
                    }
                });
            } catch (IllegalArgumentException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Base32 decoding error: " + e.getMessage(), e);
                    showToast("Base32 decoding error: " + e.getMessage());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Failed to get TOTP code: " + e.getMessage(), e);
                    showToast("Failed to get TOTP code: " + e.getMessage());
                });
            }
        }).start();
    }

    private String formatTotpCode(String totpCode) {
        if (totpCode == null || totpCode.length() != 6) {
            return totpCode;
        }
        return totpCode.substring(0, 3) + " " + totpCode.substring(3);
    }

    private void showToast(String message) {
        Toast.makeText(TotpAuth.this, message, Toast.LENGTH_LONG).show();
    }
}