package com.example.warehouse;

import static com.example.warehouse.network.AppUrl.BASE_URL;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.warehouse.model.SessionManager;
import com.example.warehouse.network.HttpTaskListener;
import com.example.warehouse.network.InternetReceiver;
import com.example.warehouse.network.MyHttpTask;

public class SplashActivity extends AppCompatActivity implements HttpTaskListener {

    private SessionManager sessionManager;
    private MyHttpTask httpTask;
    private BroadcastReceiver receiver = null;
    private boolean isReceiverRegistered = false;
    private SwipeRefreshLayout refresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        receiver = new InternetReceiver();
        status();

        httpTask = new MyHttpTask(this);
        httpTask.execute(BASE_URL);
    }

    private void status() {
        if (!isReceiverRegistered) { // Check if not already registered
            registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            isReceiverRegistered = true; // Set flag to true
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isReceiverRegistered) { // Unregister only if registered
            unregisterReceiver(receiver);
            isReceiverRegistered = false; // Reset flag
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        status(); // Re-register the receiver when resuming
    }

    @Override
    public void onTaskComplete(String result) {
        boolean Result = httpTask.getResult();
        if (Result) {
            sessionManager = new SessionManager(getApplicationContext());
            Handler handler = new Handler();
            int SPLASH_SCREEN = 3000;
            handler.postDelayed(() -> {
                sessionManager.checkLogin();
                finish();
            }, SPLASH_SCREEN);
        } else {
            Toast.makeText(getApplicationContext(), "Contact ur IT Support", Toast.LENGTH_LONG).show();
        }
    }
}