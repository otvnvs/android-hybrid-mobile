# TODO: Fix Request Body Cache Race Condition in WebView Interceptor

## Issue Description
During high-concurrency stress testing (`Promise.all`), rapid consecutive `POST` requests to `/api/arc/unzip` consistently fail on the 4th and 5th concurrent iterations with a `500 Internal Server Error`. 

Because modern Android WebViews (`shouldInterceptRequest`) cannot natively read `POST` request bodies, the application pre-stores the body payload in the native Java layer via a JavaScript-to-Java Android Bridge function before executing the actual network `fetch`. Under high-velocity concurrent execution, this architecture encounters a thread-safety or state-overwriting race condition where requests #4 and #5 retrieve corrupted, missing, or overwritten body strings, causing a JSON parsing crash inside the `ArcController`.

---

## Action Items

### 1. Audit Frontend Bridge Invocation
- [ ] Locate the JavaScript utility function responsible for passing the request body payload to the Android JavaScript Interface right before triggering `fetch()`.
- [ ] Verify that a unique, thread-safe transaction token (e.g., a `UUID` or high-resolution cryptographic timestamp) is generated for *every individual request* and passed alongside the body payload.

### 2. Harden Native Java Body Storage Cache
- [ ] Locate the native Java bridge method that receives and caches the incoming body strings from the WebView frontend.
- [ ] Ensure the storage container is an explicitly thread-safe map, such as `ConcurrentHashMap<String, String>`, rather than a standard `HashMap` or static field variable.
- [ ] Verify that keys are completely unique to the execution instance (bound to the unique transaction token) to prevent high-velocity sequential overrides.

### 3. Review Request Interception and Consumption Lifecycle
- [ ] Inspect the custom `shouldInterceptRequest` handler where the `RequestContext` instance is reconstructed.
- [ ] Ensure that after the interceptor extracts the cached request body string via its tracking key, the corresponding entry is safely deleted from the `ConcurrentHashMap` to prevent memory leaks, but *only after* the stream read has fully committed.
- [ ] Wrap the `new JSONObject(jsonConfig)` parsing block inside `ArcController` with explicit, descriptive try-catch log strings to surface exact `JSONException` error details rather than hiding behind a generic `500` catch block.

