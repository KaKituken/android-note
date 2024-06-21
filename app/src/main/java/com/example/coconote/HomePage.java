package com.example.coconote;

import android.Manifest;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.example.coconote.data.NoteComponentData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomePage extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_SMS = 100;

    private ActivityResultLauncher<Intent> noteActivityResultLauncher;

    private View popupContainerView;
    private RecyclerView recyclerView;

    static private List<NoteComponentData> mNoteData;
    private static List<String> mAllTags;
    private String mTagChosen;
    private LinearLayout buttonContainer;

    // thread related
    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // network related
    private OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    final private String TAG = "HomePage";

    private ListView smsListView;
    private List<String> smsList;
    private List<String> smsBodyList;

    public HomePage() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        ImageView expandButton = findViewById(R.id.expand_icon);

        PopupFragment popupFragment = PopupFragment.newInstance("param1", "param2");

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.popupContainerView, popupFragment);
        fragmentTransaction.commit();

        // 设置可见性
        popupContainerView = findViewById(R.id.popupContainerView);
        popupContainerView.setVisibility(View.INVISIBLE);

        // get all notes
        recyclerView = findViewById(R.id.notesRecycler);
        buttonContainer = findViewById(R.id.categoryLinear);
        mNoteData = new ArrayList<>();
        mAllTags = new ArrayList<>();
        mTagChosen = "All";
        doAsyncCode(this::getAllNote);

        // 注册启动器并定义其结果处理程序
        noteActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String noteResult = result.getData().getStringExtra("note_result");
                        Toast.makeText(this, "Note Result: " + noteResult, Toast.LENGTH_SHORT).show();

                        //refresh
                        mNoteData.clear();
                        mAllTags.clear();
                        mTagChosen = "All";
                        buttonContainer.removeAllViews();
                        doAsyncCode(this::getAllNote);
                    }
                }
        );

        smsListView = findViewById(R.id.smsListView);
        smsList = new ArrayList<>();
        smsBodyList = new ArrayList<>();

        smsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String smsContent = smsBodyList.get(position);
                smsListView.setVisibility(View.INVISIBLE);
                smsList.clear();
                smsBodyList.clear();
                doAsyncCode(() -> {
                    sendSMSNewNoteRequest(smsContent);
                });
            }
        });

        smsListView.setVisibility(View.INVISIBLE);
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

    private void getAllNote(){
        int userID = User.getInstance().getUserId();
        String url = GlobalConfig.getInstance().getBaseUrl() + "/api/users/" + Integer.toString(userID)
                + "/notes";
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = client.newCall(request);

        // send request
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    // 显示响应消息
                    Toast.makeText(HomePage.this, e.toString(), Toast.LENGTH_LONG).show();
                });
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                int statusCode = response.code();
                if(statusCode == 200){
                    String responseData = response.body().string();
                    JSONArray responseJson = null;
                    try {
                        responseJson = new JSONArray(responseData);
                        for(int i = 0; i < responseJson.length(); i++){
                            JSONObject jsonObject = responseJson.getJSONObject(i);
                            int noteId = jsonObject.getInt("id");
                            String noteTitle = jsonObject.getString("title");
                            String noteContent = jsonObject.getString("content");
                            String summary = jsonObject.getString("summary");
                            Log.d(TAG, "onResponse: " + noteContent.toString());
//                            Date noteCreatedAt = null;
//                            Date noteUpdateAt = null;
                            String noteCreatedAt = "";
                            String noteUpdateAt = "";
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                Log.d(TAG, "onResponse: entered the if");
//                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
//                                noteCreatedAt = sdf.parse(jsonObject.getString("created_at"));
//                                noteUpdateAt = sdf.parse(jsonObject.getString("updated_at"));
                                noteCreatedAt = jsonObject.getString("created_at");
                                noteUpdateAt = jsonObject.getString("updated_at");
                            }
                            JSONArray tagsArray = jsonObject.getJSONArray("tags");
                            List<String> tags = new ArrayList<>();

                            // TODO: create a recyclerview for mAllTags
                            for (int j = 0; j < tagsArray.length(); j++) {
                                String tag = tagsArray.getString(j);
                                tags.add(tagsArray.getString(j));
                                if(!mAllTags.contains(tag)) {
                                    Log.d(TAG, "onResponse: " + mAllTags.toString());
                                    mAllTags.add(tag);
                                }
                            }
                            JSONArray mediaArray = jsonObject.getJSONArray("media");
//                            int folderId = jsonObject.getInt("folder_id");
                            NoteComponentData data = new NoteComponentData(noteId, noteTitle,
                                    summary, noteCreatedAt, tags);
                            mNoteData.add(data);
                        }
                        doOnUiCode(() -> {
                            NoteRecyclerViewAdapter adapter = new NoteRecyclerViewAdapter(HomePage.this, mNoteData);
                            recyclerView.setAdapter(adapter);
                            recyclerView.setLayoutManager(new LinearLayoutManager(HomePage.this));
                            // firstly, add a 'All' tag
                            Button allButton = new Button(HomePage.this);
                            allButton.setText("All");
                            allButton.setBackgroundResource(R.drawable.button_small_radius);
                            allButton.setTextColor(getResources().getColor(R.color.white));
                            allButton.setOnClickListener(v -> {
                                // deactivate all button
                                int childCount = buttonContainer.getChildCount();
                                for(int i = 0; i < childCount; i++){
                                    Button btn = (Button)buttonContainer.getChildAt(i);
                                    btn.setBackgroundResource(R.drawable.button_small_radius_gray);
                                    btn.setTextColor(getResources().getColor(R.color.black));
                                }
                                // activate myself
                                Button clickedButton = (Button) v;
                                clickedButton.setBackgroundResource(R.drawable.button_small_radius);
                                clickedButton.setTextColor(getResources().getColor(R.color.white));
                                List<NoteComponentData> filteredData = filterDataByTag("All");
                                adapter.updateData(filteredData);
                            });
                            buttonContainer.addView(allButton);
                            for (String text : mAllTags) {
                                Button button = new Button(HomePage.this);
                                button.setText(text);
                                button.setShadowLayer(0, 0, 0, 0);
                                button.setBackgroundResource(R.drawable.button_small_radius_gray);
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                params.setMargins(20, 0, 0, 0); // 左，上，右，下的边距

                                button.setLayoutParams(params);

                                button.setOnClickListener(v -> {
                                    // deactivate all button
                                    int childCount = buttonContainer.getChildCount();
                                    for(int i = 0; i < childCount; i++){
                                        Button btn = (Button)buttonContainer.getChildAt(i);
                                        btn.setBackgroundResource(R.drawable.button_small_radius_gray);
                                        btn.setTextColor(getResources().getColor(R.color.black));
                                    }
                                    // activate myself
                                    Button clickedButton = (Button) v;
                                    clickedButton.setBackgroundResource(R.drawable.button_small_radius);
                                    clickedButton.setTextColor(getResources().getColor(R.color.white));
                                    List<NoteComponentData> filteredData = filterDataByTag(text);
                                    adapter.updateData(filteredData);
                                });

                                buttonContainer.addView(button);
                            }
                        });

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
//                    catch (ParseException e) {
//                        throw new RuntimeException(e);
//                    }
                }
                else{
                    runOnUiThread(() -> {
                        // 显示响应消息
                        Toast.makeText(HomePage.this, "404 Not Fond", Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private List<NoteComponentData> filterDataByTag(String tag) {
        if(Objects.equals(tag, "All")) {
            return mNoteData;
        }
        List<NoteComponentData> filteredData = new ArrayList<>();
        for (NoteComponentData note : mNoteData) {
            if (note.getTags().contains(tag)) {
                filteredData.add(note);
            }
        }
        return filteredData;
    }

    private List<NoteComponentData> filterDataById(List<Integer> idList) {
        List<NoteComponentData> filteredData = new ArrayList<>();
        for (NoteComponentData note : mNoteData) {
            if (idList.contains(note.getId())) {
                filteredData.add(note);
            }
        }
        return filteredData;
    }

    public static void computeTagList(){
        mAllTags.clear();
        for(NoteComponentData data: mNoteData){
            List<String> tags = data.getTags();
            for (int i = 0; i < tags.size(); i++) {
                String tag = tags.get(i);
                if(!mAllTags.contains(tag)) {
//                    Log.d(TAG, "onResponse: " + mAllTags.toString());
                    mAllTags.add(tag);
                }
            }
        }
    }

    public void newNote(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择创建笔记方式")
                .setItems(new CharSequence[]{"从短信", "手动输入"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                // 从短信创建笔记的逻辑
                                checkAndRequestSmsPermission();
                                Toast.makeText(HomePage.this, "选择了从短信创建笔记", Toast.LENGTH_SHORT).show();
                                break;
                            case 1:
                                // 手动输入创建笔记的逻辑
                                newNoteManually();
                                break;
                        }
                    }
                });
        builder.create().show();
    }

    private void newNoteManually(){
        Intent intent = new Intent(this, NoteDetail.class);
        intent.putExtra("CREATE_NOTE", true); // 传递笔记ID
        noteActivityResultLauncher.launch(intent);
    }

    // 请求读取短信的权限
    private void checkAndRequestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            // 如果没有权限，请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, PERMISSIONS_REQUEST_READ_SMS);
        } else {
            // 已有权限，可以继续执行读取 SMS 的操作
            importSmsContent();
        }
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                importSmsContent();
            } else {
                Toast.makeText(this, "Permission denied to read SMS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void importSmsContent() {
        smsListView.setVisibility(View.VISIBLE);
        Log.d(TAG, "importSmsContent: here");
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                smsList.add("From: " + address + "\n" + body);
                smsBodyList.add(body);
            } while (cursor.moveToNext());

            cursor.close();

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, smsList);
            smsListView.setAdapter(adapter);
        }
    }

    public void sendSMSNewNoteRequest(String smsContent){
        // construct request body
        String url = GlobalConfig.getInstance().getBaseUrl() + "/api/users/" +
                Integer.toString(User.getInstance().getUserId()) + "/sms";
        JSONObject params = new JSONObject();
        try{
            params.put("sms_content", smsContent);

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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(()->{
                    Toast.makeText(HomePage.this, e.toString(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code() == 201){
                    //refresh
                    getAllNote();
                    runOnUiThread(()->{
                        mNoteData.clear();
                        mAllTags.clear();
                        mTagChosen = "All";
                        buttonContainer.removeAllViews();
                        Toast.makeText(HomePage.this, "上传短信成功", Toast.LENGTH_SHORT).show();
                    });
                }
                else {
                    runOnUiThread(()->{
                        Toast.makeText(HomePage.this, "上传短信失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }


    public void openNoteActivity(NoteComponentData noteData) {
        Intent intent = new Intent(this, NoteDetail.class);
        intent.putExtra("NOTE_ID", noteData.getId());
        intent.putExtra("CREATE_NOTE", false); // 传递笔记ID
        noteActivityResultLauncher.launch(intent);
    }

    public void LLMSearch(View view) {
        // 创建AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search for");

        // 创建一个LinearLayout来放置输入框
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // 创建输入框
        final EditText searchInput = new EditText(this);
        searchInput.setHint("Enter whatever you want");
        layout.addView(searchInput);

        builder.setView(layout);

        // 设置确认和取消按钮
        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String signature = searchInput.getText().toString();
            doAsyncCode(()->{
                LLMSearchRequest(signature);
            });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        // 显示弹窗
        builder.show();
    }

    private void LLMSearchRequest(String keyword){
        // construct request body
        String url = GlobalConfig.getInstance().getBaseUrl() + "/api/users/" +
                Integer.toString(User.getInstance().getUserId()) + "/notes/fuzzy_search?query=" +
                keyword;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(()->{
                    Toast.makeText(HomePage.this, e.toString(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code() == 200){
                    String responseData = response.body().string();
                    List<Integer> filterId = new ArrayList<>();
                    try {
                        JSONObject responseJson = new JSONObject(responseData);
                        JSONArray idArray = responseJson.getJSONArray("results");
                        for(int i = 0; i < idArray.length(); i++){
                            JSONObject idObject = idArray.getJSONObject(i);
                            filterId.add(idObject.getInt("id"));
                        }
                    }
                    catch (JSONException e){
                        runOnUiThread(()->{
                            Toast.makeText(HomePage.this, e.toString(), Toast.LENGTH_SHORT).show();

                        });
                    }
                    runOnUiThread(() -> {
                        Toast.makeText(HomePage.this, "筛选成功", Toast.LENGTH_SHORT).show();
                        List<NoteComponentData> filteredData = filterDataById(filterId);
                        NoteRecyclerViewAdapter adapter = new NoteRecyclerViewAdapter(HomePage.this, mNoteData);
                        adapter.updateData(filteredData);
                        recyclerView.setAdapter(adapter);
                    });
                }
                else {
                    runOnUiThread(()->{
                        Toast.makeText(HomePage.this, "签名修改失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    public void resetBtn(){
        buttonContainer.removeAllViews();
        NoteRecyclerViewAdapter adapter = new NoteRecyclerViewAdapter(HomePage.this, mNoteData);
        // firstly, add a 'All' tag
        Button allButton = new Button(HomePage.this);
        allButton.setText("All");
        allButton.setBackgroundResource(R.drawable.button_small_radius);
        allButton.setTextColor(getResources().getColor(R.color.white));
        allButton.setOnClickListener(v -> {
            // deactivate all button
            int childCount = buttonContainer.getChildCount();
            for(int i = 0; i < childCount; i++){
                Button btn = (Button)buttonContainer.getChildAt(i);
                btn.setBackgroundResource(R.drawable.button_small_radius_gray);
                btn.setTextColor(getResources().getColor(R.color.black));
            }
            // activate myself
            Button clickedButton = (Button) v;
            clickedButton.setBackgroundResource(R.drawable.button_small_radius);
            clickedButton.setTextColor(getResources().getColor(R.color.white));
            List<NoteComponentData> filteredData = filterDataByTag("All");
            adapter.updateData(filteredData);
        });
        buttonContainer.addView(allButton);
        for (String text : mAllTags) {
            Button button = new Button(HomePage.this);
            button.setText(text);
            button.setShadowLayer(0, 0, 0, 0);
            button.setBackgroundResource(R.drawable.button_small_radius_gray);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(20, 0, 0, 0); // 左，上，右，下的边距

            button.setLayoutParams(params);

            button.setOnClickListener(v -> {
                // deactivate all button
                int childCount = buttonContainer.getChildCount();
                for(int i = 0; i < childCount; i++){
                    Button btn = (Button)buttonContainer.getChildAt(i);
                    btn.setBackgroundResource(R.drawable.button_small_radius_gray);
                    btn.setTextColor(getResources().getColor(R.color.black));
                }
                // activate myself
                Button clickedButton = (Button) v;
                clickedButton.setBackgroundResource(R.drawable.button_small_radius);
                clickedButton.setTextColor(getResources().getColor(R.color.white));
                List<NoteComponentData> filteredData = filterDataByTag(text);
                adapter.updateData(filteredData);
            });

            buttonContainer.addView(button);
        }
    }
}