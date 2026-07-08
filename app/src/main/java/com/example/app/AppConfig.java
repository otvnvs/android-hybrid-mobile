//package com.example.app;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//import org.json.JSONObject;
//import android.util.Log;
//
//public class AppConfig {
//    private static final String TAG = "JAVA_AppConfig";
//    private static final String PREFS_NAME = "MaintenancePrefs";
//    private final Context context;
//
//    public AppConfig(Context context) {
//        this.context = context.getApplicationContext();
//        Log.d(TAG, "AppConfig module tracking initialized.");
//    }
//
//    public String getVirtualHost() {
//        String host = context.getString(R.string.virtual_host);
//        if (host == null || host.isEmpty()) return "";
//        return host.startsWith("https://") ? host : "https://" + host;
//    }
//
//    public String getWorkspaceFolderName() {
//        return context.getString(R.string.config_workspace_folder_name);
//    }
//
//    /**
//     * Persists the UI maintenance bundle properties safely into SharedPreferences storage
//     */
//    public void saveMaintenanceSettings(String autoUpdate, String interval, String url, String useAuth, String user, String pass, String subpath) {
//        Log.i(TAG, "saveMaintenanceSettings() invoked. Parsing incoming configuration bundle attributes...");
//        
//        try {
//            boolean parsedAutoUpdate = Boolean.parseBoolean(autoUpdate);
//            int parsedInterval = Integer.parseInt(interval);
//            boolean parsedUseAuth = Boolean.parseBoolean(useAuth);
//
//            Log.d(TAG, " -> Extracted fields data map:");
//            Log.d(TAG, "    [Auto Update]: " + parsedAutoUpdate);
//            Log.d(TAG, "    [Interval Hours]: " + parsedInterval);
//            Log.d(TAG, "    [Target Remote URL]: " + url);
//            Log.d(TAG, "    [Target SubPath]: " +  subpath != null ? subpath.trim() : "");
//            Log.d(TAG, "    [Requires Auth Proxy]: " + parsedUseAuth);
//            Log.d(TAG, "    [Username Field]: " + user);
//            Log.d(TAG, "    [Password Field Masked]: " + (pass != null ? "********" : "null"));
//
//            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
//            SharedPreferences.Editor editor = prefs.edit();
//            
//            editor.putBoolean("auto_update_enabled", parsedAutoUpdate);
//            editor.putInt("update_interval_hours", parsedInterval);
//            editor.putString("update_target_url", url);
//            editor.putBoolean("use_authentication", parsedUseAuth);
//            editor.putString("auth_username", user);
//            editor.putString("auth_password", pass);
//            editor.putString("update_target_subpath", subpath != null ? subpath.trim() : "");
//            
//            editor.apply(); // Execute safe, non-blocking asynchronous disk writes
//            Log.i(TAG, " -> Transaction committed successfully to Shared Preference database storage.");
//            
//        } catch (Exception e) {
//            Log.e(TAG, " -> Transaction breakdown! Failed to write settings data layout to disk: " + e.getMessage());
//        }
//    }
//
//
//
//    public String getUpdateTargetSubpath() {
//        String value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//                .getString("update_target_subpath", "");
//        Log.d(TAG, "getUpdateTargetSubpath() requested. Returning: " + value);
//        return value;
//    }
//
//    // --- Active parameters database reading interfaces ---
//
//    public boolean isAutoUpdateEnabled() {
//        boolean value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//                .getBoolean("auto_update_enabled", false);
//        Log.d(TAG, "isAutoUpdateEnabled() requested. Returning: " + value);
//        return value;
//    }
//
//    public int getUpdateIntervalHours() {
//        int value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//                .getInt("update_interval_hours", 24);
//        Log.d(TAG, "getUpdateIntervalHours() requested. Returning: " + value);
//        return value;
//    }
//
//    public String getUpdateTargetUrl() {
//        String value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//                .getString("update_target_url", "");
//        Log.d(TAG, "getUpdateTargetUrl() requested. Returning: " + value);
//        return value;
//    }
//
//    public boolean useAuthentication() {
//        boolean value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//                .getBoolean("use_authentication", false);
//        Log.d(TAG, "useAuthentication() requested. Returning: " + value);
//        return value;
//    }
//
//    public String getAuthUsername() {
//        String value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//                .getString("auth_username", "");
//        Log.d(TAG, "getAuthUsername() requested. Returning: " + value);
//        return value;
//    }
//
//    public String getAuthPassword() {
//        String value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//                .getString("auth_password", "");
//        Log.d(TAG, "getAuthPassword() requested. (Value retrieved securely from storage matrix)");
//        return value;
//    }
//
//    /**
//     * Updated: Serializes all runtime properties including subpath into an atomic JSON string
//     */
//    public String getMaintenanceConfigJson() {
//        try {
//            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
//            JSONObject json = new JSONObject();
//
//            json.put("autoUpdate", prefs.getBoolean("auto_update_enabled", false));
//            json.put("interval", prefs.getInt("update_interval_hours", 24));
//            json.put("url", prefs.getString("update_target_url", ""));
//            json.put("useAuth", prefs.getBoolean("use_authentication", false));
//            json.put("user", prefs.getString("auth_username", ""));
//            json.put("pass", prefs.getString("auth_password", ""));
//            json.put("subpath", prefs.getString("update_target_subpath", "")); // Expose to frontend
//
//            return json.toString();
//        } catch (Exception e) {
//            return "{}";
//        }
//    }
//}
//--------------------------------------------------------------------------------
package com.example.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import android.util.Log;

