package com.example.app.services.example;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.example.app.MainActivity;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class PermissionsController {
    private static final String TAG = "PermissionsController";

    public PermissionsController() {}

    @RequestMapping(path = "/api/permissions/status", method = "POST")
    public ResponseContext checkPermissions(RequestContext request) {
        try {
            Context context = request.getAndroidContext();
            byte[] bodyBytes = request.getBody();
            String rawBodyText = (bodyBytes != null && bodyBytes.length > 0) ? new String(bodyBytes, StandardCharsets.UTF_8) : "{}";
            
            JSONObject bodyJson = new JSONObject(rawBodyText);
            JSONArray requestedPermissions = bodyJson.optJSONArray("permissions");

            if (requestedPermissions == null || requestedPermissions.length() == 0) {
                return buildErrorResponse(400, "Missing required array parameter: permissions");
            }

            JSONObject checkResults = new JSONObject();
            if (context != null) {
                for (int i = 0; i < requestedPermissions.length(); i++) {
                    String perm = requestedPermissions.getString(i);
                    int status = context.checkCallingOrSelfPermission(perm);
                    checkResults.put(perm, (status == PackageManager.PERMISSION_GRANTED) ? "GRANTED" : "DENIED");
                }
                JSONObject result = new JSONObject();
                result.put("status", "success");
                result.put("permissions_matrix", checkResults);
                return ResponseContext.status(200).contentType("application/json").body(result.toString()).build();
            }
            return buildErrorResponse(500, "Android context unavailable");
        } catch (Exception e) {
            return buildErrorResponse(500, "Error: " + e.getMessage());
        }
    }

    @RequestMapping(path = "/api/permissions/request", method = "POST")
    public ResponseContext requestPermissions(RequestContext request) {
        try {
            Context context = request.getAndroidContext();
            byte[] bodyBytes = request.getBody();
            String rawBodyText = (bodyBytes != null && bodyBytes.length > 0) ? new String(bodyBytes, StandardCharsets.UTF_8) : "{}";
            
            JSONObject bodyJson = new JSONObject(rawBodyText);
            JSONArray standardPermissions = bodyJson.optJSONArray("permissions");

            if (standardPermissions == null || standardPermissions.length() == 0) {
                return buildErrorResponse(400, "Missing required array parameter: permissions");
            }

            if (!(context instanceof MainActivity)) {
                return buildErrorResponse(422, "Active context must be an instance of MainActivity");
            }

            final MainActivity activity = (MainActivity) context;
            final int totalPerms = standardPermissions.length();
            final String[] permissionsArray = new String[totalPerms];
            
            for (int i = 0; i < totalPerms; i++) {
                permissionsArray[i] = standardPermissions.getString(i);
            }

            // Always trigger asynchronously on the UI thread to prevent layout freezing
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 23) {
                        activity.requestPermissions(permissionsArray, 2002);
                    }
                }
            });

            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("message", "System dialog sequence triggered successfully");
            return ResponseContext.status(202).contentType("application/json").body(result.toString()).build();

        } catch (Exception e) {
            Log.e(TAG, "Permissions request pipeline failure", e);
            return buildErrorResponse(500, "System execution failure: " + e.getMessage());
        }
    }

    private ResponseContext buildErrorResponse(int code, String message) {
        JSONObject errJson = new JSONObject();
        try {
            errJson.put("status", "error");
            errJson.put("message", message);
        } catch (Exception ignored) {}
        return ResponseContext.status(code).contentType("application/json").body(errJson.toString()).build();
    }
}

