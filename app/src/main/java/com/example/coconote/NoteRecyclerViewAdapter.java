package com.example.coconote;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coconote.data.NoteComponentData;

import java.util.List;

public class NoteRecyclerViewAdapter extends RecyclerView.Adapter<NoteRecyclerViewAdapter.NoteViewHolder>{
    private List<NoteComponentData> mData;
    private final Context context;

    public NoteRecyclerViewAdapter(Context context, List<NoteComponentData> mData) {
        this.context = context;
        this.mData = mData;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_note_card, parent, false);

        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteComponentData data = mData.get(position);
        holder.noteTitleView.setText(data.getTitle());
        holder.noteDescriptionView.setText(data.getDescription());
        holder.noteTimeView.setText(data.getTime());

        // 清空noteTagContainer以防止重复添加标签
        holder.noteTagContainer.removeAllViews();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        for (String tag: data.getTags()){
            TextView tagText = new TextView(context);
            tagText.setText(tag);
            tagText.setTextSize(14);
            tagText.setTypeface(null, Typeface.BOLD); // 设置字体加粗
            tagText.setTextColor(context.getResources().getColor(R.color.theme));
            params.setMargins(0, 0, 30, 0);
            tagText.setLayoutParams(params);
            holder.noteTagContainer.addView(tagText);
        }

        // 设置外边距
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        layoutParams.setMargins(0, 0, 0, 56); // 左、上、右、下的边距
        holder.itemView.setLayoutParams(layoutParams);
    }


    @Override
    public int getItemCount() {
        return mData.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<NoteComponentData> newData) {
        this.mData = newData;
        notifyDataSetChanged();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder{

        TextView noteTitleView;
        TextView noteDescriptionView;
        TextView noteTimeView;
        LinearLayout noteTagContainer;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTitleView = itemView.findViewById(R.id.noteTitle);
            noteDescriptionView = itemView.findViewById(R.id.noteDescription);
            noteTimeView = itemView.findViewById(R.id.noteCreateDate);
            noteTagContainer = itemView.findViewById(R.id.tagLinear);
        }
    }
}
