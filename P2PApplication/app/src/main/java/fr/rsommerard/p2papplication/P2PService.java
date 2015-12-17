package fr.rsommerard.p2papplication;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class P2PService extends Service {

    private static final String SERVICE_NAME = "_rsp2p";
    private static final String SERVICE_TYPE = "_http._tcp";

    private final String TAG = "P2PService";

    private boolean mDataToSend = true;

    private NsdManager.ResolveListener mResolveListener;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager mNsdManager;
    private String mServiceName;

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mWifiP2pChannel;
    private WifiP2pDevice mPeer;
    private int mPeerPort;
    private WifiP2pDnsSdServiceRequest mWifiP2pDnsSdServiceRequest;
    private WifiP2pDnsSdServiceInfo mWifiP2pDnsSdServiceInfo;

    private boolean mIsServiceDiscoveryRunning;
    private NsdManager.RegistrationListener mRegistrationListener;
    private IntentFilter mWifiIntentFilter;

    private String mDeviceName;

    private P2PServerThread mP2PServerThread;
    private P2PWifiBroadcastReceiver mWifiBroadcastReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mServiceName = null;
        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

        initAndStartServerThread();
        startServiceDiscovery();
    }

    private void initAndStartServerThread() {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            Log.d(TAG, "Exception: " + e.getMessage());
        }

        // Network wifi
        // -----------------------------------------------------------------------------------------
        /*NsdServiceInfo serviceInfo = new NsdServiceInfo();

        // TODO: replace Build.DEVICE by a random name.

        serviceInfo.setServiceName(Build.DEVICE + SERVICE_NAME);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(serverSocket.getLocalPort());

        mRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "Service registration failed: " + errorCode);
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "Service unregistration failed: " + errorCode);
            }

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                mServiceName = serviceInfo.getServiceName();
                Log.d(TAG, "Service registered as: " + mServiceName);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service unregistered: " + serviceInfo.getServiceName());
            }
        };

        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);*/

        // Direct wifi
        // -----------------------------------------------------------------------------------------
        mWifiBroadcastReceiver = new P2PWifiBroadcastReceiver(this);

        mWifiIntentFilter = new IntentFilter();
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        registerReceiver(mWifiBroadcastReceiver, mWifiIntentFilter);

        Map<String, String> record = new HashMap<String, String>();
        record.put("port", String.valueOf(serverSocket.getLocalPort()));

        mWifiP2pDnsSdServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(
                Build.DEVICE + SERVICE_NAME, SERVICE_TYPE, record);

        mWifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mWifiP2pChannel = mWifiP2pManager.initialize(this, getMainLooper(), null);

        mWifiP2pManager.addLocalService(mWifiP2pChannel, mWifiP2pDnsSdServiceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Local service added with success: " + Build.DEVICE + SERVICE_NAME);
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "Local add service failed: " + reasonCode);
            }
        });

        mWifiP2pManager.setDnsSdResponseListeners(mWifiP2pChannel,
                new WifiP2pManager.DnsSdServiceResponseListener() {
                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType,
                                                        WifiP2pDevice device) {
                        Log.d(TAG, "Service Found: " + instanceName + " - " + registrationType);
                    }
                },
                new WifiP2pManager.DnsSdTxtRecordListener() {
                    @Override
                    public void onDnsSdTxtRecordAvailable(String fullDomainName,
                                                          Map<String, String> record,
                                                          final WifiP2pDevice device) {
                        Log.d(TAG, "Device name: " + device.deviceName);
                        Log.d(TAG, device.deviceName + ": port: " + record.get("port"));

                        mPeer = device;
                        mPeerPort = Integer.parseInt(record.get("port"));
                        Log.d(TAG, "Peer port: " + mPeerPort);

                        if (fullDomainName.contains(mDeviceName)) {
                            Log.d(TAG, "Trying to connect himself: " + mDeviceName);
                            return;
                        }


                        if (fullDomainName.contains(SERVICE_NAME)) {
                            WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
                            wifiP2pConfig.deviceAddress = device.deviceAddress;
                            wifiP2pConfig.wps.setup = WpsInfo.PBC;

                            /*if (mWifiP2pDnsSdServiceRequest != null)
                                mWifiP2pManager.removeServiceRequest(mWifiP2pChannel,
                                        mWifiP2pDnsSdServiceRequest, new WifiP2pManager.ActionListener() {
                                            @Override
                                            public void onSuccess() {
                                                Log.d(TAG, "Service request removed");
                                            }

                                            @Override
                                            public void onFailure(int reason) {
                                                Log.d(TAG, "Remove service request failed: " + reason);
                                            }
                                        }
                                );*/

                            if (mDataToSend) {
                                mWifiP2pManager.connect(mWifiP2pChannel, wifiP2pConfig, new WifiP2pManager.ActionListener() {

                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "Connecting to " + device.deviceName);
                                    }

                                    @Override
                                    public void onFailure(int reason) {
                                        Log.d(TAG, "Connection failed: " + reason);
                                    }
                                });
                            }
                        }
                    }
                }
        );

        mWifiP2pDnsSdServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        mWifiP2pManager.addServiceRequest(mWifiP2pChannel, mWifiP2pDnsSdServiceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Service request added with success");
            }

            @Override
            public void onFailure(int error) {
                Log.e(TAG, "Add service request failed: " + error);
            }
        });

        // Start server thread
        // -----------------------------------------------------------------------------------------
        mP2PServerThread = new P2PServerThread(serverSocket);
        mP2PServerThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        // unregisterNetworkService();

        stopServerThread();

        // stopNetworkServiceDiscovery();
        stopDirectServiceDiscovery();

        unregisterReceiver(mWifiBroadcastReceiver);
    }

    private void unregisterNetworkService() {
        mNsdManager.unregisterService(mRegistrationListener);
    }

    private void stopServerThread() {
        if (mP2PServerThread != null && mP2PServerThread.isAlive()) {
            mP2PServerThread.finish();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startServiceDiscovery() {
        initResolveListener();
        initDiscoveryListener();

        // Network wifi
        // -----------------------------------------------------------------------------------------
        /*mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);*/


        // Direct wifi
        // -----------------------------------------------------------------------------------------
        mWifiP2pManager.discoverServices(mWifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Services discovery initiated");
            }

            @Override
            public void onFailure(int code) {
                Log.e(TAG, "Services discovery failed: " + code);
            }
        });
    }

    private void initResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "Resolve failed: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Resolve Succeeded: " + serviceInfo);

                Log.d(TAG, "Data to send?: " + String.valueOf(mDataToSend));

                if (mDataToSend) {
                    int servicePort = serviceInfo.getPort();
                    InetAddress serviceHost = serviceInfo.getHost();

                    new P2PClientThread(serviceHost, servicePort).start();

                    mDataToSend = false;
                }
            }
        };
    }

    private void stopNetworkServiceDiscovery() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    private void stopDirectServiceDiscovery() {
        if (mWifiP2pManager != null && mWifiP2pChannel != null) {
            mWifiP2pManager.stopPeerDiscovery(mWifiP2pChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Stop discovery failed: " + reasonCode);
                }

                @Override
                public void onSuccess() {
                }

            });

            mWifiP2pManager.removeLocalService(mWifiP2pChannel, mWifiP2pDnsSdServiceInfo, new WifiP2pManager.ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Remove local service failed: " + reasonCode);
                }

                @Override
                public void onSuccess() {
                }

            });

            mWifiP2pManager.removeGroup(mWifiP2pChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Remove group failed: " + reasonCode);
                }

                @Override
                public void onSuccess() {
                }

            });
        }
    }

    private void initDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.d(TAG, "Discovery failed: " + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.d(TAG, "Discovery failed: " + errorCode);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service discovery success: " + serviceInfo);

                if (!serviceInfo.getServiceType().contains(SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + serviceInfo.getServiceType());
                } else if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (serviceInfo.getServiceName().contains(SERVICE_NAME)) {
                    mNsdManager.resolveService(serviceInfo, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service lost: " + serviceInfo);
            }
        };
    }

    public void clientProcess(WifiP2pInfo wifiP2pInfo) {
        new P2PClientThread(wifiP2pInfo.groupOwnerAddress, mPeerPort).start();
        mDataToSend = false;
    }

    public void setDeviceName(String deviceName) {
        mDeviceName = deviceName;
    }
}
