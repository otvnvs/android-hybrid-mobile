package com.example.app.services.http.environment;

import android.content.Context;
import org.json.JSONObject;

import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;

public class EnvironmentController {

    public EnvironmentController() {}

    @RequestMapping(path = "/api/environment.json", method = "GET")
    public ResponseContext getEnvironment(RequestContext request) {
        try {
            JSONObject result = new JSONObject();
            result.put("environment", "android-hybrid");

            return ResponseContext.status(200)
                    .contentType("application/json")
                    .header("X-Server-Response-Engine", "Android-Native-JVM")
                    .body(result.toString())
                    .build();
        } catch (Exception e) {
            return buildErrorResponse(500, e.getMessage());
        }
    }

    private ResponseContext buildErrorResponse(int code, String message) {
        JSONObject errJson = new JSONObject();
        try {
            errJson.put("status", "error");
            errJson.put("message", message);
        } catch (Exception ignored) {}

        return ResponseContext.status(code)
                .contentType("application/json")
                .body(errJson.toString())
                .build();
    }
}
