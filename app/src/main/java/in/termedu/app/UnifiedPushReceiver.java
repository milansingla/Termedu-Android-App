package in.termedu.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import org.unifiedpush.android.connector.MessagingReceiver;
import androidx.annotation.NonNull;

public class UnifiedPushReceiver extends MessagingReceiver {

    @Override
    public void onNewEndpoint(@NonNull Context context, @NonNull String endpoint, @NonNull String instance) {
        Log.d("WebToApk", "New endpoint: " + endpoint);
        // Store endpoint for retrieval
        SharedPreferences prefs = context.getSharedPreferences("unifiedpush", Context.MODE_PRIVATE);
        prefs.edit().putString("endpoint_" + instance, endpoint).apply();

        // Broadcast to MainActivity
        Intent intent = new Intent("com.termedu.webtoapk.NEW_ENDPOINT");
        intent.putExtra("endpoint", endpoint);
        context.sendBroadcast(intent);
    }

    @Override
    public void onRegistrationFailed(@NonNull Context context, @NonNull String instance) {
        Log.e("WebToApk", "UnifiedPush registration failed for: " + instance);
    }

    @Override
    public void onUnregistered(@NonNull Context context, @NonNull String instance) {
        Log.d("WebToApk", "UnifiedPush unregistered: " + instance);
        SharedPreferences prefs = context.getSharedPreferences("unifiedpush", Context.MODE_PRIVATE);
        prefs.edit().remove("endpoint_" + instance).apply();
    }

    @Override
    public void onMessage(@NonNull Context context, @NonNull byte[] message, @NonNull String instance) {
        Log.d("WebToApk", "Push message received for: " + instance);
        // Handle incoming push message here if needed
    }
}