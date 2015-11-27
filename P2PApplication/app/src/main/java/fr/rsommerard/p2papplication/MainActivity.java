package fr.rsommerard.p2papplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "P2PApplication";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startServiceButton = (Button) findViewById(R.id.button_start_service);
        startServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService();
            }
        });

        Button stopServiceButton = (Button) findViewById(R.id.button_stop_service);
        stopServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService();
            }
        });
    }

    public void startService() {
        Log.d(TAG, "startService P2PService");
        startService(new Intent(this, P2PService.class));
    }

    public void stopService() {
        Log.d(TAG, "stopService P2PService");
        stopService(new Intent(this, P2PService.class));
    }
}
