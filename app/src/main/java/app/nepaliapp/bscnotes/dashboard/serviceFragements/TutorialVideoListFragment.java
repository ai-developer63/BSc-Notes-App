package app.nepaliapp.bscnotes.dashboard.serviceFragements;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import app.nepaliapp.bscnotes.R;
import app.nepaliapp.bscnotes.adapters.VideoCardAdapter;
import app.nepaliapp.bscnotes.common.Url;
import app.nepaliapp.bscnotes.common_auth.MySingleton;
import app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity;

public class TutorialVideoListFragment extends Fragment {

    private static final String ARG_INITIAL_VIDEOS = "initialVideosJson";

    private SwipeRefreshLayout swipeRefreshVideos;
    private RecyclerView rvAllTutorialVideos;
    private TextView tvEmptyVideosState;
    private VideoCardAdapter adapter;
    private JSONArray allVideosArray = new JSONArray();
    private Url urlHelper;

    public TutorialVideoListFragment() {
        // Required empty constructor
    }

    public static TutorialVideoListFragment newInstance(String initialVideosJson) {
        TutorialVideoListFragment fragment = new TutorialVideoListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INITIAL_VIDEOS, initialVideosJson);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tutorial_video_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        urlHelper = new Url();
        ImageView btnBack = view.findViewById(R.id.btnBackFromVideoList);
        swipeRefreshVideos = view.findViewById(R.id.swipeRefreshVideos);
        rvAllTutorialVideos = view.findViewById(R.id.rvAllTutorialVideos);
        tvEmptyVideosState = view.findViewById(R.id.tvEmptyVideosState);

        swipeRefreshVideos.setColorSchemeColors(Color.parseColor("#2563EB"));
        rvAllTutorialVideos.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnBack.setOnClickListener(v -> {
            if (getActivity() instanceof DashBoardManagerActivity) {
                ((DashBoardManagerActivity) getActivity()).popOneStepBack();
            }
        });

        adapter = new VideoCardAdapter(requireContext(), allVideosArray, this::openVideoPlayer);
        rvAllTutorialVideos.setAdapter(adapter);

        if (getArguments() != null) {
            String preload = getArguments().getString(ARG_INITIAL_VIDEOS);
            if (preload != null && !preload.isEmpty()) {
                try {
                    allVideosArray = new JSONArray(preload);
                    adapter.updateData(allVideosArray);
                } catch (JSONException ignored) {}
            }
        }

        swipeRefreshVideos.setOnRefreshListener(this::fetchAllTutorialVideos);
        fetchAllTutorialVideos();
    }

    private void fetchAllTutorialVideos() {
        swipeRefreshVideos.setRefreshing(true);

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                urlHelper.getTutorialVideos(),
                null,
                response -> {
                    if (!isAdded() || getContext() == null) return;
                    swipeRefreshVideos.setRefreshing(false);
                    allVideosArray = (response != null) ? response : new JSONArray();
                    adapter.updateData(allVideosArray);
                    tvEmptyVideosState.setVisibility(allVideosArray.length() == 0 ? View.VISIBLE : View.GONE);
                },
                error -> {
                    if (!isAdded() || getContext() == null) return;
                    swipeRefreshVideos.setRefreshing(false);
                    if (allVideosArray.length() == 0) {
                        tvEmptyVideosState.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(requireContext(), "Unable to load tutorial videos", Toast.LENGTH_SHORT).show();
                }
        );

        MySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    private void openVideoPlayer(JSONObject videoObj) {
        if (videoObj == null || !(getActivity() instanceof DashBoardManagerActivity)) return;

        String videoUrl = videoObj.optString("videoUrl", videoObj.optString("link", ""));
        String title = videoObj.optString("title", "Tutorial Video");
        String desc = videoObj.optString("description", "");

        VideoPlayingFragment playerFragment = VideoPlayingFragment.newInstance(
                videoUrl,
                title,
                desc,
                allVideosArray.toString()
        );

        ((DashBoardManagerActivity) getActivity()).openSubFragment(playerFragment, true);
    }
}