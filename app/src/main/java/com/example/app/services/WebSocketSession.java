package com.example.app.services;

import android.content.Context;
import android.webkit.WebView;

public class WebSocketSession {
    private final String socketId;
    private final String path;
    private final WebView webView;
    private final Context androidContext;

    public WebSocketSession(Context context, String socketId, String path, WebView webView) {
        this.androidContext = context.getApplicationContext(); // Safeguard memory context
        this.socketId = socketId;
        this.path = path;
        this.webView = webView;
    }

    public String getSocketId() { 
        return this.socketId; 
    }
    
    public String getPath() { 
        return this.path; 
    }
    
    public Context getContext() { 
        return this.androidContext; 
    }

    public void send(final String payload) {
        if (webView == null) return;
        
        webView.post(new Runnable() {
            @Override 
            public void run() {
                // Safeguard backslashes and single quote literals to block runtime parsing drops
                String safePayload = payload.replace("\\", "\\\\").replace("'", "\\'");
                
                // FIXED: Use Java formatting to evaluate the MESSAGE string template variable explicitly
                String jsCode = String.format(
                    com.example.app.services.WebScripts.WEBSOCKET_MESSAGE_FRAME_SCRIPT,
                    socketId,
                    safePayload
                );
                
                webView.evaluateJavascript(jsCode, null);
            }
        });
    }
}

