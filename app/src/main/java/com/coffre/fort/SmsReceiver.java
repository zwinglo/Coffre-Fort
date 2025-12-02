package com.coffre.fort;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.TextUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmsReceiver extends BroadcastReceiver {

    public static final String ACTION_SMS_SAVED = "com.coffre.fort.ACTION_SMS_SAVED";

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
        long messageTimestamp = 0L;

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
            if (messageTimestamp == 0L) {
                messageTimestamp = smsMessage.getTimestampMillis();
            }
            messageBody.append(smsMessage.getMessageBody());
        }

        if (TextUtils.isEmpty(sender)) {
            sender = context.getString(R.string.sms_document_title_unknown);
        }

        Document document = new Document();
        document.setTitle(context.getString(R.string.message_document_title, sender));
        document.setCategory(context.getString(R.string.category_messages));
        document.setContent(MessageFormatter.buildDocumentContent(context, sender, messageTimestamp, messageBody.toString()));
        document.setTimestamp(messageTimestamp == 0L ? System.currentTimeMillis() : messageTimestamp);

        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        long documentId = databaseHelper.addDocument(document);
        databaseHelper.close();
        document.setId((int) documentId);

        MessageEmailDispatcher.dispatch(context, document, sender, MessageEmailDispatcher.MessageType.SMS);

        Intent refreshIntent = new Intent(ACTION_SMS_SAVED);
        refreshIntent.setPackage(context.getPackageName());
        refreshIntent.putExtra("document_id", document.getId());
        context.sendBroadcast(refreshIntent);
    }

}
