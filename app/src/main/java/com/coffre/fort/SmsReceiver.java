package com.coffre.fort;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.TextUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        PendingResult pendingResult = goAsync();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                processIncomingSms(context.getApplicationContext(), bundle);
            } finally {
                executor.shutdown();
                pendingResult.finish();
            }
        });
    }

    private void processIncomingSms(Context context, Bundle bundle) {
        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) {
            return;
        }
        String format = bundle.getString("format");
        StringBuilder messageBody = new StringBuilder();
        String sender = null;

        for (Object pdu : pdus) {
            SmsMessage smsMessage;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
            }
            if (sender == null) {
                sender = smsMessage.getDisplayOriginatingAddress();
            }
            messageBody.append(smsMessage.getMessageBody());
        }

        if (TextUtils.isEmpty(sender)) {
            sender = context.getString(R.string.sms_document_title_unknown);
        }

        Document document = new Document();
        document.setTitle(context.getString(R.string.sms_document_title, sender));
        document.setCategory(context.getString(R.string.category_sms));
        document.setContent(messageBody.toString());
        document.setTimestamp(System.currentTimeMillis());

        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        databaseHelper.addDocument(document);
        databaseHelper.close();

        sendEmailNotification(context, sender, messageBody.toString());
    }

    private void sendEmailNotification(Context context, String sender, String messageBody) {
        EmailConfigManager configManager = new EmailConfigManager(context);
        if (!configManager.isConfigured()) {
            return;
        }

        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        String formattedDate = dateFormat.format(new Date());
        String subject = context.getString(R.string.sms_email_subject, sender);
        String body = context.getString(R.string.sms_email_body, sender, formattedDate, messageBody);
        EmailSender.sendEmail(context, subject, body);
    }
}
