package com.shopwallet.ituchallenger.util;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SecureStorageUtil {

    private static final String KEY_ALIAS = "ShopWalletKeyAlias";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String TAG = "SecureStorageUtil";

    public static HashMap<String, Object> saveDataToKeystore(Context context, String alias, HashMap<String, Object> data) {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
                keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build());
                keyGenerator.generateKey();
            }

            SecretKey secretKey = ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            byte[] encryptedData = cipher.doFinal(data.toString().getBytes());

            // Save encrypted data and IV in shared preferences or a secure place
            context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE).edit()
                    .putString(alias, new String(encryptedData, StandardCharsets.ISO_8859_1))
                    .putString(alias + "_iv", new String(iv, StandardCharsets.ISO_8859_1))
                    .apply();

        } catch (Exception e) {
            Log.e(TAG, "Error saving data to Keystore: " + e.getMessage());
        }
        return data;
    }

    public static HashMap<String, Object> retrieveDataFromKeystore(Context context, String alias) {
        HashMap<String, Object> data = new HashMap<>();
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            SecretKey secretKey = ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // Retrieve encrypted data and IV from shared preferences or a secure place
            String encryptedDataString = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
                    .getString(alias, null);
            String ivString = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
                    .getString(alias + "_iv", null);

            if (encryptedDataString != null && ivString != null) {
                byte[] encryptedData = encryptedDataString.getBytes(StandardCharsets.ISO_8859_1);
                byte[] iv = ivString.getBytes(StandardCharsets.ISO_8859_1);

                GCMParameterSpec spec = new GCMParameterSpec(128, iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
                byte[] decryptedData = cipher.doFinal(encryptedData);

                // Convert the decrypted data back to a HashMap (assuming it was converted to a String before saving)
                String decryptedDataString = new String(decryptedData);
                String[] entries = decryptedDataString.substring(1, decryptedDataString.length() - 1).split(", ");
                for (String entry : entries) {
                    String[] keyValue = entry.split("=");
                    data.put(keyValue[0], keyValue[1]);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving data from Keystore: " + e.getMessage());
        }
        return data;
    }

    public static void saveAccessTokenToKeystore(Context context, String accessToken) {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
                keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build());
                keyGenerator.generateKey();
            }

            SecretKey secretKey = ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            byte[] encryptedData = cipher.doFinal(accessToken.getBytes(StandardCharsets.UTF_8));

            // Save encrypted data and IV in shared preferences or a secure place
            context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE).edit()
                    .putString("accessToken", new String(encryptedData, StandardCharsets.ISO_8859_1))
                    .putString("accessToken_iv", new String(iv, StandardCharsets.ISO_8859_1))
                    .apply();

        } catch (Exception e) {
            Log.e(TAG, "Error saving access token to Keystore: " + e.getMessage());
        }
    }

    public static String retrieveAccessTokenFromKeystore(Context context) {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            SecretKey secretKey = ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // Retrieve encrypted data and IV from shared preferences or a secure place
            String encryptedDataString = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
                    .getString("accessToken", null);
            String ivString = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
                    .getString("accessToken_iv", null);

            if (encryptedDataString != null && ivString != null) {
                byte[] encryptedData = encryptedDataString.getBytes(StandardCharsets.ISO_8859_1);
                byte[] iv = ivString.getBytes(StandardCharsets.ISO_8859_1);

                GCMParameterSpec spec = new GCMParameterSpec(128, iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
                byte[] decryptedData = cipher.doFinal(encryptedData);

                return new String(decryptedData, StandardCharsets.UTF_8);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving access token from Keystore: " + e.getMessage());
        }
        return "";
    }

    public static void saveSecretKeyToKeystore(Context context, String secretKey) {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
                keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build());
                keyGenerator.generateKey();
            }

            SecretKey secretKeyInstance = ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeyInstance);
            byte[] iv = cipher.getIV();
            byte[] encryptedData = cipher.doFinal(secretKey.getBytes(StandardCharsets.UTF_8));

            // Save encrypted data and IV in shared preferences or a secure place
            context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE).edit()
                    .putString("secretKey", new String(encryptedData, StandardCharsets.ISO_8859_1))
                    .putString("secretKey_iv", new String(iv, StandardCharsets.ISO_8859_1))
                    .apply();

        } catch (Exception e) {
            Log.e(TAG, "Error saving secret key to Keystore: " + e.getMessage());
        }
    }

    public static String retrieveSecretKeyFromKeystore(Context context) {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            SecretKey secretKeyInstance = ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // Retrieve encrypted data and IV from shared preferences or a secure place
            String encryptedDataString = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
                    .getString("secretKey", null);
            String ivString = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
                    .getString("secretKey_iv", null);

            if (encryptedDataString != null && ivString != null) {
                byte[] encryptedData = encryptedDataString.getBytes(StandardCharsets.ISO_8859_1);
                byte[] iv = ivString.getBytes(StandardCharsets.ISO_8859_1);

                GCMParameterSpec spec = new GCMParameterSpec(128, iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKeyInstance, spec);
                byte[] decryptedData = cipher.doFinal(encryptedData);

                Log.e(TAG, "retrieved secret key from Keystore:success ");
                return new String(decryptedData, StandardCharsets.UTF_8);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving secret key from Keystore: " + e.getMessage());
        }
        Log.e(TAG, "Error retrieving secret key from Keystore: EMPTY");
        return "";
    }

    public static void clearAllUserPreferences(Context context) {
        try {
            context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE).edit().clear().apply();
            Log.d(TAG, "All user preferences cleared successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing user preferences: " + e.getMessage());
        }
    }

    public static boolean isUserLoggedIn(Context context) {
        try {
            String accessToken = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
                    .getString("accessToken", null);
            return accessToken != null;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if user is logged in: " + e.getMessage());
        }
        return false;
    }
}
