package com.example.app.services;

import android.os.Environment;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AppController {
    private static final String TAG = "AppController";
    private final StorageService storageService = new StorageService();

    @RequestMapping(path = "/api/app/export-localstorage", method = "POST")
    public ResponseContext exportLocalStorage(RequestContext request) {
        Log.i(TAG, " -> REST API: Triggering localstorage serialization export.");
        
        byte[] rawBody = request.getBody();
        if (rawBody.length == 0) {
            JSONObject errJson = new JSONObject();
            try {
                errJson.put("status", "error");
                errJson.put("message", "Payload packet empty.");
            } catch (JSONException ignored) {}

            return ResponseContext.status(400)
                    .body(errJson.toString())
                    .build();
        }

        try {
            String fileName = "ahm-localstorage-dump.json";
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadDir, fileName);
            
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            fos.write(rawBody);
            fos.close();
            
            android.media.MediaScannerConnection.scanFile(request.getAndroidContext(), new String[]{file.getAbsolutePath()}, null, null);
            
            JSONObject successJson = new JSONObject();
            successJson.put("status", "success");
            successJson.put("message", "Exported to downloads: " + fileName);

            return ResponseContext.status(200)
                    .body(successJson.toString())
                    .build();
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed executing export pipeline: " + e.getMessage());
            
            JSONObject errJson = new JSONObject();
            try {
                errJson.put("status", "error");
                errJson.put("message", "Failed saving file to disk.");
            } catch (JSONException ignored) {}

            return ResponseContext.status(500)
                    .body(errJson.toString())
                    .build();
        }
    }

    @RequestMapping(path = "/api/app/import-localstorage", method = "GET")
    public ResponseContext importLocalStorage(RequestContext request) {
        Log.i(TAG, " -> REST API: Accessing disk files to read storage backup.");
        try {
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadDir, "ahm-localstorage-dump.json");
            
            if (!file.exists() || !file.isFile()) {
                JSONObject missingJson = new JSONObject();
                missingJson.put("status", "error");
                missingJson.put("message", "Backup file 'ahm-localstorage-dump.json' not found in Downloads folder.");

                return ResponseContext.status(404)
                        .body(missingJson.toString())
                        .build();
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] fileBytes = new byte[(int) file.length()];
            int bytesRead = fis.read(fileBytes);
            fis.close();

            return ResponseContext.status(200)
                    .header("Cache-Control", "no-cache")
                    .body(fileBytes)
                    .build();
        } catch (IOException | JSONException e) {
            JSONObject errJson = new JSONObject();
            try {
                errJson.put("status", "error");
                errJson.put("message", "Internal storage read exception.");
            } catch (JSONException ignored) {}

            return ResponseContext.status(500)
                    .body(errJson.toString())
                    .build();
        }
    }

    // Demonstrates reading incoming JSON bodies and outputting structured org.json responses
    @RequestMapping(path = "/api/app/device-status", method = "GET")
    public ResponseContext getDeviceStatus(RequestContext request) {
        JSONObject responseJson = new JSONObject();
        
        try {
            responseJson.put("status", "active");
            responseJson.put("protocol", request.getHttpVersion());
            responseJson.put("userAgent", request.getHeader("User-Agent"));
            responseJson.put("domain", request.getDomain());
            
            // Example reading dynamic url path glob parameter e.g., if path was /api/app/device-status/{id}
            String pathId = request.getPathParam("id");
            if (pathId != null) {
                responseJson.put("requestedId", pathId);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON assembly tracking failed", e);
        }

        return ResponseContext.status(200)
                .header("X-Powered-By", "Android Native Framework Interceptor")
                .body(responseJson.toString())
                .build();
    }

    //--------------------------------------------------------------------------------
    //storage services
    //--------------------------------------------------------------------------------

    // Set a root storage sandbox folder inside Documents
    private File getStorageRoot() {
        //return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AppSandbox");
        return Environment.getExternalStorageDirectory();
    }

    /**
     * Helper to resolve path inputs while preventing path traversal attacks
     */
    private File resolveSafeFile(String relativePath) throws IOException {
        File root = getStorageRoot();
        if (!root.exists()) root.mkdirs();
        if (relativePath == null || relativePath.isEmpty()) return root;
        
        File target = new File(root, relativePath);
        if (!target.getCanonicalPath().startsWith(root.getCanonicalPath())) {
            throw new SecurityException("Directory traversal attack detected!");
        }
        return target;
    }

    // =========================================================================
    // READ OPERATIONS
    // =========================================================================

    @RequestMapping(path = "/api/fs/list", method = "GET")
    public ResponseContext listDirectory(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path"); // e.g., ?path=subfolder
            File targetDir = resolveSafeFile(pathQuery);
            
            JSONArray contents = storageService.readDirectory(targetDir);
            
            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("files", contents);
            
            return ResponseContext.status(200).body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(400, "Failed listing directory: " + e.getMessage());
        }
    }

    @RequestMapping(path = "/api/fs/read", method = "GET")
    public ResponseContext readFileContent(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path"); // e.g., ?path=notes/todo.txt
            File targetFile = resolveSafeFile(pathQuery);
            
            byte[] fileData = storageService.readFile(targetFile);
            String mimeType = "application/octet-stream";
            
            if (targetFile.getName().endsWith(".txt")) mimeType = "text/plain";
            if (targetFile.getName().endsWith(".json")) mimeType = "application/json";

            return ResponseContext.status(200)
                    .contentType(mimeType)
                    .body(fileData)
                    .build();
        } catch (Exception e) {
            return buildErrorResponse(404, "Failed reading file: " + e.getMessage());
        }
    }

    // =========================================================================
    // CREATE OPERATIONS
    // =========================================================================

    @RequestMapping(path = "/api/fs/mkdir", method = "POST")
    public ResponseContext createDirectory(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path"); // e.g., ?path=deep/nested/folder
            String recursiveStr = request.getQueryParam("recursive"); // e.g., ?recursive=true
            
            boolean recursive = "true".equalsIgnoreCase(recursiveStr);
            File targetDir = resolveSafeFile(pathQuery);
            
            boolean success = storageService.createDirectory(targetDir, recursive);
            
            JSONObject result = new JSONObject();
            result.put("status", success ? "success" : "error");
            result.put("message", success ? "Directory matched/created." : "Could not create directory structural layout.");
            
            return ResponseContext.status(success ? 200 : 500).body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(400, "Directory processing failure: " + e.getMessage());
        }
    }

    @RequestMapping(path = "/api/fs/write", method = "POST")
    public ResponseContext createOrWriteFile(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path"); // e.g., ?path=logs/session.json
            File targetFile = resolveSafeFile(pathQuery);
            
            // Uses request body payload
            byte[] dataPayload = request.getBody();
            storageService.createFile(targetFile, dataPayload);
            
            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("message", "File saved successfully: " + targetFile.getName());
            
            return ResponseContext.status(200).body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(500, "File persist error: " + e.getMessage());
        }
    }

    // =========================================================================
    // DELETE OPERATIONS
    // =========================================================================

    @RequestMapping(path = "/api/fs/delete", method = "DELETE")
    public ResponseContext deleteFileSystemPath(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path"); // e.g., ?path=deep/nested/folder
            String recursiveStr = request.getQueryParam("recursive"); // e.g., ?recursive=true
            
            boolean recursive = "true".equalsIgnoreCase(recursiveStr);
            File targetFile = resolveSafeFile(pathQuery);
            
            // Safety Check: Do not let API delete the root folder accidentally
            if (targetFile.getCanonicalPath().equals(getStorageRoot().getCanonicalPath())) {
                return buildErrorResponse(403, "Forbidden: Cannot delete the storage environment root context.");
            }

            boolean success = storageService.deletePath(targetFile, recursive);
            
            JSONObject result = new JSONObject();
            result.put("status", success ? "success" : "error");
            result.put("message", success ? "Deleted resource cleanly." : "Failed completely clearing resource targets.");
            
            return ResponseContext.status(success ? 200 : 500).body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(400, "Resource cleaning failure: " + e.getMessage());
        }
    }


    /**
     * Inline helper for generating uniform error messaging structures cleanly
     */
    private ResponseContext buildErrorResponse(int code, String message) {
        JSONObject errJson = new JSONObject();
        try {
            errJson.put("status", "error");
            errJson.put("message", message);
        } catch (Exception ignored) {}
        return ResponseContext.status(code).body(errJson.toString()).build();
    }
}