public class AppConfig {
    private static final String TAG = "JAVA_AppConfig";
    private static final String PREFS_NAME = "MaintenancePrefs";
    
    // Constant string identifiers representing the two explicit storage choices
    public static final String STORAGE_MODE_SANDBOX = "sandbox";
    public static final String STORAGE_MODE_PUBLIC = "public";
    
    private final Context context;

    public AppConfig(Context context) {
        this.context = context.getApplicationContext();
        Log.d(TAG, "AppConfig module tracking initialized.");
    }

    public String getVirtualHost() {
        String host = context.getString(R.string.virtual_host);
        if (host == null || host.isEmpty()) return "";
        return host.startsWith("https://") ? host : "https://" + host;
    }

    public String getWorkspaceFolderName() {
        return context.getString(R.string.config_workspace_folder_name);
    }

    /**
     * Refactored signature to accept and persist the explicit target storage strategy choice.
     */
    public void saveMaintenanceSettings(String autoUpdate, String interval, String url, String useAuth, 
                                        String user, String pass, String subpath, String storageMode) {
        Log.i(TAG, "saveMaintenanceSettings() invoked. Synchronizing application configuration state...");
        try {
            boolean parsedAutoUpdate = Boolean.parseBoolean(autoUpdate);
            int parsedInterval = Integer.parseInt(interval);
            boolean parsedUseAuth = Boolean.parseBoolean(useAuth);

            // Sanitize incoming storage strategy parameter, defaulting strictly to safe sandbox isolation
            String verifiedStorageMode = STORAGE_MODE_PUBLIC.equalsIgnoreCase(storageMode) ? STORAGE_MODE_PUBLIC : STORAGE_MODE_SANDBOX;

            Log.d(TAG, " -> Extracted profile data fields matrix:");
            Log.d(TAG, "    [Auto Update]: " + parsedAutoUpdate);
            Log.d(TAG, "    [Interval Hours]: " + parsedInterval);
            Log.d(TAG, "    [Target Remote URL]: " + url);
            Log.d(TAG, "    [Target SubPath]: " + (subpath != null ? subpath.trim() : ""));
            Log.d(TAG, "    [Requires Auth Proxy]: " + parsedUseAuth);
            Log.d(TAG, "    [Username Field]: " + user);
            Log.d(TAG, "    [Target Storage Strategy Mode]: " + verifiedStorageMode);

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("auto_update_enabled", parsedAutoUpdate);
            editor.putInt("update_interval_hours", parsedInterval);
            editor.putString("update_target_url", url);
            editor.putBoolean("use_authentication", parsedUseAuth);
            editor.putString("auth_username", user);
            editor.putString("auth_password", pass);
            editor.putString("update_target_subpath", subpath != null ? subpath.trim() : "");
            
            // Persist the explicit selection string
            editor.putString("active_storage_mode", verifiedStorageMode);
            editor.apply();
            Log.i(TAG, " -> Transaction committed successfully to Shared Preference database storage.");
        } catch (Exception e) {
            Log.e(TAG, " -> Transaction breakdown! Failed to write settings data layout to disk: " + e.getMessage());
        }
    }

    /**
     * Overloaded fallback implementation to preserve legacy method signatures safely across companion modules.
     */
    public void saveMaintenanceSettings(String autoUpdate, String interval, String url, String useAuth, String user, String pass, String subpath) {
        saveMaintenanceSettings(autoUpdate, interval, url, useAuth, user, pass, subpath, STORAGE_MODE_SANDBOX);
    }

    /**
     * Explicit getter providing programmatic inspection rules for dynamic path engines.
     */
    public String getActiveStorageMode() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String mode = prefs.getString("active_storage_mode", STORAGE_MODE_SANDBOX);
        Log.d(TAG, "getActiveStorageMode() requested. Operational anchor path verified as: " + mode);
        return mode;
    }

    /**
     * Diagnostic helper function returning whether public external partition access is active.
     */
    public boolean isPublicWorkspaceEnabled() {
        return STORAGE_MODE_PUBLIC.equals(getActiveStorageMode());
    }

    public String getUpdateTargetSubpath() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("update_target_subpath", "");
    }

    public boolean isAutoUpdateEnabled() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("auto_update_enabled", false);
    }

    public int getUpdateIntervalHours() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt("update_interval_hours", 24);
    }

    public String getUpdateTargetUrl() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("update_target_url", "");
    }

    public boolean useAuthentication() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("use_authentication", false);
    }

    public String getAuthUsername() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("auth_username", "");
    }

    public String getAuthPassword() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("auth_password", "");
    }

    /**
     * Expanded JSON payload constructor mapping properties directly to the Vue frontend config tree.
     */
    public String getMaintenanceConfigJson() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            JSONObject json = new JSONObject();
            json.put("autoUpdate", prefs.getBoolean("auto_update_enabled", false));
            json.put("interval", prefs.getInt("update_interval_hours", 24));
            json.put("url", prefs.getString("update_target_url", ""));
            json.put("useAuth", prefs.getBoolean("use_authentication", false));
            json.put("user", prefs.getString("auth_username", ""));
            json.put("pass", prefs.getString("auth_password", ""));
            json.put("subpath", prefs.getString("update_target_subpath", ""));
            
            // Injects the selection string outbound into the web data stream
            json.put("storageMode", prefs.getString("active_storage_mode", STORAGE_MODE_SANDBOX));
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }
}

