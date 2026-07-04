package com.example.app.services.example;

import android.util.Log;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ExampleController {
    private static final String TAG = "ExampleController";

    public ExampleController() {}

    // 1. GET METHOD Validation: Processes Query Parameters and responds with Custom Headers
    @RequestMapping(path = "/api/example/get-test", method = "GET")
    public ResponseContext testGetMethod(RequestContext request) {
        try {
            String trackingId = request.getQueryParam("tracking_id");
            String filterType = request.getQueryParam("filter");

            JSONObject result = new JSONObject();
            result.put("status", "processed");
            result.put("received_tracking_id", trackingId != null ? trackingId : "none");
            result.put("received_filter", filterType != null ? filterType : "none");

            return ResponseContext.status(200)
                    .contentType("application/json")
                    .header("X-Server-Response-Engine", "Android-Native-JVM")
                    .header("X-Echo-Tracking-ID", trackingId != null ? trackingId : "none")
                    .body(result.toString())
                    .build();
        } catch (Exception e) {
            return buildErrorResponse(500, e.getMessage());
        }
    }

    // 2. POST / PUT / PATCH / DELETE Matrix Endpoint: Extracted Body, Header and Status Controls
    @RequestMapping(path = "/api/example/mutation-test", method = "POST")
    public ResponseContext testPostMethod(RequestContext request) { return handleMutation(request); }

    @RequestMapping(path = "/api/example/mutation-test", method = "PUT")
    public ResponseContext testPutMethod(RequestContext request) { return handleMutation(request); }

    @RequestMapping(path = "/api/example/mutation-test", method = "PATCH")
    public ResponseContext testPatchMethod(RequestContext request) { return handleMutation(request); }

    @RequestMapping(path = "/api/example/mutation-test", method = "DELETE")
    public ResponseContext testDeleteMethod(RequestContext request) { return handleMutation(request); }

    private ResponseContext handleMutation(RequestContext request) {
        try {
            String activeMethod = request.getMethod().toUpperCase();
            
            // Extract a user-agent header and a custom authentication token header tracking attribute
            String userAgent = request.getHeader("User-Agent");
            String clientProfile = request.getHeader("X-Client-Profile");

            // Extract request body payload parameters cleanly
            byte[] bodyBytes = request.getBody();
            String rawBodyText = (bodyBytes != null && bodyBytes.length > 0) ? new String(bodyBytes, StandardCharsets.UTF_8) : "{}";
            JSONObject bodyJson = new JSONObject(rawBodyText);

            // Read dynamic instructions straight out of the request payload configuration
            int targetStatusCode = bodyJson.optInt("requested_status_code", 200);
            String customResponseEchoMsg = bodyJson.optString("message_payload", "Default Echo response");

            JSONObject result = new JSONObject();
            result.put("echo_method", activeMethod);
            result.put("echo_message", customResponseEchoMsg);
            result.put("detected_user_agent", userAgent != null ? userAgent : "unknown");
            result.put("detected_profile_header", clientProfile != null ? clientProfile : "none");
            result.put("payload_integrity_check", true);

            return ResponseContext.status(targetStatusCode)
                    .contentType("application/json")
                    .header("X-Processed-By-Method", activeMethod)
                    .header("Cache-Control", "no-store, max-age=0")
                    .body(result.toString())
                    .build();

        } catch (Exception e) {
            return buildErrorResponse(400, "Mutation runtime parsing violation: " + e.getMessage());
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

