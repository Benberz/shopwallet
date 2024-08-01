package com.shopwallet.ituchallenger;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FirestoreHelper {

    private final FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public void updateWalletHolderStatus(String holderRefId) {
        // Create a reference to the 'itu_challenge_wallet_holders' collection
        DocumentReference holderRef = db.collection("itu_challenge_wallet_holders").document(holderRefId);

        // Create the data to update
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "inactive");
        updates.put("modified", getCurrentDatetime());

        // Update the document
        holderRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Successfully updated
                    System.out.println("DocumentSnapshot successfully updated!");
                })
                .addOnFailureListener(e -> {
                    // Failed to update
                    System.err.println("Error updating document: " + e);
                });

    }

    private String getCurrentDatetime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}