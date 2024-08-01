package com.shopwallet.ituchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

public class Settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        }

        CardView faqLink = findViewById(R.id.faqCardView);
        CardView siteLink = findViewById(R.id.siteLinkCardView);

        faqLink.setOnClickListener(v -> startActivity(new Intent(this, FAQ.class)));
        siteLink.setOnClickListener(v -> startActivity(new Intent(this, SiteLink.class)));

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button click
            startActivity(new Intent(this, Dashboard.class)); // to navigate back to MainActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}