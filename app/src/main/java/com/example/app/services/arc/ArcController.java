//package com.example.app.services.maintenance;
//import android.os.Environment;
//import android.util.Log;
//import com.example.app.MainActivity;
//import com.example.app.AppConfig;
//import com.example.app.services.RequestMapping;
//import com.example.app.services.RequestContext;
//import com.example.app.services.ResponseContext;
//import com.example.app.services.WebServiceRegistry;
//import com.example.app.UpdateManager; 
//import com.example.app.StorageManager;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.json.JSONArray;
//import java.nio.charset.StandardCharsets;
//import com.example.app.services.StorageService;
//
//public class ArcController {
//    private static final String TAG = "ArcController";
//    private final StorageService storageService = new StorageService();
//
//    public ArcController () {
//    }
//
//    //--------------------------------------------------------------------------------
//    //storage services
//    //--------------------------------------------------------------------------------
//
//    // Set a root storage sandbox folder inside Documents
//    private File getStorageRoot() {
//        //return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AppSandbox");
//        return Environment.getExternalStorageDirectory();
//    }
//
//    /**
//     * Helper to resolve path inputs while preventing path traversal attacks
//     */
//    private File resolveSafeFile(String relativePath) throws IOException {
//        File root = getStorageRoot();
//        if (!root.exists()) root.mkdirs();
//        if (relativePath == null || relativePath.isEmpty()) return root;
//        
//        File target = new File(root, relativePath);
//        if (!target.getCanonicalPath().startsWith(root.getCanonicalPath())) {
//            throw new SecurityException("Directory traversal attack detected!");
//        }
//        return target;
//    }
//
//    // =========================================================================
//    // ARCHIVAL OPERATIONS
//    // =========================================================================
//
//	@RequestMapping(path="/api/arc/unzip", method="POST")
//	public ResponseContext unzipArchiveFile(RequestContext request) {
//	    Log.d(TAG,"public ResponseContext unzipArchiveFile(RequestContext request)");
//	    try {
//		// Parse zip parameters from the JSON body payload
//		String jsonConfig = new String(request.getBody(), StandardCharsets.UTF_8);
//		JSONObject unzipRequest = new JSONObject(jsonConfig);
//		
//		String zipPath = unzipRequest.getString("zipPath");
//		String targetDirectoryPath = unzipRequest.getString("targetDirectory");
//
//		// Securely resolve source zip archive and target directory limits
//		File zipFile = resolveSafeFile(zipPath);
//		File targetDir = resolveSafeFile(targetDirectoryPath);
//
//		if (!zipFile.exists() || !zipFile.isFile()) {
//		    return buildErrorResponse(404, "Source ZIP file archive resource not found on local disk.");
//		}
//
//		// Ensure root output destination directory structure exists safely
//		if (!targetDir.exists()) {
//		    targetDir.mkdirs();
//		}
//
//		// Initialize Native Java Zip Processing Streams
//		try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))) {
//		    java.util.zip.ZipEntry entry;
//		    byte[] buffer = new byte[4096];
//
//		    while ((entry = zis.getNextEntry()) != null) {
//			// Ensure individual path entries are resolved against target safety zones
//			File newFile = resolveSafeFile(targetDirectoryPath + File.separator + entry.getName());
//
//			// Guard against structural Zip Slip path-traversal attacks embedded in archives
//			if (!newFile.getCanonicalPath().startsWith(targetDir.getCanonicalPath())) {
//			    throw new SecurityException("Zip Slip directory traversal attack attempt blocked: " + entry.getName());
//			}
//
//			if (entry.isDirectory()) {
//			    if (!newFile.exists()) {
//				newFile.mkdirs();
//			    }
//			} else {
//			    // Create structural parent layout context if missing for nested items
//			    File parent = newFile.getParentFile();
//			    if (parent != null && !parent.exists()) {
//				parent.mkdirs();
//			    }
//
//			    // Extract the compressed bytes directly to disk path
//			    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(newFile)) {
//				int len;
//				while ((len = zis.read(buffer)) > 0) {
//				    fos.write(buffer, 0, len);
//				}
//			    }
//			}
//			zis.closeEntry();
//		    }
//		}
//
//		// Return clean verification analytics tracing payload unpack status
//		JSONObject result = new JSONObject();
//		result.put("status", "success");
//		result.put("message", "Archive successfully extracted onto native filesystem.");
//		result.put("targetDirectory", targetDirectoryPath);
//
//		return ResponseContext.status(200)
//			.contentType("application/json")
//			.body(result.toString())
//			.build();
//
//	    } catch (SecurityException se) {
//	        Log.d(TAG, "Directory traversal extraction safety boundary violation: " + se.getMessage());
//		return buildErrorResponse(403, "Directory traversal extraction safety boundary violation: " + se.getMessage());
//	    } catch (Exception e) {
//	        Log.d(TAG, "Native extraction extraction execution layer crash: " + e.getMessage());
//		return buildErrorResponse(500, "Native extraction extraction execution layer crash: " + e.getMessage());
//	    }
//	}
//
//	@RequestMapping(path="/api/arc/zip", method="POST")
//	public ResponseContext zipDirectoryOrFile(RequestContext request) {
//	    try {
//		// Parse zip parameters from the JSON body payload
//		String jsonConfig = new String(request.getBody(), StandardCharsets.UTF_8);
//		JSONObject zipRequest = new JSONObject(jsonConfig);
//		
//		String sourcePath = zipRequest.getString("sourcePath");
//		String targetZipPath = zipRequest.getString("targetZipPath");
//
//		// Securely resolve paths within your storage root boundaries
//		File sourceFile = resolveSafeFile(sourcePath);
//		File targetZipFile = resolveSafeFile(targetZipPath);
//
//		if (!sourceFile.exists()) {
//		    return buildErrorResponse(404, "Source directory or file resource not found on local disk.");
//		}
//
//		// Ensure parent folder configuration for output ZIP exists safely
//		File parentDir = targetZipFile.getParentFile();
//		if (parentDir != null && !parentDir.exists()) {
//		    parentDir.mkdirs();
//		}
//
//		// Initialize Native Java Zip Output Streams
//		try (java.io.FileOutputStream fos = new java.io.FileOutputStream(targetZipFile);
//		     java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos)) {
//		    
//		    // Initiate recursive packing routine
//		    zipRecursiveHelper(sourceFile, sourceFile, zos);
//		}
//
//		// Trigger MediaScanner so the system acknowledges the file if stored in public directories
//		if (targetZipPath.contains("Download")) {
//		    android.media.MediaScannerConnection.scanFile(
//			request.getAndroidContext(), 
//			new String[]{targetZipFile.getAbsolutePath()}, 
//			null, 
//			null
//		    );
//		}
//
//		// Return clean verification tracking compression task completion
//		JSONObject result = new JSONObject();
//		result.put("status", "success");
//		result.put("message", "Files compressed successfully into ZIP archive.");
//		result.put("archiveSize", targetZipFile.length());
//
//		return ResponseContext.status(200)
//			.contentType("application/json")
//			.body(result.toString())
//			.build();
//
//	    } catch (SecurityException se) {
//		return buildErrorResponse(403, "Directory traversal compression boundary violation: " + se.getMessage());
//	    } catch (Exception e) {
//		return buildErrorResponse(500, "Native compression execution layer crash: " + e.getMessage());
//	    }
//	}
//
//    // =========================================================================
//    // HELPERS
//    // =========================================================================
//    /**
//     * Inline helper for generating uniform error messaging structures cleanly
//     */
//    private ResponseContext buildErrorResponse(int code, String message) {
//        JSONObject errJson = new JSONObject();
//        try {
//            errJson.put("status", "error");
//            errJson.put("message", message);
//        } catch (Exception ignored) {}
//        return ResponseContext.status(code).body(errJson.toString()).build();
//    }
//
//}
//--------------------------------------------------------------------------------
package com.example.app.services.maintenance;

