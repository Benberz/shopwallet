package com.shopwallet.ituchallenger;

import static com.shopwallet.ituchallenger.BuildConfig.CLIENT_KEY;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.fnsv.bsa.sdk.BsaSdk;
import com.fnsv.bsa.sdk.callback.SdkResponseCallback;
import com.fnsv.bsa.sdk.response.ErrorResult;
import com.fnsv.bsa.sdk.response.VerifyOtpResponse;
import com.shopwallet.ituchallenger.util.SecureStorageUtil;

import java.util.HashMap;

/**
 * VerifyOTP is an activity that handles the OTP verification process.
 * It allows the user to enter a 6-digit OTP and verifies it using the BSA SDK.
 */
public class VerifyOTP extends AppCompatActivity {

    private static final String TAG = "VerifyOTPClass"; // Tag for logging
    private HashMap<String, Object> inputData; // Stores input data passed between activities

    private EditText otpDigit1, otpDigit2, otpDigit3, otpDigit4, otpDigit5, otpDigit6; // EditText fields for OTP digits
    private ProgressBar progressBar; // ProgressBar to indicate loading state

    private static String from = ""; // Indicates the source activity or action

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        // Retrieve the inputData from the Intent
        Intent intent = getIntent();
        inputData = (HashMap<String, Object>) intent.getSerializableExtra("inputData");
        if (inputData == null) {
            // If inputData is null, retrieve it from secure storage
            inputData = SecureStorageUtil.retrieveDataFromKeystore(getApplicationContext(), "inputData");
        }
        String email = (String) inputData.get("email");

        // Initialize UI elements
        TextView otpEmailTextView = findViewById(R.id.otpEmailTextView);
        TextView timeRemainingTextView = findViewById(R.id.timeRemainingTextView);
        TextView timeTextView = findViewById(R.id.timeTextView);

        // Retrieve OTP from EditTexts
        otpDigit1 = findViewById(R.id.otpDigit1);
        otpDigit2 = findViewById(R.id.otpDigit2);
        otpDigit3 = findViewById(R.id.otpDigit3);
        otpDigit4 = findViewById(R.id.otpDigit4);
        otpDigit5 = findViewById(R.id.otpDigit5);
        otpDigit6 = findViewById(R.id.otpDigit6);

        // Set up the progress bar
        progressBar = findViewById(R.id.verifyOtpProgressBar);
        progressBar.setVisibility(View.GONE);

        // Set up OTP digit fields to shift focus to the next field
        setupOtpEditTexts();

        // Set the email value in the UI
        if (email != null) {
            otpEmailTextView.setText(email);
        }

        // Hide timeRemainingTextView and timeTextView
        timeRemainingTextView.setVisibility(View.GONE);
        timeTextView.setVisibility(View.GONE);

        // Set the Next Navigation function
        Button nextCreateAccountButton = findViewById(R.id.selectAuthTypeNextButton);

        // Determine the source activity or action
        from = intent.getStringExtra("from");
        Log.d(TAG, "Value of from is: " + from);
        if ("Dashboard".equals(from)) {
            nextCreateAccountButton.setText(R.string.cancel); // Set button text to "Cancel" if from Dashboard
        }

