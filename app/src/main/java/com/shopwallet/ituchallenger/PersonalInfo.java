package com.shopwallet.ituchallenger;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.shopwallet.ituchallenger.util.SecureStorageUtil;

import java.util.HashMap;
import java.util.Objects;

public class PersonalInfo extends AppCompatActivity {

    /**
     * Called when the activity is first created. This method sets up the user interface,
     * initializes the toolbar, retrieves and displays the user's personal information,
     * and configures the "Remove Device" button.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the most recent data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        // Retrieve stored user information from secure storage
        HashMap<String, Object> storedInputData = SecureStorageUtil.retrieveDataFromKeystore(this, "inputData");

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.personalInfoToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Personal Information");
        }

        // Set up TextViews to display user information
        TextView userIdTextView = findViewById(R.id.usernameValue);
        TextView fullNameTextView = findViewById(R.id.nameValue);
        TextView emailTextView = findViewById(R.id.emailValue);

        // Populate TextViews with the stored user information
        userIdTextView.setText(Objects.requireNonNull(storedInputData.get("userKey")).toString());
        fullNameTextView.setText(Objects.requireNonNull(storedInputData.get("name")).toString());
        emailTextView.setText(Objects.requireNonNull(storedInputData.get("email")).toString());

        // Set up the "Remove Device" button and its functionality
        Button removeDeviceButton = findViewById(R.id.removeDeviceButton);
        removeDeviceButton.setOnClickListener(view -> confirmUnregisterDevice());
    }

    /**
     * Handles the selection of options in the options menu.
     * Specifically handles the action when the back button is pressed in the toolbar.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to proceed,
     *                 true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button click
            startActivity(new Intent(this, Profile.class)); // Navigate back to the Profile activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Initiates the process to unregister the current device.
     * This method starts the `ConfirmRemoveDevice` activity, which will handle the removal confirmation.
     */
    private void confirmUnregisterDevice() {
        // Optionally, start another activity or perform additional actions
        Intent signInActivity = new Intent(PersonalInfo.this, ConfirmRemoveDevice.class);
        startActivity(signInActivity);
    }
}
