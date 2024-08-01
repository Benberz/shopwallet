package com.shopwallet.ituchallenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.fnsv.bsa.sdk.BsaSdk;
import com.fnsv.bsa.sdk.callback.SdkAuthResponseCallback;
import com.fnsv.bsa.sdk.callback.SdkResponseCallback;
import com.fnsv.bsa.sdk.response.AuthHistoryResponse;
import com.fnsv.bsa.sdk.response.AuthResultResponse;
import com.fnsv.bsa.sdk.response.ErrorResult;
import com.shopwallet.ituchallenger.util.SecureStorageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AuthenticationHistory extends AppCompatActivity {

    private static final String TAG = "AuthenticationHistory";

    protected RecyclerView recyclerView;
    protected AuthHistoryAdapter adapter;
    protected List<AuthHistory> authHistoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication_history);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.authHistoryToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.authentication_history_text);
        }

        recyclerView = findViewById(R.id.authHistoryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        authHistoryList = new ArrayList<>();
        adapter = new AuthHistoryAdapter(this, authHistoryList);
        recyclerView.setAdapter(adapter);

        loadAuthHistoryData();
    }

    private void loadAuthHistoryData() {
        HashMap<String, Object> inputData = SecureStorageUtil.retrieveDataFromKeystore(this, "inputData");
        String userKey = (String) inputData.get("userKey");
        Log.d(TAG, " userKey: " + userKey);

        // Show a dialog while processing
        runOnUiThread(() -> {
            @SuppressLint("InflateParams") AlertDialog progressDialog = new AlertDialog.Builder(this)
                    .setView(LayoutInflater.from(this).inflate(R.layout.dialog_progress, null))
                    .setCancelable(false)
                    .create();
            progressDialog.show();


            BsaSdk.getInstance().getSdkService().appAuthenticator(userKey, true, this, new SdkAuthResponseCallback<>() {
                @Override
                public void onSuccess(AuthResultResponse result) {
                    Log.d(TAG, "App authentication successful: " + result.rtMsg + " | " + result.getRtCode());
                    progressDialog.dismiss(); // Dismiss the dialog when authentication is successful
                    getAuthHistoryData();
                }

                @Override
                public void onProcess(boolean isProcessing, String processMessage) {
                    Log.d(TAG, "Processing authentication: " + processMessage);
                    runOnUiThread(() -> Toast.makeText(AuthenticationHistory.this, "Processing authentication: " + processMessage, Toast.LENGTH_LONG).show());
                }

                @Override
                public void onFailed(ErrorResult errorResult) {
                    Log.e(TAG, "App authentication failed: " + errorResult.getErrorMessage());
                    runOnUiThread(() -> Toast.makeText(AuthenticationHistory.this, "Authentication failed: " + errorResult.getErrorMessage(), Toast.LENGTH_SHORT).show());
                    progressDialog.dismiss(); // Dismiss the dialog when authentication fails
                }
            });
        });
    }

    private void getAuthHistoryData() {
        BsaSdk.getInstance().getSdkService().getAuthHistory(1, 20, new SdkResponseCallback<>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSuccess(AuthHistoryResponse response) {
                if (response != null && response.getRtCode() == 0) {
                    authHistoryList.clear();
                    for (AuthHistoryResponse.data data : response.data) {
                        AuthHistory authHistory = new AuthHistory(data.platform, data.regDt, data.status);
                        authHistoryList.add(authHistory);
                    }
                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                }
            }

            @Override
            public void onFailed(ErrorResult errorResult) {
                Log.e(TAG, "Failed to load authentication history: " + errorResult.getErrorMessage());
                runOnUiThread(() -> Toast.makeText(AuthenticationHistory.this, "Failed to load authentication history: " + errorResult.getErrorMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button click
            startActivity(new Intent(this, Profile.class)); // to navigate back to Profile
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}