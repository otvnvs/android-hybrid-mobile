//package com.example.app;
//
//import android.content.Context;
//import android.util.Base64;
//import android.util.Log;
//import java.io.BufferedInputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.nio.channels.FileChannel;
//import java.nio.charset.StandardCharsets;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipInputStream;
//
//public class UpdateManager {
//    private static final String TAG = "JAVA_UpdateManager";
//    private final Context context;
//    private final AppConfig config;
//
//    public UpdateManager(Context context, AppConfig config) {
//        this.context = context.getApplicationContext();
//        this.config = config;
//        Log.d(TAG, "UpdateManager system worker monitoring initialized.");
//    }
//
//    /**
//     * Spawns an asynchronous stream background thread to process remote network data downloads
//     */
//    public void startZipDownload() {
//        Log.i(TAG, "startZipDownload() invoked. Launching remote network pipeline thread...");
//        
//        new Thread(() -> {
//            HttpURLConnection connection = null;
//            InputStream inputStream = null;
//            FileOutputStream outputStream = null;
//            
//            try {
//                String targetUrlStr = config.getUpdateTargetUrl();
//                if (targetUrlStr == null || targetUrlStr.isEmpty()) {
//                    throw new IOException("Aborting network task: Remote Update URL property configuration is empty.");
//                }
//
//                Log.d(TAG, " -> Connecting to target endpoint: " + targetUrlStr);
//                URL url = new URL(targetUrlStr);
//                connection = (HttpURLConnection) url.openConnection();
//                connection.setConnectTimeout(15000);
//                connection.setReadTimeout(15000);
//                connection.setRequestMethod("GET");
//
//                if (config.useAuthentication()) {
//                    Log.d(TAG, " -> Injecting dynamic Basic Authentication credentials headers matrix.");
//                    String authStr = config.getAuthUsername() + ":" + config.getAuthPassword();
//                    String base64Auth = Base64.encodeToString(authStr.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
//                    connection.setRequestProperty("Authorization", "Basic " + base64Auth);
//                }
//
//                connection.connect();
//                int responseCode = connection.getResponseCode();
//                Log.d(TAG, " -> Server endpoint responded with code signature: " + responseCode);
//
//                if (responseCode != HttpURLConnection.HTTP_OK) {
//                    throw new IOException("Server returned invalid response state: " + responseCode + " - " + connection.getResponseMessage());
//                }
//
//                File tempZipFile = new File(context.getCacheDir(), "remote_deployment_package.zip");
//                if (tempZipFile.exists()) tempZipFile.delete();
//
//                inputStream = new BufferedInputStream(connection.getInputStream());
//                outputStream = new FileOutputStream(tempZipFile);
//
//                byte[] dataBuffer = new byte[4096];
//                int bytesRead;
//                long totalBytesDownloaded = 0;
//
//                Log.i(TAG, " -> Streaming remote packet payload chunks down into internal cache storage cell...");
//                while ((bytesRead = inputStream.read(dataBuffer, 0, 4096)) != -1) {
//                    totalBytesDownloaded += bytesRead;
//                    outputStream.write(dataBuffer, 0, bytesRead);
//                }
//                
//                outputStream.flush();
//                Log.i(TAG, " -> Binary packet stream fetch complete. Bytes Written: " + totalBytesDownloaded);
//
//                // ─── EXTRACT TARGET 1: INTERNAL MUTABLE SANDBOX WORKSPACE ──────────────────
//                File sandboxDir = new File(context.getFilesDir(), "www");
//                Log.i(TAG, " -> Initiating sandbox extraction pipeline into: " + sandboxDir.getAbsolutePath());
//                extractZipToSandbox(tempZipFile, sandboxDir);
//                
//                // ─── EXTRACT TARGET 2: DUAL-WRITE OUT TO EXTERNAL PUBLIC SD CARD ───────────
//                Log.i(TAG, " -> Dual-write triggered: Cloning extracted asset bundle out to public SD card folder tree...");
//                syncSandboxToExternalFolder(sandboxDir);
//
//                if (tempZipFile.exists()) tempZipFile.delete();
//                Log.i(TAG, " -> App Workspace fully updated across both storage tiers. Teardown finalized.");
//
//            } catch (Exception e) {
//                Log.e(TAG, " -> Critical network loop exception encountered during file update task: " + e.getMessage());
//            } finally {
//                try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) {}
//                try { if (inputStream != null) inputStream.close(); } catch (IOException ignored) {}
//                if (connection != null) connection.disconnect();
//            }
//        }).start();
//    }
//
//    /**
//     * Internal inline duplication helper that safely maps sandbox nodes out onto shared public space cells
//     */
//    private void syncSandboxToExternalFolder(File sandboxDir) {
//        try {
//            String folderName = config.getWorkspaceFolderName();
//            File publicDocsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS);
//            
//            if (publicDocsDir == null) {
//                throw new IOException("Public storage root path mapping returned null. Is storage unmounted?");
//            }
//
//            File externalTargetDir = new File(new File(publicDocsDir, folderName), "www");
//            Log.d(TAG, "    [Syncing Sync Destination Paths]: " + externalTargetDir.getAbsolutePath());
//
//            // Run structural recursive folder replication
//            copyDirectoryRecursive(sandboxDir, externalTargetDir);
//            Log.i(TAG, " -> Dual-write copy to SD Card complete.");
//
//        } catch (Exception e) {
//            Log.e(TAG, "    Dual-write sync execution encountered an error: " + e.getMessage());
//        }
//    }
//
//    private void copyDirectoryRecursive(File source, File destination) throws IOException {
//        if (source.isDirectory()) {
//            if (!destination.exists() && !destination.mkdirs()) {
//                throw new IOException("Failed to create destination path node branch: " + destination.getAbsolutePath());
//            }
//            String[] children = source.list();
//            if (children != null) {
//                for (String child : children) {
//                    copyDirectoryRecursive(new File(source, child), new File(destination, child));
//                }
//            }
//        } else {
//            // High efficiency kernel-level transfer channels channel pipelines
//            try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
//                 FileChannel destChannel = new FileOutputStream(destination).getChannel()) {
//                sourceChannel.transferTo(0, sourceChannel.size(), destChannel);
//            }
//        }
//    }
//
//    private void extractZipToSandbox(File zipFile, File targetDirectory) throws IOException {
//        if (targetDirectory.exists()) {
//            Log.d(TAG, "    Clearing out existing file nodes in destination target folder...");
//            deleteDirectoryRecursive(targetDirectory);
//        }
//        if (!targetDirectory.mkdirs()) {
//            throw new IOException("Failed to initialize system internal sandbox destination folder trees layout.");
//        }
//
//        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
//            ZipEntry entry = zipIn.getNextEntry();
//            
//            while (entry != null) {
//                File filePath = new File(targetDirectory, entry.getName());
//                
//                String canonicalTarget = targetDirectory.getCanonicalPath();
//                String canonicalEntry = filePath.getCanonicalPath();
//                if (!canonicalEntry.startsWith(canonicalTarget)) {
//                    throw new SecurityException("Security Violation: Zip entry tried to escape sandbox tree boundaries: " + entry.getName());
//                }
//
//                if (!entry.isDirectory()) {
//                    File parentDir = filePath.getParentFile();
//                    if (parentDir != null && !parentDir.exists()) {
//                        parentDir.mkdirs();
//                    }
//                    try (FileOutputStream fos = new FileOutputStream(filePath)) {
//                        byte[] buffer = new byte[4096];
//                        int readLen;
//                        while ((readLen = zipIn.read(buffer)) != -1) {
//                            fos.write(buffer, 0, readLen);
//                        }
//                    }
//                } else {
//                    filePath.mkdirs();
//                }
//                
//                zipIn.closeEntry();
//                entry = zipIn.getNextEntry();
//            }
//            Log.i(TAG, " -> Extraction execution loop completed successfully.");
//        }
//    }
//
//    private void deleteDirectoryRecursive(File element) {
//        if (element.isDirectory()) {
//            File[] files = element.listFiles();
//            if (files != null) {
//                for (File subFile : files) {
//                    deleteDirectoryRecursive(subFile);
//                }
//            }
//        }
//        element.delete();
//    }
//}
//--------------------------------------------------------------------------------
package com.example.app;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UpdateManager {
    private static final String TAG = "JAVA_UpdateManager";
    private final Context context;
    private final AppConfig config;
    private static volatile String currentStatusMessage = "Idle";

    public interface OnUpdateCompleteListener {
        void onUpdateFinished();
    }

    public UpdateManager(Context context, AppConfig config) {
        this.context = context.getApplicationContext();
        this.config = config;
        Log.d(TAG, "UpdateManager system worker monitoring initialized.");
    }
    /**
     * Spawns an asynchronous stream background thread to process remote network data downloads
     */
//    public void startZipDownload() {
//        Log.i(TAG, "startZipDownload() invoked. Launching remote network pipeline thread...");
//        
//        new Thread(() -> {
//            HttpURLConnection connection = null;
//            InputStream inputStream = null;
//            FileOutputStream outputStream = null;
//            
//            try {
//                String targetUrlStr = config.getUpdateTargetUrl();
//                if (targetUrlStr == null || targetUrlStr.isEmpty()) {
//                    throw new IOException("Aborting network task: Remote Update URL property configuration is empty.");
//                }
//
//                Log.d(TAG, " -> Connecting to target endpoint: " + targetUrlStr);
//                URL url = new URL(targetUrlStr);
//                connection = (HttpURLConnection) url.openConnection();
//                connection.setConnectTimeout(15000);
//                connection.setReadTimeout(15000);
//                connection.setRequestMethod("GET");
//
//                if (config.useAuthentication()) {
//                    Log.d(TAG, " -> Injecting dynamic Basic Authentication credentials headers matrix.");
//                    String authStr = config.getAuthUsername() + ":" + config.getAuthPassword();
//                    String base64Auth = Base64.encodeToString(authStr.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
//                    connection.setRequestProperty("Authorization", "Basic " + base64Auth);
//                }
//
//                connection.connect();
//                int responseCode = connection.getResponseCode();
//                Log.d(TAG, " -> Server endpoint responded with code signature: " + responseCode);
//
//                if (responseCode != HttpURLConnection.HTTP_OK) {
//                    throw new IOException("Server returned invalid response state: " + responseCode + " - " + connection.getResponseMessage());
//                }
//
//                File tempZipFile = new File(context.getCacheDir(), "remote_deployment_package.zip");
//                if (tempZipFile.exists()) tempZipFile.delete();
//
//                inputStream = new BufferedInputStream(connection.getInputStream());
//                outputStream = new FileOutputStream(tempZipFile);
//
//                byte[] dataBuffer = new byte[4096];
//                int bytesRead;
//                long totalBytesDownloaded = 0;
//
//                Log.i(TAG, " -> Streaming remote packet payload chunks down into internal cache storage cell...");
//                while ((bytesRead = inputStream.read(dataBuffer, 0, 4096)) != -1) {
//                    totalBytesDownloaded += bytesRead;
//                    outputStream.write(dataBuffer, 0, bytesRead);
//                }
//                
//                outputStream.flush();
//                Log.i(TAG, " -> Binary packet stream fetch complete. Bytes Written: " + totalBytesDownloaded);
//
//                // ─── EXTRACT TARGET 1: INTERNAL MUTABLE SANDBOX WORKSPACE ──────────────────
//                File sandboxDir = new File(context.getFilesDir(), "www");
//                Log.i(TAG, " -> Initiating sandbox extraction pipeline into: " + sandboxDir.getAbsolutePath());
//                extractZipToSandbox(tempZipFile, sandboxDir);
//                
//                // ─── EXTRACT TARGET 2: DUAL-WRITE OUT TO EXTERNAL PUBLIC SD CARD ───────────
//                syncSandboxToExternalFolder(sandboxDir);
//
//                if (tempZipFile.exists()) tempZipFile.delete();
//                Log.i(TAG, " -> App Workspace fully updated across both storage tiers. Teardown finalized.");
//
//                // ─── TRIGGER REFRESH CALL EVENT HERE ──────────────────────────────
//                if (listener != null) {
//                    Log.d(TAG, " -> Dispatching update finalized signal to orchestrator listener hook.");
//                    listener.onUpdateFinished();
//                }
//
//            } catch (Exception e) {
//                Log.e(TAG, " -> Critical network loop exception encountered during file update task: " + e.getMessage());
//            } finally {
//                try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) {}
//                try { if (inputStream != null) inputStream.close(); } catch (IOException ignored) {}
//                if (connection != null) connection.disconnect();
//            }
//        }).start();
//    }
    /**
     * Spawns an asynchronous stream background thread to process remote network data downloads
     * Added: OnUpdateCompleteListener parameter to trigger UI reloads on completion
     */
    public void startZipDownload(final OnUpdateCompleteListener listener) {
        Log.i(TAG, "startZipDownload() invoked. Launching remote network pipeline thread...");
        
        new Thread(() -> {
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            
            try {
                String targetUrlStr = config.getUpdateTargetUrl();
                if (targetUrlStr == null || targetUrlStr.isEmpty()) {
                    throw new IOException("Aborting network task: Remote Update URL property configuration is empty.");
                }

                Log.d(TAG, " -> Connecting to target endpoint: " + targetUrlStr);
                URL url = new URL(targetUrlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestMethod("GET");

                if (config.useAuthentication()) {
                    Log.d(TAG, " -> Injecting dynamic Basic Authentication credentials headers matrix.");
                    String authStr = config.getAuthUsername() + ":" + config.getAuthPassword();
                    String base64Auth = Base64.encodeToString(authStr.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                    connection.setRequestProperty("Authorization", "Basic " + base64Auth);
                }

                currentStatusMessage = "Downloading archive...";
                connection.connect();

                int responseCode = connection.getResponseCode();
                Log.d(TAG, " -> Server endpoint responded with code signature: " + responseCode);

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Server returned invalid response state: " + responseCode + " - " + connection.getResponseMessage());
                }

                File tempZipFile = new File(context.getCacheDir(), "remote_deployment_package.zip");
                if (tempZipFile.exists()) tempZipFile.delete();

                inputStream = new BufferedInputStream(connection.getInputStream());
                outputStream = new FileOutputStream(tempZipFile);

                byte[] dataBuffer = new byte[4096];
                int bytesRead;
                long totalBytesDownloaded = 0;

                Log.i(TAG, " -> Streaming remote packet payload chunks down into internal cache storage cell...");
                while ((bytesRead = inputStream.read(dataBuffer, 0, 4096)) != -1) {
                    totalBytesDownloaded += bytesRead;
                    outputStream.write(dataBuffer, 0, bytesRead);
                }
                
                outputStream.flush();
                Log.i(TAG, " -> Binary packet stream fetch complete. Bytes Written: " + totalBytesDownloaded);

                // ─── EXTRACT TARGET 1: INTERNAL MUTABLE SANDBOX WORKSPACE ──────────────────
                File sandboxDir = new File(context.getFilesDir(), "www");
                Log.i(TAG, " -> Initiating sandbox extraction pipeline into: " + sandboxDir.getAbsolutePath());
                currentStatusMessage = "Extracting files...";
                extractZipToSandbox(tempZipFile, sandboxDir);
                
                // ─── EXTRACT TARGET 2: DUAL-WRITE OUT TO EXTERNAL PUBLIC SD CARD ───────────
                currentStatusMessage = "Synchronizing storage tiers...";
                syncSandboxToExternalFolder(sandboxDir);

                if (tempZipFile.exists()) tempZipFile.delete();
                Log.i(TAG, " -> App Workspace fully updated across both storage tiers. Teardown finalized.");

                // ─── TRIGGER REFRESH INTERFACE SIGNAL EVENT HERE ──────────────────────────
                if (listener != null) {
                    Log.d(TAG, " -> Dispatching update finalized signal to orchestrator listener hook.");
                    currentStatusMessage = "Update complete!";
                    listener.onUpdateFinished();
                }

                // Reset back to idle state after a short delay
                new Thread(() -> {
                    try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                    if ("Update complete!".equals(currentStatusMessage)) {
                        currentStatusMessage = "Idle";
                    }
                }).start();

            } catch (Exception e) {
                Log.e(TAG, " -> Critical network loop exception encountered during file update task: " + e.getMessage());
                currentStatusMessage = "Error: " + e.getMessage();
            } finally {
                try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) {}
                try { if (inputStream != null) inputStream.close(); } catch (IOException ignored) {}
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    /**
     * Replicates internal private sandbox assets out to the shared device storage card workspace
     */
    private void syncSandboxToExternalFolder(File sandboxDir) {
        Log.i(TAG, "─── STARTING SD CARD SYNC TRANSACTION ───");
        
        try {
            // Verify runtime system permissions explicitly in logs
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                boolean hasManageAll = android.os.Environment.isExternalStorageManager();
                Log.d(TAG, " [Security Status Check] Environment.isExternalStorageManager() = " + hasManageAll);
                if (!hasManageAll) {
                    Log.e(TAG, " !! CRITICAL !! App lacks MANAGE_EXTERNAL_STORAGE permission at the OS level.");
                }
            }

            String folderName = config.getWorkspaceFolderName();
            File publicDocsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS);
            
            if (publicDocsDir == null) {
                throw new IOException("Public storage path returned null. Shared directory is unmounted.");
            }

            // Standardize absolute destination route path file node
            File externalTargetDir = new File(new File(publicDocsDir, folderName), "www");
            
            Log.d(TAG, " -> Parameters Verification Matrix:");
            Log.d(TAG, "    [Source Dir Exists]: " + sandboxDir.exists() + " (" + sandboxDir.getAbsolutePath() + ")");
            Log.d(TAG, "    [Target Destination]: " + externalTargetDir.getAbsolutePath());

            // Pre-clean the old directory if it exists to ensure dead assets are purged
            if (externalTargetDir.exists()) {
                Log.d(TAG, " -> Target directory detected. Pre-purging old public structures...");
                deleteDirectoryRecursive(externalTargetDir);
            }

            // Force directory tree recreation
            if (!externalTargetDir.mkdirs()) {
                Log.w(TAG, " -> Warning: mkdirs() returned false. Directory might exist or folder write was blocked.");
            }

            // Run structural recursive file replication
            int totalFilesCloned = copyDirectoryRecursiveWithLogs(sandboxDir, externalTargetDir);
            Log.i(TAG, "─── SD CARD SYNC COMPLETE (Files Cloned: " + totalFilesCloned + ") ───");

        } catch (Exception e) {
            Log.e(TAG, " !! CRITICAL SYNC BREAKDOWN !! Exception: " + e.getMessage(), e);
        }
    }

    /**
     * Highly verbose recursive file copier tracking loops
     */
    private int copyDirectoryRecursiveWithLogs(File source, File destination) throws IOException {
        int fileCount = 0;
        
        if (source.isDirectory()) {
            if (!destination.exists() && !destination.mkdirs()) {
                throw new IOException("OS permission engine blocked folder creation target path: " + destination.getAbsolutePath());
            }
            
            String[] children = source.list();
            if (children != null) {
                for (String child : children) {
                    File nextSource = new File(source, child);
                    File nextDest = new File(destination, child);
                    fileCount += copyDirectoryRecursiveWithLogs(nextSource, nextDest);
                }
            }
        } else {
            Log.d(TAG, "    [Copying File] " + source.getName() + " ➔ " + destination.getAbsolutePath());
            
            try (FileChannel sourceChannel = new java.io.FileInputStream(source).getChannel();
                 FileChannel destChannel = new java.io.FileOutputStream(destination).getChannel()) {
                
                long bytesTransferred = sourceChannel.transferTo(0, sourceChannel.size(), destChannel);
                Log.v(TAG, "    [Bytes Written]: " + bytesTransferred);
                fileCount++;
                
            } catch (IOException ioException) {
                Log.e(TAG, "    !! FILE WRITE DENIED !! FAILED PATH: " + destination.getAbsolutePath() + " Error: " + ioException.getMessage());
                throw ioException;
            }
        }
        return fileCount;
    }
//    private void extractZipToSandbox(File zipFile, File targetDirectory) throws IOException {
//        if (targetDirectory.exists()) {
//            Log.d(TAG, "    Clearing out existing file nodes in destination target folder...");
//            deleteDirectoryRecursive(targetDirectory);
//        }
//        if (!targetDirectory.mkdirs()) {
//            throw new IOException("Failed to initialize system internal sandbox destination folder trees layout.");
//        }
//
//        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
//            ZipEntry entry = zipIn.getNextEntry();
//            
//            while (entry != null) {
//                File filePath = new File(targetDirectory, entry.getName());
//                
//                String canonicalTarget = targetDirectory.getCanonicalPath();
//                String canonicalEntry = filePath.getCanonicalPath();
//                if (!canonicalEntry.startsWith(canonicalTarget)) {
//                    throw new SecurityException("Security Violation: Zip entry tried to escape sandbox tree boundaries: " + entry.getName());
//                }
//
//                if (!entry.isDirectory()) {
//                    File parentDir = filePath.getParentFile();
//                    if (parentDir != null && !parentDir.exists()) {
//                        parentDir.mkdirs();
//                    }
//                    try (FileOutputStream fos = new FileOutputStream(filePath)) {
//                        byte[] buffer = new byte[4096];
//                        int readLen;
//                        while ((readLen = zipIn.read(buffer)) != -1) {
//                            fos.write(buffer, 0, readLen);
//                        }
//                    }
//                } else {
//                    filePath.mkdirs();
//                }
//                
//                zipIn.closeEntry();
//                entry = zipIn.getNextEntry();
//            }
//            Log.i(TAG, " -> Extraction execution loop completed successfully.");
//        }
//    }
    private void extractZipToSandbox(File zipFile, File targetDirectory) throws IOException {
        if (targetDirectory.exists()) {
            Log.d(TAG, "    Clearing out existing file nodes in destination target folder...");
            deleteDirectoryRecursive(targetDirectory);
        }
        if (!targetDirectory.mkdirs()) {
            throw new IOException("Failed to initialize system internal sandbox destination folder trees layout.");
        }

        // --- STEP 1: PRE-SCAN THE ZIP FILE FOR A GITHUB WRAPPER FOLDER ---
        String commonRootPrefix = "";
        try (ZipInputStream scanIn = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry firstEntry = scanIn.getNextEntry();
            if (firstEntry != null && firstEntry.isDirectory()) {
                commonRootPrefix = firstEntry.getName();
                Log.i(TAG, " -> GitHub root wrapper directory prefix detected: " + commonRootPrefix);
            }
        }

        // Load configuration subpath target property values
        String subpathConfig = config.getUpdateTargetSubpath();
        Log.i(TAG, " -> Target scoped subpath constraint rule evaluation: \"" + subpathConfig + "\"");

        // --- STEP 2: SCOPED EXTRACTION LOOP ---
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry = zipIn.getNextEntry();
            
            while (entry != null) {
                String entryName = entry.getName();
                
                // 1. Remove the GitHub parent folder path layer if present
                if (!commonRootPrefix.isEmpty() && entryName.startsWith(commonRootPrefix)) {
                    entryName = entryName.substring(commonRootPrefix.length());
                }

                // Ensure leading slashes are omitted to standardize comparisons
                if (entryName.startsWith("/")) {
                    entryName = entryName.substring(1);
                }

                if (!entryName.isEmpty()) {
                    boolean shouldExtract = true;
                    String finalExtractionPath = entryName;

                    // 2. Evaluate subpath structural matching boundaries
                    if (!subpathConfig.isEmpty()) {
                        // Standardize configuration parameters strings matching zip entry structures
                        String cleanSubpath = subpathConfig.endsWith("/") ? subpathConfig : subpathConfig + "/";
                        if (cleanSubpath.startsWith("/")) {
                            cleanSubpath = cleanSubpath.substring(1);
                        }

                        if (entryName.startsWith(cleanSubpath)) {
                            // Slice off the subpath folder layers so matching files unpack directly to root
                            finalExtractionPath = entryName.substring(cleanSubpath.length());
                            shouldExtract = !finalExtractionPath.isEmpty();
                        } else {
                            shouldExtract = false; // Filter out files that don't match the subpath scope
                        }
                    }

                    if (shouldExtract) {
                        File filePath = new File(targetDirectory, finalExtractionPath);
                        
                        String canonicalTarget = targetDirectory.getCanonicalPath();
                        String canonicalEntry = filePath.getCanonicalPath();
                        if (!canonicalEntry.startsWith(canonicalTarget)) {
                            throw new SecurityException("Security Violation: Zip entry tried to escape sandbox tree boundaries: " + entry.getName());
                        }

                        if (!entry.isDirectory()) {
                            File parentDir = filePath.getParentFile();
                            if (parentDir != null && !parentDir.exists()) {
                                parentDir.mkdirs();
                            }
                            Log.v(TAG, "    Unpacking targeted scoped element: " + finalExtractionPath);
                            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                                byte[] buffer = new byte[4096];
                                int readLen;
                                while ((readLen = zipIn.read(buffer)) != -1) {
                                    fos.write(buffer, 0, readLen);
                                }
                            }
                        } else {
                            filePath.mkdirs();
                        }
                    }
                }
                
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            Log.i(TAG, " -> Subpath-aware extraction execution loop completed successfully.");
        }
    }


    private void deleteDirectoryRecursive(File element) {
        if (element.isDirectory()) {
            File[] files = element.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    deleteDirectoryRecursive(subFile);
                }
            }
        }
        element.delete();
    }

    public static String getCurrentStatus() {
        return currentStatusMessage;
    }
}

