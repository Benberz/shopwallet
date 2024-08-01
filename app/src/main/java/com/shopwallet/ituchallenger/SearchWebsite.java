package com.shopwallet.ituchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SearchWebsite extends AppCompatActivity {

    private EditText searchInput;
    private RecyclerView searchResults;
    private Button linkButton;
    private List<Website> websites; // Define a Website model class to hold website data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_website);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.websiteSearchToolbar);
        setSupportActionBar(toolbar);

        searchInput = findViewById(R.id.searchInput);
        searchResults = findViewById(R.id.searchResults);
        linkButton = findViewById(R.id.linkButton);

        ImageButton searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> searchWebsites());

        ImageButton searchNowButton = findViewById(R.id.searchNowButton);
        searchNowButton.setOnClickListener(v -> searchWebsites());

        // Setup RecyclerView
        searchResults.setLayoutManager(new LinearLayoutManager(this));
        websites = new ArrayList<>();
        SearchResultAdapter adapter = new SearchResultAdapter(websites);
        searchResults.setAdapter(adapter);

    }

    private void searchWebsites() {
        String query = searchInput.getText().toString();
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mocked API call - Replace with actual API call using Retrofit or similar
        websites.clear();
        websites.add(new Website("ShopWallet"));
        websites.add(new Website("FNSPay"));
        websites.add(new Website("OtherWebsite"));
        searchResults.getAdapter().notifyDataSetChanged();
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