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
import java.io.ByteArrayOutputStream;
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

import java.io.PrintWriter;
import java.io.StringWriter;


class MyWebViewClient extends WebViewClient {
    private static final String TAG = "JS_CONSOLE_MyWebViewClient";
    private final Context mContext;
    private final AppConfig mConfig;
    private final WebServiceRegistry mServiceRegistry;
private static String storedSessionCookies = "";

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

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
        view.evaluateJavascript(com.example.app.services.WebScripts.WEBSOCKET_PROXY_SCRIPT, null);
    } else {
        view.loadUrl("javascript:" + com.example.app.services.WebScripts.WEBSOCKET_PROXY_SCRIPT);
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
//new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {@Override public void run() {android.widget.Toast.makeText(mContext, "A:"+targetHost, android.widget.Toast.LENGTH_SHORT).show();}});

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
	    } else if (targetHost != null) {
//	        return super.shouldInterceptRequest(view, request);

//              // =========================================================================
//              // 2. CROSS-ORIGIN PROXY INTERCEPTION (CORS Bypass)
//              // =========================================================================
//              new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {@Override public void run() {android.widget.Toast.makeText(mContext, "B:"+targetHost, android.widget.Toast.LENGTH_SHORT).show();}});
//		String urlString = uri.toString();
//		String method = request.getMethod().toUpperCase();
//		Log.d(TAG, " -> Global Cross-Origin Proxy Catch: " + urlString + " [" + method + "]");
//
//		// Handle CORS HTTP OPTIONS Preflight Requests
//		if (method.equals("OPTIONS") && mConfig != null) {
//		    Map<String, String> preflightHeaders = new HashMap<>();
//		    preflightHeaders.put("Access-Control-Allow-Origin", mConfig.getVirtualHost());
//		    preflightHeaders.put("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, X-CSRF-Token, X-Requested-With");
//		    preflightHeaders.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE, PATCH");
//		    preflightHeaders.put("Access-Control-Allow-Credentials", "true");
//		    preflightHeaders.put("Access-Control-Max-Age", "3600");
//
//		    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//			return new WebResourceResponse("text/plain", "UTF-8", 200, "OK", preflightHeaders, new ByteArrayInputStream("".getBytes()));
//		    }
//		}
//
//		// Execute the native HTTP network request on behalf of the WebView
//		try {
//		    URL url = new URL(urlString);
//		    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//		    connection.setRequestMethod(method);
//
//		    for (Map.Entry<String, String> entry : request.getRequestHeaders().entrySet()) {
//			if (!entry.getKey().equalsIgnoreCase("Origin") && !entry.getKey().equalsIgnoreCase("Referer")) {
//			    connection.addRequestProperty(entry.getKey(), entry.getValue());
//			}
//		    }
//		    connection.connect();
//
//		    int statusCode = connection.getResponseCode();
//		    InputStream responseStream = (statusCode >= 200 && statusCode < 300) ? connection.getInputStream() : connection.getErrorStream();
//		    
//		    String contentType = connection.getContentType();
//		    String mimeType = "application/json";
//		    if (contentType != null) {
//			mimeType = contentType.contains(";") ? contentType.split(";")[0].trim() : contentType;
//		    }
//
//		    Map<String, String> responseHeaders = new HashMap<>();
//		    responseHeaders.put("Access-Control-Allow-Origin", mConfig.getVirtualHost());
//		    responseHeaders.put("Access-Control-Allow-Headers", "*");
//		    responseHeaders.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE, PATCH");
//		    responseHeaders.put("Access-Control-Allow-Credentials", "true");
//
//		    String statusMessage = connection.getResponseMessage();
//		    if (statusMessage == null || statusMessage.isEmpty()) {
//			statusMessage = "OK";
//		    }
//
//		    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//			return new WebResourceResponse(mimeType, "UTF-8", statusCode, statusMessage, responseHeaders, responseStream);
//		    } else {
//			return new WebResourceResponse(mimeType, "UTF-8", responseStream);
//		    }
//		} catch (Exception e) {
//		    Log.e(TAG, "Global CORS Interception Proxy Failure: " + e.getMessage());
//		}


















//    String urlString = uri.toString();
//    String method = request.getMethod().toUpperCase();
//    final String devicePrefix = "[" + android.os.Build.MODEL + "][" + method + "] ";
//    logToRemoteServer(devicePrefix + "Proxy active targeting: " + urlString);
//
//    // ========================================================
//    // 1. OPTIONS CORS PRE-FLIGHT HANDSHAKE
//    // ========================================================
//    if (method.equals("OPTIONS") && mConfig != null) {
//        Map<String, String> preflightHeaders = new java.util.HashMap<>();
//        preflightHeaders.put("Access-Control-Allow-Origin", mConfig.getVirtualHost().trim());
//        preflightHeaders.put("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, X-CSRF-Token, X-Requested-With");
//        preflightHeaders.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE, PATCH");
//        preflightHeaders.put("Access-Control-Allow-Credentials", "true");
//        preflightHeaders.put("Access-Control-Max-Age", "3600");
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            return new WebResourceResponse("text/plain", "UTF-8", 204, "No Content", preflightHeaders, new ByteArrayInputStream(new byte[0]));
//        }
//    }
//
//    // ========================================================
//    // 2. LIVE DATA TRANSMISSION MUTATION LAYER
//    // ========================================================
//    try {
//        URL url = new URL(urlString);
//        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        
//        // Apply Zebra SSL trust-store bypass automatically if running on legacy versions
//        if (connection instanceof javax.net.ssl.HttpsURLConnection && Build.VERSION.SDK_INT <= 30) {
//            javax.net.ssl.HttpsURLConnection sslConn = (javax.net.ssl.HttpsURLConnection) connection;
//            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
//                new javax.net.ssl.X509TrustManager() {
//                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
//                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
//                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
//                }
//            };
//            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
//            sc.init(null, trustAllCerts, new java.security.SecureRandom());
//            sslConn.setSSLSocketFactory(sc.getSocketFactory());
//            sslConn.setHostnameVerifier((hostname, session) -> true);
//        }
//
//        connection.setRequestMethod(method);
//
//        // Explicitly extract and forward request headers from JavaScript to the SAP Server
//        logToRemoteServer(devicePrefix + "--- COPYING OUTBOUND REQUEST HEADERS TO SAP ---");
//        for (Map.Entry<String, String> entry : request.getRequestHeaders().entrySet()) {
//            String key = entry.getKey();
//            String value = entry.getValue();
//            
//            // Log exactly what is being sent to your terminal window
//            logToRemoteServer(devicePrefix + " -> Request Header Map: [" + key + "] = " + value);
//            
//            if (!key.equalsIgnoreCase("Origin") && !key.equalsIgnoreCase("Referer")) {
//                connection.setRequestProperty(key, value);
//            }
//        }
//
//        // Handle body streaming validation for mutations
//        if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) {
//            connection.setDoOutput(true);
//            
//            // Note: Since WebView shields the request body stream natively, 
//            // if your SAP transaction fails here because the JSON list of items is empty,
//            // the payload must be passed across using your established JavaScript native bridge interface!
//            logToRemoteServer(devicePrefix + "Outbound payload verification initialized.");
//        }
//
//        connection.connect();
//        
//        int statusCode = connection.getResponseCode();
//        logToRemoteServer(devicePrefix + "Response code returned from SAP gateway: " + statusCode);
//        
//        InputStream rawStream = (statusCode >= 200 && statusCode < 300) ? connection.getInputStream() : connection.getErrorStream();
//
//        // Fully buffer incoming server stream response into memory heap space
//        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//        byte[] data = new byte[4096];
//        int nRead;
//        while ((nRead = rawStream.read(data, 0, data.length)) != -1) {
//            buffer.write(data, 0, nRead);
//        }
//        buffer.flush();
//        byte[] responseBytes = buffer.toByteArray();
//        InputStream bufferedStream = new ByteArrayInputStream(responseBytes);
//
//        String contentType = connection.getContentType();
//        String mimeType = "application/json";
//        if (contentType != null) {
//            mimeType = contentType.contains(";") ? contentType.split(";")[0].trim() : contentType;
//        }
//
//        // Extract and copy response headers from the SAP Server back to the WebView JavaScript engine
//        Map<String, String> responseHeaders = new HashMap<>();
//        logToRemoteServer(devicePrefix + "--- COPYING INBOUND RESPONSE HEADERS BACK TO WEBVIEW ---");
//        for (Map.Entry<String, java.util.List<String>> header : connection.getHeaderFields().entrySet()) {
//            if (header.getKey() != null && !header.getValue().isEmpty()) {
//                StringBuilder valBuilder = new StringBuilder();
//                for (int i = 0; i < header.getValue().size(); i++) {
//                    valBuilder.append(header.getValue().get(i));
//                    if (i < header.getValue().size() - 1) valBuilder.append(", ");
//                }
//                String lowerKey = header.getKey().toLowerCase();
//                String headerValue = valBuilder.toString();
//                
//                logToRemoteServer(devicePrefix + " -> Response Header Map: [" + lowerKey + "] = " + headerValue);
//                responseHeaders.put(lowerKey, headerValue);
//            }
//        }
//
//        // Re-inject required CORS headers to ensure the web shell clears the tracking context safely
//        responseHeaders.put("access-control-allow-origin", mConfig.getVirtualHost().trim());
//        responseHeaders.put("access-control-allow-headers", "*");
//        responseHeaders.put("access-control-allow-methods", "GET, POST, OPTIONS, PUT, DELETE, PATCH");
//        responseHeaders.put("access-control-allow-credentials", "true");
//        responseHeaders.put("access-control-expose-headers", "x-csrf-token, set-cookie, content-type");
//
//        String statusMessage = connection.getResponseMessage();
//        if (statusMessage == null || statusMessage.isEmpty()) {
//            statusMessage = "OK";
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            return new WebResourceResponse(mimeType, "UTF-8", statusCode, statusMessage, responseHeaders, bufferedStream);
//        } else {
//            return new WebResourceResponse(mimeType, "UTF-8", bufferedStream);
//        }
//
//    } catch (Exception e) {
//        StringWriter sw = new StringWriter();
//        PrintWriter pw = new PrintWriter(sw);
//        e.printStackTrace(pw);
//        logToRemoteServer(devicePrefix + "PROXY ERROR EXCEPTION:\n" + sw.toString());
//        Log.e(TAG, "Global CORS Interception Proxy Failure: " + e.getMessage());
//    }
//






























//        String urlString = uri.toString();
//        String method = request.getMethod().toUpperCase();
//        final String devicePrefix = "[" + android.os.Build.MODEL + "][" + method + "] ";
//        logToRemoteServer(devicePrefix + "Proxy intercept triggered targeting: " + urlString);
//
//        // ========================================================
//        // 1. OPTIONS CORS PRE-FLIGHT HANDSHAKE
//        // ========================================================
//        if (method.equals("OPTIONS") && mConfig != null) {
//            logToRemoteServer(devicePrefix + "Catch OPTIONS Pre-flight hook.");
//            Map<String, String> preflightHeaders = new java.util.HashMap<>();
//            preflightHeaders.put("Access-Control-Allow-Origin", mConfig.getVirtualHost().trim());//sq
//            preflightHeaders.put("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, X-CSRF-Token, X-Requested-With");
//            preflightHeaders.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE, PATCH");
//            preflightHeaders.put("Access-Control-Allow-Credentials", "true");
//            preflightHeaders.put("Access-Control-Max-Age", "3600");
//            
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                logToRemoteServer(devicePrefix + "Returning 204 Mock CORS Pre-flight directly to engine.");
//                return new WebResourceResponse("text/plain", "UTF-8", 204, "No Content", preflightHeaders, new ByteArrayInputStream(new byte[0]));
//            }
//        }
//
//        // ========================================================
//        // 2. THREAD-SAFE SYNCHRONOUS COOKIE SWEEP ENGINE
//        // ========================================================
//        final String[] synchronousCookies = new String[1];
//        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
//        final String finalUrlString = urlString;
//        final WebView finalView = view;
//
//        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    android.webkit.CookieManager mainThreadCookieManager = android.webkit.CookieManager.getInstance();
//                    String fetched = null;
//
//                    // Tier 1: Target URI Check
//                    android.net.Uri cleanUri = android.net.Uri.parse(finalUrlString);
//                    String targetScheme = cleanUri.getScheme();
//                    String targetHostDomain = cleanUri.getHost();
//                    if (targetScheme != null && targetHostDomain != null) {
//                        fetched = mainThreadCookieManager.getCookie(targetScheme + "://" + targetHostDomain);
//                    }
//
//                    // Tier 2: Live WebView Address Check
//                    if ((fetched == null || fetched.isEmpty()) && finalView != null) {
//                        String currentWebUrl = finalView.getUrl();
//                        if (currentWebUrl != null && !currentWebUrl.isEmpty()) {
//                            android.net.Uri webUri = android.net.Uri.parse(currentWebUrl);
//                            if (webUri.getScheme() != null && webUri.getHost() != null) {
//                                fetched = mainThreadCookieManager.getCookie(webUri.getScheme() + "://" + webUri.getHost());
//                            }
//                        }
//                    }
//
//                    // Tier 3: Core Virtual Host Check
//                    if ((fetched == null || fetched.isEmpty()) && mConfig != null && !mConfig.getVirtualHost().isEmpty()) {
//                        android.net.Uri fallbackUri = android.net.Uri.parse(mConfig.getVirtualHost().trim());
//                        if (fallbackUri.getScheme() != null && fallbackUri.getHost() != null) {
//                            fetched = mainThreadCookieManager.getCookie(fallbackUri.getScheme() + "://" + fallbackUri.getHost());
//                        }
//                    }
//
//                    synchronousCookies[0] = fetched;
//                } catch (Exception err) {
//                    Log.e(TAG, "Main thread cookie extraction failed: " + err.getMessage());
//                } finally {
//                    latch.countDown();
//                }
//            }
//        });
//
//        try {
//            // Halt the proxy background worker thread for a maximum of 500ms for safety
//            latch.await(500, java.util.concurrent.TimeUnit.MILLISECONDS);
//        } catch (InterruptedException latchException) {
//            Log.e(TAG, "Latch timeout context dropped: " + latchException.getMessage());
//        }
//        // ========================================================
//        // 3. LIVE DATA TRANSMISSION & MUTATION LAYER
//        // ========================================================
//        try {
//            logToRemoteServer(devicePrefix + "Opening HttpURLConnection instance...");
//            URL url = new URL(urlString);
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            
//            // RECOVERY PATCH: Bypass expired/untrusted SSL handshakes on the legacy Zebra system
//            if (connection instanceof javax.net.ssl.HttpsURLConnection && Build.VERSION.SDK_INT <= 30) {
//                logToRemoteServer(devicePrefix + "Condition true (API <= 30). Injecting untrusted Certificate Authority bypass wrapper...");
//                javax.net.ssl.HttpsURLConnection sslConn = (javax.net.ssl.HttpsURLConnection) connection;
//                javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
//                    new javax.net.ssl.X509TrustManager() {
//                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
//                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
//                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
//                    }
//                };
//                javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
//                sc.init(null, trustAllCerts, new java.security.SecureRandom());
//                sslConn.setSSLSocketFactory(sc.getSocketFactory());
//                sslConn.setHostnameVerifier((hostname, session) -> true);
//                logToRemoteServer(devicePrefix + "SSL Bypass completed successfully.");
//            }
//
//            connection.setRequestMethod(method);
//
//            // Forward incoming framework request properties out to the destination server 
//            logToRemoteServer(devicePrefix + "--- COPYING OUTBOUND REQUEST HEADERS TO SAP ---");
//            for (Map.Entry<String, String> entry : request.getRequestHeaders().entrySet()) {
//                String key = entry.getKey();
//                String value = entry.getValue();
//                logToRemoteServer(devicePrefix + " -> Request Header Map: [" + key + "] = " + value);
//                
//                if (!key.equalsIgnoreCase("Origin") && !key.equalsIgnoreCase("Referer")) {
//                    connection.addRequestProperty(key, value);//sq
//                }
//            }
//
//            // Safely verify if main-thread extraction retrieved cookies successfully
//            String activeCookies = synchronousCookies[0];
//            if (activeCookies != null && !activeCookies.isEmpty()) {
//                //connection.setRequestProperty("Cookie", activeCookies);
//                logToRemoteServer(devicePrefix + " -> SUCCESS: Thread-safe Injected Live Cookies: " + activeCookies);
//            } else {
//                logToRemoteServer(devicePrefix + " -> Warning: Absolute thread-safe cookie sync breakdown. No active parameters found.");
//            }
//
//            if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) {
//                connection.setDoOutput(true);
//                logToRemoteServer(devicePrefix + "Outbound payload verification initialized.");
//            }
//
//            logToRemoteServer(devicePrefix + "Connecting to host endpoint...");
//            connection.connect();
//            
//            int statusCode = connection.getResponseCode();
//            logToRemoteServer(devicePrefix + "Response code returned from SAP gateway: " + statusCode);
//            
//            String statusMessage = connection.getResponseMessage();
//            if (statusMessage == null || statusMessage.isEmpty()) {
//                statusMessage = "OK";
//            }
//            
//            InputStream rawStream = (statusCode >= 200 && statusCode < 300) ? connection.getInputStream() : connection.getErrorStream();
//
//            // Fully buffer incoming server stream response into memory heap space
//            logToRemoteServer(devicePrefix + "Buffering payload stream context into application heap space...");
//            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//            byte[] data = new byte[4096];
//            int nRead;
//            while ((nRead = rawStream.read(data, 0, data.length)) != -1) {
//                buffer.write(data, 0, nRead);
//            }
//            buffer.flush();
//            byte[] responseBytes = buffer.toByteArray();
//            InputStream bufferedStream = new ByteArrayInputStream(responseBytes);
//            logToRemoteServer(devicePrefix + "Stream buffer complete. Total payload size caught: " + responseBytes.length + " bytes.");
//
//            String contentType = connection.getContentType();
//            String mimeType = "application/json";
//            if (contentType != null) {
//                mimeType = contentType.contains(";") ? contentType.split(";")[0].trim() : contentType;
//            }
//
//            // Extract and copy response headers from the SAP Server back to the WebView JavaScript engine
//            Map<String, String> responseHeaders = new HashMap<>();
//            logToRemoteServer(devicePrefix + "--- COPYING INBOUND RESPONSE HEADERS BACK TO WEBVIEW ---");
//            for (Map.Entry<String, java.util.List<String>> header : connection.getHeaderFields().entrySet()) {
//                if (header.getKey() != null && !header.getValue().isEmpty()) {
//                    StringBuilder valBuilder = new StringBuilder();
//                    for (int i = 0; i < header.getValue().size(); i++) {
//                        valBuilder.append(header.getValue().get(i));
//                        if (i < header.getValue().size() - 1) valBuilder.append(", ");
//                    }
//                    String lowerKey = header.getKey().toLowerCase();
//                    String headerValue = valBuilder.toString();
//                    
//                    logToRemoteServer(devicePrefix + " -> Response Header Map: [" + lowerKey + "] = " + headerValue);
//                    responseHeaders.put(lowerKey, headerValue);
//                }
//            }
//
//            // Re-inject required CORS parameters to ensure the web shell clears the tracking context safely
//            responseHeaders.put("Access-Control-Allow-Origin", mConfig.getVirtualHost().trim());//sq
//            responseHeaders.put("Access-Control-Allow-Headers", "*");
//            responseHeaders.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE, PATCH");
//            responseHeaders.put("Access-Control-Allow-Credentials", "true");
//            responseHeaders.put("Access-Control-Expose-Headers", "x-csrf-token, set-cookie, content-type");
//
//            logToRemoteServer(devicePrefix + "Returning configured WebResourceResponse bundle to client frame. [MimeType: " + mimeType + "]");
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                return new WebResourceResponse(mimeType, "UTF-8", statusCode, statusMessage, responseHeaders, bufferedStream);
//            } else {
//                return new WebResourceResponse(mimeType, "UTF-8", bufferedStream);
//            }
//
//        } catch (Exception e) {
//            StringWriter sw = new StringWriter();
//            PrintWriter pw = new PrintWriter(sw);
//            e.printStackTrace(pw);
//            logToRemoteServer(devicePrefix + "CRITICAL PROXY EXCEPTION OCCURRED:\n" + sw.toString());
//            Log.e(TAG, "Global CORS Interception Proxy Failure: " + e.getMessage());
//        }
//
//
//
//
//
//
//
//
//
//
//
//
//    } else if (targetHost != null && targetHost.contains("professorsoft.com")) {
//        String urlString = uri.toString();
//        String method = request.getMethod().toUpperCase();
//        final String logPrefix = "[JAVA_PROXY][" + method + "] ";
//        
//        android.util.Log.d("JS_CONSOLE", logPrefix + "Intercepted matching SAP Route -> Target: " + urlString);
//
//        // Make sure the static cookie storage token container is safely initialized
//        if (com.example.app.MyWebViewClient.storedSessionCookies == null) {
//            com.example.app.MyWebViewClient.storedSessionCookies = "";
//        }
//
//        // ========================================================
//        // 1. OPTIONS CORS PRE-FLIGHT HANDSHAKE
//        // ========================================================
//        if (method.equals("OPTIONS") && mConfig != null) {
//            android.util.Log.d("JS_CONSOLE", logPrefix + "Processing synthetic OPTIONS CORS pre-flight layout validation...");
//            Map<String, String> preflightHeaders = new java.util.HashMap<>();
//            preflightHeaders.put("Access-Control-Allow-Origin", mConfig.getVirtualHost().trim());
//            preflightHeaders.put("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, X-CSRF-Token, X-Requested-With");
//            preflightHeaders.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE, PATCH");
//            preflightHeaders.put("Access-Control-Allow-Credentials", "true");
//            preflightHeaders.put("Access-Control-Max-Age", "3600");
//            
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                android.util.Log.d("JS_CONSOLE", logPrefix + "Returning 204 No Content directly to Chromium engine to drop pre-flight block.");
//                return new WebResourceResponse("text/plain", "UTF-8", 204, "No Content", preflightHeaders, new ByteArrayInputStream(new byte[0]));
//            }
//        }
//
//        // ========================================================
//        // 2. THREAD-SAFE SYNCHRONOUS COOKIE SWEEP ENGINE
//        // ========================================================
//        final String[] synchronousCookies = new String[1];
//        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
//        final String finalUrlString = urlString;
//        final WebView finalView = view;
//
//        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    android.webkit.CookieManager mainThreadCookieManager = android.webkit.CookieManager.getInstance();
//                    String fetched = null;
//
//                    android.net.Uri cleanUri = android.net.Uri.parse(finalUrlString);
//                    String targetScheme = cleanUri.getScheme();
//                    String targetHostDomain = cleanUri.getHost();
//                    if (targetScheme != null && targetHostDomain != null) {
//                        fetched = mainThreadCookieManager.getCookie(targetScheme + "://" + targetHostDomain);
//                    }
//
//                    if ((fetched == null || fetched.isEmpty()) && finalView != null) {
//                        String currentWebUrl = finalView.getUrl();
//                        if (currentWebUrl != null && !currentWebUrl.isEmpty()) {
//                            android.net.Uri webUri = android.net.Uri.parse(currentWebUrl);
//                            if (webUri.getScheme() != null && webUri.getHost() != null) {
//                                fetched = mainThreadCookieManager.getCookie(webUri.getScheme() + "://" + webUri.getHost());
//                            }
//                        }
//                    }
//
//                    if ((fetched == null || fetched.isEmpty()) && mConfig != null && !mConfig.getVirtualHost().isEmpty()) {
//                        android.net.Uri fallbackUri = android.net.Uri.parse(mConfig.getVirtualHost().trim());
//                        if (fallbackUri.getScheme() != null && fallbackUri.getHost() != null) {
//                            fetched = mainThreadCookieManager.getCookie(fallbackUri.getScheme() + "://" + fallbackUri.getHost());
//                        }
//                    }
//
//                    synchronousCookies[0] = fetched;
//                } catch (Exception err) {
//                    Log.e(TAG, "Main thread cookie extraction failed: " + err.getMessage());
//                } finally {
//                    latch.countDown();
//                }
//            }
//        });
//
//        try {
//            latch.await(500, java.util.concurrent.TimeUnit.MILLISECONDS);
//        } catch (InterruptedException latchException) {
//            Log.e(TAG, "Latch timeout context dropped: " + latchException.getMessage());
//        }





		//new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {@Override public void run() {android.widget.Toast.makeText(mContext, "C:"+targetHost, android.widget.Toast.LENGTH_SHORT).show();}});
	        return super.shouldInterceptRequest(view, request);




	    } else {
		//new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {@Override public void run() {android.widget.Toast.makeText(mContext, "C:"+targetHost, android.widget.Toast.LENGTH_SHORT).show();}});
	        return super.shouldInterceptRequest(view, request);
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

//	@Override
//	public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
//	    Log.w(TAG, "WebView SSL Certificate validation bypass triggered for: " + error.getUrl());
//	    
//	    // Force the WebView to proceed regardless of the device architecture or patch era
//	    handler.proceed(); 
//	}

private void logToRemoteServer(final String message) {
/*
    new Thread(new Runnable() {
        @Override
        public void run() {
            java.net.HttpURLConnection conn = null;
            try {
                java.net.URL url = new java.net.URL("http://192.168.0.31:8080");
                conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);

                java.io.OutputStream os = conn.getOutputStream();
                os.write(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                os.flush();
                os.close();

                // Read response code to finalize the connection pool
                int responseCode = conn.getResponseCode(); 
            } catch (Exception e) {
                android.util.Log.e(TAG, "Failed sending remote log item: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
    }).start();
*/
}

}
