//package com.example.app.services.maintenance;
//import android.content.Context;
//import android.os.Environment;
//import android.util.Log;
//import com.example.app.MainActivity;
//import com.example.app.AppConfig;
//import com.example.app.services.RequestMapping;
//import com.example.app.services.RequestContext;
//import com.example.app.services.ResponseContext;
//import com.example.app.services.WebServiceRegistry;
//import com.example.app.UpdateManager; 
//import com.example.app.StorageManager;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.json.JSONArray;
//import java.nio.charset.StandardCharsets;
//import com.example.app.services.StorageService;
//import android.os.StatFs;
//
//public class FsController {
//    private static final String TAG = "FsController";
//    private static volatile String currentStatusMessage = "Idle";
//    private final StorageService storageService = new StorageService();
//
//    public FsController () {
//    }
//
//    //--------------------------------------------------------------------------------
//    //storage services
//    //--------------------------------------------------------------------------------
//
//    // Set a root storage sandbox folder inside Documents
//    private File getStorageRoot() {
//        //return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AppSandbox");
//        return Environment.getExternalStorageDirectory();
//    }
//
//    /**
//     * Helper to resolve path inputs while preventing path traversal attacks
//     */
//    private File resolveSafeFile(String relativePath) throws IOException {
//        File root = getStorageRoot();
//        if (!root.exists()) root.mkdirs();
//        if (relativePath == null || relativePath.isEmpty()) return root;
//        
//        File target = new File(root, relativePath);
//        if (!target.getCanonicalPath().startsWith(root.getCanonicalPath())) {
//            throw new SecurityException("Directory traversal attack detected!");
//        }
//        return target;
//    }
//
//    // =========================================================================
//    // LOCATION OPERATIONS
//    // =========================================================================
//
//    @RequestMapping(path = "/api/fs/locations", method = "GET")
//    public ResponseContext getStorageLocations(RequestContext request) {
//        try {
//            Context context = request.getAndroidContext();
//            JSONObject locations = new JSONObject();
//
//            // 1. Resolve Shared External Storage path (dynamically derived from your layout)
//            File externalRoot = getStorageRoot();
//            locations.put("external_storage_root", externalRoot.getAbsolutePath());
//
//            // 2. Resolve Private Sandbox Database directory safely
//            if (context != null) {
//                // Returns /data/user/0/com.example.app/databases
//                File sandboxDbDir = context.getDatabasePath("probe.db").getParentFile();
//                if (sandboxDbDir != null) {
//                    if (!sandboxDbDir.exists()) sandboxDbDir.mkdirs();
//                    locations.put("sandbox_databases_root", sandboxDbDir.getAbsolutePath());
//                } else {
//                    locations.put("sandbox_databases_root", "unknown_path_error");
//                }
//                locations.put("package_name", context.getPackageName());
//            } else {
//                locations.put("sandbox_databases_root", "context_unavailable");
//            }
//
//            JSONObject result = new JSONObject();
//            result.put("status", "success");
//            result.put("locations", locations);
//
//            return ResponseContext.status(200)
//                    .contentType("application/json")
//                    .header("X-Server-Response-Engine", "Android-Native-JVM")
//                    .body(result.toString())
//                    .build();
//
//        } catch (Exception e) {
//            return buildErrorResponse(500, "Failed to resolve storage directories: " + e.getMessage());
//        }
//    }
//
//    // =========================================================================
//    // READ OPERATIONS
//    // =========================================================================
//
//    @RequestMapping(path = "/api/fs/list", method = "GET")
//    public ResponseContext listDirectory(RequestContext request) {
//        try {
//            String pathQuery = request.getQueryParam("path"); // e.g., ?path=subfolder
//            File targetDir = resolveSafeFile(pathQuery);
//            
//            JSONArray contents = storageService.readDirectory(targetDir);
//            
//            JSONObject result = new JSONObject();
//            result.put("status", "success");
//            result.put("files", contents);
//            
//            return ResponseContext.status(200).body(result.toString()).build();
//        } catch (Exception e) {
//            return buildErrorResponse(400, "Failed listing directory: " + e.getMessage());
//        }
//    }
//
//    @RequestMapping(path = "/api/fs/read", method = "GET")
//    public ResponseContext readFileContent(RequestContext request) {
//        try {
//            String pathQuery = request.getQueryParam("path"); // e.g., ?path=notes/todo.txt
//            File targetFile = resolveSafeFile(pathQuery);
//            
//            byte[] fileData = storageService.readFile(targetFile);
//            String mimeType = "application/octet-stream";
//            
//            if (targetFile.getName().endsWith(".txt")) mimeType = "text/plain";
//            if (targetFile.getName().endsWith(".json")) mimeType = "application/json";
//
//            return ResponseContext.status(200)
//                    .contentType(mimeType)
//                    .body(fileData)
//                    .build();
//        } catch (Exception e) {
//            return buildErrorResponse(404, "Failed reading file: " + e.getMessage());
//        }
//    }
//
//    // =========================================================================
//    // CREATE OPERATIONS
//    // =========================================================================
//
//    @RequestMapping(path = "/api/fs/mkdir", method = "POST")
//    public ResponseContext createDirectory(RequestContext request) {
//        try {
//            String pathQuery = request.getQueryParam("path"); // e.g., ?path=deep/nested/folder
//            String recursiveStr = request.getQueryParam("recursive"); // e.g., ?recursive=true
//            
//            boolean recursive = "true".equalsIgnoreCase(recursiveStr);
//            File targetDir = resolveSafeFile(pathQuery);
//            
//            boolean success = storageService.createDirectory(targetDir, recursive);
//            
//            JSONObject result = new JSONObject();
//            result.put("status", success ? "success" : "error");
//            result.put("message", success ? "Directory matched/created." : "Could not create directory structural layout.");
//            
//            return ResponseContext.status(success ? 200 : 500).body(result.toString()).build();
//        } catch (Exception e) {
//            return buildErrorResponse(400, "Directory processing failure: " + e.getMessage());
//        }
//    }
//
////    @RequestMapping(path = "/api/fs/write", method = "POST")
////    public ResponseContext createOrWriteFile(RequestContext request) {
////        try {
////            String pathQuery = request.getQueryParam("path"); // e.g., ?path=logs/session.json
////            File targetFile = resolveSafeFile(pathQuery);
////            
////            // Uses request body payload
////            byte[] dataPayload = request.getBody();
////            storageService.createFile(targetFile, dataPayload);
////            
////            JSONObject result = new JSONObject();
////            result.put("status", "success");
////            result.put("message", "File saved successfully: " + targetFile.getName());
////            
////            return ResponseContext.status(200).body(result.toString()).build();
////        } catch (Exception e) {
////            return buildErrorResponse(500, "File persist error: " + e.getMessage());
////        }
////    }
////@RequestMapping(path = "/api/fs/write", method = "POST")
////public ResponseContext createOrWriteFile(RequestContext request) {
////    try {
////        String pathQuery = request.getQueryParam("path");
////        // FIX: Extract content via parameter key wrapper instead of reading a raw body stream
////        String textContent = request.getQueryParam("content"); 
////        
////        File targetFile = resolveSafeFile(pathQuery);
////        byte[] dataPayload = textContent != null ? textContent.getBytes("UTF-8") : new byte[0];
////        
////        storageService.createFile(targetFile, dataPayload);
////
////        JSONObject result = new JSONObject();
////        result.put("status", "success");
////        return ResponseContext.status(200).body(result.toString()).build();
////    } catch (Exception e) {
////        return buildErrorResponse(500, "File persist error: " + e.getMessage());
////    }
////}
//@RequestMapping(path="/api/fs/write", method="POST")
//public ResponseContext createOrWriteFile(RequestContext request) {
//    try {
//        String pathQuery = request.getQueryParam("path");
//        File targetFile = resolveSafeFile(pathQuery);
//
//        // FIX: Extract raw binary bytes directly from the body envelope payload
//        byte[] dataPayload = request.getBody();
//        if (dataPayload == null) {
//            dataPayload = new byte[0];
//        }
//
//        // Commit byte buffers directly to flash sectors via your storage service
//        storageService.createFile(targetFile, dataPayload);
//
//        JSONObject result = new JSONObject();
//        result.put("status", "success");
//        return ResponseContext.status(200).body(result.toString()).build();
//
//    } catch (Exception e) {
//        return buildErrorResponse(500, "File persist error: " + e.getMessage());
//    }
//}
//
//
//    // =========================================================================
//    // DELETE OPERATIONS
//    // =========================================================================
//
//    @RequestMapping(path = "/api/fs/delete", method = "DELETE")
//    public ResponseContext deleteFileSystemPath(RequestContext request) {
//        try {
//            String pathQuery = request.getQueryParam("path"); // e.g., ?path=deep/nested/folder
//            String recursiveStr = request.getQueryParam("recursive"); // e.g., ?recursive=true
//            
//            boolean recursive = "true".equalsIgnoreCase(recursiveStr);
//            File targetFile = resolveSafeFile(pathQuery);
//            
//            // Safety Check: Do not let API delete the root folder accidentally
//            if (targetFile.getCanonicalPath().equals(getStorageRoot().getCanonicalPath())) {
//                return buildErrorResponse(403, "Forbidden: Cannot delete the storage environment root context.");
//            }
//
//            boolean success = storageService.deletePath(targetFile, recursive);
//            
//            JSONObject result = new JSONObject();
//            result.put("status", success ? "success" : "error");
//            result.put("message", success ? "Deleted resource cleanly." : "Failed completely clearing resource targets.");
//            
//            return ResponseContext.status(success ? 200 : 500).body(result.toString()).build();
//        } catch (Exception e) {
//            return buildErrorResponse(400, "Resource cleaning failure: " + e.getMessage());
//        }
//    }
//
//	// Private structural helper method to handle deep recursive archive compilation
//	private void zipRecursiveHelper(File rootFolder, File currentFile, java.util.zip.ZipOutputStream zos) throws IOException {
//	    if (currentFile.isDirectory()) {
//		File[] children = currentFile.listFiles();
//		if (children != null) {
//		    for (File child : children) {
//			zipRecursiveHelper(rootFolder, child, zos);
//		    }
//		}
//	    } else {
//		// Calculate standard relative zip path entry notation (using forward slashes)
//		String rootPath = rootFolder.getCanonicalPath();
//		String currentPath = currentFile.getCanonicalPath();
//		String relativeZipPath = currentPath.substring(rootPath.length() + 1).replace(File.separatorChar, '/');
//
//		java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(relativeZipPath);
//		zos.putNextEntry(zipEntry);
//
//		// Pipe actual resource bytes into the outbound stream architecture
//		try (java.io.FileInputStream fis = new java.io.FileInputStream(currentFile)) {
//		    byte[] buffer = new byte[4096]; // Fixed
//		    int bytesRead;
//		    while ((bytesRead = fis.read(buffer)) >= 0) {
//			zos.write(buffer, 0, bytesRead);
//		    }
//		}
//		zos.closeEntry();
//	    }
//	}
//
//    // --- ADDED DISK SPACE PARTITION DIAGNOSTICS ---
//    @RequestMapping(path = "/api/fs/diskspace", method = "GET")
//    public ResponseContext getDiskSpaceDiagnostics(RequestContext request) {
//        try {
//            JSONObject root = new JSONObject();
//            Context context = request.getAndroidContext();
//
//            // 1. Core Internal Device Flash Storage Allocation Analytics
//            JSONObject internalStorage = new JSONObject();
//            File internalPath = Environment.getDataDirectory();
//            StatFs internalStat = new StatFs(internalPath.getPath());
//            
//            long blockSizeInt = android.os.Build.VERSION.SDK_INT >= 18 ? internalStat.getBlockSizeLong() : internalStat.getBlockSize();
//            long availableBlocksInt = android.os.Build.VERSION.SDK_INT >= 18 ? internalStat.getAvailableBlocksLong() : internalStat.getAvailableBlocks();
//            long totalBlocksInt = android.os.Build.VERSION.SDK_INT >= 18 ? internalStat.getBlockCountLong() : internalStat.getBlockCount();
//            
//            internalStorage.put("partition_path", internalPath.getAbsolutePath());
//            internalStorage.put("total_space_bytes", totalBlocksInt * blockSizeInt);
//            internalStorage.put("available_space_bytes", availableBlocksInt * blockSizeInt);
//            internalStorage.put("status", "success");
//            root.put("internal_partition", internalStorage);
//
//            // 2. Removable/Secondary Hardware Micro-SD Card Mount Audits
//            JSONObject secondaryStorage = new JSONObject();
//            boolean sdCardDetected = false;
//            String sdCardPath = "unmounted";
//            long sdTotalBytes = 0;
//            long sdAvailBytes = 0;
//
//            if (context != null) {
//                File[] externalDirs = context.getExternalFilesDirs(null);
//                if (externalDirs != null && externalDirs.length > 1 && externalDirs[1] != null) {
//                    File sdFile = externalDirs[1];
//                    String rawSdPath = sdFile.getAbsolutePath();
//                    int androidIndex = rawSdPath.indexOf("/Android");
//                    if (androidIndex != -1) {
//                        File sdRoot = new File(rawSdPath.substring(0, androidIndex));
//                        if (sdRoot.exists()) {
//                            StatFs sdStat = new StatFs(sdRoot.getPath());
//                            long blockSizeSd = android.os.Build.VERSION.SDK_INT >= 18 ? sdStat.getBlockSizeLong() : sdStat.getBlockSize();
//                            long availableBlocksSd = android.os.Build.VERSION.SDK_INT >= 18 ? sdStat.getAvailableBlocksLong() : sdStat.getAvailableBlocks();
//                            long totalBlocksSd = android.os.Build.VERSION.SDK_INT >= 18 ? sdStat.getBlockCountLong() : sdStat.getBlockCount();
//                            
//                            sdCardDetected = true;
//                            sdCardPath = sdRoot.getAbsolutePath();
//                            sdTotalBytes = totalBlocksSd * blockSizeSd;
//                            sdAvailBytes = availableBlocksSd * blockSizeSd;
//                        }
//                    }
//                }
//            }
//
//            secondaryStorage.put("removable_sdcard_mounted", sdCardDetected);
//            secondaryStorage.put("partition_path", sdCardPath);
//            secondaryStorage.put("total_space_bytes", sdTotalBytes);
//            secondaryStorage.put("available_space_bytes", sdAvailBytes);
//            root.put("secondary_partition", secondaryStorage);
//
//            // 3. Application App-Specific Local Sandbox Cache Space Allocation Metrics
//            JSONObject cacheInfo = new JSONObject();
//            if (context != null) {
//                File cacheDir = context.getCacheDir();
//                File codeCacheDir = android.os.Build.VERSION.SDK_INT >= 21 ? context.getCodeCacheDir() : null;
//                
//                long totalCacheUsage = 0;
//                if (cacheDir != null && cacheDir.exists()) totalCacheUsage += calculateDirSizeHelper(cacheDir);
//                if (codeCacheDir != null && codeCacheDir.exists()) totalCacheUsage += calculateDirSizeHelper(codeCacheDir);
//                
//                cacheInfo.put("sandbox_cache_path", cacheDir != null ? cacheDir.getAbsolutePath() : "unknown");
//                cacheInfo.put("active_cache_usage_bytes", totalCacheUsage);
//                cacheInfo.put("status", "success");
//            } else {
//                cacheInfo.put("status", "Context unavailable");
//            }
//            root.put("app_sandbox_cache", cacheInfo);
//
//            return ResponseContext.status(200)
//                    .contentType("application/json")
//                    .header("X-Server-Response-Engine", "Android-Native-JVM")
//                    .body(root.toString())
//                    .build();
//
//        } catch (Exception e) {
//            Log.e(TAG, "Disk space analytics engine execution crash", e);
//            return buildErrorResponse(500, "Disk inspection failure: " + e.getMessage());
//        }
//    }
//
//    // =========================================================================
//    // HELPERS
//    // =========================================================================
//    /**
//     * Inline helper for generating uniform error messaging structures cleanly
//     */
//    private ResponseContext buildErrorResponse(int code, String message) {
//        JSONObject errJson = new JSONObject();
//        try {
//            errJson.put("status", "error");
//            errJson.put("message", message);
//        } catch (Exception ignored) {}
//        return ResponseContext.status(code).body(errJson.toString()).build();
//    }
//    private long calculateDirSizeHelper(File directory) {
//        long size = 0;
//        File[] files = directory.listFiles();
//        if (files != null) {
//            for (File file : files) {
//                if (file.isFile()) {
//                    size += file.length();
//                } else {
//                    size += calculateDirSizeHelper(file);
//                }
//            }
//        }
//        return size;
//    }
//
//
//}
//--------------------------------------------------------------------------------
package com.example.app.services.maintenance;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.example.app.MainActivity;
import com.example.app.AppConfig;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import com.example.app.services.StorageService;
import android.os.StatFs;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FsController {
    private static final String TAG = "FsController";
    private final StorageService storageService = new StorageService();

    public FsController() {}

    /**
     * ◄ DYNAMIC STORAGE STRATEGY ROUTER
     * Dynamically anchors the execution root directory context based on your active panel options.
     */
    private File getStorageRoot(RequestContext request) {
        AppConfig config = request.getAppConfig();
        Context context = request.getAndroidContext();
        
        if (config != null && config.isPublicWorkspaceEnabled()) {
            // Public Shared Documents Environment Strategy Active
            File publicDocsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File publicRoot = new File(publicDocsDir, config.getWorkspaceFolderName());
            if (!publicRoot.exists()) {
                publicRoot.mkdirs();
            }
            return publicRoot;
        } else {
            // DEFAULT SECURE BEHAVIOR: Private Application Sandbox Cache Isolation Strategy Active
            File sandboxRoot = new File(context.getFilesDir(), "www");
            if (!sandboxRoot.exists()) {
                sandboxRoot.mkdirs();
            }
            return sandboxRoot;
        }
    }

    private File resolveSafeFile(RequestContext request, String relativePath) throws IOException {
        File root = getStorageRoot(request);
        if (relativePath == null || relativePath.isEmpty()) {
            return root;
        }
        
        File target = new File(root, relativePath);
        // Canonical protection guardrail layer to check for directory traversal path manipulation
        if (!target.getCanonicalPath().startsWith(root.getCanonicalPath())) {
            throw new SecurityException("Directory traversal validation escape attempt blocked cleanly.");
        }
        return target;
    }

    @RequestMapping(path="/api/fs/locations", method="GET")
    public ResponseContext getStorageLocations(RequestContext request) {
        try {
            Context context = request.getAndroidContext();
            JSONObject locations = new JSONObject();
            File activeRoot = getStorageRoot(request);
            
            locations.put("external_storage_root", activeRoot.getAbsolutePath());
            
            if (context != null) {
                File sandboxDbDir = context.getDatabasePath("probe.db").getParentFile();
                if (sandboxDbDir != null) {
                    if (!sandboxDbDir.exists()) sandboxDbDir.mkdirs();
                    locations.put("sandbox_databases_root", sandboxDbDir.getAbsolutePath());
                } else {
                    locations.put("sandbox_databases_root", "unknown_path_error");
                }
                locations.put("package_name", context.getPackageName());
            } else {
                locations.put("sandbox_databases_root", "context_unavailable");
            }
            
            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("locations", locations);
            return ResponseContext.status(200).contentType("application/json")
                    .header("X-Server-Response-Engine", "Android-Native-JVM")
                    .body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(500, "Failed to resolve storage directories: " + e.getMessage());
        }
    }

    @RequestMapping(path="/api/fs/list", method="GET")
    public ResponseContext listDirectory(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path");
            File targetDir = resolveSafeFile(request, pathQuery);
            JSONArray contents = storageService.readDirectory(targetDir);
            
            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("files", contents);
            return ResponseContext.status(200).body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(400, "Failed listing directory: " + e.getMessage());
        }
    }
    @RequestMapping(path="/api/fs/read", method="GET")
    public ResponseContext readFileContent(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path");
            File targetFile = resolveSafeFile(request, pathQuery);
            byte[] fileData = storageService.readFile(targetFile);
            
            String mimeType = "application/octet-stream";
            if (targetFile.getName().endsWith(".txt")) mimeType = "text/plain";
            if (targetFile.getName().endsWith(".json")) mimeType = "application/json";
            
            return ResponseContext.status(200).contentType(mimeType).body(fileData).build();
        } catch (Exception e) {
            return buildErrorResponse(404, "Failed reading file: " + e.getMessage());
        }
    }

    @RequestMapping(path="/api/fs/mkdir", method="POST")
    public ResponseContext createDirectory(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path");
            String recursiveStr = request.getQueryParam("recursive");
            boolean recursive = "true".equalsIgnoreCase(recursiveStr);
            
            File targetDir = resolveSafeFile(request, pathQuery);
            boolean success = storageService.createDirectory(targetDir, recursive);
            
            JSONObject result = new JSONObject();
            result.put("status", success ? "success" : "error");
            result.put("message", success ? "Directory matched/created." : "Could not create directory structural layout.");
            return ResponseContext.status(success ? 200 : 500).body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(400, "Directory processing failure: " + e.getMessage());
        }
    }

    @RequestMapping(path="/api/fs/write", method="POST")
    public ResponseContext createOrWriteFile(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path");
            File targetFile = resolveSafeFile(request, pathQuery);
            byte[] dataPayload = request.getBody();
            if (dataPayload == null) {
                dataPayload = new byte[0];
            }
            
            storageService.createFile(targetFile, dataPayload);
            JSONObject result = new JSONObject();
            result.put("status", "success");
            return ResponseContext.status(200).body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(500, "File persist error: " + e.getMessage());
        }
    }

    @RequestMapping(path="/api/fs/delete", method="DELETE")
    public ResponseContext deleteFileSystemPath(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path");
            String recursiveStr = request.getQueryParam("recursive");
            boolean recursive = "true".equalsIgnoreCase(recursiveStr);
            
            File targetFile = resolveSafeFile(request, pathQuery);
            if (targetFile.getCanonicalPath().equals(getStorageRoot(request).getCanonicalPath())) {
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

    @RequestMapping(path="/api/fs/diskspace", method="GET")
    public ResponseContext getDiskSpaceDiagnostics(RequestContext request) {
        try {
            JSONObject root = new JSONObject();
            Context context = request.getAndroidContext();
            
            JSONObject internalStorage = new JSONObject();
            File internalPath = Environment.getDataDirectory();
            StatFs internalStat = new StatFs(internalPath.getPath());
            
            long blockSizeInt = android.os.Build.VERSION.SDK_INT >= 18 ? internalStat.getBlockSizeLong() : internalStat.getBlockSize();
            long availableBlocksInt = android.os.Build.VERSION.SDK_INT >= 18 ? internalStat.getAvailableBlocksLong() : internalStat.getAvailableBlocks();
            long totalBlocksInt = android.os.Build.VERSION.SDK_INT >= 18 ? internalStat.getBlockCountLong() : internalStat.getBlockCount();
            
            internalStorage.put("partition_path", internalPath.getAbsolutePath());
            internalStorage.put("total_space_bytes", totalBlocksInt * blockSizeInt);
            internalStorage.put("available_space_bytes", availableBlocksInt * blockSizeInt);
            internalStorage.put("status", "success");
            root.put("internal_partition", internalStorage);
            
            JSONObject secondaryStorage = new JSONObject();
            boolean sdCardDetected = false;
            String sdCardPath = "unmounted";
            long sdTotalBytes = 0;
            long sdAvailBytes = 0;
            
            if (context != null) {
                File[] externalDirs = context.getExternalFilesDirs(null);
                if (externalDirs != null && externalDirs.length > 1 && externalDirs[1] != null) {
                    File sdFile = externalDirs[1];
                    String rawSdPath = sdFile.getAbsolutePath();
                    int androidIndex = rawSdPath.indexOf("/Android");
                    if (androidIndex != -1) {
                        File sdRoot = new File(rawSdPath.substring(0, androidIndex));
                        if (sdRoot.exists()) {
                            StatFs sdStat = new StatFs(sdRoot.getPath());
                            long blockSizeSd = android.os.Build.VERSION.SDK_INT >= 18 ? sdStat.getBlockSizeLong() : sdStat.getBlockSize();
                            long availableBlocksSd = android.os.Build.VERSION.SDK_INT >= 18 ? sdStat.getAvailableBlocksLong() : sdStat.getAvailableBlocks();
                            long totalBlocksSd = android.os.Build.VERSION.SDK_INT >= 18 ? sdStat.getBlockCountLong() : sdStat.getBlockCount();
                            
                            sdCardDetected = true;
                            sdCardPath = sdRoot.getAbsolutePath();
                            sdTotalBytes = totalBlocksSd * blockSizeSd;
                            sdAvailBytes = availableBlocksSd * blockSizeSd;
                        }
                    }
                }
            }
            
            secondaryStorage.put("removable_sdcard_mounted", sdCardDetected);
            secondaryStorage.put("partition_path", sdCardPath);
            secondaryStorage.put("total_space_bytes", sdTotalBytes);
            secondaryStorage.put("available_space_bytes", sdAvailBytes);
            root.put("secondary_partition", secondaryStorage);
            
            JSONObject cacheInfo = new JSONObject();
            if (context != null) {
                File cacheDir = context.getCacheDir();
                File codeCacheDir = android.os.Build.VERSION.SDK_INT >= 21 ? context.getCodeCacheDir() : null;
                long totalCacheUsage = 0;
                
                if (cacheDir != null && cacheDir.exists()) totalCacheUsage += calculateDirSizeHelper(cacheDir);
                if (codeCacheDir != null && codeCacheDir.exists()) totalCacheUsage += calculateDirSizeHelper(codeCacheDir);
                
                cacheInfo.put("sandbox_cache_path", cacheDir != null ? cacheDir.getAbsolutePath() : "unknown");
                cacheInfo.put("active_cache_usage_bytes", totalCacheUsage);
                cacheInfo.put("status", "success");
            } else {
                cacheInfo.put("status", "Context unavailable");
            }
            root.put("app_sandbox_cache", cacheInfo);
            
            return ResponseContext.status(200).contentType("application/json")
                    .header("X-Server-Response-Engine", "Android-Native-JVM")
                    .body(root.toString()).build();
        } catch (Exception e) {
            Log.e(TAG, "Disk space analytics engine execution crash", e);
            return buildErrorResponse(500, "Disk inspection failure: " + e.getMessage());
        }
    }

    private ResponseContext buildErrorResponse(int code, String message) {
        JSONObject errJson = new JSONObject();
        try {
            errJson.put("status", "error");
            errJson.put("message", message);
        } catch (Exception ignored) {}
        return ResponseContext.status(code).body(errJson.toString()).build();
    }

    private long calculateDirSizeHelper(File directory) {
        long size = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else {
                    size += calculateDirSizeHelper(file);
                }
            }
        }
        return size;
    }
}

