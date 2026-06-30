package com.example.app.services;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import com.example.app.AppConfig;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebServiceRegistry {
    private static final String TAG = "WebServiceRegistry_DEBUG";
    private final List<RouteMappingMetadata> routeMetadataList = new ArrayList<>();
    private final Object controllerInstance;

    public WebServiceRegistry() {
        Log.d(TAG, "Initializing WebServiceRegistry compilation sequence...");
        this.controllerInstance = new AppController();
        compileControllerRoutes();
    }

    private static class RouteMappingMetadata {
        final String httpMethod;
        final String rawAnnotatedPath;
        final Pattern regexPattern;
        final List<String> pathTokenKeys;
        final Method executionTarget;

        RouteMappingMetadata(String httpMethod, String rawAnnotatedPath, Pattern regexPattern, List<String> pathTokenKeys, Method executionTarget) {
            this.httpMethod = httpMethod;
            this.rawAnnotatedPath = rawAnnotatedPath;
            this.regexPattern = regexPattern;
            this.pathTokenKeys = pathTokenKeys;
            this.executionTarget = executionTarget;
        }
    }

    private void compileControllerRoutes() {
        Method[] methods = controllerInstance.getClass().getMethods();
        Log.d(TAG, "Reflecting class methods inside AppController. Total methods found: " + methods.length);
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping mapping = method.getAnnotation(RequestMapping.class);
                String cleanPath = mapping.path();
                List<String> pathTokenNames = new ArrayList<>();
                
                // Parse curly tokens like {id}
                Matcher tokenMatcher = Pattern.compile("\\{([^}]+)\\}").matcher(cleanPath);
                while (tokenMatcher.find()) {
                    pathTokenNames.add(tokenMatcher.group(1));
                }
                
                String generalizedRegexPattern = cleanPath.replaceAll("\\{[^}]+\\}", "([^/]+)");
                Pattern compiledRegex = Pattern.compile("^" + generalizedRegexPattern + "$");

                routeMetadataList.add(new RouteMappingMetadata(
                        mapping.method().toUpperCase(), 
                        cleanPath,
                        compiledRegex, 
                        pathTokenNames, 
                        method
                ));
                Log.d(TAG, "==> COMPILED ROUTE: [" + mapping.method().toUpperCase() + "] " + cleanPath + " -> REGEX: " + compiledRegex.pattern());
            }
        }
        Log.d(TAG, "Route compilation complete. Registered endpoint configurations: " + routeMetadataList.size());
    }

    public WebResourceResponse dispatch(Context context, AppConfig config, WebResourceRequest request, String path, String method) {
        String cleanMethod = method.toUpperCase();
        
        Log.d(TAG, "------------------ INBOUND DISPATCH EVENT ------------------");
        Log.d(TAG, "Received raw path parameter vector -> Path: \"" + path + "\" | Method: \"" + cleanMethod + "\"");
        
        // Strip out trailing slash safety layers
        String lookupPath = path;
        if (lookupPath.length() > 1 && lookupPath.endsWith("/")) {
            lookupPath = lookupPath.substring(0, lookupPath.length() - 1);
            Log.d(TAG, "Sanitized path variable (removed trailing slash) -> \"" + lookupPath + "\"");
        }

        Log.d(TAG, "Evaluating matching matrices against " + routeMetadataList.size() + " registered mappings...");
        for (RouteMappingMetadata route : routeMetadataList) {
            Log.d(TAG, "Checking: [" + route.httpMethod + "] vs [" + cleanMethod + "] | Pattern: " + route.regexPattern.pattern());
            
            if (route.httpMethod.equals(cleanMethod)) {
                Matcher matcher = route.regexPattern.matcher(lookupPath);
                boolean matches = matcher.matches();
                Log.d(TAG, "Pattern Match Status for route \"" + route.rawAnnotatedPath + "\": " + matches);
                
                if (matches) {
                    Log.d(TAG, "[MATCH SUCCESS] Target method identified: " + route.executionTarget.getName() + "()");
                    try {
                        Log.d(TAG, "Assembling RequestContext parameters...");
                        RequestContext ctx = new RequestContext(context, config, request, path);

                        for (int i = 0; i < matcher.groupCount(); i++) {
                            String tokenKey = route.pathTokenKeys.get(i);
                            String tokenValue = matcher.group(i + 1);
                            ctx.addPathParam(tokenKey, tokenValue);
                            Log.d(TAG, "Extracted path segment mapping -> Key: {" + tokenKey + "} = \"" + tokenValue + "\"");
                        }

                        Log.d(TAG, "Invoking reflected target method pipeline...");
                        ResponseContext response = (ResponseContext) route.executionTarget.invoke(controllerInstance, ctx);
                        
                        if (response != null) {
                            Log.d(TAG, "Method executed cleanly. Constructing WebResourceResponse -> Status: " + response.getStatusCode() + ", Mime: " + response.getMimeType());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                return new WebResourceResponse(
                                        response.getMimeType(), "UTF-8",
                                        response.getStatusCode(), response.getStatusMessage(),
                                        response.getHeaders(), new ByteArrayInputStream(response.getBody())
                                );
                            } else {
                                return new WebResourceResponse(
                                        response.getMimeType(), "UTF-8",
                                        new ByteArrayInputStream(response.getBody())
                                );
                            }
                        } else {
                            Log.w(TAG, "[WARNING] Controller method returned an empty response layout block.");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "[CRASH] Exception caught during method invocation processing structural paths", e);
                    }
                }
            }
        }
        Log.w(TAG, "[ROUTING FAILURE] No matching endpoint structure could resolve path: " + path + " [" + method + "]. Returning null fallback.");
        Log.d(TAG, "------------------------------------------------------------");
        return null; 
    }
}

