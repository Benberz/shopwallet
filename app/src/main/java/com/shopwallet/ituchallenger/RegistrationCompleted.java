package com.shopwallet.ituchallenger;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import pl.droidsonroids.gif.GifImageView;

public class RegistrationCompleted extends AppCompatActivity {

    private static final int PROGRESS_ANIMATION_DURATION = 3000; // Duration for the progress bar animation in milliseconds

    /**
     * Called when the activity is starting. This is where most initialization should go:
     * setting the content view, initializing UI components, and setting up listeners.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *                           shut down then this Bundle contains the data it most recently
     *                           supplied in onSaveInstanceState(Bundle). Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_completed);

        // Initialize UI components
        ProgressBar progressBar = findViewById(R.id.registerCompleteProgressBar);
        GifImageView successGif = findViewById(R.id.successGifImageView);
        Button closeButton = findViewById(R.id.closeButton);

        // Animate the progress bar from 0% to 100%
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
        progressAnimator.setDuration(PROGRESS_ANIMATION_DURATION);
        progressAnimator.start();

        // Delay showing the success GIF and enabling the close button until after the progress animation completes
        new Handler().postDelayed(() -> {
            // Hide the progress bar and show the success GIF
            progressBar.setVisibility(ProgressBar.GONE);
            successGif.setVisibility(GifImageView.VISIBLE);

            // Show a toast message indicating successful registration
            Toast.makeText(RegistrationCompleted.this, "Registration Successful", Toast.LENGTH_SHORT).show();

            // Enable the close button after the success animation completes
            closeButton.setEnabled(true);
        }, PROGRESS_ANIMATION_DURATION);

        // Initially disable the close button until the animation completes
        closeButton.setEnabled(false);

        // Set up the close button to handle the click event
        closeButton.setOnClickListener(view -> {
            // Handle the back press which will navigate to the sign-in activity
            shouldHandleBackPress();
        });

        // Add a custom callback for handling the back button press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Custom back press handling logic
                shouldHandleBackPress();
            }
        });
    }

    /**
     * Handles the navigation to the Sign In activity and finishes the current activity.
     * This method is called when the back button is pressed or the close button is clicked.
     */
    private void shouldHandleBackPress() {
        // Start the Sign In activity and finish the current one
        Intent signInActivity = new Intent(RegistrationCompleted.this, SignIn.class);
        startActivity(signInActivity);
        finish();
    }
}
