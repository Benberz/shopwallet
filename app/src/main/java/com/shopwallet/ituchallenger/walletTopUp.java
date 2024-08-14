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

/**
 * Activity for handling wallet top-up operations.
 * This activity allows users to top up their wallet by entering an amount,
 * verifying the balance, and processing the top-up transaction.
 * It also handles bank account linking and displays relevant dialogs.
 */
public class walletTopUp extends AppCompatActivity implements BankLinkFragment.BankLinkCallback {

    private static final String TAG = "walletTopUpClass";
    private EditText amountInput;
    private Button submitReloadButton;
    private HolderBankAccount holderBankAccount;

    private HashMap<String, Object> inputData;
    private String docRef;
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

        // Initialize views
        amountInput = findViewById(R.id.creditAmount);
        submitReloadButton = findViewById(R.id.submitMobileReloadButton);
        topUpProgressBar = findViewById(R.id.topUpProgressBar);
        topUpProcessingTextView = findViewById(R.id.topUpProcessingTextView);

        // Initially hide progress bar and processing text
        topUpProgressBar.setVisibility(View.GONE);
        topUpProcessingTextView.setVisibility(View.GONE);

        // Retrieve input data from secure storage
        inputData = SecureStorageUtil.retrieveDataFromKeystore(walletTopUp.this, "inputData");
        linkedBankDocRef = (String) inputData.get("linkedBankRef");

        // Initialize Firestore instance and retrieve references
        db = FirebaseFirestore.getInstance();
        holderRefId = (String) inputData.get("holderRefId");
        walletBalanceDocRef = (String) inputData.get("balanceRefId");

        // Debug: Print inputData to log
        for (Map.Entry<String, Object> entry : inputData.entrySet()) {
            Log.d(TAG, "Key: " + entry.getKey() + ", Value: " + entry.getValue() + ", Value Type: " + entry.getValue().getClass().getName());
        }

        // Check if bank account is registered
        if (inputData.get("registeredBank") == null || Objects.requireNonNull(inputData.get("registeredBank")).toString().isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Link your Bank Account to your wallet first.", Snackbar.LENGTH_SHORT).show();
            showBankLinkDialog();
            // Disable amount input and submit button if bank account is not linked
            amountInput.setEnabled(false);
            submitReloadButton.setEnabled(false);
        }

        holderBankAccount = new HolderBankAccount();

        // Set up submit button click listener
        submitReloadButton.setOnClickListener(view -> {
            String amountStr = amountInput.getText().toString().trim();
            if (validate(amountStr)) {
                double amount = Double.parseDouble(amountStr);
                // Show processing text and progress bar
                topUpProgressBar.setVisibility(View.VISIBLE);
                topUpProcessingTextView.setVisibility(View.VISIBLE);
                fetchBalanceAndTopUp(linkedBankDocRef, walletBalanceDocRef, amount);
            }

            // Temporarily save data
            SecureStorageUtil.saveDataToKeystore(getApplicationContext(), "inputData", inputData);
        });
    }

    /**
     * Validates the entered amount.
     * Checks if the amount is empty, non-numeric, or less than or equal to zero.
     *
     * @param amountStr The amount entered by the user.
     * @return True if the amount is valid; false otherwise.
     */
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

    /**
     * Fetches the balance from the bank account and processes the top-up.
     * If the balance is sufficient, the wallet is topped up; otherwise, an error message is shown.
     *
     * @param linkedBankRef The reference to the linked bank account.
     * @param walletRef The reference to the wallet to top up.
     * @param amount The amount to top up.
     */
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

    /**
     * Tops up the wallet with the specified amount.
     * On success, the transaction is recorded and a success message is shown.
     * On failure, an error message is displayed.
     *
     * @param walletRef The reference to the wallet to top up.
     * @param amount The amount to top up.
     */
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

    /**
     * Records the top-up transaction in Firestore.
     * The transaction includes the title, amount, datetime, and user reference.
     *
     * @param amount The amount topped up.
     */
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

    /**
     * Gets the current datetime in the format "yyyy-MM-dd HH:mm:ss".
     *
     * @return The current datetime as a string.
     */
    private String getCurrentDatetime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    /**
     * Shows a dialog with the specified title and message.
     * If the title indicates a successful transaction, navigates back to the Dashboard.
     *
     * @param title The title of the dialog.
     * @param message The message to display in the dialog.
     */
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

    /**
     * Shows a dialog to link a bank account.
     * If the bank account is successfully linked, enables the amount input and submit button.
     */
    private void showBankLinkDialog() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        BankLinkFragment bankLinkFragment = BankLinkFragment.newInstance(holderRefId);
        bankLinkFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogLinkBankTheme);

        // Set callback for bank link success
        bankLinkFragment.setBankLinkCallback(this);

        bankLinkFragment.show(fragmentManager, "BankLinkFragment");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button click to navigate back to Dashboard
            startActivity(new Intent(this, Dashboard.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBankLinked(DocumentReference documentReference) {
        // Called when the bank is successfully linked in BankLinkFragment
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