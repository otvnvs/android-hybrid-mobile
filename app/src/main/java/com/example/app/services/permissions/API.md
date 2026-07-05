## 8. Android Core Security Runtime Permissions (`PermissionsController`)

Provides lightweight, deadlock-free structural tracking and programmatic user dialog dispatching capabilities over the application container permission lifecycle context. 

To eliminate low-level JVM-to-WebView threading deadlocks on modern Android versions, the request API operates strictly asynchronously, allowing the client testing framework to implement non-blocking polling loops during synchronous evaluation scenarios.

### `POST /api/permissions/status`
*   **Description:** Batch-evaluates the active system clearance metrics for an incoming list of Android manifest permission strings.
*   **Query Parameters:** None.
*   **Request Body:**
    ```json
    {
      "permissions": [
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO"
      ]
    }
    ```
*   **Response Status:** `200 OK` (Success)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:**
    ```json
    {
      "status": "success",
      "permissions_matrix": {
        "android.permission.CAMERA": "GRANTED",
        "android.permission.RECORD_AUDIO": "DENIED"
      }
    }
    ```

### `POST /api/permissions/request`
*   **Description:** Asynchronously dispatches an invitation request to cross over onto the native Android UI thread loop and inflate the platform user authorization prompt dialog box modal over the active viewport layout.
*   **Query Parameters:** None.
*   **Request Body:**
    ```json
    {
      "permissions": [
        "android.permission.CAMERA"
      ]
    }
    ```
*   **Response Status:** `202 Accepted` (Request acknowledged and safely queued for native user interaction)
*   **Response Headers:** `Content-Type: application/json`
*   **Response Body:**
    ```json
    {
      "status": "success",
      "message": "System dialog sequence triggered successfully"
    }
    ```

