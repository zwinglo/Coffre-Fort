package com.coffre.fort;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import com.coffre.fort.MessageSyncManager;

public class MessageDetailActivity extends AppCompatActivity implements AttachmentAdapter.OnAttachmentClickListener {

    private TextView addressTextView;
    private TextView dateTextView;
    private TextView typeTextView;
    private TextView bodyTextView;
    private TextView attachmentsEmptyTextView;
    private RecyclerView attachmentsRecyclerView;

    private DatabaseHelper databaseHelper;
    private AttachmentAdapter attachmentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_detail);
        setTitle(R.string.message_detail_title);

        addressTextView = findViewById(R.id.detailAddressTextView);
        dateTextView = findViewById(R.id.detailDateTextView);
        typeTextView = findViewById(R.id.detailTypeTextView);
        bodyTextView = findViewById(R.id.detailBodyTextView);
        attachmentsEmptyTextView = findViewById(R.id.detailAttachmentsEmptyTextView);
        attachmentsRecyclerView = findViewById(R.id.detailAttachmentsRecyclerView);

        databaseHelper = new DatabaseHelper(this);
        attachmentAdapter = new AttachmentAdapter(this);
        attachmentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        attachmentsRecyclerView.setAdapter(attachmentAdapter);

        long messageId = getIntent().getLongExtra("message_id", -1L);
        if (messageId == -1L) {
            finish();
            return;
        }

        loadMessage(messageId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    private void loadMessage(long messageId) {
        VaultMessage message = databaseHelper.getMessage(messageId);
        if (message == null) {
            finish();
            return;
        }
        String address = TextUtils.isEmpty(message.getAddress())
                ? getString(R.string.sms_document_title_unknown)
                : message.getAddress();
        addressTextView.setText(address);
        dateTextView.setText(MessageFormatter.formatTimestamp(message.getDate()));
        typeTextView.setText(MessageSyncManager.PROVIDER_MMS.equals(message.getProviderType())
                ? getString(R.string.message_type_mms)
                : getString(R.string.message_type_sms));

        String body = TextUtils.isEmpty(message.getBody())
                ? getString(R.string.sms_email_empty_body_placeholder)
                : message.getBody();
        bodyTextView.setText(body);

        List<MessageAttachment> attachments = databaseHelper.getAttachmentsForMessage(messageId);
        if (attachments.isEmpty()) {
            attachmentsEmptyTextView.setVisibility(android.view.View.VISIBLE);
            attachmentsRecyclerView.setVisibility(android.view.View.GONE);
        } else {
            attachmentsEmptyTextView.setVisibility(android.view.View.GONE);
            attachmentsRecyclerView.setVisibility(android.view.View.VISIBLE);
            attachmentAdapter.setAttachments(attachments);
        }
    }

    @Override
    public void onAttachmentClick(MessageAttachment attachment) {
        File file = new File(attachment.getFilePath());
        if (!file.exists()) {
            Toast.makeText(this, R.string.attachment_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mimeType = TextUtils.isEmpty(attachment.getContentType()) ? "*/*" : attachment.getContentType();
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.open_attachment)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_app_for_attachment, Toast.LENGTH_LONG).show();
        }
    }
}
