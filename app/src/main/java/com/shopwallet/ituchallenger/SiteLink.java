package com.shopwallet.ituchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;

/**
 * SiteLink activity that displays a toolbar with a site link title and a search icon.
 * The search icon directs the user to a search functionality when clicked.
 */
public class SiteLink extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_link);

        // Set up the toolbar for Site Link
        Toolbar toolbar = findViewById(R.id.siteLinkToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            // Enable the back button on the toolbar
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            // Set the title of the toolbar to "Site Link"
            getSupportActionBar().setTitle(R.string.site_link);
        }

        // Set up the search icon button
        ImageButton searchButton = findViewById(R.id.searchWebsiteSiteButton);
        searchButton.setOnClickListener(v -> {
            // Launch the SearchWebsite activity when the search icon is clicked
            Intent intent = new Intent(SiteLink.this, SearchWebsite.class);
            startActivity(intent);
        });
    }

    /**
     * Handles item selection from the options menu.
     * In this case, it handles the action when the back button is clicked.
     *
     * @param item The menu item that was selected.
     * @return true if the item was handled, false otherwise.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Finish the current activity to navigate back to the previous one (e.g., MainActivity)
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}