package com.coffre.fort;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "coffre_fort.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_DOCUMENTS = "documents";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_ATTACHMENT_URI = "attachment_uri";
    private static final String COLUMN_ATTACHMENT_MIME = "attachment_mime";
    private static final String COLUMN_ATTACHMENT_NAME = "attachment_name";

    private static final String TABLE_AUTH = "auth";
    private static final String COLUMN_PASSWORD = "password";

    private static final String TABLE_MESSAGES = "messages";
    private static final String COLUMN_MESSAGE_LOCAL_ID = "localId";
    private static final String COLUMN_MESSAGE_PROVIDER_ID = "providerId";
    private static final String COLUMN_MESSAGE_PROVIDER_TYPE = "providerType";
    private static final String COLUMN_MESSAGE_ADDRESS = "address";
    private static final String COLUMN_MESSAGE_DATE = "date";
    private static final String COLUMN_MESSAGE_BODY = "body";
    private static final String COLUMN_MESSAGE_BOX_TYPE = "boxType";
    private static final String COLUMN_MESSAGE_HAS_ATTACHMENTS = "hasAttachments";

    private static final String TABLE_ATTACHMENTS = "message_attachments";
    private static final String COLUMN_ATTACHMENT_ID = "attachmentId";
    private static final String COLUMN_ATTACHMENT_MESSAGE_ID = "messageLocalId";
    private static final String COLUMN_ATTACHMENT_PROVIDER_PART_ID = "providerPartId";
    private static final String COLUMN_ATTACHMENT_PATH = "filePath";
    private static final String COLUMN_ATTACHMENT_CONTENT_TYPE = "contentType";
    private static final String COLUMN_ATTACHMENT_SIZE = "sizeBytes";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createDocumentsTable = "CREATE TABLE " + TABLE_DOCUMENTS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TITLE + " TEXT, "
                + COLUMN_CONTENT + " TEXT, "
                + COLUMN_CATEGORY + " TEXT, "
                + COLUMN_TIMESTAMP + " INTEGER, "
                + COLUMN_ATTACHMENT_URI + " TEXT, "
                + COLUMN_ATTACHMENT_MIME + " TEXT, "
                + COLUMN_ATTACHMENT_NAME + " TEXT)";
        db.execSQL(createDocumentsTable);

        String createAuthTable = "CREATE TABLE " + TABLE_AUTH + " ("
                + COLUMN_PASSWORD + " TEXT)";
        db.execSQL(createAuthTable);

        createMessageTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_DOCUMENTS + " ADD COLUMN " + COLUMN_ATTACHMENT_URI + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_DOCUMENTS + " ADD COLUMN " + COLUMN_ATTACHMENT_MIME + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_DOCUMENTS + " ADD COLUMN " + COLUMN_ATTACHMENT_NAME + " TEXT");
        }
        if (oldVersion < 3) {
            createMessageTables(db);
        }
    }

    private void createMessageTables(SQLiteDatabase db) {
        String createMessagesTable = "CREATE TABLE " + TABLE_MESSAGES + " ("
                + COLUMN_MESSAGE_LOCAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_MESSAGE_PROVIDER_ID + " INTEGER, "
                + COLUMN_MESSAGE_PROVIDER_TYPE + " TEXT, "
                + COLUMN_MESSAGE_ADDRESS + " TEXT, "
                + COLUMN_MESSAGE_DATE + " INTEGER, "
                + COLUMN_MESSAGE_BODY + " TEXT, "
                + COLUMN_MESSAGE_BOX_TYPE + " INTEGER, "
                + COLUMN_MESSAGE_HAS_ATTACHMENTS + " INTEGER DEFAULT 0, "
                + "UNIQUE(" + COLUMN_MESSAGE_PROVIDER_ID + ", " + COLUMN_MESSAGE_PROVIDER_TYPE + ") ON CONFLICT IGNORE" +
                ")";
        db.execSQL(createMessagesTable);

        String createAttachmentsTable = "CREATE TABLE " + TABLE_ATTACHMENTS + " ("
                + COLUMN_ATTACHMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_ATTACHMENT_MESSAGE_ID + " INTEGER, "
                + COLUMN_ATTACHMENT_PROVIDER_PART_ID + " TEXT, "
                + COLUMN_ATTACHMENT_PATH + " TEXT, "
                + COLUMN_ATTACHMENT_CONTENT_TYPE + " TEXT, "
                + COLUMN_ATTACHMENT_SIZE + " INTEGER, "
                + "UNIQUE(" + COLUMN_ATTACHMENT_MESSAGE_ID + ", " + COLUMN_ATTACHMENT_PROVIDER_PART_ID + ") ON CONFLICT IGNORE, "
                + "FOREIGN KEY(" + COLUMN_ATTACHMENT_MESSAGE_ID + ") REFERENCES " + TABLE_MESSAGES + "(" + COLUMN_MESSAGE_LOCAL_ID + ")" +
                ")";
        db.execSQL(createAttachmentsTable);
    }

    public void setPassword(String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_AUTH, null, null);
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_PASSWORD, hashPassword(password));
        db.insert(TABLE_AUTH, null, values);
        db.close();
    }

    public boolean verifyPassword(String password) {
        String storedHash = getPasswordHash();
        if (storedHash == null) {
            return false;
        }
        return storedHash.equals(hashPassword(password));
    }

    private String getPasswordHash() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_AUTH, new String[]{COLUMN_PASSWORD},
                null, null, null, null, null);
        
        String password = null;
        if (cursor.moveToFirst()) {
            password = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return password;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public boolean hasPassword() {
        return getPasswordHash() != null;
    }

    public long addDocument(Document document) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, document.getTitle());
        values.put(COLUMN_CONTENT, document.getContent());
        values.put(COLUMN_CATEGORY, document.getCategory());
        values.put(COLUMN_TIMESTAMP, document.getTimestamp());
        values.put(COLUMN_ATTACHMENT_URI, document.getAttachmentUri());
        values.put(COLUMN_ATTACHMENT_MIME, document.getAttachmentMimeType());
        values.put(COLUMN_ATTACHMENT_NAME, document.getAttachmentName());
        
        long id = db.insert(TABLE_DOCUMENTS, null, values);
        db.close();
        return id;
    }

    public List<Document> getAllDocuments() {
        List<Document> documents = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_DOCUMENTS, null, null, null, null, null,
                COLUMN_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            do {
                documents.add(readDocumentFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return documents;
    }

    public List<Document> getDocumentsByCategory(String category) {
        List<Document> documents = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_DOCUMENTS, null,
                COLUMN_CATEGORY + "=?", new String[]{category},
                null, null, COLUMN_TIMESTAMP + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                documents.add(readDocumentFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return documents;
    }

    public List<Document> getDocumentsByCategories(List<String> categories) {
        List<Document> documents = new ArrayList<>();
        if (categories == null || categories.isEmpty()) {
            return documents;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        String placeholders = TextUtils.join(",", Collections.nCopies(categories.size(), "?"));
        Cursor cursor = db.query(TABLE_DOCUMENTS, null,
                COLUMN_CATEGORY + " IN (" + placeholders + ")",
                categories.toArray(new String[0]),
                null, null, COLUMN_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            do {
                documents.add(readDocumentFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return documents;
    }

    public Document getDocument(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_DOCUMENTS, null,
                COLUMN_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null);
        
        Document document = null;
        if (cursor.moveToFirst()) {
            document = readDocumentFromCursor(cursor);
        }
        cursor.close();
        db.close();
        return document;
    }

    public boolean updateDocumentCategory(int id, String newCategory) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY, newCategory);
        int rowsUpdated = db.update(TABLE_DOCUMENTS, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rowsUpdated > 0;
    }

    public void deleteDocument(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DOCUMENTS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public int deleteAllDocuments() {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_DOCUMENTS, null, null);
        db.close();
        return rowsDeleted;
    }

    public int getDocumentCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(" + COLUMN_ID + ") FROM " + TABLE_DOCUMENTS, null);

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    private Document readDocumentFromCursor(Cursor cursor) {
        return new Document(
            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
            cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ATTACHMENT_URI)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ATTACHMENT_MIME)),
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ATTACHMENT_NAME))
        );
    }

    public long upsertMessage(VaultMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();
        long existingId = getExistingMessageId(db, message.getProviderId(), message.getProviderType());

        ContentValues values = new ContentValues();
        values.put(COLUMN_MESSAGE_PROVIDER_ID, message.getProviderId());
        values.put(COLUMN_MESSAGE_PROVIDER_TYPE, message.getProviderType());
        values.put(COLUMN_MESSAGE_ADDRESS, message.getAddress());
        values.put(COLUMN_MESSAGE_DATE, message.getDate());
        values.put(COLUMN_MESSAGE_BODY, message.getBody());
        values.put(COLUMN_MESSAGE_BOX_TYPE, message.getBoxType());
        values.put(COLUMN_MESSAGE_HAS_ATTACHMENTS, message.hasAttachments() ? 1 : 0);

        long resultId;
        if (existingId != -1) {
            boolean existingHasAttachments = existingMessageHasAttachments(db, existingId);
            if (existingHasAttachments && !message.hasAttachments()) {
                values.put(COLUMN_MESSAGE_HAS_ATTACHMENTS, 1);
            }
            db.update(TABLE_MESSAGES, values, COLUMN_MESSAGE_LOCAL_ID + "=?", new String[]{String.valueOf(existingId)});
            resultId = existingId;
        } else {
            resultId = db.insert(TABLE_MESSAGES, null, values);
        }
        db.close();
        return resultId;
    }

    public void updateMessageAttachmentFlag(long localId, boolean hasAttachments) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MESSAGE_HAS_ATTACHMENTS, hasAttachments ? 1 : 0);
        db.update(TABLE_MESSAGES, values, COLUMN_MESSAGE_LOCAL_ID + "=?", new String[]{String.valueOf(localId)});
        db.close();
    }

    public VaultMessage getMessage(long localId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MESSAGES, null, COLUMN_MESSAGE_LOCAL_ID + "=?",
                new String[]{String.valueOf(localId)}, null, null, null);
        VaultMessage message = null;
        if (cursor.moveToFirst()) {
            message = readMessageFromCursor(cursor);
        }
        cursor.close();
        db.close();
        return message;
    }

    public List<VaultMessage> getAllMessages() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MESSAGES, null, null, null, null, null,
                COLUMN_MESSAGE_DATE + " DESC");
        List<VaultMessage> messages = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                messages.add(readMessageFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return messages;
    }

    public long insertAttachment(MessageAttachment attachment) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ATTACHMENT_MESSAGE_ID, attachment.getMessageLocalId());
        values.put(COLUMN_ATTACHMENT_PROVIDER_PART_ID, attachment.getProviderPartId());
        values.put(COLUMN_ATTACHMENT_PATH, attachment.getFilePath());
        values.put(COLUMN_ATTACHMENT_CONTENT_TYPE, attachment.getContentType());
        values.put(COLUMN_ATTACHMENT_SIZE, attachment.getSizeBytes());
        long id = db.insert(TABLE_ATTACHMENTS, null, values);
        db.close();
        return id;
    }

    private boolean existingMessageHasAttachments(SQLiteDatabase db, long localId) {
        Cursor cursor = db.query(TABLE_MESSAGES, new String[]{COLUMN_MESSAGE_HAS_ATTACHMENTS},
                COLUMN_MESSAGE_LOCAL_ID + "=?", new String[]{String.valueOf(localId)}, null, null, null);
        boolean hasAttachments = false;
        if (cursor.moveToFirst()) {
            hasAttachments = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_HAS_ATTACHMENTS)) == 1;
        }
        cursor.close();
        return hasAttachments;
    }

    public boolean attachmentExists(long messageLocalId, String providerPartId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTACHMENTS, new String[]{COLUMN_ATTACHMENT_ID},
                COLUMN_ATTACHMENT_MESSAGE_ID + "=? AND " + COLUMN_ATTACHMENT_PROVIDER_PART_ID + "=?",
                new String[]{String.valueOf(messageLocalId), providerPartId}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    public List<MessageAttachment> getAttachmentsForMessage(long messageLocalId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTACHMENTS, null,
                COLUMN_ATTACHMENT_MESSAGE_ID + "=?",
                new String[]{String.valueOf(messageLocalId)}, null, null, null);
        List<MessageAttachment> attachments = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                attachments.add(readAttachmentFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return attachments;
    }

    private long getExistingMessageId(SQLiteDatabase db, long providerId, String providerType) {
        Cursor cursor = db.query(TABLE_MESSAGES, new String[]{COLUMN_MESSAGE_LOCAL_ID},
                COLUMN_MESSAGE_PROVIDER_ID + "=? AND " + COLUMN_MESSAGE_PROVIDER_TYPE + "=?",
                new String[]{String.valueOf(providerId), providerType}, null, null, null);
        long id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_LOCAL_ID));
        }
        cursor.close();
        return id;
    }

    private VaultMessage readMessageFromCursor(Cursor cursor) {
        long localId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_LOCAL_ID));
        long providerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_PROVIDER_ID));
        String providerType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_PROVIDER_TYPE));
        String address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_ADDRESS));
        long date = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_DATE));
        String body = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_BODY));
        int boxType = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_BOX_TYPE));
        boolean hasAttachments = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_HAS_ATTACHMENTS)) == 1;
        return new VaultMessage(localId, providerId, providerType, address, date, body, boxType, hasAttachments);
    }

    private MessageAttachment readAttachmentFromCursor(Cursor cursor) {
        long attachmentId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ATTACHMENT_ID));
        long messageId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ATTACHMENT_MESSAGE_ID));
        String providerPartId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ATTACHMENT_PROVIDER_PART_ID));
        String filePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ATTACHMENT_PATH));
        String contentType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ATTACHMENT_CONTENT_TYPE));
        long size = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ATTACHMENT_SIZE));
        return new MessageAttachment(attachmentId, messageId, providerPartId, filePath, contentType, size);
    }
}
