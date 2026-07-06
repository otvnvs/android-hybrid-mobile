package com.example.app.services.webview;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;

import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;

import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

public class WebViewController {
    private static final String TAG = "WebViewController";

    public WebViewController() {}

    @RequestMapping(path = "/api/webview/diagnostics", method = "GET")
    public ResponseContext getWebViewDiagnostics(RequestContext request) {
        try {
            JSONObject root = new JSONObject();
            Context context = request.getAndroidContext();

            // 1. Structural Application Cleartext Security Policies (Reflection-Safe)
            JSONObject security = new JSONObject();
            boolean cleartextAllowed = true; 

            if (Build.VERSION.SDK_INT >= 23) {
                try {
                    Class<?> nspClass = Class.forName("android.security.NetworkSecurityPolicy");
                    Object nspInstance = nspClass.getMethod("getInstance").invoke(null);
                    try {
                        cleartextAllowed = (Boolean) nspClass.getMethod("isCleartextTrafficAllowed").invoke(nspInstance);
                    } catch (NoSuchMethodException nsme) {
                        cleartextAllowed = (Boolean) nspClass.getMethod("isCleartextTrafficAllowed", String.class).invoke(nspInstance, "localhost");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Reflection failed to extract cleartext traffic rules: " + e.getMessage());
                }
            }

            security.put("uses_cleartext_traffic_allowed", cleartextAllowed);
            security.put("target_sdk_compliance", context != null ? context.getApplicationInfo().targetSdkVersion : -1);
            root.put("security_policy", security);

            // 2. Active Cookie Engine Parameter Checks
            JSONObject cookies = new JSONObject();
            try {
                CookieManager cookieManager = CookieManager.getInstance();
                if (cookieManager != null) {
                    cookies.put("accept_cookies_enabled", cookieManager.acceptCookie());
                    if (Build.VERSION.SDK_INT >= 21) {
                        cookies.put("has_cookies_stored", cookieManager.hasCookies());
                    } else {
                        cookies.put("has_cookies_stored", "unknown_below_api21");
                    }
                } else {
                    cookies.put("status", "CookieManager unavailable");
                }
            } catch (Exception e) {
                cookies.put("status", "error_reading_cookies: " + e.getMessage());
            }
            root.put("cookie_engine", cookies);

            // 3. Thread-Safe Base Setting Configurations
            JSONObject configurations = new JSONObject();
            // Statically map core active properties instead of instantiating widgets on background sockets
            configurations.put("javascript_enabled", true); 
            configurations.put("dom_storage_enabled", true);
            configurations.put("database_enabled", true);
            configurations.put("file_access_enabled", true);
            configurations.put("loads_images_automatically", true);
            configurations.put("mixed_content_mode", 2); // COMPATIBILITY_MODE default for security verification
            configurations.put("active_cache_mode", "LOAD_DEFAULT");
            configurations.put("status", "Thread-safe default values mapping applied");
            root.put("configurations", configurations);

            // 4. Low-Level Web Cache & Local Database Partition Sizes
            JSONObject webStorage = new JSONObject();
            if (context != null) {
                java.io.File cacheDir = context.getCacheDir();
                java.io.File appCacheDir = new java.io.File(cacheDir.getParentFile(), "app_webview");
                
                long cacheSizeBytes = 0;
                if (appCacheDir.exists() && appCacheDir.isDirectory()) {
                    cacheSizeBytes = calculateDirectorySize(appCacheDir);
                } else if (cacheDir.exists()) {
                    cacheSizeBytes = calculateDirectorySize(cacheDir);
                }
                
                webStorage.put("webview_cache_directory_path", appCacheDir.getAbsolutePath());
                webStorage.put("webview_cache_allocated_bytes", cacheSizeBytes);
                webStorage.put("status", "success");
            } else {
                webStorage.put("status", "Context unavailable");
            }
            root.put("storage_allocation", webStorage);

            return ResponseContext.status(200)
                    .contentType("application/json")
                    .header("X-Server-Response-Engine", "Android-Native-JVM")
                    .body(root.toString())
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "WebView diagnostics interrogation pipeline crash", e);
            return buildErrorResponse(500, "WebView inspection pipeline error: " + e.getMessage());
        }
    }

    private long calculateDirectorySize(java.io.File directory) {
        long length = 0;
        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isFile()) {
                    length += file.length();
                } else {
                    length += calculateDirectorySize(file);
                }
            }
        }
        return length;
    }

    private ResponseContext buildErrorResponse(int code, String message) {
        JSONObject errJson = new JSONObject();
        try {
            errJson.put("status", "error");
            errJson.put("message", message);
        } catch (Exception ignored) {}
        return ResponseContext.status(code)
                .contentType("application/json")
                .body(errJson.toString())
                .build();
    }
}

