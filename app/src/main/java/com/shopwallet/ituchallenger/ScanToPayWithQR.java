package com.shopwallet.ituchallenger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONException;
import org.json.JSONObject;

public class ScanToPayWithQR extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100; // Request code for camera permission
    private static final String TAG = "ScanToPayWithQRClass"; // Log tag for debugging

    private final ActivityResultLauncher<ScanOptions> scanContract = registerForActivityResult(new ScanContract(), result -> {
        if (result != null) {
            String qrText = result.getContents();
            if (qrText == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                processQRCode(qrText);
            }
        }
    });

    /**
     * Called when the activity is first created. Initializes the activity, sets up the UI,
     * and checks for camera permission. If permission is granted, it starts the QR code scanner.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *                           shut down then this Bundle contains the data it most recently
     *                           supplied in onSaveInstanceState(Bundle). Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_to_pay_with_qr);

        // Initialize the close button and set its click listener to finish the activity
        FloatingActionButton buttonClose = findViewById(R.id.closeScanQRFloatingActionButton);
        buttonClose.setOnClickListener(view -> finish());

        // Check for camera permission; if not granted, request it
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            startQRCodeScanner();
        }
    }

    /**
     * Starts the QR code scanner using ScanOptions and ScanContract.
     * Configures the scanner with desired barcode formats, prompt message, and other settings.
     */
    private void startQRCodeScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan a QR code");
        options.setCameraId(0);  // Use the rear camera
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(true);
        options.setOrientationLocked(true);  // Ensure orientation is not locked by the scanner

        scanContract.launch(options);
    }

    /**
     * Processes the scanned QR code content. Extracts wallet ID and requested amount,
     * then starts the money transfer activity with the extracted information.
     *
     * @param qrText The content of the scanned QR code.
     */
    private void processQRCode(String qrText) {
        Log.e(TAG, "qrText: " + qrText);
        try {
            JSONObject jsonObject = new JSONObject(qrText);

            if (jsonObject.has("walletId")) {
                String walletId = jsonObject.getString("walletId");
                double requestedAmount = 0.0;

                if (jsonObject.has("requestedAmount")) {
                    requestedAmount = jsonObject.getDouble("requestedAmount");
                }

                Log.d(TAG, "***** walletId: " + walletId + " | requestedAmount: " + requestedAmount);
                double finalRequestedAmount = requestedAmount;
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Captured Wallet ID: " + walletId + " | requestedAmount: " + finalRequestedAmount, Toast.LENGTH_LONG).show());

                // Start the money transfer activity with the extracted wallet ID and requested amount
                Intent intent = new Intent(ScanToPayWithQR.this, moneyTransfer.class);
                intent.putExtra("walletId", walletId);
                intent.putExtra("requestedAmount", requestedAmount);
                startActivity(intent);
                finish();

            } else {
                Toast.makeText(this, "Invalid QR code format: Missing walletId", Toast.LENGTH_SHORT).show();
                finish();
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Handles the result of the camera permission request.
     * If the permission is granted, starts the QR code scanner. Otherwise, shows a denial message.
     *
     * @param requestCode The request code passed in requestPermissions().
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQRCodeScanner();
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
