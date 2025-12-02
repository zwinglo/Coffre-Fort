package com.coffre.fort;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_SMS = 2002;
    private static final String[] MESSAGE_PERMISSIONS = new String[]{
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.RECEIVE_MMS,
            Manifest.permission.RECEIVE_WAP_PUSH,
            Manifest.permission.READ_SMS
    };

    private DatabaseHelper databaseHelper;
    private EmailConfigManager emailConfigManager;

    private TextInputEditText currentPasswordEditText;
    private TextInputEditText newPasswordEditText;
    private TextInputEditText confirmPasswordEditText;
    private TextView documentCountTextView;
    private TextView emailStatusTextView;
    private TextView permissionStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.settings_title);

        databaseHelper = new DatabaseHelper(this);
        emailConfigManager = new EmailConfigManager(this);

        currentPasswordEditText = findViewById(R.id.currentPasswordEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        documentCountTextView = findViewById(R.id.documentCountTextView);
        emailStatusTextView = findViewById(R.id.emailStatusTextView);
        permissionStatusTextView = findViewById(R.id.permissionStatusTextView);

        Button updatePasswordButton = findViewById(R.id.updatePasswordButton);
        Button logoutButton = findViewById(R.id.logoutButton);
        Button deleteAllButton = findViewById(R.id.deleteAllButton);
        Button emailSettingsButton = findViewById(R.id.emailSettingsButton);
        Button mailTestButton = findViewById(R.id.mailTestButton);
        Button requestPermissionButton = findViewById(R.id.requestPermissionButton);
        Button openSystemSettingsButton = findViewById(R.id.openSystemSettingsButton);

        updatePasswordButton.setOnClickListener(v -> changePassword());
        logoutButton.setOnClickListener(v -> logout());
        deleteAllButton.setOnClickListener(v -> confirmDeleteAll());
        emailSettingsButton.setOnClickListener(v -> openEmailSettings());
        mailTestButton.setOnClickListener(v -> sendTestEmail());
        requestPermissionButton.setOnClickListener(v -> requestCriticalPermissions());
        openSystemSettingsButton.setOnClickListener(v -> openSystemPermissions());

        refreshDocumentSummary();
        refreshEmailSummary();
        refreshPermissionSummary();
    }

    private void changePassword() {
        String currentPassword = getTrimmedText(currentPasswordEditText);
        String newPassword = getTrimmedText(newPasswordEditText);
        String confirmPassword = getTrimmedText(confirmPasswordEditText);

        if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, R.string.settings_password_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, R.string.settings_password_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!databaseHelper.verifyPassword(currentPassword)) {
            Toast.makeText(this, R.string.settings_password_invalid_current, Toast.LENGTH_SHORT).show();
            return;
        }

        databaseHelper.setPassword(newPassword);
        Toast.makeText(this, R.string.settings_password_updated, Toast.LENGTH_SHORT).show();
        clearPasswordFields();
    }

    private void logout() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void confirmDeleteAll() {
        int documentCount = databaseHelper.getDocumentCount();
        String message = getString(R.string.settings_delete_all_message, documentCount);

        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_delete_all_title)
                .setMessage(message)
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteAllDocuments())
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteAllDocuments() {
        int deleted = databaseHelper.deleteAllDocuments();
        refreshDocumentSummary();
        Toast.makeText(this, getString(R.string.settings_delete_all_success, deleted), Toast.LENGTH_SHORT).show();
    }

    private void openEmailSettings() {
        Intent intent = new Intent(this, EmailSettingsActivity.class);
        startActivity(intent);
    }

    private void sendTestEmail() {
        if (!emailConfigManager.isConfigured()) {
            Toast.makeText(this, R.string.email_test_missing_config, Toast.LENGTH_SHORT).show();
            return;
        }

        EmailSender.sendEmail(this,
                getString(R.string.email_test_subject),
                getString(R.string.email_test_body));
        Toast.makeText(this, R.string.email_test_started, Toast.LENGTH_SHORT).show();
    }

    private void refreshEmailSummary() {
        if (emailConfigManager.isConfigured()) {
            String tlsStatus = emailConfigManager.isUseTls()
                    ? getString(R.string.settings_tls_enabled)
                    : getString(R.string.settings_tls_disabled);
            String status = getString(R.string.settings_email_configured,
                    safeValue(emailConfigManager.getHost()),
                    safeValue(emailConfigManager.getRecipient()),
                    tlsStatus);
            emailStatusTextView.setText(status);
        } else {
            emailStatusTextView.setText(R.string.settings_email_missing);
        }
    }

    private void refreshDocumentSummary() {
        int count = databaseHelper.getDocumentCount();
        documentCountTextView.setText(getString(R.string.settings_document_count, count));
    }

    private void refreshPermissionSummary() {
        boolean smsGranted = areAllMessagePermissionsGranted();

        String smsStatus = smsGranted
                ? getString(R.string.settings_permission_granted)
                : getString(R.string.settings_permission_missing);

        String summary = getString(R.string.settings_permission_summary, smsStatus);
        permissionStatusTextView.setText(summary);
    }

    private void requestCriticalPermissions() {
        ActivityCompat.requestPermissions(this, MESSAGE_PERMISSIONS, REQUEST_PERMISSION_SMS);
    }

    private void openSystemPermissions() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_SMS) {
            refreshPermissionSummary();
            boolean granted = true;
            if (grantResults.length == 0) {
                granted = false;
            } else {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        granted = false;
                        break;
                    }
                }
            }
            if (granted) {
                Toast.makeText(this, R.string.settings_permission_granted_toast, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.settings_permission_denied_toast, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean areAllMessagePermissionsGranted() {
        for (String permission : MESSAGE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void clearPasswordFields() {
        currentPasswordEditText.setText("");
        newPasswordEditText.setText("");
        confirmPasswordEditText.setText("");
    }

    private String getTrimmedText(TextInputEditText editText) {
        if (editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private String safeValue(String value) {
        return TextUtils.isEmpty(value) ? getString(R.string.settings_placeholder_value) : value;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDocumentSummary();
        refreshEmailSummary();
        refreshPermissionSummary();
    }
}
