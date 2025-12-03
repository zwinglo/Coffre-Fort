package com.coffre.fort;

import android.content.Context;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MessageEmailDispatcher {

    private MessageEmailDispatcher() {
    }

    public enum MessageType {
        SMS,
        MMS,
        CHAT
    }

    public static void dispatch(Context context, Document document, String sender, MessageType type,
                                 String messageBody, long timestamp) {
        EmailConfigManager configManager = new EmailConfigManager(context);
        if (!configManager.isConfigured()) {
            return;
        }

        String safeSender = TextUtils.isEmpty(sender)
                ? context.getString(R.string.sms_document_title_unknown)
                : sender;

        String subject = generateSubject(context, type);
        long safeTimestamp = timestamp == 0L ? document.getTimestamp() : timestamp;
        String body = MessageFormatter.buildEmailBody(context, safeSender, safeTimestamp, messageBody);

        EmailSender.sendEmail(context, subject, body, document);
    }

    private static String generateSubject(Context context, MessageType type) {
        String label;
        if (type == MessageType.MMS) {
            label = context.getString(R.string.message_type_mms);
        } else if (type == MessageType.CHAT) {
            label = context.getString(R.string.message_type_chat);
        } else {
            label = context.getString(R.string.message_type_sms);
        }

        String datePart = new SimpleDateFormat("ddMMyy", Locale.getDefault()).format(new Date());
        String suffix = String.format(Locale.getDefault(), "%02d", (System.currentTimeMillis() / 1000) % 100);
        return String.format(Locale.getDefault(), "%s - %s%s", label, datePart, suffix);
    }
}
