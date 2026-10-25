package app.nepaliapp.bscnotes.dashboard.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity;
import app.nepaliapp.bscnotes.dashboard.serviceFragements.NoticeDetailFragment;
import app.nepaliapp.bscnotes.dashboard.serviceFragements.TutorialVideoListFragment;
import app.nepaliapp.bscnotes.dashboard.serviceFragements.VideoPlayingFragment;

public class HomeFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvGreeting, tvCollegeBadge, tvHeaderNotePass, tvBigNotePassCount;
    private TextView tvOpenFullNotesCatalog, tvViewAllNotices, tvSeeAllTutorialVideos;
    private TextView tvGuideVideoTitle1, tvGuideVideoDesc1, tvGuideVideoTitle2, tvGuideVideoDesc2;
    private TextView tvNoticeBadge1, tvNoticeTitle1, tvNoticeSubtext1;
    private TextView tvNoticeBadge2, tvNoticeTitle2, tvNoticeSubtext2;
    private View dividerNoticeTwo;

    private ImageView ivProfileAvatar, ivAppGuideThumb, ivAppGuideThumbTwo, btnNotifications;
    private ImageCarousel bannerCarousel;
    private RecyclerView rvCategories;
    private MaterialButton btnEarnNotePass;
    private LinearLayout btnRedeemUnlockFeatures, layoutHomeVideosSection, layoutHomeNoticeSection;

    private MaterialCardView cardAvatar, cardTopNotePassPill, cardSupportBadge;
    private MaterialCardView cardAppGuideVideo, cardAppGuideVideoTwo;
    private LinearLayout noticeItemOne, noticeItemTwo;

    private int notePassPoints = 250;
    private Url urlHelper;

    private JSONArray activeCarouselJsonArray = new JSONArray();
    private JSONArray activeHomeVideosJsonArray = new JSONArray();
    private JSONArray activeHomeNoticesJsonArray = new JSONArray();

    public HomeFragment() {}

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
        tvSeeAllTutorialVideos = view.findViewById(R.id.tvSeeAllTutorialVideos);

        tvGuideVideoTitle1 = view.findViewById(R.id.tvGuideVideoTitle1);
        tvGuideVideoDesc1 = view.findViewById(R.id.tvGuideVideoDesc1);
        tvGuideVideoTitle2 = view.findViewById(R.id.tvGuideVideoTitle2);
        tvGuideVideoDesc2 = view.findViewById(R.id.tvGuideVideoDesc2);

        tvNoticeBadge1 = view.findViewById(R.id.tvNoticeBadge1);
        tvNoticeTitle1 = view.findViewById(R.id.tvNoticeTitle1);
        tvNoticeSubtext1 = view.findViewById(R.id.tvNoticeSubtext1);
        tvNoticeBadge2 = view.findViewById(R.id.tvNoticeBadge2);
        tvNoticeTitle2 = view.findViewById(R.id.tvNoticeTitle2);
        tvNoticeSubtext2 = view.findViewById(R.id.tvNoticeSubtext2);
        dividerNoticeTwo = view.findViewById(R.id.dividerNoticeTwo);

        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);
        ivAppGuideThumb = view.findViewById(R.id.ivAppGuideThumb);
        ivAppGuideThumbTwo = view.findViewById(R.id.ivAppGuideThumbTwo);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        bannerCarousel = view.findViewById(R.id.bannerCarousel);

        rvCategories = view.findViewById(R.id.categories);
        rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        rvCategories.setNestedScrollingEnabled(false);

        btnEarnNotePass = view.findViewById(R.id.btnEarnNotePass);
        btnRedeemUnlockFeatures = view.findViewById(R.id.btnRedeemUnlockFeatures);
        layoutHomeVideosSection = view.findViewById(R.id.layoutHomeVideosSection);
        layoutHomeNoticeSection = view.findViewById(R.id.layoutHomeNoticeSection);

        cardAvatar = view.findViewById(R.id.cardAvatar);
        cardTopNotePassPill = view.findViewById(R.id.cardTopNotePassPill);
        cardSupportBadge = view.findViewById(R.id.cardSupportBadge);

        cardAppGuideVideo = view.findViewById(R.id.cardAppGuideVideo);
        cardAppGuideVideoTwo = view.findViewById(R.id.cardAppGuideVideoTwo);
        noticeItemOne = view.findViewById(R.id.noticeItemOne);
        noticeItemTwo = view.findViewById(R.id.noticeItemTwo);

        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#2563EB"));
    }

    private void loadDashboardContent() {
        swipeRefreshLayout.setRefreshing(true);
        updateNotePassDisplay();

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

        MySingleton.getInstance(requireContext()).addToRequestQueue(dashboardRequest);
    }

    private void dispatchDashboardPayload(JSONObject response) {
        if (response == null) return;

        JSONObject userBlock = response.optJSONObject("user");
        if (userBlock != null) renderIdentityHeader(userBlock);

        JSONArray carouselsBlock = response.optJSONArray("carousels");
        if (carouselsBlock != null) renderCarousels(carouselsBlock);

        JSONArray categoriesBlock = response.optJSONArray("categories");
        if (categoriesBlock != null) renderCategories(categoriesBlock);

        JSONArray homeVideosBlock = response.optJSONArray("homeVideos");
        if (homeVideosBlock != null) renderHomeVideos(homeVideosBlock);

        JSONArray noticesBlock = response.optJSONArray("notices");
        if (noticesBlock != null) renderHomeNotices(noticesBlock);
    }

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

    private void renderHomeVideos(JSONArray homeVideosArray) {
        activeHomeVideosJsonArray = homeVideosArray;
        if (homeVideosArray.length() == 0) {
            layoutHomeVideosSection.setVisibility(View.GONE);
            return;
        }
        layoutHomeVideosSection.setVisibility(View.VISIBLE);

        JSONObject v1 = homeVideosArray.optJSONObject(0);
        if (v1 != null) {
            cardAppGuideVideo.setVisibility(View.VISIBLE);
            tvGuideVideoTitle1.setText(v1.optString("title", "App Guide"));
            String cleanDesc1 = Html.fromHtml(v1.optString("description", ""), Html.FROM_HTML_MODE_COMPACT).toString().trim();
            tvGuideVideoDesc1.setText(cleanDesc1);
            tvGuideVideoDesc1.setVisibility(cleanDesc1.isEmpty() ? View.GONE : View.VISIBLE);

            Glide.with(this).load(v1.optString("thumbnailUrl", ""))
                    .placeholder(R.mipmap.ic_launcher).centerCrop().into(ivAppGuideThumb);
        }

        if (homeVideosArray.length() >= 2) {
            JSONObject v2 = homeVideosArray.optJSONObject(1);
            if (v2 != null) {
                cardAppGuideVideoTwo.setVisibility(View.VISIBLE);
                tvGuideVideoTitle2.setText(v2.optString("title", "Tutorial Video"));
                String cleanDesc2 = Html.fromHtml(v2.optString("description", ""), Html.FROM_HTML_MODE_COMPACT).toString().trim();
                tvGuideVideoDesc2.setText(cleanDesc2);
                tvGuideVideoDesc2.setVisibility(cleanDesc2.isEmpty() ? View.GONE : View.VISIBLE);

                Glide.with(this).load(v2.optString("thumbnailUrl", ""))
                        .placeholder(R.mipmap.ic_launcher).centerCrop().into(ivAppGuideThumbTwo);
            }
        } else {
            cardAppGuideVideoTwo.setVisibility(View.GONE);
        }
    }

    private void renderHomeNotices(JSONArray noticesArray) {
        activeHomeNoticesJsonArray = noticesArray;

        if (noticesArray.length() == 0) {
            if (layoutHomeNoticeSection != null) layoutHomeNoticeSection.setVisibility(View.GONE);
            return;
        }
        if (layoutHomeNoticeSection != null) layoutHomeNoticeSection.setVisibility(View.VISIBLE);

        // Bind Notice 1
        JSONObject n1 = noticesArray.optJSONObject(0);
        if (n1 != null) {
            noticeItemOne.setVisibility(View.VISIBLE);
            bindSingleHomeNoticeRow(n1, tvNoticeBadge1, tvNoticeTitle1, tvNoticeSubtext1);
        }

        // Bind Notice 2
        if (noticesArray.length() >= 2) {
            JSONObject n2 = noticesArray.optJSONObject(1);
            if (n2 != null) {
                if (dividerNoticeTwo != null) dividerNoticeTwo.setVisibility(View.VISIBLE);
                noticeItemTwo.setVisibility(View.VISIBLE);
                bindSingleHomeNoticeRow(n2, tvNoticeBadge2, tvNoticeTitle2, tvNoticeSubtext2);
            }
        } else {
            if (dividerNoticeTwo != null) dividerNoticeTwo.setVisibility(View.GONE);
            noticeItemTwo.setVisibility(View.GONE);
        }
    }

    private void bindSingleHomeNoticeRow(JSONObject noticeObj, TextView tvBadge, TextView tvTitle, TextView tvSub) {
        if (tvTitle != null) tvTitle.setText(noticeObj.optString("heading", "Notice Update"));
        if (tvSub != null) tvSub.setText(noticeObj.optString("subtext", "Tap to view details"));

        JSONObject badgeObj = noticeObj.optJSONObject("badge");
        if (tvBadge != null && badgeObj != null) {
            tvBadge.setText(badgeObj.optString("text", "NOTICE"));
            try {
                int textColor = Color.parseColor(badgeObj.optString("textColor", "#E11D48"));
                int bgColor = Color.parseColor(badgeObj.optString("bgColor", "#FFE4E6"));
                tvBadge.setTextColor(textColor);
                GradientDrawable pill = new GradientDrawable();
                pill.setColor(bgColor);
                pill.setCornerRadius(12f);
                tvBadge.setBackground(pill);
            } catch (Exception ignored) {}
        }
    }

    private void setupCarouselConfig() {
        bannerCarousel.registerLifecycle(getViewLifecycleOwner());
        bannerCarousel.setCarouselListener(new CarouselListener() {
            @Nullable
            @Override
            public ViewBinding onCreateViewHolder(@NonNull LayoutInflater layoutInflater, @NonNull ViewGroup viewGroup) {
                return null;
            }
            @Override
            public void onBindViewHolder(@NonNull ViewBinding viewBinding, @NonNull CarouselItem carouselItem, int i) {}
            @Override
            public void onClick(int position, @NonNull CarouselItem carouselItem) {
                handleCarouselClick(position);
            }
            @Override
            public void onLongClick(int i, @NonNull CarouselItem carouselItem) {}
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

        cardAppGuideVideo.setOnClickListener(v -> openTutorialVideoInSubFragment(0));
        cardAppGuideVideoTwo.setOnClickListener(v -> openTutorialVideoInSubFragment(1));
        tvSeeAllTutorialVideos.setOnClickListener(v -> openAllTutorialVideosSubFragment());

        // Notice Card Click Listeners
        noticeItemOne.setOnClickListener(v -> openNoticeDetailFromHome(0));
        noticeItemTwo.setOnClickListener(v -> openNoticeDetailFromHome(1));
        tvViewAllNotices.setOnClickListener(v -> openFullNoticesTabOrSubFragment());
    }

    private void openNoticeDetailFromHome(int index) {
        JSONObject item = activeHomeNoticesJsonArray.optJSONObject(index);
        if (item == null) {
            openFullNoticesTabOrSubFragment();
            return;
        }
        long noticeId = item.optLong("id", 0L);
        String heading = item.optString("heading", "Notice Details");
        if (noticeId > 0 && getActivity() instanceof DashBoardManagerActivity) {
            NoticeDetailFragment detailFragment = NoticeDetailFragment.newInstance(noticeId, heading);
            ((DashBoardManagerActivity) getActivity()).openSubFragment(detailFragment, true);
        }
    }

    private void openFullNoticesTabOrSubFragment() {
        if (getActivity() instanceof DashBoardManagerActivity) {
            ((DashBoardManagerActivity) getActivity()).openSubFragment(new NoticeFragment(), true);
        }
    }

    private void openTutorialVideoInSubFragment(int index) {
        if (!(getActivity() instanceof DashBoardManagerActivity)) return;
        JSONObject videoObj = activeHomeVideosJsonArray.optJSONObject(index);
        if (videoObj == null) {
            openAllTutorialVideosSubFragment();
            return;
        }
        VideoPlayingFragment playerFragment = VideoPlayingFragment.newInstance(
                videoObj.optString("videoUrl", ""),
                videoObj.optString("title", "Tutorial Video"),
                videoObj.optString("description", ""),
                activeHomeVideosJsonArray.toString()
        );
        ((DashBoardManagerActivity) getActivity()).openSubFragment(playerFragment, true);
    }

    private void openAllTutorialVideosSubFragment() {
        if (getActivity() instanceof DashBoardManagerActivity) {
            TutorialVideoListFragment listFragment = TutorialVideoListFragment.newInstance(
                    activeHomeVideosJsonArray.toString()
            );
            ((DashBoardManagerActivity) getActivity()).openSubFragment(listFragment, true);
        }
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

    private void showDedicatedSupportDialog() {
        String[] supportItems = {
                "🎬 Watch Video: How to Use App",
                "📲 Chat with Support on WhatsApp",
                "⚡ NotePass / Feature Unlock Issue"
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🎧 Help & Support")
                .setItems(supportItems, (dialog, which) -> {
                    if (which == 0) openTutorialVideoInSubFragment(0);
                })
                .setNegativeButton("Close", null)
                .show();
    }
}