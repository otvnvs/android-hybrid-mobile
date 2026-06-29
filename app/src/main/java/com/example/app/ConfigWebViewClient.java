package com.example.app;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.os.Environment;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import android.util.Log;
import android.view.KeyEvent;//Sat Jun 27 13:11:10 SAST 2026 dominique phone secret keys


public class ConfigWebViewClient extends WebViewClient {
    private static final String TAG = "JAVA_ConfigWebViewClient";
    private final Context mContext;
    private final AppConfig mConfig;

    public ConfigWebViewClient(Context context, AppConfig config) {
        this.mContext = context;
        this.mConfig = config;
    }

	//Sat Jun 27 13:10:18 SAST 2026 dominique phone secret keys
    /**
     * Prevents the maintenance browser viewport from consuming volume rockers.
     * Passes the button event chain back up to the parent Activity layout layer.
     */
    @Override
    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true;
        }
        return super.shouldOverrideKeyEvent(view, event);
    }

    private String getRawVirtualHost() {
        if (mConfig == null || mConfig.getVirtualHost().isEmpty()) return null;
        return Uri.parse(mConfig.getVirtualHost()).getHost();
    }

//    @Override
//    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
//        return handleUrlRouting(view, request.getUrl());
//    }
//    @Override
//    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
//        Uri uri = request.getUrl();
//        String targetHost = uri.getHost();
//        String rawVirtualHost = getRawVirtualHost();
//        if (targetHost != null && targetHost.equals(rawVirtualHost)) {
//            String path = uri.getPath();
//            if (path != null) {
//                if (path.startsWith("/")) {
//                    path = path.substring(1);
//                }
//                if (path.isEmpty()) {
//                    path = "index.html";
//                }
//                try {
//                    // Resolve file from SD card, Sandbox, or assets
//                    InputStream targetStream = resolveAssetStream(path);
//                    String mimeType = getMimeType(path);
//                    return new WebResourceResponse(mimeType, "UTF-8", targetStream);
//                } catch (IOException e) {
//                    Log.e(TAG, "Error loading local maintenance asset: " + e.toString());
//                    String errorHtml = "<html><body style='font-family:sans-serif;padding:20px;text-align:center;'>"
//                            + "<h2>Maintenance Error</h2>"
//                            + "<p>Failed to load the maintenance interface local-side.</p>"
//                            + "</body></html>";
//                    InputStream fallbackStream = new ByteArrayInputStream(errorHtml.getBytes(StandardCharsets.UTF_8));
//                    return new WebResourceResponse("text/html", "UTF-8", fallbackStream);
//                }
//            }
//        }
//        return super.shouldInterceptRequest(view, request);
//    }
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        String path = uri.getPath();
        String method = request.getMethod().toUpperCase();
        
        // Suppress browser favicon request clutter silently
        if (path != null && path.contains("favicon.ico")) {
            return new WebResourceResponse("image/x-icon", "UTF-8", new java.io.ByteArrayInputStream(new byte[0]));
        }

        String targetHost = uri.getHost();
        String rawVirtualHost = getRawVirtualHost();

        // Process Virtual Domain Interceptions securely local-side
        if (targetHost != null && targetHost.equals(rawVirtualHost) && path != null) {
            final MainActivity activity = (MainActivity) mContext;
            
            // ─── ROUTE 1: GET PROFILE DATA ──────────────────────────────────────────
            if ("/api/maintenance/config".equals(path) && "GET".equals(method)) {
                Log.i(TAG, " -> REST API [GET]: Fetching maintenance configuration values.");
                String configJson = mConfig.getMaintenanceConfigJson();
                return createJsonResponse(configJson, rawVirtualHost);
            }

//            // ─── ROUTE 2: POST SAVE PROFILE DATA ────────────────────────────────────
//            if ("/api/maintenance/save".equals(path) && "POST".equals(method)) {
//                Log.i(TAG, " -> REST API [POST]: Committing maintenance profile data bundle properties.");
//                
//                // Read configurations natively right out of the API route query stream parameters matrix
//                mConfig.saveMaintenanceSettings(
//                    uri.getQueryParameter("autoUpdate"),
//                    uri.getQueryParameter("interval"),
//                    uri.getQueryParameter("url"),
//                    uri.getQueryParameter("useAuth"),
//                    uri.getQueryParameter("user"),
//                    uri.getQueryParameter("pass")
//                );
//                return createJsonResponse("{\"status\":\"success\",\"message\":\"Settings saved cleanly.\"}", rawVirtualHost);
//            }
            // ─── ROUTE 2: POST SAVE PROFILE DATA ────────────────────────────────────
            if ("/api/maintenance/save".equals(path) && "POST".equals(method)) {
                Log.i(TAG, " -> REST API [POST]: Committing maintenance profile data bundle properties.");
                
                mConfig.saveMaintenanceSettings(
                    uri.getQueryParameter("autoUpdate"),
                    uri.getQueryParameter("interval"),
                    uri.getQueryParameter("url"),
                    uri.getQueryParameter("useAuth"),
                    uri.getQueryParameter("user"),
                    uri.getQueryParameter("pass"),
                    uri.getQueryParameter("subpath") // Passed cleanly to storage engine
                );
                return createJsonResponse("{\"status\":\"success\",\"message\":\"Settings saved cleanly.\"}", rawVirtualHost);
            }

//            // ─── ROUTE 3: POST TRIGGER REMOTE UPDATE ────────────────────────────────
//            if ("/api/maintenance/download".equals(path) && "POST".equals(method)) {
//                Log.i(TAG, " -> REST API [POST]: User invoked manual background download zip update loop.");
//                // activity.getUpdateManager().startZipDownload();
//                return createJsonResponse("{\"status\":\"success\",\"message\":\"Download sequence initialized.\"}", rawVirtualHost);
//            }
            // ─── ROUTE 3: POST TRIGGER REMOTE UPDATE ────────────────────────────────
//            if ("/api/maintenance/download".equals(path) && "POST".equals(method)) {
//                Log.i(TAG, " -> REST API [POST]: User invoked manual background download zip update loop.");
//                
//                // Dynamically instantiate the downloader module right here
//                UpdateManager directUpdateManager = new UpdateManager(activity, mConfig);
//                directUpdateManager.startZipDownload();
//                
//                return createJsonResponse("{\"status\":\"success\",\"message\":\"Download sequence initialized.\"}", rawVirtualHost);
//            }
            // ─── ROUTE 3: POST TRIGGER REMOTE UPDATE ────────────────────────────────
            if ("/api/maintenance/download".equals(path) && "POST".equals(method)) {
                Log.i(TAG, " -> REST API [POST]: User invoked manual background download zip update loop.");
                
                // Dynamically pass a completion listener interface routine block straight to the worker
                UpdateManager directUpdateManager = new UpdateManager(activity, mConfig);
                
                directUpdateManager.startZipDownload(new UpdateManager.OnUpdateCompleteListener() {
                    @Override
                    public void onUpdateFinished() {
                        Log.i(TAG, " -> Received notification hook from update worker thread. Forwarding reload routine to main thread UI run-loop...");
                        
                        // Force the UI execution block context to evaluate on Android's main UI render stream thread
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Invoke the main activity orchestrator to reload its primary content view viewport
                                activity.reloadPrimaryWebViewToRoot();
                            }
                        });
                    }
                });
                
                return createJsonResponse("{\"status\":\"success\",\"message\":\"Download sequence initialized.\"}", rawVirtualHost);
            }


