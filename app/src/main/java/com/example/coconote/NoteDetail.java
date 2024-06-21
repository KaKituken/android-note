package com.example.coconote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.wasabeef.richeditor.RichEditor;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NoteDetail extends AppCompatActivity {
    final private int PICK_IMAGE_REQUEST = 1;
    final private int PICK_AUDIO_REQUEST = 2;

    final private String TAG = "NoteDetail";
    private int mediaCount = 0;

    private boolean createNewNote;

    private RichEditor mEditor;

    // thread related
    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // network related
    private OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // note related
    private int noteId;
    private String title;
    private Date noteCreatedAt;
    private Date noteUpdateAt;
    private String content;
    private ArrayList<String> tags;

    // Views
    private EditText titleView;
    private LinearLayout tagsContainer;
    private TextView timeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        // save views
        titleView = findViewById(R.id.noteTitleDetail);
        tagsContainer = findViewById(R.id.tagLinearContainer);
        timeView = findViewById(R.id.note_edit_time);
        mEditor = (RichEditor) findViewById(R.id.editor);
        mEditor.setEditorFontSize(22);
        mEditor.setEditorFontColor(Color.BLACK);
        mEditor.setEditorBackgroundColor(Color.WHITE);
        //mEditor.setBackgroundColor(Color.BLUE);
        //mEditor.setBackgroundResource(R.drawable.bg);
        mEditor.setPadding(10, 10, 10, 10);
        //mEditor.setBackground("https://raw.githubusercontent.com/wasabeef/art/master/chip.jpg");
        mEditor.setPlaceholder("Insert text here...");
        //mEditor.setInputEnabled(false);

        mEditor.setOnTextChangeListener(new RichEditor.OnTextChangeListener() {
            @Override
            public void onTextChange(String text) {
                content = text;
                Log.d(TAG, "onTextChange: " + text);
            }
        });

        findViewById(R.id.undo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.undo();
            }
        });

        findViewById(R.id.redo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.redo();
            }
        });

        findViewById(R.id.align_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setAlignLeft();
            }
        });

        findViewById(R.id.align_center).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setAlignCenter();
            }
        });

        findViewById(R.id.align_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setAlignRight();
            }
        });

        findViewById(R.id.h_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(1);
            }
        });

        findViewById(R.id.h_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(2);
            }
        });

        findViewById(R.id.h_3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(3);
            }
        });

        findViewById(R.id.bold).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setBold();
            }
        });

        findViewById(R.id.italic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setItalic();
            }
        });

        findViewById(R.id.underline).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setUnderline();
            }
        });

        findViewById(R.id.list_unordered).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setBullets();
            }
        });

        findViewById(R.id.list_ordered).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setNumbers();
            }
        });

        findViewById(R.id.audio).setOnClickListener(v ->  {
            insertAudio();
        });

        findViewById(R.id.editor_image).setOnClickListener(v -> {
            insertImage();
        });

        this.noteId = getIntent().getIntExtra("NOTE_ID", 0);
        this.createNewNote = getIntent().getBooleanExtra("CREATE_NOTE", false);
        if(!this.createNewNote){
            doAsyncCode(this::getNoteDetails);
        }
        else {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.getDefault());
            this.noteCreatedAt = new Date();
            String currentTime = sdf.format(new Date());

            // 将当前时间设置到TextView中
            timeView.setText(currentTime);
            this.tags = new ArrayList<>();
            // 设置可见性
            ImageView trash = findViewById(R.id.delete_note);
            trash.setVisibility(View.INVISIBLE);
        }
    }

    // 封装的异步执行方法
    private void doAsyncCode(Runnable task) {
        executorService.submit(task);
    }

    // 封装的主线程UI更新方法
    private void doOnUiCode(Runnable task) {
        mainHandler.post(task);
    }

    public void insertImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    public void insertAudio() {
        Intent intent = new Intent();
        intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Audio"), PICK_AUDIO_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            doAsyncCode(() -> uploadImageToBackend(imageUri));
        } else if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri audioUri = data.getData();
            doAsyncCode(() -> uploadAudioToBackend(audioUri));
        }
    }

    private void uploadImageToBackend(Uri imageUri){
        byte[] imageBytes;
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            assert inputStream != null;
            imageBytes = Utils.getBytes(inputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        @SuppressLint("DefaultLocale") String url = String.format("%s/api/users/%d/notes/%d/media",
                GlobalConfig.getInstance().getBaseUrl(),
                User.getInstance().getUserId(),
                this.noteId);

        RequestBody imageBody = RequestBody.create(
                MediaType.parse("image/jpeg"),
                imageBytes
        );
        @SuppressLint("DefaultLocale") String imageName = String.format("image_%d_%d_%d.jpg",
                User.getInstance().getUserId(),
                this.noteId,
                this.mediaCount);
        this.mediaCount++;
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("media",
                        imageName, imageBody)
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
                    Toast.makeText(NoteDetail.this, e.toString(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code() == 201){
                    String responseData = response.body().string();
                    try {
                        JSONObject responseJson = new JSONObject(responseData);
                        String imageFilename = responseJson.getString("file_name");
                        String imageUrlShow = GlobalConfig.getInstance().getBaseUrl() +
                                "/api/media/uploads/image/" + imageFilename;
                        runOnUiThread(()->{
                            // insert image here
                            mEditor.insertImage(imageUrlShow, "image", 50);
                        });
                    }
                    catch (JSONException e){
                        runOnUiThread(()->{
                            runOnUiThread(()->{
                                Toast.makeText(NoteDetail.this, e.toString(), Toast.LENGTH_SHORT).show();
                            });
                        });
                    }
                }
                else {
                    runOnUiThread(()->{
                        runOnUiThread(()->{
                            Toast.makeText(NoteDetail.this, "上传多媒体失败", Toast.LENGTH_SHORT).show();
                        });
                    });
                }
            }
        });
    }

    private void uploadAudioToBackend(Uri audioUri) {
        byte[] audioBytes;
        try {
            InputStream inputStream = getContentResolver().openInputStream(audioUri);
            assert inputStream != null;
            audioBytes = Utils.getBytes(inputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        @SuppressLint("DefaultLocale") String url = String.format("%s/api/users/%d/notes/%d/media",
                GlobalConfig.getInstance().getBaseUrl(),
                User.getInstance().getUserId(),
                this.noteId);

        RequestBody audioBody = RequestBody.create(
                MediaType.parse("audio/mpeg"),
                audioBytes
        );
        @SuppressLint("DefaultLocale") String audioName = String.format("audio_%d_%d_%d.mp3",
                User.getInstance().getUserId(),
                this.noteId,
                this.mediaCount);
        this.mediaCount++;
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("media",
                        audioName, audioBody)
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
                    Toast.makeText(NoteDetail.this, e.toString(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code() == 201){
                    String responseData = response.body().string();
                    try {
                        JSONObject responseJson = new JSONObject(responseData);
                        String audioFilename = responseJson.getString("file_name");
                        String audioUrlShow = GlobalConfig.getInstance().getBaseUrl() +
                                "/api/media/uploads/audio/" + audioFilename;
                        runOnUiThread(()->{
                            // insert audio here
                            mEditor.insertAudio(audioUrlShow);
                        });
                    }
                    catch (JSONException e){
                        runOnUiThread(()->{
                            runOnUiThread(()->{
                                Toast.makeText(NoteDetail.this, e.toString(), Toast.LENGTH_SHORT).show();
                            });
                        });
                    }
                }
                else {
                    runOnUiThread(()->{
                        runOnUiThread(()->{
                            Toast.makeText(NoteDetail.this, "上传多媒体失败", Toast.LENGTH_SHORT).show();
                        });
                    });
                }
            }
        });
    }

    private void getNoteDetails(){
        int userID = User.getInstance().getUserId();
        String url = GlobalConfig.getInstance().getBaseUrl() + "/api/users/" + Integer.toString(userID)
                + "/notes/" + Integer.toString(this.noteId);
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = client.newCall(request);
        // send request
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                {
//                    'id': note.id,
//                        'title': note.title,
//                        'content': note.content,
//                        'created_at': note.created_at,
//                        'updated_at': note.updated_at,
//                        'tags': note.tags,
//                        'folder_id': note.folder_id,
//                        'media': media
//                }
                int statusCode = response.code();
                Log.d(TAG, "onResponse: statusCode = " + Integer.toString(statusCode));
                if(statusCode == 200){
                    String responseData = response.body().string();
                    JSONObject responseJson = null;
                    try {
                        responseJson = new JSONObject(responseData);
                        content = responseJson.getString("content");
                        title = responseJson.getString("title");
                        noteCreatedAt = null;
                        noteUpdateAt = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.getDefault());
                            noteCreatedAt = sdf.parse(responseJson.getString("created_at"));
                            noteUpdateAt = sdf.parse(responseJson.getString("updated_at"));
                        }
                        JSONArray jsonTags = responseJson.getJSONArray("tags");
                        tags = new ArrayList<>();
                        for(int i = 0; i < jsonTags.length(); i++){
                            tags.add(jsonTags.getString(i));
                        }
                        doOnUiCode(() -> {
                            titleView.setText(title);
                            mEditor.setHtml(content);
                            for(String tag: tags){
                                NoteDetail.this.addTagToLayout(tag);
                            }
                            timeView.setText(noteUpdateAt.toString());
                        });
                    }
                    catch (JSONException e){
                        runOnUiThread(() -> {
                            // 显示响应消息
                            Log.e(TAG, "onResponse: " + e.toString());
                            Toast.makeText(NoteDetail.this, e.toString(), Toast.LENGTH_LONG).show();
                        });
                    } catch (ParseException e) {
                        Log.e(TAG, "onResponse: " + e.toString());
                        throw new RuntimeException(e);
                    }

                }
                else {
                    runOnUiThread(() -> {
                        // 显示响应消息
                        Toast.makeText(NoteDetail.this, "404 Not Fond", Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    // 显示响应消息
                    Toast.makeText(NoteDetail.this, e.toString(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    public void showAddTagDialog(View v) {
        // 创建一个 AlertDialog.Builder 对象
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Tag");

        // 使用 LayoutInflater 创建一个 EditText 视图
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.add_tag_window, null);
        dialogView.setMinimumHeight(400);
        builder.setView(dialogView);

        // 找到 EditText 视图
        EditText input = dialogView.findViewById(R.id.editTextTag);

        // 设置对话框按钮
        builder.setPositiveButton("Add", (dialog, which) -> {
            String tag = input.getText().toString();
            if (!tag.isEmpty()) {
                tags.add(tag);
                addTagToLayout(tag);
            } else {
                Toast.makeText(this, "Tag cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        // 显示对话框
        builder.show();
    }

    private void addTagToLayout(String tag){
        // 创建一个新的 TextView 作为标签
        TextView tagView = new TextView(this);
        tagView.setText(tag);
        tagView.setBackgroundResource(R.drawable.button_small_radius_green);
        tagView.setPadding(8, 4, 8, 4);
        tagView.setGravity(Gravity.CENTER);
        tagView.setPadding(30, 0, 30, 0);
        tagView.setTextColor(Color.WHITE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                80
        );
        params.setMargins(0, 8, 16, 8);
        params.gravity = Gravity.CENTER;

        tagView.setLayoutParams(params);

        // 设置长按监听器
        tagView.setOnLongClickListener(v -> {
            // 显示确认删除对话框
            new AlertDialog.Builder(this)
                    .setTitle("确认删除")
                    .setMessage("确定要删除此标签吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        // 从父布局中移除标签
                        ((LinearLayout) v.getParent()).removeView(v);
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        // 添加标签到 LinearLayout
        LinearLayout tagContainer = findViewById(R.id.tagLinearContainer);
        // 获取当前子视图的数量
        int childCount = tagContainer.getChildCount();

        // 将新的标签添加到倒数第二个位置
        tagContainer.addView(tagView, childCount - 1);
    }

    public void backAndSave(View view){
        Toast.makeText(this, "笔记保存中，正在生成摘要", Toast.LENGTH_SHORT).show();
        title = titleView.getText().toString();
        doAsyncCode(this::backAndSaveRequest);
    }

    @SuppressLint("DefaultLocale")
    private void backAndSaveRequest(){
        // construct request body
        String url;
        if(createNewNote){
            url = GlobalConfig.getInstance().getBaseUrl() +
                    String.format("/api/users/%d/notes", User.getInstance().getUserId());
        } else {
            url = GlobalConfig.getInstance().getBaseUrl() +
                    String.format("/api/users/%d/notes/%d", User.getInstance().getUserId(), noteId);
        }

        JSONObject params = new JSONObject();
        try{
            params.put("content", content);
            params.put("title", title);
            JSONArray tagsArray = new JSONArray(tags);
            params.put("tags", tagsArray);
        } catch (JSONException e) {
            Log.d(TAG, "error" + e.toString());
        }
        String params_str = params.toString();
        RequestBody body = RequestBody.create(params_str, JSON);
        Request request;
        if(createNewNote){
            request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
        }
        else {
            request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .build();
        }

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                doOnUiCode(()->{
                    Toast.makeText(NoteDetail.this, e.toString(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code() == 201 && createNewNote){
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("note_result", "保存笔记成功");
                    setResult(RESULT_OK, resultIntent);
                    finish(); //
                } else if (response.code() == 200 && !createNewNote) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("note_result", "保存笔记成功");
                    setResult(RESULT_OK, resultIntent);
                    finish(); //
                } else {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("note_result", "保存笔记失败");
                    setResult(RESULT_CANCELED, resultIntent);
                    runOnUiThread(()->{
                        Toast.makeText(NoteDetail.this, "保存笔记失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });


    }


    public void deleteNote(View view) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除此笔记吗？")
                .setPositiveButton("确定", (dialog, which) -> doAsyncCode(this::deleteItem))
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteItem() {
        int userID = User.getInstance().getUserId();
        String url = GlobalConfig.getInstance().getBaseUrl() + "/api/users/" + Integer.toString(userID)
                + "/notes/" + Integer.toString(this.noteId);
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    // 显示响应消息
                    Toast.makeText(NoteDetail.this, e.toString(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code() == 200){
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("note_result", "保存删除成功");
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            }
        });
    }
}