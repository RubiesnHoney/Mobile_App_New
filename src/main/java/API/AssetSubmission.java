package API;


public class AssetSubmission {

    private String assetTag;
    private String location;
    private String condition;
    private String notes;
    private String imageBase64;
    private long timestamp;

    public AssetSubmission() {}

    public AssetSubmission(String assetTag, String location, String condition,
                           String notes, String imageBase64) {
        this.assetTag = assetTag;
        this.location = location;
        this.condition = condition;
        this.notes = notes;
        this.imageBase64 = imageBase64;
        this.timestamp = System.currentTimeMillis();
}
    // Getters and Setters
    public String getAssetTag() { return assetTag; }
    public void setAssetTag(String assetTag) { this.assetTag = assetTag; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}



