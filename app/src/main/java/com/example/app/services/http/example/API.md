## Application & System Services (`ExampleController`)

### `GET /api/example/get-test`
*   **Description:** Intercepts and logs a diagnostic test call natively on the device while parsing structural analytical tracking criteria. Displays a native Android Toast notice upon tracking interception.
*   **Query Parameters:**
    *   `tracking_id` (Optional): An unique identifier tracking string mapped to the request lifetime.
    *   `filter` (Optional): Classification filter criteria applied to the active processing scope.
*   **Request Body:** None.
*   **Response Status:**
    *   `200 OK` (On successful request processing)
    *   `500 Internal Server Error` (If runtime system exceptions occur)
*   **Response Headers:**
    *   `Content-Type: application/json`
    *   `X-Server-Response-Engine: Android-Native-JVM`
    *   `X-Echo-Tracking-ID: <received_tracking_id_or_none>`
*   **Response Body:**
    ```json
    {
      "status": "processed",
      "received_tracking_id": "none",
      "received_filter": "none"
    }
    ```

---

### `POST /api/example/mutation-test`
*   **Description:** Processes a structural state change or write operation request payload inside the native framework runtime engine. Displays an interactive Toast displaying the incoming message payload parameter.
*   **Query Parameters:** None.
*   **Request Body:** `application/json` (Optional metadata configurations).
    ```json
    {
      "requested_status_code": 200,
      "message_payload": "Custom override message"
    }
    ```
*   **Response Status:** 
    *   Dynamic response code dictated by the request body (`requested_status_code`, defaults to `200 OK`)
    *   `400 Bad Request` (If the input payload text breaks JSON structurally or encounters verification failures)
*   **Response Headers:**
    *   `Content-Type: application/json`
    *   `X-Processed-By-Method: POST`
    *   `Cache-Control: no-store, max-age=0`
*   **Response Body:**
    ```json
    {
      "echo_method": "POST",
      "echo_message": "Default Echo response",
      "payload_integrity_check": true
    }
    ```

---

### `PUT /api/example/mutation-test`
*   **Description:** Handles full replacements or updates of server objects passing through the mutation route mapping logic. Displays an intercept message Toast.
*   **Query Parameters:** None.
*   **Request Body:** `application/json` (Optional metadata configurations mirroring `POST` layout).
*   **Response Status:**
    *   Dynamic code based on body instruction (Defaults to `200 OK`)
    *   `400 Bad Request` (On body structural mapping breakdown)
*   **Response Headers:**
    *   `Content-Type: application/json`
    *   `X-Processed-By-Method: PUT`
    *   `Cache-Control: no-store, max-age=0`
*   **Response Body:**
    ```json
    {
      "echo_method": "PUT",
      "echo_message": "Default Echo response",
      "payload_integrity_check": true
    }
    ```

---

### `PATCH /api/example/mutation-test`
*   **Description:** Manages precise partial structural payload updates inside the target pipeline execution path. Triggers a diagnostic user interface validation Toast.
*   **Query Parameters:** None.
*   **Request Body:** `application/json` (Optional metadata configurations mirroring `POST` layout).
*   **Response Status:**
    *   Dynamic code based on body instruction (Defaults to `200 OK`)
    *   `400 Bad Request` (On body structural mapping breakdown)
*   **Response Headers:**
    *   `Content-Type: application/json`
    *   `X-Processed-By-Method: PATCH`
    *   `Cache-Control: no-store, max-age=0`
*   **Response Body:**
    ```json
    {
      "echo_method": "PATCH",
      "echo_message": "Default Echo response",
      "payload_integrity_check": true
    }
    ```

---

### `DELETE /api/example/mutation-test`
*   **Description:** Facilitates deletion executions for registered backend objects while rendering native execution confirmations onto the hardware display layer via a Toast banner.
*   **Query Parameters:** None.
*   **Request Body:** `application/json` (Optional metadata configurations mirroring `POST` layout).
*   **Response Status:**
    *   Dynamic code based on body instruction (Defaults to `200 OK`)
    *   `400 Bad Request` (On body structural mapping breakdown)
*   **Response Headers:**
    *   `Content-Type: application/json`
    *   `X-Processed-By-Method: DELETE`
    *   `Cache-Control: no-store, max-age=0`
*   **Response Body:**
    ```json
    {
      "echo_method": "DELETE",
      "echo_message": "Default Echo response",
      "payload_integrity_check": true
    }
    ```

## Real-Time Communication & WebSockets (`ExampleWssController`)

### `WS /api/ws/testing-suite`
*   **Description:** Establishes a persistent, stateful WebSocket connection to handle real-time bi-directional messaging, diagnostic testing suites, and data stream pushes directly from the native Android JVM runtime layer.

---

#### **Lifecycle: Connection Opened (`@WebSocketOnOpen`)**
*   **Trigger:** Executed automatically when a client initiates a handshake and connects successfully to the gateway path.
*   **Inbound Initial Payload:** None.
*   **Server Outbound Welcome Frame:**
    ```json
    {
      "status": "connected",
      "message": "Welcome from Native Android JVM Lifecycle Handler",
      "assigned_id": "ws_session_unique_id_string"
    }
    ```

---

#### **Message Processing: Text Frames (`@WebSocketMapping`)**
Handles inbound textual payloads sent by the client. The runtime evaluates commands inside the message layer and routes execution dynamically.

##### **Scenario A: Standard Command Echo**
*   **Client Inbound Frame:**
    ```json
    {
      "command": "any_custom_test_action"
    }
    ```
*   **Server Immediate Outbound Response:**
    ```json
    {
      "status": "success",
      "echo_command": "any_custom_test_action"
    }
    ```

##### **Scenario B: Multithreaded Heartbeat Stream**
*   **Client Inbound Frame:**
    ```json
    {
      "command": "start_heartbeat_stream"
    }
    ```
*   **Server Stream Pushes (Interval-Based Execution):**
    Spawns an asynchronous background stream that broadcasts updates sequentially every 1000ms.
    *   **Tick 1 (After 1s delay):**
        ```json
        { "status": "streaming", "tick_index": 1, "engine_layer": "Android-Native-JVM" }
        ```
    *   **Tick 2 (After 2s delay):**
        ```json
        { "status": "streaming", "tick_index": 2, "engine_layer": "Android-Native-JVM" }
        ```
    *   **Tick 3 (After 3s delay):**
        ```json
        { "status": "streaming", "tick_index": 3, "engine_layer": "Android-Native-JVM" }
        ```
    *   **Final Frame (Stream Termination):**
        ```json
        { "status": "complete" }
        ```

##### **Scenario C: Exception Fallback Handling**
*   **Trigger:** Dispatched if the raw incoming textual data structure breaks JSON structure parsing limits.
*   **Server Error Outbound Response:**
    ```json
    {
      "status": "error",
      "message": "Value <invalid_text> of type java.lang.String cannot be converted to JSONObject"
    }
    ```

---

#### **Lifecycle: Connection Closed (`@WebSocketOnClose`)**
*   **Trigger:** Executed instantly when a client drops network presence, requests a disconnect frame, or times out.
*   **Server Behavior:** Logs the drop context safely via `Log.i` to clear open native resources tracking contexts linked to the corresponding internal session identifier.

