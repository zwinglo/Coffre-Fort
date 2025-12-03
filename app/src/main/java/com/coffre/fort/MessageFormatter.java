package com.coffre.fort;

import android.content.Context;
import android.text.TextUtils;

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

    public static String buildEmailBody(Context context, String sender, long timestamp, String messageBody) {
        String safeSender = TextUtils.isEmpty(sender)
                ? context.getString(R.string.sms_document_title_unknown)
                : sender;
        String safeBody = TextUtils.isEmpty(messageBody)
                ? context.getString(R.string.sms_email_empty_body_placeholder)
                : messageBody;
        String formattedDate = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                .format(new Date(timestamp == 0L ? System.currentTimeMillis() : timestamp));
        return context.getString(R.string.sms_email_body, safeSender, formattedDate, safeBody);
    }

    public static String formatTimestamp(long timestamp) {
        long safeTimestamp = timestamp == 0L ? System.currentTimeMillis() : timestamp;
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                .format(new Date(safeTimestamp));
    }
}
