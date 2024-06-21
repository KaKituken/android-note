package com.example.coconote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.coconote.data.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserPage extends AppCompatActivity {

    final private String TAG = "AppCompatActivity";

    private View popupContainerView;
    final private int PICK_IMAGE_REQUEST = 1;
    private ImageView avatarImageView;
    private TextView usernameView;
    private TextView signatureView;

    // thread related
    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // network related
    private OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_page);

        ImageView expandButton = findViewById(R.id.expand_icon);
        avatarImageView = findViewById(R.id.avatar_img);
        usernameView = findViewById(R.id.username_str);

        usernameView.setText(User.getInstance().getUserNickname());

        PopupFragment popupFragment = PopupFragment.newInstance("param1", "param2");

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.popupContainerView, popupFragment);
        fragmentTransaction.commit();

        // 设置可见性
        popupContainerView = findViewById(R.id.popupContainerView);
        popupContainerView.setVisibility(View.INVISIBLE);

        // load avatar
        String avatar = User.getInstance().getUserAvatar();
        String avatar_url = GlobalConfig.getInstance().getBaseUrl() + "/api/media/uploads/avatar/"
                + avatar;
        Glide.with(this)
                .load(avatar_url)
                .diskCacheStrategy(DiskCacheStrategy.NONE) // 禁用磁盘缓存
                .skipMemoryCache(true) // 禁用内存缓存
                .circleCrop()
                .into(avatarImageView);
        Toast.makeText(UserPage.this, avatar_url, Toast.LENGTH_SHORT).show();

        // Signature
        signatureView = findViewById(R.id.signature);
        signatureView.setText(User.getInstance().getUserSignature());
    }

    // 封装的异步执行方法
    private void doAsyncCode(Runnable task) {
        executorService.submit(task);
    }

    // 封装的主线程UI更新方法
    private void doOnUiCode(Runnable task) {
        mainHandler.post(task);
    }


    public void showPopupMenu(View view) {
        popupContainerView.setVisibility(popupContainerView.getVisibility() == View.INVISIBLE ?
                View.VISIBLE : View.INVISIBLE);
    }

    public void editUsername(View view) {
        // 创建弹窗
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.add_tag_window, null);
        builder.setView(dialogView);

        EditText usernameInput = dialogView.findViewById(R.id.editTextTag);

        builder.setTitle("编辑用户昵称")
                .setPositiveButton("保存", (dialog, id) -> {
                    // 保存新用户名
                    String newUsername = usernameInput.getText().toString();
                    if (!newUsername.isEmpty()) {
                        // 保存用户名的逻辑，例如发送请求到服务器或保存到本地数据库
                        doAsyncCode(()->changeUsernameRequest(newUsername));
                    } else {
                        Toast.makeText(UserPage.this, "昵称不能为空", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", (dialog, id) -> dialog.cancel());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void changeUsernameRequest(String newUsername){
        // construct request body
        String url = GlobalConfig.getInstance().getBaseUrl() + "/api/users/" +
                Integer.toString(User.getInstance().getUserId()) + "/nickname";
        JSONObject params = new JSONObject();
        try{
            params.put("nickname", newUsername);
        } catch (JSONException e) {
            Log.d(TAG, "error" + e.toString());
        }
        String params_str = params.toString();
        RequestBody body = RequestBody.create(params_str, JSON);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(()->{
                    Toast.makeText(UserPage.this, e.toString(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code() == 200){
                    User.getInstance().setUserNickname(newUsername);
                    runOnUiThread(()->{
                        usernameView.setText(newUsername);
                        Toast.makeText(UserPage.this, "昵称修改成功", Toast.LENGTH_SHORT).show();
                    });
                }
                else {
                    runOnUiThread(()->{
                        Toast.makeText(UserPage.this, "昵称修改失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    public void changeAvatar(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            doAsyncCode(() -> changeAvatarRequest(imageUri));
        }
    }

    private void changeAvatarRequest(Uri newImageUri){
        byte[] imageBytes;
        try {
            InputStream inputStream = getContentResolver().openInputStream(newImageUri);
            assert inputStream != null;
            imageBytes = Utils.getBytes(inputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        @SuppressLint("DefaultLocale") String url = String.format("%s/api/users/%d/avatar",
                GlobalConfig.getInstance().getBaseUrl(),
                User.getInstance().getUserId());

        RequestBody imageBody = RequestBody.create(
                MediaType.parse("image/jpeg"),
                imageBytes
        );
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("avatar",
                        User.getInstance().getUserEmail()+"avatar.jpg", imageBody)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(()->{
                    Toast.makeText(UserPage.this, e.toString(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code() == 201){
                    String responseData = response.body().string();
                    try {
                        JSONObject responseJson = new JSONObject(responseData);
                        String imageUrl = responseJson.getString("file_name");
                        User.getInstance().setUserAvatar(imageUrl);
                        String imageUrlShow = GlobalConfig.getInstance().getBaseUrl() + "/api/media/uploads/avatar/"
                                + imageUrl;
                        runOnUiThread(()->{
                            Glide.with(UserPage.this).clear(avatarImageView);
                            Glide.with(UserPage.this)
                                    .load(imageUrlShow)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE) // 禁用磁盘缓存
                                    .skipMemoryCache(true) // 禁用内存缓存
                                    .circleCrop()
                                    .into(avatarImageView);
                        });
                        runOnUiThread(()->{
                            runOnUiThread(()->{
                                Toast.makeText(UserPage.this, "更改头像成功", Toast.LENGTH_SHORT).show();
                            });
                        });
                    }
                    catch (JSONException e){
                        runOnUiThread(()->{
                            runOnUiThread(()->{
                                Toast.makeText(UserPage.this, e.toString(), Toast.LENGTH_SHORT).show();
                            });
                        });
                    }
                }
                else {
                    runOnUiThread(()->{
                        runOnUiThread(()->{
                            Toast.makeText(UserPage.this, "更改头像失败", Toast.LENGTH_SHORT).show();
                        });
                    });
                }
            }
        });
    }

    public void changePassword(View view) {
        // 创建AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        // 创建一个LinearLayout来放置输入框
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // 创建原始密码输入框
        final EditText oldPasswordInput = new EditText(this);
        oldPasswordInput.setHint("Enter old password");
        oldPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(oldPasswordInput);

        // 创建新密码输入框
        final EditText newPasswordInput = new EditText(this);
        newPasswordInput.setHint("Enter new password");
        newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(newPasswordInput);

        builder.setView(layout);

        // 设置确认和取消按钮
        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String oldPassword = oldPasswordInput.getText().toString();
            String newPassword = newPasswordInput.getText().toString();
            doAsyncCode(() -> changePasswordRequest(oldPassword, newPassword));
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        // 显示弹窗
        builder.show();
    }

    void changePasswordRequest(String oldPassword, String newPassword){
        // construct request body
        String url = GlobalConfig.getInstance().getBaseUrl() + "/api/users/" +
                Integer.toString(User.getInstance().getUserId()) + "/password";
        JSONObject params = new JSONObject();
        try{
            params.put("old_password", oldPassword);
            params.put("new_password", newPassword);

        } catch (JSONException e) {
            Log.d(TAG, "error" + e.toString());
        }
        String params_str = params.toString();
        RequestBody body = RequestBody.create(params_str, JSON);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(()->{
                    Toast.makeText(UserPage.this, e.toString(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code() == 200){
                    runOnUiThread(()->{
                        Toast.makeText(UserPage.this, "密码修改成功", Toast.LENGTH_SHORT).show();
                    });
                }
                else {
                    runOnUiThread(()->{
                        Toast.makeText(UserPage.this, "密码修改失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }


    public void cancelUser(View view) {
        // 创建AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete User");

        // 创建一个LinearLayout来放置输入框
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // 创建密码输入框
        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter your password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordInput);

        builder.setView(layout);

        // 设置确认和取消按钮
        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String password = passwordInput.getText().toString();
            doAsyncCode(()->{
                cancelUserRequest(password);
            });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        // 显示弹窗
        builder.show();
    }

    void cancelUserRequest(String password){
        // construct request body
        String url = GlobalConfig.getInstance().getBaseUrl() + "/api/users/" +
                Integer.toString(User.getInstance().getUserId());
        JSONObject params = new JSONObject();
        try{
            params.put("password", password);

        } catch (JSONException e) {
            Log.d(TAG, "error" + e.toString());
        }
        String params_str = params.toString();
        RequestBody body = RequestBody.create(params_str, JSON);
        Request request = new Request.Builder()
                .url(url)
                .delete(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(()->{
                    Toast.makeText(UserPage.this, e.toString(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code() == 200){
                    runOnUiThread(()->{
                        Toast.makeText(UserPage.this, "用户注销成功", Toast.LENGTH_SHORT).show();
                        User.getInstance().signOut();
                        Intent intent = new Intent(UserPage.this, MainActivity.class);
                        startActivity(intent);
                    });
                }
                else {
                    runOnUiThread(()->{
                        Toast.makeText(UserPage.this, "用户注销失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    public void changeSignature(View view){
        // 创建AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Signature");

        // 创建一个LinearLayout来放置输入框
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // 创建密码输入框
        final EditText signatureInput = new EditText(this);
        signatureInput.setHint("Enter your new signature");
        layout.addView(signatureInput);

        builder.setView(layout);

        // 设置确认和取消按钮
        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String signature = signatureInput.getText().toString();
            doAsyncCode(()->{
                changeSignatureRequest(signature);
            });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        // 显示弹窗
        builder.show();
    }

    public void changeSignatureRequest(String signature){
        // construct request body
        String url = GlobalConfig.getInstance().getBaseUrl() + "/api/users/" +
                Integer.toString(User.getInstance().getUserId()) + "/signature";
        JSONObject params = new JSONObject();
        try{
            params.put("signature", signature);

        } catch (JSONException e) {
            Log.d(TAG, "error" + e.toString());
        }
        String params_str = params.toString();
        RequestBody body = RequestBody.create(params_str, JSON);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(()->{
                    Toast.makeText(UserPage.this, e.toString(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code() == 200){
                    User.getInstance().setUserSignature(signature);
                    runOnUiThread(()->{
                        Toast.makeText(UserPage.this, "签名修改成功", Toast.LENGTH_SHORT).show();
                        signatureView.setText(signature);
                    });
                }
                else {
                    runOnUiThread(()->{
                        Toast.makeText(UserPage.this, "签名修改失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

}