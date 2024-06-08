package com.example.coconote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

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

public class LoginMy extends AppCompatActivity {

    private static final String TAG = "LoginMy";

    // thread related
    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // network related
    private OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // layout related
    private EditText usernameInput;
    private EditText passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_my);

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

    public void login(View view) {
        String username = usernameInput.getText().toString();
        String password = passwordInput.getText().toString();

        doAsyncCode(() -> setLoginRequest(username, password));
    }

    private void setLoginRequest(String username, String password){
        runOnUiThread(() -> {
            Toast.makeText(LoginMy.this, "Logging in", Toast.LENGTH_LONG).show();
        });

        // construct request body
        String url = GlobalConfig.getInstance().getBaseUrl() + "/api/users/login";
        JSONObject params = new JSONObject();
        try{
            params.put("username", username);
            params.put("password", password);
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

        // send request
        call.enqueue(new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                int statusCode = response.code();
                if(statusCode == 200) {
                    String responseData = response.body().string();
                    try {
                        JSONObject responseJson = new JSONObject(responseData);
                        String message = responseJson.getString("message");
                        JSONObject userInfo = responseJson.getJSONObject("user");
                        String userId = userInfo.getString("id");
                        String username = userInfo.getString("username");
                        String email = userInfo.getString("email");
                        String avatar = userInfo.getString("avatar");
                        String nickname = userInfo.getString("nickname");
                        String signature = userInfo.getString("signature");

                        User user = User.getInstance();
                        user.setUsername(username);
                        user.setUserEmail(email);
                        user.setUserNickname(nickname);
                        user.setUserId(userId);
                        user.setUserSignature(signature);

                        runOnUiThread(() -> {
                            // 显示响应消息
                            Toast.makeText(LoginMy.this, message, Toast.LENGTH_LONG).show();

                        });
                    } catch (JSONException e) {
                        runOnUiThread(() -> {
                            // 显示响应消息
                            Toast.makeText(LoginMy.this, e.toString(), Toast.LENGTH_LONG).show();

                        });
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, e.toString());
            }
        });
    }
}