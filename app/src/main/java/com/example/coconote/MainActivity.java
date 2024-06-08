package com.example.coconote;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

//    private OkHttpClient client = new OkHttpClient();
//    private ExecutorService executorService = Executors.newFixedThreadPool(2);
//    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!isNetworkAvailable()){
            showNoNetworkDialog();
        }
    }

//    // 封装的异步执行方法
//    private void doAsyncCode(Runnable task) {
//        executorService.submit(task);
//    }
//
//    // 封装的主线程UI更新方法
//    private void doOnUiCode(Runnable task) {
//        mainHandler.post(task);
//    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    private void showNoNetworkDialog() {
        new AlertDialog.Builder(this)
                .setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again.")
                .setCancelable(false)
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isNetworkAvailable()) {
                            recreate();
                        } else {
                            showNoNetworkDialog();
                        }
                    }
                })
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
    }


    public void toRegister(View view) {
        Log.d(TAG, "clicked!");
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    public void toLogin(View view) {
        Log.d(TAG, "clicked!");
        Intent intent = new Intent(this, LoginMy.class);
        startActivity(intent);
    }
}