package com.example.app.services.maintenance;
import android.os.Environment;
import android.util.Log;
import com.example.app.MainActivity;
import com.example.app.AppConfig;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import com.example.app.services.WebServiceRegistry;
import com.example.app.UpdateManager; 
import com.example.app.StorageManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.nio.charset.StandardCharsets;
import com.example.app.services.StorageService;

public class FsController {
    private static final String TAG = "FsController";
    private static volatile String currentStatusMessage = "Idle";
    private final StorageService storageService = new StorageService();

    public FsController () {
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
//@RequestMapping(path = "/api/fs/write", method = "POST")
//public ResponseContext createOrWriteFile(RequestContext request) {
//    try {
//        String pathQuery = request.getQueryParam("path");
//        // FIX: Extract content via parameter key wrapper instead of reading a raw body stream
//        String textContent = request.getQueryParam("content"); 
//        
//        File targetFile = resolveSafeFile(pathQuery);
//        byte[] dataPayload = textContent != null ? textContent.getBytes("UTF-8") : new byte[0];
//        
//        storageService.createFile(targetFile, dataPayload);
//
//        JSONObject result = new JSONObject();
//        result.put("status", "success");
//        return ResponseContext.status(200).body(result.toString()).build();
//    } catch (Exception e) {
//        return buildErrorResponse(500, "File persist error: " + e.getMessage());
//    }
//}
@RequestMapping(path="/api/fs/write", method="POST")
public ResponseContext createOrWriteFile(RequestContext request) {
    try {
        String pathQuery = request.getQueryParam("path");
        File targetFile = resolveSafeFile(pathQuery);

        // FIX: Extract raw binary bytes directly from the body envelope payload
        byte[] dataPayload = request.getBody();
        if (dataPayload == null) {
            dataPayload = new byte[0];
        }

        // Commit byte buffers directly to flash sectors via your storage service
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

