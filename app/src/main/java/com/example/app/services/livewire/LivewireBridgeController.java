package com.example.app.services.livewire;

import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;
import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;
import com.example.app.services.WebSocketMapping;
import com.example.app.services.WebSocketOnClose;
import com.example.app.services.WebSocketOnOpen;
import com.example.app.services.WebSocketSession;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LivewireBridgeController {
    private static final String TAG = "JAVA_LivewireBridge";

    // Track active long-running laptop TCP background threads by target IP key
    private final ConcurrentHashMap<String, LivewireTunnelWorker> activeLaptopTunnels = new ConcurrentHashMap<>();
    
    // Track active monitoring WebSockets opened by the WebView front-end
    private final ConcurrentHashMap<String, WebSocketSession> activeEchoSessions = new ConcurrentHashMap<>();
    
    private final ExecutorService tunnelThreadPool = Executors.newCachedThreadPool();

    // Global reference hook if you choose not to unpack it from request loops
    public static WebView globalWebViewFallbackInstance = null;

    public LivewireBridgeController() {}

    // =========================================================================
    // 1. THE REST API ENGINE (MANAGEMENT LAYER)
    // =========================================================================

    @RequestMapping(path = "/api/livewire/connect", method = "POST")
    public ResponseContext initializeConnection(RequestContext request) {
        try {
            String target = request.getQueryParam("target");
            if (target == null || target.isEmpty()) {
                return buildJsonResponse(400, "error", "Missing 'target' parameter.");
            }

            if (activeLaptopTunnels.containsKey(target)) {
                return buildJsonResponse(200, "status", "Tunnel already active to this target device.");
            }

            // Target mapping fallback context extractor resolution step
            WebView webView = extractWebViewFromRequest(request); 
            if (webView == null) {
                return buildJsonResponse(500, "error", "Active running target WebView context could not be resolved.");
            }

            // Launch the background daemon thread to read from websocat on your laptop
            LivewireTunnelWorker worker = new LivewireTunnelWorker(target, webView);
            activeLaptopTunnels.put(target, worker);
            tunnelThreadPool.submit(worker);

            return buildJsonResponse(200, "success", "Background bridge thread successfully initialized to: " + target);
        } catch (Exception e) {
            Log.e(TAG, "Failed initializing REST livewire tunnel", e);
            return buildJsonResponse(500, "error", e.getMessage());
        }
    }

    @RequestMapping(path = "/api/livewire/connections", method = "GET")
    public ResponseContext listActiveConnections(RequestContext request) {
        try {
            JSONObject root = new JSONObject();
            root.put("status", "success");

            JSONArray tunnels = new JSONArray();
            for (String target : activeLaptopTunnels.keySet()) {
                tunnels.put(target);
            }
            root.put("active_laptop_tunnels", tunnels);

            JSONArray echoSessions = new JSONArray();
            for (String socketId : activeEchoSessions.keySet()) {
                echoSessions.put(socketId);
            }
            root.put("active_webview_echo_channels", echoSessions);

            return ResponseContext.status(200).contentType("application/json").body(root.toString()).build();
        } catch (Exception e) {
            return buildJsonResponse(500, "error", e.getMessage());
        }
    }

    @RequestMapping(path = "/api/livewire/disconnect", method = "POST")
    public ResponseContext destroyConnection(RequestContext request) {
        String target = request.getQueryParam("target");
        if (target == null || target.isEmpty()) {
            return buildJsonResponse(400, "error", "Missing target parameter.");
        }

        LivewireTunnelWorker worker = activeLaptopTunnels.remove(target);
        if (worker != null) {
            worker.terminate();
            broadcastToEchos("{\"event\":\"tunnel_destroyed\",\"target\":\"" + target + "\"}");
            return buildJsonResponse(200, "success", "Tunnel closed completely for: " + target);
        }

        return buildJsonResponse(404, "error", "No active running background tunnel discovered for key string matching: " + target);
    }
    // =========================================================================
    // 2. THE WEBSOCKET SERVICE (ECHO & EVENT MONITORING LAYER)
    // =========================================================================

    @WebSocketOnOpen(path = "/api/ws/livewire/echo")
    public void onEchoConnect(WebSocketSession session) {
        Log.i(TAG, "WebView connected to live monitoring link: " + session.getSocketId());
        activeEchoSessions.put(session.getSocketId(), session);
        
        try {
            JSONObject info = new JSONObject();
            info.put("type", "welcome");
            info.put("message", "Livewire Echo diagnostics stream active.");
            info.put("active_tunnels_count", activeLaptopTunnels.size());
            session.send(info.toString());
        } catch (Exception ignored) {}
    }

    @WebSocketMapping(path = "/api/ws/livewire/echo")
    public void handleEchoTraffic(WebSocketSession session, String message) {
        Log.d(TAG, "Echo processing frame payload from client interface: " + message);
        session.send("{\"type\":\"echo_response\",\"payload\":" + JSONObject.quote(message) + "}");
    }

    @WebSocketOnClose(path = "/api/ws/livewire/echo")
    public void onEchoDisconnect(WebSocketSession session) {
        Log.i(TAG, "WebView unlinked from live monitoring path: " + session.getSocketId());
        activeEchoSessions.remove(session.getSocketId());
    }

    // =========================================================================
    // 3. INTERNAL UTILITY HELPERS
    // =========================================================================

    private void broadcastToEchos(String jsonMessage) {
        for (WebSocketSession session : activeEchoSessions.values()) {
            try {
                session.send(jsonMessage);
            } catch (Exception ignored) {}
        }
    }

    private ResponseContext buildJsonResponse(int code, String status, String message) {
        JSONObject res = new JSONObject();
        try {
            res.put("status", status);
            res.put("message", message);
        } catch (Exception ignored) {}
        return ResponseContext.status(code).contentType("application/json").body(res.toString()).build();
    }

    private WebView extractWebViewFromRequest(RequestContext request) {
        // If your framework doesn't bundle a WebView inside RequestContext directly, 
        // fall back to using the global static registration reference hook.
        return globalWebViewFallbackInstance; 
    }
    /**
     * Dedicated line reader tracking streaming traffic allocations from dev laptop.
     */
    private class LivewireTunnelWorker implements Runnable {
        private final String targetUrl;
        private final WebView webViewRef;
        private volatile boolean running = true;
        private java.net.Socket rawSocket = null;

        public LivewireTunnelWorker(String targetUrl, WebView webViewRef) {
            this.targetUrl = targetUrl;
            this.webViewRef = webViewRef;
        }

        @Override
        public void run() {
            try {
                String cleanTarget = targetUrl.replace("ws://", "").replace("wss://", "");
                String[] parts = cleanTarget.split(":");
                String ip = parts[0];
                int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 8080;

                Log.d(TAG, "[Tunnel Worker] Launching background TCP link to -> " + ip + ":" + port);
                rawSocket = new java.net.Socket(ip, port);
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(rawSocket.getInputStream()));
                broadcastToEchos("{\"event\":\"tunnel_connected\",\"target\":\"" + targetUrl + "\"}");

                String incomingPayload;
                while (running && (incomingPayload = reader.readLine()) != null) {
                    final String codeToExecute = incomingPayload;
                    Log.v(TAG, "[Tunnel Worker] Inbound compilation command streaming in: " + codeToExecute);

                    broadcastToEchos("{\"event\":\"executing_payload\",\"bytes\":" + codeToExecute.length() + "}");

                    if (webViewRef != null) {
                        webViewRef.post(new Runnable() {
                            @Override
                            public void run() {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                    webViewRef.evaluateJavascript(codeToExecute, value -> {
                                        broadcastToEchos("{\"event\":\"execution_completed\",\"result\":" + JSONObject.quote(value) + "}");
                                    });
                                } else {
                                    webViewRef.loadUrl("javascript:" + codeToExecute);
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing tunnel background streaming thread runtime", e);
                broadcastToEchos("{\"event\":\"tunnel_error\",\"message\":" + JSONObject.quote(e.getMessage()) + "}");
                terminate();
            }
        }

        public void terminate() {
            running = false;
            try {
                if (rawSocket != null && !rawSocket.isClosed()) {
                    rawSocket.close();
                }
            } catch (Exception ignored) {}
            activeLaptopTunnels.remove(targetUrl);
            Log.d(TAG, "[Tunnel Worker] Background network streaming thread closed down cleanly.");
        }
    }
}