import android.os.Environment;
import android.util.Log;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import com.example.app.services.StorageService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.json.JSONObject;

public class ArcController
{
    private static final String TAG="ArcController";
    private final StorageService storageService=new StorageService();

    public ArcController(){}

    private File getStorageRoot(){
        return Environment.getExternalStorageDirectory();
    }

    private File resolveSafeFile(String relativePath)throws IOException{
        File root=getStorageRoot();
        if(!root.exists())root.mkdirs();
        if(relativePath==null||relativePath.isEmpty())return root;
        File target=new File(root,relativePath);
        if(!target.getCanonicalPath().startsWith(root.getCanonicalPath())){
            throw new SecurityException("Directory traversal attack detected!");
        }
        return target;
    }

    @RequestMapping(path="/api/arc/unzip",method="POST")
    public ResponseContext unzipArchiveFile(RequestContext request){
        Log.d(TAG,"public ResponseContext unzipArchiveFile(RequestContext request)");
        try{
            String jsonConfig=new String(request.getBody(),StandardCharsets.UTF_8);
            JSONObject unzipRequest=new JSONObject(jsonConfig);
            String zipPath=unzipRequest.getString("zipPath");
            String targetDirectoryPath=unzipRequest.getString("targetDirectory");
            File zipFile=resolveSafeFile(zipPath);
            File targetDir=resolveSafeFile(targetDirectoryPath);
            if(!zipFile.exists()||!zipFile.isFile()){
                return buildErrorResponse(404,"Source ZIP file archive resource not found on local disk.");
            }
            if(!targetDir.exists()){
                targetDir.mkdirs();
            }
            try(java.util.zip.ZipInputStream zis=new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))){
                java.util.zip.ZipEntry entry;
                byte[]buffer=new byte[4096];
                while((entry=zis.getNextEntry())!=null){
                    File newFile=resolveSafeFile(targetDirectoryPath+File.separator+entry.getName());
                    if(!newFile.getCanonicalPath().startsWith(targetDir.getCanonicalPath())){
                        throw new SecurityException("Zip Slip directory traversal attack attempt blocked: "+entry.getName());
                    }
                    if(entry.isDirectory()){
                        if(!newFile.exists()){
                            newFile.mkdirs();
                        }
                    }else{
                        File parent=newFile.getParentFile();
                        if(parent!=null&&!parent.exists()){
                            parent.mkdirs();
                        }
                        try(java.io.FileOutputStream fos=new java.io.FileOutputStream(newFile)){
                            int len;
                            while((len=zis.read(buffer))>0){
                                fos.write(buffer,0,len);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }
            JSONObject result=new JSONObject();
            result.put("status","success");
            result.put("message","Archive successfully extracted onto native filesystem.");
            result.put("targetDirectory",targetDirectoryPath);
            return ResponseContext.status(200).contentType("application/json").body(result.toString()).build();
        }catch(SecurityException se){
            Log.d(TAG,"Directory traversal extraction safety boundary violation: "+se.getMessage());
            return buildErrorResponse(403,"Directory traversal extraction safety boundary violation: "+se.getMessage());
        }catch(Exception e){
            Log.d(TAG,"Native extraction extraction execution layer crash: "+e.getMessage());
            return buildErrorResponse(500,"Native extraction extraction execution layer crash: "+e.getMessage());
        }
    }

    @RequestMapping(path="/api/arc/zip",method="POST")
    public ResponseContext zipDirectoryOrFile(RequestContext request){
        try{
            String jsonConfig=new String(request.getBody(),StandardCharsets.UTF_8);
            JSONObject zipRequest=new JSONObject(jsonConfig);
            String sourcePath=zipRequest.getString("sourcePath");
            String targetZipPath=zipRequest.getString("targetZipPath");
            File sourceFile=resolveSafeFile(sourcePath);
            File targetZipFile=resolveSafeFile(targetZipPath);
            if(!sourceFile.exists()){
                return buildErrorResponse(404,"Source directory or file resource not found on local disk.");
            }
            File parentDir=targetZipFile.getParentFile();
            if(parentDir!=null&&!parentDir.exists()){
                parentDir.mkdirs();
            }
            try(FileOutputStream fos=new FileOutputStream(targetZipFile);ZipOutputStream zos=new ZipOutputStream(fos)){
                zipRecursiveHelper(sourceFile,sourceFile,zos);
            }
            if(targetZipPath.contains("Download")){
                android.media.MediaScannerConnection.scanFile(request.getAndroidContext(),new String[]{targetZipFile.getAbsolutePath()},null,null);
            }
            JSONObject result=new JSONObject();
            result.put("status","success");
            result.put("message","Files compressed successfully into ZIP archive.");
            result.put("archiveSize",targetZipFile.length());
            return ResponseContext.status(200).contentType("application/json").body(result.toString()).build();
        }catch(SecurityException se){
            return buildErrorResponse(403,"Directory traversal compression boundary violation: "+se.getMessage());
        }catch(Exception e){
            return buildErrorResponse(500,"Native compression execution layer crash: "+e.getMessage());
        }
    }

    // Restored the missing method needed for compression logic to compile
    private void zipRecursiveHelper(File rootFolder, File sourceFile, ZipOutputStream zos) throws IOException {
        if (sourceFile.isDirectory()) {
            File[] files = sourceFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    zipRecursiveHelper(rootFolder, file, zos);
                }
            }
        } else {
            byte[] buffer = new byte[4096];
            String relativePath = rootFolder.toURI().relativize(sourceFile.toURI()).getPath();
            try (FileInputStream fis = new FileInputStream(sourceFile)) {
                zos.putNextEntry(new ZipEntry(relativePath));
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
            }
        }
    }

    private ResponseContext buildErrorResponse(int code,String message){
        JSONObject errJson=new JSONObject();
        try{
            errJson.put("status","error");
            errJson.put("message",message);
        }catch(Exception ignored){}
        return ResponseContext.status(code).body(errJson.toString()).build();
    }
}

