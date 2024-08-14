package com.shopwallet.ituchallenger;

import static com.shopwallet.ituchallenger.BuildConfig.CLIENT_KEY;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.fnsv.bsa.sdk.BsaSdk;
import com.fnsv.bsa.sdk.callback.SdkResponseCallback;
import com.fnsv.bsa.sdk.common.SdkConstant;
import com.fnsv.bsa.sdk.response.ErrorResult;
import com.fnsv.bsa.sdk.response.SendOtpResponse;

import java.util.HashMap;

/**
 * SignIn activity that handles user authentication via OTP (One-Time Password).
 * It collects user input, validates the data, and interacts with the BSA SDK to send an OTP for verification.
 */
public class SignIn extends AppCompatActivity {

    private static final String TAG = "SignInActivity";

    private EditText userInput;
    private EditText nameInput;
    private EditText emailInput;
    private Button nextRegisterDeviceButton;
    private ProgressBar signInProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize input fields
        userInput = findViewById(R.id.phoneNumberReloadInput);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailAddressInput);
        signInProgressBar = findViewById(R.id.signInProgressBar);

        // Hide the progress bar initially
        signInProgressBar.setVisibility(View.GONE);

        // Set up the "Next" button to initiate OTP sending
        nextRegisterDeviceButton = findViewById(R.id.signInNextButton);
        nextRegisterDeviceButton.setOnClickListener(view -> {
            nextRegisterDeviceButton.setEnabled(false); // Disable button to avoid double clicks
            if (validateInputs()) {
                signInProgressBar.setVisibility(View.VISIBLE);
                sendOtp();
            }
        });
    }

    /**
     * Validates user input fields and sets appropriate icons and error messages.
     *
     * @return true if all inputs are valid, false otherwise.
     */
    private boolean validateInputs() {
        boolean isValid = true;

        // Validate User ID (e.g., phone number or username)
        if (TextUtils.isEmpty(userInput.getText())) {
            userInput.setError("User ID cannot be empty");
            setIcon(userInput, R.drawable.ic_failure); // Replace with your failure icon
            isValid = false;
        } else {
            setIcon(userInput, R.drawable.ic_success); // Replace with your success icon
        }

        // Validate Name
        if (TextUtils.isEmpty(nameInput.getText())) {
            nameInput.setError("Name cannot be empty");
            setIcon(nameInput, R.drawable.ic_failure); // Replace with your failure icon
            isValid = false;
        } else {
            setIcon(nameInput, R.drawable.ic_success); // Replace with your success icon
        }

        // Validate Email
        if (TextUtils.isEmpty(emailInput.getText()) || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput.getText()).matches()) {
            emailInput.setError("Enter a valid email address");
            setIcon(emailInput, R.drawable.ic_failure); // Replace with your failure icon
            isValid = false;
        } else {
            setIcon(emailInput, R.drawable.ic_success); // Replace with your success icon
        }

        // Re-enable the Next button after validation
        nextRegisterDeviceButton.setEnabled(true);
        return isValid;
    }

    /**
     * Sets an icon on the right side of the EditText field to indicate success or failure.
     *
     * @param editText  The EditText to set the icon on.
     * @param iconResId The resource ID of the icon to display.
     */
    private void setIcon(EditText editText, int iconResId) {
        Drawable icon = ContextCompat.getDrawable(this, iconResId);
        if (icon != null) {
            icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
            editText.setCompoundDrawables(null, null, icon, null);
        }
    }

    /**
     * Sends an OTP using the BSA SDK. The OTP is sent to the user's email for verification.
     */
    private void sendOtp() {
        // Prepare parameters for the OTP request
        HashMap<String, Object> params = new HashMap<>();
        params.put("clientKey", CLIENT_KEY); // Replace with your actual client key
        params.put("userKey", userInput.getText().toString());
        params.put("name", nameInput.getText().toString());
        params.put("verifyType", SdkConstant.OtpType.EMAIL.getValue());
        params.put("verifyData", emailInput.getText().toString());

        // Send the OTP using the BSA SDK
        BsaSdk.getInstance().getSdkService().sendOtpByRegisterDevice(params, new SdkResponseCallback<>() {
            @Override
            public void onSuccess(SendOtpResponse result) {
                Log.d(TAG, "OTP sent successfully rtCode: " + result.rtCode + " | rtMsg: " + result.rtMsg);
                assert result.data != null;
                Log.d(TAG, "Result {Data} data.result: " + result.data.result + " | data.authType: " + result.data.authType);

                handleSuccess();
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                signInProgressBar.setVisibility(View.GONE);
                handleError(errorResult);
            }
        });
    }

    /**
     * Handles the success response after sending OTP. Navigates to the VerifyOTP activity.
     */
    private void handleSuccess() {
        Log.d(TAG, "OTP sent successfully");

        // Hide the progress bar
        signInProgressBar.setVisibility(View.GONE);

        // Prepare data to pass to the next activity
        HashMap<String, Object> inputData = new HashMap<>();
        inputData.put("userKey", userInput.getText().toString().trim());
        inputData.put("email", emailInput.getText().toString().trim());
        inputData.put("name", nameInput.getText().toString().trim());

        // Start the VerifyOTP activity and pass the input data
        Intent registerDeviceIntent = new Intent(SignIn.this, VerifyOTP.class);
        registerDeviceIntent.putExtra("from", "signIn"); // String data
        registerDeviceIntent.putExtra("inputData", inputData);
        startActivity(registerDeviceIntent);
        finish(); // Finish the current activity
    }

    /**
     * Handles errors that occur during the OTP sending process.
     * Displays appropriate error messages to the user based on the error code.
     *
     * @param errorResult The error result object containing the error code and message.
     */
    private void handleError(ErrorResult errorResult) {
        String errorMessage;

        // Determine the error message based on the error code
        switch (errorResult.getErrorCode()) {
            case 2000:
                errorMessage = "Invalid client key. Please check your client key and try again.";
                break;
            case 2004:
                errorMessage = "Channel does not exist. Check the URL used for initialization. If it happens constantly, please inquire with the person in charge.";
                break;
            case 2008:
                errorMessage = "Unregistered user. Please check your BSA sign-in status.";
                break;
            case 2010:
                errorMessage = "User authentication in-progress. Depending on the circumstances, cancel previous authentication and request a new one.";
                break;
            case 2100:
                errorMessage = "Token is expired. Please retry to authenticate.";
                break;
            case 2101:
                errorMessage = "Token's signature is invalid. Cancel previous authentication and request a new one.";
                break;
            case 2102:
                errorMessage = "Can't access with this token. Cancel previous authentication and request a new one.";
                break;
            case 2103:
                errorMessage = "Unexpected token error. Cancel previous authentication and request a new one.";
                break;
            case 2105:
                errorMessage = "Expired token. Please retry to authenticate.";
                break;
            case 2106:
                errorMessage = "Unmatched signature token. Please retry to authenticate.";
                break;
            case 2107:
                errorMessage = "Token does not exist. Please retry to authenticate.";
                break;
            case 5001:
                errorMessage = "Authentication timeout. Please authenticate again.";
                break;
            case 5005:
                errorMessage = "Unauthorized user. Please contact support.";
                break;
            case 5006:
                errorMessage = "Permanently suspended user. Please contact support.";
                break;
            case 5007:
                errorMessage = "An unknown error occurred. Please contact support.";
                break;
            case 5010:
                errorMessage = "Authentication failure. Please contact the person in charge to solve this matter.";
                break;
            case 5011:
                errorMessage = "User authentication canceled. Please contact the person in charge to solve this matter.";
                break;
            case 5015:
                errorMessage = "Failed to create channel. It can occur when the parameters are not enough. If it happens constantly, please inquire with the person in charge.";
                break;
            case 5017:
                errorMessage = "Failed to send push notification. Problems with FCM (Firebase Cloud Messaging) etc. Also check whether the updated token is correct.";
                break;
            case 5022:
                errorMessage = "Verification failure. Node verification failed. If it happens constantly, please inquire with the person in charge.";
                break;
            default:
                errorMessage = "OTP sending failed: " + errorResult.getErrorMessage();
                break;
        }

        Log.e(TAG, errorMessage);

        // Show error dialog on the UI thread
        runOnUiThread(() -> showErrorDialog(errorMessage));
    }

    /**
     * Displays an error dialog with the given error message.
     *
     * @param errorMessage The error message to display.
     */
    private void showErrorDialog(String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(errorMessage)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}