//            // ─── ROUTE 4: POST DUPLICATE STORAGE SYNC ──────────────────────────────
//            if ("/api/maintenance/sync-sd".equals(path) && "POST".equals(method)) {
//                Log.i(TAG, " -> REST API [POST]: Triggering sandbox workspace duplication sync out to SD Card...");
//                activity.getStorageManager().syncSandboxToExternal();
//                return createJsonResponse("{\"status\":\"success\",\"message\":\"SD Card sync task spawned cleanly.\"}", rawVirtualHost);
//            }
            // ─── ROUTE 4: POST DUPLICATE STORAGE SYNC ──────────────────────────────
            if ("/api/maintenance/sync-sd".equals(path) && "POST".equals(method)) {
                Log.i(TAG, " -> REST API [POST]: Triggering sandbox workspace duplication sync out to SD Card...");
                
                // Fixed: Instantiate an isolated execution runner directly to bypass missing activity methods
                StorageManager directStorageManager = new StorageManager(activity, mConfig);
                directStorageManager.syncSandboxToExternal();
                
                return createJsonResponse("{\"status\":\"success\",\"message\":\"SD Card sync task spawned cleanly.\"}", rawVirtualHost);
            }


            // ─── ROUTE 5: POST VIEWPORT DISMISSAL CLOSURE ───────────────────────────
            if ("/api/maintenance/close".equals(path) && "POST".equals(method)) {
                Log.i(TAG, " -> REST API [POST]: Interface exit action requested.");
                activity.runOnUiThread(activity::onSecretTriggered); // Trigger view layer close safely on main thread
                return createJsonResponse("{\"status\":\"success\",\"message\":\"Teardown signal passed.\"}", rawVirtualHost);
            }

            // ─── ROUTE 6: GET LIVE WORKER PROGRESS STATUS ───────────────────────────
            if ("/api/maintenance/status".equals(path) && "GET".equals(method)) {
                String currentStatus = UpdateManager.getCurrentStatus();
                String payload = String.format("{\"status\":\"%s\"}", currentStatus);
                return createJsonResponse(payload, rawVirtualHost);
            }


            // ─── STANDARD FILE STREAM INTERCEPTOR FALLBACK ───────────────────────────
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.isEmpty()) {
                path = "index.html";
            }
            try {
                InputStream targetStream = resolveAssetStream(path);
                String mimeType = getMimeType(path);
                return new WebResourceResponse(mimeType, "UTF-8", targetStream);
            } catch (IOException e) {
                Log.e(TAG, "Error loading local maintenance asset: " + e.toString());
                String errorHtml = "<html><body><h2>Maintenance Error</h2></body></html>";
                InputStream fallbackStream = new java.io.ByteArrayInputStream(errorHtml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return new WebResourceResponse("text/html", "UTF-8", fallbackStream);
            }
        }
        return super.shouldInterceptRequest(view, request);
    }



    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return handleUrlRouting(view, Uri.parse(url));
    }

    private InputStream resolveAssetStream(String relativePath) throws IOException {
        String formattedPath = "www/" + relativePath;
        String folderName = mConfig.getWorkspaceFolderName();
        File publicDocsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        
        if (publicDocsDir != null) {
            File workspaceFile = new File(new File(publicDocsDir, folderName), formattedPath);
            if (workspaceFile.exists() && workspaceFile.isFile()) {
                return new FileInputStream(workspaceFile);
            }
        }
        
        File sandboxFile = new File(mContext.getFilesDir(), formattedPath);
        if (sandboxFile.exists() && sandboxFile.isFile()) {
            return new FileInputStream(sandboxFile);
        }
        return mContext.getAssets().open(formattedPath);
    }

    private boolean handleUrlRouting(WebView view, Uri uri) {
        String scheme = uri.getScheme();
        
        // Isolate administrative hooks to this client instance exclusively
        if ("hybrid-action".equals(scheme)) {
            handleSecureNativeAction(uri);
            return true; // Stop standard page navigation instantly
        }

        String host = uri.getHost();
        String rawVirtualHost = getRawVirtualHost();
        if (host != null && (host.equals(rawVirtualHost) || host.endsWith("." + rawVirtualHost))) {
            return false;
        }
        return true;
    }

    private void handleSecureNativeAction(Uri uri) {
        String action = uri.getHost();
        Log.i(TAG, "Config Interception executing command: " + action);

        MainActivity activity = (mContext instanceof MainActivity) ? (MainActivity) mContext : null;
        if (activity == null) return;

        switch (action) {
            case "sync-sd":
                Log.i(TAG, "Triggering sandbox deployment duplication sync out to public SD Card...");
                // activity.getStorageManager().syncSandboxToExternal();
                break;
                
//            case "save-config":
//                String url = uri.getQueryParameter("url");
//                String interval = uri.getQueryParameter("interval");
//                String user = uri.getQueryParameter("user");
//                String pass = uri.getQueryParameter("pass");
//                Log.i(TAG, "Saving new maintenance profiles: " + url + " | Check Interval: " + interval);
//                // activity.saveMaintenanceSettings(url, interval, user, pass);
//                break;
            case "save-config":
                String autoUpdate = uri.getQueryParameter("autoUpdate");
                String interval = uri.getQueryParameter("interval");
                String url = uri.getQueryParameter("url");
                String useAuth = uri.getQueryParameter("useAuth");
                String user = uri.getQueryParameter("user");
                String pass = uri.getQueryParameter("pass");
                String subpath= uri.getQueryParameter("subpath");
                
                Log.i(TAG, "Config Interception executing command: save-config");
                
                // Fixed: Saves data directly via the configuration module
                mConfig.saveMaintenanceSettings(autoUpdate, interval, url, useAuth, user, pass, subpath);
                break;
                
            case "trigger-update":
                Log.i(TAG, "User clicked update download manually. Booting fetch loop...");
                // activity.getUpdateManager().startZipDownload();
                break;
        }
    }

    private String getMimeType(String path) {
        if (path.contains("?")) path = path.split("\\?")[0];
        if (path.contains("#")) path = path.split("#")[0];
        
        if (path.endsWith(".html") || path.endsWith(".htm")) return "text/html";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    /**
     * Atomically compiles a robust JSON response stream container complete with web isolation header parameters
     */
    private WebResourceResponse createJsonResponse(String jsonPayload, String host) {
        java.io.InputStream stream = new java.io.ByteArrayInputStream(
                jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
        WebResourceResponse response = new WebResourceResponse("application/json", "UTF-8", stream);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            java.util.Map<String, String> headers = new java.util.HashMap<>();
            headers.put("Access-Control-Allow-Origin", "https://" + host);
            headers.put("Access-Control-Allow-Credentials", "true");
            response.setResponseHeaders(headers);
        }
        return response;
    }

}

