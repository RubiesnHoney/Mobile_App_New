package API;

import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public interface ApiService {

    // Existing methods
    @GET("api/assets")
    Call<List<AssetResponse>> getAssets();

    @POST("api/assets/submit")
    Call<SubmissionResponse> submitAsset(@Body AssetSubmission submission);

    @GET("api/assets/search")
    Call<List<AssetResponse>> searchAssets(@Query("q") String query);

    // NEW: Sync asset with multipart (image + data)
    @Multipart
    @POST("api/assets/submit")
    Call<AssetSyncResponse> submitAssetWithImage(
            @Part("qr_code") RequestBody qrCode,
            @Part("name") RequestBody name,
            @Part("description") RequestBody description,
            @Part("category") RequestBody category,
            @Part("location") RequestBody location,
            @Part("condition") RequestBody condition,
            @Part("notes") RequestBody notes,
            @Part MultipartBody.Part image
    );

    // NEW: Check asset verification status
    @GET("api/assets/{id}/status")
    Call<SyncStatusResponse> checkAssetStatus(@Path("id") String assetId);

    // NEW: Batch check verification status
    @GET("api/assets/sync")
    Call<List<SyncStatusResponse>> syncAssets(
            @Query("device_id") String deviceId,
            @Query("last_sync") Long lastSync
    );
}