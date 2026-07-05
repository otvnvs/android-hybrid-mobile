## Cross-Origin Network Proxy Broker (`NetController`)

### `POST /api/net/proxy`
*   **Description:** Bypasses browser CORS constraints by forwarding a custom HTTP/HTTPS connection stream to a target server via the native Android Java layer network architecture. Returns remote response structures and headers.
*   **Query Parameters:** None.
*   **Request Body:** `application/json` object detailing connection requirements:
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
      "status": 201,
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
