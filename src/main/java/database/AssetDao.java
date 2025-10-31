package database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface AssetDao {

    // Existing queries
    @Query("SELECT * FROM assets ORDER BY timestamp DESC")
    List<AssetEntity> getAllAssets();

    @Query("SELECT * FROM assets WHERE synced = 0")
    List<AssetEntity> getUnsyncedAssets();

    @Query("SELECT * FROM assets WHERE assetTag LIKE :search OR location LIKE :search")
    List<AssetEntity> searchAssets(String search);

    @Insert
    long insertAsset(AssetEntity asset);

    @Update
    void updateAsset(AssetEntity asset);

    @Delete
    void deleteAsset(AssetEntity asset);

    @Query("DELETE FROM assets")
    void clearAllAssets();

    @Query("SELECT COUNT(*) FROM assets WHERE synced = 0")
    int getUnsyncedCount();

    // NEW: Sync-related queries
    @Query("UPDATE assets SET synced = :synced, serverAssetId = :serverId, lastSyncAttempt = :timestamp WHERE id = :assetId")
    void updateSyncStatus(int assetId, boolean synced, String serverId, long timestamp);

    @Query("UPDATE assets SET verified = :verified WHERE serverAssetId = :serverAssetId")
    void updateVerificationStatus(String serverAssetId, boolean verified);

    @Query("UPDATE assets SET syncError = :error, lastSyncAttempt = :timestamp WHERE id = :assetId")
    void updateSyncError(int assetId, String error, long timestamp);

    @Query("DELETE FROM assets WHERE verified = 1 AND synced = 1")
    int deleteVerifiedAssets();

    @Query("SELECT * FROM assets WHERE id = :assetId")
    AssetEntity getAssetById(int assetId);

    @Query("SELECT * FROM assets WHERE assetTag = :assetTag LIMIT 1")
    AssetEntity getAssetByTag(String assetTag);
}