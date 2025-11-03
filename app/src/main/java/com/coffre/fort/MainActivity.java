package com.coffre.fort;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DocumentAdapter.OnDocumentClickListener {

    private RecyclerView documentsRecyclerView;
    private TextView emptyTextView;
    private Spinner categorySpinner;
    private FloatingActionButton addDocumentFab;
    
    private DatabaseHelper databaseHelper;
    private DocumentAdapter documentAdapter;
    private String[] categories;
    private String selectedCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);

        documentsRecyclerView = findViewById(R.id.documentsRecyclerView);
        emptyTextView = findViewById(R.id.emptyTextView);
        categorySpinner = findViewById(R.id.categorySpinner);
        addDocumentFab = findViewById(R.id.addDocumentFab);

        categories = new String[]{
            getString(R.string.all_categories),
            getString(R.string.category_text),
            getString(R.string.category_images),
            getString(R.string.category_media),
            getString(R.string.category_other)
        };

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
            Intent intent = new Intent(MainActivity.this, AddDocumentActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDocuments();
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
}
