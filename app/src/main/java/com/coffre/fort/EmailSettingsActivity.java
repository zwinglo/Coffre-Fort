package com.coffre.fort;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class EmailSettingsActivity extends AppCompatActivity {

    private TextInputEditText hostEditText;
    private TextInputEditText portEditText;
    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText recipientEditText;
    private SwitchMaterial tlsSwitch;
    private Button saveButton;

    private EmailConfigManager configManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_settings);

        configManager = new EmailConfigManager(this);

        hostEditText = findViewById(R.id.smtpHostEditText);
        portEditText = findViewById(R.id.smtpPortEditText);
        usernameEditText = findViewById(R.id.smtpUsernameEditText);
        passwordEditText = findViewById(R.id.smtpPasswordEditText);
        recipientEditText = findViewById(R.id.recipientEditText);
        tlsSwitch = findViewById(R.id.tlsSwitch);
        saveButton = findViewById(R.id.saveEmailSettingsButton);

        populateFields();

        saveButton.setOnClickListener(v -> saveConfiguration());
    }

    private void populateFields() {
        hostEditText.setText(configManager.getHost());
        portEditText.setText(String.valueOf(configManager.getPort()));
        usernameEditText.setText(configManager.getUsername());
        passwordEditText.setText(configManager.getPassword());
        recipientEditText.setText(configManager.getRecipient());
        tlsSwitch.setChecked(configManager.isUseTls());
    }

    private void saveConfiguration() {
        String host = getTrimmedText(hostEditText);
        String portText = getTrimmedText(portEditText);
        String username = getTrimmedText(usernameEditText);
        String password = getTrimmedText(passwordEditText);
        String recipient = getTrimmedText(recipientEditText);
        boolean useTls = tlsSwitch.isChecked();

        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(portText)
                || TextUtils.isEmpty(username) || TextUtils.isEmpty(password)
                || TextUtils.isEmpty(recipient)) {
            Toast.makeText(this, R.string.email_settings_required, Toast.LENGTH_SHORT).show();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.invalid_port_number, Toast.LENGTH_SHORT).show();
            return;
        }

        configManager.saveConfiguration(host, port, username, password, recipient, useTls);
        Toast.makeText(this, R.string.email_settings_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getTrimmedText(TextInputEditText editText) {
        return editText.getText() == null ? null : editText.getText().toString().trim();
    }
}
