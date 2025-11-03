package com.coffre.fort;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText passwordEditText;
    private Button loginButton;
    private Button createPasswordButton;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        databaseHelper = new DatabaseHelper(this);

        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        createPasswordButton = findViewById(R.id.createPasswordButton);

        updateUI();

        loginButton.setOnClickListener(v -> login());
        createPasswordButton.setOnClickListener(v -> createPassword());
    }

    private void updateUI() {
        if (databaseHelper.hasPassword()) {
            loginButton.setEnabled(true);
            createPasswordButton.setEnabled(false);
        } else {
            loginButton.setEnabled(false);
            createPasswordButton.setEnabled(true);
        }
    }

    private void login() {
        String enteredPassword = passwordEditText.getText().toString();
        
        if (TextUtils.isEmpty(enteredPassword)) {
            Toast.makeText(this, R.string.empty_password, Toast.LENGTH_SHORT).show();
            return;
        }

        String savedPassword = databaseHelper.getPassword();
        if (enteredPassword.equals(savedPassword)) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, R.string.invalid_password, Toast.LENGTH_SHORT).show();
            passwordEditText.setText("");
        }
    }

    private void createPassword() {
        String newPassword = passwordEditText.getText().toString();
        
        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, R.string.empty_password, Toast.LENGTH_SHORT).show();
            return;
        }

        databaseHelper.setPassword(newPassword);
        Toast.makeText(this, R.string.password_created, Toast.LENGTH_SHORT).show();
        updateUI();
    }
}
