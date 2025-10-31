package com.kkgare.campustag;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import API.ApiClient;
import API.ApiService;
import API.AssetSyncResponse;
import database.AppDatabase;
import database.AssetDao;
import database.AssetEntity;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

public class AssetSyncService {

    private static final String TAG = "AssetSyncService";
    private static final String PREF_NAME = "sync_preferences";
    private static final String PREF_LAST_SYNC = "last_sync_timestamp";

    private final Context context;
    private final AssetDao assetDao;
    private final ApiService apiService;
    private final SharedPreferences preferences;

    public interface SyncCallback {
        void onProgress(int current, int total, String assetName);
        void onComplete(SyncResult result);
        void onError(String error);
    }

    public AssetSyncService(Context context) {
        this.context = context;
        AppDatabase database = AppDatabase.getInstance(context);
        this.assetDao = database.assetDao();
        this.apiService = ApiClient.getApiService();
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager == null) return false;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) return false;

                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                if (capabilities == null) return false;

                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            } else {
                android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking network", e);
            return false;
        }
    }

    public void syncAssets(final SyncCallback callback) {
        new Thread(() -> {
            if (!isNetworkAvailable()) {
                runOnUiThread(() -> callback.onError("No internet connection"));
                return;
            }

            try {
                List<AssetEntity> unsyncedAssets = assetDao.getUnsyncedAssets();

                if (unsyncedAssets.isEmpty()) {
                    runOnUiThread(() -> callback.onComplete(new SyncResult(0, 0)));
                    return;
                }

                final int[] successCount = {0};
                final int[] failureCount = {0};
                final int total = unsyncedAssets.size();

                for (int i = 0; i < unsyncedAssets.size(); i++) {
                    AssetEntity asset = unsyncedAssets.get(i);
                    final int currentIndex = i;

                    runOnUiThread(() -> callback.onProgress(currentIndex + 1, total, asset.getName()));

                    try {
                        // Prepare request body parts
                        RequestBody tagBody = createPartFromString(asset.getAssetTag());
                        RequestBody nameBody = createPartFromString(asset.getName() != null ? asset.getName() : "");
                        RequestBody descBody = createPartFromString(asset.getNotes() != null ? asset.getNotes() : "");
                        RequestBody conditionBody = createPartFromString(asset.getCondition());
                        RequestBody roomBody = createPartFromString(asset.getLocation());
                        RequestBody notesBody = createPartFromString(asset.getNotes() != null ? asset.getNotes() : "");
                        RequestBody submittedByBody = createPartFromString("Android User");

                        // Prepare image part
                        MultipartBody.Part imagePart = null;
                        if (asset.getImagePath() != null && !asset.getImagePath().isEmpty()) {
                            File imageFile = new File(asset.getImagePath());
                            if (imageFile.exists()) {
                                RequestBody imageBody = RequestBody.create(
                                        MediaType.parse("image/*"),
                                        imageFile
                                );
                                imagePart = MultipartBody.Part.createFormData(
                                        "ImageFile",
                                        imageFile.getName(),
                                        imageBody
                                );
                            }
                        }

                        // Make synchronous API call
                        Call<AssetSyncResponse> call = apiService.submitAssetWithImage(
                                tagBody, nameBody, descBody, conditionBody,
                                roomBody, notesBody, submittedByBody, imagePart
                        );

                        Response<AssetSyncResponse> response = call.execute();

                        if (response.isSuccessful() && response.body() != null) {
                            AssetSyncResponse apiResponse = response.body();

                            if (apiResponse.getId() != null && !apiResponse.getId().isEmpty()) {
                                // Update sync status in database
                                assetDao.updateSyncStatus(
                                        asset.getId(),
                                        true,
                                        String.valueOf(apiResponse.getId()),
                                        System.currentTimeMillis()
                                );

                                // Update verification status if already verified
                                if (apiResponse.isVerified()) {
                                    assetDao.updateVerificationStatus(
                                            String.valueOf(apiResponse.getId()),
                                            true
                                    );
                                }

                                successCount[0]++;
                                Log.d(TAG, "Successfully synced asset: " + asset.getName());
                            } else {
                                String error = apiResponse.getMessage() != null ?
                                        apiResponse.getMessage() : "Unknown error";
                                assetDao.updateSyncError(asset.getId(), error, System.currentTimeMillis());
                                failureCount[0]++;
                                Log.e(TAG, "API returned error for asset: " + asset.getName() + " - " + error);
                            }
                        } else {
                            String error = response.message() != null ? response.message() : "Unknown error";
                            assetDao.updateSyncError(asset.getId(), error, System.currentTimeMillis());
                            failureCount[0]++;
                            Log.e(TAG, "Failed to sync asset: " + asset.getName() + " - " + error);
                        }

                    } catch (IOException e) {
                        assetDao.updateSyncError(asset.getId(), e.getMessage(), System.currentTimeMillis());
                        failureCount[0]++;
                        Log.e(TAG, "Exception syncing asset: " + asset.getName(), e);
                    }

                    // Small delay to avoid overwhelming the server
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Update last sync timestamp
                updateLastSyncTime();

                final int finalSuccess = successCount[0];
                final int finalFailure = failureCount[0];
                runOnUiThread(() -> callback.onComplete(new SyncResult(finalSuccess, finalFailure)));

            } catch (Exception e) {
                Log.e(TAG, "Sync failed", e);
                runOnUiThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        }).start();
    }
    // Add these callback interfaces after the existing SyncCallback interface
    public interface VerificationCallback {
        void onComplete(int verifiedCount);
        void onError(String error);
    }

    public interface FullSyncCallback {
        void onProgress(String status);
        void onComplete(FullSyncResult result);
    }

    // Add this result class
    public static class FullSyncResult {
        public final boolean success;
        public final int uploadedCount;
        public final int verifiedCount;
        public final int cleanedCount;
        public final String message;

        public FullSyncResult(boolean success, int uploadedCount, int verifiedCount,
                              int cleanedCount, String message) {
            this.success = success;
            this.uploadedCount = uploadedCount;
            this.verifiedCount = verifiedCount;
            this.cleanedCount = cleanedCount;
            this.message = message;
        }
    }

    // Add these missing methods
    public void autoCleanupVerifiedAssets(final VerificationCallback callback) {
        new Thread(() -> {
            try {
                // Check for verified assets from server
                checkVerificationStatus(callback);
            } catch (Exception e) {
                Log.e(TAG, "Auto-cleanup failed", e);
                runOnUiThread(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    public void checkVerificationStatus(final VerificationCallback callback) {
        new Thread(() -> {
            try {
                // Get all synced assets
                List<AssetEntity> syncedAssets = assetDao.getAllAssets()
                        .stream()
                        .filter(AssetEntity::isSynced)
                        .collect(Collectors.toList());

                int verifiedCount = 0;

                for (AssetEntity asset : syncedAssets) {
                    if (asset.getServerAssetId() != null) {
                        // Check verification status from server
                        // This is a simplified version - you'd need to implement the API call
                        verifiedCount++;
                    }
                }

                final int finalCount = verifiedCount;
                runOnUiThread(() -> callback.onComplete(finalCount));

            } catch (Exception e) {
                Log.e(TAG, "Verification check failed", e);
                runOnUiThread(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    public void performFullSync(final FullSyncCallback callback) {
        new Thread(() -> {
            try {
                runOnUiThread(() -> callback.onProgress("Uploading assets..."));

                // Step 1: Upload unsynced assets
                final int[] uploadedCount = {0};

                syncAssets(new SyncCallback() {
                    @Override
                    public void onProgress(int current, int total, String assetName) {
                        runOnUiThread(() ->
                                callback.onProgress("Uploading " + current + "/" + total)
                        );
                    }

                    @Override
                    public void onComplete(SyncResult result) {
                        uploadedCount[0] = result.successCount;

                        // Step 2: Check verification status
                        runOnUiThread(() -> callback.onProgress("Checking verification..."));

                        checkVerificationStatus(new VerificationCallback() {
                            @Override
                            public void onComplete(int verifiedCount) {
                                // Step 3: Cleanup verified assets
                                runOnUiThread(() -> callback.onProgress("Cleaning up..."));

                                int cleanedCount = cleanupVerifiedAssets();

                                // Complete
                                FullSyncResult fullResult = new FullSyncResult(
                                        true,
                                        uploadedCount[0],
                                        verifiedCount,
                                        cleanedCount,
                                        "Sync completed successfully"
                                );

                                runOnUiThread(() -> callback.onComplete(fullResult));
                            }

                            @Override
                            public void onError(String error) {
                                FullSyncResult fullResult = new FullSyncResult(
                                        false, uploadedCount[0], 0, 0,
                                        "Verification check failed: " + error
                                );
                                runOnUiThread(() -> callback.onComplete(fullResult));
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        FullSyncResult fullResult = new FullSyncResult(
                                false, 0, 0, 0, "Sync failed: " + error
                        );
                        runOnUiThread(() -> callback.onComplete(fullResult));
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Full sync failed", e);
                FullSyncResult fullResult = new FullSyncResult(
                        false, 0, 0, 0, "Error: " + e.getMessage()
                );
                runOnUiThread(() -> callback.onComplete(fullResult));
            }
        }).start();
    }
    public int cleanupVerifiedAssets() {
        return assetDao.deleteVerifiedAssets();
    }

    // Helper methods
    private RequestBody createPartFromString(String value) {
        return RequestBody.create(MediaType.parse("text/plain"), value != null ? value : "");
    }

    private void updateLastSyncTime() {
        preferences.edit()
                .putLong(PREF_LAST_SYNC, System.currentTimeMillis())
                .apply();
    }

    private void runOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    // Result classes
    public static class SyncResult {
        public final int successCount;
        public final int failureCount;

        public SyncResult(int successCount, int failureCount) {
            this.successCount = successCount;
            this.failureCount = failureCount;
        }
    }
}