package com.kkgare.campustag;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import database.AppDatabase;
import database.AssetEntity;

public class SyncActivity extends AppCompatActivity {

    private static final String TAG = "SyncActivity";

    private TextView pendingSubmissionsText;
    private TextView pendingPhotosText;
    private TextView lastSyncText;
    private Button syncNowButton;
    private Button autoSyncButton;
    private ProgressBar syncProgressBar;
    private TextView syncStatusText;

    private AssetSyncService syncService;
    private AppDatabase database;
    private Handler handler;
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Data Synchronization");
        }

        // Initialize services
        syncService = new AssetSyncService(this);
        database = AppDatabase.getInstance(this);
        handler = new Handler();

        initializeViews();
        loadSyncStatus();

        // Set up periodic updates
        startStatusUpdater();

        syncNowButton.setOnClickListener(v -> performSync());
        autoSyncButton.setOnClickListener(v -> toggleAutoSync());
    }

    private void initializeViews() {
        pendingSubmissionsText = findViewById(R.id.pending_submissions_text);
        pendingPhotosText = findViewById(R.id.pending_photos_text);
        lastSyncText = findViewById(R.id.last_sync_text);
        syncNowButton = findViewById(R.id.sync_now_button);
        autoSyncButton = findViewById(R.id.auto_sync_button);

        // Add these to your layout XML if not present
        syncProgressBar = findViewById(R.id.sync_progress_bar);
        syncStatusText = findViewById(R.id.sync_status_text);

        if (syncProgressBar != null) syncProgressBar.setVisibility(View.GONE);
        if (syncStatusText != null) syncStatusText.setVisibility(View.GONE);

        // Load auto-sync button state
        updateAutoSyncButton();
    }

    private void startStatusUpdater() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                loadSyncStatus();
                handler.postDelayed(this, 3000); // Update every 3 seconds
            }
        };
        handler.post(updateRunnable);
    }

    private void loadSyncStatus() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                int unsyncedCount = db.assetDao().getUnsyncedCount();

                runOnUiThread(() -> {
                    pendingSubmissionsText.setText(String.valueOf(unsyncedCount));
                    pendingPhotosText.setText(String.valueOf(unsyncedCount));

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    long lastSync = prefs.getLong("last_sync", 0);
                    if (lastSync > 0) {
                        long diff = System.currentTimeMillis() - lastSync;
                        lastSyncText.setText(formatTimeDiff(diff));
                    } else {
                        lastSyncText.setText("Never");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading sync status", e);
            }
        }).start();
    }

    private String formatTimeDiff(long diffMillis) {
        long seconds = diffMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    private void performSync() {
        if (!isInternetAvailable()) {
            new AlertDialog.Builder(this)
                    .setTitle("No Internet Connection")
                    .setMessage("Please connect to the internet to sync your assets.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Show progress
        if (syncProgressBar != null) syncProgressBar.setVisibility(View.VISIBLE);
        if (syncStatusText != null) {
            syncStatusText.setVisibility(View.VISIBLE);
            syncStatusText.setText("Syncing...");
        }
        syncNowButton.setEnabled(false);

        syncService.syncAssets(new AssetSyncService.SyncCallback() {
            @Override
            public void onProgress(int current, int total, String assetName) {
                runOnUiThread(() -> {
                    if (syncStatusText != null) {
                        syncStatusText.setText("Syncing " + current + "/" + total + ": " + assetName);
                    }
                });
            }

            @Override
            public void onComplete(AssetSyncService.SyncResult result) {
                runOnUiThread(() -> {
                    // Hide progress
                    if (syncProgressBar != null) syncProgressBar.setVisibility(View.GONE);
                    if (syncStatusText != null) syncStatusText.setVisibility(View.GONE);
                    syncNowButton.setEnabled(true);

                    // Save sync timestamp
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SyncActivity.this);
                    prefs.edit().putLong("last_sync", System.currentTimeMillis()).apply();

                    // Show result
                    String message = "Sync completed!\n" +
                            "Success: " + result.successCount + "\n" +
                            "Failed: " + result.failureCount;

                    new AlertDialog.Builder(SyncActivity.this)
                            .setTitle("Sync Complete")
                            .setMessage(message)
                            .setPositiveButton("OK", null)
                            .show();

                    // Refresh status
                    loadSyncStatus();

                    // Auto-check for verified assets and cleanup
                    checkAndCleanupVerifiedAssets();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Hide progress
                    if (syncProgressBar != null) syncProgressBar.setVisibility(View.GONE);
                    if (syncStatusText != null) syncStatusText.setVisibility(View.GONE);
                    syncNowButton.setEnabled(true);

                    Toast.makeText(SyncActivity.this,
                            "Sync failed: " + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void checkAndCleanupVerifiedAssets() {
        syncService.checkVerificationStatus(new AssetSyncService.VerificationCallback() {
            @Override
            public void onComplete(int verifiedCount) {
                runOnUiThread(() -> {
                    if (verifiedCount > 0) {
                        // Delete verified assets
                        new Thread(() -> {
                            int deletedCount = syncService.cleanupVerifiedAssets();
                            runOnUiThread(() -> {
                                if (deletedCount > 0) {
                                    Toast.makeText(SyncActivity.this,
                                            deletedCount + " verified assets cleaned up",
                                            Toast.LENGTH_SHORT).show();
                                    loadSyncStatus();
                                }
                            });
                        }).start();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Verification check failed: " + error);
            }
        });
    }

    private void toggleAutoSync() {
        SharedPreferences prefs = getSharedPreferences("sync_preferences", MODE_PRIVATE);
        boolean currentState = prefs.getBoolean("auto_sync", true);
        boolean newState = !currentState;

        prefs.edit().putBoolean("auto_sync", newState).apply();

        updateAutoSyncButton();

        Toast.makeText(this, "Auto-sync " + (newState ? "enabled" : "disabled"),
                Toast.LENGTH_SHORT).show();
    }

    private void updateAutoSyncButton() {
        SharedPreferences prefs = getSharedPreferences("sync_preferences", MODE_PRIVATE);
        boolean autoSyncEnabled = prefs.getBoolean("auto_sync", true);

        if (autoSyncButton != null) {
            autoSyncButton.setText("Auto-Sync: " + (autoSyncEnabled ? "ON" : "OFF"));
        }
    }

    private boolean isInternetAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Network network = cm.getActiveNetwork();
                    if (network == null) return false;

                    NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                    return capabilities != null &&
                            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                } else {
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking internet", e);
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSyncStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}