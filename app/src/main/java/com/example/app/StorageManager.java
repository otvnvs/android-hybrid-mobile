package com.example.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class StorageManager {
    private static final String TAG = "JAVA_StorageManager";
    private final Context context;
    private final AppConfig config;

    public StorageManager(Context context, AppConfig config) {
        this.context = context.getApplicationContext();
        this.config = config;
        Log.d(TAG, "StorageManager tracking initialized.");
    }

    public void createPublicWorkspaceDirectory() {
        String folderName = config.getWorkspaceFolderName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, "placeholder.txt");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/" + folderName + "/www");
            Uri externalUri = MediaStore.Files.getContentUri("external");
            try {
                Uri fileUri = resolver.insert(externalUri, values);
                if (fileUri != null) {
                    resolver.delete(fileUri, null, null);
                    Log.i(TAG, "MediaStore Workspace Checked: Documents/" + folderName + "/www");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error generating Scoped Storage trace: " + e.getMessage());
            }
        } else {
            File legacyDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (legacyDocs != null) {
                File workspace = new File(new File(legacyDocs, folderName), "www");
                if (!workspace.exists()) {
                    workspace.mkdirs();
                }
            }
        }
    }

    /**
     * Replicates internal private sandbox assets out to the shared device storage card workspace
     */
    public void syncSandboxToExternal() {
        Log.i(TAG, "syncSandboxToExternal() worker invoked. Preparing file sync task...");
        
        new Thread(() -> {
            try {
                // Pinpoint mutable internal source location
                File sandboxDir = new File(context.getFilesDir(), "www");
                String folderName = config.getWorkspaceFolderName();
                File publicDocsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                
                if (publicDocsDir == null) {
                    throw new IOException("Public shared directory space is currently unavailable or unmounted.");
                }
                if (!sandboxDir.exists()) {
                    throw new IOException("Source internal sandbox path directory does not exist yet. Run an update check first.");
                }

                File externalTargetDir = new File(new File(publicDocsDir, folderName), "www");
                Log.d(TAG, " -> Starting file replication pipeline tracking loops:");
                Log.d(TAG, "    [Source Internal]: " + sandboxDir.getAbsolutePath());
                Log.d(TAG, "    [Target Public]:   " + externalTargetDir.getAbsolutePath());

                // Spawns low-level channel memory copier loops
                copyDirectoryRecursive(sandboxDir, externalTargetDir);
                Log.i(TAG, " -> Asset replication task completed successfully. Public workspace is synchronized.");
                
            } catch (Exception e) {
                Log.e(TAG, " -> Synchronization thread encountered critical failure: " + e.getMessage());
            }
        }).start();
    }

    private void copyDirectoryRecursive(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists() && !destination.mkdirs()) {
                throw new IOException("Failed to create destination workspace branch node: " + destination.getAbsolutePath());
            }
            String[] layoutItems = source.list();
            if (layoutItems != null) {
                for (String item : layoutItems) {
                    copyDirectoryRecursive(new File(source, item), new File(destination, item));
                }
            }
        } else {
            // High-efficiency kernel-level file transfer channel stream
            try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
                 FileChannel destChannel = new FileOutputStream(destination).getChannel()) {
                Log.v(TAG, "    Replicating binary payload component: " + source.getName());
                sourceChannel.transferTo(0, sourceChannel.size(), destChannel);
            }
        }
    }

    public String determineStartupPath() {
        String folderName = config.getWorkspaceFolderName();
        File publicDocsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File workspaceIndex = (publicDocsDir != null) ? new File(new File(publicDocsDir, folderName), "www/index.html") : null;

        if (workspaceIndex != null && workspaceIndex.exists() && workspaceIndex.isFile()) {
            return "/index.html";
        }

        File sandboxIndex = new File(context.getFilesDir(), "www/index.html");
        if (sandboxIndex.exists() && sandboxIndex.isFile()) {
            return "/index.html";
        }

        try {
            String[] assetsList = context.getAssets().list("www");
            if (assetsList != null) {
                for (String file : assetsList) {
                    if ("index.html".equals(file)) {
                        return "/index.html";
                    }
                }
            }
        } catch (Exception ignored) {}

        return "/error.html";
    }
}

