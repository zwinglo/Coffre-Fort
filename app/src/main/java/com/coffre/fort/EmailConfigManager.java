package com.coffre.fort;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class EmailConfigManager {
    private static final String PREFS_NAME = "email_config";
    private static final String KEY_HOST = "host";
    private static final String KEY_PORT = "port";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_RECIPIENT = "recipient";
    private static final String KEY_USE_TLS = "use_tls";

    private final SharedPreferences preferences;

    public EmailConfigManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveConfiguration(String host, int port, String username, String password, String recipient, boolean useTls) {
        preferences.edit()
                .putString(KEY_HOST, host)
                .putInt(KEY_PORT, port)
                .putString(KEY_USERNAME, username)
                .putString(KEY_PASSWORD, password)
                .putString(KEY_RECIPIENT, recipient)
                .putBoolean(KEY_USE_TLS, useTls)
                .apply();
    }

    public String getHost() {
        return preferences.getString(KEY_HOST, null);
    }

    public int getPort() {
        return preferences.getInt(KEY_PORT, 587);
    }

    public String getUsername() {
        return preferences.getString(KEY_USERNAME, null);
    }

    public String getPassword() {
        return preferences.getString(KEY_PASSWORD, null);
    }

    public String getRecipient() {
        return preferences.getString(KEY_RECIPIENT, null);
    }

    public boolean isUseTls() {
        return preferences.getBoolean(KEY_USE_TLS, true);
    }

    public boolean isConfigured() {
        return !TextUtils.isEmpty(getHost())
                && !TextUtils.isEmpty(getUsername())
                && !TextUtils.isEmpty(getPassword())
                && !TextUtils.isEmpty(getRecipient());
    }
}
