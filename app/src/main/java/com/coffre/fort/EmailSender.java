package com.coffre.fort;

import android.content.Context;
import android.util.Log;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public final class EmailSender {
    private static final String TAG = "EmailSender";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private EmailSender() {
    }

    public static void sendEmail(Context context, String subject, String body) {
        EmailConfigManager configManager = new EmailConfigManager(context);
        if (!configManager.isConfigured()) {
            Log.w(TAG, "Email configuration missing; skipping email send");
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                Properties properties = new Properties();
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.host", configManager.getHost());
                properties.put("mail.smtp.port", String.valueOf(configManager.getPort()));
                properties.put("mail.smtp.starttls.enable", String.valueOf(configManager.isUseTls()));
                properties.put("mail.smtp.connectiontimeout", "10000");
                properties.put("mail.smtp.timeout", "10000");

                Session session = Session.getInstance(properties, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(configManager.getUsername(), configManager.getPassword());
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(configManager.getUsername()));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(configManager.getRecipient()));
                message.setSubject(subject);
                message.setText(body);

                Transport.send(message);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send email", e);
            }
        });
    }
}
