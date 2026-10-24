package app.nepaliapp.bscnotes.dashboard.fragments;

import android.annotation.SuppressLint;
import android.graphics.Color;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.imaginativeworld.whynotimagecarousel.ImageCarousel;
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.nepaliapp.bscnotes.R;

public class HomeFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvGreeting, tvCollegeBadge, tvHeaderNotePass, tvBigNotePassCount;
    private TextView tvOpenFullNotesCatalog, tvViewAllNotices;
    private ImageView ivProfileAvatar, ivAppGuideThumb, btnNotifications;
    private ImageCarousel bannerCarousel;
    private MaterialButton btnEarnNotePass;
    private LinearLayout btnRedeemUnlockFeatures;

    private MaterialCardView cardAvatar, cardTopNotePassPill, cardSupportBadge;
    private MaterialCardView catFullNotes, catPractical, catOldIsGold, catVideos;
    private MaterialCardView cardAppGuideVideo;
    private LinearLayout noticeItemOne, noticeItemTwo;

    private int notePassPoints = 250;
    private ExoPlayer guideExoPlayer;

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

        initViews(view);
        setupCarousel();
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

        btnEarnNotePass = view.findViewById(R.id.btnEarnNotePass);
        btnRedeemUnlockFeatures = view.findViewById(R.id.btnRedeemUnlockFeatures);

        cardAvatar = view.findViewById(R.id.cardAvatar);
        cardTopNotePassPill = view.findViewById(R.id.cardTopNotePassPill);
        cardSupportBadge = view.findViewById(R.id.cardSupportBadge);

        catFullNotes = view.findViewById(R.id.catFullNotes);
        catPractical = view.findViewById(R.id.catPractical);
        catOldIsGold = view.findViewById(R.id.catOldIsGold);
        catVideos = view.findViewById(R.id.catVideos);

        cardAppGuideVideo = view.findViewById(R.id.cardAppGuideVideo);
        noticeItemOne = view.findViewById(R.id.noticeItemOne);
        noticeItemTwo = view.findViewById(R.id.noticeItemTwo);

        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#2563EB"));
    }

    private void updateNotePassDisplay() {
        String formattedPoints = String.format(Locale.US, "%,d", notePassPoints);
        tvHeaderNotePass.setText(String.format(Locale.US, "⚡ %s", formattedPoints));
        tvBigNotePassCount.setText(String.format(Locale.US, "%s NotePass", formattedPoints));
    }

    @SuppressLint("SetTextI18n")
    private void setStudentIdentityHeader(String fullName, String collegeName, String bscYear) {
        String safeFirstName = "Scholar";
        if (fullName != null && !fullName.trim().isEmpty()) {
            safeFirstName = fullName.trim().split("\\s+")[0];
            if (safeFirstName.length() > 12) {
                safeFirstName = safeFirstName.substring(0, 11) + "…";
            }
        }
        tvGreeting.setText("Namaste, " + safeFirstName + "!");

        String displayCollege = (collegeName != null && !collegeName.trim().isEmpty())
                ? collegeName.trim() : "Tribhuvan University";
        String displayYear = (bscYear != null && !bscYear.trim().isEmpty())
                ? bscYear.trim() : "BSc";

        tvCollegeBadge.setText("🏛️ " + displayYear + " • " + displayCollege);
    }

    private void setupCarousel() {
        bannerCarousel.registerLifecycle(getViewLifecycleOwner());
        List<CarouselItem> items = new ArrayList<>();
        items.add(new CarouselItem("https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=900&q=80", "Complete BSc Full Notes, Practicals & Solved Old is Gold"));
        items.add(new CarouselItem("https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=900&q=80", "Earn Free NotePass & Unlock Full Video Courses"));
        bannerCarousel.setData(items);
    }

    private void setupClickListeners() {
        swipeRefreshLayout.setOnRefreshListener(this::loadDashboardContent);

        cardAvatar.setOnClickListener(v -> Toast.makeText(requireContext(), "Opening Profile...", Toast.LENGTH_SHORT).show());
        btnEarnNotePass.setOnClickListener(v -> showEarnNotePassRewardedAdDialog());
        cardSupportBadge.setOnClickListener(v -> showDedicatedSupportDialog());

        // --- NAVIGATION CLICKS ---
        // This is how we switch tabs from within a Fragment!
        View.OnClickListener unlockClick = v -> navigateToBottomTab(R.id.nav_unlock);
        cardTopNotePassPill.setOnClickListener(unlockClick);
        btnRedeemUnlockFeatures.setOnClickListener(unlockClick);

        View.OnClickListener notesClick = v -> navigateToBottomTab(R.id.nav_courses);
        catFullNotes.setOnClickListener(notesClick);
        tvOpenFullNotesCatalog.setOnClickListener(notesClick);

        catVideos.setOnClickListener(v -> navigateToBottomTab(R.id.nav_videos));

        // --- OTHER CLICKS ---
        catPractical.setOnClickListener(v -> Toast.makeText(requireContext(), "Opening Practical Lab Manuals...", Toast.LENGTH_SHORT).show());
        catOldIsGold.setOnClickListener(v -> Toast.makeText(requireContext(), "Opening Solved Old is Gold...", Toast.LENGTH_SHORT).show());
        cardAppGuideVideo.setOnClickListener(v -> playOptimizedAppGuideVideo());
    }

    /**
     * Reaches up to the parent Activity and selects the corresponding BottomNav tab.
     */
    private void navigateToBottomTab(int tabId) {
        if (requireActivity() != null) {
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNavigation);
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
        releaseGuidePlayer(); // Prevent video from playing in background if user leaves app
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

    private void loadDashboardContent() {
        swipeRefreshLayout.setRefreshing(true);

        setStudentIdentityHeader("Scholar", "Butwal Multiple Campus", "1st Yr");
        updateNotePassDisplay();

        Glide.with(this).load("https://ui-avatars.com/api/?name=BSc+Notes&background=2563EB&color=fff&bold=true").circleCrop().into(ivProfileAvatar);
        Glide.with(this).load("https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=900&q=80").centerCrop().into(ivAppGuideThumb);

        swipeRefreshLayout.postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 500);
    }
}