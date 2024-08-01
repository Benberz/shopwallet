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

    private static final int PROGRESS_ANIMATION_DURATION = 3000; // Duration for the progress bar animation in ms

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_completed);

        ProgressBar progressBar = findViewById(R.id.registerCompleteProgressBar);
        GifImageView successGif = findViewById(R.id.successGifImageView);
        // set the Sign In button functionality
        Button closeButton = findViewById(R.id.closeButton);

        // Animate the progress bar
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
        progressAnimator.setDuration(PROGRESS_ANIMATION_DURATION);
        progressAnimator.start();

        // Delay showing the success GIF until after the progress animation completes
        new Handler().postDelayed(() -> {
            progressBar.setVisibility(ProgressBar.GONE);
            successGif.setVisibility(GifImageView.VISIBLE);

            // Show the success message
            Toast.makeText(RegistrationCompleted.this, "Registration Successful", Toast.LENGTH_SHORT).show();

            // Enable the close button after the success animation
            closeButton.setEnabled(true);
        }, PROGRESS_ANIMATION_DURATION);

        // Disable the close button until the animation completes
        closeButton.setEnabled(false);

        closeButton.setOnClickListener(view -> {
            // start the sign in activity
            shouldHandleBackPress();
        });

        // Add a callback for handling the back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Custom back press handling logic
                shouldHandleBackPress();
            }
        });
    }

    private void shouldHandleBackPress() {
        // start the sign in activity
        Intent signInActivity = new Intent(RegistrationCompleted.this, SignIn.class);
        startActivity(signInActivity);
        finish();
    }
}