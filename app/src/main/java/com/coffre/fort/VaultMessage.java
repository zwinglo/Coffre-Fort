package com.coffre.fort;

public class VaultMessage {
    private long localId;
    private final long providerId;
    private final String providerType;
    private final String address;
    private final long date;
    private final String body;
    private final int boxType;
    private final boolean hasAttachments;

    public VaultMessage(long providerId, String providerType, String address, long date, String body, int boxType, boolean hasAttachments) {
        this(-1, providerId, providerType, address, date, body, boxType, hasAttachments);
    }

    public VaultMessage(long localId, long providerId, String providerType, String address, long date, String body, int boxType, boolean hasAttachments) {
        this.localId = localId;
        this.providerId = providerId;
        this.providerType = providerType;
        this.address = address;
        this.date = date;
        this.body = body;
        this.boxType = boxType;
        this.hasAttachments = hasAttachments;
    }

    public long getLocalId() {
        return localId;
    }

    public void setLocalId(long localId) {
        this.localId = localId;
    }

    public long getProviderId() {
        return providerId;
    }

    public String getProviderType() {
        return providerType;
    }

    public String getAddress() {
        return address;
    }

    public long getDate() {
        return date;
    }

    public String getBody() {
        return body;
    }

    public int getBoxType() {
        return boxType;
    }

    public boolean hasAttachments() {
        return hasAttachments;
    }
}
