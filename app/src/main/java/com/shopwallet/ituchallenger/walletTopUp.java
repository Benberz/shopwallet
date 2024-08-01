package com.shopwallet.ituchallenger;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shopwallet.ituchallenger.util.SecureStorageUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class walletTopUp extends AppCompatActivity implements BankLinkFragment.BankLinkCallback {

    private static final String TAG = "walletTopUpClass";
    private EditText amountInput;
    private Button submitReloadButton;
    private HolderBankAccount holderBankAccount;

    HashMap<String, Object> inputData;
    String docRef;
    private String walletBalanceDocRef;
    private String linkedBankDocRef;

    private FirebaseFirestore db;
    private String holderRefId;

    private ProgressBar topUpProgressBar;
    private TextView topUpProcessingTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_top_up);

        // Set up the toolbar for wallet Top Up
        Toolbar toolbar = findViewById(R.id.walletTopUpToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.wallet_topup);
        }

        amountInput = findViewById(R.id.creditAmount);
        submitReloadButton = findViewById(R.id.submitMobileReloadButton);
        topUpProgressBar = findViewById(R.id.topUpProgressBar);
        topUpProcessingTextView = findViewById(R.id.topUpProcessingTextView);

        topUpProgressBar.setVisibility(View.GONE);
        topUpProcessingTextView.setVisibility(View.GONE);

        inputData = SecureStorageUtil.retrieveDataFromKeystore(walletTopUp.this, "inputData");
        linkedBankDocRef = (String) inputData.get("linkedBankRef");

        db = FirebaseFirestore.getInstance();
        holderRefId = (String) inputData.get("holderRefId");
        walletBalanceDocRef = (String) inputData.get("balanceRefId");

        // Debug: Print inputData to log
        for (Map.Entry<String, Object> entry : inputData.entrySet()) {
            Log.d(TAG, "Key: " + entry.getKey() + ", Value: " + entry.getValue() + ", Value Type: " + entry.getValue().getClass().getName());
        }

        if (inputData.get("registeredBank") == null || Objects.requireNonNull(inputData.get("registeredBank")).toString().isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Link your Bank Account to your wallet first.", Snackbar.LENGTH_SHORT).show();
            showBankLinkDialog();
            // Disable amount and submit button
            amountInput.setEnabled(false);
            submitReloadButton.setEnabled(false);
        }

        holderBankAccount = new HolderBankAccount();

        submitReloadButton.setOnClickListener(view -> {
            String amountStr = amountInput.getText().toString().trim();
            if (validate(amountStr)) {
                double amount = Double.parseDouble(amountStr);
                // show processing text and progress bar
                topUpProgressBar.setVisibility(View.VISIBLE);
                topUpProcessingTextView.setVisibility(View.VISIBLE);
                fetchBalanceAndTopUp(linkedBankDocRef, walletBalanceDocRef, amount);
            }

            // temporary save
            SecureStorageUtil.saveDataToKeystore(getApplicationContext(), "inputData", inputData);
        });
    }

    private boolean validate(String amountStr) {
        if (amountStr.isEmpty()) {
            amountInput.setError("Amount is required");
            return false;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                amountInput.setError("Amount must be greater than zero");
                return false;
            }
        } catch (NumberFormatException e) {
            amountInput.setError("Invalid amount");
            return false;
        }
        return true;
    }

    private void fetchBalanceAndTopUp(String linkedBankRef, String walletRef, double amount) {
        holderBankAccount.getBalance(linkedBankRef, new HolderBankAccount.BalanceCallback() {
            @Override
            public void onSuccess(double balance) {
                if (balance >= amount) {
                    topUpWallet(walletRef, amount);
                } else {
                    topUpProgressBar.setVisibility(View.INVISIBLE);
                    topUpProcessingTextView.setVisibility(View.INVISIBLE);
                    showTransactionDialog("Transaction Failed", "Insufficient balance.");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                topUpProgressBar.setVisibility(View.INVISIBLE);
                topUpProcessingTextView.setVisibility(View.INVISIBLE);
                showTransactionDialog("Transaction Failed", errorMessage);
            }
        });
    }

    private void topUpWallet(String walletRef, double amount) {
        holderBankAccount.topUpWallet(walletRef, amount, new HolderBankAccount.TopUpCallback() {
            @Override
            public void onSuccess() {
                topUpProgressBar.setVisibility(View.GONE);
                topUpProcessingTextView.setVisibility(View.GONE);
                showTransactionDialog("Transaction Successful", "Top-up successful.");
                recordTransaction(amount);
            }

            @Override
            public void onFailure(String errorMessage) {
                topUpProgressBar.setVisibility(View.GONE);
                topUpProcessingTextView.setVisibility(View.GONE);
                showTransactionDialog("Transaction Failed", errorMessage);
            }
        });
    }

    private void recordTransaction(double amount) {

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("title", "Wallet Top Up");
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

    private void showTransactionDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            if (title.equals("Transaction Successful")) {
                startActivity(new Intent(walletTopUp.this, Dashboard.class));
                finish();
            }
        });
        builder.show();
    }

    private void showBankLinkDialog() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Retrieve holderRefId from inputData
        // String holderRefId = (String) inputData.get("holderRefId");

        BankLinkFragment bankLinkFragment = BankLinkFragment.newInstance(holderRefId);
        bankLinkFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogLinkBankTheme);

        // Set callback for bank link success
        bankLinkFragment.setBankLinkCallback(this);

        bankLinkFragment.show(fragmentManager, "BankLinkFragment");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button click
            startActivity(new Intent(this, Dashboard.class)); // to navigate back to Dashboard
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBankLinked(DocumentReference documentReference) {
        // This method is called when bank linking is successful in BankLinkFragment
        inputData.put("registeredBank", "yes");
        inputData.put("linkedBankRef", documentReference.getId());
        SecureStorageUtil.saveDataToKeystore(getApplicationContext(), "inputData", inputData);

        docRef = (String) inputData.get("linkedBankRef");
        linkedBankDocRef = docRef;
        // Enable amount input and submit button
        amountInput.setEnabled(true);
        submitReloadButton.setEnabled(true);

        // Debug: Print inputData to log
        for (Map.Entry<String, Object> entry : inputData.entrySet()) {
            Log.d(TAG, "Key: " + entry.getKey() + ", Value: " + entry.getValue() + ", Value Type: " + entry.getValue().getClass().getName());
        }

        // Optionally, update UI or perform any actions needed upon bank registration
        Snackbar.make(findViewById(android.R.id.content), "Bank account linked successfully.", Snackbar.LENGTH_SHORT).show();
    }
}
