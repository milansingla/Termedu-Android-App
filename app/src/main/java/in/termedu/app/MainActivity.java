package in.termedu.app;

import android.content.DialogInterface;
import android.net.http.SslError;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.os.Bundle;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.os.Handler;
import android.webkit.WebSettings;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;
import android.webkit.JsResult;
import android.webkit.JsPromptResult;
import android.widget.FrameLayout;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.graphics.Color;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebResourceError;
import androidx.annotation.Nullable;
import android.webkit.JavascriptInterface;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.os.Looper;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.TextView;
import android.app.Activity;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient.FileChooserParams;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.app.DownloadManager;
import android.webkit.URLUtil;
import android.os.Environment;
import static android.content.Context.DOWNLOAD_SERVICE;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import org.unifiedpush.android.connector.UnifiedPush;
import static org.unifiedpush.android.connector.ConstantsKt.INSTANCE_DEFAULT;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import org.json.JSONException;
import org.json.JSONObject;
import androidx.annotation.NonNull;
import android.media.AudioManager;
import androidx.activity.OnBackPressedCallback;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

// FCM imports
import com.google.firebase.messaging.FirebaseMessaging;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "TERMEdu";

    private static final int LOCATION_PERMISSION_REQUEST_CODE    = 1;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE      = 3;
    private static final int MICROPHONE_PERMISSION_REQUEST_CODE  = 4;
    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE  = 5;

    private static final String NOTIFICATION_CHANNEL_ID   = "fcm_high_importance";
    private static final String NOTIFICATION_CHANNEL_NAME = "Push Notifications";

    static final String BASE_URL = "https://app.termedu.in";

    private WebView webview;
    private SwipeRefreshLayout swipeRefreshLayout;
    private UserScriptManager userScriptManager;
    private ProgressBar spinner;
    private Animation fadeInAnimation;
    private View mainLayout;
    private View errorLayout;
    private ViewGroup parentLayout;
    private boolean errorOccurred = false;
    private ValueCallback<Uri[]> mFilePathCallback;
    private ActivityResultLauncher<Intent> fileChooserLauncher;
    private WebAppInterface webAppInterface;
    private BroadcastReceiver unifiedPushEndpointReceiver;
    private BroadcastReceiver mediaActionReceiver;
    private boolean doubleBackToExitPressedOnce = false;
    private PermissionRequest pendingWebPermissionRequest;
    private AudioManager audioManager;

    String mainURL = BASE_URL;
    boolean requireDoubleBackToExit      = true;
    boolean allowSubdomains              = true;
    boolean enableExternalLinks          = true;
    boolean openExternalLinksInBrowser   = true;
    boolean confirmOpenInBrowser         = true;
    boolean allowOpenMobileApp           = false;
    boolean confirmOpenExternalApp       = true;
    boolean enableMicrophone             = true;
    boolean enableCamera                 = true;
    boolean isolateAudioSession          = false;
    boolean forceVideoFullscreenRotation = true;
    String  cookies                      = "";
    String  basicAuth                    = "";
    String  userAgent                    = "";
    boolean blockLocalhostRequests       = false;
    boolean JSEnabled                    = true;
    boolean JSCanOpenWindowsAutomatically = true;
    boolean DomStorageEnabled            = true;
    boolean DatabaseEnabled              = true;
    boolean MediaPlaybackRequiresUserGesture = false;
    boolean AllowFileAccess              = true;
    boolean AllowFileAccessFromFileURLs  = true;
    boolean showDetailsOnErrorScreen     = false;
    boolean forceLandscapeMode           = false;
    boolean edgeToEdge                   = false;
    boolean forceDarkTheme               = false;
    boolean DebugWebView                 = false;
    boolean geolocationEnabled           = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (forceDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        getWindow().setStatusBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("TERM Edu push notifications");
            channel.enableVibration(true);
            channel.setShowBadge(true);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        if (forceLandscapeMode) {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        setContentView(R.layout.activity_main);
        mainLayout   = findViewById(android.R.id.content);
        parentLayout = (ViewGroup) mainLayout.getParent();
        userScriptManager = new UserScriptManager(this, mainURL);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        handleFcmIntentIfPresent(getIntent());

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri    data   = intent.getData();
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            mainURL = data.toString();
        }

        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        webview = findViewById(R.id.webView);
        spinner = findViewById(R.id.progressBar1);

        // ── SwipeRefreshLayout setup ─────────────────────────────────────────
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright);
        swipeRefreshLayout.setOnRefreshListener(() -> webview.reload());
        webview.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                swipeRefreshLayout.setEnabled(scrollY == 0));

        webview.setWebViewClient(new CustomWebViewClient());
        webview.setWebChromeClient(new CustomWebChrome());
        webAppInterface = new WebAppInterface(this);
        webview.addJavascriptInterface(webAppInterface, "WebToApk");

        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(JSEnabled);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(JSCanOpenWindowsAutomatically);
        webSettings.setGeolocationEnabled(geolocationEnabled);
        webSettings.setDomStorageEnabled(DomStorageEnabled);
        webSettings.setDatabaseEnabled(DatabaseEnabled);
        webSettings.setMediaPlaybackRequiresUserGesture(MediaPlaybackRequiresUserGesture);
        webSettings.setAllowFileAccess(AllowFileAccess);
        webSettings.setAllowFileAccessFromFileURLs(AllowFileAccessFromFileURLs);
        WebView.setWebContentsDebuggingEnabled(DebugWebView);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // ── FIX: Force mobile/portrait viewport so exam screen renders correctly ──
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        // ── FIX 2: Strip "; wv" from UA so YouTube serves the full custom player ──
        // The "; wv" token is the WebView marker YouTube uses to detect an embedded
        // WebView and fall back to its stripped native embed UI, hiding your custom
        // quality controls, watermark, and branding. Removing it makes YouTube treat
        // this WebView as a regular Chrome browser.
        if (!userAgent.isEmpty()) {
            webSettings.setUserAgentString(userAgent);
        } else {
            String defaultUA = webSettings.getUserAgentString();
            String fixedUA = defaultUA.replace("; wv", "");
            webSettings.setUserAgentString(fixedUA);
            Log.d(TAG, "User-Agent set to: " + fixedUA);
        }

        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webview.setOverScrollMode(WebView.OVER_SCROLL_NEVER);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webview, true);
        cookieManager.setCookie(mainURL, cookies);
        cookieManager.flush();

        requestRequiredPermissions();
        initFcmToken();

        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Uri[] results = null;
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent intentData = result.getData();
                        if (intentData.getClipData() != null) {
                            int count = intentData.getClipData().getItemCount();
                            results = new Uri[count];
                            for (int i = 0; i < count; i++) {
                                results[i] = intentData.getClipData().getItemAt(i).getUri();
                            }
                        } else if (intentData.getData() != null) {
                            results = new Uri[]{intentData.getData()};
                        }
                    }
                    if (mFilePathCallback != null) {
                        mFilePathCallback.onReceiveValue(results);
                        mFilePathCallback = null;
                    }
                }
        );

        webview.setDownloadListener((url, userAgent1, contentDisposition, mimetype, contentLength) -> {
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype);
            String cookie = CookieManager.getInstance().getCookie(url);
            request.addRequestHeader("cookie", cookie);
            request.addRequestHeader("User-Agent", userAgent1);
            request.setDescription(getString(R.string.download_description));
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimetype));
            try {
                downloadManager.enqueue(request);
                Toast.makeText(getApplicationContext(),
                        R.string.download_started, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(),
                        R.string.download_failed, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to start download", e);
            }
        });

        unifiedPushEndpointReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String endpoint = intent.getStringExtra("endpoint");
                String p256dh   = intent.getStringExtra("p256dh");
                String auth     = intent.getStringExtra("auth");
                Log.d(TAG, "UnifiedPush endpoint: " + endpoint);
                if (endpoint != null && p256dh != null && auth != null && webview != null) {
                    try {
                        JSONObject keys = new JSONObject();
                        keys.put("p256dh", p256dh);
                        keys.put("auth", auth);
                        JSONObject subscription = new JSONObject();
                        subscription.put("endpoint", endpoint);
                        subscription.put("expirationTime", JSONObject.NULL);
                        subscription.put("keys", keys);
                        String subscriptionJson = subscription.toString();
                        webview.post(() -> {
                            String js = "if (typeof window.__shim_onNewEndpoint === 'function') "
                                    + "{ window.__shim_onNewEndpoint('"
                                    + subscriptionJson.replace("'", "\\'") + "'); }";
                            webview.evaluateJavascript(js, null);
                        });
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to create subscription JSON", e);
                    }
                }
            }
        };
        ContextCompat.registerReceiver(
                this,
                unifiedPushEndpointReceiver,
                new IntentFilter("com.termedu.webtoapk.NEW_ENDPOINT"),
                ContextCompat.RECEIVER_NOT_EXPORTED);

        if (savedInstanceState != null) {
            webview.restoreState(savedInstanceState);
        } else {
            webview.loadUrl(mainURL);
        }

        mediaActionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null
                        && MediaPlaybackService.BROADCAST_MEDIA_ACTION.equals(intent.getAction())) {
                    String mediaAction =
                            intent.getStringExtra(MediaPlaybackService.EXTRA_MEDIA_ACTION);
                    if (mediaAction != null) executeMediaActionInWebView(mediaAction);
                }
            }
        };
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
                .registerReceiver(mediaActionReceiver,
                        new IntentFilter(MediaPlaybackService.BROADCAST_MEDIA_ACTION));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webview.canGoBack()) {
                    webview.goBack();
                } else if (requireDoubleBackToExit) {
                    if (doubleBackToExitPressedOnce) {
                        finish();
                    } else {
                        doubleBackToExitPressedOnce = true;
                        Toast.makeText(MainActivity.this,
                                R.string.exit_app, Toast.LENGTH_SHORT).show();
                        new Handler(Looper.getMainLooper()).postDelayed(
                                () -> doubleBackToExitPressedOnce = false, 2000);
                    }
                } else {
                    finish();
                }
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FCM
    // ═══════════════════════════════════════════════════════════════════════

    private void initFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "FCM token: " + token);
                    getSharedPreferences("fcm", MODE_PRIVATE)
                            .edit().putString("token", token).apply();
                    sendFcmTokenToServer(token);
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "FCM token fetch failed", e));
    }

    static void sendFcmTokenToServer(Context context, String token) {
        SharedPreferences authPrefs =
                context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String userId = authPrefs.getString("userId", "");

        String url = BASE_URL + "/api/fcm/save-token";
        try {
            JSONObject body = new JSONObject();
            body.put("token", token);
            body.put("userId", userId);

            RequestQueue queue = Volley.newRequestQueue(context);
            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST, url, body,
                    response -> Log.d("TERMEdu", "FCM token saved: " + response),
                    error   -> Log.e("TERMEdu", "FCM token send failed: " + error));
            queue.add(req);
        } catch (JSONException e) {
            Log.e("TERMEdu", "FCM JSON error", e);
        }
    }

    private void sendFcmTokenToServer(String token) {
        sendFcmTokenToServer(this, token);
    }

    private void handleFcmIntentIfPresent(Intent intent) {
        if (intent == null) return;
        String fcmType = intent.getStringExtra("fcm_type");
        if ("support".equals(fcmType)) {
            String supportUrl = BASE_URL + "/support?fcm=support";
            if (webview != null) {
                webview.loadUrl(supportUrl);
            } else {
                mainURL = supportUrl;
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleFcmIntentIfPresent(intent);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Permissions
    // ═══════════════════════════════════════════════════════════════════════

    private void requestRequiredPermissions() {
        java.util.List<String> permissionsToRequest = new java.util.ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (enableCamera && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }
        if (enableMicrophone
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (pendingWebPermissionRequest != null) {
            boolean cameraGranted = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            boolean micGranted = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
            java.util.List<String> grantedResources = new java.util.ArrayList<>();
            if (cameraGranted) grantedResources.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
            if (micGranted)    grantedResources.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE);
            if (!grantedResources.isEmpty()) {
                pendingWebPermissionRequest.grant(grantedResources.toArray(new String[0]));
            } else {
                pendingWebPermissionRequest.deny();
            }
            pendingWebPermissionRequest = null;
        }

        for (int i = 0; i < permissions.length; i++) {
            Log.d(TAG, (grantResults[i] == PackageManager.PERMISSION_GRANTED
                    ? "Granted: " : "Denied: ") + permissions[i]);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private void registerForUnifiedPush(final String vapidPublicKey) {
        if (vapidPublicKey == null || vapidPublicKey.isEmpty()) return;
        try {
            UnifiedPush.registerApp(MainActivity.this, INSTANCE_DEFAULT,
                    new java.util.ArrayList<>(), vapidPublicKey);
        } catch (Exception e) {
            Log.e(TAG, "UnifiedPush registration failed: " + e.getMessage());
            new Handler(Looper.getMainLooper()).post(() ->
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.push_distributor_required_title)
                            .setMessage(R.string.push_distributor_required_message)
                            .setPositiveButton(R.string.learn_more, (d, w) ->
                                    startActivity(new Intent(Intent.ACTION_VIEW,
                                            Uri.parse("https://unifiedpush.org/users/distributors/"))))
                            .setNegativeButton(android.R.string.cancel, null)
                            .show());
        }
    }

    private void executeMediaActionInWebView(String action) {
        if (webview != null) {
            webview.post(() -> {
                String js = "if (typeof window.__runMediaAction === 'function') "
                        + "{ window.__runMediaAction('" + action + "'); }";
                webview.evaluateJavascript(js, null);
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unifiedPushEndpointReceiver != null) unregisterReceiver(unifiedPushEndpointReceiver);
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mediaActionReceiver);
        stopService(new Intent(this, MediaPlaybackService.class));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webview.saveState(outState);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CustomWebChrome
    // ═══════════════════════════════════════════════════════════════════════
    private class CustomWebChrome extends WebChromeClient {

        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            Log.d(TAG, "WebView permission: "
                    + java.util.Arrays.toString(request.getResources()));
            boolean needsCamera = false, needsMic = false;
            for (String r : request.getResources()) {
                if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) needsCamera = true;
                if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)) needsMic    = true;
            }
            boolean cameraOk = !needsCamera || ContextCompat.checkSelfPermission(
                    MainActivity.this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED;
            boolean micOk = !needsMic || ContextCompat.checkSelfPermission(
                    MainActivity.this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;

            if (cameraOk && micOk) {
                request.grant(request.getResources());
            } else {
                pendingWebPermissionRequest = request;
                java.util.List<String> toAsk = new java.util.ArrayList<>();
                if (!cameraOk) toAsk.add(Manifest.permission.CAMERA);
                if (!micOk)    toAsk.add(Manifest.permission.RECORD_AUDIO);
                ActivityCompat.requestPermissions(MainActivity.this,
                        toAsk.toArray(new String[0]), CAMERA_MIC_PERMISSION_REQUEST_CODE);
            }
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage m) {
            switch (m.messageLevel()) {
                case ERROR:   Log.e(TAG, m.sourceId() + ":" + m.lineNumber() + " " + m.message()); break;
                case WARNING: Log.w(TAG, m.sourceId() + ":" + m.lineNumber() + " " + m.message()); break;
                default:      Log.d(TAG, m.sourceId() + ":" + m.lineNumber() + " " + m.message()); break;
            }
            return true;
        }

        @Override
        public boolean onJsAlert(WebView v, String url, String msg,
                                 final android.webkit.JsResult r) {
            new AlertDialog.Builder(MainActivity.this).setMessage(msg)
                    .setPositiveButton(android.R.string.ok, (d, w) -> r.confirm())
                    .setCancelable(false).create().show();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView v, String url, String msg, final JsResult r) {
            new AlertDialog.Builder(MainActivity.this).setMessage(msg)
                    .setPositiveButton(android.R.string.ok,     (d, w) -> r.confirm())
                    .setNegativeButton(android.R.string.cancel, (d, w) -> r.cancel())
                    .setCancelable(false).create().show();
            return true;
        }

        @Override
        public boolean onJsPrompt(WebView v, String url, String msg,
                                  String def, final JsPromptResult r) {
            EditText input = new EditText(MainActivity.this);
            input.setText(def);
            new AlertDialog.Builder(MainActivity.this).setMessage(msg).setView(input)
                    .setPositiveButton(android.R.string.ok,
                            (d, w) -> r.confirm(input.getText().toString()))
                    .setNegativeButton(android.R.string.cancel, (d, w) -> r.cancel())
                    .setCancelable(false).create().show();
            return true;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin,
                                                       GeolocationPermissions.Callback cb) {
            cb.invoke(origin, true, false);
        }

        private View mCustomView;
        private CustomViewCallback mCustomViewCallback;
        private int mOriginalOrientation, mOriginalSystemUiVisibility;

        @Override
        public void onHideCustomView() {
            ((FrameLayout) getWindow().getDecorView()).removeView(mCustomView);
            mCustomView = null;
            getWindow().getDecorView().setSystemUiVisibility(mOriginalSystemUiVisibility);
            if (forceVideoFullscreenRotation) setRequestedOrientation(mOriginalOrientation);
            mCustomViewCallback.onCustomViewHidden();
            mCustomViewCallback = null;
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (mCustomView != null) { onHideCustomView(); return; }
            mCustomView = view;
            mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            mOriginalOrientation        = getRequestedOrientation();
            mCustomViewCallback         = callback;
            // Only force landscape for real video players, not exam/UI fullscreen requests
            if (forceVideoFullscreenRotation && containsVideoSurface(view))
                setRequestedOrientation(
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            ((FrameLayout) getWindow().getDecorView()).addView(mCustomView,
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        /**
         * Returns true if the view hierarchy contains a VideoView or SurfaceView
         * (real video player), not an exam/UI fullscreen request.
         */
        private boolean containsVideoSurface(View view) {
            if (view instanceof android.widget.VideoView
                    || view instanceof android.view.SurfaceView) {
                return true;
            }
            if (view instanceof android.view.ViewGroup) {
                android.view.ViewGroup group = (android.view.ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    if (containsVideoSurface(group.getChildAt(i))) return true;
                }
            }
            return false;
        }

        @Override
        public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> cb,
                                         FileChooserParams params) {
            if (mFilePathCallback != null) mFilePathCallback.onReceiveValue(null);
            mFilePathCallback = cb;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            String[] types = params.getAcceptTypes();
            if (types.length > 0 && types[0] != null && !types[0].isEmpty()) {
                if      (types[0].contains("video")) i.setType("video/*");
                else if (types[0].contains("audio")) i.setType("audio/*");
                else if (!types[0].contains("image")) i.setType("*/*");
            }
            if (params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE)
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            try {
                fileChooserLauncher.launch(Intent.createChooser(i, "Select file"));
            } catch (ActivityNotFoundException e) {
                mFilePathCallback = null;
                Toast.makeText(MainActivity.this,
                        "Unable to open file manager", Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CustomWebViewClient
    // ═══════════════════════════════════════════════════════════════════════
    private class CustomWebViewClient extends WebViewClient {

        @Override
        public void onReceivedSslError(WebView v, SslErrorHandler h,
                                       android.net.http.SslError e) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(R.string.notification_error_ssl_cert_invalid)
                    .setPositiveButton("continue", (d, w) -> h.proceed())
                    .setNegativeButton("cancel",   (d, w) -> h.cancel())
                    .create().show();
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView v,
                                              android.webkit.HttpAuthHandler h,
                                              String host, String realm) {
            if (basicAuth != null && !basicAuth.isEmpty()) {
                String[] parts = basicAuth.split(":", 2);
                if (parts.length == 2) {
                    String domain = "";
                    try { domain = Uri.parse(mainURL).getHost(); }
                    catch (Exception ex) { ex.printStackTrace(); }
                    boolean ok = allowSubdomains
                            ? host.endsWith(domain) || domain.endsWith(host)
                            : host.equals(domain);
                    if (ok) { h.proceed(parts[0], parts[1]); return; }
                }
            }
            View dv = getLayoutInflater().inflate(R.layout.auth_dialog, null);
            EditText u = dv.findViewById(R.id.username);
            EditText p = dv.findViewById(R.id.password);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Authentication Required").setView(dv)
                    .setPositiveButton("OK",     (d, w) -> h.proceed(
                            u.getText().toString(), p.getText().toString()))
                    .setNegativeButton("Cancel", (d, w) -> h.cancel()).show();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
            String url = req.getUrl().toString();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                if (allowOpenMobileApp) {
                    Runnable open = () -> {
                        try { v.getContext().startActivity(
                                new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                        catch (ActivityNotFoundException e) {
                            Log.e(TAG, "No app for: " + url); }
                    };
                    if (confirmOpenExternalApp) {
                        new AlertDialog.Builder(v.getContext())
                                .setTitle(R.string.external_link)
                                .setMessage(R.string.open_in_external_app)
                                .setPositiveButton(android.R.string.yes, (d, w) -> open.run())
                                .setNegativeButton(android.R.string.no, null).show();
                    } else open.run();
                }
                return true;
            }
            String ud = req.getUrl().getHost();
            String md = Uri.parse(mainURL).getHost();
            if (ud == null || md == null) return handleExternalLink(url, v);
            boolean internal = allowSubdomains
                    ? ud.endsWith(md) || md.endsWith(ud) : ud.equals(md);
            return !internal && handleExternalLink(url, v);
        }

        private boolean handleExternalLink(String url, WebView v) {
            if (!enableExternalLinks) return true;
            if (!openExternalLinksInBrowser) return false;
            if (confirmOpenInBrowser) {
                new AlertDialog.Builder(v.getContext())
                        .setTitle(R.string.external_link).setMessage(R.string.open_in_browser)
                        .setPositiveButton(android.R.string.yes, (d, w) ->
                                v.getContext().startActivity(
                                        new Intent(Intent.ACTION_VIEW, Uri.parse(url))))
                        .setNegativeButton(android.R.string.no, null).show();
            } else {
                v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
            return true;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest req) {
            String host = req.getUrl().getHost();
            if (blockLocalhostRequests && ("127.0.0.1".equals(host)
                    || "localhost".equalsIgnoreCase(host)
                    || "::1".equals(host) || "0:0:0:0:0:0:0:1".equals(host))) {
                return new WebResourceResponse("text/plain", "UTF-8", null);
            }
            return super.shouldInterceptRequest(v, req);
        }

        @Override
        public void onPageStarted(WebView wv, String url, Bitmap favicon) {
            super.onPageStarted(wv, url, favicon);
            userScriptManager.injectScripts(wv, url);
        }

        @Override
        public void onPageFinished(WebView wv, String url) {
            // ── Stop swipe refresh animation ─────────────────────────────────
            swipeRefreshLayout.setRefreshing(false);

            SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
            String token = prefs.getString("token", null);
            if (token != null && !token.isEmpty())
                wv.evaluateJavascript(
                        "localStorage.setItem('session', '" + token + "');", null);

            // ── FIX 1: TTS guard — ensure the AI only speaks its own reply text,
            //    not a re-echo of the question. This patches window.speechSynthesis
            //    so that any speak() call is intercepted and the utterance text is
            //    validated against the last known assistant message role before
            //    being passed through. If your frontend sets window.__lastAiReply
            //    to the assistant's text, the guard enforces that only that text
            //    (or a substring of it) is spoken. If window.__lastAiReply is not
            //    set (i.e. on non-exam pages), speech passes through untouched. ──
            wv.evaluateJavascript(
                    "(function() {" +
                            "  if (window.__ttsGuardInstalled) return;" +
                            "  window.__ttsGuardInstalled = true;" +
                            "  var _origSpeak = window.speechSynthesis.speak.bind(window.speechSynthesis);" +
                            "  window.speechSynthesis.speak = function(utterance) {" +
                            "    var allowed = window.__lastAiReply;" +
                            "    if (!allowed || !utterance || !utterance.text) {" +
                            "      _origSpeak(utterance); return;" +
                            "    }" +
                            "    var uText = utterance.text.trim();" +
                            "    var aText = allowed.trim();" +
                            "    if (aText.indexOf(uText) !== -1 || uText === aText) {" +
                            "      _origSpeak(utterance);" +
                            "    } else {" +
                            "      console.warn('[TTS Guard] Blocked echoed question text from being spoken.');" +
                            "    }" +
                            "  };" +
                            "})();",
                    null
            );

            if (!errorOccurred) {
                spinner.setVisibility(View.GONE);
                if (!wv.isShown()) {
                    wv.startAnimation(fadeInAnimation);
                    wv.setVisibility(View.VISIBLE);
                }
            }
            super.onPageFinished(wv, url);
        }

        @Override
        public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err) {
            if (!req.isForMainFrame()) return;
            int code = err.getErrorCode();
            switch (code) {
                case ERROR_AUTHENTICATION: case ERROR_BAD_URL: case ERROR_CONNECT:
                case ERROR_FAILED_SSL_HANDSHAKE: case ERROR_FILE: case ERROR_FILE_NOT_FOUND:
                case ERROR_HOST_LOOKUP: case ERROR_IO: case ERROR_PROXY_AUTHENTICATION:
                case ERROR_TIMEOUT: case ERROR_TOO_MANY_REQUESTS: case ERROR_UNKNOWN:
                case ERROR_UNSUPPORTED_AUTH_SCHEME: case ERROR_UNSUPPORTED_SCHEME:
                    errorOccurred = true;
                    swipeRefreshLayout.setRefreshing(false);
                    errorLayout = getLayoutInflater().inflate(
                            R.layout.error, parentLayout, false);
                    parentLayout.removeView(mainLayout);
                    parentLayout.addView(errorLayout);
                    if (showDetailsOnErrorScreen) {
                        TextView tv = errorLayout.findViewById(R.id.errorText);
                        if (tv != null) tv.setText("Error " + code + ":\n"
                                + err.getDescription() + "\nURL: " + req.getUrl());
                    }
                    break;
                default:
                    Log.w(TAG, "Minor error " + code + ": " + err.getDescription());
            }
        }
    }

    public void tryAgain(View v) {
        parentLayout.removeView(errorLayout);
        parentLayout.addView(mainLayout);
        webview.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);
        errorOccurred = false;
        webview.reload();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  WebAppInterface (JS bridge)
    // ═══════════════════════════════════════════════════════════════════════
    private class WebAppInterface {
        private final Context context;
        WebAppInterface(Context c) { this.context = c; }

        @JavascriptInterface public void showShortToast(String msg) {
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
        }
        @JavascriptInterface public void showLongToast(String msg) {
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show());
        }
        @JavascriptInterface public boolean hasNotificationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                return ContextCompat.checkSelfPermission(context,
                        Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED;
            return true;
        }
        @JavascriptInterface public void requestNotificationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!hasNotificationPermission())
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                NOTIFICATION_PERMISSION_REQUEST_CODE);
                });
            }
        }
        @JavascriptInterface public void showNotification(String title, String message) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && !hasNotificationPermission()) return;
            Intent i = new Intent(context, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(
                    context, 0, i, PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Builder b =
                    new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(title).setContentText(message)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(pi).setAutoCancel(true);
            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED
                    || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                NotificationManagerCompat.from(context).notify(
                        (int) System.currentTimeMillis(), b.build());
            }
        }
        @JavascriptInterface public void share(String title, String text, String url) {
            Intent si = new Intent(Intent.ACTION_SEND);
            si.setType("text/plain");
            si.putExtra(Intent.EXTRA_SUBJECT, title);
            si.putExtra(Intent.EXTRA_TEXT,
                    (text != null ? text : "") + (url != null ? "\n" + url : ""));
            context.startActivity(Intent.createChooser(si,
                    title == null ? "Share" : title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        @JavascriptInterface public void unifiedPushSubscribe(String vapidPublicKey) {
            new Handler(Looper.getMainLooper()).post(() ->
                    MainActivity.this.registerForUnifiedPush(vapidPublicKey));
        }
        @JavascriptInterface public void unifiedPushUnregister() {
            UnifiedPush.unregisterApp(context, INSTANCE_DEFAULT);
        }
        @JavascriptInterface public String getUnifiedPushSubscriptionJson() {
            SharedPreferences p =
                    context.getSharedPreferences("unifiedpush", Context.MODE_PRIVATE);
            String ep  = p.getString("endpoint_" + INSTANCE_DEFAULT, null);
            String dh  = p.getString("p256dh_"   + INSTANCE_DEFAULT, null);
            String au  = p.getString("auth_"     + INSTANCE_DEFAULT, null);
            if (ep == null || ep.isEmpty() || dh == null || au == null) return "";
            try {
                JSONObject keys = new JSONObject(); keys.put("p256dh", dh); keys.put("auth", au);
                JSONObject sub  = new JSONObject();
                sub.put("endpoint", ep); sub.put("expirationTime", JSONObject.NULL);
                sub.put("keys", keys);
                return sub.toString();
            } catch (JSONException e) { return ""; }
        }
        @JavascriptInterface public String getNotificationPermissionState() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                return hasNotificationPermission() ? "granted" : "prompt";
            return "granted";
        }
        @JavascriptInterface public void saveToken(String token) {
            context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                    .edit().putString("token", token).apply();
        }
        @JavascriptInterface public void saveUserId(String userId) {
            context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                    .edit().putString("userId", userId).apply();
            String fcmToken = context.getSharedPreferences("fcm", Context.MODE_PRIVATE)
                    .getString("token", null);
            if (fcmToken != null) sendFcmTokenToServer(context, fcmToken);
        }
        @JavascriptInterface public void updateMediaMetadata(
                String title, String artist, String album, @Nullable String artworkUrl) {
            Intent i = new Intent(context, MediaPlaybackService.class);
            i.setAction(MediaPlaybackService.ACTION_UPDATE_METADATA);
            i.putExtra("title", title); i.putExtra("artist", artist);
            i.putExtra("album", album); i.putExtra("artworkUrl", artworkUrl);
            context.startService(i);
        }
        @JavascriptInterface public void updateMediaPlaybackState(String state) {
            Intent i = new Intent(context, MediaPlaybackService.class);
            i.setAction(MediaPlaybackService.ACTION_UPDATE_STATE);
            i.putExtra("state", state); context.startService(i);
        }
        @JavascriptInterface public void setMediaActionHandlers(String[] actions) {
            Intent i = new Intent(context, MediaPlaybackService.class);
            i.setAction(MediaPlaybackService.ACTION_SET_HANDLERS);
            i.putExtra("actions", actions); context.startService(i);
        }
        @JavascriptInterface public void updateMediaPositionState(
                double duration, double rate, double position) {
            Intent i = new Intent(context, MediaPlaybackService.class);
            i.setAction(MediaPlaybackService.ACTION_UPDATE_POSITION);
            i.putExtra("duration", duration); i.putExtra("playbackRate", rate);
            i.putExtra("position", position); context.startService(i);
        }
        @JavascriptInterface public boolean hasCameraPermission() {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED;
        }
        @JavascriptInterface public boolean hasMicrophonePermission() {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
        }
        @JavascriptInterface public void requestCameraPermission() {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!hasCameraPermission())
                    ActivityCompat.requestPermissions((Activity) context,
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_PERMISSION_REQUEST_CODE);
            });
        }
        @JavascriptInterface public void requestMicrophonePermission() {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!hasMicrophonePermission())
                    ActivityCompat.requestPermissions((Activity) context,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            MICROPHONE_PERMISSION_REQUEST_CODE);
            });
        }
    }
}