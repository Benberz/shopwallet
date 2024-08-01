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

public class CreateAccount extends AppCompatActivity {

    private static final String TAG = "CreateAccountClass";

    private EditText userIDInput;
    private EditText emailAddressInput;
    private EditText phoneNumberInput;
    private EditText nameInput;

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

        // Set click listener on the Next button
        nextCreateAccountButton.setOnClickListener(view -> validateInputs());

        TextView signInTextView = findViewById(R.id.SignInTextView);
        signInTextView.setPaintFlags(signInTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        signInTextView.setOnClickListener(v -> {
            Intent intent = new Intent(CreateAccount.this, SignIn.class);
            startActivity(intent);
        });
    }

    private void validateInputs() {
        HashMap<String, String> inputData = new HashMap<>();
        inputData.put("userKey", userIDInput.getText().toString().trim());
        inputData.put("email", emailAddressInput.getText().toString().trim());
        inputData.put("phoneNum", phoneNumberInput.getText().toString().trim());
        inputData.put("name", nameInput.getText().toString().trim());

        if (TextUtils.isEmpty(inputData.get("userKey"))) {
            userIDInput.setError("User ID is required");
            userIDInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(inputData.get("email"))) {
            emailAddressInput.setError("Email is required");
            emailAddressInput.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(Objects.requireNonNull(inputData.get("email"))).matches()) {
            emailAddressInput.setError("Invalid email address");
            emailAddressInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(inputData.get("phoneNum"))) {
            phoneNumberInput.setError("Phone number is required");
            phoneNumberInput.requestFocus();
            return;
        }

        if (!TextUtils.isDigitsOnly(inputData.get("phoneNum")) || Objects.requireNonNull(inputData.get("phoneNum")).length() < 10) {
            phoneNumberInput.setError("Invalid phone number");
            phoneNumberInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(inputData.get("name"))) {
            nameInput.setError("Full name is required");
            nameInput.requestFocus();
            return;
        }

        Toast.makeText(this, "All inputs are valid", Toast.LENGTH_SHORT).show();

        checkForDuplicateEmail(inputData);
    }

    private void checkForDuplicateEmail(HashMap<String, String> inputData) {

        BsaSdk.getInstance().getSdkService().isDuplicatedEmailOrPhoneNumber(java.util.Map.of("verifyType", SdkConstant.OtpType.EMAIL.getValue(), "verifyData", Objects.requireNonNull(inputData.get("email"))), new SdkResponseCallback<>() {
            @Override
            public void onSuccess(CheckDuplicateEmailOrPhoneNumberReponse result) {
                if (result != null && result.getRtCode() == 0) {
                    Log.d(TAG, "Email is not duplicated.");
                    requestEmailVerification(inputData);
                } else if (result != null) {
                    if (result.getRtCode() == 2019) {
                        showAlertDialog("Email already registered", "The email you entered is already registered. Please use a different email.");
                    } else {
                        Log.d(TAG, "Email check failed with code: " + result.getRtCode());
                    }
                }
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                if (errorResult != null) {
                    Log.d(TAG, "Email check failed with error: " + errorResult.getErrorMessage());
                    showAlertDialog("Error", "Email already used. Please try another one.");
                }
            }
        });
    }

    private void requestEmailVerification(HashMap<String, String> inputData) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("clientKey", BuildConfig.CLIENT_KEY);
        params.put("email", inputData.get("email"));

        BsaSdk.getInstance().getSdkService().sendOtpByEmail(params, new SdkResponseCallback<>() {
            @Override
            public void onSuccess(SendOtpResponse result) {
                if (result != null && result.getRtCode() == 0) {
                    Log.d(TAG, "OTP sent to email.");
                    Toast.makeText(getApplicationContext(), "OTP sent to email", Toast.LENGTH_SHORT).show();

                    Intent createAccountIntent = new Intent(CreateAccount.this, VerifyOTP.class);
                    createAccountIntent.putExtra("from", "createAccount");
                    createAccountIntent.putExtra("inputData", inputData);
                    startActivity(createAccountIntent);
                    finish();
                } else {
                    assert result != null;
                    Log.d(TAG, "Failed to send OTP, code: " + result.getRtCode());
                    showAlertDialog("Error", "Failed to send OTP. Please try again.");
                }
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                if (errorResult != null) {
                    Log.d(TAG, "Failed to send OTP, error: " + errorResult.getErrorMessage());
                    showAlertDialog("Error", "Failed to send OTP. Please try again.");
                }
            }
        });
    }

    private void showAlertDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .setIcon(R.drawable.ic_baseline_question_answer_24)
                .show();
    }
}