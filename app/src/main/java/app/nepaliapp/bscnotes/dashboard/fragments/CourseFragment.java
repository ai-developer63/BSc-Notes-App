package app.nepaliapp.bscnotes.dashboard.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import app.nepaliapp.bscnotes.R;

import java.util.ArrayList;
import java.util.List;

public class CourseFragment extends Fragment {

    private RecyclerView recyclerViewCourses;
    private List<String> courseList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_course, container, false);

        recyclerViewCourses = view.findViewById(R.id.recyclerViewCourses);
        recyclerViewCourses.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Sample course data
        courseList = new ArrayList<>();
        courseList.add("Data Structures and Algorithms (CSC-201)");
        courseList.add("Object-Oriented Programming (CSC-202)");
        courseList.add("Computer Networks (CSC-203)");
        courseList.add("Operating Systems (CSC-204)");
        courseList.add("Database Management Systems (CSC-205)");

        // Set up a lightweight inline adapter
        RecyclerView.Adapter<CourseViewHolder> adapter = new RecyclerView.Adapter<CourseViewHolder>() {
            @NonNull
            @Override
            public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TextView textView = new TextView(parent.getContext());
                textView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
                textView.setPadding(32, 24, 32, 24);
                textView.setTextSize(16f);
                textView.setTextColor(Color.DKGRAY);
                return new CourseViewHolder(textView);
            }

            @Override
            public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
                ((TextView) holder.itemView).setText(courseList.get(position));
            }

            @Override
            public int getItemCount() {
                return courseList.size();
            }
        };

        recyclerViewCourses.setAdapter(adapter);

        return view;
    }

    private static class CourseViewHolder extends RecyclerView.ViewHolder {
        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}