package com.shopwallet.ituchallenger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fnsv.bsa.sdk.BsaSdk;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_KEY = BuildConfig.CLIENT_KEY;
    private static final String API_SERVER_URL = BuildConfig.API_SERVER_URL;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private boolean isWriteExternalStorageDenied = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check and request permissions
        if (!checkAndRequestPermissions()) {
            // Permissions not granted, handle as needed (e.g., show rationale or exit app)
             showPermissionsRationale();
            Toast.makeText(this, "[NOT] All necessary permissions granted", Toast.LENGTH_LONG).show();
        } else {
            // Permissions granted, proceed with initialization
            Toast.makeText(this, "All necessary permissions granted", Toast.LENGTH_SHORT).show();
            initializeSdk();
        }

        // Set the Create Account button functionality
        Button createAccountButton = findViewById(R.id.createAccountButton);
        createAccountButton.setOnClickListener(view -> {
            // Start the Create Account Activity
            Intent createAccountIntent = new Intent(MainActivity.this, CreateAccount.class);
            startActivity(createAccountIntent);
        });

        // Set the Sign In button functionality
        Button signInButton = findViewById(R.id.signInButton);
        signInButton.setOnClickListener(view -> {
            // Start the Sign In activity
            Intent signInActivity = new Intent(MainActivity.this, SignIn.class);
            startActivity(signInActivity);
        });
    }

    private boolean checkAndRequestPermissions() {
        String[] permissions = new String[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.VIBRATE,
                    Manifest.permission.WAKE_LOCK,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        }

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    isWriteExternalStorageDenied = true;
                } else {
                    allPermissionsGranted = false;
                }
            }
        }

        if (!allPermissionsGranted || isWriteExternalStorageDenied) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            Toast.makeText(this, "All necessary permissions granted", Toast.LENGTH_SHORT).show();
        }

        return allPermissionsGranted;
    }

    private void showPermissionsRationale() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app requires certain permissions to function properly. Please grant the necessary permissions.")
                .setPositiveButton("Grant", (dialog, which) -> checkAndRequestPermissions())
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .create()
                .show();
    }

    private void initializeSdk() {
        BsaSdk.getInstance().init(
                getApplicationContext(),
                CLIENT_KEY,
                API_SERVER_URL
        );
        Toast.makeText(this, "BsaSdk initialized successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            boolean writeExternalStorageGranted = true;

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        writeExternalStorageGranted = false;
                    } else {
                        allPermissionsGranted = false;
                    }
                }
            }

            if (allPermissionsGranted || writeExternalStorageGranted) {
                Toast.makeText(this, "All necessary permissions granted", Toast.LENGTH_SHORT).show();
                // Permissions granted, proceed with initialization
                initializeSdk();
            } else {
                Toast.makeText(this, "Necessary permissions denied. The app may not work properly.", Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "WRITE_EXTERNAL_STORAGE permission denied. Some features may not work.", Toast.LENGTH_SHORT).show();
                // showPermissionsRationale();
            }
        }
    }
}