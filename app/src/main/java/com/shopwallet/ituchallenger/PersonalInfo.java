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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        HashMap<String, Object> storedInputData = SecureStorageUtil.retrieveDataFromKeystore(this, "inputData");

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.personalInfoToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Personal Information");
        }

        // setup profile info
        TextView userIdTextView = findViewById(R.id.usernameValue);
        TextView fullNameTextView =  findViewById(R.id.nameValue);
        TextView emailTextView =  findViewById(R.id.emailValue);
        // set info in text views
        userIdTextView.setText(Objects.requireNonNull(storedInputData.get("userKey")).toString());
        fullNameTextView.setText(Objects.requireNonNull(storedInputData.get("name")).toString());
        emailTextView.setText(Objects.requireNonNull(storedInputData.get("email")).toString());

        // set the Remove Device Button functionality
        Button removeDeviceButton = findViewById(R.id.removeDeviceButton);
        // set the Remove Device Button functionality
        removeDeviceButton.setOnClickListener(view -> confirmUnregisterDevice());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button click
            startActivity(new Intent(this, Profile.class)); // to navigate back to MainActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmUnregisterDevice() {
        // Optionally, start another activity or perform additional actions
        Intent signInActivity = new Intent(PersonalInfo.this, ConfirmRemoveDevice.class);
        startActivity(signInActivity);
    }
}