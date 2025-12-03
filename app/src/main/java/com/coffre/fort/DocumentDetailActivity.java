package com.coffre.fort;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class DocumentDetailActivity extends AppCompatActivity {

    private TextView titleTextView;
    private TextView categoryTextView;
    private TextView contentTextView;
    private TextView dateTextView;
    private TextView attachmentNameTextView;
    private TextView attachmentExtensionTextView;
    private TextView attachmentSizeTextView;
    private TextView audioStatusTextView;
    private TextView noPreviewTextView;
    private ImageView attachmentImageView;
    private VideoView attachmentVideoView;
    private LinearLayout attachmentContainer;
    private LinearLayout audioContainer;
    private Button audioToggleButton;
    private Button openAttachmentButton;
    private Button changeCategoryButton;
    private Button emailButton;
    private Button deleteButton;

    private DatabaseHelper databaseHelper;
    private int documentId;
    private MediaPlayer mediaPlayer;
    private Uri attachmentUri;
    private String attachmentMimeType;
    private Document currentDocument;

    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_detail);

        databaseHelper = new DatabaseHelper(this);

        titleTextView = findViewById(R.id.titleTextView);
        categoryTextView = findViewById(R.id.categoryTextView);
        contentTextView = findViewById(R.id.contentTextView);
        dateTextView = findViewById(R.id.dateTextView);
        attachmentNameTextView = findViewById(R.id.attachmentNameTextView);
        attachmentExtensionTextView = findViewById(R.id.attachmentExtensionTextView);
        attachmentSizeTextView = findViewById(R.id.attachmentSizeTextView);
        audioStatusTextView = findViewById(R.id.audioStatusTextView);
        noPreviewTextView = findViewById(R.id.noPreviewTextView);
        attachmentContainer = findViewById(R.id.attachmentContainer);
        attachmentImageView = findViewById(R.id.attachmentImageView);
        attachmentVideoView = findViewById(R.id.attachmentVideoView);
        audioContainer = findViewById(R.id.audioContainer);
        audioToggleButton = findViewById(R.id.audioToggleButton);
        openAttachmentButton = findViewById(R.id.openAttachmentButton);
        changeCategoryButton = findViewById(R.id.changeCategoryButton);
        emailButton = findViewById(R.id.emailButton);
        deleteButton = findViewById(R.id.deleteButton);

        documentId = getIntent().getIntExtra("document_id", -1);

        if (documentId != -1) {
            loadDocument();
        }

        changeCategoryButton.setOnClickListener(v -> showCategoryPicker());
        emailButton.setOnClickListener(v -> sendDocumentByEmail());
        deleteButton.setOnClickListener(v -> confirmDelete());
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseMediaPlayer();
        if (attachmentVideoView != null) {
            attachmentVideoView.stopPlayback();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    private void loadDocument() {
        currentDocument = databaseHelper.getDocument(documentId);
        if (currentDocument == null) {
            return;
        }

        titleTextView.setText(currentDocument.getTitle());
        String normalizedCategory = CategoryUtils.normalizeCategory(this, currentDocument.getCategory());
        currentDocument.setCategory(normalizedCategory);
        categoryTextView.setText(normalizedCategory);

        String formattedDate = dateFormat.format(new Date(currentDocument.getTimestamp()));
        dateTextView.setText(getString(R.string.document_detail_timestamp, formattedDate));

        if (TextUtils.isEmpty(currentDocument.getContent())) {
            contentTextView.setVisibility(View.GONE);
        } else {
            contentTextView.setVisibility(View.VISIBLE);
            contentTextView.setText(currentDocument.getContent());
        }

        if (currentDocument.hasAttachment()) {
            populateAttachmentViews(currentDocument);
        } else {
            attachmentNameTextView.setVisibility(View.GONE);
            openAttachmentButton.setVisibility(View.GONE);
            noPreviewTextView.setVisibility(View.GONE);
            attachmentContainer.setVisibility(View.GONE);
        }
    }

    private void populateAttachmentViews(Document document) {
        attachmentUri = Uri.parse(document.getAttachmentUri());
        attachmentMimeType = document.getAttachmentMimeType();
        if (TextUtils.isEmpty(attachmentMimeType)) {
            attachmentMimeType = getContentResolver().getType(attachmentUri);
        }

        attachmentContainer.setVisibility(View.VISIBLE);
        attachmentImageView.setVisibility(View.GONE);
        attachmentVideoView.setVisibility(View.GONE);
        audioContainer.setVisibility(View.GONE);
        openAttachmentButton.setVisibility(View.GONE);
        noPreviewTextView.setVisibility(View.GONE);
        attachmentExtensionTextView.setVisibility(View.GONE);
        attachmentSizeTextView.setVisibility(View.GONE);
        resetAudioUi();

        String attachmentName = document.getAttachmentName();
        if (TextUtils.isEmpty(attachmentName)) {
            attachmentName = AttachmentUtils.getDisplayName(this, attachmentUri);
        }
        if (TextUtils.isEmpty(attachmentName)) {
            attachmentName = getString(R.string.unknown_file);
        }

        attachmentNameTextView.setVisibility(View.VISIBLE);
        String displayMimeType = TextUtils.isEmpty(attachmentMimeType)
                ? getString(R.string.unknown_mime_type)
                : attachmentMimeType;
        attachmentNameTextView.setText(getString(R.string.attachment_name_format, attachmentName, displayMimeType));

        String extension = AttachmentUtils.getFileExtension(attachmentName, attachmentMimeType);
        if (!TextUtils.isEmpty(extension)) {
            attachmentExtensionTextView.setVisibility(View.VISIBLE);
            attachmentExtensionTextView.setText(getString(R.string.attachment_extension_format, extension));
        }

        long fileSize = AttachmentUtils.getFileSize(this, attachmentUri);
        if (fileSize >= 0) {
            attachmentSizeTextView.setVisibility(View.VISIBLE);
            attachmentSizeTextView.setText(
                    getString(R.string.attachment_size_format, AttachmentUtils.formatFileSize(fileSize)));
        } else {
            attachmentSizeTextView.setVisibility(View.VISIBLE);
            attachmentSizeTextView.setText(R.string.attachment_size_unknown);
        }

        if (attachmentMimeType != null) {
            if (attachmentMimeType.startsWith("image/")) {
                showImageAttachment();
            } else if (attachmentMimeType.startsWith("video/")) {
                showVideoAttachment();
            } else if (attachmentMimeType.startsWith("audio/")) {
                showAudioAttachment();
            } else {
                showGenericAttachment();
            }
        } else {
            showGenericAttachment();
        }
    }

    private void showImageAttachment() {
        attachmentImageView.setVisibility(View.VISIBLE);
        attachmentImageView.setImageURI(attachmentUri);
        noPreviewTextView.setVisibility(View.GONE);
        openAttachmentButton.setVisibility(View.VISIBLE);
        openAttachmentButton.setOnClickListener(v -> openAttachment());
    }

    private void showVideoAttachment() {
        attachmentVideoView.setVisibility(View.VISIBLE);
        attachmentVideoView.setVideoURI(attachmentUri);
        android.widget.MediaController mediaController = new android.widget.MediaController(this);
        mediaController.setAnchorView(attachmentVideoView);
        attachmentVideoView.setMediaController(mediaController);
        attachmentVideoView.setOnPreparedListener(mp -> {
            attachmentVideoView.seekTo(1);
            mp.setLooping(false);
        });
        noPreviewTextView.setVisibility(View.GONE);
        openAttachmentButton.setVisibility(View.VISIBLE);
        openAttachmentButton.setOnClickListener(v -> openAttachment());
    }

    private void showAudioAttachment() {
        audioContainer.setVisibility(View.VISIBLE);
        audioToggleButton.setOnClickListener(v -> toggleAudioPlayback());
        noPreviewTextView.setVisibility(View.GONE);
        openAttachmentButton.setVisibility(View.VISIBLE);
        openAttachmentButton.setOnClickListener(v -> openAttachment());
    }

    private void showGenericAttachment() {
        noPreviewTextView.setVisibility(View.VISIBLE);
        openAttachmentButton.setVisibility(View.VISIBLE);
        openAttachmentButton.setOnClickListener(v -> openAttachment());
    }

    private void toggleAudioPlayback() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
            try {
                mediaPlayer.setDataSource(this, attachmentUri);
                mediaPlayer.setOnPreparedListener(mp -> {
                    audioStatusTextView.setText(R.string.audio_status_playing);
                    audioToggleButton.setText(R.string.pause_audio);
                    mp.start();
                });
                mediaPlayer.setOnCompletionListener(mp -> resetAudioUi());
                mediaPlayer.prepareAsync();
                audioStatusTextView.setText(R.string.audio_status_loading);
            } catch (IOException e) {
                audioStatusTextView.setText(R.string.audio_status_error);
                Toast.makeText(this, R.string.audio_playback_error, Toast.LENGTH_SHORT).show();
                releaseMediaPlayer();
            }
            return;
        }

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            audioStatusTextView.setText(R.string.audio_status_paused);
            audioToggleButton.setText(R.string.play_audio);
        } else {
            mediaPlayer.start();
            audioStatusTextView.setText(R.string.audio_status_playing);
            audioToggleButton.setText(R.string.pause_audio);
        }
    }

    private void resetAudioUi() {
        if (audioStatusTextView != null) {
            audioStatusTextView.setText(R.string.audio_status_idle);
        }
        if (audioToggleButton != null) {
            audioToggleButton.setText(R.string.play_audio);
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        resetAudioUi();
    }

    private void openAttachment() {
        if (attachmentUri == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mimeType = attachmentMimeType != null ? attachmentMimeType : "*/*";
        intent.setDataAndType(attachmentUri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_app_for_attachment, Toast.LENGTH_SHORT).show();
        }
    }

    private void showCategoryPicker() {
        if (currentDocument == null) {
            return;
        }
        String[] categories = getResources().getStringArray(R.array.document_categories);
        String currentCategory = CategoryUtils.normalizeCategory(this, currentDocument.getCategory());
        final int[] selectedIndex = {0};
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(currentCategory)) {
                selectedIndex[0] = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.change_category_dialog_title)
                .setSingleChoiceItems(categories, selectedIndex[0], (dialog, which) -> selectedIndex[0] = which)
                .setPositiveButton(R.string.ok, (dialog, which) -> updateCategory(categories[selectedIndex[0]]))
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }

    private void updateCategory(String newCategory) {
        if (currentDocument == null) {
            return;
        }
        String normalizedCategory = CategoryUtils.normalizeCategory(this, newCategory);
        boolean updated = databaseHelper.updateDocumentCategory(currentDocument.getId(), normalizedCategory);
        if (updated) {
            currentDocument.setCategory(normalizedCategory);
            categoryTextView.setText(normalizedCategory);
            Toast.makeText(this, R.string.change_category_success, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.change_category_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendDocumentByEmail() {
        if (currentDocument == null) {
            return;
        }
        EmailConfigManager emailConfigManager = new EmailConfigManager(this);
        if (!emailConfigManager.isConfigured()) {
            Toast.makeText(this, R.string.document_email_missing_config, Toast.LENGTH_LONG).show();
            return;
        }

        String subject = getString(R.string.email_document_subject, currentDocument.getTitle());
        String body = buildEmailBody();
        EmailSender.sendEmail(this, subject, body, currentDocument);
        Toast.makeText(this, R.string.document_email_started, Toast.LENGTH_SHORT).show();
    }

    private String buildEmailBody() {
        String category = CategoryUtils.normalizeCategory(this, currentDocument.getCategory());
        String formattedDate = dateFormat.format(new Date(currentDocument.getTimestamp()));
        String content = TextUtils.isEmpty(currentDocument.getContent())
                ? getString(R.string.email_document_body_placeholder)
                : currentDocument.getContent();
        return getString(R.string.email_document_body,
                currentDocument.getTitle(),
                category,
                formattedDate,
                content);
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
