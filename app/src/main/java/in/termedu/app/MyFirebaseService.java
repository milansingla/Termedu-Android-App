package in.termedu.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseService extends FirebaseMessagingService {

    private static final String TAG              = "TERMEdu_FCM";
    private static final String CHANNEL_ID       = "fcm_high_importance";
    private static final String CHANNEL_NAME     = "Push Notifications";

    // ═══════════════════════════════════════════════════════════════════════
    //  Token refresh — called when FCM issues a new token
    // ═══════════════════════════════════════════════════════════════════════
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token refreshed: " + token);

        // Persist locally
        getSharedPreferences("fcm", Context.MODE_PRIVATE)
                .edit().putString("token", token).apply();

        // Send to backend (static helper in MainActivity)
        MainActivity.sendFcmTokenToServer(getApplicationContext(), token);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Message received — handles foreground messages.
    //  Background / killed-state messages with a notification payload are
    //  displayed automatically by the FCM SDK; only data-only messages in
    //  the background come here.
    // ═══════════════════════════════════════════════════════════════════════
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message from: " + remoteMessage.getFrom());

        String title = null;
        String body  = null;
        String fcmType = null;

        // ── Case A: notification payload (sent from Firebase Console or backend
        //            with notification block) ─────────────────────────────────
        if (remoteMessage.getNotification() != null) {
            RemoteMessage.Notification n = remoteMessage.getNotification();
            title = n.getTitle();
            body  = n.getBody();
            Log.d(TAG, "Notification payload → title=" + title + " body=" + body);
        }

        // ── Case B: data payload (sent from backend with only data block,
        //            or alongside a notification block for extra params) ───────
        Map<String, String> data = remoteMessage.getData();
        if (!data.isEmpty()) {
            if (title == null) title = data.get("title");
            if (body  == null) body  = data.get("body");
            fcmType = data.get("fcm_type"); // e.g. "support"
            Log.d(TAG, "Data payload → " + data.toString());
        }

        // Fallback defaults
        if (title == null || title.isEmpty()) title = "TERM Edu";
        if (body  == null || body.isEmpty())  body  = "You have a new notification";

        showNotification(title, body, fcmType);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Build and display the system notification
    // ═══════════════════════════════════════════════════════════════════════
    private void showNotification(String title, String body, String fcmType) {
        // Create (or confirm) the high-importance channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("TERM Edu push notifications");
            channel.enableVibration(true);
            channel.setShowBadge(true);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        // ── Build the tap intent ─────────────────────────────────────────────
        // If this is a support notification, carry fcm_type so MainActivity
        // knows to open the support page.
        Intent tapIntent = new Intent(this, MainActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (fcmType != null && !fcmType.isEmpty()) {
            tapIntent.putExtra("fcm_type", fcmType);
        }

        int notificationId = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                notificationId,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // ── Build the notification ───────────────────────────────────────────
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)  // must be a valid drawable
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted — skipping notification");
                return;
            }
        }

        NotificationManagerCompat.from(this).notify(notificationId, builder.build());
        Log.d(TAG, "Notification shown: " + title);
    }
}