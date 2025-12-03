package com.coffre.fort;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import android.webkit.MimeTypeMap;

import java.io.File;

import java.util.Locale;

public final class AttachmentUtils {
    private AttachmentUtils() {
    }

    public static String getDisplayName(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        String displayName = null;
        Cursor cursor = context.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    displayName = cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        if (displayName == null) {
            displayName = uri.getLastPathSegment();
        }
        return displayName;
    }

    public static long getFileSize(Context context, Uri uri) {
        if (uri == null) {
            return -1L;
        }

        long size = -1L;
        Cursor cursor = context.getContentResolver().query(uri,
                new String[]{OpenableColumns.SIZE}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        if (size < 0 && "file".equalsIgnoreCase(uri.getScheme())) {
            File file = new File(uri.getPath());
            if (file.exists()) {
                size = file.length();
            }
        }
        return size;
    }

    public static String formatFileSize(long sizeBytes) {
        if (sizeBytes < 0) {
            return null;
        }
        final long kilo = 1024L;
        final long mega = kilo * 1024L;
        final long giga = mega * 1024L;

        if (sizeBytes >= giga) {
            return String.format(Locale.getDefault(), "%.2f Go", (double) sizeBytes / giga);
        } else if (sizeBytes >= mega) {
            return String.format(Locale.getDefault(), "%.2f Mo", (double) sizeBytes / mega);
        } else if (sizeBytes >= kilo) {
            return String.format(Locale.getDefault(), "%.2f Ko", (double) sizeBytes / kilo);
        }
        return String.format(Locale.getDefault(), "%d octets", sizeBytes);
    }

    public static String getFileExtension(String fileName, String mimeType) {
        if (!TextUtils.isEmpty(fileName) && fileName.contains(".")) {
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
            if (!TextUtils.isEmpty(extension)) {
                return extension.toUpperCase(Locale.getDefault());
            }
        }
        if (!TextUtils.isEmpty(mimeType)) {
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (!TextUtils.isEmpty(extension)) {
                return extension.toUpperCase(Locale.getDefault());
            }
        }
        return null;
    }
}
