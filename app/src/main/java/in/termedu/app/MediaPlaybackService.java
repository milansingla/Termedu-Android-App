package in.termedu.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.io.InputStream;
import java.net.URL;

public class MediaPlaybackService extends Service {
    public static final String ACTION_UPDATE_METADATA = "UPDATE_METADATA";
    public static final String ACTION_UPDATE_STATE = "UPDATE_STATE";
    public static final String ACTION_SET_HANDLERS = "SET_HANDLERS";
    public static final String ACTION_UPDATE_POSITION = "UPDATE_POSITION";
    public static final String BROADCAST_MEDIA_ACTION = "com.termedu.webtoapk.MEDIA_ACTION";
    public static final String EXTRA_MEDIA_ACTION = "media_action";

    private static final String CHANNEL_ID = "media_playback_channel";
    private static final int NOTIFICATION_ID = 1001;

    private MediaSessionCompat mediaSession;
    private String currentTitle = "Playing";
    private String currentArtist = "";
    private String currentAlbum = "";
    private String currentState = "none";
    private String[] currentActions = new String[0];

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        setupMediaSession();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Media Playback", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Media playback controls");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(this, "WebToApkMediaSession");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override public void onPlay() { broadcastMediaAction("play"); }
            @Override public void onPause() { broadcastMediaAction("pause"); }
            @Override public void onSkipToNext() { broadcastMediaAction("nexttrack"); }
            @Override public void onSkipToPrevious() { broadcastMediaAction("previoustrack"); }
            @Override public void onStop() { broadcastMediaAction("stop"); }
            @Override public void onSeekTo(long pos) {
                broadcastMediaAction("seekto:" + (pos / 1000.0));
            }
        });
        mediaSession.setActive(true);
    }

    private void broadcastMediaAction(String action) {
        Intent intent = new Intent(BROADCAST_MEDIA_ACTION);
        intent.putExtra(EXTRA_MEDIA_ACTION, action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d("WebToApk", "Media action broadcast: " + action);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        if (action == null) return START_STICKY;

        switch (action) {
            case ACTION_UPDATE_METADATA:
                currentTitle = intent.getStringExtra("title") != null ? intent.getStringExtra("title") : "Playing";
                currentArtist = intent.getStringExtra("artist") != null ? intent.getStringExtra("artist") : "";
                currentAlbum = intent.getStringExtra("album") != null ? intent.getStringExtra("album") : "";
                String artworkUrl = intent.getStringExtra("artworkUrl");
                updateMetadata(artworkUrl);
                updateNotification();
                break;

            case ACTION_UPDATE_STATE:
                currentState = intent.getStringExtra("state") != null ? intent.getStringExtra("state") : "none";
                updatePlaybackState();
                updateNotification();
                break;

            case ACTION_SET_HANDLERS:
                String[] actions = intent.getStringArrayExtra("actions");
                currentActions = actions != null ? actions : new String[0];
                updateNotification();
                break;

            case ACTION_UPDATE_POSITION:
                double duration = intent.getDoubleExtra("duration", 0);
                double playbackRate = intent.getDoubleExtra("playbackRate", 1);
                double position = intent.getDoubleExtra("position", 0);
                updatePosition(duration, playbackRate, position);
                break;
        }

        return START_STICKY;
    }

    private void updateMetadata(@Nullable String artworkUrl) {
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentAlbum);

        if (artworkUrl != null && !artworkUrl.isEmpty()) {
            new Thread(() -> {
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream((InputStream) new URL(artworkUrl).getContent());
                    if (bitmap != null) {
                        mediaSession.setMetadata(builder
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                                .build());
                    }
                } catch (Exception e) {
                    Log.e("WebToApk", "Failed to load artwork", e);
                    mediaSession.setMetadata(builder.build());
                }
            }).start();
        } else {
            mediaSession.setMetadata(builder.build());
        }
    }

    private void updatePlaybackState() {
        int state;
        switch (currentState) {
            case "playing": state = PlaybackStateCompat.STATE_PLAYING; break;
            case "paused": state = PlaybackStateCompat.STATE_PAUSED; break;
            case "none": default: state = PlaybackStateCompat.STATE_NONE; break;
        }

        long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SEEK_TO;

        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .setActions(actions)
                .build());
    }

    private void updatePosition(double duration, double playbackRate, double position) {
        int state = currentState.equals("playing") ?
                PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;

        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(state, (long)(position * 1000), (float) playbackRate)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_SEEK_TO)
                .build());

        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (long)(duration * 1000))
                .build());
    }

    private void updateNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_IMMUTABLE);

        boolean isPlaying = "playing".equals(currentState);

        androidx.media.app.NotificationCompat.MediaStyle mediaStyle =
                new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(currentTitle)
                .setContentText(currentArtist)
                .setContentIntent(pendingIntent)
                .setStyle(mediaStyle)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(isPlaying);

        // Previous
        builder.addAction(android.R.drawable.ic_media_previous, "Previous",
                createActionIntent("previoustrack"));
        // Play/Pause
        if (isPlaying) {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause",
                    createActionIntent("pause"));
        } else {
            builder.addAction(android.R.drawable.ic_media_play, "Play",
                    createActionIntent("play"));
        }
        // Next
        builder.addAction(android.R.drawable.ic_media_next, "Next",
                createActionIntent("nexttrack"));

        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private PendingIntent createActionIntent(String action) {
        Intent intent = new Intent(this, MediaPlaybackService.class);
        intent.setAction("MEDIA_BUTTON_" + action.toUpperCase());
        return PendingIntent.getService(this, action.hashCode(), intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}