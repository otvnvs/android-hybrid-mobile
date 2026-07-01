# Native Network Proxy REST API Documentation (`/api/net`)

This documentation outlines the REST API endpoint used to route network requests through the native Android layer. This bridge bypasses browser-level sandboxing, cookies, and Cross-Origin Resource Sharing (CORS) restrictions imposed on the WebView webframe.

---

## Endpoints

### 1. Proxy HTTP Request

Executes an outbound HTTP request using native Android network capabilities and forwards the results back to the web client.

* **URL:** `/api/net/proxy`
* **Method:** `POST`
* **Headers:** 
  * `Content-Type: application/json`
* **Query Parameters:** None.

#### Request Body
The request payload must be a JSON object detailing the destination server configurations:

| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `url` | string | **Yes** | The complete target destination web endpoint URL. |
| `method` | string | No | HTTP request method (e.g., `GET`, `POST`, `PUT`, `DELETE`). Defaults to `GET`. |
| `headers` | object | No | A collection of key-value pairs representing custom HTTP request headers to pass along. |
| `body` | string | No | Raw string content payload to transmit during write operations (`POST`, `PUT`, `PATCH`). |

**Example Request Payload:**
```json
{
  "url": "https://external-service.com",
  "method": "POST",
  "headers": {
    "Authorization": "Bearer token_abc123",
    "Content-Type": "application/json"
  },
  "body": "{\"userId\": 101, \"action\": \"sync\"}"
}
```

---

## Response Schema

The proxy endpoint wraps the intercepted third-party response into a unified status container to help client-side adapters reconstruct standard response elements.

* **Success Status:** `200 OK` (Indicates that the proxy endpoint ran to completion, regardless of the target server's final status code).
* **Content-Type:** `application/json`

#### Response Body

| Field | Type | Description |
| :--- | :--- | :--- |
| `status` | integer | The literal HTTP status code received directly from the destination target server (e.g., `200`, `401`, `500`). |
| `headers` | object | Dictionary object matching header lines received back from the remote server. |
| `body` | string | The unparsed text-based string content returned by the destination target server. |

**Example Successful Bridge Response:**
```json
{
  "status": 201,
  "headers": {
    "Content-Type": "application/json",
    "Server": "nginx",
    "Cache-Control": "no-cache"
  },
  "body": "{\"status\":\"created\",\"id\":9982}"
}
```

---

## Error Handling

If the internal proxy pipeline fails to communicate with the target host entirely (e.g., the device has no internet connection, or the URL format is invalid), the interceptor surfaces an internal error code:

* **Error Status:** `500 Internal Server Error`
* **Content-Type:** `application/json`

**Example Interception Failure Response:**
```json
{
  "status": "error",
  "message": "Native proxy routing failed: Unknown host exception occurred while accessing destination target server."
}
```


### 2. Native File Download (`wget` Utility)

Downloads a remote file from a public or authenticated URL directly into the Android application's storage environment via the native network layer.

* **URL:** `/api/net/download`
* **Method:** `POST`
* **Headers:** 
  * `Content-Type: application/json`

#### Request Body

| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `url` | string | **Yes** | The absolute URL of the file to download from the internet. |
| `path` | string | **Yes** | The local destination path inside app storage where the file will save. |
| `headers` | object | No | Custom headers required to fetch the file (e.g., Bearer tokens). |

**Example Request Payload:**
```json
{
  "url": "https://example.com",
  "path": "documents/reports/july_report.pdf",
  "headers": {
    "Authorization": "Bearer asset_download_token_990"
  }
}
```

#### Response Body (`200 OK`)
```json
{
  "status": "success",
  "message": "Resource downloaded successfully via native pipeline.",
  "local_path": "documents/reports/july_report.pdf",
  "file_size_bytes": 1048576
}
```

## Upcoming Roadmap & TODOs

The following features are planned for future releases to enhance the capabilities and security of the native network proxy and download utilities.

### Network Proxy Enhancements (`/api/net/proxy`)
- [ ] **Binary & Base64 Payload Stream Mapping**: Add automated payload encoding conversions to support transporting raw image data, PDFs, and compressed archives directly through the JSON wrapper.
- [ ] **Native Connection Timeout Thresholds**: Implement explicit, configurable connection and read timeouts on the `HttpURLConnection` initialization step to prevent slow remote servers from hanging the WebView thread context.
- [ ] **Strict Domain Whitelisting Controls**: Introduce an access control layer within the native bridge to restrict outbound proxy operations exclusively to approved target host configurations.

### Download Utility Enhancements (`/api/net/download`)
- [ ] **Real-Time Progress Tracking Interceptor**: Expose an endpoint that tracks the number of bytes written to disk, allowing the JavaScript framework to render accurate download progress bars.
- [ ] **Asynchronous Background Worker Pool**: Transition download streams away from the primary request handling thread into an execution pool managed by native Android services.
- [ ] **Auto-Resume & Range Header Recovery**: Add checking mechanisms that inspect partially written local files and append explicit `Range: bytes=...` headers to safely pick up broken download pipelines.

