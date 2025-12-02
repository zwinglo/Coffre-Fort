package com.coffre.fort;

import android.content.Context;
import android.text.TextUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class SmsEmailDispatcher {

    private SmsEmailDispatcher() {
    }

    public static void dispatch(Context context, Document document, String sender) {
        EmailConfigManager configManager = new EmailConfigManager(context);
        if (!configManager.isConfigured()) {
            return;
        }

        String safeSender = TextUtils.isEmpty(sender)
                ? context.getString(R.string.sms_document_title_unknown)
                : sender;

        String subject = generateSubject();
        String formattedDate = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(document.getTimestamp()));
        String content = TextUtils.isEmpty(document.getContent())
                ? context.getString(R.string.sms_email_empty_body_placeholder)
                : document.getContent();
        String body = context.getString(R.string.sms_email_body, safeSender, formattedDate, content);

        EmailSender.sendEmail(context, subject, body, document);
    }

    private static String generateSubject() {
        String datePart = new SimpleDateFormat("ddMMyy", Locale.getDefault()).format(new Date());
        String suffix = String.format(Locale.getDefault(), "%02d", (System.currentTimeMillis() / 1000) % 100);
        return String.format(Locale.getDefault(), "SMS - %s%s", datePart, suffix);
    }
}
