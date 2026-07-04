//package com.example.app.services;
//public class WebScripts {
//    public static final String INTERCEPT_SCRIPT = 
//        "(function() {\n" +
//        "    if (window.HasAndroidFetchIntercepted) return;\n" +
//        "    window.HasAndroidFetchIntercepted = true;\n" +
//        "    \n" +
//        "    // --- 1. SAFE INTERCEPT NATIVE BROWSER FETCH API ---\n" +
//        "    const originalFetch = window.fetch;\n" +
//        "    window.fetch = function(input, init) {\n" +
//        "        let targetUrl = '';\n" +
//        "        let requestConfig = init || {};\n" +
//        "        \n" +
//        "        if (typeof input === 'string') {\n" +
//        "            targetUrl = input;\n" +
//        "        } else if (input instanceof URL) {\n" +
//        "            targetUrl = input.href;\n" +
//        "        } else if (input && typeof input.url === 'string') {\n" +
//        "            targetUrl = input.url;\n" +
//        "            if (!init) { requestConfig = input; }\n" +
//        "        }\n" +
//        "        \n" +
//        "        const method = (requestConfig.method || 'GET').toUpperCase();\n" +
//        "        const validMutations = ['POST', 'PUT', 'PATCH', 'DELETE'];\n" +
//        "        \n" +
//        "        // FIX: Expanded to match all protocol operations perfectly\n" +
//        "        if (requestConfig.body && validMutations.includes(method)) {\n" +
//        "            let payloadString = '';\n" +
//        "            if (typeof requestConfig.body === 'string') {\n" +
//        "                payloadString = requestConfig.body;\n" +
//        "            } else {\n" +
//        "                try { payloadString = JSON.stringify(requestConfig.body); } catch(e) { payloadString = requestConfig.body.toString(); }\n" +
//        "            }\n" +
//        "            \n" +
//        "            if (window.AndroidBridge && typeof window.AndroidBridge.captureRequestBody === 'function') {\n" +
//        "                window.AndroidBridge.captureRequestBody(method, targetUrl, payloadString);\n" +
//        "            }\n" +
//        "        }\n" +
//        "        return originalFetch.apply(this, arguments);\n" +
//        "    };\n" +
//        "    \n" +
//        "    // --- 2. SAFE INTERCEPT XMLHTTPREQUESTS (AJAX/AXIOS) ---\n" +
//        "    const originalOpen = XMLHttpRequest.prototype.open;\n" +
//        "    XMLHttpRequest.prototype.open = function(method, url) {\n" +
//        "        this._method = method ? method.toUpperCase() : 'GET';\n" +
//        "        this._url = url;\n" +
//        "        return originalOpen.apply(this, arguments);\n" +
//        "    };\n" +
//        "    \n" +
//        "    const originalSend = XMLHttpRequest.prototype.send;\n" +
//        "    XMLHttpRequest.prototype.send = function(body) {\n" +
//        "        const validMutations = ['POST', 'PUT', 'PATCH', 'DELETE'];\n" +
//        "        \n" +
//        "        // FIX: Expanded to capture all active AJAX layout payload streams\n" +
//        "        if (body && validMutations.includes(this._method) && typeof this._url === 'string') {\n" +
//        "            let payloadString = typeof body === 'string' ? body : JSON.stringify(body);\n" +
//        "            if (window.AndroidBridge && typeof window.AndroidBridge.captureRequestBody === 'function') {\n" +
//        "                window.AndroidBridge.captureRequestBody(this._method, this._url, payloadString);\n" +
//        "            }\n" +
//        "        }\n" +
//        "        return originalSend.apply(this, arguments);\n" +
//        "    };\n" +
//        "})();";
//}
package com.example.app.services;

public class WebScripts {
    public static final String INTERCEPT_SCRIPT = 
        "(function() {\n" +
        "    if (window.HasAndroidFetchIntercepted) return;\n" +
        "    window.HasAndroidFetchIntercepted = true;\n" +
        "    \n" +
        "    const originalFetch = window.fetch;\n" +
        "    window.fetch = function(input, init) {\n" +
        "        let targetUrl = '';\n" +
        "        let requestConfig = init || {};\n" +
        "        \n" +
        "        if (typeof input === 'string') {\n" +
        "            targetUrl = input;\n" +
        "        } else if (input instanceof URL) {\n" +
        "            targetUrl = input.href;\n" +
        "        } else if (input && typeof input.url === 'string') {\n" +
        "            targetUrl = input.url;\n" +
        "            if (!init) { requestConfig = input; }\n" +
        "        }\n" +
        "        \n" +
        "        const method = (requestConfig.method || 'GET').toUpperCase();\n" +
        "        const validMutations = ['POST', 'PUT', 'PATCH', 'DELETE'];\n" +
        "        \n" +
        "        if (requestConfig.body && validMutations.includes(method)) {\n" +
        "            // Telemetry Point 1: Calculate serialization cost inside JavaScript\n" +
        "            const startSerialization = performance.now();\n" +
        "            let payloadString = typeof requestConfig.body === 'string' \n" +
        "                ? requestConfig.body \n" +
        "                : JSON.stringify(requestConfig.body);\n" +
        "            const endSerialization = performance.now();\n" +
        "            \n" +
        "            if (window.AndroidBridge && typeof window.AndroidBridge.captureRequestBody === 'function') {\n" +
        "                const jsTimestampStr = String(Date.now());\n" +
        "                \n" +
        "                // Inject serialization stats into console logcat implicitly\n" +
        "                console.log('[TELEMETRY_JS] Route: ' + method + ':' + targetUrl + ' | Stringify Duration: ' + (endSerialization - startSerialization).toFixed(4) + 'ms');\n" +
        "                \n" +
        "                window.AndroidBridge.captureRequestBody(method, targetUrl, payloadString, jsTimestampStr);\n" +
        "            }\n" +
        "        }\n" +
        "        return originalFetch.apply(this, arguments);\n" +
        "    };\n" +
        "    \n" +
        "    const originalOpen = XMLHttpRequest.prototype.open;\n" +
        "    XMLHttpRequest.prototype.open = function(method, url) {\n" +
        "        this._method = method ? method.toUpperCase() : 'GET';\n" +
        "        this._url = url;\n" +
        "        return originalOpen.apply(this, arguments);\n" +
        "    };\n" +
        "    \n" +
        "    const originalSend = XMLHttpRequest.prototype.send;\n" +
        "    XMLHttpRequest.prototype.send = function(body) {\n" +
        "        const validMutations = ['POST', 'PUT', 'PATCH', 'DELETE'];\n" +
        "        if (body && validMutations.includes(this._method) && typeof this._url === 'string') {\n" +
        "            const startSerialization = performance.now();\n" +
        "            let payloadString = typeof body === 'string' ? body : JSON.stringify(body);\n" +
        "            const endSerialization = performance.now();\n" +
        "            \n" +
        "            if (window.AndroidBridge && typeof window.AndroidBridge.captureRequestBody === 'function') {\n" +
        "                console.log('[TELEMETRY_JS_AJAX] Route: ' + this._method + ':' + this._url + ' | Stringify Duration: ' + (endSerialization - startSerialization).toFixed(4) + 'ms');\n" +
        "                window.AndroidBridge.captureRequestBody(this._method, this._url, payloadString, String(Date.now()));\n" +
        "            }\n" +
        "        }\n" +
        "        return originalSend.apply(this, arguments);\n" +
        "    };\n" +
        "})();";
}
