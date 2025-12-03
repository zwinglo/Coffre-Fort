package com.coffre.fort;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class AddDocumentActivity extends AppCompatActivity {

    private static final String STATE_ATTACHMENT_URI = "state_attachment_uri";
    private static final String STATE_ATTACHMENT_MIME = "state_attachment_mime";
    private static final String STATE_ATTACHMENT_NAME = "state_attachment_name";

    private EditText titleEditText;
    private EditText contentEditText;
    private TextView contentLabel;
    private Spinner categorySpinner;
    private Button saveButton;
    private Button cancelButton;
    private Button attachButton;
    private Button clearAttachmentButton;
    private TextView attachmentSummaryTextView;

    private DatabaseHelper databaseHelper;

    private Uri selectedAttachmentUri;
    private String selectedAttachmentMimeType;
    private String selectedAttachmentName;

    private ActivityResultLauncher<Intent> attachmentPickerLauncher;
    private String[] categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_document);

        databaseHelper = new DatabaseHelper(this);

        titleEditText = findViewById(R.id.titleEditText);
        contentEditText = findViewById(R.id.contentEditText);
        contentLabel = findViewById(R.id.contentLabel);
        categorySpinner = findViewById(R.id.categorySpinner);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        attachButton = findViewById(R.id.attachButton);
        clearAttachmentButton = findViewById(R.id.clearAttachmentButton);
        attachmentSummaryTextView = findViewById(R.id.attachmentSummaryTextView);

        categories = getResources().getStringArray(R.array.document_categories);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        attachmentPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleAttachmentResult(result.getData());
                    }
                }
        );

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateContentRequirements(categories[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        attachButton.setOnClickListener(v -> openAttachmentPicker());
        clearAttachmentButton.setOnClickListener(v -> clearAttachment());
        saveButton.setOnClickListener(v -> saveDocument());
        cancelButton.setOnClickListener(v -> finish());

        if (savedInstanceState != null) {
            restoreAttachmentState(savedInstanceState);
        }

        updateContentRequirements(categories[categorySpinner.getSelectedItemPosition()]);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedAttachmentUri != null) {
            outState.putString(STATE_ATTACHMENT_URI, selectedAttachmentUri.toString());
        }
        outState.putString(STATE_ATTACHMENT_MIME, selectedAttachmentMimeType);
        outState.putString(STATE_ATTACHMENT_NAME, selectedAttachmentName);
    }

    private void restoreAttachmentState(Bundle savedInstanceState) {
        String attachmentUriString = savedInstanceState.getString(STATE_ATTACHMENT_URI);
        if (!TextUtils.isEmpty(attachmentUriString)) {
            selectedAttachmentUri = Uri.parse(attachmentUriString);
        }
        selectedAttachmentMimeType = savedInstanceState.getString(STATE_ATTACHMENT_MIME);
        selectedAttachmentName = savedInstanceState.getString(STATE_ATTACHMENT_NAME);
        updateAttachmentSummary();
    }

    private void updateContentRequirements(String category) {
        boolean requiresAttachment = requiresAttachment(category);
        boolean requiresTextContent = requiresTextContent(category);

        contentLabel.setText(requiresAttachment
                ? R.string.document_description
                : R.string.document_content);

        if (requiresTextContent) {
            contentEditText.setHint(R.string.document_content_hint);
        } else {
            contentEditText.setHint(R.string.document_description_hint);
        }

        attachmentSummaryTextView.setVisibility(View.VISIBLE);
        attachButton.setVisibility(View.VISIBLE);

        if (requiresAttachment && selectedAttachmentUri == null) {
            attachmentSummaryTextView.setText(R.string.attachment_required_hint);
        } else {
            updateAttachmentSummary();
        }

        if (!requiresAttachment && selectedAttachmentUri == null) {
            attachmentSummaryTextView.setText(R.string.attachment_optional);
        }

        clearAttachmentButton.setVisibility(selectedAttachmentUri != null ? View.VISIBLE : View.GONE);
    }

    private boolean requiresAttachment(String category) {
        return category.equals(getString(R.string.category_images))
                || category.equals(getString(R.string.category_media))
                || category.equals(getString(R.string.category_other));
    }

    private boolean requiresTextContent(String category) {
        return category.equals(getString(R.string.category_text))
                || category.equals(getString(R.string.category_sms));
    }

    private void openAttachmentPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/*", "image/*", "video/*", "audio/*", "application/*"});
        attachmentPickerLauncher.launch(intent);
    }

    private void handleAttachmentResult(Intent data) {
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }

        final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } catch (SecurityException ignored) {
        }

        selectedAttachmentUri = uri;
        selectedAttachmentMimeType = getContentResolver().getType(uri);
        selectedAttachmentName = AttachmentUtils.getDisplayName(this, uri);
        if (TextUtils.isEmpty(selectedAttachmentName)) {
            selectedAttachmentName = getString(R.string.unknown_file);
        }
        updateAttachmentSummary();
        updateContentRequirements(getCurrentCategory());
    }

    private void updateAttachmentSummary() {
        if (selectedAttachmentUri == null) {
            attachmentSummaryTextView.setText(R.string.no_attachment_selected);
            clearAttachmentButton.setVisibility(View.GONE);
        } else {
            attachmentSummaryTextView.setText(getString(R.string.attachment_selected, selectedAttachmentName));
            clearAttachmentButton.setVisibility(View.VISIBLE);
        }
    }

    private void clearAttachment() {
        selectedAttachmentUri = null;
        selectedAttachmentMimeType = null;
        selectedAttachmentName = null;
        updateAttachmentSummary();
        updateContentRequirements(getCurrentCategory());
    }

    private void saveDocument() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();
        String category = getCurrentCategory();

        boolean requiresAttachment = requiresAttachment(category);
        boolean requiresTextContent = requiresTextContent(category);

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, R.string.title_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (requiresTextContent && TextUtils.isEmpty(content)) {
            Toast.makeText(this, R.string.content_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (requiresAttachment && selectedAttachmentUri == null) {
            Toast.makeText(this, R.string.attachment_required_toast, Toast.LENGTH_SHORT).show();
            return;
        }

        Document document = new Document();
        document.setTitle(title);
        document.setContent(TextUtils.isEmpty(content) ? null : content);
        document.setCategory(category);
        document.setAttachmentUri(selectedAttachmentUri != null ? selectedAttachmentUri.toString() : null);
        document.setAttachmentMimeType(selectedAttachmentMimeType);
        document.setAttachmentName(selectedAttachmentName);

        databaseHelper.addDocument(document);
        if (category.equals(getString(R.string.category_messages))
                || category.equals(getString(R.string.category_sms))) {
            MessageEmailDispatcher.dispatch(this, document, title, MessageEmailDispatcher.MessageType.CHAT, content, document.getTimestamp());
        }
        Toast.makeText(this, R.string.document_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getCurrentCategory() {
        Object selectedItem = categorySpinner.getSelectedItem();
        return selectedItem != null ? selectedItem.toString() : categories[0];
    }
}
