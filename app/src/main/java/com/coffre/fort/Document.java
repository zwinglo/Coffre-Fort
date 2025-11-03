package com.coffre.fort;

public class Document {
    private int id;
    private String title;
    private String content;
    private String category;
    private long timestamp;
    private String attachmentUri;
    private String attachmentMimeType;
    private String attachmentName;

    public Document() {
        this.timestamp = System.currentTimeMillis();
    }

    public Document(int id, String title, String content, String category, long timestamp,
                    String attachmentUri, String attachmentMimeType, String attachmentName) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.timestamp = timestamp;
        this.attachmentUri = attachmentUri;
        this.attachmentMimeType = attachmentMimeType;
        this.attachmentName = attachmentName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getAttachmentUri() {
        return attachmentUri;
    }

    public void setAttachmentUri(String attachmentUri) {
        this.attachmentUri = attachmentUri;
    }

    public String getAttachmentMimeType() {
        return attachmentMimeType;
    }

    public void setAttachmentMimeType(String attachmentMimeType) {
        this.attachmentMimeType = attachmentMimeType;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public boolean hasAttachment() {
        return attachmentUri != null && !attachmentUri.isEmpty();
    }
}
