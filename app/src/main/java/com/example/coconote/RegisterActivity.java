package com.example.coconote;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;


public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    // thread related
    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // network related
    private OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // layout related
    private EditText emailInput;
    private EditText usernameInput;
    private EditText passwordInput;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailInput = findViewById(R.id.email_input);
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
    }

    // 封装的异步执行方法
    private void doAsyncCode(Runnable task) {
        executorService.submit(task);
    }

    // 封装的主线程UI更新方法
    private void doOnUiCode(Runnable task) {
        mainHandler.post(task);
    }

    public void register(View view) {
        String email = emailInput.getText().toString();
        String username = usernameInput.getText().toString();
        String password = passwordInput.getText().toString();

        // 在这里处理注册逻辑，例如验证输入，发送数据到服务器等
        // 这里只是显示一个简单的Toast作为示例

        doAsyncCode(() -> sendRegisterRequest(email, username, password));
    }

    private void sendRegisterRequest(String email, String username, String password) {
        runOnUiThread(() -> {
            Toast.makeText(RegisterActivity.this, "Registering", Toast.LENGTH_LONG).show();
        });
        String url = GlobalConfig.getInstance().getBaseUrl() + "/api/users/register";
        JSONObject params = new JSONObject();
        try{
            params.put("username", username);
            params.put("password", password);
            params.put("email", email);
        } catch (JSONException e) {
            Log.d(TAG, "error" + e.toString());
        }
        String params_str = params.toString();
        RequestBody body = RequestBody.create(params_str, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Call call = client.newCall(request);

        call.enqueue(new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body().string();
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(responseData);
                    String message = jsonObject.getString("message");
                    boolean status = jsonObject.getBoolean("status");
                    Log.d(TAG, "message:" + message);
                    Log.d(TAG, "status:" + status);

                    runOnUiThread(() -> {
                        // 显示响应消息
                        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
                        // 处理状态
                        if (status) {
                            // 注册成功逻辑
                        } else {
                            // 注册失败逻辑
                        }
                    });
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, e.toString());
            }
        });
    }

    public void uploadAvatar(View view){

    }
}
