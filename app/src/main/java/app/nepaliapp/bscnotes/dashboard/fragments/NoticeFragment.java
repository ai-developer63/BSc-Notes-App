package app.nepaliapp.bscnotes.dashboard.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import app.nepaliapp.bscnotes.R;
import app.nepaliapp.bscnotes.adapters.NoticeListAdapter;
import app.nepaliapp.bscnotes.common.CommonFunctions;
import app.nepaliapp.bscnotes.common.Url;
import app.nepaliapp.bscnotes.common_auth.MySingleton;
import app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity;
import app.nepaliapp.bscnotes.dashboard.serviceFragements.NoticeDetailFragment;

public class NoticeFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshNotices;
    private RecyclerView rvNotices;
    private LinearLayout layoutEmptyNotices;

    private final List<JSONObject> noticeList = new ArrayList<>();
    private NoticeListAdapter adapter;
    private Url urlHelper;

    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private static final int PAGE_SIZE = 15;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        urlHelper = new Url();
        swipeRefreshNotices = view.findViewById(R.id.swipeRefreshNotices);
        rvNotices = view.findViewById(R.id.rvNotices);
        layoutEmptyNotices = view.findViewById(R.id.layoutEmptyNotices);

        swipeRefreshNotices.setColorSchemeColors(Color.parseColor("#2563EB"));

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        rvNotices.setLayoutManager(layoutManager);

        adapter = new NoticeListAdapter(noticeList, this::openNoticeDetail);
        rvNotices.setAdapter(adapter);

        // Endless pagination scroll listener
        rvNotices.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPos = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPos) >= totalItemCount - 3 && firstVisibleItemPos >= 0) {
                        fetchNoticesPage(currentPage + 1, false);
                    }
                }
            }
        });

        swipeRefreshNotices.setOnRefreshListener(() -> fetchNoticesPage(0, true));

        if (noticeList.isEmpty()) {
            fetchNoticesPage(0, true);
        }
    }

    private void fetchNoticesPage(int page, boolean clearExisting) {
        isLoading = true;
        if (page == 0) swipeRefreshNotices.setRefreshing(true);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                urlHelper.getPaginatedNotices(page, PAGE_SIZE),
                null,
                response -> {
                    if (!isAdded() || getContext() == null) return;
                    isLoading = false;
                    swipeRefreshNotices.setRefreshing(false);

                    currentPage = response.optInt("currentPage", page);
                    isLastPage = response.optBoolean("isLast", true);

                    if (clearExisting) {
                        noticeList.clear();
                    }

                    JSONArray arr = response.optJSONArray("notices");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.optJSONObject(i);
                            if (item != null) noticeList.add(item);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    layoutEmptyNotices.setVisibility(noticeList.isEmpty() ? View.VISIBLE : View.GONE);
                },
                error -> {
                    if (!isAdded() || getContext() == null) return;
                    isLoading = false;
                    swipeRefreshNotices.setRefreshing(false);
                    new CommonFunctions().handleErrorResponse(requireContext(), error);
                }
        );

        MySingleton.getInstance(requireContext()).addToRequestQueue(req);
    }

    private void openNoticeDetail(JSONObject noticeObj) {
        if (!(getActivity() instanceof DashBoardManagerActivity)) return;
        long id = noticeObj.optLong("id", 0L);
        String heading = noticeObj.optString("heading", "Notice Details");
        NoticeDetailFragment detailFragment = NoticeDetailFragment.newInstanceWithPayload(id, heading, noticeObj.toString());
        ((DashBoardManagerActivity) getActivity()).openSubFragment(detailFragment, true);
    }
}