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
    private TextView audioStatusTextView;
    private TextView noPreviewTextView;
    private ImageView attachmentImageView;
    private VideoView attachmentVideoView;
    private LinearLayout attachmentContainer;
    private LinearLayout audioContainer;
    private Button audioToggleButton;
    private Button openAttachmentButton;
    private Button deleteButton;

    private DatabaseHelper databaseHelper;
    private int documentId;
    private MediaPlayer mediaPlayer;
    private Uri attachmentUri;
    private String attachmentMimeType;

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
        audioStatusTextView = findViewById(R.id.audioStatusTextView);
        noPreviewTextView = findViewById(R.id.noPreviewTextView);
        attachmentContainer = findViewById(R.id.attachmentContainer);
        attachmentImageView = findViewById(R.id.attachmentImageView);
        attachmentVideoView = findViewById(R.id.attachmentVideoView);
        audioContainer = findViewById(R.id.audioContainer);
        audioToggleButton = findViewById(R.id.audioToggleButton);
        openAttachmentButton = findViewById(R.id.openAttachmentButton);
        deleteButton = findViewById(R.id.deleteButton);

        documentId = getIntent().getIntExtra("document_id", -1);

        if (documentId != -1) {
            loadDocument();
        }

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
        Document document = databaseHelper.getDocument(documentId);
        if (document == null) {
            return;
        }

        titleTextView.setText(document.getTitle());
        categoryTextView.setText(document.getCategory());

        String formattedDate = dateFormat.format(new Date(document.getTimestamp()));
        dateTextView.setText(getString(R.string.document_detail_timestamp, formattedDate));

        if (TextUtils.isEmpty(document.getContent())) {
            contentTextView.setVisibility(View.GONE);
        } else {
            contentTextView.setVisibility(View.VISIBLE);
            contentTextView.setText(document.getContent());
        }

        if (document.hasAttachment()) {
            populateAttachmentViews(document);
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
