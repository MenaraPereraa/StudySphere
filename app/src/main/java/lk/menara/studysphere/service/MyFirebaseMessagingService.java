package lk.menara.studysphere.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import lk.menara.studysphere.R;
import lk.menara.studysphere.activity.MainActivity;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_Service";
    private static final String CHANNEL_ID = "studysphere_notifications";
    private static final String CHANNEL_NAME = "StudySphere Notifications";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Handle data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Handle notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            handleNotificationMessage(remoteMessage.getNotification());
        }
    }

    /**
     * Called when a new token is generated for the device.
     * This token should be sent to your backend to identify the device for targeted messaging.
     */
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // TODO: Send this token to your backend server to store for this user
        // This allows your backend to send messages to this specific device
        sendTokenToServer(token);
    }

    /**
     * Handle data payload from FCM message
     */
    private void handleDataMessage(java.util.Map<String, String> data) {
        // Extract custom data from the message
        String title = data.getOrDefault("title", "StudySphere");
        String message = data.getOrDefault("body", "You have a new notification");
        String notificationType = data.getOrDefault("type", "general");

        Log.d(TAG, "Notification Type: " + notificationType);
        showNotification(title, message, notificationType);
    }

    /**
     * Handle notification payload from FCM message
     */
    private void handleNotificationMessage(RemoteMessage.Notification notification) {
        String title = notification.getTitle();
        String body = notification.getBody();

        if (title == null) {
            title = "StudySphere";
        }
        if (body == null) {
            body = "You have a new notification";
        }

        showNotification(title, body, "general");
    }


    private void showNotification(String title, String message, String notificationType) {
        // Create notification channel for Android 8+
        createNotificationChannel();

        // Create intent to open MainActivity when notification is clicked
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("notification_type", notificationType);

        // Create pending intent with proper flags for different Android versions
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT :
                PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        // Build notification with NotificationCompat for backward compatibility
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);

        // Add style for longer messages
        builder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(message));

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Notification displayed: " + title);
        }
    }

    /**
     * Create notification channel for Android 8 (API 26) and above
     * Required for displaying notifications on Android 8+
     */
    private void createNotificationChannel() {
        // Notification channels are only supported on Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription("Notifications from DriveGO car rental app");
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Send FCM token to your backend server.
     * This token is required by your backend to send messages to this specific device.
     *
     * @param token The FCM token for this device
     */
    private void sendTokenToServer(String token) {
        // TODO: Implement this method to send token to your backend
        // Example (using your existing Firebase setup):
        /*
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "FCM token sent to server successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send FCM token to server", e);
                    });
        }
        */
    }
}