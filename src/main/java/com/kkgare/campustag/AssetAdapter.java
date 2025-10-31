package com.kkgare.campustag;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AssetAdapter extends RecyclerView.Adapter<AssetAdapter.AssetViewHolder> {

    private List<Asset> assets;

    public AssetAdapter(List<Asset> assets) {
        this.assets = assets;
    }

    @NonNull
    @Override
    public AssetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_asset, parent, false);
        return new AssetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssetViewHolder holder, int position) {
        Asset asset = assets.get(position);
        holder.bind(asset);
    }

    @Override
    public int getItemCount() {
        return assets.size();
    }

    class AssetViewHolder extends RecyclerView.ViewHolder {
        TextView tagText;
        TextView nameText;
        TextView locationText;
        TextView statusText;

        AssetViewHolder(View itemView) {
            super(itemView);
            tagText = itemView.findViewById(R.id.asset_tag_text);
            nameText = itemView.findViewById(R.id.asset_name_text);
            locationText = itemView.findViewById(R.id.asset_location_text);
            statusText = itemView.findViewById(R.id.asset_status_text);
        }

        void bind(Asset asset) {
            tagText.setText(asset.getTag());
            nameText.setText(asset.getName());
            locationText.setText(asset.getLocation());
            statusText.setText(asset.getCondition());

            // Set status color based on condition
            int colorRes;
            switch (asset.getCondition().toLowerCase()) {
                case "good":
                    colorRes = R.color.status_good;
                    break;
                case "fair":
                    colorRes = R.color.status_fair;
                    break;
                case "poor":
                    colorRes = R.color.status_poor;
                    break;
                default:
                    colorRes = R.color.status_good;
            }
            statusText.setBackgroundResource(colorRes);
        }
    }
}
