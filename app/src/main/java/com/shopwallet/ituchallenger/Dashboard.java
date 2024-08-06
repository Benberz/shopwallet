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

    private void refreshDashboard() {
        // Show the progress bar while loading data
        swipeRefreshLayout.setRefreshing(true);

        // Reload card holder info and recent transactions
        getCardHolderInfo();
        setupRecentTransactionsList();

        // Hide the progress bar once loading is complete
        swipeRefreshLayout.setRefreshing(false);
    }

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
    private String formatWalletId(String walletIdStr) {
        if (walletIdStr != null && walletIdStr.length() >= 10) {
            walletIdStr = walletIdStr.substring(0, 3) + " " + walletIdStr.substring(3, 6) + " " + walletIdStr.substring(6);
        }
        return walletIdStr;
    }

    // Helper method to format creationDate to MM/YYYY with month spelled out
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

    private void showBalanceBottomSheet()  {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_show_balance, findViewById(android.R.id.content), false);
        dialog.setContentView(view);

        Button buttonClose = dialog.findViewById(R.id.closeBalanceDialogButton);
        TextView balanceTextView = view.findViewById(R.id.balanceValueTextView);

        assert buttonClose != null;
        buttonClose.setOnClickListener(v -> {
            // Handle Yes button click
            // Add your deregister logic here

            dialog.dismiss();
        });

        trackWalletBalance(balanceTextView);

        dialog.show();
    }

    private void trackWalletBalance(TextView balanceTextView) {
        balanceDocRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Error fetching balance", e);
                balanceTextView.setText(R.string.error_fetching_balance);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                Double balance = documentSnapshot.getDouble("balance");
                NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("en", "NG"));
                currencyFormat.setMinimumFractionDigits(2); // Ensure at least 2 decimal places
                currencyFormat.setMaximumFractionDigits(2); // Limit to 2 decimal places
                currencyFormat.setGroupingUsed(true); // Enable grouping separators (commas)

                String balanceFormatted = currencyFormat.format(balance);
                balanceTextView.setText(balanceFormatted);
            } else {
                Log.e(TAG, "Document does not exist");
                balanceTextView.setText(R.string.balance_not_found);
            }
        });
    }

    private void setupRecentTransactionsList() {
        ListView transactionsListView = findViewById(R.id.transactionsListView);
        ArrayList<Transaction> transactions = new ArrayList<>();
        TransactionAdapter adapter = new TransactionAdapter(Dashboard.this, transactions);
        transactionsListView.setAdapter(adapter);

        // Add this part to handle ListView scrolling and SwipeRefreshLayout behavior
        // Add this part to handle ListView scrolling and SwipeRefreshLayout behavior
        transactionsListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // Do nothing here
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


        holderRefId = (String) inputData.get("holderRefId");
        if (holderRefId == null || holderRefId.isEmpty()) {
            Toast.makeText(this, "User Information not loaded properly, please sign in again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SignIn.class));
            finish();
            return;
        }

        // Query for transactions where user value equals holderRefId
        Query userTransactionQuery = db.collection("itu_challenge_wallet_transactions")
                .whereEqualTo("user", holderRefId)
                .orderBy("datetime", Query.Direction.DESCENDING);

        // Query for transactions where receiver value equals holderRefId
        Query receiverTransactionQuery = db.collection("itu_challenge_wallet_transactions")
                .whereEqualTo("receiver", holderRefId)
                .orderBy("datetime", Query.Direction.DESCENDING);

        // Use Tasks to combine both queries
        Task<List<QuerySnapshot>> combinedTask = Tasks.whenAllSuccess(userTransactionQuery.get(), receiverTransactionQuery.get());

        combinedTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                transactions.clear();
                List<QuerySnapshot> querySnapshots = task.getResult();

                for (QuerySnapshot querySnapshot : querySnapshots) {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String title = (document.contains("receiver") && holderRefId.equals(document.get("receiver"))) ? "Received": document.getString("title");
                        double amount = Objects.requireNonNullElse(document.getDouble("amount"), 0.00);
                        String datetime = document.getString("datetime");

                        NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("en", "NG"));
                        currencyFormat.setMinimumFractionDigits(2); // Ensure at least 2 decimal places
                        currencyFormat.setMaximumFractionDigits(2); // Limit to 2 decimal places
                        currencyFormat.setGroupingUsed(true); // Enable grouping separators (commas)

                        String amountFormatted = currencyFormat.format(amount);

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

                        transactions.add(new Transaction(title, amountFormatted, datetime, iconResId));
                    }
                }

                // Sort transactions by datetime in descending order
                Collections.sort(transactions, (t1, t2) -> t2.getDatetime().compareTo(t1.getDatetime()));

                // Notify the adapter of the data change on the UI thread
                runOnUiThread(adapter::notifyDataSetChanged);
            } else {
                Log.e(TAG, "Error getting documents: ", task.getException());
            }
        });
    }


    @SuppressLint("NonConstantResourceId")
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.dashboardBottomNavigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            switch (itemId) {
                case R.id.nav_home:
                    startActivity(new Intent(this, Dashboard.class));
                    return true;
                case R.id.nav_qr_auth:
                    startActivity(new Intent(this, QRCodeAuth.class));
                    return true;
                case R.id.nav_otp_auth:
                    // performAuthenticationAndNavigate(userKey, OtpAuth.class);
                    startActivity(new Intent(this, OtpAuth.class));
                    if (isReceiverRegistered) {
                        unregisterReceiver(authReceiver);
                        unregisterReceiver(snackbarReceiver);
                        isReceiverRegistered = false;
                        Log.e(TAG, "---> BroadcastReceiver unregistered");
                    }

                    return true;
                case R.id.nav_totp_auth:
                    Intent totpIntent = new Intent(this, TotpAuth.class);
                    startActivity(totpIntent);
                    return true;
                case R.id.nav_settings:
                    startActivity(new Intent(this, Settings.class));
                    return true;
            }
            return false;
        });
    }

    private void performAuthenticationAndNavigate(String userKey, Class<?> destinationActivity) {
        String authType = (String) inputData.get("authType");
        Log.d(TAG, "Auth Type: " + authType);

        if (authType != null) {
            if (authType.equals("3")) {
                authenticateBiometric(() -> callInAppAuthenticator(userKey, () -> startActivity(new Intent(Dashboard.this, destinationActivity))));
            } else if (authType.equals("4")) {
                authenticateDeviceCredential(() -> callInAppAuthenticator(userKey, () -> startActivity(new Intent(Dashboard.this, destinationActivity)) ));
            } else {
                Log.e(TAG, "Unknown authentication type");
                Snackbar.make(findViewById(android.R.id.content), "Unknown authentication type", Snackbar.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Invalid authentication type data");
            Snackbar.make(findViewById(android.R.id.content), "Invalid authentication type data", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void authenticateBiometric(Runnable onSuccess) {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Log.e(TAG, "Biometric authentication error: " + errString);
                    Snackbar.make(findViewById(android.R.id.content), "Biometric Authentication error: " + errString, Snackbar.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    onSuccess.run();

                    // check if this is from Firebase Push Service

                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Log.e(TAG, "Biometric authentication failed");
                    Snackbar.make(findViewById(android.R.id.content), "Biometric Authentication failed", Snackbar.LENGTH_SHORT).show();
                    // sendAuthResultToService(false);
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
    }

    private void authenticateDeviceCredential(Runnable onSuccess) {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager.isDeviceSecure()) {
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent("Authentication required", "Authenticate to proceed");
            pendingAction = onSuccess;
            deviceCredentialLauncher.launch(intent);
        } else {
            Log.e(TAG, "Device is not secure");
            Snackbar.make(findViewById(android.R.id.content), "Device is not secure", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void callInAppAuthenticator(String userKey, Runnable success) {

        // Show a dialog while processing
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setView(LayoutInflater.from(this).inflate(R.layout.dialog_progress, findViewById(android.R.id.content), false))
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    cancelExistingAuth();
                    dialogInterface.dismiss(); // Dismiss the dialog
                })
                .create();
        progressDialog.show();


        BsaSdk.getInstance().getSdkService().appAuthenticator(userKey, true, this, new SdkAuthResponseCallback<>() {
            @Override
            public void onSuccess(AuthResultResponse authResultResponse) {
                Log.d(TAG, "In-app authentication successful: code: " + authResultResponse.getRtCode());
                String accessToken = SdkUtil.getAccessToken();
                Log.e(TAG, "[callInAppAuthenticator]___ Access Token is set, | Access token: " + accessToken);
                progressDialog.dismiss(); // Dismiss the dialog when authentication fails
                success.run();
            }

            @Override
            public void onProcess(boolean b, String s) {
                Log.d(TAG, "In-app authentication processing...: s: " + s);
                Snackbar.make(findViewById(android.R.id.content), "In-app authentication processing...: s: " + s, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                progressDialog.dismiss(); // Dismiss the dialog when authentication fails
                Log.e(TAG, "In-app authentication failed: " + errorResult.getErrorMessage() + " | code: " + errorResult.getErrorCode());
                runOnUiThread(() -> handleAuthError(errorResult.getErrorCode()));
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void handleAuthError(int errorCode) {
        String title = "Authentication Error";
        String description;
        String solution;

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
            dialogButton.setOnClickListener(v -> {
                // if (errorCode == 2010) cancelExistingAuth();
                dialog.dismiss();
            });
            dialog.show();
        });
    }

    private void callNormalAuthenticator(String userKey, Runnable success) {

        // Show a dialog while processing
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setView(LayoutInflater.from(this).inflate(R.layout.dialog_progress, findViewById(android.R.id.content), false))
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    cancelExistingAuth();
                    dialogInterface.dismiss(); // Dismiss the dialog
                })
                .create();
        progressDialog.show();


        BsaSdk.getInstance().getSdkService().normalAuthenticator(userKey, true, this, new SdkAuthResponseCallback<>() {
            @Override
            public void onSuccess(AuthCompleteResponse authResultResponse) {
                Log.d(TAG, "Normal authentication successful: code: " + authResultResponse.getRtCode());
                progressDialog.dismiss(); // Dismiss the dialog when authentication fails
                success.run();
                Snackbar.make(findViewById(android.R.id.content), "Normal (User ID) authentication successful", Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onProcess(boolean b, String s) {
                Log.d(TAG, "Normal authentication processing...: s: " + s);
                Snackbar.make(findViewById(android.R.id.content), "Normal authentication processing...: s: " + s, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                progressDialog.dismiss(); // Dismiss the dialog when authentication fails
                // Log.e(TAG, "channelKey: " + channelKey);
                Log.e(TAG, "Normal authentication failed: " + errorResult.getErrorMessage() + " | code: " + errorResult.getErrorCode());
                runOnUiThread(() -> handleAuthError(errorResult.getErrorCode()));
            }
        });
    }

    private void checkExistingAuthRequest() {
        BsaSdk.getInstance().getSdkService().existAuth(userKey, new SdkResponseCallback<>() {
            @Override
            public void onSuccess(@NonNull AuthExistResponse authExist) {
                if (authExist.data.isExist) {
                    Log.d(TAG, "Ongoing authentication found clientName: " + authExist.data.clientName);
                    Log.d(TAG, "Ongoing authentication found siteUrl: " + authExist.data.siteUrl);
                    Log.d(TAG, "Ongoing authentication found clientKey: " + authExist.data.clientKey);
                    Log.d(TAG, "Ongoing authentication found: timeout" + authExist.data.timeout);
                    // Handle ongoing authentication process if needed
                    Snackbar.make(findViewById(android.R.id.content), "Ongoing authentication from: " + authExist.data.clientName + " web.", Snackbar.LENGTH_LONG).show();
                    // You can show a message to the user or take appropriate action
                    // cancelExistingAuth();
                    callNormalAuthenticator(userKey, FirebasePushService::completeAuth);
                } else {
                    Log.d(TAG, "No ongoing authentication found");
                    // Proceed with the normal authentication process
                    Snackbar.make(findViewById(android.R.id.content), "No ongoing authentication found", Snackbar.LENGTH_LONG).show();
                    // callNormalAuthenticator(userKey, FirebasePushService::completeAuth);

                }
            }

            @Override
            public void onFailed(ErrorResult authFailed) {
                if (authFailed != null) {
                    Log.e(TAG, "Error checking existing auth: " + authFailed.getErrorMessage());
                    Snackbar.make(findViewById(android.R.id.content), "Error checking existing auth: " + authFailed.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                }
                // Proceed with the normal authentication process if there's an error checking the existing auth
                // callNormalAuthenticator(userKey, FirebasePushService::completeAuth);
            }
        });
    }

    private void cancelExistingAuth() {
        BsaSdk.getInstance().getSdkService().cancelAuth(new SdkResponseCallback<>() {
            @Override
            public void onSuccess(AuthCancelResponse authCancel) {
                if (authCancel != null) {
                    Log.d(TAG, "Authentication cancellation successful: code: " + authCancel.rtCode + " | " + authCancel.rtMsg);
                    // Handle successful cancellation if needed
                    Snackbar.make(findViewById(android.R.id.content), "Authentication cancellation successful", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailed(ErrorResult authFailed) {
                if (authFailed != null) {
                    Log.e(TAG, "Error cancelling authentication: code: " + authFailed.getErrorCode() + " | message: " + authFailed.getErrorMessage());
                }
                // Handle failed cancellation if needed
                Snackbar.make(findViewById(android.R.id.content), "Error cancelling authentication", Snackbar.LENGTH_LONG).show();
            }
        });
    }

}