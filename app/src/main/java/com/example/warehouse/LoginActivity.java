package com.example.warehouse;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.warehouse.model.SessionManager;
import com.example.warehouse.network.AppUrl;
import com.example.warehouse.network.InternetReceiver;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText email, password;
    private SwipeRefreshLayout refresh;
    private CardView login;
    private SessionManager session;
    private BroadcastReceiver receiver = null;
    private boolean isReceiverRegistered = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        construct();
        status();

        login.setOnClickListener(v -> {
            if (Objects.requireNonNull(email.getText()).toString().isEmpty() || Objects.requireNonNull(password.getText()).toString().isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please input a valid username or password", Toast.LENGTH_LONG).show();
            } else {
                processLogin(email.getText().toString(), password.getText().toString());
            }
        });

        refresh.setOnRefreshListener(() -> new android.os.Handler().postDelayed(
                () -> {
                    status();
                    refresh.setEnabled(true);
                    refresh.setRefreshing(false);
                },
                2000 // Delay in milliseconds
        ));
    }

    protected void construct() {
        session = new SessionManager(getApplicationContext());
        receiver = new InternetReceiver();
        password = findViewById(R.id.password);
        refresh = findViewById(R.id.main);
        email = findViewById(R.id.email);
        login = findViewById(R.id.login);
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

    protected void processLogin(String _email, String _password) {
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.POST, AppUrl.LOGIN_URL, s -> {
            try {
                JSONObject resultJSON = new JSONObject(s);
                int status = Integer.parseInt(resultJSON.getString("status"));
                switch (status) {
                    case 201:
                        Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_LONG).show();
                        break;
                    case 210:
                        try {
                            JSONObject result = new JSONObject(resultJSON.getString("body"));
                            session.createSession(
                                    result.getString("id"),
                                    result.getString("name"),
                                    _password
                            );
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } catch (Throwable throwable) {
                            Toast.makeText(LoginActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        break;
                    default:
                }
            } catch (JSONException jsonException) {
                Toast.makeText(LoginActivity.this, jsonException.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, volleyError -> Toast.makeText(LoginActivity.this, volleyError.getMessage(), Toast.LENGTH_LONG).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", _email);
                params.put("password", _password);
                return params;
            }
        };
        queue.add(request);
    }

}
