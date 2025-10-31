package API;

public class SyncStatusResponse {
    private String assetId;  // Server asset ID
    private boolean verified;
    private boolean synced;

    public SyncStatusResponse() {}

    // Getters and Setters
    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }
}