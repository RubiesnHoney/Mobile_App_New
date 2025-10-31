package com.kkgare.campustag;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity{

    private SwitchCompat autoSyncSwitch; // Was: private Switch autoSyncSwitch;
    private SwitchCompat notificationSwitch;
    private TextView versionText;
    private Button logoutButton;
    private Button clearDataButton;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
        { getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings"); }

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        initializeViews();
        loadPreferences();
        setupClickListeners();
    }

    private void initializeViews() {
        autoSyncSwitch = findViewById(R.id.auto_sync_switch);
        notificationSwitch = findViewById(R.id.notification_switch);
        versionText = findViewById(R.id.version_text);
        logoutButton = findViewById(R.id.logout_button);
        clearDataButton = findViewById(R.id.clear_data_button);


        if (autoSyncSwitch == null || notificationSwitch == null || versionText == null || logoutButton == null || clearDataButton == null)
        { Toast.makeText(this, "Layout error: Missing views", Toast.LENGTH_LONG).show();
            finish(); return; }

        // Set version info
        versionText.setText("Version 1.0.0");
    }

    private void loadPreferences() {
        autoSyncSwitch.setChecked(preferences.getBoolean("auto_sync", true));
        notificationSwitch.setChecked(preferences.getBoolean("notifications", true));
    }

    private void setupClickListeners() {
        autoSyncSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("auto_sync", isChecked).apply();
            Toast.makeText(this, "Auto-sync " + (isChecked ? "enabled" : "disabled"),
                    Toast.LENGTH_SHORT).show();
        });

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("notifications", isChecked).apply();
            Toast.makeText(this, "Notifications " + (isChecked ? "enabled" : "disabled"),
                    Toast.LENGTH_SHORT).show();
        });

        logoutButton.setOnClickListener(v -> showLogoutDialog());
        clearDataButton.setOnClickListener(v -> showClearDataDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Clear user session
                    preferences.edit().clear().apply();
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showClearDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Data")
                .setMessage("This will delete all local data. Are you sure?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Clear local database (implement with Room)
                    Toast.makeText(this, "Local data cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}
