package com.example.warehouse;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.warehouse.model.Item;
import com.example.warehouse.model.SessionManager;
import com.example.warehouse.network.AppUrl;
import com.example.warehouse.network.InternetReceiver;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ScanItemActivity extends AppCompatActivity {
    protected ArrayList<String> items = Item.getItems();

    private CardView scan, move;
    private ImageView btn_back;
    private RelativeLayout loading_content;
    private FrameLayout loading_bar;
    private ListView list_item;
    private SwipeRefreshLayout refresh;
    private SessionManager session;
    private BroadcastReceiver receiver = null;
    private boolean isReceiverRegistered = false; // Add a flag

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_item);

        construct();
        status();

        Intent getBarcodeScanner = getIntent();
        String batch_number = getBarcodeScanner.getStringExtra("batch_number");
        addItem(batch_number);

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                process_scan();
            }
        });

        move.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                move_item();
            }
        });

        list_item.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                show_alert("DELETE", (String) parent.getItemAtPosition(position));
            }
        });

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Item.destroy(true);
                finish();
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
        btn_back = findViewById(R.id.btn_back);
        loading_content = findViewById(R.id.content);
        loading_bar = findViewById(R.id.bar);
        list_item = findViewById(R.id.list_item);
        refresh = findViewById(R.id.main);
        scan = findViewById(R.id.scan);
        move = findViewById(R.id.move);
    }

    protected void addItem(String _item) {
        if (!_item.isEmpty()) {
            Item.setItem(_item);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.list_view, this.items);
            list_item.setAdapter(arrayAdapter);
        }
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

    protected void move_item() {
        StringBuilder postItemBuilder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            postItemBuilder.append(items.get(i));
            if (i != items.size() - 1) {
                postItemBuilder.append(",");
            }
        }
        String post_item = postItemBuilder.toString();
        Intent move_item = new Intent(getApplicationContext(), AreaActivity.class);
        move_item.putExtra("items", post_item);
        startActivity(move_item);
    }

    private void process_scan() {
        ScanOptions scanOptions = new ScanOptions();
        scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        scanOptions.setBeepEnabled(false);
        scanOptions.setOrientationLocked(true);
        scanOptions.setCaptureActivity(CaptureAct.class);
        scanOptions.setPrompt("Volume button to toggle Flash On/Off");
        barcode_scanner.launch(scanOptions);
    }

    protected ActivityResultLauncher<ScanOptions> barcode_scanner = registerForActivityResult(new ScanContract(), result -> {
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
                JSONObject jsonObject = new JSONObject(s);
                int status = Integer.parseInt(jsonObject.getString("status"));
                switch (status) {
                    case 201:
                        set_visibility(false);
                        show_alert("MOVE", "");
                        break;
                    case 210:
                        set_visibility(false);
                        addItem(_content);
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

    protected void show_alert(String __MODE__, String _batch) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ScanItemActivity.this);
        if (__MODE__.equals("MOVE")) {
            builder.setTitle("Move Item")
                    .setMessage("Unknown Batch Number");
            builder.setPositiveButton("Yes", (dialog, which) -> dialog.cancel()).show();
        } else if (__MODE__.equals("DELETE")) {
            builder.setTitle("Delete Item")
                    .setMessage("Are you sure to delete this item?");
            builder.setPositiveButton("Yes", (dialog, which) -> {
                Item.delete(_batch);
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.list_view, this.items);
                list_item.setAdapter(arrayAdapter);
            });
            builder.setNegativeButton("No", (dialog, which) -> dialog.cancel()).show();
        }
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

}