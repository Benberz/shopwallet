package com.shopwallet.ituchallenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.fnsv.bsa.sdk.BsaSdk;
import com.fnsv.bsa.sdk.callback.SdkAuthResponseCallback;
import com.fnsv.bsa.sdk.callback.SdkResponseCallback;
import com.fnsv.bsa.sdk.response.AuthBiometricResponse;
import com.fnsv.bsa.sdk.response.AuthResultResponse;
import com.fnsv.bsa.sdk.response.DeleteUserResponse;
import com.fnsv.bsa.sdk.response.ErrorResult;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.shopwallet.ituchallenger.util.SecureStorageUtil;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Executor;

public class Profile extends AppCompatActivity {

    private static final String TAG = "ProfileClass";
    HashMap<String, Object> storedInputData;

    /**
     * Called when the activity is first created. This method sets up the user interface,
     * initializes the toolbar, retrieves and displays the user's profile information,
     * and configures the card click listeners for navigating to different sections.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the most recent data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        storedInputData = SecureStorageUtil.retrieveDataFromKeystore(this, "inputData");

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.profileToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.profile_text);
        }

        // Set up user ID and full name
        TextView fullNameTextView = findViewById(R.id.username);
        TextView userIdTextView = findViewById(R.id.userId);

        fullNameTextView.setText(Objects.requireNonNull(storedInputData.get("name")).toString());
        userIdTextView.setText(Objects.requireNonNull(storedInputData.get("userKey")).toString());

        // Menu options functions
        findViewById(R.id.personalInfoCard).setOnClickListener(v -> {
            // Handle Personal Info click
            performAuthenticationAndNavigate(() -> startActivity(new Intent(Profile.this, PersonalInfo.class)));
        });

        findViewById(R.id.authenticationTypeCard).setOnClickListener(v -> {
            // Handle Authentication Type click
            startActivity(new Intent(Profile.this, AuthenticationType.class));
        });

        findViewById(R.id.authenticationHistoryCard).setOnClickListener(v -> {
            // Handle Authentication History click
            performAuthenticationAndNavigate(() -> startActivity(new Intent(Profile.this, AuthenticationHistory.class)));
        });

        findViewById(R.id.deleteAccountCard).setOnClickListener(v -> {
            // Handle Delete Account click
            performAuthenticationAndNavigate(this::showDeleteAccountDialog);
        });
    }

    /**
     * Handles the selection of options in the options menu.
     * Specifically handles the action when the back button is pressed in the toolbar.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to proceed,
     * true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button click
            startActivity(new Intent(this, Dashboard.class)); // to navigate back to MainActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Performs authentication based on the stored authentication type.
     * Depending on the authType, it may initiate biometric authentication or PIN/pattern authentication.
     * On successful authentication, it runs the provided success action.
     *
     * @param onSuccess The action to perform on successful authentication.
     */
    private void performAuthenticationAndNavigate(Runnable onSuccess) {
        String authType = (String) storedInputData.get("authType");
        String userKey = (String) storedInputData.get("userKey");

        assert authType != null;
        if (authType.equals("3")) {
            // Biometric authentication
            BiometricManager biometricManager = BiometricManager.from(this);
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                Executor executor = ContextCompat.getMainExecutor(this);
                BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Log.e(TAG, "Biometric authentication error: " + errString);
                        Snackbar.make(findViewById(android.R.id.content), "Authentication error: " + errString, Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        callInAppAuthenticator(userKey, onSuccess);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Log.e(TAG, "Biometric authentication failed");
                        Snackbar.make(findViewById(android.R.id.content), "Authentication failed", Snackbar.LENGTH_SHORT).show();
                    }
                });

                BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Biometric Authentication")
                        .setSubtitle("Authenticate to proceed")
                        .setNegativeButtonText("Cancel")
                        .build();

