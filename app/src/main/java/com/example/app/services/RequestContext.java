package com.example.app.services;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceRequest;
import com.example.app.AppConfig;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RequestContext {
    private final Context androidContext;
    private final AppConfig appConfig;
    private final String method;
    private final String path;
    private final String protocol;
    private final String domain;
    private final String httpVersion; // <-- ADD THIS FIELD
    private final Map<String, String> queryParams = new HashMap<>();
    private final Map<String, String> pathParams = new HashMap<>();
    private final Map<String, String> headers;
    private final byte[] body;

    public RequestContext(Context context, AppConfig config, WebResourceRequest request, String path) {
        this.androidContext = context;
        this.appConfig = config;
        this.path = path;
        this.method = request.getMethod() != null ? request.getMethod().toUpperCase() : "GET";
        this.httpVersion = "HTTP/1.1"; // <-- ADD THIS INITIALIZATION
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Map<String, String> rawHeaders = request.getRequestHeaders();
            this.headers = rawHeaders != null ? rawHeaders : new HashMap<>();
        } else {
            this.headers = new HashMap<>();
        }

        // 1. Extract Protocol (Scheme) and Host Domain using standard Android Uri utilities
        Uri uri = request.getUrl();
        this.protocol = uri.getScheme();
        this.domain = uri.getHost();

        // 2. Parse Query Parameters dynamically (?key=value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && uri.getQuery() != null) {
            for (String queryParamName : uri.getQueryParameterNames()) {
                this.queryParams.put(queryParamName, uri.getQueryParameter(queryParamName));
            }
        }

        // 3. Extract Request Payload (Retained payload header trick)
        String extractedBody = "";
        if (headers.containsKey("X-Export-Data")) {
            try {
                extractedBody = URLDecoder.decode(headers.get("X-Export-Data"), "UTF-8");
            } catch (Exception e) {
                extractedBody = "";
            }
        }
        this.body = extractedBody.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Internal framework setter to safely populate globbed path values during route processing
     */
    public void addPathParam(String key, String value) {
        this.pathParams.put(key, value);
    }

    // Context Getters
    public Context getAndroidContext() { return androidContext; }
    public AppConfig getAppConfig() { return appConfig; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getProtocol() { return protocol; }
    public String getDomain() { return domain; }
    public String getHttpVersion() { return httpVersion; } // <-- ADD THIS METHOD
    public String getQueryParam(String key) { return queryParams.get(key); }
    public String getPathParam(String key) { return pathParams.get(key); }
    public Map<String, String> getHeaders() { return headers; }
    public String getHeader(String key) { return headers.get(key); } // <-- ADD THIS METHOD
    public byte[] getBody() { return body; }
}

