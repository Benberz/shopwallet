package com.shopwallet.ituchallenger;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

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

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class moneyTransfer extends AppCompatActivity {

    private static final String TAG = "moneyTransferClass";
    private EditText walletId;
    private EditText recipientName;
    private EditText transferAmount;
    private ProgressBar transferAmountProgressBar;
    private FirebaseFirestore db;
    private String walletBalanceDocRef;
    private String walletIdStr;
    private String amountStr;
    private String holderRefId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_money_transfer);

        // Set up the toolbar for money transfer
        Toolbar toolbar = findViewById(R.id.moneyTransferToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.money_transfer);
        }

        HashMap<String, Object> inputData = SecureStorageUtil.retrieveDataFromKeystore(this, "inputData");

        walletId = findViewById(R.id.phoneNumberReloadInput);
        recipientName = findViewById(R.id.receiverNameInput);
        transferAmount = findViewById(R.id.creditAmount);

        transferAmountProgressBar = findViewById(R.id.moneyTransferProgressBar);
        Button submitTransfer = findViewById(R.id.submitMoneyTransferButton);
        ImageButton scanToPayButton = findViewById(R.id.scanToPayImageButton);

        transferAmountProgressBar.setVisibility(View.INVISIBLE);

        db = FirebaseFirestore.getInstance();
        walletBalanceDocRef = (String) inputData.get("balanceRefId");
        holderRefId = (String) inputData.get("holderRefId");

        // Debug: Print inputData to log
        for (Map.Entry<String, Object> entry : inputData.entrySet()) {
            Log.d(TAG, "Key: " + entry.getKey() + ", Value: " + entry.getValue() + ", Value Type: " + entry.getValue().getClass().getName());
        }

        Log.d(TAG, "walletBalanceDocRef: " + walletBalanceDocRef);

        // Add TextWatcher to walletId EditText
        walletId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                // No action needed before text changes
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (charSequence.length() < 10) {
                    // Clear failure icon if text length is less than 10
                    setIcon(recipientName, 0);
                    recipientName.setText("");
                    setIcon(walletId, 0);
                } else if (charSequence.length() == 10) {
                    validateWalletIdAndProceed();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() == 10) {
                    validateWalletIdAndProceed();
                }
            }
        });

        submitTransfer.setOnClickListener(view -> {
            if (validateInputs()) {
                checkSenderBalanceAndTransfer(walletIdStr, Double.parseDouble(amountStr));
            }
        });

        scanToPayButton.setOnClickListener(v -> {
            // implement the BottomDialogSheet to load the ScanToPayWithQr Activity here
            Intent intent = new Intent(moneyTransfer.this, ScanToPayWithQR.class);
            startActivity(intent);
        });

        // Retrieve values passed from ScanToPayWithQR activity
        Intent intent = getIntent();
        String scannedWalletId = intent.getStringExtra("walletId");
        double requestedAmount = intent.getDoubleExtra("requestedAmount", 0.0);

        if (scannedWalletId != null && requestedAmount > 0) {
            TextView requestedPaymentStatus = findViewById(R.id.noteDeductionTextView);
            requestedPaymentStatus.setText(R.string.process_requested_amount_text);
            transferAmountProgressBar.setVisibility(View.VISIBLE);
            checkSenderBalanceAndTransfer(scannedWalletId, requestedAmount);
        } else if (scannedWalletId != null && (!scannedWalletId.isEmpty())) {
            walletId.setText(scannedWalletId);
            transferAmount.requestFocus();
            validateWalletIdAndProceed();
        }
    }

    private boolean validateInputs() {
        walletIdStr = walletId.getText().toString().trim();
        amountStr = transferAmount.getText().toString().trim();

        if (walletIdStr.isEmpty()) {
            walletId.setError("Wallet ID is required");
            return false;
        }

        if (amountStr.isEmpty()) {
            transferAmount.setError("Transfer amount is required");
            return false;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                transferAmount.setError("Amount must be greater than zero");
                return false;
            }
        } catch (NumberFormatException e) {
            transferAmount.setError("Invalid amount");
            return false;
        }

        return true;
    }

    private void validateWalletIdAndProceed() {
        String walletIdStr = walletId.getText().toString().trim();

        CollectionReference walletHoldersRef = db.collection("itu_challenge_wallet_holders");
        Query query = walletHoldersRef.whereEqualTo("walletId", walletIdStr).whereEqualTo("status", "active");

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                setIcon(walletId, R.drawable.ic_success);
                String name = document.getString("name");
                recipientName.setText(name);
                recipientName.setEnabled(false);
                setIcon(recipientName, R.drawable.ic_success);
            } else {
                walletId.setError("Wallet ID not found");
                recipientName.setText("");
                setIcon(walletId, R.drawable.ic_failure);
                setIcon(recipientName, R.drawable.ic_failure);
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

    public void checkSenderBalanceAndTransfer(String receiverWalletId, double amount) {
        db.collection("itu_challenge_wallet_balances")
                .document(walletBalanceDocRef)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Double currentBalance = task.getResult().getDouble("balance");
                        String senderWalletId = task.getResult().getString("walletId");

                        Log.d(TAG, "------ receiverWalletId: " + receiverWalletId);
                        Log.d(TAG, "------ currentBalance: " + currentBalance);
                        Log.d(TAG, "------- senderWalletId: " + senderWalletId);

                        if (senderWalletId == null) {
                            Log.e(TAG, "Sender wallet ID is null. Please check the Firestore document structure.");
                            showErrorDialog("Sender wallet ID not found. Please check your account.");
                            return;
                        }

                        if (senderWalletId.equals(receiverWalletId)) {
                            showErrorDialog("You cannot transfer money to yourself");
                            walletId.setText("");
                            recipientName.setText("");
                            return;
                        }

                        if (currentBalance != null && currentBalance >= amount) {
                            getReceiverDocumentReference(receiverWalletId, (receiverRef, e) -> {
                                if (e != null) {
                                    showErrorDialog(e.getMessage());
                                } else {
                                    updateBalances(receiverRef, amount, currentBalance);
                                }
                            });
                        } else {
                            showErrorDialog("Insufficient balance");
                        }
                    } else {
                        showErrorDialog("Failed to retrieve balance");
                    }
                });
    }

    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void getReceiverDocumentReference(String receiverWalletId, ReceiverReferenceCallback callback) {
        CollectionReference walletBalancesRef = db.collection("itu_challenge_wallet_balances");
        Query query = walletBalancesRef.whereEqualTo("walletId", receiverWalletId);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                DocumentSnapshot receiverSnapshot = task.getResult().getDocuments().get(0);
                callback.onCallback(receiverSnapshot.getReference(), null);
            } else {
                callback.onCallback(null, new FirebaseFirestoreException("Receiver wallet ID not found.", FirebaseFirestoreException.Code.NOT_FOUND));
            }
        });
    }

    private void updateBalances(DocumentReference receiverRef, double amount, double senderCurrentBalance) {
        transferAmountProgressBar.setVisibility(View.VISIBLE);
        AtomicReference<String> receiverUserRefId = new AtomicReference<>("");

        db.runTransaction(transaction -> {
            // Get sender's balance
            DocumentReference senderRef = db.collection("itu_challenge_wallet_balances").document(walletBalanceDocRef);
            transaction.get(senderRef);

            double senderNewBalance = senderCurrentBalance - amount;

            // Get receiver's balance
            DocumentSnapshot receiverSnapshot = transaction.get(receiverRef);
            double receiverNewBalance = Objects.requireNonNullElse(receiverSnapshot.getDouble("balance"), 0.00) + amount;
            receiverUserRefId.set(receiverSnapshot.getString("user"));
            // Perform updates
            transaction.update(senderRef, "balance", senderNewBalance, "modified", getCurrentDatetime());
            transaction.update(receiverRef, "balance", receiverNewBalance, "modified", getCurrentDatetime());

            return null;
        }).addOnSuccessListener(aVoid -> {
            transferAmountProgressBar.setVisibility(View.INVISIBLE);

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "NG"));
            String formattedAmountSent = currencyFormat.format(amount);
            // String formattedBalance = currencyFormat.format(balance);
            showTransactionDialog("Transaction Successful", "Transfer of " + formattedAmountSent + " sent to " + recipientName.getText().toString());
            recordTransaction(amount, receiverUserRefId.get());
        }).addOnFailureListener(e -> {
            transferAmountProgressBar.setVisibility(View.INVISIBLE);
            showTransactionDialog("Transaction Failed", e.getMessage());
        });
    }

    private void recordTransaction(double amount, String receiverUserRefId) {

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("title", "Money Transfer");
        transaction.put("amount", amount);
        transaction.put("datetime", getCurrentDatetime());
        transaction.put("user", holderRefId);
        transaction.put("receiver", receiverUserRefId);
        transaction.put("status", "unread");

        db.collection("itu_challenge_wallet_transactions")
                .add(transaction)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Transaction recorded successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to record transaction", e));
    }

    private String getCurrentDatetime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private void showTransactionDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            if (title.equals("Transaction Successful")) {
                startActivity(new Intent(moneyTransfer.this, Dashboard.class));
                finish();
            }
        });
        builder.show();
    }

    private interface ReceiverReferenceCallback {
        void onCallback(DocumentReference receiverRef, Exception e);
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