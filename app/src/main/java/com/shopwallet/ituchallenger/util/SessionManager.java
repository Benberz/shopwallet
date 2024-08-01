package com.shopwallet.ituchallenger.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;

public class SessionManager {

    private static final String PREF_NAME = "ShopWalletSession";
    private static final String IS_LOGIN = "IsLoggedIn";
    private static final String KEY_USER_DATA = "UserData";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;
    private static SessionManager instance;

    private SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void createSession(HashMap<String, Object> userData) {
        editor.putBoolean(IS_LOGIN, true);
        editor.putString(KEY_USER_DATA, userData.toString());
        editor.apply();
    }

    public HashMap<String, Object> getUserData() {
        HashMap<String, Object> userData = new HashMap<>();
        String userDataString = prefs.getString(KEY_USER_DATA, null);
        if (userDataString != null) {
            // Convert string back to HashMap (you might need a proper parser)
            // This is a simple example
            userData.put("data", userDataString);
        }
        return userData;
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(IS_LOGIN, false);
    }
}