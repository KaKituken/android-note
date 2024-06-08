package com.example.coconote;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class GenericRecyclerViewAdapter<T> extends RecyclerView.Adapter<GenericRecyclerViewAdapter.ViewHolder> {

    private final List<T> mData;
    private final FragmentManager fragmentManager;
    private int fragmentContainerId;
    private final FragmentCreator<T> fragmentCreator;

    public interface FragmentCreator<T> {
        NoteCardFragment createFragment(T data);
    }

    public GenericRecyclerViewAdapter(FragmentActivity activity, List<T> data, int fragmentContainerId, FragmentCreator<T> fragmentCreator) {
        this.mData = data;
        this.fragmentManager = activity.getSupportFragmentManager();
        this.fragmentContainerId = fragmentContainerId;
        this.fragmentCreator = fragmentCreator;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_note_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        T data = mData.get(position);
        NoteCardFragment fragment = fragmentCreator.createFragment(data);
        fragmentManager.beginTransaction()
                .replace(holder.itemView.findViewById(R.id.noteItem).getId(), fragment)
                .commit();
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}