        // Set OnClickListener for the Next button
        nextCreateAccountButton.setOnClickListener(view -> {
            if ("createAccount".equals(from) || "signIn".equals(from)) {
                // Prompt user to enter OTP if from createAccount or signIn
                promptUserForOtp(email);
            } else if ("Dashboard".equals(from)) {
                // Navigate to Dashboard if from Dashboard
                Intent completeRegistrationIntent = new Intent(VerifyOTP.this, Dashboard.class);
                startActivity(completeRegistrationIntent);
            } else {
                // Navigate to RegistrationCompleted for other cases
                Intent completeRegistrationIntent = new Intent(VerifyOTP.this, RegistrationCompleted.class);
                startActivity(completeRegistrationIntent);
            }
        });
    }

    /**
     * Prompts the user to enter the OTP and initiates verification if a valid OTP is entered.
     *
     * @param email The user's email associated with the OTP.
     */
    private void promptUserForOtp(String email) {
        // Concatenate the OTP digits from the EditText fields
        String otp = otpDigit1.getText().toString() +
                otpDigit2.getText().toString() +
                otpDigit3.getText().toString() +
                otpDigit4.getText().toString() +
                otpDigit5.getText().toString() +
                otpDigit6.getText().toString();

        // Check if the OTP has 6 digits
        if (otp.length() == 6) {
            progressBar.setVisibility(View.VISIBLE); // Show progress bar while verifying OTP
            confirmEmailVerification(email, otp);
        } else {
            Toast.makeText(this, "Please enter a valid 6-digit OTP.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Confirms the email verification by sending the OTP to the server via the BSA SDK.
     *
     * @param email The user's email to verify.
     * @param otp   The 6-digit OTP entered by the user.
     */
    private void confirmEmailVerification(String email, String otp) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("clientKey", CLIENT_KEY); // Add client key to parameters
        params.put("email", email); // Add email to parameters
        params.put("authNum", otp); // Add OTP to parameters

        // Call the BSA SDK to verify the OTP
        BsaSdk.getInstance().getSdkService().verifyOtpByEmail(params, new SdkResponseCallback<>() {
            @Override
            public void onSuccess(VerifyOtpResponse result) {
                progressBar.setVisibility(View.GONE); // Hide progress bar on success
                if (result != null && result.getRtCode() == 0) {
                    // Email verified successfully
                    Log.d(TAG, "Email verified successfully.");
                    Toast.makeText(VerifyOTP.this, "Email verified successfully.", Toast.LENGTH_SHORT).show();
                    // Add disposeToken to inputData
                    inputData.put("authNum", otp);
                    inputData.put("disposeToken", result.data.disposeToken);

                    // Move to TwoFactorAuth activity
                    Intent twoFactorIntent = new Intent(VerifyOTP.this, TwoFactorAuth.class);
                    twoFactorIntent.putExtra("from", from);
                    twoFactorIntent.putExtra("inputData", inputData);
                    startActivity(twoFactorIntent);
                    finish(); // Close the current activity
                } else {
                    assert result != null;
                    Log.d(TAG, "Failed to verify email, code: " + result.getRtCode());
                    handleError(result.getRtCode()); // Handle verification error
                }
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                progressBar.setVisibility(View.GONE); // Hide progress bar on failure
                if (errorResult != null) {
                    Log.d(TAG, "Failed to verify email, error: " + errorResult.getErrorMessage());
                    Toast.makeText(VerifyOTP.this, "Error verifying OTP: " + errorResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Handles error codes returned by the BSA SDK during OTP verification.
     *
     * @param errorCode The error code returned by the SDK.
     */
    private void handleError(int errorCode) {
        String message;
        switch (errorCode) {
            case 2000:
                message = "Invalid client key. Check the client key used for initialization.";
                break;
            case 2004:
                message = "Channel does not exist. Check the URL used for initialization.";
                break;
            case 2008:
                message = "Unregistered user. Check the BSA sign-in status.";
                break;
            case 2010:
                message = "User authentication in-progress. Cancel the previous authentication and request a new one.";
                break;
            case 2100:
            case 2105:
            case 2107:
                message = "Token is expired. Retry to authenticate.";
                break;
            case 2101:
            case 2102:
            case 2103:
                message = "Token error. Cancel the previous authentication and request a new one.";
                break;
            case 5001:
                message = "Authentication timeout. Make the request for authentication once again.";
                break;
            case 5005:
            case 5006:
            case 5007:
                message = "Unauthorized or suspended user. Contact the person in charge.";
                break;
            case 5010:
                message = "Authentication failure. Contact the person in charge.";
                break;
            case 5011:
                message = "User authentication canceled. Contact the person in charge.";
                break;
            case 5015:
                message = "Failed to create channel. Check the parameters or contact the person in charge.";
                break;
            case 5017:
                message = "Failed to send push notification. Check the FCM token or contact the person in charge.";
                break;
            case 5022:
                message = "Verification failure. Contact the person in charge.";
                break;
            case 9001:
                message = "Current Android version does not support biometric authentication. It will be switched to PIN or pattern authentication.";
                break;
            case 9003:
                message = "Device does not support biometric authentication. It will be switched to PIN or pattern authentication.";
                break;
            case 9004:
                message = "Biometric information not found on the device. It will be switched to PIN or pattern authentication.";
                break;
            case 9005:
                message = "Guardian CCS does not have biometric information. Use RegisterBiometric() from GuardianSdk.";
                break;
            case 9006:
                message = "Biometric information has been changed. Use resetBiometricChange() to reset.";
                break;
            case 9007:
                message = "Biometric information already registered. Use the right biometric information.";
                break;
            case 9008:
                message = "Biometric information does not match. Use the right biometric information.";
                break;
            case 9009:
                message = "Biometric authentication error. Contact the person in charge.";
                break;
            case 10002:
                message = "SDK error. Contact the person in charge.";
                break;
            case 10003:
                message = "Server error. Contact the person in charge.";
                break;
            case 10004:
                message = "Server connection error. Check the internet connection and server address.";
                break;
            default:
                message = "Unknown error occurred. Please try again.";
                break;
            }
            showDialog(message);
        }

    /**
     * Shows an alert dialog with the specified error message.
     * This method creates and displays an AlertDialog with a title of "Error" and
     * a message passed as a parameter. It includes a single button that dismisses the dialog.
     *
     * @param message The error message to display in the dialog.
     */
    private void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Sets up TextWatchers for OTP EditText fields to handle focus management.
     * This method assigns a TextWatcher to each OTP EditText field to automatically
     * move focus to the next field when a digit is entered, and move back to the previous
     * field if a digit is deleted. This ensures a smooth user experience while entering
     * OTP digits.
     */
    private void setupOtpEditTexts() {
        otpDigit1.addTextChangedListener(new OtpTextWatcher(otpDigit2, null));
        otpDigit2.addTextChangedListener(new OtpTextWatcher(otpDigit3, otpDigit1));
        otpDigit3.addTextChangedListener(new OtpTextWatcher(otpDigit4, otpDigit2));
        otpDigit4.addTextChangedListener(new OtpTextWatcher(otpDigit5, otpDigit3));
        otpDigit5.addTextChangedListener(new OtpTextWatcher(otpDigit6, otpDigit4));
        otpDigit6.addTextChangedListener(new OtpTextWatcher(null, otpDigit5));
    }

    /**
     * A TextWatcher implementation for managing focus between OTP digit EditTexts.
     * This class handles focus shifting between EditTexts based on user input.
     * When a digit is entered, focus shifts to the next EditText. If a digit is deleted,
     * focus moves to the previous EditText.
     */
    private static class OtpTextWatcher implements TextWatcher {
        private final EditText nextView; // The EditText to move focus to
        private final EditText prevView; // The EditText to move focus from

        /**
         * Constructor for OtpTextWatcher.
         *
         * @param nextView The EditText to focus on after entering a digit.
         * @param prevView The EditText to focus on if a digit is deleted.
         */
        public OtpTextWatcher(EditText nextView, EditText prevView) {
            this.nextView = nextView;
            this.prevView = prevView;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            // Move focus to the next EditText if one character is entered
            if (s.length() == 1 && nextView != null) {
                nextView.requestFocus();
            } else if (s.length() == 0 && prevView != null) {
                // Move focus to the previous EditText if a character is deleted
                prevView.requestFocus();
            }
        }
    }
}
