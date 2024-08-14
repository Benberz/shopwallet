package com.shopwallet.ituchallenger;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.fnsv.bsa.sdk.BsaSdk;
import com.fnsv.bsa.sdk.callback.SdkResponseCallback;
import com.fnsv.bsa.sdk.response.ErrorResult;
import com.fnsv.bsa.sdk.response.RegisterUserResponse;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.shopwallet.ituchallenger.util.SecureStorageUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class Agreements extends AppCompatActivity {

    // Define a tag for logging
    private static final String TAG = "AgreementsClass";
    // Checkboxes for different agreements
    private CheckBox agreeGccsCheckBox;
    private CheckBox agreePersonCheckBox;
    private CheckBox agreeDeviceCheckBox;

    // A HashMap to store input data from previous activity
    private HashMap<String, Object> inputData;

    // Variable to store the secret key
    private String secretKey = "";

    // Firebase Firestore database instance
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agreements);

        // Initialize Cloud Firestore
        db = FirebaseFirestore.getInstance();

        // Retrieve the inputData from the Intent passed from the previous activity
        Intent intent = getIntent();
        inputData = (HashMap<String, Object>) intent.getSerializableExtra("inputData");

        // Initialize CheckBoxes and Button
        agreeGccsCheckBox = findViewById(R.id.agreeGccsCheckBox);
        agreePersonCheckBox = findViewById(R.id.agreePersonCheckBox);
        agreeDeviceCheckBox = findViewById(R.id.agreeDeviceCheckBox);
        Button agreeAndRegisterNextButton = findViewById(R.id.agreeAndRegisterNextButton);

        // Set an OnClickListener for the agree and register button
        agreeAndRegisterNextButton.setOnClickListener(view -> {
            // Collect the agreement responses and store them in inputData
            inputData.put("agreeGccs", agreeGccsCheckBox.isChecked());
            inputData.put("agreePerson", agreePersonCheckBox.isChecked());
            inputData.put("agreeDevice", agreeDeviceCheckBox.isChecked());

            // Debug: Print inputData to log for debugging
            for (Map.Entry<String, Object> entry : inputData.entrySet()) {
                Log.d(TAG, "Key: " + entry.getKey() + ", Value: " + entry.getValue() + ", Value Type: " + entry.getValue().getClass().getName() + "\n\n");
            }

            // Call the registerUser function to proceed with user registration
            registerUser(inputData);
        });
    }

    /**
     * Registers the user using the BsaSdk and stores the necessary information in Firestore.
     */
    private void registerUser(HashMap<String, Object> inputData) {
        // Get Firebase messaging token and add it to inputData
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> inputData.put("token", token))
                .addOnFailureListener(e -> handleRegistrationError("Failed to get Firebase token: " + e.getMessage()));

        // Debug: Print inputData to log for debugging
        for (Map.Entry<String, Object> entry : inputData.entrySet()) {
            Log.d(TAG, "Key: " + entry.getKey() + ", Value: " + entry.getValue() + ", Value Type: " + entry.getValue().getClass().getName());
        }

        // Register the user using the BsaSdk
        BsaSdk.getInstance().getSdkService().registerUser(inputData, new SdkResponseCallback<>() {
            @Override
            public void onSuccess(RegisterUserResponse result) {
                Log.d(TAG, "User registration successful, " + result.rtCode + " and RtMsg: " + result.rtMsg + " ");
                if (result.data.secretKey == null) {
                    Log.e(TAG, "Secret key is null ++++++++++");
                } else {
                    Log.e(TAG, "Secret key is not null: " + result.data.secretKey);
                    // Store the secret key and proceed with Firestore operations
                    secretKey = result.data.secretKey;
                    inputData.put("secretKey", secretKey);
                    SecureStorageUtil.saveSecretKeyToKeystore(getApplicationContext(), secretKey);

                    // Add user data to Firestore
                    addDocumentToFirestore(inputData);

                    // Save inputData to secure storage
                    SecureStorageUtil.saveDataToKeystore(getApplicationContext(), "inputData", inputData);

                    // Notify the user of successful registration and navigate to the next activity
                    runOnUiThread(() -> {
                        Toast.makeText(Agreements.this, "User registration successful", Toast.LENGTH_LONG).show();
                        // Navigate to the RegistrationCompleted activity
                        Intent registrationCompletedIntent = new Intent(Agreements.this, RegistrationCompleted.class);
                        startActivity(registrationCompletedIntent);
                        finish();
                    });
                }
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                // Handle registration errors
                handleRegistrationError(errorResult.getErrorCode(), errorResult.getErrorMessage());
            }
        });
    }

    /**
     * Adds a new document to Firestore for the registered user or updates an existing inactive document.
     */
    private void addDocumentToFirestore(HashMap<String, Object> inputData) {
        String userKey = Objects.requireNonNull(inputData.get("userKey")).toString();

        // Query the collection to check for an inactive document with the same userKey
        db.collection("itu_challenge_wallet_holders")
                .whereEqualTo("userKey", userKey)
                .whereEqualTo("status", "inactive")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // If document with 'status' == 'inactive' exists, update it
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String documentId = documentSnapshot.getId();

                        // Update the document
                        Map<String, Object> updatedData = new HashMap<>();
                        updatedData.put("status", "active");
                        updatedData.put("modified", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

                        db.collection("itu_challenge_wallet_holders").document(documentId)
                                .update(updatedData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "DocumentSnapshot updated with ID: " + documentId);
                                    runOnUiThread(() -> Toast.makeText(this, "Holder document updated with ID: " + documentId, Toast.LENGTH_LONG).show());
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Error updating document", e);
                                    handleFirestoreError("Error updating document in Firestore: " + e.getMessage());
                                });
                    } else {
                        // If no such document exists, proceed with the normal flow
                        Map<String, Object> documentData = new HashMap<>();
                        documentData.put("name", inputData.get("name"));
                        documentData.put("userKey", inputData.get("userKey"));
                        documentData.put("phoneNum", inputData.get("phoneNum"));
                        documentData.put("email", inputData.get("email"));

                        // Generate a random 10-digit walletId
                        Random random = new Random();
                        int walletId = 1000000000 + random.nextInt(900000000);

                        // Get the current datetime
                        String currentDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                        documentData.put("walletId", String.valueOf(walletId));
                        documentData.put("created", currentDatetime);
                        documentData.put("modified", "");
                        documentData.put("status", "active");

                        db.collection("itu_challenge_wallet_holders")
                                .add(documentData)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                                    runOnUiThread(() -> Toast.makeText(this, "Holder document added with ID: " + documentReference.getId(), Toast.LENGTH_LONG).show());
                                    inputData.put("holderRefId", documentReference.getId());
                                    inputData.put("walletId", String.valueOf(walletId));

                                    // Add document to wallet balances collection
                                    addWalletBalanceToFirestore(String.valueOf(walletId), documentReference.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Error adding document", e);
                                    handleFirestoreError("Error adding document to Firestore: " + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error querying document", e);
                    handleFirestoreError("Error querying document from Firestore: " + e.getMessage());
                });
    }

    /**
     * Adds a wallet balance document to the Firestore collection.
     */
    private void addWalletBalanceToFirestore(String walletId, String documentRefId) {
        Map<String, Object> balanceData = new HashMap<>();
        balanceData.put("walletId", walletId);
        balanceData.put("user", documentRefId);

        // Get the current datetime
        String currentDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        balanceData.put("modified", currentDatetime);
        balanceData.put("balance", 0);

        db.collection("itu_challenge_wallet_balances")
                .add(balanceData)
                .addOnSuccessListener(balanceDocumentReference -> {
                    Log.d(TAG, "Balance document added with ID: " + balanceDocumentReference.getId());
                    inputData.put("balanceRefId", balanceDocumentReference.getId());
                    runOnUiThread(() -> Toast.makeText(this, "Balance document added with ID: " + balanceDocumentReference.getId(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding balance document", e);
                    handleFirestoreError("Error adding balance document to Firestore: " + e.getMessage());
                });
    }

    /**
     * Handles registration errors by displaying an alert dialog.
     */

    private void handleRegistrationError(String errorMessage) {
        Log.e(TAG, errorMessage);
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle("Registration Error")
                .setMessage(errorMessage)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create()
                .show());
    }

    /**
     * Handles registration errors by displaying an alert dialog.
     */
    private void handleRegistrationError(int errorCode, String errorMessage) {
        String userFriendlyMessage;
        switch (errorCode) {
            case 2000:
                userFriendlyMessage = "Invalid client key. Please check your client key and try again.";
                break;
            case 2008:
                userFriendlyMessage = "Unregistered user. Please check your BSA sign-in status.";
                break;
            case 5001:
                userFriendlyMessage = "Authentication timeout. Please authenticate again.";
                break;
            case 5005:
                userFriendlyMessage = "Unauthorized user. Please contact support.";
                break;
            case 5006:
                userFriendlyMessage = "Permanently suspended user. Please contact support.";
                break;
            case 5007:
                userFriendlyMessage = "An unknown error occurred. Please contact support.";
                break;
            default:
                userFriendlyMessage = "User registration failed: " + errorMessage;
                break;
        }
        Log.e(TAG, userFriendlyMessage);
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle("Registration Error")
                .setMessage(userFriendlyMessage)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create()
                .show());
    }

    /**
     * Handles errors related to Firestore operations.
     */
    private void handleFirestoreError(String errorMessage) {
        Log.e(TAG, errorMessage);
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle("Firestore Error")
                .setMessage(errorMessage)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create()
                .show());
    }
}