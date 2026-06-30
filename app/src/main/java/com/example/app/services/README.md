# Custom Micro-REST Framework for Android WebView

This directory houses a lightweight, high-performance, annotation-driven REST API framework that bridges web frontends (Vue/React/Vanilla JS) with native Android system capabilities. It functions entirely without third-party frameworks or dependencies using core Java reflection, annotations, and regular expression path globbing.

## Directory Structure

```text
./services/
├── RequestMapping.java     # Target annotation layer mapping routes and HTTP verbs
├── RequestContext.java     # Decoupled inbound container parsing URLs, headers, and params
├── ResponseContext.java    # Fluid response builder controlling statuses, headers, and bytes
├── StorageService.java     # Low-level file system driver managing recursive I/O safely
├── AppController.java      # Functional endpoint registry handling business logic operations
└── WebServiceRegistry.java # Reflection-based framework router engine and parameter injector
```

## Key Architectural Framework Features

*   **Annotation-Driven Engine:** Write simple functional methods tagged with `@RequestMapping(path = "/api/...", method = "GET|POST|DELETE")` inside `AppController.java` to declare a service.
*   **Path Wildcard Globbing:** Supports dynamic route variables using bracket notation (e.g., `/api/products/{id}/details`). The engine automatically tokenizes these placeholders via a generated regular expression pattern engine.
*   **Advanced Parameter Parsing:** Automatically splits and exposes the connection Protocol (scheme), Host Domain, Path segments, and URL Query variables (`?key=value`) directly to your functions.
*   **Decoupled Context Wrappers:** Abstracts raw Android classes away from business operations. Endpoints interact purely with clean `RequestContext` inputs and output `ResponseContext` structures.
*   **Path Traversal Prevention:** Includes native canonical path resolution guards to block malicious directory traversal attempts (e.g., `../../etc/passwd`).

---

## Direct Integration Framework Sample

Below is an example snippet showing how to implement query parameter reading, path variable globbing, structural parsing via `org.json`, and fluid response building:

```java
@RequestMapping(path = "/api/fs/item/{category}", method = "GET")
public ResponseContext findFileSystemItem(RequestContext request) {
    // 1. Read Globbed Path Wildcards & Query String Fields
    String category = request.getPathParam("category"); 
    String filterName = request.getQueryParam("search"); 

    // 2. Read Meta Data Parameters
    String protocol = request.getProtocol(); // e.g., "https"
    String targetHost = request.getDomain(); // e.g., "example.com"

    try {
        // 3. Assemble Clean Outputs via org.json
        org.json.JSONObject payload = new org.json.JSONObject();
        payload.put("category", category);
        payload.put("queryFilter", filterName);
        payload.put("originatingProtocol", protocol);

        // 4. Return Fluent Builder Object Response
        return ResponseContext.status(200, "OK")
                .contentType("application/json")
                .header("Cache-Control", "no-cache")
                .body(payload.toString())
                .build();
    } catch (Exception e) {
        return ResponseContext.status(500).body("{\"error\":\"JSON compilation crash\"}").build();
    }
}
```

# Architectural Integration Guide: Micro-REST & MyWebViewClient

This document outlines the pipeline layout showing how the custom micro-REST framework hooks directly into the native Android WebView routing layers without breaking standard local file serving loops.

---

## The Component Interception Blueprint

```text
 [ Front-end JavaScript Fetch ]
               │
               ▼
┌────────────────────────────────────────┐
│  MyWebViewClient.shouldInterceptRequest │
└────────────────────────────────────────┘
               │
               ├─ Host Mismatch? ──► [ Global CORS HTTP Intercept Proxy / Web ]
               │
               ▼ (Virtual Host Matches)
┌────────────────────────────────────────┐
│    WebServiceRegistry.dispatch()       │
└────────────────────────────────────────┘
               │
               ├─ RegEx Match Success? ──► [ RequestContext Build ]
               │                                      │
               │                                      ▼
               │                           [ AppController Reflection ]
               │                                      │
               │                                      ▼
               │                           [ Translate ResponseContext ]
               │                                      │
               ▼ (No Match / returns null)            ▼
┌────────────────────────────────────────┐   ┌─────────────────────────┐
│ Fallback: resolveAssetStream (www/*)  │   │ Return native Android   │
└────────────────────────────────────────┘   │  WebResourceResponse    │
               │                             └─────────────────────────┘
               ▼ (Asset Missing)
┌────────────────────────────────────────┐
│  Render Fallback HTML Error Template   │
└────────────────────────────────────────┘
```

---

## Lifecycle Phase Breakdown

### 1. Inbound Interception Point
Every time the front-end Vue application dispatches a resource call (e.g., `fetch('/api/fs/list?path=Movies')`), the Android engine drops the thread task sequence directly into `MyWebViewClient.shouldInterceptRequest()`.

### 2. Domain Filtering Validation
The web view client extracts the incoming request hostname and compares it with your application configuration's structural properties:
*   **Virtual Domain Hit:** If `targetHost.equals(rawVirtualHost)` evaluates to `true`, the pipeline shifts execution traffic immediately into the micro-REST service layer handlers.
*   **External Traffic Miss:** If it is a generic background asset or remote URL target, the logic bypasses local components completely and flows straight down into your global cross-origin CORS proxy connections or default public internet handlers.

### 3. Service Registry Processing Execution
Once inside the local domain filtering route context block, `MyWebViewClient` hands control off to the routing registry table:
```java
WebResourceResponse serviceResponse = mServiceRegistry.dispatch(mContext, mConfig, request, path, method);
```
*   **RegEx Segment Matching:** The `WebServiceRegistry` evaluates the path against pre-compiled regular expression patterns compiled from your controller's `@RequestMapping` rules.
*   **Payload Wrapping:** If a match matches cleanly, a `RequestContext` instance parses the protocol, domains, parameters, and payloads. Globbed path tokens are extracted into key-value attributes.
*   **Reflection Dispatches:** The targeted `AppController` business method executes using runtime reflection mapping.
*   **Response Translation Layer:** The controller returns a decoupled `ResponseContext` holding status values and payload streams. The registry intercepts this and transforms it directly into a complex, native Android `WebResourceResponse` object containing your custom response codes and custom response headers.

### 4. Sequential Safe Asset Fallbacks
If the regular expression evaluation loop fails to find a valid controller destination string match, `dispatch()` returns `null`. 

`MyWebViewClient` captures the empty response and processes the string input using standard fallback asset tracking pipelines:
1. It reads the string as an asset file path layout lookup targeting your embedded app repository (`www/*`).
2. If no physical asset exists at that workspace address, an `IOException` error is caught.
3. The app serves the built-in, raw fallback `Application Error` HTML block back up to the frontend UI layer, preventing a black screen error layout.

