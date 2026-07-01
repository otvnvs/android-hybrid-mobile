# File System REST API Documentation (`/api/fs`)

This documentation outlines the REST API endpoints (`/list`, `/read`, `/write`, `/mkdir`, `/delete`) exposed by the Android WebView interceptor (`WebViewClient.shouldInterceptRequest`). These endpoints support file management operations with optional client-side simulation via `localStorage` in offline scenarios.

## Endpoints

### 1. List Directory Contents
* **URL:** `/api/fs/list`
* **Method:** `GET`
* **Query Parameters:** `path` (string, required) - URL-encoded directory path.
* **Response:** JSON list of files/directories.

### 2. Read File Content
* **URL:** `/api/fs/read`
* **Method:** `GET`
* **Query Parameters:** `path` (string, required) - URL-encoded file path.
* **Response:** `text/plain` file content.

### 3. Write File Content
* **URL:** `/api/fs/write`
* **Method:** `POST`
* **Query Parameters:**
  * `path` (string, required) - URL-encoded path where the file should be saved.
  * `content` (string, optional) - URL-encoded text content to write to the file.
* **Body:** Empty (payload is processed via the `content` query parameter).
* **Response:** JSON object indicating execution status.
  ```json
  {
    "status": "success"
  }
  ```
### 4. Create Directory
* **URL:** `/api/fs/mkdir`
* **Method:** `POST`
* **Query Parameters:** `path` (required), `recursive` (optional, boolean).

### 5. Delete File or Directory
* **URL:** `/api/fs/delete`
* **Method:** `DELETE`
* **Query Parameters:** `path` (required), `recursive` (optional, boolean).

### 6. Extract Zip Archive File
* **URL:** `/api/fs/unzip`
* **Method:** `POST`
* **Headers:** 
  * `Content-Type: application/json`
* **Query Parameters:** None.

#### Request Body
The request payload must be a JSON structural object defining target compression coordinates:

| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `zipPath` | string | **Yes** | Relative path targeting the local `.zip` file on disk to extract. |
| `targetDirectory` | string | **Yes** | Relative path directory location where content files should be extracted. |

**Example Request Payload:**
```json
{
  "zipPath": "downloads/web_assets_v2.zip",
  "targetDirectory": "www/updates/v2"
}
```

#### Response Body (`200 OK`)
```json
{
  "status": "success",
  "message": "Archive successfully extracted onto native filesystem.",
  "targetDirectory": "www/updates/v2"
}
```

### 7. Create Zip Archive From Directory
* **URL:** `/api/fs/zip`
* **Method:** `POST`
* **Headers:** 
  * `Content-Type: application/json`
* **Query Parameters:** None.

#### Request Body
The request payload must be a JSON structural object defining target paths:

| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `sourcePath` | string | **Yes** | Relative path targeting the local directory or file to compress. |
| `targetZipPath` | string | **Yes** | Relative path destination where the output `.zip` archive file will be saved. |

**Example Request Payload:**
```json
{
  "sourcePath": "www/userdata/backups",
  "targetZipPath": "downloads/user_data_export.zip"
}
```

#### Response Body (`200 OK`)
```json
{
  "status": "success",
  "message": "Files compressed successfully into ZIP archive.",
  "archiveSize": 5242880
}
```

