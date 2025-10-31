package com.kkgare.campustag;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import database.AppDatabase;

public class MainActivity extends AppCompatActivity {

    private AssetSyncService syncService;
    private AppDatabase database;
    private TextView unsyncedBadge;
    private Handler handler;
    private Runnable updateBadgeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize services
        syncService = new AssetSyncService(this);
        database = AppDatabase.getInstance(this);
        handler = new Handler();

        // Initialize views
        CardView scanAssetCard = findViewById(R.id.scan_asset_card);
        CardView assetListCard = findViewById(R.id.asset_list_card);
        CardView syncDataCard = findViewById(R.id.sync_data_card);
        CardView settingsCard = findViewById(R.id.settings_card);

        // Set up click listeners
        scanAssetCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, QRScanActivity.class);
            startActivity(intent);
        });

        assetListCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AssetListActivity.class);
            startActivity(intent);
        });

        syncDataCard.setOnClickListener(v -> performManualSync());

        settingsCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Auto-sync on startup if network available
        autoSyncIfPossible();

        // Start updating badge
        startBadgeUpdater();
    }

    private void startBadgeUpdater() {
        updateBadgeRunnable = new Runnable() {
            @Override
            public void run() {
                updateUnsyncedBadge();
                handler.postDelayed(this, 2000); // Update every 2 seconds
            }
        };
        handler.post(updateBadgeRunnable);
    }

    private void updateUnsyncedBadge() {
        new Thread(() -> {
            try {
                int count = database.assetDao().getUnsyncedCount();
                runOnUiThread(() -> {
                    // You can add a badge view to your sync card in the layout
                    // For now, we'll just log it
                    if (count > 0) {
                        // Update UI to show pending count
                        // Toast removed to avoid annoying users
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void autoSyncIfPossible() {
        new Thread(() -> {
            try {
                if (syncService.isNetworkAvailable()) {
                    int unsyncedCount = database.assetDao().getUnsyncedCount();

                    if (unsyncedCount > 0) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                    "Auto-syncing " + unsyncedCount + " pending assets...",
                                    Toast.LENGTH_SHORT).show();
                        });

                        syncService.syncAssets(new AssetSyncService.SyncCallback() {
                            @Override
                            public void onProgress(int current, int total, String assetName) {
                                // Silent auto-sync
                            }

                            @Override
                            public void onComplete(AssetSyncService.SyncResult result) {
                                runOnUiThread(() -> {
                                    if (result.successCount > 0) {
                                        Toast.makeText(MainActivity.this,
                                                "Auto-synced " + result.successCount + " assets",
                                                Toast.LENGTH_SHORT).show();

                                        // NEW: After sync, check for verified assets and auto-delete
                                        autoCheckAndCleanup();
                                    }
                                });
                            }

                            @Override
                            public void onError(String error) {
                                // Silent fail for auto-sync
                            }
                        });
                    } else {
                        // No pending uploads, but check for verified assets to cleanup
                        autoCheckAndCleanup();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // NEW: Add this method to MainActivity
    private void autoCheckAndCleanup() {
        syncService.autoCleanupVerifiedAssets(new AssetSyncService.VerificationCallback() {
            @Override
            public void onComplete(int verifiedCount) {
                if (verifiedCount > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                verifiedCount + " verified assets cleaned up",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                // Silent fail for auto-cleanup
                Log.d("MainActivity", "Auto-cleanup check failed: " + error);
            }
        });
    }

    private void performManualSync() {
        if (!syncService.isNetworkAvailable()) {
            new AlertDialog.Builder(this)
                    .setTitle("No Internet Connection")
                    .setMessage("Please connect to the internet to sync your assets.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Sync Assets")
                .setMessage("This will upload all pending assets to the server and check verification status. Continue?")
                .setPositiveButton("Sync", (dialog, which) -> performFullSync())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performFullSync() {
        // Show progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Syncing...")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        syncService.performFullSync(new AssetSyncService.FullSyncCallback() {
            @Override
            public void onProgress(String status) {
                runOnUiThread(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.setMessage(status);
                    }
                });
            }

            @Override
            public void onComplete(AssetSyncService.FullSyncResult result) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();

                    StringBuilder message = new StringBuilder();
                    message.append("Sync Complete!\n\n");

                    if (result.uploadedCount > 0) {
                        message.append("✓ Uploaded: ").append(result.uploadedCount).append(" assets\n");
                    }
                    if (result.verifiedCount > 0) {
                        message.append("✓ Verified: ").append(result.verifiedCount).append(" assets\n");
                    }
                    if (result.cleanedCount > 0) {
                        message.append("✓ Cleaned: ").append(result.cleanedCount).append(" assets");
                    }
                    if (result.uploadedCount == 0 && result.verifiedCount == 0) {
                        message.append("All assets are up to date!");
                    }

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(result.success ? "Sync Successful" : "Sync Failed")
                            .setMessage(result.success ? message.toString() : result.message)
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh when returning to activity
        updateUnsyncedBadge();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop badge updater
        if (handler != null && updateBadgeRunnable != null) {
            handler.removeCallbacks(updateBadgeRunnable);
        }
    }
}