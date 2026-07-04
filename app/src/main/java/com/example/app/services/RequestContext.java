//package com.example.app.services;
//
//import android.content.Context;
//import android.net.Uri;
//import android.os.Build;
//import android.webkit.WebResourceRequest;
//import com.example.app.AppConfig;
//import java.net.URLDecoder;
//import java.nio.charset.StandardCharsets;
//import java.util.HashMap;
//import java.util.Map;
//
//public class RequestContext {
//    private final Context androidContext;
//    private final AppConfig appConfig;
//    private final String method;
//    private final String path;
//    private final String protocol;
//    private final String domain;
//    private final String httpVersion; // <-- ADD THIS FIELD
//    private final Map<String, String> queryParams = new HashMap<>();
//    private final Map<String, String> pathParams = new HashMap<>();
//    private final Map<String, String> headers;
//    private final byte[] body;
//
//    public RequestContext(Context context, AppConfig config, WebResourceRequest request, String path) {
//        this.androidContext = context;
//        this.appConfig = config;
//        this.path = path;
//        this.method = request.getMethod() != null ? request.getMethod().toUpperCase() : "GET";
//        this.httpVersion = "HTTP/1.1"; // <-- ADD THIS INITIALIZATION
//        
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Map<String, String> rawHeaders = request.getRequestHeaders();
//            this.headers = rawHeaders != null ? rawHeaders : new HashMap<>();
//        } else {
//            this.headers = new HashMap<>();
//        }
//
//        // 1. Extract Protocol (Scheme) and Host Domain using standard Android Uri utilities
//        Uri uri = request.getUrl();
//        this.protocol = uri.getScheme();
//        this.domain = uri.getHost();
//
//        // 2. Parse Query Parameters dynamically (?key=value)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && uri.getQuery() != null) {
//            for (String queryParamName : uri.getQueryParameterNames()) {
//                this.queryParams.put(queryParamName, uri.getQueryParameter(queryParamName));
//            }
//        }
//
//        // 3. Extract Request Payload (Retained payload header trick)
//        String extractedBody = "";
//        if (headers.containsKey("X-Export-Data")) {
//            try {
//                extractedBody = URLDecoder.decode(headers.get("X-Export-Data"), "UTF-8");
//            } catch (Exception e) {
//                extractedBody = "";
//            }
//        }
//        this.body = extractedBody.getBytes(StandardCharsets.UTF_8);
//    }
//
//    /**
//     * Internal framework setter to safely populate globbed path values during route processing
//     */
//    public void addPathParam(String key, String value) {
//        this.pathParams.put(key, value);
//    }
//
//    // Context Getters
//    public Context getAndroidContext() { return androidContext; }
//    public AppConfig getAppConfig() { return appConfig; }
//    public String getMethod() { return method; }
//    public String getPath() { return path; }
//    public String getProtocol() { return protocol; }
//    public String getDomain() { return domain; }
//    public String getHttpVersion() { return httpVersion; } // <-- ADD THIS METHOD
//    public String getQueryParam(String key) { return queryParams.get(key); }
//    public String getPathParam(String key) { return pathParams.get(key); }
//    public Map<String, String> getHeaders() { return headers; }
//    public String getHeader(String key) { return headers.get(key); } // <-- ADD THIS METHOD
//    public byte[] getBody() { return body; }
//}
//--------------------------------------------------------------------------------
//package com.example.app.services;
//
//import android.content.Context;
//import android.net.Uri;
//import android.os.Build;
//import android.webkit.WebResourceRequest;
//import com.example.app.AppConfig;
//import java.net.URLDecoder;
//import java.nio.charset.StandardCharsets;
//import java.util.HashMap;
//import java.util.Map;
//
//public class RequestContext {
//    private final Context androidContext;
//    private final AppConfig appConfig;
//    private final String method;
//    private final String path;
//    private final String protocol;
//    private final String domain;
//    private final String httpVersion;
//    private final Map<String, String> queryParams = new HashMap<>();
//    private final Map<String, String> pathParams = new HashMap<>();
//    private final Map<String, String> headers;
//    private final byte[] body;
//    
//    // NEW ACCESSIBLE ATTRIBUTE: Retains raw query parameters block for downstream proxies
//    private final String queryString; 
//
//    public RequestContext(Context context, AppConfig config, WebResourceRequest request, String path) {
//        this.androidContext = context;
//        this.appConfig = config;
//        
//        this.method = request.getMethod() != null ? request.getMethod().toUpperCase() : "GET";
//        this.httpVersion = "HTTP/1.1";
//        
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Map<String, String> rawHeaders = request.getRequestHeaders();
//            this.headers = rawHeaders != null ? rawHeaders : new HashMap<>();
//        } else {
//            this.headers = new HashMap<>();
//        }
//        
//        Uri uri = request.getUrl();
//        this.protocol = uri.getScheme();
//        this.domain = uri.getHost();
//        
//        // ADJUSTMENT: Capture the raw structural query parameters block natively
//        this.queryString = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) ? uri.getQuery() : null;
//
//        // NEW PATH RESOLVER: Retains complete suffix trails instead of localized routing keys
//        // Ensures that /api/net/proxy/https://server.com preserves full targets
//        String calculatedPath = path;
//        if (uri.getPath() != null && path != null && path.contains("proxy")) {
//            String rawUriString = uri.toString();
//            int proxyIndex = rawUriString.indexOf("/api/net/proxy/");
//            if (proxyIndex != -1) {
//                calculatedPath = rawUriString.substring(proxyIndex);
//                // Strip query component parameters from path bounds variable cleanly
//                if (calculatedPath.contains("?")) {
//                    calculatedPath = calculatedPath.substring(0, calculatedPath.indexOf("?"));
//                }
//            }
//        }
//        this.path = calculatedPath;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && uri.getQuery() != null) {
//            for (String queryParamName : uri.getQueryParameterNames()) {
//                this.queryParams.put(queryParamName, uri.getQueryParameter(queryParamName));
//            }
//        }
//        
//        String extractedBody = "";
//        if (headers.containsKey("X-Export-Data")) {
//            try {
//                extractedBody = URLDecoder.decode(headers.get("X-Export-Data"), "UTF-8");
//            } catch (Exception e) {
//                extractedBody = "";
//            }
//        }
//        this.body = extractedBody.getBytes(StandardCharsets.UTF_8);
//    }
//
//    public void addPathParam(String key, String value) {
//        this.pathParams.put(key, value);
//    }
//
//    public Context getAndroidContext() { return androidContext; }
//    public AppConfig getAppConfig() { return appConfig; }
//    public String getMethod() { return method; }
//    public String getPath() { return path; }
//    public String getProtocol() { return protocol; }
//    public String getDomain() { return domain; }
//    public String getHttpVersion() { return httpVersion; }
//    public String getQueryParam(String key) { return queryParams.get(key); }
//    public String getPathParam(String key) { return pathParams.get(key); }
//    public Map<String, String> getHeaders() { return headers; }
//    public String getHeader(String key) { return headers.get(key); }
//    public byte[] getBody() { return body; }
//    
//    // NEW ACCESSIBLE METHOD: Returns the raw query string segment parameters
//    public String getQueryString() { return queryString; }
//}
//--------------------------------------------------------------------------------
//package com.example.app.services;
//
//import android.content.Context;
//import android.net.Uri;
//import android.os.Build;
//import android.webkit.WebResourceRequest;
//import com.example.app.AppConfig;
//import java.util.HashMap;
//import java.util.Map;
//
//public class RequestContext {
//    private final Context androidContext;
//    private final AppConfig appConfig;
//    private final String method;
//    private final String path;
//    private final String protocol;
//    private final String domain;
//    private final String httpVersion;
//    private final Map<String, String> queryParams = new HashMap<>();
//    private final Map<String, String> pathParams = new HashMap<>();
//    private final Map<String, String> headers;
//    private final byte[] body;
//    private final String queryString;
//
//    public RequestContext(Context context, AppConfig config, WebResourceRequest request, String path) {
//        this.androidContext = context;
//        this.appConfig = config;
//        this.method = request.getMethod() != null ? request.getMethod().toUpperCase() : "GET";
//        this.httpVersion = "HTTP/1.1";
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Map<String, String> rawHeaders = request.getRequestHeaders();
//            this.headers = rawHeaders != null ? rawHeaders : new HashMap<>();
//        } else {
//            this.headers = new HashMap<>();
//        }
//
//        Uri uri = request.getUrl();
//        this.protocol = uri.getScheme();
//        this.domain = uri.getHost();
//        this.queryString = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) ? uri.getQuery() : null;
//
//        String calculatedPath = path;
//        if (uri.getPath() != null && path != null && path.contains("proxy")) {
//            String rawUriString = uri.toString();
//            int proxyIndex = rawUriString.indexOf("/api/net/proxy/");
//            if (proxyIndex != -1) {
//                calculatedPath = rawUriString.substring(proxyIndex);
//                if (calculatedPath.contains("?")) {
//                    calculatedPath = calculatedPath.substring(0, calculatedPath.indexOf("?"));
//                }
//            }
//        }
//        this.path = calculatedPath;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && uri.getQuery() != null) {
//            for (String queryParamName : uri.getQueryParameterNames()) {
//                this.queryParams.put(queryParamName, uri.getQueryParameter(queryParamName));
//            }
//        }
//
//        // Clean slate initialization: Fallback placeholder for standard incoming stream injection
//        this.body = new byte[0];
//    }
//
//    public void addPathParam(String key, String value) {
//        this.pathParams.put(key, value);
//    }
//
//    public Context getAndroidContext() { return androidContext; }
//    public AppConfig getAppConfig() { return appConfig; }
//    public String getMethod() { return method; }
//    public String getPath() { return path; }
//    public String getProtocol() { return protocol; }
//    public String getDomain() { return domain; }
//    public String getHttpVersion() { return httpVersion; }
//    public String getQueryParam(String key) { return queryParams.get(key); }
//    public String getPathParam(String key) { return pathParams.get(key); }
//    public Map<String, String> getHeaders() { return headers; }
//    public String getHeader(String key) { return headers.get(key); }
//    public byte[] getBody() { return body; }
//    public String getQueryString() { return queryString; }
//}

