package com.kkgare.campustag;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ConfirmationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        // FIXED: Set up toolbar safely - check for null ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Submission Complete");
        }
        // REMOVED: This duplicate line was causing the crash
        // getSupportActionBar().setTitle("Submission Complete");

        TextView assetTagText = findViewById(R.id.summary_asset_tag);
        TextView locationText = findViewById(R.id.summary_location);
        TextView conditionText = findViewById(R.id.summary_condition);
        Button backToMenuButton = findViewById(R.id.back_to_menu_button);
        Button scanAnotherButton = findViewById(R.id.scan_another_button);

        // Get data from intent
        String assetTag = getIntent().getStringExtra("asset_tag");
        String location = getIntent().getStringExtra("location");
        String condition = getIntent().getStringExtra("condition");

        // FIXED: Add null checks to prevent crashes if data is missing
        if (assetTagText != null) {
            assetTagText.setText("Asset: " + (assetTag != null ? assetTag : "Unknown"));
        }

        if (locationText != null) {
            locationText.setText("Location: " + (location != null ? location : "Unknown"));
        }

        if (conditionText != null) {
            conditionText.setText("Condition: " + (condition != null ? condition : "Unknown"));
        }

        // FIXED: Add null checks for buttons
        if (backToMenuButton != null) {
            backToMenuButton.setOnClickListener(v -> {
                Intent intent = new Intent(ConfirmationActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }

        if (scanAnotherButton != null) {
            scanAnotherButton.setOnClickListener(v -> {
                Intent intent = new Intent(ConfirmationActivity.this, QRScanActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent intent = new Intent(ConfirmationActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        return true;
    }
}