                biometricPrompt.authenticate(promptInfo);
            } else {
                Log.e(TAG, "Biometric authentication not available");
                Snackbar.make(findViewById(android.R.id.content), "Biometric authentication not available", Snackbar.LENGTH_SHORT).show();
            }
        } else if (authType.equals("4")) {
            // PIN/pattern authentication
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager.isDeviceSecure()) {
                BsaSdk.getInstance().getSdkService().authDeviceCredential(this, new SdkResponseCallback<>() {
                    @Override
                    public void onSuccess(AuthBiometricResponse authBiometricResponse) {
                        Log.d(TAG, "PIN/pattern authentication successful: code: " + authBiometricResponse.getRtCode());
                        callInAppAuthenticator(userKey, onSuccess);
                    }

                    @Override
                    public void onFailed(ErrorResult errorResult) {
                        Log.e(TAG, "PIN/pattern authentication failed: " + errorResult.getErrorMessage());
                        Snackbar.make(findViewById(android.R.id.content), "Authentication failed: " + errorResult.getErrorMessage(), Snackbar.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "Device is not secure");
                Snackbar.make(findViewById(android.R.id.content), "Device is not secure", Snackbar.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Unknown authentication type");
            Snackbar.make(findViewById(android.R.id.content), "Unknown authentication type", Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Calls the in-app authenticator using BSA SDK to perform an additional layer of authentication.
     * If successful, it runs the provided success action.
     *
     * @param userKey   The user's unique key for authentication.
     * @param onSuccess The action to perform on successful authentication.
     */
    private void callInAppAuthenticator(String userKey, Runnable onSuccess) {

        // Show a dialog while processing
        runOnUiThread(() -> {
            AlertDialog progressDialog = new AlertDialog.Builder(this)
                    .setView(LayoutInflater.from(this).inflate(R.layout.dialog_progress, findViewById(android.R.id.content), false))
                    .setCancelable(false)
                    .create();
            progressDialog.show();


            BsaSdk.getInstance().getSdkService().appAuthenticator(userKey, true, this, new SdkAuthResponseCallback<>() {
                @Override
                public void onSuccess(AuthResultResponse authResultResponse) {
                    Log.d(TAG, "In-app authentication successful: code: " + authResultResponse.getRtCode());
                    progressDialog.dismiss(); // Dismiss the dialog when authentication fails
                    onSuccess.run();
                }

                @Override
                public void onProcess(boolean b, String s) {
                }

                @Override
                public void onFailed(ErrorResult errorResult) {
                    Log.e(TAG, "In-app authentication failed: " + errorResult.getErrorMessage());
                    progressDialog.dismiss(); // Dismiss the dialog when authentication fails
                    Snackbar.make(findViewById(android.R.id.content), "Authentication failed: " + errorResult.getErrorMessage(), Snackbar.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Displays a dialog for account deletion confirmation.
     * If confirmed, it deletes the user's account using BSA SDK and navigates back to the sign-up screen.
     */
    private void showDeleteAccountDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_delete_account, findViewById(android.R.id.content), false);
        dialog.setContentView(view);

        Button buttonYes = dialog.findViewById(R.id.deleteAccountYesDialogButton);
        Button buttonCancel = dialog.findViewById(R.id.deleteAccountCancelDialogButton);

        assert buttonYes != null;
        buttonYes.setOnClickListener(v -> {
            dialog.dismiss();
            deleteUserAccount();
        });

        assert buttonCancel != null;
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * Deletes the user account using the BsaSdk service and handles the response.
     *
     * If the account deletion is successful, the method updates the wallet holder's status in Firestore,
     * clears user preferences from secure storage, and navigates the user back to the account creation page.
     * If the deletion fails, an error message is displayed.
     */
    private void deleteUserAccount() {
        // Call the deleteUser method from the BsaSdk service
        BsaSdk.getInstance().getSdkService().deleteUser(new SdkResponseCallback<>() {

            @Override
            public void onSuccess(DeleteUserResponse response) {
                // Check if the response is valid and if the deletion was successful
                if (response != null && response.getRtCode() == 0) {
                    Log.d(TAG, "Account deletion successful: " + response.rtMsg);

                    // Show a success message to the user
                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Account Deleted", Snackbar.LENGTH_LONG);
                    snackbar.setAnchorView(findViewById(R.id.dashboardBottomNavigation)); // Adjust if you have a BottomNavigationView
                    snackbar.show();

                    // Update the wallet holder status in Firestore
                    FirestoreHelper firestoreHelper = new FirestoreHelper();
                    String holderRefId = (String) storedInputData.get("holderRefId");
                    firestoreHelper.updateWalletHolderStatus(holderRefId);

                    // Clear all user preferences from secure storage
                    SecureStorageUtil.clearAllUserPreferences(Profile.this);

                    // Navigate the user back to the Create Account page
                    new Handler().postDelayed(() -> {
                        Intent intent = new Intent(Profile.this, CreateAccount.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish(); // Finish the current activity
                    }, Snackbar.LENGTH_LONG + 2000); // Delay in milliseconds
                } else {
                    // Handle case where deletion response is invalid or deletion failed
                    assert response != null;
                    Log.e(TAG, "Account deletion failed: " + response.rtMsg);
                    Snackbar.make(findViewById(android.R.id.content), "Account deletion failed: " + response.rtMsg, Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                // Handle case where the account deletion request failed
                Log.e(TAG, "Account deletion failed: " + errorResult.getErrorMessage());
                Snackbar.make(findViewById(android.R.id.content), "Account deletion failed: " + errorResult.getErrorMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}