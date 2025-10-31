package com.kkgare.campustag;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import database.AppDatabase;
import database.AssetEntity;

public class AssetListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private AssetAdapter adapter;
    private List<Asset> allAssets;
    private List<Asset> filteredAssets;
    private EditText searchEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_list);

        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Assets");
        }

        // Initialize views
        recyclerView = findViewById(R.id.assets_recycler_view);
        searchEdit = findViewById(R.id.search_edit);

        setupRecyclerView();
        loadAssets();
        setupSearch();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        allAssets = new ArrayList<>();
        filteredAssets = new ArrayList<>();
        adapter = new AssetAdapter(filteredAssets);
        recyclerView.setAdapter(adapter);
    }

    private void loadAssets() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<AssetEntity> entities = db.assetDao().getAllAssets();

            runOnUiThread(() -> {
                allAssets.clear();
                for (AssetEntity entity : entities) {
                    allAssets.add(new Asset(
                            entity.getAssetTag(),
                            entity.getName(),
                            entity.getLocation(),
                            entity.getCondition()
                    ));
                }
                filteredAssets.clear();
                filteredAssets.addAll(allAssets);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void setupSearch() {
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    private void performSearch(String searchText) {
        filteredAssets.clear();

        if (searchText == null || searchText.trim().isEmpty()) {
            // Show all assets if search is empty
            filteredAssets.addAll(allAssets);
        } else {
            String searchLower = searchText.toLowerCase().trim();

            // Filter assets based on all their properties
            for (Asset asset : allAssets) {
                if (assetMatchesSearch(asset, searchLower)) {
                    filteredAssets.add(asset);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private boolean assetMatchesSearch(Asset asset, String searchText) {
        // Create a searchable string from all asset properties
        // Assuming your Asset constructor is: Asset(id, name, location, condition)
        // You may need to adjust this based on your actual Asset class structure

        try {
            // Get asset data - you'll need to replace these with your actual field access
            String assetData = "";

            // Method 1: If your Asset has public fields
            // Uncomment and modify these lines based on your Asset class:
            //assetData = (asset.id + " " + asset.name + " " + asset.location + " " + asset.condition).toLowerCase();

            // Method 2: If your Asset has getter methods
            // Uncomment and modify these lines based on your Asset class:
            //assetData = (asset.getId() + " " + asset.getName() + " " + asset.getLocation() + " " + asset.getCondition()).toLowerCase();

            // Method 3: Using toString() method (temporary solution)
           assetData = asset.toString().toLowerCase();

            return assetData.contains(searchText);

        } catch (Exception e) {
            // Fallback: just return false if there's any issue
            return false;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}