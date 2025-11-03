package com.coffre.fort;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddDocumentActivity extends AppCompatActivity {

    private EditText titleEditText;
    private EditText contentEditText;
    private Spinner categorySpinner;
    private Button saveButton;
    private Button cancelButton;

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_document);

        databaseHelper = new DatabaseHelper(this);

        titleEditText = findViewById(R.id.titleEditText);
        contentEditText = findViewById(R.id.contentEditText);
        categorySpinner = findViewById(R.id.categorySpinner);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        String[] categories = new String[]{
            getString(R.string.category_text),
            getString(R.string.category_images),
            getString(R.string.category_media),
            getString(R.string.category_other)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        saveButton.setOnClickListener(v -> saveDocument());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void saveDocument() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, R.string.title_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, R.string.content_required, Toast.LENGTH_SHORT).show();
            return;
        }

        Document document = new Document();
        document.setTitle(title);
        document.setContent(content);
        document.setCategory(category);

        databaseHelper.addDocument(document);
        Toast.makeText(this, R.string.document_saved, Toast.LENGTH_SHORT).show();
        finish();
    }
}
