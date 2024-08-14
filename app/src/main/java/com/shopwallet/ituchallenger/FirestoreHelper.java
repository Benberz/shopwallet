package com.shopwallet.ituchallenger;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * FirestoreHelper provides utility methods to interact with Firestore database.
 * It includes methods for updating document fields and retrieving the current date and time.
 */
public class FirestoreHelper {

    private final FirebaseFirestore db;

    /**
     * Initializes FirestoreHelper and sets up the Firestore database instance.
     */
    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Updates the status of a wallet holder in the Firestore database.
     * The document is set to "inactive" status and the modification time is updated.
     *
     * @param holderRefId The ID of the wallet holder document to update.
     */
    public void updateWalletHolderStatus(String holderRefId) {
        // Create a reference to the 'itu_challenge_wallet_holders' collection
        DocumentReference holderRef = db.collection("itu_challenge_wallet_holders").document(holderRefId);

        // Create the data to update
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "inactive"); // Set the status to inactive
        updates.put("modified", getCurrentDatetime()); // Set the modified timestamp

        // Update the document
        holderRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Log success message when the document is successfully updated
                    System.out.println("DocumentSnapshot successfully updated!");
                })
                .addOnFailureListener(e -> {
                    // Log error message if the update fails
                    System.err.println("Error updating document: " + e);
                });
    }

    /**
     * Retrieves the current date and time in the format "yyyy-MM-dd HH:mm:ss".
     *
     * @return A string representing the current date and time.
     */
    private String getCurrentDatetime() {
        // Format the current date and time
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}