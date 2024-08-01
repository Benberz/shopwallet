package com.shopwallet.ituchallenger;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.shopwallet.ituchallenger.util.SecureStorageUtil;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class moneyReceive extends AppCompatActivity {

    private static final String TAG = "moneyReceiveClass";
    private FirebaseFirestore db;
    private String holderRefId;
    private String balanceRefId;
    private ListenerRegistration balanceListener;
    private ListenerRegistration transactionListener;
    private HashMap<String, Object> inputData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_money_receive);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        inputData = SecureStorageUtil.retrieveDataFromKeystore(this, "inputData");

        balanceRefId = (String) inputData.get("balanceRefId");
        holderRefId = (String) inputData.get("holderRefId");

        // Get data from intent
        Intent intent = getIntent();
        String walletIdStr = intent.getStringExtra("walletId");
        String walletIdStrQRData = "{ \"walletId\": \"" + inputData.get("walletId") + "\" }";

        // Set up the toolbar for money transfer
        Toolbar toolbar = findViewById(R.id.moneyTransferToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Receive Payment");
        }

        TextView walletIdNumberTextView = findViewById(R.id.walletIDNumber);
        ImageView walletIdNumberQRTextView = findViewById(R.id.qrCodeImageView);
        ProgressBar moneyReceiveProgressbar = findViewById(R.id.moneyReceiveProgressBar);
        TextView moneyReceiveStatusTextView = findViewById(R.id.paymentStatusTextView);

        Button submitRequestPaymentButton = findViewById(R.id.requestPaymentButton);

        moneyReceiveProgressbar.setVisibility(View.INVISIBLE);
        moneyReceiveStatusTextView.setVisibility(View.INVISIBLE);

        // Set walletId to TextView and generate QR code
        walletIdNumberTextView.setText(walletIdStr);
        generateQRCode(walletIdStrQRData, walletIdNumberQRTextView);

        // Listen for changes to balance and transactions
        listenToBalanceAndTransactions();

        submitRequestPaymentButton.setOnClickListener(v -> {
            // Handle request payment button click
            // This is where you would handle the logic for requesting payment
            showRequestPaymentDialog();
        });
    }

    private void showRequestPaymentDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.dialog_request_payment, findViewById(android.R.id.content), false);

        bottomSheetDialog.setContentView(bottomSheetView);

        EditText walletIdEditText = bottomSheetView.findViewById(R.id.walletIdEditText);
        EditText requestedAmountEditText = bottomSheetView.findViewById(R.id.requestedAmountEditText);
        TextView walletHolder = bottomSheetDialog.findViewById(R.id.walletHolderTextView);
        ImageView qrCodeImageView = bottomSheetView.findViewById(R.id.qrCodeImageView);
        Button generateQRCodeButton = bottomSheetView.findViewById(R.id.generateQRCodeButton);
        Button clearButton = bottomSheetView.findViewById(R.id.cancelQRCodeButton);

        qrCodeImageView.setVisibility(View.GONE);

        walletIdEditText.setText(Objects.requireNonNull(inputData.get("walletId")).toString());

        // Add TextWatcher to walletId EditText
        walletIdEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                // No action needed before text changes
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (charSequence.length() < 10) {
                    // Clear failure icon if text length is less than 10
                    Log.d(TAG, "onTextChanged: charSequence.length() < 10");
                    assert walletHolder != null;
                    walletHolder.setText(R.string.wallet_holder);
                } else if (charSequence.length() == 10) {
                    Log.d(TAG, "onTextChanged: charSequence.length() == 10");
                    //walletIdEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_123_24, null, null, null);
                    validateWalletIdAndProceed(walletIdEditText.getText().toString().trim(), walletIdEditText, walletHolder);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() == 10) {
                    Log.d(TAG, "afterTextChanged: editable.length() == 10");
                    validateWalletIdAndProceed(walletIdEditText.getText().toString().trim(), walletIdEditText, walletHolder);
                }
            }
        });

        // Add OnFocusChangeListener to walletId EditText
        walletIdEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateWalletIdAndProceed(walletIdEditText.getText().toString().trim(), walletIdEditText, walletHolder);
            }
        });

        generateQRCodeButton.setOnClickListener(v -> {
            String walletId = walletIdEditText.getText().toString().trim();
            String requestedAmount = requestedAmountEditText.getText().toString().trim();

            if (walletId.isEmpty() || requestedAmount.isEmpty()) {
                Toast.makeText(this, "Please enter Wallet ID and Requested Amount", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double amount = Double.parseDouble(requestedAmount);
                if (amount <= 0) {
                    Toast.makeText(this, "Requested Amount should be greater than zero", Toast.LENGTH_SHORT).show();
                    return;
                }
                String qrData = "{ \"walletId\": \"" + walletId + "\", \"requestedAmount\": \"" + requestedAmount + "\" }";
                generateQRCode(qrData, qrCodeImageView);
                qrCodeImageView.setVisibility(View.VISIBLE);
                walletIdEditText.setEnabled(false);
                requestedAmountEditText.setEnabled(false);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid Amount", Toast.LENGTH_SHORT).show();
            }
        });

        clearButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();

    }

    private void validateWalletIdAndProceed(String walletIdStr, EditText walletId, TextView holder) {

        CollectionReference walletHoldersRef = db.collection("itu_challenge_wallet_holders");
        Query query = walletHoldersRef.whereEqualTo("walletId", walletIdStr);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                setIcon(walletId, R.drawable.ic_success);
                String name = document.getString("name");
                holder.setText(name);
            } else {
                setIcon(walletId, R.drawable.ic_failure);
                walletId.setError("Wallet ID not found");
                holder.setText("---");
            }
        });
    }

    private void generateQRCode(String walletId, ImageView qrCodeImageView) {
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        try {
            Bitmap bitmap = barcodeEncoder.encodeBitmap(walletId, BarcodeFormat.QR_CODE, 400, 400);
            qrCodeImageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    // Method to set icons
    private void setIcon(EditText editText, int resId) {
        if (resId != 0) {
            Drawable icon = ContextCompat.getDrawable(this, resId);
            if (icon != null) {
                icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
            }
            editText.setCompoundDrawables(null, null, icon, null);
        } else {
            editText.setCompoundDrawables(null, null, null, null);
        }
    }

    private void listenToBalanceAndTransactions() {
        Query transactionQuery = db.collection("itu_challenge_wallet_transactions")
                .whereEqualTo("receiver", holderRefId)
                .whereEqualTo("status", "unread")
                .orderBy("datetime", Query.Direction.DESCENDING)
                .limit(1);

        transactionListener = transactionQuery.addSnapshotListener((transactionSnapshots, e) -> {
            if (e != null) {
                runOnUiThread(()->showErrorDialog("Listen failed: " + e.getMessage()));
                return;
            }

            if (transactionSnapshots != null && !transactionSnapshots.isEmpty()) {
                DocumentSnapshot latestTransaction = transactionSnapshots.getDocuments().get(0);
                Double amountReceived = latestTransaction.getDouble("amount");
                String user = latestTransaction.getString("user");

                if (amountReceived != null && user != null) {
                    // Query the name of the user
                    queryUserNameAndProceed(amountReceived, user);

                    // Update the status to "read"
                    latestTransaction.getReference().update("status", "read")
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Transaction status updated to read"))
                            .addOnFailureListener(error -> Log.e(TAG, "Error updating transaction status: ", error));
                }
            }
        });
    }

    private void queryUserNameAndProceed(Double amountReceived, String userId) {
        DocumentReference userDocRef = db.collection("itu_challenge_wallet_holders").document(userId);

        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot userSnapshot = task.getResult();
                if (userSnapshot != null && userSnapshot.exists()) {
                    String userName = userSnapshot.getString("name");
                    String senderWallet = userSnapshot.getString("walletId");
                    if (userName != null) {
                        queryBalanceAndUpdateUI(amountReceived, userName, senderWallet);
                    }
                } else {
                    runOnUiThread(()-> showErrorDialog("No user data found."));
                }
            } else {
                runOnUiThread(()-> showErrorDialog("Failed to retrieve user: " + Objects.requireNonNull(task.getException()).getMessage()));
            }
        });
    }

    private void queryBalanceAndUpdateUI(Double amountReceived, String userName, String SenderWallet) {
        DocumentReference balanceDocRef = db.collection("itu_challenge_wallet_balances").document(balanceRefId);

        balanceListener = balanceDocRef.addSnapshotListener((balanceSnapshot, e) -> {
            if (e != null) {
                runOnUiThread(()-> showErrorDialog("Listen failed: " + e.getMessage()));
                return;
            }

            if (balanceSnapshot != null && balanceSnapshot.exists()) {
                Double balance = balanceSnapshot.getDouble("balance");
                String dateReceived = balanceSnapshot.getString("modified");
                if (balance != null) {
                    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "NG"));
                    String formattedAmountReceived = currencyFormat.format(amountReceived);
                    String formattedBalance = currencyFormat.format(balance);

                    runOnUiThread(() -> showTransactionDialog("Received amount: " + formattedAmountReceived + "\nFrom: " + userName + "\nWallet:  "
                            + SenderWallet + "\nNew balance: " + formattedBalance + " \nDate Received: " + dateReceived));
                }
            } else {
                runOnUiThread(()->showErrorDialog("No balance data found."));
            }
        });
    }

    private void showTransactionDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Transaction Successful");
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(this, Dashboard.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (balanceListener != null) {
            balanceListener.remove();
        }
        if (transactionListener != null) {
            transactionListener.remove();
        }
    }
}