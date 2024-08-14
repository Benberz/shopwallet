package com.shopwallet.ituchallenger;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.fnsv.bsa.sdk.BsaSdk;
import com.fnsv.bsa.sdk.callback.SdkResponseCallback;
import com.fnsv.bsa.sdk.response.ErrorResult;
import com.fnsv.bsa.sdk.response.ReRegisterDeviceResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.shopwallet.ituchallenger.util.SecureStorageUtil;
import com.shopwallet.ituchallenger.util.SessionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import pl.droidsonroids.gif.GifImageView;

/**
 * This activity handles the device re-registration process.
 * It ensures that the user is logged in anonymously before querying the necessary Firestore documents,
 * and then attempts to re-register the device with the user's account.
 */
public class DeviceReRegistration extends AppCompatActivity {

    private static final String TAG = "DeviceReRegistrationClass";
    private static final int DELAY_DURATION = 5000; // delay for gif display

    GifImageView authStatusImageView;
    TextView authStatusTextView;

    /**
     * Called when the activity is first created.
     * Initializes the UI components and ensures anonymous login before proceeding.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this Bundle contains the most recent data supplied by onSaveInstanceState.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_re_registration);

        // Retrieve input data from Secure Storage
        HashMap<String, Object> inputData = SecureStorageUtil.retrieveDataFromKeystore(DeviceReRegistration.this, "inputData");

        // Set the UI element
        authStatusImageView = findViewById(R.id.inAppAuthenticatingAnimatedImageView);
        authStatusTextView = findViewById(R.id.authenticatingStatusTextView);

        // Ensure anonymous login before proceeding with the flow
        ensureAnonymousLogin(inputData);
    }

    /**
     * Ensures that the user is signed in anonymously. If not, signs in anonymously.
     *
     * @param inputData The input data retrieved from Secure Storage.
     */
    private void ensureAnonymousLogin(HashMap<String, Object> inputData) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            // No user is currently signed in, sign in anonymously
            auth.signInAnonymously().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Sign-in successful, proceed with querying holderRefId
                    Log.d(TAG, "Anonymous sign-in successful.");
                    reRegisterDevice(inputData);
                } else {
                    // Sign-in failed, handle the error
                    Log.e(TAG, "Anonymous sign-in failed.", task.getException());
                    handleSignInError(Objects.requireNonNull(task.getException()));
                }
            });
        } else {
            // User is already signed in, proceed with querying holderRefId
            Log.d(TAG, "User is already signed in.");
            reRegisterDevice(inputData);
        }
    }

    /**
     * Re-registers the device with the server. Queries Firestore for holderRefId, balanceRefId, and linkedBankRef if necessary.
     *
     * @param inputData The input data retrieved from Secure Storage.
     */
    private void reRegisterDevice(HashMap<String, Object> inputData) {
        String email = (String) inputData.get("email");

        if (inputData.get("holderRefId") == null || inputData.get("balanceRefId") == null || inputData.get("walletId") == null) {
            queryHolderRefId(email, inputData);
        } else {
            continueReRegistration(inputData);
        }
    }

    /**
     * Queries Firestore for the holderRefId using the user's email.
     *
     * @param email     The user's email address.
     * @param inputData The input data retrieved from Secure Storage.
     */
    private void queryHolderRefId(String email, HashMap<String, Object> inputData) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("itu_challenge_wallet_holders")
                .whereEqualTo("email", email)
                .whereEqualTo("status", "active")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentReference holderRef = task.getResult().getDocuments().get(0).getReference();
                        inputData.put("holderRefId", holderRef.getId());
                        queryBalanceRefId(holderRef.getId(), inputData);
                    } else {
                        showErrorAndNavigateBack("Failed to find holderRefId");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying holderRefId: " + e.getMessage());
                    handleFirestoreConnectionIssue(e);
                });
    }

    /**
     * Queries Firestore for the balanceRefId using the holderRefId.
     *
     * @param holderRefId The holderRefId obtained from the previous query.
     * @param inputData   The input data retrieved from Secure Storage.
     */
    private void queryBalanceRefId(String holderRefId, HashMap<String, Object> inputData) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("itu_challenge_wallet_balances")
                .whereEqualTo("user", holderRefId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentReference balanceRef = task.getResult().getDocuments().get(0).getReference();
                        String walletId = (String) task.getResult().getDocuments().get(0).get("walletId");
                        inputData.put("balanceRefId", balanceRef.getId());
                        inputData.put("walletId", walletId);

                        SecureStorageUtil.saveDataToKeystore(DeviceReRegistration.this, "inputData", inputData);
                        queryLinkedBankRef(holderRefId, inputData);
                    } else {
                        showErrorAndNavigateBack("Failed to find balanceRefId");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying balanceRefId: " + e.getMessage());
                    handleFirestoreConnectionIssue(e);
                });
    }

    /**
     * Queries Firestore for the linkedBankRef using the holderRefId.
     *
     * @param holderRefId The holderRefId obtained from the previous query.
     * @param inputData   The input data retrieved from Secure Storage.
     */
    private void queryLinkedBankRef(String holderRefId, HashMap<String, Object> inputData) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("itu_challenge_linked_banks")
                .whereEqualTo("user", holderRefId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentReference linkedBankRef = task.getResult().getDocuments().get(0).getReference();
                        inputData.put("linkedBankRef", linkedBankRef.getId());
                        inputData.put("registeredBank", "yes");

                        SecureStorageUtil.saveDataToKeystore(DeviceReRegistration.this, "inputData", inputData);
                        continueReRegistration(inputData);
                    } else {
                        Toast.makeText(DeviceReRegistration.this, "Link your Bank Account when you want to add funds to your wallet", Toast.LENGTH_LONG).show();
                        continueReRegistration(inputData);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying linkedBankRef: " + e.getMessage());
                    Toast.makeText(DeviceReRegistration.this, "Link your Bank Account when you want to add funds to your wallet", Toast.LENGTH_LONG).show();
                    continueReRegistration(inputData);
                });
    }

    /**
     * Continues the device re-registration process after querying Firestore.
     *
     * @param inputData The input data retrieved from Secure Storage.
     */
    private void continueReRegistration(HashMap<String, Object> inputData) {
        Map<String, Object> params = new HashMap<>();
        params.put("userKey", inputData.get("userKey"));
        params.put("name", inputData.get("name"));
        params.put("phoneNum", inputData.get("phoneNum"));
        params.put("email", inputData.get("email"));
        params.put("authType", inputData.get("authType"));
        params.put("otpType", "email"); // or "sms" if using SMS OTP
        params.put("disposeToken", inputData.get("disposeToken"));

        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> params.put("token", token)).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get FCM token: " + e.getMessage());
            showErrorAndNavigateBack("Failed to get FCM token");
        });

        BsaSdk.getInstance().getSdkService().reRegisterUserDevice(params, new SdkResponseCallback<>() {
            @Override
            public void onSuccess(ReRegisterDeviceResponse result) {
                Log.d(TAG, "Device re-registration successful- Result Message:  " + result.rtMsg + " | Code: " + result.getRtCode());
                if (result.getRtCode() == 0) {
                    inputData.put("token", params.get("token"));
                    new Handler().postDelayed(() -> {
                        authStatusImageView.setImageResource(R.drawable.ic_success);
                        authStatusTextView.setText(R.string.auth_success_text);
                    }, DELAY_DURATION);

                    if (result.data.secretKey != null && !result.data.secretKey.isEmpty()) {
                        SecureStorageUtil.saveSecretKeyToKeystore(DeviceReRegistration.this, result.data.secretKey);
                        Log.e(TAG, "Secret key retrieved and saved successfully: " + result.data.secretKey);
                    } else {
                        runOnUiThread(() -> Toast.makeText(DeviceReRegistration.this, "Device not registered to the Account!!!", Toast.LENGTH_LONG).show());
                        finish();
                    }

                    runOnUiThread(() -> Toast.makeText(DeviceReRegistration.this, "Device re-registration successful", Toast.LENGTH_LONG).show());
                    SecureStorageUtil.saveDataToKeystore(DeviceReRegistration.this, "inputData", inputData);
                    SessionManager.getInstance(DeviceReRegistration.this).createSession(inputData);
                    navigateToDashboard();
                } else {
                    showErrorAndNavigateBack(result.rtMsg);
                }
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                Log.e(TAG, "Device re-registration failed: " + errorResult.getErrorMessage());
                if (errorResult.getErrorMessage().contains("Device not registered")) {
                    showNotRegisteredDialog();
                } else {
                    showErrorAndNavigateBack(errorResult.getErrorMessage());
                }
            }
        });
    }

    private void navigateToDashboard() {
        Intent dashboardIntent = new Intent(DeviceReRegistration.this, Dashboard.class);
        startActivity(dashboardIntent);
        finish();
    }

    /**
     * Shows an error message and navigates back to the previous activity.
     *
     * @param errorMessage The error message to be displayed.
     */
    private void showErrorAndNavigateBack(String errorMessage) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
            Intent signInIntent = new Intent(DeviceReRegistration.this, SignIn.class);
            startActivity(signInIntent);
            finish();
        });
    }

    /**
     * Displays a dialog informing the user that the device is not registered to the account.
     * The user is prompted to sign in with the registered device.
     */
    private void showNotRegisteredDialog() {
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle("Device Not Registered")
                .setMessage("Device not registered to this account, please use the registered device.")
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    Intent signInIntent = new Intent(DeviceReRegistration.this, SignIn.class);
                    startActivity(signInIntent);
                    finish();
                })
                .create()
                .show());
    }

    /**
     * Handles the sign-in error and prompts the user to try again later.
     *
     * @param e The exception thrown during the sign-in operation.
     */
    private void handleSignInError(Exception e) {
        showErrorAndNavigateBack("Anonymous sign-in failed: " + e.getMessage());
    }

    /**
     * Handles Firestore connection issues and prompts the user to try again later.
     *
     * @param e The exception thrown during the Firestore operation.
     */
    private void handleFirestoreConnectionIssue(Exception e) {
        showErrorAndNavigateBack("Connection issue: " + e.getMessage());
    }
}