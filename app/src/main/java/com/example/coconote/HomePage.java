package com.example.coconote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.coconote.data.NoteComponentData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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

    private View popupContainerView;
    private RecyclerView recyclerView;

    private List<NoteComponentData> mNoteData;
    private List<String> mAllTags;
    private String mTagChosen;
    private LinearLayout buttonContainer;

    // thread related
    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // network related
    private OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    final private String TAG = "HomePage";

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

        // note recycler view
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
        Integer userID = User.getInstance().getUserId();
        String url = GlobalConfig.getInstance().getBaseUrl() + "/api/users/" + userID.toString()
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
                        LocalDateTime noteCreatedAt = null;
                        LocalDateTime noteUpdateAt = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            Log.d(TAG, "onResponse: entered the if");
                            noteCreatedAt = LocalDateTime.parse(jsonObject.getString("created_at"));
                            noteUpdateAt = LocalDateTime.parse(jsonObject.getString("updated_at"));
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
                        int folderId = jsonObject.getInt("folder_id");
                        NoteComponentData data = new NoteComponentData(noteId, noteTitle,
                                summary, noteCreatedAt.toString(), tags);
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
            }
        });
    }

    // 假设你有一个方法根据标签过滤数据
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
}