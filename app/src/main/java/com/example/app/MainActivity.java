package com.example.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.content.pm.PackageManager;

public class MainActivity extends Activity implements SecretTriggerDetector.OnTriggerListener {
    private static final String TAG = "JAVA_MainActivity";

    private static final int MICROPHONE_PERMISSION_REQUEST_CODE = 200;

    private WebView mWebView;
    private WebView mMaintenanceWebView; // Dedicated instance
    
    private AppConfig mConfig;
    private PermissionManager mPermissionManager;
    private StorageManager mStorageManager;
    private SecretTriggerDetector mSecretDetector;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConfig = new AppConfig(this);
        mPermissionManager = new PermissionManager(this);
        mStorageManager = new StorageManager(this, mConfig);
        mSecretDetector = new SecretTriggerDetector(this);

        if (mConfig.getVirtualHost().isEmpty()) return;

        mStorageManager.createPublicWorkspaceDirectory();

        // 1. Initialize Primary Webview
        mWebView = findViewById(R.id.activity_main_webview);
        configureWebViewSettings(mWebView);
        mWebView.setWebViewClient(new MyWebViewClient(this, mConfig));

        // 2. Initialize Maintenance Webview
        mMaintenanceWebView = findViewById(R.id.activity_maintenance_webview);
        configureWebViewSettings(mMaintenanceWebView);
	mMaintenanceWebView.setWebViewClient(new ConfigWebViewClient(this, mConfig));

        // Shared Chrome client for permission requests
        WebChromeClient chromeClient = new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        };
        mWebView.setWebChromeClient(chromeClient);
        mMaintenanceWebView.setWebChromeClient(chromeClient);

	// add logging
        WebChromeClient unifiedChromeClient = new WebChromeClient() {// Create a unified WebChromeClient to route web developer logs to ADB
//            @Override
//            public void onPermissionRequest(final PermissionRequest request) {
//                runOnUiThread(() -> request.grant(request.getResources()));
//            }
    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        // Drop loop chains and force-grant all requested resources on the spot
        if (request != null) {
            request.grant(request.getResources());
        }
    }
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                // Format a clean, informative console log message payload
                String formattedMessage = String.format("[%s] Line %d of %s: %s",
                        consoleMessage.messageLevel().name(),
                        consoleMessage.lineNumber(),
                        consoleMessage.sourceId(),
                        consoleMessage.message()
                );

                // Print out to standard Logcat using your exact requested tag
                android.util.Log.d("JS_CONSOLE", formattedMessage);
                return true; // Prevents the system from printing the default WebView message format
            }
        };
        mWebView.setWebChromeClient(unifiedChromeClient);
        mMaintenanceWebView.setWebChromeClient(unifiedChromeClient);


        // Load Main Content Entry Point
        String startupPath = mStorageManager.determineStartupPath();
        mWebView.loadUrl(mConfig.getVirtualHost() + startupPath);

checkAndRequestMicrophonePermission();

        // Standard System Platform Permission Checks
        if (!mPermissionManager.hasStandardPermissions()) {
            mPermissionManager.requestStandardPermissions();
        }
        if (mPermissionManager.needsStorageManagerAccess()) {
            mPermissionManager.requestStorageManagerPermission();
        }
    }

    // Helper method added to handle native Android runtime microphone prompts
    private void checkAndRequestMicrophonePermission() {
        Log.i(TAG, "checkAndRequestMicrophonePermission()");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.i(TAG, "checkAndRequestMicrophonePermission():Build.VERSION.SDK_INT >= Build.VERSION_CODES.M");
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "checkAndRequestMicrophonePermission():checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED");
                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_REQUEST_CODE);
            }else{
                Log.i(TAG, "checkAndRequestMicrophonePermission():!checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED");
            }
        }else{
            Log.i(TAG, "checkAndRequestMicrophonePermission():!Build.VERSION.SDK_INT >= Build.VERSION_CODES.M");
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebViewSettings(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setDomStorageEnabled(true);

    
    // ADD THESE TWO LINES HERE:
    settings.setAllowContentAccess(true); // Grants access to content providers
    settings.setAllowFileAccess(true);    // Ensures internal mapped files read correctly
    

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowFileAccessFromFileURLs(true);
        }
    }

    public AppConfig getAppConfig() {
        return mConfig;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionManager.STORAGE_MANAGEMENT_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !mPermissionManager.needsStorageManagerAccess()) {
                Log.i(TAG, "Storage Permission Granted!");
                mStorageManager.createPublicWorkspaceDirectory();
                mWebView.clearCache(true);
                String startupPath = mStorageManager.determineStartupPath();
                mWebView.loadUrl(mConfig.getVirtualHost() + startupPath);
            }
        }
    }


