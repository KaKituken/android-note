package com.example.coconote;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NoteCardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NoteCardFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_TIME = "time";
    private static final String ARG_TAGS = "tags";
    private static final String ARG_ID = "id";

    // TODO: Rename and change types of parameters
    private String mTitle;
    private String mDescription;
    private String mTime;
    private List<String> mTags;
    private int mId;

    public NoteCardFragment() {
        // Required empty public constructor
    }

    final private String TAG = "NoteCardFragment";


    public static NoteCardFragment newInstance(int id, String title, String description,
                                               String time, ArrayList<String> tags) {
        NoteCardFragment fragment = new NoteCardFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putString(ARG_TIME, time);
        args.putStringArrayList(ARG_TAGS, tags);
        args.putInt(ARG_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTitle = getArguments().getString(ARG_TITLE);
            mDescription = getArguments().getString(ARG_DESCRIPTION);
            mTime = getArguments().getString(ARG_TIME);
            mTags = getArguments().getStringArrayList(ARG_TAGS);
            mId = getArguments().getInt(ARG_ID);
            Log.d(TAG, "onCreate: " + mTags.toString());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_note_card, container, false);
        TextView titleView = view.findViewById(R.id.noteTitle);
        TextView descriptionView = view.findViewById(R.id.noteDescription);
        TextView timeView = view.findViewById(R.id.noteCreateDate);
        titleView.setText(mTitle);
        descriptionView.setText(mDescription);
        timeView.setText(mTime);
        LinearLayout tagContainer = view.findViewById(R.id.tagLinear);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        for (String tag: mTags){
            Log.d(TAG, "onCreateView: " + tag);
            TextView tagText = new TextView(getContext());
            tagText.setText(tag);
            tagText.setTextSize(14);
            tagText.setTypeface(null, Typeface.BOLD); // 设置字体加粗
            tagText.setTextColor(getResources().getColor(R.color.theme));
            params.setMargins(0, 0, 30, 0);
            tagText.setLayoutParams(params);
            tagContainer.addView(tagText);
        }

        return view;
    }
}