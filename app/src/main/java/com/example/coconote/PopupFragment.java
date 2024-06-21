package com.example.coconote;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PopupFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PopupFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public PopupFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PopupFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PopupFragment newInstance(String param1, String param2) {
        PopupFragment fragment = new PopupFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_popup, container, false);
        TextView myNotesView = view.findViewById(R.id.myNote);
        TextView myView = view.findViewById(R.id.myAccount);
        TextView signOutView = view.findViewById(R.id.signOut);
        myNotesView.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), HomePage.class);
            startActivity(intent);
        });
        myView.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), UserPage.class);
            startActivity(intent);
        });
        signOutView.setOnClickListener(v -> {
            User.getInstance().signOut();
            Intent intent = new Intent(getContext(), MainActivity.class);
            startActivity(intent);
        });
        return view;
    }

}