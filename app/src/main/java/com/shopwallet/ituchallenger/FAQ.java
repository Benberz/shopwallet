package com.shopwallet.ituchallenger;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ExpandableListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FAQ extends AppCompatActivity {

    ExpandableListView expandableListView;
    ExpandableListAdapter listAdapter;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        // Set up the toolbar for FAQ
        Toolbar toolbar = findViewById(R.id.faqToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.faq_text);
        }

        expandableListView = findViewById(R.id.faqExpandableList);

        // Preparing list data
        prepareListData();

        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);
        expandableListView.setAdapter(listAdapter);
    }

    /*
     * Preparing the list data
     */
    private void prepareListData() {
        listDataHeader = new ArrayList<>();
        listDataChild = new HashMap<>();

        // Adding child data
        listDataHeader.add("I do not receive verification notification");
        listDataHeader.add("I changed my cellphone");
        listDataHeader.add("I lost my cellphone");
        listDataHeader.add("I accidentally deleted the application");

        // Adding child data
        List<String> notification = new ArrayList<>();
        notification.add("Follow steps below\n" +
                "1. Press [Resend] button on the website or application.\n" +
                "2. Log in again.\n" +
                "3. Terminate ShopWallet and try again.");

        List<String> changedPhone = new ArrayList<>();
        changedPhone.add("Steps for changing phone.");

        List<String> lostPhone = new ArrayList<>();
        lostPhone.add("Steps for lost phone.");

        List<String> deletedApp = new ArrayList<>();
        deletedApp.add("Steps for accidentally deleted application.");

        listDataChild.put(listDataHeader.get(0), notification); // Header, Child data
        listDataChild.put(listDataHeader.get(1), changedPhone);
        listDataChild.put(listDataHeader.get(2), lostPhone);
        listDataChild.put(listDataHeader.get(3), deletedApp);
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