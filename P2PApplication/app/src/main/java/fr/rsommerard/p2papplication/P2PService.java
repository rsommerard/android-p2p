package fr.rsommerard.p2papplication;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;

public class P2PService extends Service {

    private static final String SERVICE_NAME = "_RSP2P";
    private static final String SERVICE_TYPE = "_http._tcp";

    private final String TAG = "P2PService";

    private boolean mDataToSend = true;

    private NsdManager.ResolveListener mResolveListener;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager mNsdManager;
    private String mServiceName;
    private P2PClientThread mP2PClientThread;

    private boolean mIsServiceDiscoveryRunning;
    private NsdManager.RegistrationListener mRegistrationListener;

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
            Log.d(TAG, "Exception: \n" + e.getMessage());
        }

        NsdServiceInfo serviceInfo = new NsdServiceInfo();

        String randomName = String.valueOf(new Random().nextInt()) + Build.DEVICE;

        serviceInfo.setServiceName(randomName + SERVICE_NAME);
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
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

        new P2PServerThread(serverSocket).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        stopServiceDiscovery();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startServiceDiscovery() {
        initResolveListener();
        initDiscoveryListener();

        mIsServiceDiscoveryRunning = true;
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    private void initResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "Resolve failed: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Resolve Succeeded: \n" + serviceInfo);

                Log.d(TAG, "Data to send?: " + String.valueOf(mDataToSend));

                if (mDataToSend) {
                    int servicePort = serviceInfo.getPort();
                    InetAddress serviceHost = serviceInfo.getHost();

                    mP2PClientThread = new P2PClientThread(serviceHost, servicePort);
                    mP2PClientThread.start();

                    mDataToSend = false;
                }
            }
        };
    }

    private void stopServiceDiscovery() {
        if (mIsServiceDiscoveryRunning) {
            mIsServiceDiscoveryRunning = false;
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
    }

    private void initDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.d(TAG, "Discovery failed: " + errorCode);
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.d(TAG, "Discovery failed: " + errorCode);
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
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
}
