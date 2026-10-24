package app.nepaliapp.bscnotes.dashboard.serviceFragements;


import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import app.nepaliapp.bscnotes.R;
import app.nepaliapp.bscnotes.adapters.VideoCardAdapter;
import app.nepaliapp.bscnotes.common.Url;
import app.nepaliapp.bscnotes.common_auth.CustomHttpDataSourceFactory;
import app.nepaliapp.bscnotes.common_auth.MySingleton;
import app.nepaliapp.bscnotes.common_auth.StorageClass;
import app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity;

@OptIn(markerClass = UnstableApi.class)
public class VideoPlayingFragment extends Fragment {

    private static final String ARG_VIDEO_URL = "videoUrl";
    private static final String ARG_VIDEO_TITLE = "videoTitle";
    private static final String ARG_VIDEO_DESC = "videoDesc";
    private static final String ARG_VIDEO_LIST_JSON = "videoListJson";

    private PlayerView playerView;
    private TextView videoTitle, videoDescription;
    private LinearLayout playerTopHeader, playerBottomContent;
    private ImageView btnBackFromPlayer;
    private RecyclerView otherVideoRecycler;

    private ExoPlayer player;
    private VideoCardAdapter adapter;
    private boolean isFullScreen = false;

    private String currentVideoUrl = "";
    private JSONArray allVideos = new JSONArray();
    private JSONArray otherVideos = new JSONArray();

    private OnBackPressedCallback fullscreenBackCallback;

    public VideoPlayingFragment() {
        // Required empty constructor
    }

    public static VideoPlayingFragment newInstance(String videoUrl, String title, String description, String videoListJson) {
        VideoPlayingFragment fragment = new VideoPlayingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_VIDEO_URL, videoUrl);
        args.putString(ARG_VIDEO_TITLE, title);
        args.putString(ARG_VIDEO_DESC, description);
        args.putString(ARG_VIDEO_LIST_JSON, videoListJson);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video_playing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playerView = view.findViewById(R.id.player_view);
        videoTitle = view.findViewById(R.id.titleOfTheVideo);
        videoDescription = view.findViewById(R.id.descriptionOfTheVideo);
        playerTopHeader = view.findViewById(R.id.playerTopHeader);
        playerBottomContent = view.findViewById(R.id.playerBottomContent);
        btnBackFromPlayer = view.findViewById(R.id.btnBackFromPlayer);
        otherVideoRecycler = view.findViewById(R.id.recyclerViewOtherVideos);

        otherVideoRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        playerView.setControllerShowTimeoutMs(4000);
        playerView.setFullscreenButtonClickListener(isFull -> toggleFullScreen());

        btnBackFromPlayer.setOnClickListener(v -> {
            if (isFullScreen) {
                toggleFullScreen();
            } else if (getActivity() instanceof DashBoardManagerActivity) {
                ((DashBoardManagerActivity) getActivity()).popOneStepBack();
            }
        });

        setupFullscreenBackInterceptor();

        Bundle args = getArguments();
        if (args != null) {
            currentVideoUrl = args.getString(ARG_VIDEO_URL, "");
            String titleStr = args.getString(ARG_VIDEO_TITLE, "Tutorial Video");
            String descStr = args.getString(ARG_VIDEO_DESC, "");
            String listJson = args.getString(ARG_VIDEO_LIST_JSON, null);

            updateMetadataUI(titleStr, descStr);

            if (!currentVideoUrl.isEmpty()) {
                initializePlayer(currentVideoUrl);
            }

            if (listJson != null && !listJson.isEmpty()) {
                try {
                    allVideos = new JSONArray(listJson);
                } catch (JSONException ignored) {}
            }
        }

        setupRecyclerView();

