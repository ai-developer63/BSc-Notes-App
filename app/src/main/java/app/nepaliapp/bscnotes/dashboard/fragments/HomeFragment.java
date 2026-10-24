package app.nepaliapp.bscnotes.dashboard.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewbinding.ViewBinding;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.imaginativeworld.whynotimagecarousel.ImageCarousel;
import org.imaginativeworld.whynotimagecarousel.listener.CarouselListener;
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.nepaliapp.bscnotes.R;
import app.nepaliapp.bscnotes.adapters.CategoriesAdapter;
import app.nepaliapp.bscnotes.common.Url;
import app.nepaliapp.bscnotes.common_auth.MySingleton;

public class HomeFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvGreeting, tvCollegeBadge, tvHeaderNotePass, tvBigNotePassCount;
    private TextView tvOpenFullNotesCatalog, tvViewAllNotices;
    private ImageView ivProfileAvatar, ivAppGuideThumb, btnNotifications;
    private ImageCarousel bannerCarousel;
    private RecyclerView rvCategories;
    private MaterialButton btnEarnNotePass;
    private LinearLayout btnRedeemUnlockFeatures;

    private MaterialCardView cardAvatar, cardTopNotePassPill, cardSupportBadge;
    private MaterialCardView cardAppGuideVideo;
    private LinearLayout noticeItemOne, noticeItemTwo;

    private int notePassPoints = 250;
    private ExoPlayer guideExoPlayer;
    private Url urlHelper;

    // Holds raw carousel JSON objects in sync with bannerCarousel items for click routing
    private JSONArray activeCarouselJsonArray = new JSONArray();

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        urlHelper = new Url();
        initViews(view);
        setupCarouselConfig();
        setupClickListeners();
        loadDashboardContent();
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvCollegeBadge = view.findViewById(R.id.tvCollegeBadge);
        tvHeaderNotePass = view.findViewById(R.id.tvHeaderNotePass);
        tvBigNotePassCount = view.findViewById(R.id.tvBigNotePassCount);

        tvOpenFullNotesCatalog = view.findViewById(R.id.tvOpenFullNotesCatalog);
        tvViewAllNotices = view.findViewById(R.id.tvViewAllNotices);

        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);
        ivAppGuideThumb = view.findViewById(R.id.ivAppGuideThumb);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        bannerCarousel = view.findViewById(R.id.bannerCarousel);

        rvCategories = view.findViewById(R.id.categories);
        rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        rvCategories.setNestedScrollingEnabled(false);

        btnEarnNotePass = view.findViewById(R.id.btnEarnNotePass);
        btnRedeemUnlockFeatures = view.findViewById(R.id.btnRedeemUnlockFeatures);

        cardAvatar = view.findViewById(R.id.cardAvatar);
        cardTopNotePassPill = view.findViewById(R.id.cardTopNotePassPill);
        cardSupportBadge = view.findViewById(R.id.cardSupportBadge);

        cardAppGuideVideo = view.findViewById(R.id.cardAppGuideVideo);
        noticeItemOne = view.findViewById(R.id.noticeItemOne);
        noticeItemTwo = view.findViewById(R.id.noticeItemTwo);

        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#2563EB"));
    }

    // =========================================================================
    // 1. NETWORK FETCHER & MODULAR DISPATCHER
    // =========================================================================

    private void loadDashboardContent() {
        swipeRefreshLayout.setRefreshing(true);

        // Static UI elements & default wallet display
        updateNotePassDisplay();
        Glide.with(this)
                .load("https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=900&q=80")
                .centerCrop()
                .into(ivAppGuideThumb);

        JsonObjectRequest dashboardRequest = new JsonObjectRequest(
                Request.Method.GET,
                urlHelper.getDashboard(),
                null,
                response -> {
                    if (!isAdded() || getContext() == null) return;
                    swipeRefreshLayout.setRefreshing(false);
                    dispatchDashboardPayload(response);
                },
                error -> {
                    if (!isAdded() || getContext() == null) return;
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(requireContext(), "Unable to refresh dashboard", Toast.LENGTH_SHORT).show();
                }
        );

        // X-App-Code and Bearer JWT are automatically attached by MySingleton!
        MySingleton.getInstance(requireContext()).addToRequestQueue(dashboardRequest);
    }

    /**
     * Master Dispatcher: Unpacks the JSON envelope and routes each section
     * to its dedicated rendering function. To add new sections in the future,
     * just add one new optJSONArray/optJSONObject line here.
     */
    private void dispatchDashboardPayload(JSONObject response) {
        if (response == null) return;

        // 1. User Profile Header Block
        JSONObject userBlock = response.optJSONObject("user");
        if (userBlock != null) {
            renderIdentityHeader(userBlock);
        }

        // 2. Carousels Block
        JSONArray carouselsBlock = response.optJSONArray("carousels");
        if (carouselsBlock != null) {
            renderCarousels(carouselsBlock);
        }

        // 3. Study Categories Block
        JSONArray categoriesBlock = response.optJSONArray("categories");
        if (categoriesBlock != null) {
            renderCategories(categoriesBlock);
        }

        // 4. Future blocks can be routed here cleanly:
        // JSONArray noticesBlock = response.optJSONArray("notices");
        // if (noticesBlock != null) renderNotices(noticesBlock);
    }

    // =========================================================================
    // 2. DEDICATED SECTION RENDERERS
    // =========================================================================

    @SuppressLint("SetTextI18n")
    private void renderIdentityHeader(JSONObject userObj) {
        String fullName = userObj.optString("name", "Scholar");
        String collegeName = userObj.optString("collegeName", "Tribhuvan University");
        String sessionLabel = userObj.optString("sessionLabel", "BSc");

        String safeFirstName = "Scholar";
        if (!fullName.trim().isEmpty()) {
            safeFirstName = fullName.trim().split("\\s+")[0];
            if (safeFirstName.length() > 12) {
                safeFirstName = safeFirstName.substring(0, 11) + "…";
            }
        }
        tvGreeting.setText("Namaste, " + safeFirstName + "!");

        String displayCollege = !collegeName.trim().isEmpty() ? collegeName.trim() : "Tribhuvan University";
        String displayYear = !sessionLabel.trim().isEmpty() ? sessionLabel.trim() : "BSc";

        tvCollegeBadge.setText("🏛️ " + displayYear + " • " + displayCollege);

        // Dynamic Avatar based on user's name
        String avatarUrl = "https://ui-avatars.com/api/?name=" + Uri.encode(fullName) + "&background=2563EB&color=fff&bold=true";
        Glide.with(this).load(avatarUrl).circleCrop().into(ivProfileAvatar);
    }

    private void renderCarousels(JSONArray carouselsArray) {
        activeCarouselJsonArray = carouselsArray;
        List<CarouselItem> items = new ArrayList<>();

        for (int i = 0; i < carouselsArray.length(); i++) {
            JSONObject obj = carouselsArray.optJSONObject(i);
            if (obj != null) {
                String bannerUrl = obj.optString("bannerUrl", "");
                String caption = obj.optString("captions", obj.optString("name", ""));
                if (!bannerUrl.isEmpty()) {
                    items.add(new CarouselItem(bannerUrl, caption));
                }
            }
        }

        if (!items.isEmpty()) {
            bannerCarousel.setData(items);
        }
    }

    private void renderCategories(JSONArray categoriesArray) {
        CategoriesAdapter adapter = new CategoriesAdapter(requireContext(), categoriesArray);
        rvCategories.setAdapter(adapter);
    }

    // =========================================================================
    // 3. CAROUSEL & UI CLICK HANDLERS
    // =========================================================================

    private void setupCarouselConfig() {
        bannerCarousel.registerLifecycle(getViewLifecycleOwner());
        bannerCarousel.setCarouselListener(new CarouselListener() {
            @Nullable
            @Override
            public ViewBinding onCreateViewHolder(@NonNull LayoutInflater layoutInflater, @NonNull ViewGroup viewGroup) {
                return null;
            }

            @Override
            public void onBindViewHolder(@NonNull ViewBinding viewBinding, @NonNull CarouselItem carouselItem, int i) {
            }

            @Override
            public void onClick(int position, @NonNull CarouselItem carouselItem) {
                handleCarouselClick(position);
            }

            @Override
            public void onLongClick(int i, @NonNull CarouselItem carouselItem) {
            }
        });
    }

    private void handleCarouselClick(int position) {
        JSONObject clickedObj = activeCarouselJsonArray.optJSONObject(position);
        if (clickedObj == null) return;

        String openType = clickedObj.optString("openType", "UNKNOWN");
        String whatToOpen = clickedObj.optString("whatToOpen", "");

        if ("WEBPAGE".equalsIgnoreCase(openType) && !whatToOpen.isEmpty()) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(whatToOpen)));
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Invalid link", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), clickedObj.optString("name", "Banner clicked"), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateNotePassDisplay() {
        String formattedPoints = String.format(Locale.US, "%,d", notePassPoints);
        tvHeaderNotePass.setText(String.format(Locale.US, "⚡ %s", formattedPoints));
        tvBigNotePassCount.setText(String.format(Locale.US, "%s NotePass", formattedPoints));
    }

    private void setupClickListeners() {
        swipeRefreshLayout.setOnRefreshListener(this::loadDashboardContent);

        cardAvatar.setOnClickListener(v -> Toast.makeText(requireContext(), "Opening Profile...", Toast.LENGTH_SHORT).show());
        btnEarnNotePass.setOnClickListener(v -> showEarnNotePassRewardedAdDialog());
        cardSupportBadge.setOnClickListener(v -> showDedicatedSupportDialog());

        View.OnClickListener unlockClick = v -> navigateToBottomTab(R.id.nav_unlock);
        cardTopNotePassPill.setOnClickListener(unlockClick);
        btnRedeemUnlockFeatures.setOnClickListener(unlockClick);

        View.OnClickListener notesClick = v -> navigateToBottomTab(R.id.nav_courses);
        tvOpenFullNotesCatalog.setOnClickListener(notesClick);

        cardAppGuideVideo.setOnClickListener(v -> playOptimizedAppGuideVideo());
    }

    private void navigateToBottomTab(int tabId) {
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(tabId);
            }
        }
    }

    private void showEarnNotePassRewardedAdDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("⚡ Earn +50 Free NotePass")
                .setMessage("NotePass is 100% free! Watch a short video ad to add +50 NotePass to your wallet.")
                .setPositiveButton("▶ Watch Ad (+50 Free)", (dialog, which) -> {
                    notePassPoints += 50;
                    updateNotePassDisplay();
                    Toast.makeText(requireContext(), "🎉 +50 Free NotePass Added!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void playOptimizedAppGuideVideo() {
        releaseGuidePlayer();
        PlayerView playerView = new PlayerView(requireContext());
        int heightPx = (int) (230 * getResources().getDisplayMetrics().density);
        playerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx));
        playerView.setBackgroundColor(Color.BLACK);

        guideExoPlayer = new ExoPlayer.Builder(requireContext()).build();
        playerView.setPlayer(guideExoPlayer);

        String tutorialVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4";
        MediaItem mediaItem = MediaItem.fromUri(tutorialVideoUrl);
        guideExoPlayer.setMediaItem(mediaItem);
        guideExoPlayer.prepare();
        guideExoPlayer.setPlayWhenReady(true);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🎬 कसरी प्रयोग गर्ने BSc Notes App?")
                .setView(playerView)
                .setPositiveButton("Close Tutorial", (d, which) -> releaseGuidePlayer())
                .create();

        dialog.setOnDismissListener(d -> releaseGuidePlayer());
        dialog.show();
    }

    private void releaseGuidePlayer() {
        if (guideExoPlayer != null) {
            guideExoPlayer.stop();
            guideExoPlayer.release();
            guideExoPlayer = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        releaseGuidePlayer();
    }

    private void showDedicatedSupportDialog() {
        String[] supportItems = {
                "🎬 Watch Video: How to Use App",
                "📲 Chat with Support on WhatsApp",
                "⚡ NotePass / Feature Unlock Issue"
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🎧 Help & Support")
                .setItems(supportItems, (dialog, which) -> {
                    if (which == 0) playOptimizedAppGuideVideo();
                })
                .setNegativeButton("Close", null)
                .show();
    }
}