package com.example.warehouse.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;


public class InternetReceiver extends BroadcastReceiver {

    private Context appContext;
    /**
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        appContext = context;

        String status = CheckInternet.getNetworkInfo(context);
        if (status.equals("connected")) {
            //
        } else if (status.equals("disconnected")){
            showDialog();
        }
    }
    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(appContext);
        builder.setTitle("No Internet Connection")
                .setMessage("Please reconnect to your internet")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String status = CheckInternet.getNetworkInfo(appContext);
                        if (status.equals("disconnected")) {
                            showDialog();
                        }
                    }
                }).show();
    }
}
