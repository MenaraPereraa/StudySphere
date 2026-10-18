package lk.menara.studysphere.listener;

// NEW: Interface for connectivity and battery status changes
public interface ConnectivityListener {
    void onNetworkConnected();
    void onNetworkDisconnected();
    void onBatteryLow();
    void onBatteryOkay();
    void onPowerConnected();
    void onPowerDisconnected();
}
