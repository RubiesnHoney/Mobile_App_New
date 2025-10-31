package database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "assets")
public class AssetEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String assetTag;
    private String name;
    private String location;
    private String condition;
    private String notes;
    private String imagePath;
    private long timestamp;

    // NEW: Sync-related fields
    private boolean synced;
    private boolean verified;  // NEW: Whether admin has verified
    private String serverAssetId;  // NEW: ID from server
    private Long lastSyncAttempt;  // NEW: Last sync attempt time
    private String syncError;  // NEW: Error message if sync failed

    public AssetEntity() {}

    public AssetEntity(String assetTag, String name, String location, String condition, String notes, String imagePath) {
        this.assetTag = assetTag;
        this.name = name;
        this.location = location;
        this.condition = condition;
        this.notes = notes;
        this.imagePath = imagePath;
        this.synced = false;
        this.verified = false;  // NEW
        this.timestamp = System.currentTimeMillis();
    }

    // Existing Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAssetTag() { return assetTag; }
    public void setAssetTag(String assetTag) { this.assetTag = assetTag; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // NEW: Getters and Setters for sync fields
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public String getServerAssetId() { return serverAssetId; }
    public void setServerAssetId(String serverAssetId) { this.serverAssetId = serverAssetId; }

    public Long getLastSyncAttempt() { return lastSyncAttempt; }
    public void setLastSyncAttempt(Long lastSyncAttempt) { this.lastSyncAttempt = lastSyncAttempt; }

    public String getSyncError() { return syncError; }
    public void setSyncError(String syncError) { this.syncError = syncError; }
}