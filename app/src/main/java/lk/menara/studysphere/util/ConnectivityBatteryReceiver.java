package lk.menara.studysphere.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import lk.menara.studysphere.listener.ConnectivityListener;
import lk.menara.studysphere.util.CacheDatabaseHelper;

public class ConnectivityBatteryReceiver extends BroadcastReceiver {

    private final ConnectivityListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ConnectivityManager.NetworkCallback networkCallback;
    private Context appContext;
    private boolean registered = false;
    private boolean wasConnected = false;
    private boolean wasBatteryLow = false;
    private boolean wasPlugged = false;

    public ConnectivityBatteryReceiver(ConnectivityListener listener) {
        this.listener = listener;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
            handleBatteryChange(intent);
        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            updateConnectivity(isNetworkAvailable());
        }
    }

    private void updateConnectivity(boolean isConnected) {
        if (isConnected && !wasConnected) {
            wasConnected = true;
            if (listener != null) listener.onNetworkConnected();
            syncOfflineDataToFirebase();
        } else if (!isConnected && wasConnected) {
            wasConnected = false;
            if (listener != null) listener.onNetworkDisconnected();
        }
    }

    private void syncOfflineDataToFirebase() {
        if (appContext == null) return;

        new Thread(() -> {
            CacheDatabaseHelper dbHelper = CacheDatabaseHelper.getInstance(appContext);
            List<CacheDatabaseHelper.SyncItem> pendingItems = dbHelper.getPendingSyncItems();

            if (pendingItems.isEmpty()) return;

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();

            for (CacheDatabaseHelper.SyncItem item : pendingItems) {
                Map<String, Object> data = gson.fromJson(item.dataJson, mapType);

                firestore.collection(item.collection)
                        .add(data)
                        .addOnSuccessListener(documentReference -> {
                            dbHelper.deleteSyncItem(item.id);
                        });
            }
        }).start();
    }

    private void handleBatteryChange(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);

        boolean isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC
                || plugged == BatteryManager.BATTERY_PLUGGED_USB
                || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;

        if (level >= 0 && scale > 0) {
            int batteryPct = level * 100 / scale;
            boolean isBatteryLow = batteryPct <= 15 && !isPlugged;
            if (isBatteryLow && !wasBatteryLow) {
                wasBatteryLow = true;
                if (listener != null) listener.onBatteryLow();
            } else if (!isBatteryLow && wasBatteryLow) {
                wasBatteryLow = false;
                if (listener != null) listener.onBatteryOkay();
            }
        }

        if (isPlugged && !wasPlugged) {
            wasPlugged = true;
            if (listener != null) listener.onPowerConnected();
        } else if (!isPlugged && wasPlugged) {
            wasPlugged = false;
            if (listener != null) listener.onPowerDisconnected();
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private void registerNetworkCallback() {
        final ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
                final boolean connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                mainHandler.post(() -> updateConnectivity(connected));
            }

            @Override
            public void onLost(@NonNull Network network) {
                Network active = cm.getActiveNetwork();
                final boolean connected = active != null && !active.equals(network);
                mainHandler.post(() -> updateConnectivity(connected));
            }
        };
        cm.registerDefaultNetworkCallback(networkCallback);
    }

    @SuppressWarnings("deprecation")
    public static void register(Context context, ConnectivityBatteryReceiver receiver) {
        if (receiver.registered) return;
        receiver.appContext = context.getApplicationContext();
        receiver.wasConnected = receiver.isNetworkAvailable();

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        }
        ContextCompat.registerReceiver(receiver.appContext, receiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            receiver.registerNetworkCallback();
        }
        receiver.registered = true;
    }

    public static void unregister(Context context, ConnectivityBatteryReceiver receiver) {
        if (!receiver.registered) return;
        try {
            receiver.appContext.unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
        }
        if (receiver.networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) receiver.appContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            try {
                if (cm != null) cm.unregisterNetworkCallback(receiver.networkCallback);
            } catch (IllegalArgumentException ignored) {
            }
            receiver.networkCallback = null;
        }
        receiver.registered = false;
    }
}