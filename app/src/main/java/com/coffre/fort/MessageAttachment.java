package com.coffre.fort;

public class MessageAttachment {
    private final long attachmentId;
    private final long messageLocalId;
    private final String providerPartId;
    private final String filePath;
    private final String contentType;
    private final long sizeBytes;

    public MessageAttachment(long messageLocalId, String providerPartId, String filePath, String contentType, long sizeBytes) {
        this(-1, messageLocalId, providerPartId, filePath, contentType, sizeBytes);
    }

    public MessageAttachment(long attachmentId, long messageLocalId, String providerPartId, String filePath, String contentType, long sizeBytes) {
        this.attachmentId = attachmentId;
        this.messageLocalId = messageLocalId;
        this.providerPartId = providerPartId;
        this.filePath = filePath;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public long getAttachmentId() {
        return attachmentId;
    }

    public long getMessageLocalId() {
        return messageLocalId;
    }

    public String getProviderPartId() {
        return providerPartId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
