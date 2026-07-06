## 4. Cross-Origin Network Proxy Broker (`NetController`)

### `POST /api/net/proxy`
*   **Description:** Bypasses browser CORS constraints by forwarding a custom HTTP/HTTPS connection stream to a target server via the native Android Java layer network architecture. Returns remote response structures and headers.
*   **Query Parameters:**
    *   `timeout_ms` *(Optional Integer)*: Controls connection and data read timeout constraints for the proxy broker itself in milliseconds. Defaults to `15000` (15 seconds) if omitted.
*   **Request Body:** `application/json` object detailing downstream connection requirements:
    ```json
    {
      "url": "https://external.com",
      "method": "POST",
      "headers": { "Accept": "application/json" },
      "body": "{\"param\": 123}"
    }
    ```
*   **Response Status:** `200 OK` (Standard operational proxy link container response wrapper status)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:** Container detailing the proxy result outcome profile:
    ```json
    {
      "status": 200,
      "headers": { "Server": "nginx", "Content-Type": "application/json" },
      "body": "{\n  \"received\": true\n}"
    }
    ```

### `GET /api/net/download`
*   **Description:** Streams files directly from remote web services onto a localized path location context path.
*   **Query Parameters:**
    *   `url` (Required) - Absolute target remote file link download location source.
    *   `path` (Required) - Destination local sandbox filename target path.
*   **Request Body:** None.
*   **Response Status:**
    *   `200 OK` (Download stream initialized correctly)
    *   `400 Bad Request` (Missing required url or path params)
*   **Response Headers:** `Content-Type: application/json` (or dynamic error string values)
*   **Response Body:** Stream buffers (or JSON string errors if validation parameters check breaks).

### `GET /api/network/diagnostics`
*   **Description:** Generates an engineering snapshot recording the application's underlying socket environment properties and connection health metrics.
*   **Query Parameters:** None.
*   **Request Body:** None.
*   **Response Status:**
    *   `200 OK` (Success)
    *   `500 Internal Server Error` (If native interface tracking loops or DNS trace steps crash)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:**
    ```json
    {
      "interfaces": {
        "active_transport": "WIFI",
        "link_downstream_kbps": 60000,
        "link_upstream_kbps": 12000,
        "is_network_metered": false
      },
      "system_proxy": {
        "is_proxy_active": false,
        "detected_host": "none",
        "detected_port": "none"
      },
      "dns_perf": {
        "diagnostic_target_host": "google.com",
        "resolution_successful": true,
        "resolved_ip_address": "142.251.47.46",
        "resolution_latency_ms": 3
      }
    }
    ```
