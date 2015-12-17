package fr.rsommerard.p2papplication;

import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class P2PServerThread extends Thread implements Runnable {

    private final String TAG = "P2PServerThread";

    private ServerSocket mServerSocket;
    private boolean mFinish;

    public P2PServerThread(ServerSocket serverSocket) {
        super();
        mServerSocket = serverSocket;
        mFinish = false;
    }

    public void finish() {
        mFinish = true;
    }

    @Override
    public void run() {
        try {
            while(!mFinish) {
                Socket socket = mServerSocket.accept();

                InputStream inputStream = socket.getInputStream();
                byte[] buffer = new byte[1024];

                int bytes = inputStream.read(buffer);

                String message = new String(buffer, 0, bytes);

                Log.d(TAG, message);

                socket.close();
            }
        } catch (IOException e) {
            Log.d(TAG, "Exception: " + e.getMessage());
            // NOTHING
        }
    }
}
