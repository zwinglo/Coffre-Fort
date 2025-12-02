package com.coffre.fort;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

public final class EmailSender {
    private static final String TAG = "EmailSender";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private EmailSender() {
    }

    public static void sendEmail(Context context, String subject, String body) {
        sendEmail(context, subject, body, null);
    }

    public static void sendEmail(Context context, String subject, String body, Document attachmentSource) {
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

                String username = Objects.requireNonNull(configManager.getUsername(), "Username must not be null");

                String safeSubject = (subject == null || subject.trim().isEmpty())
                        ? "(Sans objet)"
                        : subject.trim();
                String safeBody = (body == null || body.trim().isEmpty())
                        ? "(Message vide)"
                        : body.trim();

                message.setFrom(new InternetAddress(username));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(configManager.getRecipient()));
                message.setSubject(safeSubject);

                if (attachmentSource != null && attachmentSource.hasAttachment()) {
                    MimeBodyPart textPart = new MimeBodyPart();
                    textPart.setText(safeBody);

                    MimeMultipart multipart = new MimeMultipart();
                    multipart.addBodyPart(textPart);

                    MimeBodyPart attachmentPart = buildAttachmentPart(context, attachmentSource);
                    if (attachmentPart != null) {
                        multipart.addBodyPart(attachmentPart);
                    }

                    message.setContent(multipart);
                } else {
                    message.setText(safeBody);
                }

                Transport.send(message);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send email", e);
            }
        });
    }

    private static MimeBodyPart buildAttachmentPart(Context context, Document attachmentSource) throws Exception {
        if (!attachmentSource.hasAttachment() || attachmentSource.getAttachmentUri() == null) {
            return null;
        }

        Uri uri = Uri.parse(attachmentSource.getAttachmentUri());
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                Log.w(TAG, "Unable to open attachment stream for URI: " + uri);
                return null;
            }

            String mimeType = TextUtils.isEmpty(attachmentSource.getAttachmentMimeType())
                    ? "application/octet-stream"
                    : attachmentSource.getAttachmentMimeType();
            DataSource dataSource = new ByteArrayDataSource(inputStream, mimeType);

            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.setDataHandler(new DataHandler(dataSource));
            String fileName = TextUtils.isEmpty(attachmentSource.getAttachmentName())
                    ? "piece_jointe"
                    : attachmentSource.getAttachmentName();
            attachmentPart.setFileName(fileName);
            return attachmentPart;
        } catch (IOException e) {
            Log.e(TAG, "Failed to read attachment from URI: " + uri, e);
            return null;
        }
    }
}
