package com.example.coconote;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
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
    private ImageView avatarImageView;
    private Uri imageUri = null;   // avatarUri
    private String email;

    private static final int PICK_IMAGE_REQUEST = 1;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailInput = findViewById(R.id.email_input);
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        avatarImageView = findViewById(R.id.avatar_img);
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
        email = emailInput.getText().toString();
        String username = usernameInput.getText().toString();
        String password = passwordInput.getText().toString();

        if(email.equals("") || username.equals("") || password.equals("")){
            Toast.makeText(RegisterActivity.this, "请提供邮件、用户名和密码", Toast.LENGTH_LONG).show();
            return;
        }

        if(imageUri == null || Uri.EMPTY.equals(imageUri)){
            Toast.makeText(RegisterActivity.this, "请上传头像", Toast.LENGTH_LONG).show();
            return;
        }

        // 在这里处理注册逻辑，例如验证输入，发送数据到服务器等
        // 这里只是显示一个简单的Toast作为示例

        doAsyncCode(() -> sendRegisterRequest(email, username, password));
    }

    private void sendRegisterRequest(String email, String username, String password) {
        byte[] imageBytes;
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            assert inputStream != null;
            imageBytes = getBytes(inputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        RequestBody imageBody = RequestBody.create(
                MediaType.parse("image/jpeg"),
                imageBytes
        );
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
        RequestBody jsonBody = RequestBody.create(params_str, JSON);
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", email+"avatar.jpg", imageBody)
                .addFormDataPart("json", params_str)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        Call call = client.newCall(request);

        call.enqueue(new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code()==201){
                    String responseData = response.body().string();
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(responseData);
                        String message = jsonObject.getString("message");
                        Log.d(TAG, "message:" + message);

                        runOnUiThread(() -> {
                            // 显示响应消息
                            Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
                            // 处理状态
                            emailInput.setText("");
                            usernameInput.setText("");
                            passwordInput.setText("");
                            imageUri = null;
                            Glide.with(RegisterActivity.this).clear(avatarImageView);
                            Intent intent = new Intent(RegisterActivity.this, LoginMy.class);
                            startActivity(intent);
                        });
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    runOnUiThread(() -> {
                        // 显示响应消息
                        Toast.makeText(RegisterActivity.this, "Register Failed", Toast.LENGTH_LONG).show();
                        // 处理状态
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    // 显示响应消息
                    Toast.makeText(RegisterActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                    // 处理状态
                });
            }
        });
    }

    public void uploadAvatar(View view){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            this.imageUri = imageUri;
            Glide.with(this)
                    .load(imageUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .circleCrop()
                    .into(avatarImageView);
        }
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}
