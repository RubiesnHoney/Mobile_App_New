package com.kkgare.campustag;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class AssetUploader {

    private static final String TAG = "AssetUploader";
    private static final String BASE_URL = "http://campustag.runasp.net/api/Assets";

    /**
     * Upload asset with image file
     */
    public void uploadAsset(String tag, String name, String description,
                            String condition, String room, String notes,
                            String submittedBy, File imageFile, UploadCallback callback) {

        new Thread(() -> {
            try {
                String urlString = BASE_URL + "?Tag=" + urlEncode(tag) +
                        "&Name=" + urlEncode(name) +
                        "&Description=" + urlEncode(description) +
                        "&Condition=" + urlEncode(condition) +
                        "&Room=" + urlEncode(room) +
                        "&Notes=" + urlEncode(notes) +
                        "&SubmittedBy=" + urlEncode(submittedBy);

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

                String boundary = "Boundary-" + UUID.randomUUID().toString();
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setRequestProperty("Accept", "text/plain");

                OutputStream out = conn.getOutputStream();

                if (imageFile != null && imageFile.exists()) {
                    writeFilePart(out, boundary, "formFile", imageFile);
                }

                out.write(("\r\n--" + boundary + "--\r\n").getBytes());
                out.flush();
                out.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readResponse(conn);
                    callback.onSuccess(response);
                    Log.d(TAG, "Upload successful: " + response);
                } else {
                    String errorBody = readErrorResponse(conn);
                    callback.onError("HTTP Error: " + responseCode + " - " + errorBody);
                    Log.e(TAG, "Upload failed with code: " + responseCode);
                }

                conn.disconnect();

            } catch (Exception e) {
                callback.onError(e.getMessage());
                Log.e(TAG, "Upload error", e);
            }
        }).start();
    }


    public void uploadAssetWithBase64(String tag, String name, String description,
                                      String condition, String room, String notes,
                                      String submittedBy, String imageBase64,
                                      UploadCallback callback) {

        new Thread(() -> {
            try {
                String urlString = BASE_URL + "?Tag=" + urlEncode(tag) +
                        "&Name=" + urlEncode(name) +
                        "&Description=" + urlEncode(description) +
                        "&Condition=" + urlEncode(condition) +
                        "&Room=" + urlEncode(room) +
                        "&Notes=" + urlEncode(notes) +
                        "&SubmittedBy=" + urlEncode(submittedBy) +
                        "&ImageBase64=" + urlEncode(imageBase64);

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "text/plain");
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readResponse(conn);
                    callback.onSuccess(response);
                    Log.d(TAG, "Upload successful: " + response);
                } else {
                    String errorBody = readErrorResponse(conn);
                    callback.onError("HTTP Error: " + responseCode + " - " + errorBody);
                    Log.e(TAG, "Upload failed with code: " + responseCode);
                }

                conn.disconnect();

            } catch (Exception e) {
                callback.onError(e.getMessage());
                Log.e(TAG, "Upload error", e);
            }
        }).start();
    }

    public String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }

    public String fileToBase64(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = fis.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }
        fis.close();
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    private void writeFilePart(OutputStream out, String boundary, String fieldName,
                               File file) throws IOException {
        String filename = file.getName();
        String mimeType = getMimeType(filename);

        out.write(("--" + boundary + "\r\n").getBytes());
        out.write(("Content-Disposition: form-data; name=\"" + fieldName +
                "\"; filename=\"" + filename + "\"\r\n").getBytes());
        out.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes());

        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int len;
        while ((len = fis.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
        fis.close();
    }

    private String getMimeType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            default:
                return "application/octet-stream";
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        java.io.InputStream is = conn.getInputStream();
        java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

    private String readErrorResponse(HttpURLConnection conn) {
        try {
            java.io.InputStream is = conn.getErrorStream();
            if (is == null) return "No error details";
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private String urlEncode(String value) {
        if (value == null) return "";
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    public interface UploadCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}