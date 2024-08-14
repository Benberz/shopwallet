package com.shopwallet.ituchallenger;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fnsv.bsa.sdk.BsaSdk;
import com.fnsv.bsa.sdk.callback.SdkAuthResponseCallback;
import com.fnsv.bsa.sdk.callback.SdkResponseCallback;
import com.fnsv.bsa.sdk.common.SdkUtil;
import com.fnsv.bsa.sdk.response.AuthCancelResponse;
import com.fnsv.bsa.sdk.response.AuthCompleteResponse;
import com.fnsv.bsa.sdk.response.AuthExistResponse;
import com.fnsv.bsa.sdk.response.AuthResultResponse;
import com.fnsv.bsa.sdk.response.ErrorResult;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.shopwallet.ituchallenger.util.SecureStorageUtil;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Activity that represents the user's dashboard.
 * Manages user authentication, displays wallet balances, recent transactions,
 * and handles broadcast messages for authentication and UI updates.
 */
public class Dashboard extends AppCompatActivity {

    private static final String TAG = "DashboardClass";
    private HashMap<String, Object> inputData;
    private Runnable pendingAction;
    private ActivityResultLauncher<Intent> deviceCredentialLauncher;

    private FirebaseFirestore db;
    private DocumentReference balanceDocRef;
    private String holderRefId;
    private String userKey;

    private SwipeRefreshLayout swipeRefreshLayout;

    public static final String ACTION_AUTHENTICATE = "com.shopwallet.ituchallenger.ACTION_AUTHENTICATE";
    private boolean isReceiverRegistered = false;
    private long lastReceivedBroadcastTime = 0;
    private static final long BROADCAST_RECEIVE_THRESHOLD_MS = 1000; // 2 seconds
    private static String lastBroadcastId = "";
    // private String channelKey;

    public static final String ACTION_SHOW_SNACKBAR = "com.shopwallet.ituchallenger.ACTION_SHOW_SNACKBAR";
    public static final String EXTRA_SNACKBAR_MESSAGE = "com.shopwallet.ituchallenger.EXTRA_SNACKBAR_MESSAGE";

