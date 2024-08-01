package com.shopwallet.ituchallenger;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionUtils {

    public static final int PERMISSION_REQUEST_CODE = 1;

    public static boolean checkAndRequestPermissions(Activity activity) {
        String[] permissions = {
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
                Manifest.permission.USE_FULL_SCREEN_INTENT
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = append(permissions, Manifest.permission.POST_NOTIFICATIONS);
        }

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(activity, permissions, PERMISSION_REQUEST_CODE);
        }

        return allPermissionsGranted;
    }

    private static String[] append(String[] arr, String element) {
        String[] result = new String[arr.length + 1];
        System.arraycopy(arr, 0, result, 0, arr.length);
        result[arr.length] = element;
        return result;
    }
}
