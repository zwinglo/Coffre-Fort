package com.coffre.fort;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MmsReceiver extends BroadcastReceiver {

    private static final String TAG = "MmsReceiver";
    private static final int ADDRESS_TYPE_FROM = 137;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }

        Bundle extras = intent.getExtras();
        String mimeType = intent.getType();
        if (extras == null || !"application/vnd.wap.mms-message".equals(mimeType)) {
            return;
        }

        PendingResult pendingResult = goAsync();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                processLatestMms(context.getApplicationContext());
            } finally {
                executor.shutdown();
                pendingResult.finish();
            }
        });
    }

    private void processLatestMms(Context context) {
        Uri inboxUri = Telephony.Mms.Inbox.CONTENT_URI;
        String[] projection = {Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.SUBJECT};
        try (Cursor cursor = context.getContentResolver().query(
                inboxUri,
                projection,
                null,
                null,
                Telephony.Mms.DATE + " DESC LIMIT 1")) {
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }

            long mmsId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Mms._ID));
            long dateSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Mms.DATE));
            long timestamp = dateSeconds > 0 ? dateSeconds * 1000L : System.currentTimeMillis();

            String subject = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Mms.SUBJECT));
            String sender = extractSender(context, mmsId);
            String body = extractTextParts(context, mmsId, subject);

            Document document = new Document();
            document.setTitle(context.getString(R.string.message_document_title, sender));
            document.setCategory(context.getString(R.string.category_messages));
            document.setContent(MessageFormatter.buildDocumentContent(context, sender, timestamp, body));
            document.setTimestamp(timestamp);

            AttachmentInfo attachmentInfo = findFirstAttachment(context, mmsId);
            if (attachmentInfo != null) {
                document.setAttachmentUri(attachmentInfo.uri);
                document.setAttachmentMimeType(attachmentInfo.mimeType);
                document.setAttachmentName(attachmentInfo.displayName);
            }

            DatabaseHelper databaseHelper = new DatabaseHelper(context);
            long documentId = databaseHelper.addDocument(document);
            databaseHelper.close();
            document.setId((int) documentId);

            MessageEmailDispatcher.dispatch(context, document, sender, MessageEmailDispatcher.MessageType.MMS);

            Intent refreshIntent = new Intent(SmsReceiver.ACTION_SMS_SAVED);
            refreshIntent.setPackage(context.getPackageName());
            refreshIntent.putExtra("document_id", document.getId());
            context.sendBroadcast(refreshIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to process MMS", e);
        }
    }

    private String extractSender(Context context, long mmsId) {
        Uri addrUri = Uri.parse("content://mms/" + mmsId + "/addr");
        String[] projection = {Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.TYPE};
        try (Cursor cursor = context.getContentResolver().query(addrUri, projection,
                Telephony.Mms.Addr.TYPE + "=" + ADDRESS_TYPE_FROM, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Mms.Addr.ADDRESS));
                if (!TextUtils.isEmpty(address)) {
                    return address;
                }
            }
        }
        return context.getString(R.string.sms_document_title_unknown);
    }

    private String extractTextParts(Context context, long mmsId, String subject) {
        StringBuilder bodyBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(subject)) {
            bodyBuilder.append(subject).append('\n');
        }

        Uri partUri = Uri.parse("content://mms/" + mmsId + "/part");
        try (Cursor cursor = context.getContentResolver().query(partUri,
                new String[]{"_id", "ct", "text", "_data"}, null, null, null)) {
            if (cursor == null) {
                return bodyBuilder.toString();
            }
            while (cursor.moveToNext()) {
                String contentType = cursor.getString(cursor.getColumnIndexOrThrow("ct"));
                if (contentType != null && contentType.startsWith("text/")) {
                    String text = cursor.getString(cursor.getColumnIndexOrThrow("text"));
                    String dataLocation = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
                    if (!TextUtils.isEmpty(dataLocation)) {
                        String partId = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                        text = readTextFromPart(context, partId);
                    }
                    if (!TextUtils.isEmpty(text)) {
                        if (bodyBuilder.length() > 0) {
                            bodyBuilder.append('\n');
                        }
                        bodyBuilder.append(text);
                    }
                }
            }
        }
        if (bodyBuilder.length() == 0) {
            return context.getString(R.string.sms_email_empty_body_placeholder);
        }
        return bodyBuilder.toString();
    }

    private String readTextFromPart(Context context, String partId) {
        Uri uri = Uri.parse("content://mms/part/" + partId);
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                return null;
            }
            try (InputStreamReader isr = new InputStreamReader(inputStream);
                 BufferedReader reader = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to read MMS text part", e);
            return null;
        }
    }

    private AttachmentInfo findFirstAttachment(Context context, long mmsId) {
        Uri partUri = Uri.parse("content://mms/" + mmsId + "/part");
        String[] projection = {"_id", "ct", "name", "cl", "_data"};
        try (Cursor cursor = context.getContentResolver().query(partUri, projection, null, null, null)) {
            if (cursor == null) {
                return null;
            }
            while (cursor.moveToNext()) {
                String contentType = cursor.getString(cursor.getColumnIndexOrThrow("ct"));
                String dataLocation = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
                if (contentType != null && !contentType.startsWith("text/") && !TextUtils.isEmpty(dataLocation)) {
                    String partId = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                    String displayName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    if (TextUtils.isEmpty(displayName)) {
                        displayName = cursor.getString(cursor.getColumnIndexOrThrow("cl"));
                    }
                    if (TextUtils.isEmpty(displayName)) {
                        displayName = "piece_jointe_mms";
                    }
                    Uri uri = Uri.parse("content://mms/part/" + partId);
                    return new AttachmentInfo(uri.toString(), contentType, displayName);
                }
            }
        }
        return null;
    }

    private static class AttachmentInfo {
        final String uri;
        final String mimeType;
        final String displayName;

        AttachmentInfo(String uri, String mimeType, String displayName) {
            this.uri = uri;
            this.mimeType = mimeType;
            this.displayName = displayName;
        }
    }
}
