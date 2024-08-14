package com.shopwallet.ituchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * OnBoarding activity that provides a tutorial or introductory screen
 * to guide users through the features of the app using a ViewPager2
 * and TabLayout for swiping through different onBoarding screens.
 */
public class OnBoarding extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);

        // Initialize the ViewPager2, TabLayout, and Skip button
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        Button btnSkip = findViewById(R.id.btnSkip);

        // Create an instance of OnBoardingAdapter to manage onBoarding fragments
        OnBoardingAdapter adapter = new OnBoardingAdapter(this);

        // Add onBoarding fragments to the adapter
        adapter.addFragment(OnBoardingFragment.newInstance("Welcome to the future!",
                "Your financial freedom is at your fingertips.", R.drawable.welcome_page));
        adapter.addFragment(OnBoardingFragment.newInstance("Wallet Account",
                "You can easily create and sign in to your wallet in one go.", R.drawable.create_signin_account));
        adapter.addFragment(OnBoardingFragment.newInstance("Wallet Dashboard",
                "Carry out all your transactions from a single view.", R.drawable.dashboard_view));
        adapter.addFragment(OnBoardingFragment.newInstance("Wallet Top Up",
                "Credit your wallet from your bank account.", R.drawable.wallet_top_up));
        adapter.addFragment(OnBoardingFragment.newInstance("Send Funds",
                "Wallet to Wallet Transfer. Send money easily to others", R.drawable.send));
        adapter.addFragment(OnBoardingFragment.newInstance("Receive Funds",
                "Receive money seamlessly from other.", R.drawable.receive));
        adapter.addFragment(OnBoardingFragment.newInstance("Request Payment",
                "Request to be paid specific amount.", R.drawable.request_amount));
        adapter.addFragment(OnBoardingFragment.newInstance("Credit your Mobile",
                "Reload your mobile phone credit from your wallet", R.drawable.reload_mobile));
        adapter.addFragment(OnBoardingFragment.newInstance("Wallet Balance",
                "Track your funds from a single view.", R.drawable.balance));
        adapter.addFragment(OnBoardingFragment.newInstance("Secure and Safe",
                "Password-less blockchain based authentication.", R.drawable.passwordless));
        adapter.addFragment(OnBoardingFragment.newInstance("Secure Transactions",
                "Carry out all your transactions securely.", R.drawable.transaction_auth));

        // Set the adapter to the ViewPager2
        viewPager.setAdapter(adapter);

        // Attach the TabLayout to the ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {}).attach();

        // Set a click listener on the Skip button to mark onBoarding as completed
        // and navigate to the MainActivity
        btnSkip.setOnClickListener(v -> {
            // Mark onBoarding as completed in SharedPreferences
            SharedPreferences preferences = getSharedPreferences("OnBoarding", MODE_PRIVATE);
            preferences.edit().putBoolean("Completed", true).apply();

            // Start the MainActivity and finish the OnBoarding activity
            startActivity(new Intent(OnBoarding.this, MainActivity.class));
            finish();
        });
    }
}