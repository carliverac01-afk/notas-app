package com.example.notesandroid.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class SessionManager {
    private static final String PREFS = "session";
    private static final String TOKEN = "token";
    private static final String USER_NAME = "user_name";
    private static final String USER_EMAIL = "user_email";
    private static final String LAST_SYNC = "last_sync";
    private static final String INITIAL_SYNC_DATE = "2000-01-01T00:00:00Z";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            preferences = EncryptedSharedPreferences.create(
                    context,
                    PREFS,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo inicializar el almacenamiento seguro", exception);
        }
    }

    public void saveSession(String token, String name, String email) {
        preferences.edit()
                .putString(TOKEN, token)
                .putString(USER_NAME, name)
                .putString(USER_EMAIL, email)
                .apply();
    }

    public String getToken() {
        return preferences.getString(TOKEN, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public UserSession getUserSession() {
        return new UserSession(
                preferences.getString(USER_NAME, ""),
                preferences.getString(USER_EMAIL, "")
        );
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
