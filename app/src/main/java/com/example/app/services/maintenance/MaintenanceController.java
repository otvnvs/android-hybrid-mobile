package com.example.app.services.maintenance;

import android.util.Log;
import com.example.app.MainActivity;
import com.example.app.AppConfig;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import com.example.app.services.WebServiceRegistry;
import com.example.app.UpdateManager; 
import com.example.app.StorageManager;

public class MaintenanceController {
    private static final String TAG = "MaintenanceController";
    private static volatile String currentStatusMessage = "Idle";

    public MaintenanceController() {
    }

    @RequestMapping(path="/api/maintenance/config", method="GET")
    public ResponseContext getMaintenanceConfig(RequestContext request) {
        Log.i(TAG, " -> REST API [GET]: Fetching maintenance configuration values.");
        String configJson = request.getAppConfig().getMaintenanceConfigJson();
        return ResponseContext.status(200)
                .contentType("application/json")
                .body(configJson)
                .build();
    }

    @RequestMapping(path="/api/maintenance/save", method="POST")
    public ResponseContext saveMaintenanceConfig(RequestContext request) {
        Log.i(TAG, " -> REST API [POST]: Committing maintenance profile data bundle properties.");
        
        // Extract parameters uniformly via request query context
        String autoUpdate = request.getQueryParam("autoUpdate");
        String interval = request.getQueryParam("interval");
        String url = request.getQueryParam("url");
        String useAuth = request.getQueryParam("useAuth");
        String user = request.getQueryParam("user");
        String pass = request.getQueryParam("pass");
        String subpath = request.getQueryParam("subpath");

        request.getAppConfig().saveMaintenanceSettings(autoUpdate, interval, url, useAuth, user, pass, subpath);
        
        return ResponseContext.status(200)
                .contentType("application/json")
                .body("{\"status\":\"success\",\"message\":\"Settings saved cleanly.\"}")
                .build();
    }

//    @RequestMapping(path="/api/maintenance/download", method="POST")
//    public ResponseContext triggerMaintenanceDownload(RequestContext request) {
//        Log.i(TAG, " -> REST API [POST]: Flattened manual background update sequence triggered.");
//        
//        // 1. Parse operational flags directly from incoming request context parameters
//        String mergeQuery = request.getQueryParam("merge");
//        final boolean shouldMerge = "true".equalsIgnoreCase(mergeQuery);
//        
//        final android.content.Context appCtx = request.getAndroidContext();
//        if (!(appCtx instanceof com.example.app.MainActivity)) {
//            return ResponseContext.status(500)
//                    .contentType("application/json")
//                    .body("{\"status\":\"error\",\"message\":\"Context context tracking configuration mismatch.\"}")
//                    .build();
//        }
//        final com.example.app.MainActivity activity = (com.example.app.MainActivity) appCtx;
//        final com.example.app.AppConfig appConfig = request.getAppConfig();
//
//        // 2. Spawn a single isolated worker thread to execute the whole pipeline sequentially
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                java.net.HttpURLConnection connection = null;
//                java.io.InputStream inputStream = null;
//                java.io.FileOutputStream outputStream = null;
//                
//                try {
//                    String targetUrlStr = appConfig.getUpdateTargetUrl();
//                    if (targetUrlStr == null || targetUrlStr.isEmpty()) {
//                        throw new java.io.IOException("Aborting update: Target URL configuration property is empty.");
//                    }
//                    
//                    Log.d(TAG, " -> Connecting flat network layer pipeline to endpoint: " + targetUrlStr);
//                    java.net.URL url = new java.net.URL(targetUrlStr);
//                    connection = (java.net.HttpURLConnection) url.openConnection();
//                    connection.setConnectTimeout(15000);
//                    connection.setReadTimeout(15000);
//                    connection.setRequestMethod("GET");
//                    
//                    if (appConfig.useAuthentication()) {
//                        Log.d(TAG, " -> Building flat basic authentication authorization parameters context.");
//                        String authStr = appConfig.getAuthUsername() + ":" + appConfig.getAuthPassword();
//                        String base64Auth = android.util.Base64.encodeToString(authStr.getBytes(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
//                        connection.setRequestProperty("Authorization", "Basic " + base64Auth);
//                    }
//                    
//                    connection.connect();
//                    int responseCode = connection.getResponseCode();
//                    if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
//                        throw new java.io.IOException("Remote source server returned invalid status code tracking signature: " + responseCode);
//                    }
//                    
//                    java.io.File tempZipFile = new java.io.File(appCtx.getCacheDir(), "remote_deployment_package.zip");
//                    if (tempZipFile.exists()) {
//                        tempZipFile.delete();
//                    }
//                    
//                    inputStream = new java.io.BufferedInputStream(connection.getInputStream());
//                    outputStream = new java.io.FileOutputStream(tempZipFile);
//                    byte[] dataBuffer = new byte[4096];
//                    int bytesRead;
//                    
//                    Log.i(TAG, " -> Streaming remote packet payload down into local staging file cache...");
//                    while ((bytesRead = inputStream.read(dataBuffer, 0, 4096)) != -1) {
//                        outputStream.write(dataBuffer, 0, bytesRead);
//                    }
//                    outputStream.flush();
//                    outputStream.close();
//                    outputStream = null;
//                    
//                    java.io.File sandboxDir = new java.io.File(appCtx.getFilesDir(), "www");
//                    Log.i(TAG, " -> Network fetch complete. Initiating file deployment sequence directly in sandbox: " + sandboxDir.getAbsolutePath());
//                    // 3. Flattened Workspace Reset & Directory Staging
//                    if (sandboxDir.exists()) {
//                        if (!shouldMerge) {
//                            Log.d(TAG, " -> [Mode: Overwrite] Executing inline recursive deletion of old assets...");
//                            java.util.Stack<java.io.File> deleteStack = new java.util.Stack<>();
//                            deleteStack.push(sandboxDir);
//                            java.util.List<java.io.File> filesToDelete = new java.util.ArrayList<>();
//                            
//                            while (!deleteStack.isEmpty()) {
//                                java.io.File current = deleteStack.pop();
//                                filesToDelete.add(current);
//                                java.io.File[] children = current.listFiles();
//                                if (children != null) {
//                                    for (java.io.File child : children) {
//                                        deleteStack.push(child);
//                                    }
//                                }
//                            }
//                            // Delete from back to front to ensure files go before their parents
//                            for (int i = filesToDelete.size() - 1; i >= 0; i--) {
//                                filesToDelete.get(i).delete();
//                            }
//                        } else {
//                            Log.d(TAG, " -> [Mode: Merge] Target directory exists. Skipping deletion step.");
//                        }
//                    }
//
//                    if (!sandboxDir.exists() && !sandboxDir.mkdirs()) {
//                        throw new java.io.IOException("Failed initializing app sandbox workspace layout.");
//                    }
//
//                    // 4. Pre-scan for Automatic GitHub Parent Prefix Tracking
//                    String commonRootPrefix = "";
//                    java.util.zip.ZipInputStream scanIn = new java.util.zip.ZipInputStream(new java.io.FileInputStream(tempZipFile));
//                    java.util.zip.ZipEntry firstEntry = scanIn.getNextEntry();
//                    if (firstEntry != null && firstEntry.isDirectory()) {
//                        commonRootPrefix = firstEntry.getName();
//                        Log.i(TAG, " -> Flattened Layer: GitHub root wrapper directory prefix detected: " + commonRootPrefix);
//                    }
//                    scanIn.close();
//
//                    // 5. Inlined ZIP Extraction Mechanism
//                    java.util.zip.ZipInputStream zipIn = new java.util.zip.ZipInputStream(new java.io.FileInputStream(tempZipFile));
//                    java.util.zip.ZipEntry entry;
//                    byte[] extractBuffer = new byte[4096];
//
//                    while ((entry = zipIn.getNextEntry()) != null) {
//                        String entryName = entry.getName();
//                        if (!commonRootPrefix.isEmpty() && entryName.startsWith(commonRootPrefix)) {
//                            entryName = entryName.substring(commonRootPrefix.length());
//                        }
//                        if (entryName.startsWith("/")) {
//                            entryName = entryName.substring(1);
//                        }
//                        if (entryName.isEmpty()) {
//                            zipIn.closeEntry();
//                            continue;
//                        }
//
//                        java.io.File targetFile = new java.io.File(sandboxDir, entryName);
//                        
//                        // Strict validation guard against malicious directory traversal payload strings (Zip Slip mitigation)
//                        if (!targetFile.getCanonicalPath().startsWith(sandboxDir.getCanonicalPath())) {
//                            throw new SecurityException("Zip Slip directory traversal attack attempt blocked inline: " + entry.getName());
//                        }
//
//                        if (entry.isDirectory()) {
//                            if (!targetFile.exists()) {
//                                targetFile.mkdirs();
//                            }
//                        } else {
//                            java.io.File parent = targetFile.getParentFile();
//                            if (parent != null && !parent.exists()) {
//                                parent.mkdirs();
//                            }
//                            
//                            java.io.FileOutputStream fos = new java.io.FileOutputStream(targetFile);
//                            int len;
//                            while ((len = zipIn.read(extractBuffer)) > 0) {
//                                fos.write(extractBuffer, 0, len);
//                            }
//                            fos.close();
//                        }
//                        zipIn.closeEntry();
//                    }
//                    zipIn.close();
//                    Log.i(TAG, " -> Inline extraction transaction finished.");
//                    // 6. Flattened Dual-Tier Storage Mirroring Task (Sync out to External Storage)
//                    Log.i(TAG, "─── STARTING SD CARD SYNC TRANSACTION INLINE ───");
//                    String folderName = appConfig.getWorkspaceFolderName();
//                    java.io.File publicDocsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS);
//                    
//                    if (publicDocsDir != null) {
//                        java.io.File externalTargetDir = new java.io.File(new java.io.File(publicDocsDir, folderName), "www");
//                        Log.d(TAG, " -> Target Destination Path context: " + externalTargetDir.getAbsolutePath());
//                        
//                        // Clean external layout context cleanly if explicit overwrite mode matches
//                        if (externalTargetDir.exists() && !shouldMerge) {
//                            Log.d(TAG, " -> Pre-purging old public storage layout structures...");
//                            java.util.Stack<java.io.File> extDeleteStack = new java.util.Stack<>();
//                            extDeleteStack.push(externalTargetDir);
//                            java.util.List<java.io.File> extFilesToDelete = new java.util.ArrayList<>();
//                            
//                            while (!extDeleteStack.isEmpty()) {
//                                java.io.File currentExt = extDeleteStack.pop();
//                                extFilesToDelete.add(currentExt);
//                                java.io.File[] extChildren = currentExt.listFiles();
//                                if (extChildren != null) {
//                                    for (java.io.File child : extChildren) {
//                                        extDeleteStack.push(child);
//                                    }
//                                }
//                            }
//                            for (int i = extFilesToDelete.size() - 1; i >= 0; i--) {
//                                extFilesToDelete.get(i).delete();
//                            }
//                        }
//
//                        if (!externalTargetDir.exists()) {
//                            externalTargetDir.mkdirs();
//                        }
//
//                        // Flattened structural inline copy sequence using java.util.Stack
//                        java.util.Stack<java.io.File[]> copyStack = new java.util.Stack<>();
//                        copyStack.push(new java.io.File[]{sandboxDir, externalTargetDir});
//                        int totalFilesCloned = 0;
//
//                        while (!copyStack.isEmpty()) {
//                            java.io.File[] pair = copyStack.pop();
//                            java.io.File src = pair[0];
//                            java.io.File dest = pair[1];
//
//                            if (src.isDirectory()) {
//                                if (!dest.exists()) {
//                                    dest.mkdirs();
//                                }
//                                java.io.File[] children = src.listFiles();
//                                if (children != null) {
//                                    for (java.io.File child : children) {
//                                        copyStack.push(new java.io.File[]{child, new java.io.File(dest, child.getName())});
//                                    }
//                                }
//                            } else {
//                                Log.d(TAG, "    [Inline Copying File] " + src.getName() + " -> " + dest.getAbsolutePath());
//                                try (java.nio.channels.FileChannel srcChannel = new java.io.FileInputStream(src).getChannel();
//                                     java.nio.channels.FileChannel destChannel = new java.io.FileOutputStream(dest).getChannel()) {
//                                    srcChannel.transferTo(0, srcChannel.size(), destChannel);
//                                    totalFilesCloned++;
//                                } catch (java.io.IOException ioEx) {
//                                    Log.e(TAG, "    !! INLINE WRITE FAILURE !! Target blocked: " + dest.getAbsolutePath() + " -> " + ioEx.getMessage());
//                                }
//                            }
//                        }
//                        Log.i(TAG, "─── SD CARD SYNC COMPLETE (Files Cloned Inline: " + totalFilesCloned + ") ───");
//                    } else {
//                        Log.e(TAG, " !! CRITICAL !! Storage tier unmounted or missing public Documents target reference root context.");
//                    }
//
//                    // 7. Cleanup operations and dynamic application re-navigation hooks
//                    if (tempZipFile.exists()) {
//                        tempZipFile.delete();
//                    }
//                    Log.i(TAG, " -> Inline Update System finalized cleanly. Signaling interface dispatchers...");
//                    
//                    // Dispatch the main UI loop reload trigger across thread barriers safely
//                    activity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            activity.reloadPrimaryWebViewToRoot();
//                        }
//                    });
//
//                } catch (Exception e) {
//                    Log.e(TAG, " -> Critical flat pipeline thread exception caught: " + e.getMessage(), e);
//                } finally {
//                    try { if (outputStream != null) outputStream.close(); } catch (java.io.IOException ignored) {}
//                    try { if (inputStream != null) inputStream.close(); } catch (java.io.IOException ignored) {}
//                    if (connection != null) connection.disconnect();
//                }
//            }
//        }).start();
//
//        // Immediately respond 200 to keep the asynchronous front-end polling framework operational
//        return ResponseContext.status(200)
//                .contentType("application/json")
//                .body("{\"status\":\"success\",\"message\":\"Asynchronous flattened execution thread processing pipeline launched successfully.\"}")
//                .build();
//    }
// [MaintenanceController.java] [triggerMaintenanceDownload - Chunk 0 of 2]

@RequestMapping(path="/api/maintenance/download",method="POST")
public ResponseContext triggerMaintenanceDownload(RequestContext request){
    Log.i(TAG," -> REST API [POST]: Flattened manual background update sequence triggered.");
    String mergeQuery=request.getQueryParam("merge");
    final boolean shouldMerge="true".equalsIgnoreCase(mergeQuery);
    final android.content.Context appCtx=request.getAndroidContext();
    if(!(appCtx instanceof com.example.app.MainActivity)){
        return ResponseContext.status(500).contentType("application/json").body("{\"status\":\"error\",\"message\":\"Context context tracking configuration mismatch.\"}").build();
    }
    final com.example.app.MainActivity activity=(com.example.app.MainActivity)appCtx;
    final com.example.app.AppConfig appConfig=request.getAppConfig();
    new Thread(new Runnable(){
        @Override public void run(){
            java.net.HttpURLConnection connection=null;
            java.io.InputStream inputStream=null;
            java.io.FileOutputStream outputStream=null;
            try{
                String targetUrlStr=appConfig.getUpdateTargetUrl();
                if(targetUrlStr==null||targetUrlStr.isEmpty()){
                    throw new java.io.IOException("Aborting update: Target URL configuration property is empty.");
                }
                Log.d(TAG," -> Connecting flat network layer pipeline to endpoint: "+targetUrlStr);
                java.net.URL url=new java.net.URL(targetUrlStr);
                connection=(java.net.HttpURLConnection)url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestMethod("GET");
                if(appConfig.useAuthentication()){
                    Log.d(TAG," -> Building flat basic authentication authorization parameters context.");
                    String authStr=appConfig.getAuthUsername()+":"+appConfig.getAuthPassword();
                    String base64Auth=android.util.Base64.encodeToString(authStr.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);
                    connection.setRequestProperty("Authorization","Basic "+base64Auth);
                }
                connection.connect();
                int responseCode=connection.getResponseCode();
                if(responseCode!=java.net.HttpURLConnection.HTTP_OK){
                    throw new java.io.IOException("Remote source server returned invalid status code tracking signature: "+responseCode);
                }
                java.io.File tempZipFile=new java.io.File(appCtx.getCacheDir(),"remote_deployment_package.zip");
                if(tempZipFile.exists()){
                    tempZipFile.delete();
                }
                inputStream=new java.io.BufferedInputStream(connection.getInputStream());
                outputStream=new java.io.FileOutputStream(tempZipFile);
                byte[]dataBuffer=new byte[4096];
                int bytesRead;
                Log.i(TAG," -> Streaming remote packet payload down into local staging file cache...");
                while((bytesRead=inputStream.read(dataBuffer,0,4096))!=-1){
                    outputStream.write(dataBuffer,0,bytesRead);
                }
                outputStream.flush();
                outputStream.close();
                outputStream=null;
                java.io.File sandboxDir=new java.io.File(appCtx.getFilesDir(),"www");
                Log.i(TAG," -> Network fetch complete. Initiating file deployment sequence directly in sandbox: "+sandboxDir.getAbsolutePath());
                if(sandboxDir.exists()){
                    if(!shouldMerge){
                        Log.d(TAG," -> [Mode: Overwrite] Executing inline recursive deletion of old assets...");
                        java.util.Stack<java.io.File>deleteStack=new java.util.Stack<>();
                        deleteStack.push(sandboxDir);
                        java.util.List<java.io.File>filesToDelete=new java.util.ArrayList<>();
                        while(!deleteStack.isEmpty()){
                            java.io.File current=deleteStack.pop();
                            filesToDelete.add(current);
                            java.io.File[]children=current.listFiles();
                            if(children!=null){
                                for(java.io.File child:children){
                                    deleteStack.push(child);
                                }
                            }
                        }
                        for(int i=filesToDelete.size()-1;i>=0;i--){
                            filesToDelete.get(i).delete();
                        }
                    }else{
                        Log.d(TAG," -> [Mode: Merge] Target directory exists. Skipping deletion step.");
                    }
                }
                if(!sandboxDir.exists()&&!sandboxDir.mkdirs()){
                    throw new java.io.IOException("Failed initializing app sandbox workspace layout.");
                }
                String commonRootPrefix="";
                java.util.zip.ZipInputStream scanIn=new java.util.zip.ZipInputStream(new java.io.FileInputStream(tempZipFile));
                java.util.zip.ZipEntry firstEntry=scanIn.getNextEntry();
                if(firstEntry!=null&&firstEntry.isDirectory()){
                    commonRootPrefix=firstEntry.getName();
                    Log.i(TAG," -> Flattened Layer: GitHub root wrapper directory prefix detected: "+commonRootPrefix);
                }
                scanIn.close();

                // ─── START OF SUBPATH PARSING LOGIC ───
                String userSubpath = appConfig.getUpdateTargetSubpath();
                if (userSubpath == null) {
                    userSubpath = "";
                }
                userSubpath = userSubpath.trim();
                if (userSubpath.startsWith("/")) {
                    userSubpath = userSubpath.substring(1);
                }
                if (userSubpath.endsWith("/")) {
                    userSubpath = userSubpath.substring(0, userSubpath.length() - 1);
                }
                // ─── END OF SUBPATH PARSING LOGIC ───

                java.util.zip.ZipInputStream zipIn=new java.util.zip.ZipInputStream(new java.io.FileInputStream(tempZipFile));
                java.util.zip.ZipEntry entry;
                byte[]extractBuffer=new byte[4096];
                while((entry=zipIn.getNextEntry())!=null){
                    String entryName=entry.getName();
                    if(!commonRootPrefix.isEmpty()&&entryName.startsWith(commonRootPrefix)){
                        entryName=entryName.substring(commonRootPrefix.length());
                    }
                    if(entryName.startsWith("/")){
                        entryName=entryName.substring(1);
                    }

                    // ─── START OF SUBPATH FILTERING & EXTRACTION TRANSFORMATION ───
                    if (!userSubpath.isEmpty()) {
                        if (!entryName.startsWith(userSubpath)) {
                            zipIn.closeEntry();
                            continue;
                        }
                        entryName = entryName.substring(userSubpath.length());
                        if (entryName.startsWith("/")) {
                            entryName = entryName.substring(1);
                        }
                        if (entryName.isEmpty()) {
                            zipIn.closeEntry();
                            continue;
                        }
                    }
                    // ─── END OF SUBPATH FILTERING & EXTRACTION TRANSFORMATION ───

                    if(entryName.isEmpty()){
                        zipIn.closeEntry();
                        continue;
                    }
                    java.io.File targetFile=new java.io.File(sandboxDir,entryName);
                    if(!targetFile.getCanonicalPath().startsWith(sandboxDir.getCanonicalPath())){
                        throw new SecurityException("Zip Slip directory traversal attack attempt blocked inline: "+entry.getName());
                    }
                    if(entry.isDirectory()){
                        if(!targetFile.exists()){
                            targetFile.mkdirs();
                        }
                    }else{
                        java.io.File parent=targetFile.getParentFile();
                        if(parent!=null&&!parent.exists()){
                            parent.mkdirs();
                        }
                        java.io.FileOutputStream fos=new java.io.FileOutputStream(targetFile);
                        int len;
                        while((len=zipIn.read(extractBuffer))>0){
                            fos.write(extractBuffer,0,len);
                        }
                        fos.close();
                    }
                    zipIn.closeEntry();
                }
                zipIn.close();
                Log.i(TAG," -> Inline extraction transaction finished.");
                Log.i(TAG,"─── STARTING SD CARD SYNC TRANSACTION INLINE ───");
                String folderName=appConfig.getWorkspaceFolderName();
                java.io.File publicDocsDir=android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS);
                if(publicDocsDir!=null){
                    java.io.File externalTargetDir=new java.io.File(new java.io.File(publicDocsDir,folderName),"www");
                    Log.d(TAG," -> Target Destination Path context: "+externalTargetDir.getAbsolutePath());
                    if(externalTargetDir.exists()&&!shouldMerge){
                        Log.d(TAG," -> Pre-purging old public storage layout structures...");
                        java.util.Stack<java.io.File>extDeleteStack=new java.util.Stack<>();
                        extDeleteStack.push(externalTargetDir);
                        java.util.List<java.io.File>extFilesToDelete=new java.util.ArrayList<>();
                        while(!extDeleteStack.isEmpty()){
                            java.io.File currentExt=extDeleteStack.pop();
                            extFilesToDelete.add(currentExt);
                            java.io.File[]extChildren=currentExt.listFiles();
                            if(extChildren!=null){
                                for(java.io.File child:extChildren){
                                    extDeleteStack.push(child);
                                }
                            }
                        }
                        for(int i=extFilesToDelete.size()-1;i>=0;i--){
                            extFilesToDelete.get(i).delete();
                        }
                    }
                    if(!externalTargetDir.exists()){
                        externalTargetDir.mkdirs();
                    }
                    java.util.Stack<java.io.File[]>copyStack=new java.util.Stack<>();
                    copyStack.push(new java.io.File[]{sandboxDir,externalTargetDir});
                    int totalFilesCloned=0;
                    while(!copyStack.isEmpty()){
                        java.io.File[]pair=copyStack.pop();
                        java.io.File src=pair[0];
                        java.io.File dest=pair[1];
                        if(src.isDirectory()){
                            if(!dest.exists()){
                                dest.mkdirs();
                            }
                            java.io.File[]children=src.listFiles();
                            if(children!=null){
                                for(java.io.File child:children){
                                    copyStack.push(new java.io.File[]{child,new java.io.File(dest,child.getName())});
                                }
                            }
                        }else{
                            Log.d(TAG,"    [Inline Copying File] "+src.getName()+" -> "+dest.getAbsolutePath());
                            try(java.nio.channels.FileChannel srcChannel=new java.io.FileInputStream(src).getChannel();java.nio.channels.FileChannel destChannel=new java.io.FileOutputStream(dest).getChannel()){
                                srcChannel.transferTo(0,srcChannel.size(),destChannel);
                                totalFilesCloned++;
                            }catch(java.io.IOException ioEx){
                                Log.e(TAG,"    !! INLINE WRITE FAILURE !! Target blocked: "+dest.getAbsolutePath()+" -> "+ioEx.getMessage());
                            }
                        }
                    }
                    Log.i(TAG,"─── SD CARD SYNC COMPLETE (Files Cloned Inline: "+totalFilesCloned+") ───");
                }else{
                    Log.e(TAG," !! CRITICAL !! Storage tier unmounted or missing public Documents target reference root context.");
                }
                if(tempZipFile.exists()){
                    tempZipFile.delete();
                }
                Log.i(TAG," -> Inline Update System finalized cleanly. Signaling interface dispatchers...");
                activity.runOnUiThread(new Runnable(){
                    @Override public void run(){
                        activity.reloadPrimaryWebViewToRoot();
                    }
                });
            }catch(Exception e){
                Log.e(TAG," -> Critical flat pipeline thread exception caught: "+e.getMessage(),e);
            }finally{
                try{if(outputStream!=null)outputStream.close();}catch(java.io.IOException ignored){}
                try{if(inputStream!=null)inputStream.close();}catch(java.io.IOException ignored){}
                if(connection!=null)connection.disconnect();
            }
        }
    }).start();
    return ResponseContext.status(200).contentType("application/json").body("{\"status\":\"success\",\"message\":\"Asynchronous flattened execution thread processing pipeline launched successfully.\"}").build();
}



    @RequestMapping(path="/api/maintenance/sync-sd", method="POST")
    public ResponseContext syncSandboxStorage(RequestContext request) {
        Log.i(TAG, " -> REST API [POST]: Triggering sandbox workspace duplication sync out to SD Card...");
        
        MainActivity activity = (MainActivity) request.getAndroidContext();
        StorageManager directStorageManager = new StorageManager(activity, request.getAppConfig());
        directStorageManager.syncSandboxToExternal();

        return ResponseContext.status(200)
                .contentType("application/json")
                .body("{\"status\":\"success\",\"message\":\"SD Card sync task spawned cleanly.\"}")
                .build();
    }

    @RequestMapping(path="/api/maintenance/close", method="POST")
    public ResponseContext closeMaintenanceInterface(RequestContext request) {
        Log.i(TAG, " -> REST API [POST]: Interface exit action requested.");
        
        MainActivity activity = (MainActivity) request.getAndroidContext();
        activity.runOnUiThread(activity::onSecretTriggered);

        return ResponseContext.status(200)
                .contentType("application/json")
                .body("{\"status\":\"success\",\"message\":\"Teardown signal passed.\"}")
                .build();
    }

    @RequestMapping(path="/api/maintenance/status", method="GET")
    public ResponseContext getMaintenanceStatus(RequestContext request) {
        String currentStatus = UpdateManager.getCurrentStatus();
        String payload = String.format("{\"status\":\"%s\"}", currentStatus);
        
        return ResponseContext.status(200)
                .contentType("application/json")
                .body(payload)
                .build();
    }

}

