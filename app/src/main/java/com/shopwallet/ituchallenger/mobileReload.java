package com.shopwallet.ituchallenger;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.shopwallet.ituchallenger.util.SecureStorageUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class mobileReload extends AppCompatActivity {

    private static final String TAG = "mobileReload";

    private EditText phoneNumber;
    private EditText receiverName;
    private EditText creditAmount;
    private ProgressBar mobileReloadProgressBar;
    private FirebaseFirestore db;
    private String walletBalanceDocRef;
    private String phoneNumberStr;
    private String amountStr;
    private String holderRefId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_reload);

        // Set up the toolbar for wallet Top Up
        Toolbar toolbar = findViewById(R.id.mobileReloadToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.mobile_reload);
        }

        // Retrieve inputData from SecureStorageUtil
        HashMap<String, Object> inputData = SecureStorageUtil.retrieveDataFromKeystore(this, "inputData");
        walletBalanceDocRef = (String) inputData.get("balanceRefId");
        holderRefId = (String) inputData.get("holderRefId");

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get the inputs
        phoneNumber = findViewById(R.id.phoneNumberReloadInput);
        receiverName = findViewById(R.id.receiverNameInput);
        creditAmount = findViewById(R.id.creditAmount);
        mobileReloadProgressBar = findViewById(R.id.mobileReloadProgressBar);
        Button mobileReloadSubmitButton = findViewById(R.id.submitMobileReloadButton);

        mobileReloadProgressBar.setVisibility(View.INVISIBLE);

        phoneNumberStr = phoneNumber.getText().toString().trim();

        // Add TextWatcher to phoneNumber EditText
        phoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (charSequence.length() <= 11) {
                    setIcon(receiverName, 0);
                    receiverName.setText("");
                    setIcon(phoneNumber, 0);
                    phoneNumberStr = phoneNumber.getText().toString().trim();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() == 11) {
                    validatePhoneNumberAndFetchName();
                    Log.d(TAG, "validatePhoneNumberAndFetchName(); (afterTextChanged)");
                    setIcon(phoneNumber, R.drawable.ic_success);
                } else {
                    setIcon(receiverName, 0);
                    setIcon(phoneNumber, 0);
                }
            }
        });

        // Add TextWatcher to creditAmount EditText
        creditAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                setIcon(creditAmount, 0);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (validateInputs()) {
                    checkUserBalance(Double.parseDouble(amountStr));
                }
            }
        });

        // Set OnClickListener for the submit button
        mobileReloadSubmitButton.setOnClickListener(view -> {
            if (validateInputs()) {
                performMobileReload(phoneNumberStr, Double.parseDouble(amountStr));
            }
        });
    }

    private boolean validateInputs() {
        phoneNumberStr = phoneNumber.getText().toString().trim();
        amountStr = creditAmount.getText().toString().trim();

        if (phoneNumberStr.isEmpty()) {
            phoneNumber.setError("Phone number is required");
            return false;
        }

        if (amountStr.isEmpty()) {
            creditAmount.setError("Credit amount is required");
            return false;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                creditAmount.setError("Amount must be greater than zero");
                return false;
            }
        } catch (NumberFormatException e) {
            creditAmount.setError("Invalid amount");
            return false;
        }

        return true;
    }

    private void validatePhoneNumberAndFetchName() {
        CollectionReference walletHoldersRef = db.collection("itu_challenge_wallet_holders");
        Query query = walletHoldersRef.whereEqualTo("phoneNum", phoneNumberStr);

        Log.d(TAG, "-------phoneNumberStr: " + phoneNumberStr);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                String queriedPhoneNumber = document.getString("phoneNum");

                Log.d(TAG, "--------queriedPhoneNumber: " + queriedPhoneNumber);

                if (phoneNumberStr.equals(queriedPhoneNumber)) {
                    setIcon(phoneNumber, R.drawable.ic_success);
                    String name = document.getString("name");
                    receiverName.setText(name);
                    receiverName.setEnabled(false);
                    setIcon(receiverName, R.drawable.ic_success);
                } else {
                    phoneNumber.setError("Phone number mismatch");
                    receiverName.setText("");
                    setIcon(phoneNumber, R.drawable.ic_failure);
                    setIcon(receiverName, R.drawable.ic_failure);
                }
            } else {
                phoneNumber.setError("Phone number not found");
                receiverName.setText("");
                setIcon(phoneNumber, R.drawable.ic_failure);
                setIcon(receiverName, R.drawable.ic_failure);
            }
        });
    }

    private void setIcon(EditText editText, int iconResId) {
        Drawable icon = iconResId != 0 ? ContextCompat.getDrawable(this, iconResId) : null;
        if (iconResId == 0) {
            editText.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        } else {
            editText.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
        }
    }

    private void checkUserBalance(double amount) {
        db.collection("itu_challenge_wallet_balances")
                .document(walletBalanceDocRef)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Double currentBalance = task.getResult().getDouble("balance");

                        if (currentBalance != null && currentBalance >= amount) {
                            setIcon(creditAmount, R.drawable.ic_success);
                        } else {
                            creditAmount.setError("Insufficient balance");
                            setIcon(creditAmount, R.drawable.ic_failure);
                        }
                    } else {
                        creditAmount.setError("Failed to retrieve balance");
                        setIcon(creditAmount, R.drawable.ic_failure);
                    }
                });
    }

    private void performMobileReload(String phoneNumber, double amount) {
        mobileReloadProgressBar.setVisibility(View.VISIBLE);

        db.runTransaction(transaction -> {
            DocumentReference senderRef = db.collection("itu_challenge_wallet_balances").document(walletBalanceDocRef);
            DocumentSnapshot senderSnapshot = transaction.get(senderRef);
            double senderCurrentBalance = Objects.requireNonNullElse(senderSnapshot.getDouble("balance"),0.00);

            if (senderCurrentBalance < amount) {
                throw new FirebaseFirestoreException("Insufficient balance", FirebaseFirestoreException.Code.ABORTED);
            }

            double senderNewBalance = senderCurrentBalance - amount;
            transaction.update(senderRef, "balance", senderNewBalance);

            return null;
        }).addOnSuccessListener(aVoid -> {
            new Handler().postDelayed(() -> {
                boolean reloadSuccess = mockMobileReloadAPI(phoneNumber, amount);

                mobileReloadProgressBar.setVisibility(View.INVISIBLE);

                if (reloadSuccess) {
                    showTransactionDialog("Transaction Successful", "Reload of " + amount + " to " + phoneNumber + " was successful.");
                    recordTransaction(amount);
                } else {
                    showTransactionDialog("Transaction Failed", "Mobile reload failed");
                }
            }, 5000); // Simulate 5 seconds delay for API call
        }).addOnFailureListener(e -> {
            mobileReloadProgressBar.setVisibility(View.INVISIBLE);
            showTransactionDialog("Transaction Failed", e.getMessage());
        });
    }

    private void recordTransaction(double amount) {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("title", "Mobile Reload");
        transaction.put("amount", amount);
        transaction.put("datetime", getCurrentDatetime());
        transaction.put("user", holderRefId);

        db.collection("itu_challenge_wallet_transactions")
                .add(transaction)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Transaction recorded successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to record transaction", e));
    }

    private String getCurrentDatetime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private boolean mockMobileReloadAPI(String phoneNumber, double amount) {
        // Mock API call to telecom company
        // Return true for success, false for failure
        Log.d(TAG, "PhoneNumber: " + phoneNumber + " | amount: " + amount);
        return true; // Mock success
    }

    private void showTransactionDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            if (title.equals("Transaction Successful")) {
                startActivity(new Intent(mobileReload.this, Dashboard.class));
                finish();
            }
        });
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(this, Dashboard.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}