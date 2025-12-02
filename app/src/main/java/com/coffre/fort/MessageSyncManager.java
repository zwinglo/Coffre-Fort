package com.coffre.fort;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Locale;

public class MessageSyncManager {

    public static final String PROVIDER_SMS = "SMS";
    public static final String PROVIDER_MMS = "MMS";
    public static final String ACTION_MESSAGES_UPDATED = "com.coffre.fort.ACTION_MESSAGES_UPDATED";
    private static final int ADDRESS_TYPE_FROM = 137;
    private static final String TAG = "MessageSyncManager";

    private final Context context;
    private final DatabaseHelper databaseHelper;

    public MessageSyncManager(Context context) {
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
    }

    public void synchronizeMessages() {
        syncSms();
        syncMms();
        Intent intent = new Intent(ACTION_MESSAGES_UPDATED);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    private boolean hasReadPermissions() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private void syncSms() {
        if (!hasReadPermissions()) {
            return;
        }
        Uri inboxUri = Telephony.Sms.Inbox.CONTENT_URI;
        String[] projection = {Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.BODY, Telephony.Sms.TYPE};
        try (Cursor cursor = context.getContentResolver().query(inboxUri, projection, null, null, null)) {
            if (cursor == null) {
                return;
            }
            while (cursor.moveToNext()) {
                long providerId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID));
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                int boxType = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));

                VaultMessage message = new VaultMessage(providerId, PROVIDER_SMS, address, date, body, boxType, false);
                databaseHelper.upsertMessage(message);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to synchronize SMS", e);
        }
    }

    private void syncMms() {
        if (!hasReadPermissions()) {
            return;
        }
        Uri inboxUri = Telephony.Mms.Inbox.CONTENT_URI;
        String[] projection = {Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.MESSAGE_BOX, Telephony.Mms.SUBJECT};
        try (Cursor cursor = context.getContentResolver().query(inboxUri, projection, null, null, null)) {
            if (cursor == null) {
                return;
            }
            while (cursor.moveToNext()) {
                long providerId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Mms._ID));
                long dateSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Mms.DATE));
                long timestamp = dateSeconds > 0 ? dateSeconds * 1000L : System.currentTimeMillis();
                int boxType = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX));

                String address = extractSender(providerId);
                String subject = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Mms.SUBJECT));
                String body = extractTextParts(providerId, subject);

                VaultMessage message = new VaultMessage(providerId, PROVIDER_MMS, address, timestamp, body, boxType, false);
                long localId = databaseHelper.upsertMessage(message);

                boolean hasAttachments = processAttachments(providerId, localId);
                if (hasAttachments) {
                    databaseHelper.updateMessageAttachmentFlag(localId, true);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to synchronize MMS", e);
        }
    }

    private String extractSender(long mmsId) {
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

    private String extractTextParts(long mmsId, String subject) {
        StringBuilder bodyBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(subject)) {
            bodyBuilder.append(subject).append('\n');
        }
        Uri partUri = Uri.parse("content://mms/part");
        try (Cursor cursor = context.getContentResolver().query(partUri,
                new String[]{"_id", "ct", "text", "_data"}, "mid = ?", new String[]{String.valueOf(mmsId)}, null)) {
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
                        text = readTextFromPart(partId);
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

    private String readTextFromPart(String partId) {
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

    private boolean processAttachments(long mmsId, long messageLocalId) {
        Uri partUri = Uri.parse("content://mms/part");
        boolean hasAttachment = false;
        try (Cursor cursor = context.getContentResolver().query(partUri,
                new String[]{"_id", "ct", "name", "cl", "_data"}, "mid = ?",
                new String[]{String.valueOf(mmsId)}, null)) {
            if (cursor == null) {
                return false;
            }
            while (cursor.moveToNext()) {
                String contentType = cursor.getString(cursor.getColumnIndexOrThrow("ct"));
                if (contentType == null || contentType.startsWith("text/")) {
                    continue;
                }
                String partId = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                if (databaseHelper.attachmentExists(messageLocalId, partId)) {
                    hasAttachment = true;
                    continue;
                }
                Uri dataUri = ContentUris.withAppendedId(Uri.parse("content://mms/part"), Long.parseLong(partId));
                String fileName = buildAttachmentName(mmsId, partId, contentType,
                        cursor.getString(cursor.getColumnIndexOrThrow("cl")));
                File destination = new File(getAttachmentsDir(), fileName);
                long size = copyToFile(dataUri, destination);
                if (size > 0) {
                    MessageAttachment attachment = new MessageAttachment(messageLocalId, partId,
                            destination.getAbsolutePath(), contentType, size);
                    databaseHelper.insertAttachment(attachment);
                    hasAttachment = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to process MMS attachments", e);
        }
        return hasAttachment;
    }

    private File getAttachmentsDir() {
        File dir = new File(context.getFilesDir(), "attachments");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    private long copyToFile(Uri source, File destination) {
        try (InputStream in = context.getContentResolver().openInputStream(source);
             OutputStream out = new FileOutputStream(destination)) {
            if (in == null) {
                return 0L;
            }
            byte[] buffer = new byte[8 * 1024];
            int read;
            long total = 0L;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                total += read;
            }
            return total;
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy MMS attachment", e);
            return 0L;
        }
    }

    private String buildAttachmentName(long mmsId, String partId, String contentType, String fallbackName) {
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType);
        if (TextUtils.isEmpty(extension) && contentType != null && contentType.contains("/")) {
            extension = contentType.substring(contentType.indexOf('/') + 1);
        }
        if (TextUtils.isEmpty(extension)) {
            extension = "bin";
        }
        if (TextUtils.isEmpty(fallbackName)) {
            return String.format(Locale.US, "%d_%s.%s", mmsId, partId, extension);
        }
        return String.format(Locale.US, "%d_%s_%s", mmsId, partId, fallbackName);
    }
}
