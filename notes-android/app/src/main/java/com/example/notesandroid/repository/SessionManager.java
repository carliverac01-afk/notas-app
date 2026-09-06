package com.example.notesandroid.repository;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREFS = "session";
    private static final String TOKEN = "token";
    private static final String LAST_SYNC = "last_sync";
    private static final String INITIAL_SYNC_DATE = "2000-01-01T00:00:00Z";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        preferences.edit().putString(TOKEN, token).apply();
    }

    public String getToken() {
        return preferences.getString(TOKEN, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public String getAuthorizationHeader() {
        return "Bearer " + getToken();
    }

    public String getLastSync() {
        return preferences.getString(LAST_SYNC, INITIAL_SYNC_DATE);
    }

    public void saveLastSync(String instant) {
        preferences.edit().putString(LAST_SYNC, instant).apply();
    }

    public void logout() {
        preferences.edit().clear().apply();
    }
}
