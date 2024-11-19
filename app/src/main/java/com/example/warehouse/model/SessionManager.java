package com.example.warehouse.model;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.warehouse.LoginActivity;
import com.example.warehouse.MainActivity;

import java.util.HashMap;

public class SessionManager {
    private static SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;
    private final Context context;
    private static final String myPreferences = "Warehouse";
    private static final String USERID = "idKey";
    private static final String NAME = "nameKey";
    private static final String EMAIL = "emailKey";
    private static final String LOGGED = "statusKey";

    public SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(myPreferences, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void createSession(String id, String email, String name) {
        editor.putBoolean(LOGGED, true);
        editor.putString(USERID, id);
        editor.putString(NAME, name);
        editor.putString(EMAIL, email);
        editor.commit();
    }

    public void checkLogin() {
        if (!this.isLogin()) {
            Intent i = new Intent(context, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } else {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

    public boolean isLogin() {
        return sharedPreferences.getBoolean(LOGGED, false);
    }

    public void logout() {
        editor.clear();
        editor.commit();
        Intent i = new Intent(context, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    public static HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<String, String>();
        user.put(USERID, sharedPreferences.getString(USERID, null));
        user.put(NAME, sharedPreferences.getString(NAME, null));
        user.put(EMAIL, sharedPreferences.getString(EMAIL, null));
        return user;
    }
}
