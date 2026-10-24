package app.nepaliapp.bscnotes.dashboard.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.VolleyError;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import app.nepaliapp.bscnotes.R;

public class CollegeSearchDialogHelper {

    public interface OnCollegeSelectedListener {
        void onCollegeSelected(long collegeId, String collegeName, String chatBadge, String city, String district);
    }

    private final Activity activity;
    private final ProfileNetworkManager networkManager;
    private final String defaultDistrict;
    private final String defaultCity;
    private final String defaultLevel;
    private final String defaultAffiliation;
    private final OnCollegeSelectedListener listener;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private final List<JSONObject> collegeList = new ArrayList<>();
    private CollegeAdapter adapter;

    private int currentPage = 0;
    private boolean hasMore = false;
    private boolean isLoading = false;
    private String currentQuery = "";
    private boolean ignoreContextualFilters = false;

    public CollegeSearchDialogHelper(
            Activity activity,
            ProfileNetworkManager networkManager,
            String defaultDistrict,
            String defaultCity,
            String defaultLevel,
            String defaultAffiliation,
            OnCollegeSelectedListener listener) {
        this.activity = activity;
        this.networkManager = networkManager;
        this.defaultDistrict = defaultDistrict;
        this.defaultCity = defaultCity;
        this.defaultLevel = defaultLevel;
        this.defaultAffiliation = defaultAffiliation;
        this.listener = listener;
    }

    public void show() {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_college_search, null);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvSearchFilterHint = view.findViewById(R.id.tvSearchFilterHint);
        MaterialButton btnClose = view.findViewById(R.id.btnCloseCollegeSearch);
        TextInputEditText etSearch = view.findViewById(R.id.etCollegeSearchQuery);
        Chip chipToggleAllNepal = view.findViewById(R.id.chipToggleAllNepal);
        ProgressBar pbLoading = view.findViewById(R.id.pbCollegeSearch);
        RecyclerView rvColleges = view.findViewById(R.id.rvColleges);
        TextView tvEmpty = view.findViewById(R.id.tvEmptyColleges);

        MaterialButton btnToggleMissingForm = view.findViewById(R.id.btnToggleMissingForm);
        LinearLayout layoutMissingForm = view.findViewById(R.id.layoutMissingCollegeForm);
        EditText etMissingName = view.findViewById(R.id.etMissingCollegeName);
        EditText etMissingCity = view.findViewById(R.id.etMissingCollegeCity);
        EditText etMissingDistrict = view.findViewById(R.id.etMissingCollegeDistrict);
        MaterialButton btnSubmitMissing = view.findViewById(R.id.btnSubmitMissingCollege);

        if (defaultCity != null && !defaultCity.isEmpty()) etMissingCity.setText(defaultCity);
        if (defaultDistrict != null && !defaultDistrict.isEmpty()) etMissingDistrict.setText(defaultDistrict);

        updateFilterHintText(tvSearchFilterHint);

        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        rvColleges.setLayoutManager(layoutManager);
        adapter = new CollegeAdapter(collegeList, collegeObj -> {
            long id = collegeObj.optLong("id", 0L);
            String name = collegeObj.optString("name", "");
            String badge = collegeObj.optString("chatBadge", "");
            String city = collegeObj.optString("city", "");
            String dist = collegeObj.optString("district", "");
            dialog.dismiss();
            if (listener != null) {
                listener.onCollegeSelected(id, name, badge, city, dist);
            }
        });
        rvColleges.setAdapter(adapter);