//--------------------------------------------------------------------------------
//package com.example.app.services;
//
//import android.content.Context;
//import android.net.Uri;
//import android.os.Build;
//import android.webkit.WebResourceRequest;
//import com.example.app.AppConfig;
//import java.net.URLDecoder;
//import java.nio.charset.StandardCharsets;
//import java.util.HashMap;
//import java.util.Map;
//
//public class RequestContext {
//    private final Context androidContext;
//    private final AppConfig appConfig;
//    private final String method;
//    private final String path;
//    private final String protocol;
//    private final String domain;
//    private final String httpVersion;
//    private final Map<String, String> queryParams = new HashMap<>();
//    private final Map<String, String> pathParams = new HashMap<>();
//    private final Map<String, String> headers;
//    private final byte[] body;
//    private final String queryString;
//
////    public RequestContext(Context context, AppConfig config, WebResourceRequest request, String path) {
////        this.androidContext = context;
////        this.appConfig = config;
////        this.method = request.getMethod() != null ? request.getMethod().toUpperCase() : "GET";
////        this.httpVersion = "HTTP/1.1";
////
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
////            Map<String, String> rawHeaders = request.getRequestHeaders();
////            this.headers = rawHeaders != null ? rawHeaders : new HashMap<>();
////        } else {
////            this.headers = new HashMap<>();
////        }
////
////        Uri uri = request.getUrl();
////        this.protocol = uri.getScheme();
////        this.domain = uri.getHost();
////        this.queryString = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) ? uri.getQuery() : null;
////
////        String calculatedPath = path;
////        if (uri.getPath() != null && path != null && path.contains("proxy")) {
////            String rawUriString = uri.toString();
////            int proxyIndex = rawUriString.indexOf("/api/net/proxy/");
////            if (proxyIndex != -1) {
////                calculatedPath = rawUriString.substring(proxyIndex);
////                if (calculatedPath.contains("?")) {
////                    calculatedPath = calculatedPath.substring(0, calculatedPath.indexOf("?"));
////                }
////            }
////        }
////        this.path = calculatedPath;
////
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && uri.getQuery() != null) {
////            for (String queryParamName : uri.getQueryParameterNames()) {
////                this.queryParams.put(queryParamName, uri.getQueryParameter(queryParamName));
////            }
////        }
////		String extractedBody = "";
////		if (headers.containsKey("X-Export-Data")) {
////		    extractedBody = headers.get("X-Export-Data");
////		} else if (headers.containsKey("x-export-data")) {
////		    extractedBody = headers.get("x-export-data");
////		}
////
////		try {
////		    if (extractedBody != null && !extractedBody.isEmpty()) {
////			extractedBody = URLDecoder.decode(extractedBody, "UTF-8");
////		    } else {
////			extractedBody = "";
////		    }
////		} catch (Exception e) {
////		    extractedBody = "";
////		}
////		this.body = extractedBody.getBytes(StandardCharsets.UTF_8);
////    }
//
//    public RequestContext(Context context, AppConfig config, WebResourceRequest request, String path) {
//        this.androidContext = context;
//        this.appConfig = config;
//        this.method = request.getMethod() != null ? request.getMethod().toUpperCase() : "GET";
//        this.httpVersion = "HTTP/1.1";
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Map<String, String> rawHeaders = request.getRequestHeaders();
//            this.headers = rawHeaders != null ? rawHeaders : new HashMap<>();
//        } else {
//            this.headers = new HashMap<>();
//        }
//
//        Uri uri = request.getUrl();
//        this.protocol = uri.getScheme();
//        this.domain = uri.getHost();
//        this.queryString = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) ? uri.getQuery() : null;
//
//        String calculatedPath = path;
//        if (uri.getPath() != null && path != null && path.contains("proxy")) {
//            String rawUriString = uri.toString();
//            int proxyIndex = rawUriString.indexOf("/api/net/proxy/");
//            if (proxyIndex != -1) {
//                calculatedPath = rawUriString.substring(proxyIndex);
//                if (calculatedPath.contains("?")) {
//                    calculatedPath = calculatedPath.substring(0, calculatedPath.indexOf("?"));
//                }
//            }
//        }
//        this.path = calculatedPath;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && uri.getQuery() != null) {
//            for (String queryParamName : uri.getQueryParameterNames()) {
//                this.queryParams.put(queryParamName, uri.getQueryParameter(queryParamName));
//            }
//        }
//
//        // =========================================================================
//        // STANDARD BODY STREAM CONVERSION
//        // Stripped the X-Export-Data header completely. 
//        // We now initialize a clean binary byte placeholder or read from standard stream injection.
//        // =========================================================================
//        this.body = new byte[0]; 
//    }
//
//    public void addPathParam(String key, String value) {
//        this.pathParams.put(key, value);
//    }
//
//    public Context getAndroidContext() {
//        return androidContext;
//    }
//
//    public AppConfig getAppConfig() {
//        return appConfig;
//    }
//
//    public String getMethod() {
//        return method;
//    }
//
//    public String getPath() {
//        return path;
//    }
//
//    public String getProtocol() {
//        return protocol;
//    }
//
//    public String getDomain() {
//        return domain;
//    }
//
//    public String getHttpVersion() {
//        return httpVersion;
//    }
//
//    public String getQueryParam(String key) {
//        return queryParams.get(key);
//    }
//
//    public String getPathParam(String key) {
//        return pathParams.get(key);
//    }
//
//    public Map<String, String> getHeaders() {
//        return headers;
//    }
//
//    public String getHeader(String key) {
//        return headers.get(key);
//    }
//
//    public byte[] getBody() {
//        return body;
//    }
//
//    public String getQueryString() {
//        return queryString;
//    }
//}
//--------------------------------------------------------------------------------
package com.example.app.services;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceRequest;
import com.example.app.AppConfig;
import java.util.HashMap;
import java.util.Map;

public class RequestContext {
    private final Context androidContext;
    private final AppConfig appConfig;
    private final String method;
    private final String path;
    private final String protocol;
    private final String domain;
    private final String httpVersion;
    private final Map<String, String> queryParams = new HashMap<>();
    private final Map<String, String> pathParams = new HashMap<>();
    private final Map<String, String> headers;
    private final byte[] body;
    private final String queryString;

    public RequestContext(Context context, AppConfig config, WebResourceRequest request, String path) {
        this.androidContext = context;
        this.appConfig = config;
        this.method = request.getMethod() != null ? request.getMethod().toUpperCase() : "GET";
        this.httpVersion = "HTTP/1.1";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Map<String, String> rawHeaders = request.getRequestHeaders();
            this.headers = rawHeaders != null ? rawHeaders : new HashMap<>();
        } else {
            this.headers = new HashMap<>();
        }

        Uri uri = request.getUrl();
        this.protocol = uri.getScheme();
        this.domain = uri.getHost();
        this.queryString = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) ? uri.getQuery() : null;

        String calculatedPath = path;
        if (uri.getPath() != null && path != null && path.contains("proxy")) {
            String rawUriString = uri.toString();
            int proxyIndex = rawUriString.indexOf("/api/net/proxy/");
            if (proxyIndex != -1) {
                calculatedPath = rawUriString.substring(proxyIndex);
                if (calculatedPath.contains("?")) {
                    calculatedPath = calculatedPath.substring(0, calculatedPath.indexOf("?"));
                }
            }
        }
        this.path = calculatedPath;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && uri.getQuery() != null) {
            for (String queryParamName : uri.getQueryParameterNames()) {
                this.queryParams.put(queryParamName, uri.getQueryParameter(queryParamName));
            }
        }

        // =========================================================================
        // REFACTORED STANDARD BODY ACCESS LAYER
        // Pulls the cached request payload dynamically right before the controller fires
        // =========================================================================
        //if ("POST".equals(this.method) || "PUT".equals(this.method) || "PATCH".equals(this.method)) {
        if ("POST".equals(this.method) || "PUT".equals(this.method) || "PATCH".equals(this.method) || "DELETE".equals(this.method)) {
            this.body = com.example.app.services.AndroidBridge.getAndClearBody(this.method, calculatedPath);
        } else {
            this.body = new byte[0];
        }
    }

    public void addPathParam(String key, String value) { this.pathParams.put(key, value); }
    public Context getAndroidContext() { return androidContext; }
    public AppConfig getAppConfig() { return appConfig; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getProtocol() { return protocol; }
    public String getDomain() { return domain; }
    public String getHttpVersion() { return httpVersion; }
    public String getQueryParam(String key) { return queryParams.get(key); }
    public String getPathParam(String key) { return pathParams.get(key); }
    public Map<String, String> getHeaders() { return headers; }
    public String getHeader(String key) { return headers.get(key); }
    public byte[] getBody() { return body; }
    public String getQueryString() { return queryString; }
}

