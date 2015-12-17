package fr.rsommerard.p2papplication;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class P2PClientThread extends Thread implements Runnable {

    private final String TAG = "P2PClientThread";

    private final InetAddress mServiceHost;
    private final int mServicePort;

    public P2PClientThread(InetAddress serviceHost, int servicePort) {
        super();
        mServiceHost = serviceHost;
        mServicePort = servicePort;
    }

    @Override
    public void run() {
        Socket socket = new Socket();

        try {
            socket.connect(new InetSocketAddress(mServiceHost, mServicePort), 5000);

            Log.d(TAG, "connected to server");

            OutputStream outputStream = socket.getOutputStream();

            String message = "Réfléchir, c'est fléchir deux fois. - A. Damasio";

            byte[] buffer = message.getBytes();
            outputStream.write(buffer);

            socket.close();

            Log.d(TAG, "disconnected to server");
        } catch (IOException e) {
            Log.d(TAG, "Exception: " + e.getMessage());
            // Nothing
        }
    }
}
