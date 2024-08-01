package com.shopwallet.ituchallenger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import org.json.JSONException;
import org.json.JSONObject;

public class ScanToPayWithQR extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final String TAG = "ScanToPayWithQRClass";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_to_pay_with_qr);

        FloatingActionButton buttonClose = findViewById(R.id.closeScanQRFloatingActionButton);
        buttonClose.setOnClickListener(view -> finish());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            startQRCodeScanner();
        }
    }

    private void startQRCodeScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan a QR code");
        integrator.setCameraId(0);  // Use a specific camera of the device
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.setOrientationLocked(true);  // Ensure the orientation is not locked by the scanner itself
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                processQRCode(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
            Log.e(TAG, "requestCode: " +  requestCode + " | resultCode: " + resultCode);
        }
    }

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