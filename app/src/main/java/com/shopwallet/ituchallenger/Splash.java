package com.shopwallet.ituchallenger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Splash activity that shows a splash screen with animations when the app is launched.
 * It checks if the user has completed on-boarding and navigates to the appropriate screen.
 */
public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize the logo ImageView and welcome TextView
        ImageView logoImageView = findViewById(R.id.logoImageView);
        TextView welcomeTextView = findViewById(R.id.welcomeTextView);

        // Load animations from XML resources
        Animation scaleFadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_fade_in);
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        // Start the logo animation
        logoImageView.setVisibility(View.VISIBLE);
        logoImageView.startAnimation(scaleFadeInAnimation);

        // Set an animation listener to start the text animation after the logo animation completes
        scaleFadeInAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // No action needed at the start of the animation
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Make the welcome text visible and start its fade-in animation
                welcomeTextView.setVisibility(View.VISIBLE);
                welcomeTextView.startAnimation(fadeInAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // No action needed on animation repeat
            }
        });

        // Set an animation listener for the fade-in animation to navigate after it ends
        fadeInAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // No action needed at the start of the animation
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Check if the on-boarding process has been completed
                SharedPreferences preferences = getSharedPreferences("OnBoarding", MODE_PRIVATE);
                boolean isOnBoardingCompleted = preferences.getBoolean("Completed", false);

                if (isOnBoardingCompleted) {
                    // Navigate to the Main Activity if on-boarding is completed
                    startActivity(new Intent(Splash.this, MainActivity.class));
                } else {
                    // Navigate to the OnBoarding Activity if on-boarding is not completed
                    startActivity(new Intent(Splash.this, OnBoarding.class));
                }
                // Finish the Splash activity so it cannot be returned to
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // No action needed on animation repeat
            }
        });
    }
}
