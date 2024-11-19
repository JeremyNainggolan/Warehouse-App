package com.example.warehouse;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.warehouse.model.SessionManager;
import com.example.warehouse.network.AppUrl;
import com.example.warehouse.network.InternetReceiver;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private CardView scan_act, stock_act, logout_act;
    private LinearLayout loading_content;
    private FrameLayout loading_bar;
    private SwipeRefreshLayout refresh;
    private SessionManager session;
    private BroadcastReceiver receiver = null;
    private boolean isReceiverRegistered = false; // Add a flag

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        construct();
        status();

        scan_act.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                process_scan();
            }
        });

        stock_act.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                process_stock();
                Intent stock = new Intent(getApplicationContext(), StockTakeActivity.class);
                startActivity(stock);
            }
        });

        logout_act.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Confirm?")
                        .setMessage("Are you sure to logout from the application?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            process_logout();
                            finish();
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.cancel()).show();
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
        refresh = findViewById(R.id.main);
        scan_act = findViewById(R.id.scan_act);
        stock_act = findViewById(R.id.stock_act);
        logout_act = findViewById(R.id.logout_act);
        loading_bar = findViewById(R.id.bar);
        loading_content = findViewById(R.id.content);
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

    protected void set_visibility(boolean _status) {
        if (_status) {
            loading_bar.setVisibility(View.VISIBLE);
            loading_content.setVisibility(View.GONE);
        } else {
            loading_bar.setVisibility(View.GONE);
            loading_content.setVisibility(View.VISIBLE);
        }
    }

    private void process_scan() {
        ScanOptions scanOptions = new ScanOptions();
        scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        scanOptions.setBeepEnabled(false);
        scanOptions.setOrientationLocked(true);
        scanOptions.setCaptureActivity(CaptureAct.class);
        scanOptions.setPrompt("Volume button to toggle Flash On/Off");
        scan.launch(scanOptions);
    }

    protected ActivityResultLauncher<ScanOptions> scan = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {
            set_visibility(true);
            check_item(result.getContents());
        }
    });

    private void check_item(String _content) {
        String URL = AppUrl.GET_BATCH;
        RequestQueue queue = Volley.newRequestQueue(this);
        @SuppressLint("SetTextI18n") StringRequest request = new StringRequest(Request.Method.POST, URL, s -> {
            try {
                Log.d("ITEM_TAG", s);
                JSONObject jsonObject = new JSONObject(s);
                int status = Integer.parseInt(jsonObject.getString("status"));
                switch (status) {
                    case 201:
                        set_visibility(false);
                        show_alert("MOVE_ITEM");
                        break;
                    case 210:
                        Intent intent = new Intent(getApplicationContext(), ScanItemActivity.class);
                        intent.putExtra("batch_number", _content);
                        startActivity(intent);
                        set_visibility(false);
                        break;
                }
            } catch (Throwable throwable) {
                set_visibility(false);
                Toast.makeText(getApplicationContext(), throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, volleyError -> Toast.makeText(getApplicationContext(), volleyError.getMessage(), Toast.LENGTH_LONG).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("batch_number", _content);
                return params;
            }
        };
        queue.add(request);
    }

    private void process_stock() {
        ScanOptions scanOptions = new ScanOptions();
        scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        scanOptions.setBeepEnabled(false);
        scanOptions.setOrientationLocked(true);
        scanOptions.setCaptureActivity(CaptureAct.class);
        scanOptions.setPrompt("Volume button to toggle Flash On/Off");
        stock.launch(scanOptions);
    }

    protected ActivityResultLauncher<ScanOptions> stock = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {
            set_visibility(true);
            check_stock(result.getContents());
        }
    });

    private void check_stock(String _content) {
        String URL = AppUrl.CHECK + _content;
        RequestQueue queue = Volley.newRequestQueue(this);
        @SuppressLint("SetTextI18n") StringRequest request = new StringRequest(Request.Method.GET, URL, s -> {
            try {
                JSONObject jsonObject = new JSONObject(s);
                int status = Integer.parseInt(jsonObject.getString("status"));
                switch (status) {
                    case 201:
                        set_visibility(false);
                        show_alert("STOCK_TAKE");
                        break;
                    case 210:
                        Toast.makeText(getApplicationContext(), "StockTake: " + _content, Toast.LENGTH_LONG).show();
//                        Intent intent = new Intent(getApplicationContext(), StockTakeActivity.class);
//                        intent.putExtra("batch_number", _content);
//                        startActivity(intent);
                        break;
                }
            } catch (Throwable throwable) {
                set_visibility(false);
                Toast.makeText(getApplicationContext(), throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, volleyError -> {

        });
        queue.add(request);
    }

    protected void show_alert(String _mode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        if (_mode.equals("STOCK_TAKE")) {
            builder.setTitle("Stock Take Event")
                    .setMessage("Stock Take Event is not active");
        } else if (_mode.equals("MOVE_ITEM")) {
            builder.setTitle("Move Item")
                    .setMessage("Unknown Batch Number");
        }
        builder.setPositiveButton("Yes", (dialog, which) -> dialog.cancel()).show();
    }

    private void process_logout() {
        session.logout();
    }
}
