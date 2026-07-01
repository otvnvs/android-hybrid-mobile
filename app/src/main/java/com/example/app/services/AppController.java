package com.example.app.services;

import android.os.Environment;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AppController {
    private static final String TAG = "AppController";
    private final StorageService storageService = new StorageService();

    @RequestMapping(path = "/api/app/export-localstorage", method = "POST")
    public ResponseContext exportLocalStorage(RequestContext request) {
        Log.i(TAG, " -> REST API: Triggering localstorage serialization export.");
        
        byte[] rawBody = request.getBody();
        if (rawBody.length == 0) {
            JSONObject errJson = new JSONObject();
            try {
                errJson.put("status", "error");
                errJson.put("message", "Payload packet empty.");
            } catch (JSONException ignored) {}

            return ResponseContext.status(400)
                    .body(errJson.toString())
                    .build();
        }

        try {
            String fileName = "ahm-localstorage-dump.json";
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadDir, fileName);
            
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            fos.write(rawBody);
            fos.close();
            
            android.media.MediaScannerConnection.scanFile(request.getAndroidContext(), new String[]{file.getAbsolutePath()}, null, null);
            
            JSONObject successJson = new JSONObject();
            successJson.put("status", "success");
            successJson.put("message", "Exported to downloads: " + fileName);

            return ResponseContext.status(200)
                    .body(successJson.toString())
                    .build();
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed executing export pipeline: " + e.getMessage());
            
            JSONObject errJson = new JSONObject();
            try {
                errJson.put("status", "error");
                errJson.put("message", "Failed saving file to disk.");
            } catch (JSONException ignored) {}

            return ResponseContext.status(500)
                    .body(errJson.toString())
                    .build();
        }
    }

    @RequestMapping(path = "/api/app/import-localstorage", method = "GET")
    public ResponseContext importLocalStorage(RequestContext request) {
        Log.i(TAG, " -> REST API: Accessing disk files to read storage backup.");
        try {
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadDir, "ahm-localstorage-dump.json");
            
            if (!file.exists() || !file.isFile()) {
                JSONObject missingJson = new JSONObject();
                missingJson.put("status", "error");
                missingJson.put("message", "Backup file 'ahm-localstorage-dump.json' not found in Downloads folder.");

                return ResponseContext.status(404)
                        .body(missingJson.toString())
                        .build();
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] fileBytes = new byte[(int) file.length()];
            int bytesRead = fis.read(fileBytes);
            fis.close();

            return ResponseContext.status(200)
                    .header("Cache-Control", "no-cache")
                    .body(fileBytes)
                    .build();
        } catch (IOException | JSONException e) {
            JSONObject errJson = new JSONObject();
            try {
                errJson.put("status", "error");
                errJson.put("message", "Internal storage read exception.");
            } catch (JSONException ignored) {}

            return ResponseContext.status(500)
                    .body(errJson.toString())
                    .build();
        }
    }

    // Demonstrates reading incoming JSON bodies and outputting structured org.json responses
    @RequestMapping(path = "/api/app/device-status", method = "GET")
    public ResponseContext getDeviceStatus(RequestContext request) {
        JSONObject responseJson = new JSONObject();
        
        try {
            responseJson.put("status", "active");
            responseJson.put("protocol", request.getHttpVersion());
            responseJson.put("userAgent", request.getHeader("User-Agent"));
            responseJson.put("domain", request.getDomain());
            
            // Example reading dynamic url path glob parameter e.g., if path was /api/app/device-status/{id}
            String pathId = request.getPathParam("id");
            if (pathId != null) {
                responseJson.put("requestedId", pathId);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON assembly tracking failed", e);
        }

        return ResponseContext.status(200)
                .header("X-Powered-By", "Android Native Framework Interceptor")
                .body(responseJson.toString())
                .build();
    }

    //--------------------------------------------------------------------------------
    //storage services
    //--------------------------------------------------------------------------------

    // Set a root storage sandbox folder inside Documents
    private File getStorageRoot() {
        //return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AppSandbox");
        return Environment.getExternalStorageDirectory();
    }

    /**
     * Helper to resolve path inputs while preventing path traversal attacks
     */
    private File resolveSafeFile(String relativePath) throws IOException {
        File root = getStorageRoot();
        if (!root.exists()) root.mkdirs();
        if (relativePath == null || relativePath.isEmpty()) return root;
        
        File target = new File(root, relativePath);
        if (!target.getCanonicalPath().startsWith(root.getCanonicalPath())) {
            throw new SecurityException("Directory traversal attack detected!");
        }
        return target;
    }

    // =========================================================================
    // READ OPERATIONS
    // =========================================================================

    @RequestMapping(path = "/api/fs/list", method = "GET")
    public ResponseContext listDirectory(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path"); // e.g., ?path=subfolder
            File targetDir = resolveSafeFile(pathQuery);
            
            JSONArray contents = storageService.readDirectory(targetDir);
            
            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("files", contents);
            
            return ResponseContext.status(200).body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(400, "Failed listing directory: " + e.getMessage());
        }
    }

    @RequestMapping(path = "/api/fs/read", method = "GET")
    public ResponseContext readFileContent(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path"); // e.g., ?path=notes/todo.txt
            File targetFile = resolveSafeFile(pathQuery);
            
            byte[] fileData = storageService.readFile(targetFile);
            String mimeType = "application/octet-stream";
            
            if (targetFile.getName().endsWith(".txt")) mimeType = "text/plain";
            if (targetFile.getName().endsWith(".json")) mimeType = "application/json";

            return ResponseContext.status(200)
                    .contentType(mimeType)
                    .body(fileData)
                    .build();
        } catch (Exception e) {
            return buildErrorResponse(404, "Failed reading file: " + e.getMessage());
        }
    }

    // =========================================================================
    // CREATE OPERATIONS
    // =========================================================================

    @RequestMapping(path = "/api/fs/mkdir", method = "POST")
    public ResponseContext createDirectory(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path"); // e.g., ?path=deep/nested/folder
            String recursiveStr = request.getQueryParam("recursive"); // e.g., ?recursive=true
            
            boolean recursive = "true".equalsIgnoreCase(recursiveStr);
            File targetDir = resolveSafeFile(pathQuery);
            
            boolean success = storageService.createDirectory(targetDir, recursive);
            
            JSONObject result = new JSONObject();
            result.put("status", success ? "success" : "error");
            result.put("message", success ? "Directory matched/created." : "Could not create directory structural layout.");
            
            return ResponseContext.status(success ? 200 : 500).body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(400, "Directory processing failure: " + e.getMessage());
        }
    }

