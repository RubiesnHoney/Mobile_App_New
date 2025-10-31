package com.kkgare.campustag;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import database.AppDatabase;
import database.AssetEntity;
import API.AssetSubmission;
import API.SubmissionResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import API.ApiClient;

public class AssetFormActivity extends AppCompatActivity {
    private static final String TAG = "AssetFormActivity";
    private static final int CAMERA_REQUEST = 1888;

    private TextView assetTagText;
    private Spinner locationSpinner;
    private Spinner conditionSpinner;
    private ImageView assetImageView;
    private EditText notesEdit;
    private Button capturePhotoButton;
    private Button submitButton;
    private String assetTag;
    private Bitmap capturedImage;
    private boolean useCustomLayout = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Try to load the custom layout first
            setContentView(R.layout.activity_asset_form);
            initializeViews();
            Log.d(TAG, "Custom layout loaded successfully");
        } catch (Exception e) {
            Log.w(TAG, "Custom layout failed, creating programmatic layout", e);
            createProgrammaticLayout();
            useCustomLayout = false;
        }

        setupData();
        setupListeners();
    }

    private void initializeViews() throws Exception {
        assetTagText = findViewById(R.id.asset_tag_text);
        locationSpinner = findViewById(R.id.location_spinner);
        conditionSpinner = findViewById(R.id.condition_spinner);
        assetImageView = findViewById(R.id.asset_image_view);
        notesEdit = findViewById(R.id.notes_edit);
        capturePhotoButton = findViewById(R.id.capture_photo_button);
        submitButton = findViewById(R.id.submit_button);

        // Check if any view is null
        if (assetTagText == null || locationSpinner == null || conditionSpinner == null ||
                assetImageView == null || notesEdit == null || capturePhotoButton == null || submitButton == null) {
            throw new Exception("One or more views not found in layout");
        }

        useCustomLayout = true;
    }

    private void createProgrammaticLayout() {
        try {
            LinearLayout mainLayout = new LinearLayout(this);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setPadding(32, 32, 32, 32);

            // Asset tag text
            assetTagText = new TextView(this);
            assetTagText.setText("Asset Tag: Loading...");
            assetTagText.setTextSize(18);
            assetTagText.setPadding(0, 16, 0, 16);
            mainLayout.addView(assetTagText);

            // Location spinner
            locationSpinner = new Spinner(this);
            locationSpinner.setPadding(0, 16, 0, 16);
            mainLayout.addView(locationSpinner);

            // Condition spinner
            conditionSpinner = new Spinner(this);
            conditionSpinner.setPadding(0, 16, 0, 16);
            mainLayout.addView(conditionSpinner);

            // Image view
            assetImageView = new ImageView(this);
            assetImageView.setLayoutParams(new LinearLayout.LayoutParams(300, 300));
            assetImageView.setBackgroundColor(0xFFE0E0E0);
            assetImageView.setPadding(0, 16, 0, 16);
            mainLayout.addView(assetImageView);

            // Notes edit
            notesEdit = new EditText(this);
            notesEdit.setHint("Enter notes here...");
            notesEdit.setPadding(16, 16, 16, 16);
            notesEdit.setMinLines(3);
            mainLayout.addView(notesEdit);

            // Capture photo button
            capturePhotoButton = new Button(this);
            capturePhotoButton.setText("Capture Photo");
            capturePhotoButton.setPadding(0, 16, 0, 16);
            mainLayout.addView(capturePhotoButton);

            // Submit button
            submitButton = new Button(this);
            submitButton.setText("Submit Verification");
            submitButton.setPadding(0, 32, 0, 16);
            mainLayout.addView(submitButton);

            setContentView(mainLayout);

            Toast.makeText(this, "Using basic layout - custom layout not found", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Failed to create programmatic layout", e);
            // Last resort - create minimal layout
            createMinimalLayout();
        }
    }

    private void createMinimalLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        assetTagText = new TextView(this);
        assetTagText.setText("Asset Form (Minimal Mode)");
        assetTagText.setTextSize(18);
        layout.addView(assetTagText);

        submitButton = new Button(this);
        submitButton.setText("Submit (Basic)");
        submitButton.setPadding(0, 32, 0, 16);
        layout.addView(submitButton);

        setContentView(layout);

        Toast.makeText(this, "Minimal mode - please check your layout files", Toast.LENGTH_LONG).show();
    }

    private void setupData() {
        try {
            // Setup asset tag
            assetTag = getIntent().getStringExtra("asset_tag");
            if (assetTag == null || assetTag.isEmpty()) {
                assetTag = "AST-" + System.currentTimeMillis();
            }

            if (assetTagText != null) {
                assetTagText.setText("Asset Tag: " + assetTag);
            }

            // Setup spinners only if they exist
            if (locationSpinner != null) {
                setupLocationSpinner();
            }

            if (conditionSpinner != null) {
                setupConditionSpinner();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up data", e);
            Toast.makeText(this, "Warning: Some features may not work properly", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupLocationSpinner() {
        try {
            String[] locations = {
                    "Select Location",
                    "Building A - Floor 1",
                    "Building A - Floor 2",
                    "Building B - Floor 1",
                    "Building B - Floor 2",
                    "Library",
                    "Cafeteria",
                    "Gymnasium",
                    "Laboratory",
                    "Office"
            };

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    locations
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            locationSpinner.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up location spinner", e);
        }
    }

    private void setupConditionSpinner() {
        try {
            String[] conditions = {
                    "Select Condition",
                    "Excellent",
                    "Good",
                    "Fair",
                    "Poor",
                    "Damaged",
                    "Needs Repair"
            };

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    conditions
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            conditionSpinner.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up condition spinner", e);
        }
    }

    private void setupListeners() {
        try {
            if (capturePhotoButton != null) {
                capturePhotoButton.setOnClickListener(v -> capturePhoto());
            }

            if (submitButton != null) {
                submitButton.setOnClickListener(v -> submitAssetVerification());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up listeners", e);
        }
    }

    private void capturePhoto() {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error capturing photo", e);
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK && data != null) {
            try {
                capturedImage = (Bitmap) data.getExtras().get("data");
                if (capturedImage != null && assetImageView != null) {
                    assetImageView.setImageBitmap(capturedImage);
                    assetImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing camera result", e);
                Toast.makeText(this, "Error processing photo", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void submitAssetVerification() {
        Log.d(TAG, "Submit button clicked");

        try {
            // Disable button immediately to prevent double clicks
            if (submitButton != null) {
                submitButton.setEnabled(false);
                submitButton.setText("Submitting...");
            }

            // Get form data with safe defaults
            String location = getSpinnerValue(locationSpinner, "Unknown Location");
            String condition = getSpinnerValue(conditionSpinner, "Unknown Condition");
            String notes = getEditTextValue(notesEdit, "");

            // Basic validation
            if (useCustomLayout && locationSpinner != null && locationSpinner.getSelectedItemPosition() == 0) {
                showMessage("Please select a location");
                resetSubmitButton();
                return;
            }

            if (useCustomLayout && conditionSpinner != null && conditionSpinner.getSelectedItemPosition() == 0) {
                showMessage("Please select a condition");
                resetSubmitButton();
                return;
            }

            Log.d(TAG, "Submitting: " + assetTag + ", " + location + ", " + condition);

            // Save to local database with image
            saveAssetToDatabase(location, condition, notes);

            // Check internet and sync if available
            if (isInternetAvailable()) {
                syncAssetToServer(location, condition, notes);
            }

            // Navigate to confirmation
            navigateToConfirmation(location, condition, notes);

        } catch (Exception e) {
            Log.e(TAG, "Error saving asset", e);
            showMessage("Error saving asset: " + e.getMessage());
            resetSubmitButton();
        }
    }

    private void saveAssetToDatabase(String location, String condition, String notes) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);

                // Save the captured image to internal storage and get the path
                String imagePath = null;
                if (capturedImage != null) {
                    imagePath = saveImageToInternalStorage(capturedImage);
                }

                // Create new asset entity
                AssetEntity asset = new AssetEntity(
                        assetTag,                           // assetTag
                        "Asset - " + assetTag,              // name
                        location,                           // location
                        condition,                          // condition
                        notes,                              // notes
                        imagePath                           // imagePath
                );

                // Insert into database
                long assetId = db.assetDao().insertAsset(asset);

                Log.d(TAG, "Asset saved to database with ID: " + assetId);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Asset saved locally", Toast.LENGTH_SHORT).show();
                });

                // Upload to server if we have image and internet
                if (true) {
                    com.kkgare.campustag.AssetUploader uploader = new com.kkgare.campustag.AssetUploader();
                    File imageFile = null;

                    if (imagePath != null){
                        imageFile = new File(imagePath);
                    }

                    if (true) {
                        uploader.uploadAsset(
                                assetTag,
                                "Asset - " + assetTag,
                                location,
                                condition,
                                location,
                                notes,
                                "Mobile App",
                                imageFile,
                                new com.kkgare.campustag.AssetUploader.UploadCallback() {
                                    @Override
                                    public void onSuccess(String response) {
                                        Log.d(TAG, "Upload successful: " + response);

                                        // Mark as synced in database
                                        try {
                                            asset.setSynced(true);
                                            db.assetDao().updateAsset(asset);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error updating sync status", e);
                                        }

                                        runOnUiThread(() ->
                                                Toast.makeText(AssetFormActivity.this,
                                                        "Uploaded to server", Toast.LENGTH_SHORT).show()
                                        );
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "Upload failed: " + error);
                                        runOnUiThread(() ->
                                                Toast.makeText(AssetFormActivity.this,
                                                        "Upload failed - saved locally", Toast.LENGTH_SHORT).show()
                                        );
                                    }
                                }
                        );
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error saving to database", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error saving asset: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // Your existing saveImageToInternalStorage is good, but add this verification:

    private String saveImageToInternalStorage(Bitmap bitmap) {
        try {
            // Create a file name using timestamp
            String fileName = "asset_" + assetTag + "_" + System.currentTimeMillis() + ".jpg";

            // Get the app's internal storage directory
            File directory = getFilesDir();
            File imageFile = new File(directory, fileName);

            // Compress and save the bitmap
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.close();

            // Verify file was created
            if (imageFile.exists() && imageFile.length() > 0) {
                Log.d(TAG, "Image saved successfully: " + imageFile.getAbsolutePath());
                return imageFile.getAbsolutePath();
            } else {
                Log.e(TAG, "Image file not created properly");
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
    }

    private boolean isInternetAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking internet", e);
        }
        return false;
    }

    private void syncAssetToServer(String location, String condition, String notes) {
        // Convert bitmap to base64
        String imageBase64 = "";
        if (capturedImage != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                capturedImage.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] imageBytes = baos.toByteArray();
                imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            } catch (Exception e) {
                Log.e(TAG, "Error encoding image", e);
            }
        }

        AssetSubmission submission = new AssetSubmission(
                assetTag,
                location,
                condition,
                notes,
                imageBase64
        );

        ApiClient.getApiService().submitAsset(submission)
                .enqueue(new Callback<SubmissionResponse>() {
                    @Override
                    public void onResponse(Call<SubmissionResponse> call,
                                           Response<SubmissionResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.body().isSuccess()) {
                                Log.d(TAG, "Asset synced successfully to server");

                                // Update the asset in database to mark as synced
                                new Thread(() -> {
                                    try {
                                        AppDatabase db = AppDatabase.getInstance(AssetFormActivity.this);
                                        AssetEntity asset = db.assetDao().getAssetByTag(assetTag);
                                        if (asset != null) {
                                            asset.setSynced(true);
                                            db.assetDao().updateAsset(asset);
                                            Log.d(TAG, "Asset marked as synced in database");
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error updating sync status", e);
                                    }
                                }).start();

                                runOnUiThread(() -> {
                                    Toast.makeText(AssetFormActivity.this,
                                            "Asset synced to server", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<SubmissionResponse> call, Throwable t) {
                        Log.e(TAG, "Sync failed: " + t.getMessage(), t);
                        runOnUiThread(() -> {
                            Toast.makeText(AssetFormActivity.this,
                                    "Sync failed - will retry later", Toast.LENGTH_SHORT).show();
                        });
                        // Asset remains unsynced in database
                    }
                });
    }

    private String getSpinnerValue(Spinner spinner, String defaultValue) {
        try {
            if (spinner != null && spinner.getSelectedItem() != null) {
                return spinner.getSelectedItem().toString();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting spinner value", e);
        }
        return defaultValue;
    }

    private String getEditTextValue(EditText editText, String defaultValue) {
        try {
            if (editText != null && editText.getText() != null) {
                return editText.getText().toString().trim();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting edit text value", e);
        }
        return defaultValue;
    }

    private void navigateToConfirmation(String location, String condition, String notes) {
        try {
            Intent intent = new Intent(this, ConfirmationActivity.class);
            intent.putExtra("asset_tag", assetTag);
            intent.putExtra("location", location);
            intent.putExtra("condition", condition);
            intent.putExtra("notes", notes);

            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
            new Handler().postDelayed(this::finish, 100);

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to confirmation", e);
            showMessage("Navigation error. Data saved locally.");
            resetSubmitButton();
        }
    }

    private void showMessage(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing message: " + message, e);
        }
    }

    private void resetSubmitButton() {
        try {
            if (submitButton != null) {
                submitButton.setEnabled(true);
                submitButton.setText("Submit Verification");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resetting button", e);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        try {
            onBackPressed();
        } catch (Exception e) {
            Log.e(TAG, "Error on back pressed", e);
            finish();
        }
        return true;
    }
}
