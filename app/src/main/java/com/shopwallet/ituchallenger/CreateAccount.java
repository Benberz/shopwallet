package com.shopwallet.ituchallenger;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.fnsv.bsa.sdk.BsaSdk;
import com.fnsv.bsa.sdk.callback.SdkResponseCallback;
import com.fnsv.bsa.sdk.common.SdkConstant;
import com.fnsv.bsa.sdk.response.CheckDuplicateEmailOrPhoneNumberReponse;
import com.fnsv.bsa.sdk.response.ErrorResult;
import com.fnsv.bsa.sdk.response.SendOtpResponse;

import java.util.HashMap;
import java.util.Objects;

/**
 * Activity class for handling user account creation.
 * This class includes input validation, checking for duplicate email addresses,
 * and requesting email verification via OTP.
 */
public class CreateAccount extends AppCompatActivity {

    // Tag for logging purposes
    private static final String TAG = "CreateAccountClass";

    // UI components for user input
    private EditText userIDInput;
    private EditText emailAddressInput;
    private EditText phoneNumberInput;
    private EditText nameInput;

    /**
     * Called when the activity is first created. Initializes the UI components
     * and sets up click listeners for the buttons.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        // Initialize UI components
        userIDInput = findViewById(R.id.phoneNumberReloadInput);
        emailAddressInput = findViewById(R.id.emailAddressInput);
        phoneNumberInput = findViewById(R.id.phoneNumberInput);
        nameInput = findViewById(R.id.nameInput);

        Button nextCreateAccountButton = findViewById(R.id.selectAuthTypeNextButton);

        // Set click listener on the Next button to validate inputs
        nextCreateAccountButton.setOnClickListener(view -> validateInputs());

        // Set up the "Sign In" link with an underline and click listener
        TextView signInTextView = findViewById(R.id.SignInTextView);
        signInTextView.setPaintFlags(signInTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        signInTextView.setOnClickListener(v -> {
            // Navigate to the Sign In activity
            Intent intent = new Intent(CreateAccount.this, SignIn.class);
            startActivity(intent);
        });
    }

    /**
     * Validates the user input fields. If all inputs are valid, proceeds to check for duplicate email.
     */
    private void validateInputs() {
        // Collect input data into a HashMap
        HashMap<String, String> inputData = new HashMap<>();
        inputData.put("userKey", userIDInput.getText().toString().trim());
        inputData.put("email", emailAddressInput.getText().toString().trim());
        inputData.put("phoneNum", phoneNumberInput.getText().toString().trim());
        inputData.put("name", nameInput.getText().toString().trim());

        // Validate user ID input
        if (TextUtils.isEmpty(inputData.get("userKey"))) {
            userIDInput.setError("User ID is required");
            userIDInput.requestFocus();
            return;
        }

        // Validate email address input
        if (TextUtils.isEmpty(inputData.get("email"))) {
            emailAddressInput.setError("Email is required");
            emailAddressInput.requestFocus();
            return;
        }

        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(Objects.requireNonNull(inputData.get("email"))).matches()) {
            emailAddressInput.setError("Invalid email address");
            emailAddressInput.requestFocus();
            return;
        }

        // Validate phone number input
        if (TextUtils.isEmpty(inputData.get("phoneNum"))) {
            phoneNumberInput.setError("Phone number is required");
            phoneNumberInput.requestFocus();
            return;
        }

        // Ensure phone number is digits-only and has a minimum length
        if (!TextUtils.isDigitsOnly(inputData.get("phoneNum")) || Objects.requireNonNull(inputData.get("phoneNum")).length() < 10) {
            phoneNumberInput.setError("Invalid phone number");
            phoneNumberInput.requestFocus();
            return;
        }

        // Validate full name input
        if (TextUtils.isEmpty(inputData.get("name"))) {
            nameInput.setError("Full name is required");
            nameInput.requestFocus();
            return;
        }

        // All inputs are valid; proceed to check for duplicate email
        Toast.makeText(this, "All inputs are valid", Toast.LENGTH_SHORT).show();
        checkForDuplicateEmail(inputData);
    }

    /**
     * Checks if the provided email address is already registered.
     *
     * @param inputData The user input data collected from the input fields.
     */
    private void checkForDuplicateEmail(HashMap<String, String> inputData) {

        // Use the BSA SDK to check for duplicate email addresses
        BsaSdk.getInstance().getSdkService().isDuplicatedEmailOrPhoneNumber(java.util.Map.of("verifyType", SdkConstant.OtpType.EMAIL.getValue(), "verifyData", Objects.requireNonNull(inputData.get("email"))), new SdkResponseCallback<>() {
            @Override
            public void onSuccess(CheckDuplicateEmailOrPhoneNumberReponse result) {
                // Handle success response
                if (result != null && result.getRtCode() == 0) {
                    Log.d(TAG, "Email is not duplicated.");
                    requestEmailVerification(inputData);
                } else if (result != null) {
                    // Handle specific error code for duplicate email
                    if (result.getRtCode() == 2019) {
                        showAlertDialog("Email already registered", "The email you entered is already registered. Please use a different email.");
                    } else {
                        Log.d(TAG, "Email check failed with code: " + result.getRtCode());
                    }
                }
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                // Handle failure response
                if (errorResult != null) {
                    Log.d(TAG, "Email check failed with error: " + errorResult.getErrorMessage());
                    showAlertDialog("Error", "Email already used. Please try another one.");
                }
            }
        });
    }

    /**
     * Requests email verification by sending an OTP to the provided email address.
     *
     * @param inputData The user input data collected from the input fields.
     */
    private void requestEmailVerification(HashMap<String, String> inputData) {
        // Prepare parameters for sending the OTP
        HashMap<String, Object> params = new HashMap<>();
        params.put("clientKey", BuildConfig.CLIENT_KEY);
        params.put("email", inputData.get("email"));

        // Use the BSA SDK to send the OTP to the email address
        BsaSdk.getInstance().getSdkService().sendOtpByEmail(params, new SdkResponseCallback<>() {
            @Override
            public void onSuccess(SendOtpResponse result) {
                // Handle success response for sending OTP
                if (result != null && result.getRtCode() == 0) {
                    Log.d(TAG, "OTP sent to email.");
                    Toast.makeText(getApplicationContext(), "OTP sent to email", Toast.LENGTH_SHORT).show();

                    // Navigate to the OTP verification activity
                    Intent createAccountIntent = new Intent(CreateAccount.this, VerifyOTP.class);
                    createAccountIntent.putExtra("from", "createAccount");
                    createAccountIntent.putExtra("inputData", inputData);
                    startActivity(createAccountIntent);
                    finish();
                } else {
                    // Handle failure response with specific error code
                    assert result != null;
                    Log.d(TAG, "Failed to send OTP, code: " + result.getRtCode());
                    showAlertDialog("Error", "Failed to send OTP. Please try again.");
                }
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                // Handle failure response for sending OTP
                if (errorResult != null) {
                    Log.d(TAG, "Failed to send OTP, error: " + errorResult.getErrorMessage());
                    showAlertDialog("Error", "Failed to send OTP. Please try again.");
                }
            }
        });
    }

    /**
     * Displays an AlertDialog with the provided title and message.
     *
     * @param title   The title of the AlertDialog.
     * @param message The message to be displayed in the AlertDialog.
     */
    private void showAlertDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .setIcon(R.drawable.ic_baseline_question_answer_24)
                .show();
    }
}