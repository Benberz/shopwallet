package com.shopwallet.ituchallenger;

import android.os.Handler;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class HolderBankAccount {

    private static final String TAG = "HolderBankAccountService";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface BalanceCallback {
        void onSuccess(double balance);
        void onFailure(String errorMessage);
    }

    public interface TopUpCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface BankLinkCallback {
        void onSuccess(DocumentReference documentReference);
        void onFailure(String errorMessage);
    }

    // Mock method to simulate an external API call to get the balance
    private void fetchBalanceFromApi(BalanceCallback callback) {
        // Simulate network call to fetch balance from external service
        new Handler().postDelayed(() -> {
            // Simulate a response from the API
            double fetchedBalance = 5000.0; // Replace this with actual API call logic
            callback.onSuccess(fetchedBalance);
        }, 1000); // Simulated network delay
    }

    // Method to get balance from Firestore and update the balance and modified date
    public void getBalance(String linkedReferenceId, BalanceCallback callback) {
        fetchBalanceFromApi(new BalanceCallback() {
            @Override
            public void onSuccess(double fetchedBalance) {
                // Update Firestore with fetched balance and modified date
                String currentDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                Log.e(TAG, "linkedReferenceId");
                DocumentReference docRef = db.collection("itu_challenge_linked_banks").document(linkedReferenceId);
                docRef.update("balance", fetchedBalance, "modified", currentDatetime)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Fetched Balance from Bank Updated: " + linkedReferenceId);
                            callback.onSuccess(fetchedBalance);
                        })
                        .addOnFailureListener(e -> {
                            Log.d(TAG, "***Failed to update balance: " + e.getMessage());
                            callback.onFailure("Failed to update balance: " + e.getMessage());
                        });
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure("Failed to fetch balance from API: " + errorMessage);
            }
        });
    }

    public void topUpWallet(String walletBalanceReferenceId, double amount, TopUpCallback callback) {
        DocumentReference docRef = db.collection("itu_challenge_wallet_balances").document(walletBalanceReferenceId);
        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(docRef);
                    double currentBalance;
                    currentBalance = Objects.requireNonNullElse(snapshot.getDouble("balance"), 0.0);
                    double newBalance = currentBalance + amount;
                    transaction.update(docRef, "balance", newBalance, "modified", new Date());
                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Failed to top up wallet: " + e.getMessage()));
    }

    public void linkBank(String userRef, String bankName, String accountNumber, BankLinkCallback callback) {
        // Create data object to store in Firestore
        Map<String, Object> bankData = new HashMap<>();
        bankData.put("Bank", bankName);
        bankData.put("account", accountNumber);
        bankData.put("balance", 0.0); // Initial balance
        bankData.put("user", userRef); // Replace with actual user reference or ID

        String currentDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        bankData.put("created", currentDatetime);
        bankData.put("modified", "");

        // Store data in Firestore
        db.collection("itu_challenge_linked_banks")
                .add(bankData)
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onFailure("Failed to link bank: " + e.getMessage()));
    }
}