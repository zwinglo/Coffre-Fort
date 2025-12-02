package com.coffre.fort;

import android.content.Context;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public final class MessageFormatter {

    private MessageFormatter() {
    }

    public static String buildDocumentContent(Context context, String sender, long timestamp, String messageBody) {
        String safeBody = messageBody == null ? "" : messageBody;
        String formattedDate = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                .format(new Date(timestamp == 0L ? System.currentTimeMillis() : timestamp));
        return context.getString(R.string.message_document_content, sender, formattedDate, safeBody);
    }
}
