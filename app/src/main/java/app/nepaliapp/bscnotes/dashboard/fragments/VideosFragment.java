package app.nepaliapp.bscnotes.dashboard.fragments;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import app.nepaliapp.bscnotes.R;

import java.util.ArrayList;
import java.util.List;

public class VideosFragment extends Fragment {


    private RecyclerView recyclerViewVideos;
    private List<String> videoList;

    public VideosFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_videos, container, false);

        recyclerViewVideos = view.findViewById(R.id.recyclerViewVideos);
        recyclerViewVideos.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Sample video lecture data
        videoList = new ArrayList<>();
        videoList.add("▶️ Data Structures: Linked List Explained (Ep. 1)");
        videoList.add("▶️ Object-Oriented Programming: Inheritance (Ep. 2)");
        videoList.add("▶️ Computer Networks: OSI Model Overview (Ep. 3)");
        videoList.add("▶️ Operating Systems: Process Scheduling (Ep. 4)");
        videoList.add("▶️ Database Management: Normalization 1NF, 2NF, 3NF");

        // Lightweight inline RecyclerView Adapter
        RecyclerView.Adapter<VideoViewHolder> adapter = new RecyclerView.Adapter<VideoViewHolder>() {
            @NonNull
            @Override
            public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TextView textView = new TextView(parent.getContext());
                textView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
                textView.setPadding(32, 28, 32, 28);
                textView.setTextSize(16f);
                textView.setTextColor(Color.DKGRAY);
                return new VideoViewHolder(textView);
            }

            @Override
            public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
                String videoTitle = videoList.get(position);
                TextView tv = (TextView) holder.itemView;
                tv.setText(videoTitle);

                // Handle video item click
                tv.setOnClickListener(v ->
                        Toast.makeText(requireContext(), "Opening: " + videoTitle, Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public int getItemCount() {
                return videoList.size();
            }
        };

        recyclerViewVideos.setAdapter(adapter);

        return view;
    }

    private static class VideoViewHolder extends RecyclerView.ViewHolder {
        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}