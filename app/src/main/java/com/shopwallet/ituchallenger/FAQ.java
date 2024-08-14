package com.shopwallet.ituchallenger;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ExpandableListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * FAQ Activity that displays frequently asked questions in an expandable list view.
 * It initializes the toolbar, sets up the expandable list view, and prepares the FAQ data.
 */
public class FAQ extends AppCompatActivity {

    private ExpandableListView expandableListView;
    private ExpandableListAdapter listAdapter;
    private List<String> listDataHeader; // List of FAQ headers
    private HashMap<String, List<String>> listDataChild; // Mapping of each header to its list of FAQ answers

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        // Set up the toolbar for the FAQ activity
        Toolbar toolbar = findViewById(R.id.faqToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.faq_text);
        }

        // Initialize the ExpandableListView
        expandableListView = findViewById(R.id.faqExpandableList);

        // Prepare the FAQ data
        prepareListData();

        // Set up the adapter for the ExpandableListView
        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);
        expandableListView.setAdapter(listAdapter);
    }

    /**
     * Prepares the FAQ data for display in the expandable list view.
     * Populates the list headers and child data for each header.
     */
    private void prepareListData() {
        listDataHeader = new ArrayList<>();
        listDataChild = new HashMap<>();

        // Add FAQ headers
        listDataHeader.add("I do not receive verification notification");
        listDataHeader.add("I changed my cellphone");
        listDataHeader.add("I lost my cellphone");
        listDataHeader.add("I accidentally deleted the application");

        // Add FAQ answers for each header
        List<String> notification = new ArrayList<>();
        notification.add("Follow steps below:\n" +
                "1. Press [Resend] button on the website or application.\n" +
                "2. Log in again.\n" +
                "3. Terminate ShopWallet and try again.");

        List<String> changedPhone = new ArrayList<>();
        changedPhone.add("Steps for changing phone.");

        List<String> lostPhone = new ArrayList<>();
        lostPhone.add("Steps for lost phone.");

        List<String> deletedApp = new ArrayList<>();
        deletedApp.add("Steps for accidentally deleted application.");

        // Map headers to their corresponding answers
        listDataChild.put(listDataHeader.get(0), notification);
        listDataChild.put(listDataHeader.get(1), changedPhone);
        listDataChild.put(listDataHeader.get(2), lostPhone);
        listDataChild.put(listDataHeader.get(3), deletedApp);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle toolbar item clicks
        if (item.getItemId() == android.R.id.home) {
            // Handle the back button click
            finish(); // Navigate back to the previous activity (e.g., MainActivity)
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}