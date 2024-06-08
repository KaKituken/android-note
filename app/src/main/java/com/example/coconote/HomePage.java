package com.example.coconote;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.coconote.data.NoteComponentData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomePage extends AppCompatActivity {

    private View popupContainerView;

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

        // 获取按钮容器
        LinearLayout buttonContainer = findViewById(R.id.categoryLinear);
        List<String> buttonTexts = Arrays.asList("ab", "sss", "example", "test");
        for (String text : buttonTexts) {
            Button button = new Button(this);
            button.setText(text);
            button.setShadowLayer(0, 0, 0, 0);
            button.setBackgroundResource(R.drawable.button_small_radius_gray);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(20, 0, 0, 0); // 左，上，右，下的边距

            button.setLayoutParams(params);

            buttonContainer.addView(button);
        }

        // note recycler view
        RecyclerView recyclerView = findViewById(R.id.notesRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<NoteComponentData> data = new ArrayList<>();
        List<String> items1 = new ArrayList<>();
        items1.add("Item 1");
        items1.add("Item 2");

        List<String> items2 = new ArrayList<>();
        items2.add("Item 3");
        items2.add("Item 4");

        data.add(new NoteComponentData("Course 233 discussion",
                "To do list:\n1.Me : design the interface and conduct user experiments by Tuesday.",
                "3.21 Tuesday", items1));
        data.add(new NoteComponentData("Course 234 discussion",
                "To do list:\n1.Me : complete the assignment by Monday.",
                "3.22 Friday",
                items2));
        data.add(new NoteComponentData("Course 233 discussion",
                "To do list:\n1.Me : design the interface and conduct user experiments by Tuesday.",
                "3.21 Tuesday", items1));
        data.add(new NoteComponentData("Course 234 discussion",
                "To do list:\n1.Me : complete the assignment by Monday.",
                "3.22 Friday",
                items2));

        GenericRecyclerViewAdapter<NoteComponentData> adapter = new GenericRecyclerViewAdapter<>(
                this,
                data,
                R.id.notesRecycler,
                componentData -> NoteCardFragment.newInstance(componentData.getTitle(),
                        componentData.getDescription(),
                        componentData.getTime(),
                        (ArrayList<String>) componentData.getTags())
        );

        recyclerView.setAdapter(adapter);

    }

    public void showPopupMenu(View view) {
        popupContainerView.setVisibility(popupContainerView.getVisibility() == View.INVISIBLE ?
                View.VISIBLE : View.INVISIBLE);
    }
}