        // Infinite Scroll Listener
        rvColleges.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && !isLoading && hasMore) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 4) {
                        executeSearch(currentPage + 1, pbLoading, tvEmpty);
                    }
                }
            }
        });

        // 300ms Debounced Search Input
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) debounceHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    currentQuery = s != null ? s.toString().trim() : "";
                    executeSearch(0, pbLoading, tvEmpty);
                };
                debounceHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipToggleAllNepal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ignoreContextualFilters = isChecked;
            updateFilterHintText(tvSearchFilterHint);
            executeSearch(0, pbLoading, tvEmpty);
        });

        btnToggleMissingForm.setOnClickListener(v -> {
            if (layoutMissingForm.getVisibility() == View.VISIBLE) {
                layoutMissingForm.setVisibility(View.GONE);
            } else {
                layoutMissingForm.setVisibility(View.VISIBLE);
                if (etMissingName.getText().toString().trim().isEmpty() && !currentQuery.isEmpty()) {
                    etMissingName.setText(currentQuery);
                }
            }
        });

        btnSubmitMissing.setOnClickListener(v -> {
            String reqName = etMissingName.getText().toString().trim();
            String reqCity = etMissingCity.getText().toString().trim();
            String reqDist = etMissingDistrict.getText().toString().trim();

            if (reqName.isEmpty()) {
                etMissingName.setError("Institution name is required");
                return;
            }

            btnSubmitMissing.setEnabled(false);
            btnSubmitMissing.setText("Submitting...");

            networkManager.requestMissingCollege(
                    reqName, reqCity, reqDist, defaultLevel, defaultAffiliation,
                    new ProfileNetworkManager.JsonCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            if (activity.isFinishing() || activity.isDestroyed()) return;
                            long newId = response.optLong("id", 0L);
                            String loc = !reqCity.isEmpty() ? reqCity : (!reqDist.isEmpty() ? reqDist : "Nepal");
                            String badge = reqName.substring(0, Math.min(3, reqName.length())).toUpperCase() + " • " + loc;

                            Toast.makeText(activity, "Submitted for verification & selected!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            if (listener != null) {
                                listener.onCollegeSelected(newId, reqName, badge, reqCity, reqDist);
                            }
                        }

                        @Override
                        public void onError(VolleyError error) {
                            if (activity.isFinishing() || activity.isDestroyed()) return;
                            btnSubmitMissing.setEnabled(true);
                            btnSubmitMissing.setText("Submit & Use This College");
                        }
                    }
            );
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        executeSearch(0, pbLoading, tvEmpty);
    }

    private void updateFilterHintText(TextView tvHint) {
        if (ignoreContextualFilters) {
            tvHint.setText("Showing colleges across all of Nepal");
        } else {
            List<String> parts = new ArrayList<>();
            if (defaultDistrict != null && !defaultDistrict.isEmpty()) parts.add(defaultDistrict);
            if (defaultLevel != null && !defaultLevel.isEmpty()) parts.add(defaultLevel);
            if (defaultAffiliation != null && !defaultAffiliation.isEmpty()) parts.add(defaultAffiliation);

            if (parts.isEmpty()) {
                tvHint.setText("Search by college name, short code (e.g. BMC), or city");
            } else {
                tvHint.setText("Filtered by: " + String.join(" • ", parts));
            }
        }
    }

    private void executeSearch(int pageToLoad, ProgressBar pbLoading, TextView tvEmpty) {
        isLoading = true;
        pbLoading.setVisibility(View.VISIBLE);
        if (pageToLoad == 0) {
            tvEmpty.setVisibility(View.GONE);
        }

        String distFilter = ignoreContextualFilters ? null : defaultDistrict;
        String levelFilter = ignoreContextualFilters ? null : defaultLevel;
        // Only filter by affiliation if no search query is typed or if contextual filters are active
        String affFilter = ignoreContextualFilters ? null : defaultAffiliation;

        networkManager.searchColleges(
                currentQuery, distFilter, levelFilter, affFilter, pageToLoad, 30,
                new ProfileNetworkManager.JsonCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
                        isLoading = false;
                        pbLoading.setVisibility(View.GONE);

                        currentPage = response.optInt("currentPage", 0);
                        hasMore = response.optBoolean("hasMore", false);
                        JSONArray dataArr = response.optJSONArray("data");

                        if (pageToLoad == 0) {
                            collegeList.clear();
                        }

                        if (dataArr != null) {
                            for (int i = 0; i < dataArr.length(); i++) {
                                JSONObject obj = dataArr.optJSONObject(i);
                                if (obj != null) collegeList.add(obj);
                            }
                        }

                        adapter.notifyDataSetChanged();

                        // Auto-fallback: If 0 results on first load with strict filters, automatically switch to All Nepal!
                        if (collegeList.isEmpty() && !ignoreContextualFilters && pageToLoad == 0 && currentQuery.isEmpty()) {
                            ignoreContextualFilters = true;
                            executeSearch(0, pbLoading, tvEmpty);
                            return;
                        }

                        tvEmpty.setVisibility(collegeList.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(VolleyError error) {
                        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
                        isLoading = false;
                        pbLoading.setVisibility(View.GONE);
                        if (collegeList.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );
    }

    private static class CollegeAdapter extends RecyclerView.Adapter<CollegeAdapter.ViewHolder> {

        interface OnItemClick {
            void onClick(JSONObject college);
        }

        private final List<JSONObject> items;
        private final OnItemClick onItemClick;

        CollegeAdapter(List<JSONObject> items, OnItemClick onItemClick) {
            this.items = items;
            this.onItemClick = onItemClick;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_college_search, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JSONObject obj = items.get(position);
            String name = obj.optString("name", "Institution");
            String symbol = obj.optString("symbolCharacter", "COL");
            String chatBadge = obj.optString("chatBadge", symbol);
            String board = obj.optString("affiliationBoard", "");
            String type = obj.optString("collegeType", "").replace("_", " ");

            holder.tvSymbol.setText(symbol);
            holder.tvName.setText(name);
            holder.tvChatBadge.setText(chatBadge);

            StringBuilder meta = new StringBuilder();
            if (!board.isEmpty() && !board.equalsIgnoreCase("null")) meta.append("• ").append(board);
            if (!type.isEmpty() && !type.equalsIgnoreCase("null")) meta.append(" • ").append(type);
            holder.tvMeta.setText(meta.toString());

            holder.itemView.setOnClickListener(v -> onItemClick.onClick(obj));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSymbol, tvName, tvChatBadge, tvMeta;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSymbol = itemView.findViewById(R.id.tvCollegeSymbol);
                tvName = itemView.findViewById(R.id.tvCollegeName);
                tvChatBadge = itemView.findViewById(R.id.tvCollegeChatBadge);
                tvMeta = itemView.findViewById(R.id.tvCollegeMeta);
            }
        }
    }
}