package app.nepaliapp.bscnotes.dashboard.serviceFragements;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import app.nepaliapp.bscnotes.R;
import app.nepaliapp.bscnotes.adapters.NoticeBlockAdapter;
import app.nepaliapp.bscnotes.common.CommonFunctions;
import app.nepaliapp.bscnotes.common.Url;
import app.nepaliapp.bscnotes.common_auth.MySingleton;

public class NoticeDetailFragment extends Fragment {

    private static final String ARG_NOTICE_ID = "notice_id";
    private static final String ARG_HEADING = "heading";
    private static final String ARG_PRELOADED_JSON = "preloaded_json";

    private long noticeId;
    private String heading;
    private String preloadedJson;

    private RecyclerView rvNoticeBlocks;
    private ProgressBar progressNoticeDetail;

    public static NoticeDetailFragment newInstance(long noticeId, String heading) {
        NoticeDetailFragment f = new NoticeDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_NOTICE_ID, noticeId);
        args.putString(ARG_HEADING, heading);
        f.setArguments(args);
        return f;
    }

    public static NoticeDetailFragment newInstanceWithPayload(long noticeId, String heading, String preloadedNoticeJson) {
        NoticeDetailFragment f = new NoticeDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_NOTICE_ID, noticeId);
        args.putString(ARG_HEADING, heading);
        args.putString(ARG_PRELOADED_JSON, preloadedNoticeJson);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            noticeId = getArguments().getLong(ARG_NOTICE_ID, 0L);
            heading = getArguments().getString(ARG_HEADING, "Notice Details");
            preloadedJson = getArguments().getString(ARG_PRELOADED_JSON, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notice_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView btnBack = view.findViewById(R.id.btnBackNoticeDetail);
        TextView tvToolbarTitle = view.findViewById(R.id.tvToolbarNoticeTitle);
        progressNoticeDetail = view.findViewById(R.id.progressNoticeDetail);
        rvNoticeBlocks = view.findViewById(R.id.rvNoticeBlocks);

        tvToolbarTitle.setText(heading);
        rvNoticeBlocks.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        // If opened from NoticeFragment list, contentBlocks are already present -> 0ms render!
        if (preloadedJson != null && !preloadedJson.isEmpty()) {
            try {
                JSONObject obj = new JSONObject(preloadedJson);
                if (obj.has("contentBlocks")) {
                    bindNoticeObject(obj);
                    return;
                }
            } catch (Exception ignored) {}
        }

        // Otherwise fetch hydrated detail from /api/notices/{id}
        fetchSingleNoticeDetail();
    }

    private void fetchSingleNoticeDetail() {
        progressNoticeDetail.setVisibility(View.VISIBLE);
        Url urlHelper = new Url();

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                urlHelper.getNoticeDetail(noticeId),
                null,
                response -> {
                    if (!isAdded() || getContext() == null) return;
                    progressNoticeDetail.setVisibility(View.GONE);
                    JSONObject noticeObj = response.optJSONObject("notice");
                    if (noticeObj != null) {
                        bindNoticeObject(noticeObj);
                    }
                },
                error -> {
                    if (!isAdded() || getContext() == null) return;
                    progressNoticeDetail.setVisibility(View.GONE);
                    new CommonFunctions().handleErrorResponse(requireContext(), error);
                }
        );

        MySingleton.getInstance(requireContext()).addToRequestQueue(req);
    }

    private void bindNoticeObject(JSONObject noticeObj) {
        List<JSONObject> blockList = new ArrayList<>();
        JSONArray arr = noticeObj.optJSONArray("contentBlocks");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject b = arr.optJSONObject(i);
                if (b != null) blockList.add(b);
            }
        }
        NoticeBlockAdapter adapter = new NoticeBlockAdapter(requireContext(), noticeObj, blockList);
        rvNoticeBlocks.setAdapter(adapter);
    }
}