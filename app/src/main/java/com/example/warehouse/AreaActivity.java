package com.example.warehouse;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.warehouse.model.Item;
import com.example.warehouse.model.SessionManager;
import com.example.warehouse.network.AppUrl;
import com.example.warehouse.network.InternetReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AreaActivity extends AppCompatActivity {
    protected String items;
    private BroadcastReceiver receiver = null;
    private boolean isReceiverRegistered = false; // Add a flag

    private CardView building, sub_building, confirm;
    private ImageView btn_back;
    private TextView building_txt, sub_txt;
    private ListView list_area, list_batch;
    private RelativeLayout loading_content;
    private FrameLayout loading_bar;
    private SwipeRefreshLayout refresh;

    private final String building_helper = "Building";
    private final String sub_helper = "Sub Building";
    private String temp_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area);

        this.items = getIntent().getStringExtra("items");

        construct();
        status();

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        building.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                building_txt.setText(building_helper);
                sub_txt.setText(sub_helper);
                building.setCardBackgroundColor(getResources().getColor(R.color.red));
                sub_building.setCardBackgroundColor(getResources().getColor(R.color.dark));
                list_area.setVisibility(View.VISIBLE);
                list_batch.setVisibility(View.GONE);
                add_building();
            }
        });

        sub_building.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sub_txt.setText(sub_helper);
                building.setCardBackgroundColor(getResources().getColor(R.color.dark));
                sub_building.setCardBackgroundColor(getResources().getColor(R.color.red));
                list_area.setVisibility(View.VISIBLE);
                list_batch.setVisibility(View.GONE);
                add_sub_building(temp_id);
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

    private void construct() {
        receiver = new InternetReceiver();

        building = findViewById(R.id.building);
        btn_back = findViewById(R.id.btn_back);
        sub_building = findViewById(R.id.sub_building);
        list_area = findViewById(R.id.list_area);
        list_batch = findViewById(R.id.list_batch);
        loading_content = findViewById(R.id.content);
        loading_bar = findViewById(R.id.bar);
        refresh = findViewById(R.id.main);
        building_txt = findViewById(R.id.building_txt);
        sub_txt = findViewById(R.id.sub_txt);
        confirm = findViewById(R.id.confirm);

        add_building();
    }

    protected void add_building() {
        set_visibility(true);
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.GET, AppUrl.GET_AREA, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                try {
                    JSONObject object = new JSONObject(s);
                    int status = Integer.parseInt(object.getString("status"));
                    switch (status) {
                        case 201:
                            set_visibility(false);
                            status();
                            break;
                        case 210:
                            set_visibility(false);
                            JSONObject body = object.getJSONObject("body");
                            Iterator<String> keys = body.keys();
                            ArrayList<String> data = new ArrayList<>();
                            HashMap<String, String> idMap = new HashMap<>();

                            while (keys.hasNext()) {
                                String key = keys.next();
                                JSONObject item = body.getJSONObject(key);

                                String itemName = item.getString("name");
                                String itemId = String.valueOf(item.getInt("id"));

                                data.add(itemName);
                                idMap.put(itemName, itemId);
                            }

                            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.list_view, data);
                            list_area.setAdapter(arrayAdapter);

                            list_area.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    String areaName = data.get(position);
                                    String areaId = idMap.get(areaName);
                                    temp_id = areaId;

                                    set_building_txt(areaName, Integer.parseInt(areaId));

                                    if (areaName.equals("Delivered")) {
                                        set_move(areaId, areaName);
                                    } else {
                                        add_sub_building(areaId);
                                    }

                                }
                            });
                            break;
                    }
                } catch (Exception e) {
                    set_visibility(false);
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }, volleyError -> {
            set_visibility(false);
        });
        queue.add(request);
    }

    protected void add_sub_building(String _areaID) {
        set_visibility(true);
        final String URL = AppUrl.GET_AREA + _areaID;
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.GET, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                try {
                    set_visibility(false);
                    JSONObject object = new JSONObject(s);
                    int status = Integer.parseInt(object.getString("status"));
                    switch (status) {
                        case 201:
                            set_visibility(false);
                            status();
                            break;
                        case 210:
                            set_visibility(false);
                            JSONObject body = object.getJSONObject("body");
                            Iterator<String> keys = body.keys();
                            ArrayList<String> data = new ArrayList<>();
                            HashMap<String, String> idMap = new HashMap<>();

                            while (keys.hasNext()) {
                                String key = keys.next();
                                JSONObject area = body.getJSONObject(key);

                                String areaName = area.getString("name");
                                String areaId = String.valueOf(area.getInt("id"));

                                data.add(areaName);
                                idMap.put(areaName, areaId);
                            }

                            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.list_view, data);
                            list_area.setAdapter(arrayAdapter);

                            list_area.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    String areaName = data.get(position);
                                    String areaId = idMap.get(areaName);
                                    set_sub_txt(areaName, Integer.parseInt(areaId));
                                    set_move(areaId, areaName);
                                }
                            });
                            break;
                    }
                } catch (Exception e) {
                    set_visibility(false);
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }, volleyError -> {
            set_visibility(false);
        });
        queue.add(request);
    }

    private void set_building_txt(String _name, int _id) {
        this.building.setCardBackgroundColor(getResources().getColor(R.color.dark));
        this.sub_building.setCardBackgroundColor(getResources().getColor(R.color.red));
        this.building_txt.setText(_name);
        this.building_txt.setId(_id);
    }

    private void set_sub_txt(String _name, int _id) {
        this.building.setCardBackgroundColor(getResources().getColor(R.color.dark));
        this.sub_building.setCardBackgroundColor(getResources().getColor(R.color.dark));
        this.sub_txt.setText(_name);
        this.sub_txt.setId(_id);

        this.list_area.setVisibility(View.GONE);
        this.list_batch.setVisibility(View.VISIBLE);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.list_view, Item.getItems());
        list_batch.setAdapter(arrayAdapter);
    }

    protected void set_move(String _id, String _name) {
        HashMap<String, String> userDetails = SessionManager.getUserDetails();
        String idUser = userDetails.get("idKey");
        RequestQueue queue = Volley.newRequestQueue(this);

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AreaActivity.this);
                builder.setTitle("Confirm?")
                        .setMessage("Are you sure to move this item to " + _name + "?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            StringRequest request = new StringRequest(Request.Method.POST, AppUrl.MOVE_AREA, s -> {
                                try {
                                    set_visibility(true);
                                    JSONObject resultJSON = new JSONObject(s);
                                    int status = Integer.parseInt(resultJSON.getString("status"));
                                    switch (status) {
                                        case 201:
                                            set_visibility(false);
                                            break;
                                        case 210:
                                            set_visibility(false);
                                            try {
                                                Item.destroy(true);
                                                Toast.makeText(getApplicationContext(), "Successfully move item", Toast.LENGTH_LONG).show();
                                                Intent home = new Intent(getApplicationContext(), MainActivity.class);
                                                home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(home);
                                            } catch (Throwable throwable) {
                                                Toast.makeText(getApplicationContext(), throwable.getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                            break;
                                        default:
                                    }
                                } catch (JSONException jsonException) {
                                    set_visibility(false);
                                    Toast.makeText(getApplicationContext(), jsonException.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }, volleyError -> Toast.makeText(getApplicationContext(), volleyError.getMessage(), Toast.LENGTH_LONG).show()) {
                                @Override
                                protected Map<String, String> getParams() {
                                    Map<String, String> params = new HashMap<>();
                                    params.put("batch_number", items);
                                    params.put("area_id", _id);
                                    params.put("userid", idUser);

                                    return params;
                                }
                            };
                            queue.add(request);
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.cancel()).show();
            }
        });
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