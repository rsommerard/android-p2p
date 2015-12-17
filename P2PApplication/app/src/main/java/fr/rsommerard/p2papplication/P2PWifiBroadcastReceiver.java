package fr.rsommerard.p2papplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

public class P2PWifiBroadcastReceiver extends BroadcastReceiver {

    private final String TAG = "P2PWifiBroadcast";

    private P2PService mService;

    public P2PWifiBroadcastReceiver(P2PService service) {
        mService = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
            WifiP2pInfo wifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                // we are connected with the other device, request connection
                // info to find group owner IP
                Log.d(TAG, "Wifi direct devices connected");
                mService.clientProcess(wifiP2pInfo);
            } else {
                // It's a disconnect
            }
        }

        /*if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
            Log.d(MainActivity.TAG, "# WIFI_P2P_DISCOVERY_CHANGED_ACTION");
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 0);
            if (WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED == state) {
                Log.d(MainActivity.TAG, "> WIFI_P2P_DISCOVERY_STARTED");
            } else {
                Log.d(MainActivity.TAG, "> WIFI_P2P_DISCOVERY_STOPPED");
            }
        }*/

        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            //Log.d(MainActivity.TAG, "# WIFI_P2P_PEERS_CHANGED_ACTION");
            //WifiP2pDeviceList wifiP2pDeviceList = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);

            /*for(WifiP2pDevice peer : wifiP2pDeviceList.getDeviceList()) {
                Log.d(MainActivity.TAG, "Device found: " + peer.deviceName);
            }*/

            //Log.d(MainActivity.TAG, "> " + wifiP2pDeviceList.toString());
        }

        /*if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            Log.d(MainActivity.TAG, "# WIFI_P2P_STATE_CHANGED_ACTION");
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, 0);
            if (WifiP2pManager.WIFI_P2P_STATE_ENABLED == state) {
                Log.d(MainActivity.TAG, "> WIFI_P2P_STATE_ENABLED");
            } else {
                Log.d(MainActivity.TAG, "> WIFI_P2P_STATE_DISABLED");
            }
        }*/

        if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

            mService.setDeviceName(device.deviceName);
            Log.d(TAG, device.deviceName + " status - " + device.status);
        }
    }
}
