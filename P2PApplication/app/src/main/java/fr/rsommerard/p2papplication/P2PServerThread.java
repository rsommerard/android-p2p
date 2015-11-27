package fr.rsommerard.p2papplication;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class P2PServerThread extends Thread implements Runnable {

    private final String TAG = "P2PServerThread";

    private ServerSocket mServerSocket;

    public P2PServerThread(ServerSocket serverSocket) {
        super();
        mServerSocket = serverSocket;
    }

    @Override
    public void run() {
        try {
            while(true) {
                Socket socket = mServerSocket.accept();

                InputStream inputStream = socket.getInputStream();
                byte[] buffer = new byte[1024];

                int bytes = inputStream.read(buffer);

                Log.d(TAG, new String(buffer, 0, bytes));

                socket.close();
            }
        } catch (IOException e) {
            Log.d(TAG, "Exception: \n" + e.getMessage());
            // NOTHING
        }
    }
}
