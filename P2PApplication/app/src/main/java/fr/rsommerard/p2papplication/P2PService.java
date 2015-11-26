package fr.rsommerard.p2papplication;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import java.net.InetAddress;

public class P2PService extends Service implements Handler.Callback {

    private static final String TAG = "P2PService";

    private static final String SERVICE_NAME = "_RSP2P";
    private static final String SERVICE_TYPE = "_http._tcp";

    private NsdManager.ResolveListener mResolveListener;
    private Handler mHandler;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager mNsdManager;
    private String mServiceName;
    private P2PClientThread mP2PClientThread;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        // Client part
        mServiceName = null;
        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

        initHandler();
        initResolveListener();
        initDiscoveryListener();
        discoverServices();
    }

    private void initHandler() {
        mHandler = new Handler(this);
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

                // TODO: Check if this statement is necessary
                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same IP");
                    return;
                }

                stopServiceDiscovery();

                int servicePort = serviceInfo.getPort();
                InetAddress serviceHost = serviceInfo.getHost();

                mP2PClientThread = new P2PClientThread(serviceHost, servicePort, mHandler);
                mP2PClientThread.start();
            }
        };
    }

    private void stopServiceDiscovery() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
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
                } else if (serviceInfo.getServiceName().contains(SERVICE_NAME)){
                    mNsdManager.resolveService(serviceInfo, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service lost: " + serviceInfo);
            }
        };
    }

    private void discoverServices() {
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d(TAG, "Message: " + msg.obj.toString());
        return true;
    }
}
