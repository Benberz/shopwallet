package com.shopwallet.ituchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;

public class SiteLink extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_link);

        // Set up the toolbar for Site Link
        Toolbar toolbar = findViewById(R.id.siteLinkToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.site_link);
        }

        // Add search icon click listener
        ImageButton searchButton = findViewById(R.id.searchWebsiteSiteButton);
        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(SiteLink.this, SearchWebsite.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button click
            finish(); // to navigate back to MainActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}