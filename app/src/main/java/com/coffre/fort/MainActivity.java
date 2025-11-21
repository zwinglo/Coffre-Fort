package com.coffre.fort;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DocumentAdapter.OnDocumentClickListener {

    private static final int REQUEST_SMS_PERMISSION = 1001;

    private RecyclerView documentsRecyclerView;
    private TextView emptyTextView;
    private Spinner categorySpinner;
    private FloatingActionButton addDocumentFab;

    private DatabaseHelper databaseHelper;
    private DocumentAdapter documentAdapter;
    private String[] categories;
    private String selectedCategory;
    private EmailConfigManager emailConfigManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);
        emailConfigManager = new EmailConfigManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        documentsRecyclerView = findViewById(R.id.documentsRecyclerView);
        emptyTextView = findViewById(R.id.emptyTextView);
        categorySpinner = findViewById(R.id.categorySpinner);
        addDocumentFab = findViewById(R.id.addDocumentFab);

        String[] baseCategories = getResources().getStringArray(R.array.document_categories);
        categories = new String[baseCategories.length + 1];
        categories[0] = getString(R.string.all_categories);
        System.arraycopy(baseCategories, 0, categories, 1, baseCategories.length);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(spinnerAdapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = position == 0 ? null : categories[position];
                loadDocuments();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        documentAdapter = new DocumentAdapter(this);
        documentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        documentsRecyclerView.setAdapter(documentAdapter);

        addDocumentFab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddDocumentActivity.class);
            startActivity(intent);
        });

        warnIfEmailConfigurationMissing();
        requestSmsPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDocuments();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_email_settings) {
            Intent intent = new Intent(this, EmailSettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadDocuments() {
        List<Document> documents;
        if (selectedCategory == null) {
            documents = databaseHelper.getAllDocuments();
        } else {
            documents = databaseHelper.getDocumentsByCategory(selectedCategory);
        }

        documentAdapter.setDocuments(documents);

        if (documents.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            documentsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyTextView.setVisibility(View.GONE);
            documentsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDocumentClick(Document document) {
        Intent intent = new Intent(this, DocumentDetailActivity.class);
        intent.putExtra("document_id", document.getId());
        startActivity(intent);
    }

    private void requestSmsPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECEIVE_SMS}, REQUEST_SMS_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                android.widget.Toast.makeText(this, R.string.sms_permission_denied, android.widget.Toast.LENGTH_LONG).show();
            }
        }
    }

    private void warnIfEmailConfigurationMissing() {
        if (emailConfigManager.isConfigured()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_email_settings)
                .setMessage(R.string.warning_incomplete_email_config)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
