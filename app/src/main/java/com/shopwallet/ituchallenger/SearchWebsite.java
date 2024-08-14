package com.shopwallet.ituchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Activity class for searching websites and displaying the search results in a RecyclerView.
 */
public class SearchWebsite extends AppCompatActivity {

    private EditText searchInput; // Input field for entering search queries
    private RecyclerView searchResults; // RecyclerView for displaying search results
    private List<Website> websites; // List to hold website data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_website);

        // Set up the toolbar with the appropriate title and back button
        Toolbar toolbar = findViewById(R.id.websiteSearchToolbar);
        setSupportActionBar(toolbar);

        // Initialize views
        searchInput = findViewById(R.id.searchInput);
        searchResults = findViewById(R.id.searchResults);
        Button linkButton = findViewById(R.id.linkButton);

        ImageButton searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> searchWebsites());

        ImageButton searchNowButton = findViewById(R.id.searchNowButton);
        searchNowButton.setOnClickListener(v -> searchWebsites());

        // Set up RecyclerView with a LinearLayoutManager and adapter
        searchResults.setLayoutManager(new LinearLayoutManager(this));
        websites = new ArrayList<>();
        SearchResultAdapter adapter = new SearchResultAdapter(websites);
        searchResults.setAdapter(adapter);
    }

    /**
     * Handles searching of websites based on the query entered by the user.
     * Displays mock results for demonstration purposes.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void searchWebsites() {
        String query = searchInput.getText().toString();
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mocked API call - Replace this with actual API call using Retrofit or similar
        websites.clear();
        websites.add(new Website("ShopWallet"));
        websites.add(new Website("FNSPay"));
        websites.add(new Website("OtherWebsite"));

        // Notify the adapter that the dataset has changed
        Objects.requireNonNull(searchResults.getAdapter()).notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle toolbar item selections
        if (item.getItemId() == android.R.id.home) {
            // Navigate back to the previous activity
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}