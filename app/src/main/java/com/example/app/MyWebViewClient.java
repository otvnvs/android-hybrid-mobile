//package com.example.app;
//
//import android.content.Context;
//import android.content.Intent;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Environment;
//import android.util.Log;
//import android.webkit.WebResourceError;
//import android.webkit.WebResourceRequest;
//import android.webkit.WebResourceResponse;
//import android.webkit.WebView;
//import android.webkit.WebViewClient;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.nio.charset.StandardCharsets;
//import java.util.HashMap;
//import java.util.Map;
//import android.view.KeyEvent;
//import com.example.app.services.WebServiceRegistry;
//
//import java.io.PrintWriter;
//import java.io.StringWriter;
//
//
//class MyWebViewClient extends WebViewClient {
//    private static final String TAG = "JS_CONSOLE_MyWebViewClient";
//    private final Context mContext;
//    private final AppConfig mConfig;
//    private final WebServiceRegistry mServiceRegistry;
//private static String storedSessionCookies = "";
//
//    public MyWebViewClient(Context context, AppConfig config) {
//        this.mContext = context;
//        this.mConfig = config;
//        this.mServiceRegistry = new WebServiceRegistry(context); // Initialize registry
//    }
//
//@Override
//public void onPageStarted(android.webkit.WebView view, String url, android.graphics.Bitmap favicon) {
//    super.onPageStarted(view, url, favicon);
//    
//    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
//        view.evaluateJavascript(com.example.app.services.WebScripts.INTERCEPT_SCRIPT, null);
//    } else {
//        view.loadUrl("javascript:" + com.example.app.services.WebScripts.INTERCEPT_SCRIPT);
//    }
//
//    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
//        view.evaluateJavascript(com.example.app.services.WebScripts.WEBSOCKET_PROXY_SCRIPT, null);
//    } else {
//        view.loadUrl("javascript:" + com.example.app.services.WebScripts.WEBSOCKET_PROXY_SCRIPT);
//    }
//
//}
//    /**
//     * Prevents the internal browser core from consuming volume rocker keys.
//     * Returning true instructs the WebView to route these events back up to the Activity layout node.
//     */
//    @Override
//    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
//        int keyCode = event.getKeyCode();
//        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
//            return true;
//        }
//        return super.shouldOverrideKeyEvent(view, event);
//    }
//
//    private String getRawVirtualHost() {
//        if (mConfig == null || mConfig.getVirtualHost().isEmpty()) return null;
//        return Uri.parse(mConfig.getVirtualHost()).getHost();
//    }
//	@Override 
//	public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
//	    Uri uri = request.getUrl();
//	    String targetHost = uri.getHost();
//	    String rawVirtualHost = getRawVirtualHost();
//
//
//
//	    // =========================================================================
//	    // 1. VIRTUAL HOST INTERCEPTION (Local Handlers & Assets)
//	    // =========================================================================
//	    if (targetHost != null && targetHost.equals(rawVirtualHost)) {
//
//		String path = uri.getPath();
//		if (path != null) {
//		    String method = request.getMethod();
//
//		    // Delegate endpoints to the Service Registry (Export, Import, and future handlers)
//		    WebResourceResponse serviceResponse = mServiceRegistry.dispatch(mContext, mConfig, request, path, method);
//		    if (serviceResponse != null) {
//			return serviceResponse;
//		    }
//
//		    // Fallback: Resolve and serve static asset files local-side
//		    if (path.startsWith("/")) {
//			path = path.substring(1);
//		    }
//		    if (path.isEmpty()) {
//			path = "index.html";
//		    }
//		    try {
//			InputStream targetStream = resolveAssetStream(path);
//			String mimeType = getMimeType(path);
//			return new WebResourceResponse(mimeType, "UTF-8", targetStream);
//		    } catch (IOException e) {
//			Log.e(TAG, "Exception loading asset file path: " + e.toString());
//			String errorHtml = "<html><body style='font-family:sans-serif;padding:20px;text-align:center;'>"
//				+ "<h2>Application Error</h2>"
//				+ "<p>The requested application resource could not be loaded local-side.</p>"
//				+ "</body></html>";
//			InputStream fallbackStream = new ByteArrayInputStream(errorHtml.getBytes(StandardCharsets.UTF_8));
//			return new WebResourceResponse("text/html", "UTF-8", fallbackStream);
//		    }
//		}
//	    } else if (targetHost != null) {
//	        return super.shouldInterceptRequest(view, request);
//	    } else {
//		//new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {@Override public void run() {android.widget.Toast.makeText(mContext, "C:"+targetHost, android.widget.Toast.LENGTH_SHORT).show();}});
//	        return super.shouldInterceptRequest(view, request);
//            }
//	    return super.shouldInterceptRequest(view, request);
//	}
//    private InputStream resolveAssetStream(String relativePath) throws IOException {
//        String formattedPath = "www/" + relativePath;
//        String folderName = mConfig.getWorkspaceFolderName();
//        File publicDocsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
//        
//        if (publicDocsDir != null) {
//            File workspaceFile = new File(new File(publicDocsDir, folderName), formattedPath);
//            if (workspaceFile.exists() && workspaceFile.isFile()) {
//                return new FileInputStream(workspaceFile);
//            }
//        }
//        
//        File sandboxFile = new File(mContext.getFilesDir(), formattedPath);
//        if (sandboxFile.exists() && sandboxFile.isFile()) {
//            return new FileInputStream(sandboxFile);
//        }
//        return mContext.getAssets().open(formattedPath);
//    }
//
//    private String getMimeType(String path) {
//        if (path.contains("?")) path = path.split("\\?")[0];
//        if (path.contains("#")) path = path.split("#")[0];
//        
//        if (path.endsWith(".html") || path.endsWith(".htm")) return "text/html";
//        if (path.endsWith(".js")) return "application/javascript";
//        if (path.endsWith(".css")) return "text/css";
//        if (path.endsWith(".png")) return "image/png";
//        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
//        if (path.endsWith(".svg")) return "image/svg+xml";
//        return "application/octet-stream";
//    }
//
//    @Override
//    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
//        return handleUrlRouting(view, request.getUrl());
//    }
//
//    @SuppressWarnings("deprecation")
//    @Override
//    public boolean shouldOverrideUrlLoading(WebView view, String url) {
//        return handleUrlRouting(view, Uri.parse(url));
//    }
//
//    private boolean handleUrlRouting(WebView view, Uri uri) {
//        String host = uri.getHost();
//        String rawVirtualHost = getRawVirtualHost();
//        if (host != null && (host.equals(rawVirtualHost) || host.endsWith("." + rawVirtualHost))) {
//            return false;
//        }
//        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//        view.getContext().startActivity(intent);
//        return true;
//    }
//
//    @Override
//    public void onReceivedError(WebView webview, WebResourceRequest request, WebResourceError error) {
//        if (webview == null || request == null || error == null) return;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            int errorCode = error.getErrorCode();
//            String targetUrl = request.getUrl().toString();
//            if (request.isForMainFrame()) {
//                Log.e(TAG, "CRITICAL ERROR [" + errorCode + "] TARGET: " + targetUrl);
//                if (errorCode == WebViewClient.ERROR_FILE_NOT_FOUND || errorCode == WebViewClient.ERROR_HOST_LOOKUP 
//                        || errorCode == WebViewClient.ERROR_CONNECT || errorCode == WebViewClient.ERROR_UNKNOWN) {
//                    if (!targetUrl.contains("error.html") && mConfig != null) {
//                        webview.loadUrl(mConfig.getVirtualHost() + "/error.html");
//                    }
//                }
//            }
//        }
//    }
//
//    @SuppressWarnings("deprecation")
//    @Override
//    public void onReceivedError(WebView webview, int errorCode, String description, String failingUrl) {
//        if (webview != null && failingUrl != null && !failingUrl.contains("error.html")) {
//            Log.e(TAG, "LEGACY CRITICAL ERROR: " + description);
//            if (mConfig != null) {
//                webview.loadUrl(mConfig.getVirtualHost() + "/error.html");
//            }
//        }
//    }
//
//@Override
//public void onReceivedSslError(android.webkit.WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
//    String failingUrl = (error != null) ? error.getUrl() : "Unknown URL";
//    
//    // Check if the executing device architecture is legacy (Android 10 / API 30 or lower)
//    if (android.os.Build.VERSION.SDK_INT <= 30) {
//        android.util.Log.w("JS_CONSOLE", "[ZEBRA SSL RECOVERY] Overriding untrusted/expired certificate chain validation drop for path: " + failingUrl);
//        
//        // Force the Chromium engine to ignore the handshake warning and load the data natively
//        handler.proceed(); 
//    } else {
//        // Enforce default, secure behavior for modern hardware layers like your Samsung
//        android.util.Log.d("JS_CONSOLE", "[SSL DEFAULT] Passing standard system security trust rules validation check.");
//        super.onReceivedSslError(view, handler, error);
//    }
//}
//
//}
//--------------------------------------------------------------------------------
package com.example.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.example.app.services.WebServiceRegistry;

