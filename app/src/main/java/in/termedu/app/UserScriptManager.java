package in.termedu.app;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

public class UserScriptManager {
    private Context context;
    private String mainURL;

    public UserScriptManager(Context context, String mainURL) {
        this.context = context;
        this.mainURL = mainURL;
    }

    public void injectScripts(WebView webview, String url) {
        // Inject push notification shim
        String pushShim = "javascript:(function() {" +
                "if (window.__webToApkPushShimLoaded) return;" +
                "window.__webToApkPushShimLoaded = true;" +
                "window.__shim_onNewEndpoint = function(subscriptionJson) {" +
                "  try {" +
                "    var sub = JSON.parse(subscriptionJson);" +
                "    if (window.__pushSubscriptionResolve) {" +
                "      window.__pushSubscriptionResolve(sub);" +
                "    }" +
                "  } catch(e) { console.error('Push shim error:', e); }" +
                "};" +
                "window.__runMediaAction = function(action) {" +
                "  var event = new CustomEvent('mediaaction', { detail: { action: action } });" +
                "  window.dispatchEvent(event);" +
                "};" +
                "console.log('WebToApk shim loaded');" +
                "})();";

        webview.post(() -> webview.evaluateJavascript(pushShim, null));
        Log.d("WebToApk", "Scripts injected for: " + url);
    }
}