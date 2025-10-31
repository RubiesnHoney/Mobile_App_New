package API;

public class AssetSyncResponse {
    private String id;  // Server asset ID
    private String status;
    private boolean verified;
    private String message;

    public AssetSyncResponse() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}