//    @RequestMapping(path = "/api/fs/write", method = "POST")
//    public ResponseContext createOrWriteFile(RequestContext request) {
//        try {
//            String pathQuery = request.getQueryParam("path"); // e.g., ?path=logs/session.json
//            File targetFile = resolveSafeFile(pathQuery);
//            
//            // Uses request body payload
//            byte[] dataPayload = request.getBody();
//            storageService.createFile(targetFile, dataPayload);
//            
//            JSONObject result = new JSONObject();
//            result.put("status", "success");
//            result.put("message", "File saved successfully: " + targetFile.getName());
//            
//            return ResponseContext.status(200).body(result.toString()).build();
//        } catch (Exception e) {
//            return buildErrorResponse(500, "File persist error: " + e.getMessage());
//        }
//    }
@RequestMapping(path = "/api/fs/write", method = "POST")
public ResponseContext createOrWriteFile(RequestContext request) {
    try {
        String pathQuery = request.getQueryParam("path");
        // FIX: Extract content via parameter key wrapper instead of reading a raw body stream
        String textContent = request.getQueryParam("content"); 
        
        File targetFile = resolveSafeFile(pathQuery);
        byte[] dataPayload = textContent != null ? textContent.getBytes("UTF-8") : new byte[0];
        
        storageService.createFile(targetFile, dataPayload);

        JSONObject result = new JSONObject();
        result.put("status", "success");
        return ResponseContext.status(200).body(result.toString()).build();
    } catch (Exception e) {
        return buildErrorResponse(500, "File persist error: " + e.getMessage());
    }
}


    // =========================================================================
    // DELETE OPERATIONS
    // =========================================================================

    @RequestMapping(path = "/api/fs/delete", method = "DELETE")
    public ResponseContext deleteFileSystemPath(RequestContext request) {
        try {
            String pathQuery = request.getQueryParam("path"); // e.g., ?path=deep/nested/folder
            String recursiveStr = request.getQueryParam("recursive"); // e.g., ?recursive=true
            
            boolean recursive = "true".equalsIgnoreCase(recursiveStr);
            File targetFile = resolveSafeFile(pathQuery);
            
            // Safety Check: Do not let API delete the root folder accidentally
            if (targetFile.getCanonicalPath().equals(getStorageRoot().getCanonicalPath())) {
                return buildErrorResponse(403, "Forbidden: Cannot delete the storage environment root context.");
            }

            boolean success = storageService.deletePath(targetFile, recursive);
            
            JSONObject result = new JSONObject();
            result.put("status", success ? "success" : "error");
            result.put("message", success ? "Deleted resource cleanly." : "Failed completely clearing resource targets.");
            
            return ResponseContext.status(success ? 200 : 500).body(result.toString()).build();
        } catch (Exception e) {
            return buildErrorResponse(400, "Resource cleaning failure: " + e.getMessage());
        }
    }

	@RequestMapping(path="/api/fs/unzip", method="POST")
	public ResponseContext unzipArchiveFile(RequestContext request) {
	    try {
		// Parse zip parameters from the JSON body payload
		String jsonConfig = new String(request.getBody(), StandardCharsets.UTF_8);
		JSONObject unzipRequest = new JSONObject(jsonConfig);
		
		String zipPath = unzipRequest.getString("zipPath");
		String targetDirectoryPath = unzipRequest.getString("targetDirectory");

		// Securely resolve source zip archive and target directory limits
		File zipFile = resolveSafeFile(zipPath);
		File targetDir = resolveSafeFile(targetDirectoryPath);

		if (!zipFile.exists() || !zipFile.isFile()) {
		    return buildErrorResponse(404, "Source ZIP file archive resource not found on local disk.");
		}

		// Ensure root output destination directory structure exists safely
		if (!targetDir.exists()) {
		    targetDir.mkdirs();
		}

		// Initialize Native Java Zip Processing Streams
		try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))) {
		    java.util.zip.ZipEntry entry;
		    byte[] buffer = new byte[4096];

		    while ((entry = zis.getNextEntry()) != null) {
			// Ensure individual path entries are resolved against target safety zones
			File newFile = resolveSafeFile(targetDirectoryPath + File.separator + entry.getName());

			// Guard against structural Zip Slip path-traversal attacks embedded in archives
			if (!newFile.getCanonicalPath().startsWith(targetDir.getCanonicalPath())) {
			    throw new SecurityException("Zip Slip directory traversal attack attempt blocked: " + entry.getName());
			}

			if (entry.isDirectory()) {
			    if (!newFile.exists()) {
				newFile.mkdirs();
			    }
			} else {
			    // Create structural parent layout context if missing for nested items
			    File parent = newFile.getParentFile();
			    if (parent != null && !parent.exists()) {
				parent.mkdirs();
			    }

			    // Extract the compressed bytes directly to disk path
			    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(newFile)) {
				int len;
				while ((len = zis.read(buffer)) > 0) {
				    fos.write(buffer, 0, len);
				}
			    }
			}
			zis.closeEntry();
		    }
		}

		// Return clean verification analytics tracing payload unpack status
		JSONObject result = new JSONObject();
		result.put("status", "success");
		result.put("message", "Archive successfully extracted onto native filesystem.");
		result.put("targetDirectory", targetDirectoryPath);

		return ResponseContext.status(200)
			.contentType("application/json")
			.body(result.toString())
			.build();

	    } catch (SecurityException se) {
		return buildErrorResponse(403, "Directory traversal extraction safety boundary violation: " + se.getMessage());
	    } catch (Exception e) {
		return buildErrorResponse(500, "Native extraction extraction execution layer crash: " + e.getMessage());
	    }
	}

	@RequestMapping(path="/api/fs/zip", method="POST")
	public ResponseContext zipDirectoryOrFile(RequestContext request) {
	    try {
		// Parse zip parameters from the JSON body payload
		String jsonConfig = new String(request.getBody(), StandardCharsets.UTF_8);
		JSONObject zipRequest = new JSONObject(jsonConfig);
		
		String sourcePath = zipRequest.getString("sourcePath");
		String targetZipPath = zipRequest.getString("targetZipPath");

		// Securely resolve paths within your storage root boundaries
		File sourceFile = resolveSafeFile(sourcePath);
		File targetZipFile = resolveSafeFile(targetZipPath);

		if (!sourceFile.exists()) {
		    return buildErrorResponse(404, "Source directory or file resource not found on local disk.");
		}

		// Ensure parent folder configuration for output ZIP exists safely
		File parentDir = targetZipFile.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
		    parentDir.mkdirs();
		}

		// Initialize Native Java Zip Output Streams
		try (java.io.FileOutputStream fos = new java.io.FileOutputStream(targetZipFile);
		     java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos)) {
		    
		    // Initiate recursive packing routine
		    zipRecursiveHelper(sourceFile, sourceFile, zos);
		}

		// Trigger MediaScanner so the system acknowledges the file if stored in public directories
		if (targetZipPath.contains("Download")) {
		    android.media.MediaScannerConnection.scanFile(
			request.getAndroidContext(), 
			new String[]{targetZipFile.getAbsolutePath()}, 
			null, 
			null
		    );
		}

		// Return clean verification tracking compression task completion
		JSONObject result = new JSONObject();
		result.put("status", "success");
		result.put("message", "Files compressed successfully into ZIP archive.");
		result.put("archiveSize", targetZipFile.length());

		return ResponseContext.status(200)
			.contentType("application/json")
			.body(result.toString())
			.build();

	    } catch (SecurityException se) {
		return buildErrorResponse(403, "Directory traversal compression boundary violation: " + se.getMessage());
	    } catch (Exception e) {
		return buildErrorResponse(500, "Native compression execution layer crash: " + e.getMessage());
	    }
	}

	// Private structural helper method to handle deep recursive archive compilation
	private void zipRecursiveHelper(File rootFolder, File currentFile, java.util.zip.ZipOutputStream zos) throws IOException {
	    if (currentFile.isDirectory()) {
		File[] children = currentFile.listFiles();
		if (children != null) {
		    for (File child : children) {
			zipRecursiveHelper(rootFolder, child, zos);
		    }
		}
	    } else {
		// Calculate standard relative zip path entry notation (using forward slashes)
		String rootPath = rootFolder.getCanonicalPath();
		String currentPath = currentFile.getCanonicalPath();
		String relativeZipPath = currentPath.substring(rootPath.length() + 1).replace(File.separatorChar, '/');

		java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(relativeZipPath);
		zos.putNextEntry(zipEntry);

		// Pipe actual resource bytes into the outbound stream architecture
		try (java.io.FileInputStream fis = new java.io.FileInputStream(currentFile)) {
		    byte[] buffer = new byte[4096]; // Fixed
		    int bytesRead;
		    while ((bytesRead = fis.read(buffer)) >= 0) {
			zos.write(buffer, 0, bytesRead);
		    }
		}
		zos.closeEntry();
	    }
	}




    // =========================================================================
    // NET
    // =========================================================================
	@RequestMapping(path="/api/net/proxy", method="POST")
	public ResponseContext proxyHttpRequest(RequestContext request) {
	    try {
		// Parse the bridge configuration sent by JavaScript
		String jsonConfig = new String(request.getBody(), StandardCharsets.UTF_8);
		JSONObject bridgeRequest = new JSONObject(jsonConfig);
		
		String targetUrl = bridgeRequest.getString("url");
		String method = bridgeRequest.optString("method", "GET").toUpperCase();
		JSONObject headers = bridgeRequest.optJSONObject("headers");
		String bodyPayload = bridgeRequest.optString("body", "");

		// Setup the native network connection
		java.net.URL url = new java.net.URL(targetUrl);
		java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
		conn.setRequestMethod(method);
		conn.setDoInput(true);

		// Forward headers from WebView JavaScript to native connection
		if (headers != null) {
		    java.util.Iterator<String> keys = headers.keys();
		    while (keys.hasNext()) {
			String key = keys.next();
			conn.setRequestProperty(key, headers.getString(key));
		    }
		}

		// Write outbound body payload if present
		if (!bodyPayload.isEmpty() && ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) {
		    conn.setDoOutput(true);
		    try (java.io.OutputStream os = conn.getOutputStream()) {
			os.write(bodyPayload.getBytes(StandardCharsets.UTF_8));
		    }
		}

		// Read response code
		int responseCode = conn.getResponseCode();
		
		// Read response headers to forward back
		JSONObject responseHeaders = new JSONObject();
		for (java.util.Map.Entry<String, java.util.List<String>> entries : conn.getHeaderFields().entrySet()) {
		    if (entries.getKey() != null && !entries.getValue().isEmpty()) {
			responseHeaders.put(entries.getKey(), entries.getValue().get(0));
		    }
		}

		// Stream inbound response body
		java.io.InputStream is = (responseCode >= 200 && responseCode < 400) 
		    ? conn.getInputStream() 
		    : conn.getErrorStream();
		    
		byte[] responseBytes = new byte[0];
		if (is != null) {
		    java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
		    byte[] buffer = new byte[4096];
		    int length;
		    while ((length = is.read(buffer)) != -1) {
			bos.write(buffer, 0, length);
		    }
		    responseBytes = bos.toByteArray();
		    is.close();
		}

		// Package structure to emulate direct fetch mechanics
		JSONObject wrapperResult = new JSONObject();
		wrapperResult.put("status", responseCode);
		wrapperResult.put("headers", responseHeaders);
		wrapperResult.put("body", new String(responseBytes, StandardCharsets.UTF_8));

		return ResponseContext.status(200)
			.contentType("application/json")
			.body(wrapperResult.toString())
			.build();

	    } catch (Exception e) {
		return buildErrorResponse(500, "Native proxy routing failed: " + e.getMessage());
	    }
	}

	@RequestMapping(path="/api/net/download", method="POST")
	public ResponseContext downloadFileRemote(RequestContext request) {
	    try {
		// Parse inbound parameters from JSON payload
		String jsonConfig = new String(request.getBody(), StandardCharsets.UTF_8);
		JSONObject downloadRequest = new JSONObject(jsonConfig);
		
		String sourceUrl = downloadRequest.getString("url");
		String targetPath = downloadRequest.getString("path");
		JSONObject headers = downloadRequest.optJSONObject("headers");

		// Resolve local file path securely using your traversal protection
		File targetFile = resolveSafeFile(targetPath);
		
		// Ensure parent directory architecture exists
		File parentDir = targetFile.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
		    parentDir.mkdirs();
		}

		// Establish the HTTP connection to remote resource
		java.net.URL url = new java.net.URL(sourceUrl);
		java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");

		// Attach custom authentication or routing headers if present
		if (headers != null) {
		    java.util.Iterator<String> keys = headers.keys();
		    while (keys.hasNext()) {
			String key = keys.next();
			conn.setRequestProperty(key, headers.getString(key));
		    }
		}

		int responseCode = conn.getResponseCode();
		if (responseCode < 200 || responseCode >= 300) {
		    return buildErrorResponse(responseCode, "Remote server returned failure code: " + responseCode);
		}

		// Read remote content stream and pipe directly to disk
		try (java.io.InputStream is = conn.getInputStream();
		     java.io.FileOutputStream fos = new java.io.FileOutputStream(targetFile)) {
		    
		    byte[] buffer = new byte[8192];
		    int bytesRead;
		    while ((bytesRead = is.read(buffer)) != -1) {
			fos.write(buffer, 0, bytesRead);
		    }
		}

		// Trigger MediaScanner so the system acknowledges the file if stored in public directories
		if (targetPath.contains("Download")) {
		    android.media.MediaScannerConnection.scanFile(
			request.getAndroidContext(), 
			new String[]{targetFile.getAbsolutePath()}, 
			null, 
			null
		    );
		}

		// Return clean success summary tracking payload dimensions
		JSONObject result = new JSONObject();
		result.put("status", "success");
		result.put("message", "Resource downloaded successfully via native pipeline.");
		result.put("local_path", targetPath);
		result.put("file_size_bytes", targetFile.length());

		return ResponseContext.status(200)
			.contentType("application/json")
			.body(result.toString())
			.build();

	    } catch (SecurityException se) {
		return buildErrorResponse(403, "Directory traversal safety violation: " + se.getMessage());
	    } catch (Exception e) {
		return buildErrorResponse(500, "Native download executor pipeline failed: " + e.getMessage());
	    }
	}
    // =========================================================================
    // HELPERS
    // =========================================================================
    /**
     * Inline helper for generating uniform error messaging structures cleanly
     */
    private ResponseContext buildErrorResponse(int code, String message) {
        JSONObject errJson = new JSONObject();
        try {
            errJson.put("status", "error");
            errJson.put("message", message);
        } catch (Exception ignored) {}
        return ResponseContext.status(code).body(errJson.toString()).build();
    }


}