class MyWebViewClient extends WebViewClient {
    private static final String TAG = "JS_CONSOLE_MyWebViewClient";
    private final Context mContext;
    private final AppConfig mConfig;
    private final WebServiceRegistry mServiceRegistry;

    public MyWebViewClient(Context context, AppConfig config) {
        this.mContext = context;
        this.mConfig = config;
        this.mServiceRegistry = new WebServiceRegistry(context);
    }

    @Override 
    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.evaluateJavascript(com.example.app.services.WebScripts.INTERCEPT_SCRIPT, null);
            view.evaluateJavascript(com.example.app.services.WebScripts.WEBSOCKET_PROXY_SCRIPT, null);
        } else {
            view.loadUrl("javascript:" + com.example.app.services.WebScripts.INTERCEPT_SCRIPT);
            view.loadUrl("javascript:" + com.example.app.services.WebScripts.WEBSOCKET_PROXY_SCRIPT);
        }
    }

    @Override 
    public boolean shouldOverrideKeyEvent(WebView view, android.view.KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true;
        }
        return super.shouldOverrideKeyEvent(view, event);
    }

    private String getRawVirtualHost() {
        if (mConfig == null || mConfig.getVirtualHost().isEmpty()) return null;
        return Uri.parse(mConfig.getVirtualHost()).getHost();
    }

    @Override 
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        String targetHost = uri.getHost();
        String rawVirtualHost = getRawVirtualHost();
        
        if (targetHost != null && targetHost.equals(rawVirtualHost)) {
            String path = uri.getPath();
            if (path != null) {
                String method = request.getMethod();
                WebResourceResponse serviceResponse = mServiceRegistry.dispatch(mContext, mConfig, request, path, method);
                if (serviceResponse != null) {
                    return serviceResponse;
                }
                
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
                    Log.e(TAG, "Exception loading asset file path: " + e.toString());
                    String errorHtml = "<html><body style='font-family:sans-serif;padding:20px;text-align:center;'>"
                            + "<h2>Application Error</h2>"
                            + "<p>The requested application resource could not be loaded local-side.</p>"
                            + "</body></html>";
                    InputStream fallbackStream = new ByteArrayInputStream(errorHtml.getBytes(StandardCharsets.UTF_8));
                    return new WebResourceResponse("text/html", "UTF-8", fallbackStream);
                }
            }
        }
        return super.shouldInterceptRequest(view, request);
    }

    /**
     * Refactored routing strategy evaluating config flags to provide strict path boundaries.
     */
    private InputStream resolveAssetStream(String relativePath) throws IOException {
        String formattedPath = "www/" + relativePath;

        // ◄ CONFIG-DRIVEN SWITCH: Check public memory only if public strategy is selected
        if (mConfig != null && mConfig.isPublicWorkspaceEnabled()) {
            String folderName = mConfig.getWorkspaceFolderName();
            File publicDocsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (publicDocsDir != null) {
                File workspaceFile = new File(new File(publicDocsDir, folderName), formattedPath);
                if (workspaceFile.exists() && workspaceFile.isFile()) {
                    Log.d(TAG, "[ROUTER-HIT] Serving asset from Public Shared Storage: " + formattedPath);
                    return new FileInputStream(workspaceFile);
                }
            }
        }

        // Strategy Default Option 1: Serve directly from secure App Private Sandbox Cache
        File sandboxFile = new File(mContext.getFilesDir(), formattedPath);
        if (sandboxFile.exists() && sandboxFile.isFile()) {
            Log.v(TAG, "[ROUTER-HIT] Serving asset from Private Sandbox isolation: " + formattedPath);
            return new FileInputStream(sandboxFile);
        }

        // Strategy Default Option 2: Fallback to the hardcoded baseline assets inside the APK package
        Log.v(TAG, "[ROUTER-HIT] Serving asset from base application APK assets: " + formattedPath);
        return mContext.getAssets().open(formattedPath);
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

    @Override 
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return handleUrlRouting(view, request.getUrl());
    }

    @SuppressWarnings("deprecation")
    @Override 
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return handleUrlRouting(view, Uri.parse(url));
    }

    private boolean handleUrlRouting(WebView view, Uri uri) {
        String host = uri.getHost();
        String rawVirtualHost = getRawVirtualHost();
        if (host != null && (host.equals(rawVirtualHost) || host.endsWith("." + rawVirtualHost))) {
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        view.getContext().startActivity(intent);
        return true;
    }

    @Override 
    public void onReceivedError(WebView webview, WebResourceRequest request, WebResourceError error) {
        if (webview == null || request == null || error == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int errorCode = error.getErrorCode();
            String targetUrl = request.getUrl().toString();
            if (request.isForMainFrame()) {
                Log.e(TAG, "CRITICAL ERROR [" + errorCode + "] TARGET: " + targetUrl);
                if (errorCode == WebViewClient.ERROR_FILE_NOT_FOUND || errorCode == WebViewClient.ERROR_HOST_LOOKUP || errorCode == WebViewClient.ERROR_CONNECT || errorCode == WebViewClient.ERROR_UNKNOWN) {
                    if (!targetUrl.contains("error.html") && mConfig != null) {
                        webview.loadUrl(mConfig.getVirtualHost() + "/error.html");
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override 
    public void onReceivedError(WebView webview, int errorCode, String description, String failingUrl) {
        if (webview != null && failingUrl != null && !failingUrl.contains("error.html")) {
            Log.e(TAG, "LEGACY CRITICAL ERROR: " + description);
            if (mConfig != null) {
                webview.loadUrl(mConfig.getVirtualHost() + "/error.html");
            }
        }
    }

    @Override 
    public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
        String failingUrl = (error != null) ? error.getUrl() : "Unknown URL";
        if (Build.VERSION.SDK_INT <= 30) {
            Log.w(TAG, "[ZEBRA SSL RECOVERY] Overriding certificate trust dropout chain validation for path: " + failingUrl);
            handler.proceed();
        } else {
            Log.d(TAG, "[SSL DEFAULT] Passing standard system security trust rules validation check.");
            super.onReceivedSslError(view, handler, error);
        }
    }
}