//    // Callback catching the user response to the microphone authorization prompt
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == MICROPHONE_PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Log.i(TAG, "Native microphone access granted by user.");
//            } else {
//                Log.w(TAG, "Native microphone access denied by user.");
//            }
//        }
//    }
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            Log.i(TAG, "onRequestPermissionsResult()");
	    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	    if (requestCode == MICROPHONE_PERMISSION_REQUEST_CODE) {
                Log.i(TAG, "onRequestPermissionsResult():requestCode == MICROPHONE_PERMISSION_REQUEST_CODE");
		// Fix: Access index 0 of the results array to read the actual integer value
		if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "onRequestPermissionsResult():grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED");
		    Log.i(TAG, "Native microphone access granted by user.");
		    // Optional: Reload your web view here if the web app requires a refresh to initialize audio context
		} else {
                    Log.i(TAG, "onRequestPermissionsResult():!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)");
		    Log.w(TAG, "Native microphone access denied by user.");
		}
	    }else{
                Log.i(TAG, "onRequestPermissionsResult():!(requestCode == MICROPHONE_PERMISSION_REQUEST_CODE)");
            }
	}

    /**
     * Secret Sequence Toggle Switch Rule Action
     */
    @Override
    public void onSecretTriggered() {
        if (mMaintenanceWebView == null || mConfig == null) return;

        // If the maintenance layer is visible, toggle it off (close it)
        if (mMaintenanceWebView.getVisibility() == View.VISIBLE) {
            closeMaintenanceView();
        } else {
            // Otherwise, target, load and reveal the maintenance screen layer
            mMaintenanceWebView.loadUrl(mConfig.getVirtualHost() + "/maintenance/index.html");
            mMaintenanceWebView.setVisibility(View.VISIBLE);
            mMaintenanceWebView.requestFocus();
            Log.i(TAG, "Maintenance WebView Displayed.");
        }
    }

    private void closeMaintenanceView() {
        if (mMaintenanceWebView != null) {
            mMaintenanceWebView.setVisibility(View.GONE);
            mMaintenanceWebView.loadUrl("about:blank"); // Free up runtime resources
            mWebView.requestFocus(); // Restore touch and focus control to the main core app
            Log.i(TAG, "Maintenance WebView Closed.");
        }
    }

    /**
     * Hardware Device Back Button Interceptor Navigation
     */
    @Override
    public void onBackPressed() {
        // Rule: If maintenance screen is open, close it instantly instead of popping activity stack
        if (mMaintenanceWebView != null && mMaintenanceWebView.getVisibility() == View.VISIBLE) {
            closeMaintenanceView();
        } else if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (mSecretDetector.handleKeyDown(keyCode, event)) {
//            return true;
//        }
//        return super.onKeyDown(keyCode, event);
//    }
//Sat Jun 27 13:07:57 SAST 2026 - try to fix on dominiques phone the secret keys
@Override
public boolean dispatchKeyEvent(KeyEvent event) {
    if (mSecretDetector != null) {
        int keyCode = event.getKeyCode();
        
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (mSecretDetector.handleKeyDown(keyCode, event)) {
                return true; // Stop the event from reaching the WebView
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            if (mSecretDetector.handleKeyUp(keyCode, event)) {
                return true; // Stop the event from reaching the WebView
            }
        }
    }
    return super.dispatchKeyEvent(event);
}

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mSecretDetector.handleKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Clears internal UI memory caches and forces the primary workspace view back to its updated root index.html
     */
    public void reloadPrimaryWebViewToRoot() {
        Log.i("JAVA_MainActivity", "reloadPrimaryWebViewToRoot() invoked. Refreshing production view canvas...");
        try {
            if (mWebView != null && mStorageManager != null && mConfig != null) {
                // 1. Purge old asset file references from the browser engine cache
                mWebView.clearCache(true);
                
                // 2. Re-calculate the active startup folder node index path location pointer
                String freshStartupPath = mStorageManager.determineStartupPath();
                String targetUrl = mConfig.getVirtualHost() + freshStartupPath;
                
                Log.d("JAVA_MainActivity", " -> Loading freshly synchronized content path: " + targetUrl);
                
                // 3. Force page viewport load execution
                mWebView.loadUrl(targetUrl);
                Log.i("JAVA_MainActivity", " -> Primary app workspace successfully refreshed to root level layout node.");
            }
        } catch (Exception e) {
            Log.e("JAVA_MainActivity", "Failed to force core viewport refresh: " + e.getMessage());
        }
    }

}

