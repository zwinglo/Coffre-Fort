package com.coffre.fort;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "coffre_fort.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_DOCUMENTS = "documents";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String TABLE_AUTH = "auth";
    private static final String COLUMN_PASSWORD = "password";

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
                + COLUMN_TIMESTAMP + " INTEGER)";
        db.execSQL(createDocumentsTable);

        String createAuthTable = "CREATE TABLE " + TABLE_AUTH + " ("
                + COLUMN_PASSWORD + " TEXT)";
        db.execSQL(createAuthTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOCUMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUTH);
        onCreate(db);
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
                Document document = new Document(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                );
                documents.add(document);
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
                Document document = new Document(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                );
                documents.add(document);
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
            document = new Document(
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
            );
        }
        cursor.close();
        db.close();
        return document;
    }

    public void deleteDocument(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DOCUMENTS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }
}
