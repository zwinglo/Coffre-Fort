package com.coffre.fort;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DocumentDetailActivity extends AppCompatActivity {

    private TextView titleTextView;
    private TextView categoryTextView;
    private TextView contentTextView;
    private Button deleteButton;

    private DatabaseHelper databaseHelper;
    private int documentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_detail);

        databaseHelper = new DatabaseHelper(this);

        titleTextView = findViewById(R.id.titleTextView);
        categoryTextView = findViewById(R.id.categoryTextView);
        contentTextView = findViewById(R.id.contentTextView);
        deleteButton = findViewById(R.id.deleteButton);

        documentId = getIntent().getIntExtra("document_id", -1);

        if (documentId != -1) {
            loadDocument();
        }

        deleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void loadDocument() {
        Document document = databaseHelper.getDocument(documentId);
        if (document != null) {
            titleTextView.setText(document.getTitle());
            categoryTextView.setText(document.getCategory());
            contentTextView.setText(document.getContent());
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(R.string.yes, (dialog, which) -> {
                databaseHelper.deleteDocument(documentId);
                Toast.makeText(this, R.string.document_deleted, Toast.LENGTH_SHORT).show();
                finish();
            })
            .setNegativeButton(R.string.no, null)
            .show();
    }
}