    /**
     * Handles broadcast intents related to authentication.
     * Triggers biometric or PIN pattern authentication based on the broadcast content.
     */
    private final BroadcastReceiver authReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_AUTHENTICATE.equals(intent.getAction())) {
                String broadcastId = intent.getStringExtra("broadcastId");
                Log.e(TAG, "broadcastId (Received): " + broadcastId);

                if (broadcastId != null && !broadcastId.equals(lastBroadcastId) &&
                        System.currentTimeMillis() - lastReceivedBroadcastTime > BROADCAST_RECEIVE_THRESHOLD_MS) {

                    lastBroadcastId = broadcastId;
                    lastReceivedBroadcastTime = System.currentTimeMillis();

                    Log.e(TAG, "lastBroadcastId (Received): " + lastBroadcastId);

                    Log.e(TAG, "***Broadcast Received from FirebasePushService");
                    String authType = intent.getStringExtra("authType");

                    if ("3".equals(authType)) {
                        performBiometricAuth();
                    } else if ("4".equals(authType)) {
                        performPinPatternAuth();
                    }
                } else {
                    Log.e(TAG, "Duplicate or too frequent broadcast received, ignoring.");
                    Log.e(TAG, "broadcastId (Received & Ignored)********: " + broadcastId);
                }
            }
        }
    };

    /**
     * Handles broadcast intents for displaying Snackbar messages.
     *
     */
    private final BroadcastReceiver snackbarReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && ACTION_SHOW_SNACKBAR.equals(intent.getAction())) {
                String message = intent.getStringExtra(EXTRA_SNACKBAR_MESSAGE);
                if (message != null && !message.isEmpty()) {
                    showSnackbar(message);
                }
            }
        }
    };

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate() fired!!!>>>>");
        setContentView(R.layout.activity_dashboard);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshDashboard);

        inputData = SecureStorageUtil.retrieveDataFromKeystore(this, "inputData");

        userKey = (String) inputData.get("userKey");

        String balanceRefId = (String) inputData.get("balanceRefId");
        holderRefId = (String) inputData.get("holderRefId");

        db = FirebaseFirestore.getInstance();
        assert balanceRefId != null;
        balanceDocRef = db.collection("itu_challenge_wallet_balances").document(balanceRefId);

        // Initialize buttons and set their onClickListeners
        setupButtons();

        // Initialize recent transactions list
        setupRecentTransactionsList();

        // Initialize bottom navigation
        setupBottomNavigation();

        // Initialize the device credential launcher
        deviceCredentialLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && pendingAction != null) {
                        pendingAction.run();
                        pendingAction = null;

                        // sendAuthResultToService(true); // Authentication success
                    } else {
                        Log.e(TAG, "Device credential authentication failed");
                        Snackbar.make(findViewById(android.R.id.content), "Authentication failed", Snackbar.LENGTH_SHORT).show();
                        // sendAuthResultToService(false); // Authentication failed
                    }
                });

        // Retrieve and save access token
        // retrieveAndSaveAccessToken();

        // Call method to get and display card holder info
        getCardHolderInfo();

        // Register the BroadcastReceiver
        if (!isReceiverRegistered) {
            IntentFilter authReceiverFilter = new IntentFilter(ACTION_AUTHENTICATE);
            registerReceiver(authReceiver, authReceiverFilter);
            isReceiverRegistered = true;
            Log.e(TAG, "BroadcastReceiver registered");
        }

        // Register the BroadcastReceiver
        IntentFilter snackBarFilter = new IntentFilter(ACTION_SHOW_SNACKBAR);
        registerReceiver(snackbarReceiver, snackBarFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the BroadcastReceiver
        if (isReceiverRegistered) {
            unregisterReceiver(authReceiver);
            isReceiverRegistered = false;
            Log.e(TAG, "BroadcastReceiver unregistered");
        }

        // Unregister the BroadcastReceiver
        unregisterReceiver(snackbarReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume() fired!!!");
    }

    private void showSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
    }

    /**
     * Performs biometric authentication.
     * Uses BiometricPrompt to handle biometric authentication.
     */
    private void performBiometricAuth() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Log.e(TAG, "Biometric authentication error: " + errString);
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    Log.d(TAG, "Biometric authentication succeeded");

                    String accessToken = SdkUtil.getAccessToken();
                    if (accessToken != null && !accessToken.isEmpty()) {
                        Log.e(TAG, "___ Access Token is set, calling callNormalAuthenticator | Access token: " + accessToken);
                        // callNormalAuthenticator(userKey, FirebasePushService::completeAuth);
                        checkExistingAuthRequest();
                    } else {
                        Log.e(TAG, "Access Token is not set, calling callInAppAuthenticator");

                        // callNormalAuthenticator(userKey, FirebasePushService::completeAuth);
                        checkExistingAuthRequest();

                    }
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Log.e(TAG, "Biometric authentication failed");
                }
            });

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric Authentication")
                    .setSubtitle("Authenticate using biometric credentials")
                    .setNegativeButtonText("Cancel")
                    .build();

            biometricPrompt.authenticate(promptInfo);
        } else {
            Log.e(TAG, "Biometric authentication not available or not set up");
        }
    }

    /**
     * Performs PIN or pattern authentication.
     * Starts an activity to handle PIN or pattern authentication.
     */
    private void performPinPatternAuth() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        if (keyguardManager.isKeyguardSecure()) {
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent("Authentication Required", "Please enter your PIN, pattern or password to authenticate");
            if (intent != null) {
                deviceCredentialLauncher2.launch(intent);
            }
        } else {
            Log.e(TAG, "PIN/pattern authentication not set up");
        }
    }

    private final ActivityResultLauncher<Intent> deviceCredentialLauncher2 = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Log.d(TAG, "PIN/pattern authentication succeeded");

                    Log.e(TAG, "userKey: " + userKey);
                    String accessToken = SdkUtil.getAccessToken();

                    Log.e(TAG, "___ Access Token is set, calling callNormalAuthenticator | Access token: " + accessToken);

                    //callNormalAuthenticator(userKey, FirebasePushService::completeAuth);
                    checkExistingAuthRequest();

                } else {
                    Log.e(TAG, "PIN/pattern authentication failed");
                }
            }
    );

    /**
     * Refreshes the dashboard when the swipe-to-refresh action is triggered.
     */
    private void refreshDashboard() {
        // Show the progress bar while loading data
        swipeRefreshLayout.setRefreshing(true);

        // Reload card holder info and recent transactions
        getCardHolderInfo();
        setupRecentTransactionsList();

        // Hide the progress bar once loading is complete
        swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * Retrieves and displays the card holder's information.
     * Fetches user details from Firestore and updates UI elements.
     */
    private void getCardHolderInfo() {
        TextView walletId = findViewById(R.id.cardNumber);
        TextView walletHolder = findViewById(R.id.cardHolderName);
        TextView creationDate = findViewById(R.id.creationDate);

        // Query Firestore for the specific user's wallet information
        db.collection("itu_challenge_wallet_holders")
                .document(holderRefId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String walletIdStr = document.getString("walletId");
                            String walletHolderStr = document.getString("name");
                            String walletCreationDateStr = document.getString("created");

                            // Update UI with retrieved wallet information
                            assert walletIdStr != null;
                            walletId.setText(formatWalletId(walletIdStr));
                            walletHolder.setText(walletHolderStr);
                            creationDate.setText(formatCreationDate(walletCreationDateStr));
                        } else {
                            Log.e(TAG, "No such document");
                            walletId.setText(R.string.no_wallet_id_found);
                            walletHolder.setText(R.string.no_wallet_holder_found);
                            creationDate.setText(R.string.no_creation_date);
                        }
                    } else {
                        Log.e(TAG, "Error getting documents: ", task.getException());
                        walletId.setText(R.string.error_fetching_wallet_id);
                        walletHolder.setText(R.string.error_fetching_wallet_holder);
                        creationDate.setText(R.string.error_getting_date);
                    }
                });
    }

    // Helper method to format walletId with spaces every 3 characters
    /**
     * Formats a wallet ID by inserting spaces every 3 characters.
     * This is useful for improving readability of wallet IDs.
     *
     * @param walletIdStr The original wallet ID string.
     * @return The formatted wallet ID string with spaces inserted.
     */
    private String formatWalletId(String walletIdStr) {
        if (walletIdStr != null && walletIdStr.length() >= 10) {
            walletIdStr = walletIdStr.substring(0, 3) + " " + walletIdStr.substring(3, 6) + " " + walletIdStr.substring(6);
        }
        return walletIdStr;
    }

    // Helper method to format creationDate to MM/YYYY with month spelled out
    /**
     * Formats a wallet creation date from "yyyy-MM-dd" to "MMMM yyyy".
     * This method converts the date to a more readable format with the full month name.
     *
     * @param walletCreationDateStr The original creation date string in "yyyy-MM-dd" format.
     * @return The formatted date string in "MMMM yyyy" format, or the original string if parsing fails.
     */
    private String formatCreationDate(String walletCreationDateStr) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()); // MMM for full month name
        try {
            Date date = inputFormat.parse(walletCreationDateStr);
            assert date != null;
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing creation date:", e);
            return walletCreationDateStr; // Return original string on parsing error
        }
    }

    /**
     * Sets up buttons with their onClickListeners.
     * Initializes various buttons used in the dashboard.
     */
    private void setupButtons() {
        ImageButton userProfileButton = findViewById(R.id.userProfileButton);
        ImageButton balanceButton = findViewById(R.id.balanceImageButton);
        FloatingActionButton walletTopUpButton = findViewById(R.id.walletTopUpButton);
        FloatingActionButton mobileReloadButton = findViewById(R.id.mobileReloadButton);
        FloatingActionButton moneyTransferButton = findViewById(R.id.moneyTransferButton);
        FloatingActionButton moneyReceiveButton = findViewById(R.id.moneyReceiveButton);

        userProfileButton.setOnClickListener(view -> performAuthenticationAndNavigate(userKey, Profile.class));

        balanceButton.setOnClickListener(view -> {
            String authType = (String) inputData.get("authType");
            if (authType != null) {
                if (authType.equals("3")) {
                    authenticateBiometric(() -> callInAppAuthenticator(userKey, this::showBalanceBottomSheet));
                } else if (authType.equals("4")) {
                    authenticateDeviceCredential(() -> callInAppAuthenticator(userKey, this::showBalanceBottomSheet));
                } else {
                    Log.e(TAG, "Unknown authentication type");
                    Snackbar.make(findViewById(android.R.id.content), "Unknown authentication type", Snackbar.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Invalid authentication type data");
                Snackbar.make(findViewById(android.R.id.content), "Invalid authentication type data", Snackbar.LENGTH_SHORT).show();
            }
        });

        walletTopUpButton.setOnClickListener(view -> {
            performAuthenticationAndNavigate(userKey, walletTopUp.class);
            // startActivity(new Intent(Dashboard.this, walletTopUp.class));performAuthenticationAndNavigate(walletTopUp.class);
        });
        mobileReloadButton.setOnClickListener(view -> {
             performAuthenticationAndNavigate(userKey, mobileReload.class);
            //startActivity(new Intent(Dashboard.this, mobileReload.class));
        });
        moneyTransferButton.setOnClickListener(view -> {
            performAuthenticationAndNavigate(userKey, moneyTransfer.class);
            // startActivity(new Intent(Dashboard.this, moneyTransfer.class));
        });

        moneyReceiveButton.setOnClickListener(view -> {
            // performAuthenticationAndNavigate(moneyTransfer.class);
            Intent intent = new Intent(Dashboard.this, moneyReceive.class);
            TextView walletIdTextView = findViewById(R.id.cardNumber);
            String walletId = walletIdTextView.getText().toString();
            intent.putExtra("walletId", walletId);
            startActivity(intent);
        });
    }

    /**
     * Displays a BottomSheetDialog showing the wallet balance.
     * This method inflates the layout for the balance dialog, sets up the close button,
     * and initializes the balance tracking.
     */
    private void showBalanceBottomSheet() {
        // Create and configure the BottomSheetDialog
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_show_balance, findViewById(android.R.id.content), false);
        dialog.setContentView(view);

        // Find the close button and balance TextView in the dialog layout
        Button buttonClose = dialog.findViewById(R.id.closeBalanceDialogButton);
        TextView balanceTextView = view.findViewById(R.id.balanceValueTextView);

        // Set up the close button click listener to dismiss the dialog
        assert buttonClose != null;
        buttonClose.setOnClickListener(v -> {
            // Close the dialog when the close button is clicked
            dialog.dismiss();
        });

        // Track and display the wallet balance in the TextView
        trackWalletBalance(balanceTextView);

        // Show the BottomSheetDialog
        dialog.show();
    }

    /**
     * Tracks the wallet balance in real-time and updates the provided TextView.
     * This method listens for changes in the wallet balance document and updates the
     * TextView with the formatted balance. If there is an error or the document does not exist,
     * it displays appropriate error messages.
     *
     * @param balanceTextView The TextView where the formatted balance will be displayed.
     */
    private void trackWalletBalance(TextView balanceTextView) {
        // Listen for real-time updates to the wallet balance document
        balanceDocRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                // Log and display an error message if there is a problem fetching the balance
                Log.e(TAG, "Error fetching balance", e);
                balanceTextView.setText(R.string.error_fetching_balance);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                // Retrieve the balance and format it as currency
                Double balance = documentSnapshot.getDouble("balance");
                NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("en", "NG"));
                currencyFormat.setMinimumFractionDigits(2); // Ensure at least 2 decimal places
                currencyFormat.setMaximumFractionDigits(2); // Limit to 2 decimal places
                currencyFormat.setGroupingUsed(true); // Enable grouping separators (commas)

                // Format the balance and set it to the TextView
                String balanceFormatted = currencyFormat.format(balance);
                balanceTextView.setText(balanceFormatted);
            } else {
                // Handle case where the document does not exist
                Log.e(TAG, "Document does not exist");
                balanceTextView.setText(R.string.balance_not_found);
            }
        });
    }

    /**
     * Sets up the list view for recent transactions and handles its interaction with the SwipeRefreshLayout.
     * This method initializes the ListView, sets up an adapter, and configures scrolling behavior to enable
     * or disable swipe-to-refresh functionality. It also queries the Firestore database for transactions
     * related to the user and updates the ListView with transaction data.
     */
    private void setupRecentTransactionsList() {
        // Find the ListView for transactions and create an empty list of transactions
        ListView transactionsListView = findViewById(R.id.transactionsListView);
        ArrayList<Transaction> transactions = new ArrayList<>();
        TransactionAdapter adapter = new TransactionAdapter(Dashboard.this, transactions);
        transactionsListView.setAdapter(adapter);

        // Handle ListView scrolling to enable/disable SwipeRefreshLayout
        transactionsListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // No action needed on scroll state change
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // Enable SwipeRefreshLayout only when the ListView is at the top
                if (transactionsListView.getChildCount() > 0) {
                    swipeRefreshLayout.setEnabled(firstVisibleItem == 0 && transactionsListView.getChildAt(0).getTop() == 0);
                } else {
                    swipeRefreshLayout.setEnabled(true);
                }
            }
        });

        // Retrieve holderRefId from inputData and check if it's valid
        holderRefId = (String) inputData.get("holderRefId");
        if (holderRefId == null || holderRefId.isEmpty()) {
            Toast.makeText(this, "User Information not loaded properly, please sign in again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SignIn.class));
            finish();
            return;
        }

        // Query Firestore for transactions where the user is either the sender or receiver
        Query userTransactionQuery = db.collection("itu_challenge_wallet_transactions")
                .whereEqualTo("user", holderRefId)
                .orderBy("datetime", Query.Direction.DESCENDING);

        Query receiverTransactionQuery = db.collection("itu_challenge_wallet_transactions")
                .whereEqualTo("receiver", holderRefId)
                .orderBy("datetime", Query.Direction.DESCENDING);

        // Combine the two queries into one task
        Task<List<QuerySnapshot>> combinedTask = Tasks.whenAllSuccess(userTransactionQuery.get(), receiverTransactionQuery.get());

        // Handle the result of the combined query
        combinedTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                transactions.clear(); // Clear existing transactions
                List<QuerySnapshot> querySnapshots = task.getResult();

                // Process each document in the query results
                for (QuerySnapshot querySnapshot : querySnapshots) {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        // Extract transaction details from the document
                        String title = (document.contains("receiver") && holderRefId.equals(document.get("receiver"))) ? "Received" : document.getString("title");
                        double amount = Objects.requireNonNullElse(document.getDouble("amount"), 0.00);
                        String datetime = document.getString("datetime");

                        // Format the amount as currency
                        NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("en", "NG"));
                        currencyFormat.setMinimumFractionDigits(2); // Ensure at least 2 decimal places
                        currencyFormat.setMaximumFractionDigits(2); // Limit to 2 decimal places
                        currencyFormat.setGroupingUsed(true); // Enable grouping separators (commas)

                        String amountFormatted = currencyFormat.format(amount);

                        // Determine the icon based on the transaction title
                        int iconResId;
                        switch (Objects.requireNonNull(title)) {
                            case "Mobile Reload":
                                iconResId = R.drawable.ic_baseline_send_to_mobile_24;
                                break;
                            case "Money Transfer":
                                iconResId = R.drawable.ic_baseline_money_24;
                                break;
                            case "Wallet Top Up":
                                iconResId = R.drawable.ic_baseline_account_balance_wallet_24;
                                break;
                            case "Received":
                                iconResId = R.drawable.ic_baseline_call_received_24;
                                break;
                            default:
                                iconResId = R.drawable.ic_success;
                                break;
                        }

                        // Add the transaction to the list
                        transactions.add(new Transaction(title, amountFormatted, datetime, iconResId));
                    }
                }

                // Sort transactions by datetime in descending order
                Collections.sort(transactions, (t1, t2) -> t2.getDatetime().compareTo(t1.getDatetime()));

                // Notify the adapter of data changes on the UI thread
                runOnUiThread(adapter::notifyDataSetChanged);
            } else {
                // Log and handle errors in querying transactions
                Log.e(TAG, "Error getting documents: ", task.getException());
            }
        });
    }

    /**
     * Sets up the bottom navigation view and handles item selection events.
     * This method initializes the BottomNavigationView and sets up a listener to handle navigation item selections.
     * It starts the appropriate activity based on the selected menu item.
     */
    @SuppressLint("NonConstantResourceId")
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.dashboardBottomNavigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            switch (itemId) {
                case R.id.nav_home:
                    // Navigate to the Dashboard activity
                    startActivity(new Intent(this, Dashboard.class));
                    return true;
                case R.id.nav_qr_auth:
                    // Navigate to the QRCodeAuth activity
                    startActivity(new Intent(this, QRCodeAuth.class));
                    return true;
                case R.id.nav_otp_auth:
                    // Navigate to the OtpAuth activity and unregister receivers if necessary
                    startActivity(new Intent(this, OtpAuth.class));
                    if (isReceiverRegistered) {
                        unregisterReceiver(authReceiver);
                        unregisterReceiver(snackbarReceiver);
                        isReceiverRegistered = false;
                        Log.e(TAG, "---> BroadcastReceiver unregistered");
                    }
                    return true;
                case R.id.nav_totp_auth:
                    // Navigate to the TotpAuth activity
                    Intent totpIntent = new Intent(this, TotpAuth.class);
                    startActivity(totpIntent);
                    return true;
                case R.id.nav_settings:
                    // Navigate to the Settings activity
                    startActivity(new Intent(this, Settings.class));
                    return true;
            }
            return false;
        });
    }

    /**
     * Handles authentication based on the specified authentication type and navigates to the given destination activity upon successful authentication.
     * The method retrieves the authentication type from `inputData`, performs the appropriate authentication (biometric or device credential),
     * and then starts the specified destination activity if authentication is successful.
     *
     * @param userKey            The key used for authentication.
     * @param destinationActivity The class of the activity to start after successful authentication.
     */
    private void performAuthenticationAndNavigate(String userKey, Class<?> destinationActivity) {
        // Retrieve the authentication type from input data
        String authType = (String) inputData.get("authType");
        Log.d(TAG, "Auth Type: " + authType);

        // Check if the authentication type is provided and perform corresponding authentication
        if (authType != null) {
            if (authType.equals("3")) {
                // Perform biometric authentication
                authenticateBiometric(() -> callInAppAuthenticator(userKey, () -> startActivity(new Intent(Dashboard.this, destinationActivity))));
            } else if (authType.equals("4")) {
                // Perform device credential authentication
                authenticateDeviceCredential(() -> callInAppAuthenticator(userKey, () -> startActivity(new Intent(Dashboard.this, destinationActivity))));
            } else {
                // Handle unknown authentication type
                Log.e(TAG, "Unknown authentication type");
                Snackbar.make(findViewById(android.R.id.content), "Unknown authentication type", Snackbar.LENGTH_SHORT).show();
            }
        } else {
            // Handle case where authentication type is null
            Log.e(TAG, "Invalid authentication type data");
            Snackbar.make(findViewById(android.R.id.content), "Invalid authentication type data", Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Initiates biometric authentication using the BiometricPrompt API.
     * If authentication is successful, the provided `onSuccess` runnable is executed.
     * Displays error messages if biometric authentication fails or is not available.
     *
     * @param onSuccess Runnable to be executed upon successful biometric authentication.
     */
    private void authenticateBiometric(Runnable onSuccess) {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    // Log and display biometric authentication error
                    Log.e(TAG, "Biometric authentication error: " + errString);
                    Snackbar.make(findViewById(android.R.id.content), "Biometric Authentication error: " + errString, Snackbar.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    // Execute the provided runnable upon successful authentication
                    onSuccess.run();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    // Log and display biometric authentication failure
                    Log.e(TAG, "Biometric authentication failed");
                    Snackbar.make(findViewById(android.R.id.content), "Biometric Authentication failed", Snackbar.LENGTH_SHORT).show();
                }
            });

            // Configure biometric prompt info
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric Authentication")
                    .setSubtitle("Authenticate to proceed")
                    .setNegativeButtonText("Cancel")
                    .build();

            // Start biometric authentication
            biometricPrompt.authenticate(promptInfo);
        } else {
            // Log and display if biometric authentication is not available
            Log.e(TAG, "Biometric authentication not available");
            Snackbar.make(findViewById(android.R.id.content), "Biometric authentication not available", Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Initiates device credential authentication using the KeyguardManager API.
     * If the device is secure, it launches a confirmation intent for credential authentication.
     * If authentication is successful, the provided `onSuccess` runnable is executed.
     * Displays an error message if the device is not secure.
     *
     * @param onSuccess Runnable to be executed upon successful device credential authentication.
     */
    private void authenticateDeviceCredential(Runnable onSuccess) {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager.isDeviceSecure()) {
            // Create and launch the device credential confirmation intent
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent("Authentication required", "Authenticate to proceed");
            pendingAction = onSuccess; // Store the action to be executed after successful authentication
            deviceCredentialLauncher.launch(intent);
        } else {
            // Log and display if the device is not secure
            Log.e(TAG, "Device is not secure");
            Snackbar.make(findViewById(android.R.id.content), "Device is not secure", Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Initiates in-app authentication using the BSA SDK and shows a progress dialog while processing.
     * Upon successful authentication, executes the provided `success` runnable.
     * Displays error messages if authentication fails and provides feedback through a Snackbar.
     *
     * @param userKey The key used for authentication.
     * @param success Runnable to be executed upon successful authentication.
     */
    private void callInAppAuthenticator(String userKey, Runnable success) {
        // Create and show a progress dialog while authentication is in progress
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setView(LayoutInflater.from(this).inflate(R.layout.dialog_progress, findViewById(android.R.id.content), false))
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    // Cancel the authentication process and dismiss the dialog
                    cancelExistingAuth();
                    dialogInterface.dismiss();
                })
                .create();
        progressDialog.show();

        // Initiate authentication using BSA SDK
        BsaSdk.getInstance().getSdkService().appAuthenticator(userKey, true, this, new SdkAuthResponseCallback<>() {
            @Override
            public void onSuccess(AuthResultResponse authResultResponse) {
                // Handle successful authentication
                Log.d(TAG, "In-app authentication successful: code: " + authResultResponse.getRtCode());
                String accessToken = SdkUtil.getAccessToken();
                Log.e(TAG, "[callInAppAuthenticator]___ Access Token is set, | Access token: " + accessToken);
                progressDialog.dismiss(); // Dismiss the dialog upon success
                success.run(); // Execute the success runnable
            }

            @Override
            public void onProcess(boolean b, String s) {
                // Provide feedback that authentication is in progress
                Log.d(TAG, "In-app authentication processing...: s: " + s);
                Snackbar.make(findViewById(android.R.id.content), "In-app authentication processing...: s: " + s, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                // Handle authentication failure
                progressDialog.dismiss(); // Dismiss the dialog upon failure
                Log.e(TAG, "In-app authentication failed: " + errorResult.getErrorMessage() + " | code: " + errorResult.getErrorCode());
                // Handle the authentication error
                runOnUiThread(() -> handleAuthError(errorResult.getErrorCode()));
            }
        });
    }

    /**
     * Handles authentication errors by showing an error dialog with details about the error code,
     * description, and suggested solutions. The dialog is shown on the UI thread.
     *
     * @param errorCode The error code returned from authentication failure.
     */
    @SuppressLint("SetTextI18n")
    private void handleAuthError(int errorCode) {
        // Define default values for error title, description, and solution
        String title = "Authentication Error";
        String description;
        String solution;

        // Determine description and solution based on error code
        switch (errorCode) {
            case 2004:
                description = "Channel does not exist";
                solution = "Check the URL used for initialization. If it happens constantly, please inquire the person in charge.";
                break;
            case 2010:
                description = "User authentication in-progress";
                solution = "Cancel previous authentication and request for a new one.";
                break;
            case 2100:
            case 2105:
            case 2107:
                description = "Token is expired or not found";
                solution = "Retry to authenticate.";
                break;
            case 2101:
            case 2102:
            case 2103:
                description = "Invalid token";
                solution = "Cancel previous authentication and request for a new one.";
                break;
            case 2106:
                description = "Unmatched signature token";
                solution = "Retry to authenticate.";
                break;
            case 5010:
                description = "Authentication failure";
                solution = "Contact the person in charge to solve this matter.";
                break;
            case 5011:
                description = "User authentication canceled";
                solution = "Make the person in charge solve this matter.";
                break;
            case 5015:
                description = "Failed to create channel";
                solution = "It can occur when the parameters are not enough. If it happens constantly, please inquire the person in charge.";
                break;
            case 5017:
                description = "Failed to send push notification";
                solution = "Problems with FCM (Firebase Cloud Messaging) etc. Also check whether the updated token is the correct one.";
                break;
            case 5022:
                description = "Verification failure";
                solution = "Node verification failed. If it happens constantly, please inquire the person in charge.";
                break;
            case 5000:
                description = "Socket Time Exception - Read timed out";
                solution = "Retry to authenticate.";
                break;
            default:
                description = "Unknown error";
                solution = "Please contact the person in charge.";
                break;
        }

        // Create and show an error dialog with details
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = getLayoutInflater();
            @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.dialog_error, null);
            builder.setView(dialogView);

            TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
            TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
            Button dialogButton = dialogView.findViewById(R.id.dialog_button);

            dialogTitle.setText(title);
            dialogMessage.setText("Error Code: " + errorCode + "\nDescription: " + description + "\nSolution: " + solution);

            AlertDialog dialog = builder.create();
            dialog.setIcon(R.drawable.ic_round_crisis_alert_24);
            dialogButton.setOnClickListener(v -> dialog.dismiss()); // Dismiss the dialog on button click
            dialog.show();
        });
    }

    /**
     * Initiates normal user ID-based authentication using the BSA SDK and shows a progress dialog while processing.
     * Executes the provided `success` runnable upon successful authentication and displays a Snackbar with success message.
     *
     * @param userKey The key used for authentication.
     * @param success Runnable to be executed upon successful authentication.
     */
    private void callNormalAuthenticator(String userKey, Runnable success) {
        // Create and show a progress dialog while authentication is in progress
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setView(LayoutInflater.from(this).inflate(R.layout.dialog_progress, findViewById(android.R.id.content), false))
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    // Cancel the authentication process and dismiss the dialog
                    cancelExistingAuth();
                    dialogInterface.dismiss();
                })
                .create();
        progressDialog.show();

        // Initiate normal authentication using BSA SDK
        BsaSdk.getInstance().getSdkService().normalAuthenticator(userKey, true, this, new SdkAuthResponseCallback<>() {
            @Override
            public void onSuccess(AuthCompleteResponse authResultResponse) {
                // Handle successful authentication
                Log.d(TAG, "Normal authentication successful: code: " + authResultResponse.getRtCode());
                progressDialog.dismiss(); // Dismiss the dialog upon success
                success.run(); // Execute the success runnable
                Snackbar.make(findViewById(android.R.id.content), "Normal (User ID) authentication successful", Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onProcess(boolean b, String s) {
                // Provide feedback that authentication is in progress
                Log.d(TAG, "Normal authentication processing...: s: " + s);
                Snackbar.make(findViewById(android.R.id.content), "Normal authentication processing...: s: " + s, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                // Handle authentication failure
                progressDialog.dismiss(); // Dismiss the dialog upon failure
                Log.e(TAG, "Normal authentication failed: " + errorResult.getErrorMessage() + " | code: " + errorResult.getErrorCode());
                // Handle the authentication error
                runOnUiThread(() -> handleAuthError(errorResult.getErrorCode()));
            }
        });
    }

    /**
     * Checks for any existing ongoing authentication requests using the BSA SDK.
     * If an ongoing authentication is found, it logs the details and initiates normal authentication.
     * If no ongoing authentication is found, it logs the result and proceeds accordingly.
     */
    private void checkExistingAuthRequest() {
        // Check for existing authentication requests using BSA SDK
        BsaSdk.getInstance().getSdkService().existAuth(userKey, new SdkResponseCallback<>() {
            @Override
            public void onSuccess(@NonNull AuthExistResponse authExist) {
                if (authExist.data.isExist) {
                    // Log details of the ongoing authentication
                    Log.d(TAG, "Ongoing authentication found clientName: " + authExist.data.clientName);
                    Log.d(TAG, "Ongoing authentication found siteUrl: " + authExist.data.siteUrl);
                    Log.d(TAG, "Ongoing authentication found clientKey: " + authExist.data.clientKey);
                    Log.d(TAG, "Ongoing authentication found: timeout" + authExist.data.timeout);
                    // Show a Snackbar message about the ongoing authentication
                    Snackbar.make(findViewById(android.R.id.content), "Ongoing authentication from: " + authExist.data.clientName + " web.", Snackbar.LENGTH_LONG).show();
                    // Optionally handle ongoing authentication and initiate normal authentication
                    // cancelExistingAuth();
                    callNormalAuthenticator(userKey, FirebasePushService::completeAuth);
                } else {
                    // Log that no ongoing authentication was found
                    Log.d(TAG, "No ongoing authentication found");
                    Snackbar.make(findViewById(android.R.id.content), "No ongoing authentication found", Snackbar.LENGTH_LONG).show();
                    // Optionally proceed with normal authentication
                    // callNormalAuthenticator(userKey, FirebasePushService::completeAuth);
                }
            }

            @Override
            public void onFailed(ErrorResult authFailed) {
                if (authFailed != null) {
                    // Log error checking existing authentication
                    Log.e(TAG, "Error checking existing auth: " + authFailed.getErrorMessage());
                    Snackbar.make(findViewById(android.R.id.content), "Error checking existing auth: " + authFailed.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                }
                // Optionally proceed with normal authentication if error occurs
                // callNormalAuthenticator(userKey, FirebasePushService::completeAuth);
            }
        });
    }

    /**
     * Cancels any existing authentication requests using the BSA SDK.
     * Shows a Snackbar with the result of the cancellation operation.
     */
    private void cancelExistingAuth() {
        // Cancel existing authentication using BSA SDK
        BsaSdk.getInstance().getSdkService().cancelAuth(new SdkResponseCallback<>() {
            @Override
            public void onSuccess(AuthCancelResponse authCancel) {
                if (authCancel != null) {
                    // Log successful cancellation
                    Log.d(TAG, "Authentication cancellation successful: code: " + authCancel.rtCode + " | " + authCancel.rtMsg);
                    Snackbar.make(findViewById(android.R.id.content), "Authentication cancellation successful", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailed(ErrorResult authFailed) {
                if (authFailed != null) {
                    // Log error cancelling authentication
                    Log.e(TAG, "Error cancelling authentication: code: " + authFailed.getErrorCode() + " | message: " + authFailed.getErrorMessage());
                    Snackbar.make(findViewById(android.R.id.content), "Error cancelling authentication", Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }
}