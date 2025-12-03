package com.coffre.fort;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;
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
        sendEmail(context, subject, body, (List<EmailAttachment>) null);
    }

    public static void sendEmail(Context context, String subject, String body, Document attachmentSource) {
        List<EmailAttachment> attachments = null;
        EmailConfigManager configManager = new EmailConfigManager(context);
        if (!configManager.isConfigured()) {
            Log.w(TAG, "Email configuration missing; skipping email send");
            return;
        }

        if (attachmentSource != null && attachmentSource.hasAttachment() && attachmentSource.getAttachmentUri() != null) {
            String mimeType = TextUtils.isEmpty(attachmentSource.getAttachmentMimeType())
                    ? "application/octet-stream"
                    : attachmentSource.getAttachmentMimeType();
            String fileName = TextUtils.isEmpty(attachmentSource.getAttachmentName())
                    ? "piece_jointe"
                    : attachmentSource.getAttachmentName();
            attachments = Collections.singletonList(new EmailAttachment(
                    Uri.parse(attachmentSource.getAttachmentUri()),
                    mimeType,
                    fileName
            ));
        }

        sendEmail(context, subject, body, attachments);
    }

    public static void sendEmail(Context context, String subject, String body, List<EmailAttachment> attachments) {
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

                MimeMultipart multipart = new MimeMultipart();
                boolean hasAttachments = attachments != null && !attachments.isEmpty();

                if (hasAttachments) {
                    MimeBodyPart textPart = new MimeBodyPart();
                    textPart.setText(safeBody);
                    multipart.addBodyPart(textPart);

                    for (EmailAttachment attachment : attachments) {
                        MimeBodyPart attachmentPart = buildAttachmentPart(context, attachment);
                        if (attachmentPart != null) {
                            multipart.addBodyPart(attachmentPart);
                        }
                    }
                }

                if (multipart.getCount() > 0) {
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

    private static MimeBodyPart buildAttachmentPart(Context context, EmailAttachment attachment) throws Exception {
        if (attachment == null || attachment.getUri() == null) {
            return null;
        }

        Uri uri = attachment.getUri();
        InputStream inputStream = null;
        try {
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                inputStream = context.getContentResolver().openInputStream(uri);
            } else {
                File file = ContentResolver.SCHEME_FILE.equals(uri.getScheme())
                        ? new File(uri.getPath())
                        : new File(uri.toString());
                if (file.exists()) {
                    inputStream = new FileInputStream(file);
                }
            }

            if (inputStream == null) {
                Log.w(TAG, "Unable to open attachment stream for URI: " + uri);
                return null;
            }

            String mimeType = TextUtils.isEmpty(attachment.getMimeType())
                    ? "application/octet-stream"
                    : attachment.getMimeType();
            DataSource dataSource = new ByteArrayDataSource(inputStream, mimeType);

            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.setDataHandler(new DataHandler(dataSource));
            String fileName = TextUtils.isEmpty(attachment.getDisplayName())
                    ? "piece_jointe"
                    : attachment.getDisplayName();
            attachmentPart.setFileName(fileName);
            return attachmentPart;
        } catch (IOException e) {
            Log.e(TAG, "Failed to read attachment from URI: " + uri, e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                    // Ignored
                }
            }
        }
    }

    public static class EmailAttachment {
        private final Uri uri;
        private final String mimeType;
        private final String displayName;

        public EmailAttachment(Uri uri, String mimeType, String displayName) {
            this.uri = uri;
            this.mimeType = mimeType;
            this.displayName = displayName;
        }

        public Uri getUri() {
            return uri;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
