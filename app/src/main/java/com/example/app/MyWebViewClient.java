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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import android.view.KeyEvent;//Sat Jun 27 13:11:10 SAST 2026 dominique phone secret keys
import com.example.app.services.WebServiceRegistry;

class MyWebViewClient extends WebViewClient {
    private static final String TAG = "JAVA_MyWebViewClient";
    private final Context mContext;
    private final AppConfig mConfig;
    private final WebServiceRegistry mServiceRegistry;

    public MyWebViewClient(Context context, AppConfig config) {
        this.mContext = context;
        this.mConfig = config;
        this.mServiceRegistry = new WebServiceRegistry(context); // Initialize registry
    }

@Override
public void onPageStarted(android.webkit.WebView view, String url, android.graphics.Bitmap favicon) {
    super.onPageStarted(view, url, favicon);
    
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
        view.evaluateJavascript(com.example.app.services.WebScripts.INTERCEPT_SCRIPT, null);
    } else {
        view.loadUrl("javascript:" + com.example.app.services.WebScripts.INTERCEPT_SCRIPT);
    }
}
	//Sat Jun 27 13:09:21 SAST 2026 dominique phone secret keys
    /**
     * Prevents the internal browser core from consuming volume rocker keys.
     * Returning true instructs the WebView to route these events back up to the Activity layout node.
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
	@Override 
	public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
	    Uri uri = request.getUrl();
	    String targetHost = uri.getHost();
	    String rawVirtualHost = getRawVirtualHost();



	    // =========================================================================
	    // 1. VIRTUAL HOST INTERCEPTION (Local Handlers & Assets)
	    // =========================================================================
	    if (targetHost != null && targetHost.equals(rawVirtualHost)) {
		String path = uri.getPath();
		if (path != null) {
		    String method = request.getMethod();

		    // Delegate endpoints to the Service Registry (Export, Import, and future handlers)
		    WebResourceResponse serviceResponse = mServiceRegistry.dispatch(mContext, mConfig, request, path, method);
		    if (serviceResponse != null) {
			return serviceResponse;
		    }

		    // Fallback: Resolve and serve static asset files local-side
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
	    // =========================================================================
	    // 2. CROSS-ORIGIN PROXY INTERCEPTION (CORS Bypass)
	    // =========================================================================
	    else if (targetHost != null) {
		String urlString = uri.toString();
		String method = request.getMethod().toUpperCase();
		Log.d(TAG, " -> Global Cross-Origin Proxy Catch: " + urlString + " [" + method + "]");

		// Handle CORS HTTP OPTIONS Preflight Requests
		if (method.equals("OPTIONS") && mConfig != null) {
		    Map<String, String> preflightHeaders = new HashMap<>();
		    preflightHeaders.put("Access-Control-Allow-Origin", mConfig.getVirtualHost());
		    preflightHeaders.put("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, X-CSRF-Token, X-Requested-With");
		    preflightHeaders.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE, PATCH");
		    preflightHeaders.put("Access-Control-Allow-Credentials", "true");
		    preflightHeaders.put("Access-Control-Max-Age", "3600");

		    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return new WebResourceResponse("text/plain", "UTF-8", 200, "OK", preflightHeaders, new ByteArrayInputStream("".getBytes()));
		    }
		}

		// Execute the native HTTP network request on behalf of the WebView
		try {
		    URL url = new URL(urlString);
		    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		    connection.setRequestMethod(method);

		    for (Map.Entry<String, String> entry : request.getRequestHeaders().entrySet()) {
			if (!entry.getKey().equalsIgnoreCase("Origin") && !entry.getKey().equalsIgnoreCase("Referer")) {
			    connection.addRequestProperty(entry.getKey(), entry.getValue());
			}
		    }
		    connection.connect();

		    int statusCode = connection.getResponseCode();
		    InputStream responseStream = (statusCode >= 200 && statusCode < 300) ? connection.getInputStream() : connection.getErrorStream();
		    
		    String contentType = connection.getContentType();
		    String mimeType = "application/json";
		    if (contentType != null) {
			mimeType = contentType.contains(";") ? contentType.split(";")[0].trim() : contentType;
		    }

		    Map<String, String> responseHeaders = new HashMap<>();
		    responseHeaders.put("Access-Control-Allow-Origin", mConfig.getVirtualHost());
		    responseHeaders.put("Access-Control-Allow-Headers", "*");
		    responseHeaders.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE, PATCH");
		    responseHeaders.put("Access-Control-Allow-Credentials", "true");

		    String statusMessage = connection.getResponseMessage();
		    if (statusMessage == null || statusMessage.isEmpty()) {
			statusMessage = "OK";
		    }

		    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return new WebResourceResponse(mimeType, "UTF-8", statusCode, statusMessage, responseHeaders, responseStream);
		    } else {
			return new WebResourceResponse(mimeType, "UTF-8", responseStream);
		    }
		} catch (Exception e) {
		    Log.e(TAG, "Global CORS Interception Proxy Failure: " + e.getMessage());
		}
	    }

	    return super.shouldInterceptRequest(view, request);
	}

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
                if (errorCode == WebViewClient.ERROR_FILE_NOT_FOUND || errorCode == WebViewClient.ERROR_HOST_LOOKUP 
                        || errorCode == WebViewClient.ERROR_CONNECT || errorCode == WebViewClient.ERROR_UNKNOWN) {
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
}
