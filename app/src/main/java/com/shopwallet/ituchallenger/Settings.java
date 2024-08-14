package com.shopwallet.ituchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

/**
 * Activity class for managing user settings.
 * Provides links to FAQs and the website, and sets up the toolbar with navigation options.
 */
public class Settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Set up the toolbar with navigation and title
        Toolbar toolbar = findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);

        // Configure toolbar to show the back button and title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        }

        // Initialize CardViews for FAQ and Site Link
        CardView faqLink = findViewById(R.id.faqCardView);
        CardView siteLink = findViewById(R.id.siteLinkCardView);

        // Set onClick listeners for the CardViews to navigate to respective activities
        faqLink.setOnClickListener(v -> startActivity(new Intent(this, FAQ.class)));
        siteLink.setOnClickListener(v -> startActivity(new Intent(this, SiteLink.class)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle toolbar item selections
        if (item.getItemId() == android.R.id.home) {
            // Navigate back to the Dashboard activity
            startActivity(new Intent(this, Dashboard.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}