        // Always fetch full list in background if allVideos has <= 2 items so user sees all other tutorials
        if (allVideos.length() <= 2) {
            fetchAllVideosFromServer();
        }
    }

    private void updateMetadataUI(String title, String rawDescription) {
        videoTitle.setText(title != null ? title : "");
        if (rawDescription != null && !rawDescription.trim().isEmpty()) {
            String clean = Html.fromHtml(rawDescription, Html.FROM_HTML_MODE_COMPACT).toString().trim();
            videoDescription.setText(clean);
            videoDescription.setVisibility(clean.isEmpty() ? View.GONE : View.VISIBLE);
        } else {
            videoDescription.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerView() {
        adapter = new VideoCardAdapter(requireContext(), otherVideos, video -> {
            String newUrl = extractVideoUrl(video);
            String newTitle = video.optString("title", "Tutorial Video");
            String newDesc = video.optString("description", "");

            currentVideoUrl = newUrl;
            updateMetadataUI(newTitle, newDesc);
            playVideo(newUrl);
            rebuildOtherVideosList();
        });

        rebuildOtherVideosList();
        otherVideoRecycler.setAdapter(adapter);
    }

    private void fetchAllVideosFromServer() {
        Url urlHelper = new Url();
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                urlHelper.getTutorialVideos(),
                null,
                response -> {
                    if (!isAdded() || response == null) return;
                    allVideos = response;
                    rebuildOtherVideosList();
                },
                error -> {}
        );
        MySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    private void rebuildOtherVideosList() {
        JSONArray updatedList = new JSONArray();
        for (int i = 0; i < allVideos.length(); i++) {
            JSONObject obj = allVideos.optJSONObject(i);
            if (obj != null) {
                String itemUrl = extractVideoUrl(obj);
                if (!itemUrl.equals(currentVideoUrl)) {
                    updatedList.put(obj);
                }
            }
        }
        otherVideos = updatedList;
        if (adapter != null) {
            adapter.updateData(updatedList);
        }
    }

    private String extractVideoUrl(JSONObject obj) {
        String url = obj.optString("videoUrl", "");
        if (url.isEmpty()) {
            url = obj.optString("link", "");
        }
        return url;
    }

    private void initializePlayer(String videoUrl) {
        releasePlayer();

        StorageClass tokenStore = new StorageClass(requireContext().getApplicationContext());
        String jwtToken = tokenStore.getJwtToken();
        HttpDataSource.Factory dataSourceFactory = new CustomHttpDataSourceFactory(jwtToken);

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        3000,  // min buffer
                        5000,  // max buffer
                        1500,  // buffer to start playback
                        2000   // buffer after rebuffer
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        player = new ExoPlayer.Builder(requireContext())
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory))
                .setLoadControl(loadControl)
                .build();

        playerView.setPlayer(player);
        playVideo(videoUrl);
    }

    private void playVideo(String videoUrl) {
        if (player == null || videoUrl == null || videoUrl.isEmpty()) return;
        MediaItem mediaItem = MediaItem.fromUri(videoUrl);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private void setupFullscreenBackInterceptor() {
        fullscreenBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (isFullScreen) {
                    toggleFullScreen();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), fullscreenBackCallback);
    }

    private void toggleFullScreen() {
        if (getActivity() == null) return;

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) playerView.getLayoutParams();
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getActivity().getWindow(), getActivity().getWindow().getDecorView());

        if (isFullScreen) {
            // Exit Fullscreen -> Portrait
            if (insetsController != null) {
                insetsController.show(WindowInsetsCompat.Type.systemBars());
            }
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            playerTopHeader.setVisibility(View.VISIBLE);
            playerBottomContent.setVisibility(View.VISIBLE);

            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = (int) (230 * getResources().getDisplayMetrics().density);
            params.addRule(RelativeLayout.BELOW, R.id.playerTopHeader);

            isFullScreen = false;
            fullscreenBackCallback.setEnabled(false);
        } else {
            // Enter Fullscreen -> Landscape
            if (insetsController != null) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars());
                insetsController.setSystemBarsBehavior(
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

            playerTopHeader.setVisibility(View.GONE);
            playerBottomContent.setVisibility(View.GONE);

            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.removeRule(RelativeLayout.BELOW);

            isFullScreen = true;
            fullscreenBackCallback.setEnabled(true);
        }

        playerView.setLayoutParams(params);
    }

    private void restorePortraitAndSystemBarsIfNeeded() {
        if (isFullScreen && getActivity() != null) {
            WindowInsetsControllerCompat insetsController =
                    WindowCompat.getInsetsController(getActivity().getWindow(), getActivity().getWindow().getDecorView());
            if (insetsController != null) {
                insetsController.show(WindowInsetsCompat.Type.systemBars());
            }
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            isFullScreen = false;
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
            player.pause();
        }
    }

    @Override
    public void onDestroyView() {
        restorePortraitAndSystemBarsIfNeeded();
        releasePlayer();
        super.onDestroyView();
    }
}