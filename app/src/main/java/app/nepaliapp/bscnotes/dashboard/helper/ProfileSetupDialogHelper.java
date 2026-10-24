package app.nepaliapp.bscnotes.dashboard.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import app.nepaliapp.bscnotes.R;
import app.nepaliapp.bscnotes.common.DataStorage;

public class ProfileSetupDialogHelper {

    public interface OnProfileSavedListener {
        void onProfileSaved(JSONObject savedProfile);
    }

    private final Activity activity;
    private final JSONObject bootstrapData;
    private final boolean forceCompletion;
    private final ProfileNetworkManager networkManager;
    private final DataStorage dataStorage;
    private final OnProfileSavedListener listener;

    // Parsed Remote Rules
    private boolean showCourseField = true;
    private boolean showSessionField = true;
    private boolean showCollegeField = true;
    private boolean collegeMandatory = false;
    private boolean showLocationField = true;
    private boolean isForeigner = false;
    private int serverConfigVersion = 1;
    private String detectedCountryCode = "NP";

    // Available Metadata Lists from Bootstrap
    private final List<JSONObject> availableCourses = new ArrayList<>();
    private final List<JSONObject> availableLocations = new ArrayList<>();

    // Current User Selection State
    private Long selectedCourseId = null;
    private String selectedCourseLevel = null;
    private String selectedCourseAffiliation = null;
    private Integer selectedSessionNumber = null; // 1-based index (1..8)
    private String selectedSessionLabel = null;

    private Long selectedLocationId = null;
    private String selectedDistrict = null;
    private String selectedCity = null;

    private Long selectedCollegeId = null;
    private String selectedCollegeName = null;
    private String selectedCollegeBadge = null;

    // Pre-computed High-Contrast ColorStateLists for Session Chips
    private final ColorStateList chipBgColors;
    private final ColorStateList chipTextColors;
    private final ColorStateList chipStrokeColors;

    public ProfileSetupDialogHelper(
            Activity activity,
            JSONObject bootstrapData,
            boolean forceCompletion,
            OnProfileSavedListener listener) {
        this.activity = activity;
        this.bootstrapData = bootstrapData;
        this.forceCompletion = forceCompletion;
        this.networkManager = new ProfileNetworkManager(activity);
        this.dataStorage = new DataStorage(activity);
        this.listener = listener;

        // Initialize High-Contrast Chip States once (Selected vs Unselected)
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };

        // Selected: Royal Blue (#2563EB) | Unselected: Soft Slate (#F8FAFC)
        this.chipBgColors = new ColorStateList(states, new int[]{
                Color.parseColor("#2563EB"),
                Color.parseColor("#F8FAFC")
        });

        // Selected: Pure White (#FFFFFF) | Unselected: Dark Slate (#334155)
        this.chipTextColors = new ColorStateList(states, new int[]{
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#334155")
        });

        // Selected: Deep Blue (#1D4ED8) | Unselected: Card Border (#CBD5E1)
        this.chipStrokeColors = new ColorStateList(states, new int[]{
                Color.parseColor("#1D4ED8"),
                Color.parseColor("#CBD5E1")
        });
    }

    public void show() {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || bootstrapData == null) {
            return;
        }

        parseBootstrapPayload();

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_profile_setup, null);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(!forceCompletion)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Bind Views
        TextView tvCountryBadge = view.findViewById(R.id.tvCountryBadge);
        TextInputEditText etStudentName = view.findViewById(R.id.etStudentName);

        LinearLayout sectionCourse = view.findViewById(R.id.sectionCourseField);
        MaterialCardView cardSelectCourse = view.findViewById(R.id.cardSelectCourse);
        TextView tvSelectedCourse = view.findViewById(R.id.tvSelectedCourse);

        LinearLayout sectionSession = view.findViewById(R.id.sectionSessionField);
        TextView tvSelectedSessionBadge = view.findViewById(R.id.tvSelectedSessionBadge);
        ChipGroup chipGroupSessions = view.findViewById(R.id.chipGroupSessions);

        LinearLayout sectionLocation = view.findViewById(R.id.sectionLocationField);
        MaterialCardView cardSelectLocation = view.findViewById(R.id.cardSelectLocation);
        TextView tvSelectedLocation = view.findViewById(R.id.tvSelectedLocation);

        LinearLayout sectionCollege = view.findViewById(R.id.sectionCollegeField);
        TextView tvCollegeLabel = view.findViewById(R.id.tvCollegeLabel);
        TextView btnClearCollege = view.findViewById(R.id.btnClearCollege);
        MaterialCardView cardSelectCollege = view.findViewById(R.id.cardSelectCollege);
        TextView tvSelectedCollegeName = view.findViewById(R.id.tvSelectedCollegeName);
        TextView tvSelectedCollegeBadge = view.findViewById(R.id.tvSelectedCollegeBadge);

        MaterialButton btnSave = view.findViewById(R.id.btnSaveProfile);
        ProgressBar pbSave = view.findViewById(R.id.pbSaveProfile);
        MaterialButton btnDismiss = view.findViewById(R.id.btnDismissProfileDialog);

        // 1. Apply Foreigner & Country Badge
        tvCountryBadge.setText(isForeigner ? (detectedCountryCode + " • INTL") : detectedCountryCode);

        // 2. Apply Remote UI Visibility Switches (formRules)
        sectionCourse.setVisibility(showCourseField ? View.VISIBLE : View.GONE);
        sectionSession.setVisibility((showCourseField && showSessionField) ? View.VISIBLE : View.GONE);
        sectionLocation.setVisibility((showLocationField && !isForeigner) ? View.VISIBLE : View.GONE);
        sectionCollege.setVisibility((showCollegeField && !isForeigner) ? View.VISIBLE : View.GONE);

        if (showCollegeField && !isForeigner) {
            tvCollegeLabel.setText(collegeMandatory ? "College / Campus *" : "College / Campus (Optional)");
        }

        if (!forceCompletion) {
            btnDismiss.setVisibility(View.VISIBLE);
            btnDismiss.setOnClickListener(v -> dialog.dismiss());
        }

        // 3. Pre-Fill Existing / Cross-App Profile Values
        JSONObject profile = bootstrapData.optJSONObject("profile");
        if (profile != null) {
            String existingName = cleanStr(profile.optString("userName", ""));
            if (!existingName.isEmpty() && !existingName.startsWith("User #")) {
                etStudentName.setText(existingName);
            }

            if (!profile.isNull("courseTypeId")) {
                long cId = profile.optLong("courseTypeId", 0L);
                if (cId > 0) selectedCourseId = cId;
            }
            if (!profile.isNull("currentSession")) {
                int sess = profile.optInt("currentSession", 0);
                if (sess > 0) selectedSessionNumber = sess;
            }
            selectedSessionLabel = cleanStr(profile.optString("sessionLabel", ""));

            if (!profile.isNull("locationId")) {
                long locId = profile.optLong("locationId", 0L);
                if (locId > 0) selectedLocationId = locId;
            }
            selectedDistrict = cleanStr(profile.optString("district", ""));
            selectedCity = cleanStr(profile.optString("city", ""));

            if (!profile.isNull("collegeId")) {
                long colId = profile.optLong("collegeId", 0L);
                if (colId > 0) selectedCollegeId = colId;
            }
            selectedCollegeName = cleanStr(profile.optString("collegeName", ""));
            selectedCollegeBadge = cleanStr(profile.optString("collegeChatBadge", ""));
        }

        // If only 1 course is configured for this app and none is selected, auto-select it!
        if (selectedCourseId == null && availableCourses.size() == 1) {
            selectedCourseId = availableCourses.get(0).optLong("id");
        }

        // Render initial pre-filled Course & Session Chips
        refreshCourseAndSessionUI(tvSelectedCourse, sectionSession, tvSelectedSessionBadge, chipGroupSessions);
        refreshLocationUI(tvSelectedLocation);
        refreshCollegeUI(tvSelectedCollegeName, tvSelectedCollegeBadge, btnClearCollege);

        // 4. Course Selector Click
        cardSelectCourse.setOnClickListener(v ->
                showCoursePickerDialog(tvSelectedCourse, sectionSession, tvSelectedSessionBadge, chipGroupSessions));

        // 5. Location Selector Click
        cardSelectLocation.setOnClickListener(v -> showLocationPickerDialog(tvSelectedLocation));

        // 6. College Selector Click
        cardSelectCollege.setOnClickListener(v -> {
            CollegeSearchDialogHelper searchDialog = new CollegeSearchDialogHelper(
                    activity,
                    networkManager,
                    selectedDistrict,
                    selectedCity,
                    selectedCourseLevel,
                    selectedCourseAffiliation,
                    (colId, colName, chatBadge, city, district) -> {
                        selectedCollegeId = colId;
                        selectedCollegeName = colName;
                        selectedCollegeBadge = chatBadge;

                        // Smart Auto-Fill Location from selected College if user hasn't picked a location yet
                        if ((selectedDistrict == null || selectedDistrict.isEmpty()) && district != null && !district.isEmpty()) {
                            selectedDistrict = district;
                            if (city != null && !city.isEmpty()) selectedCity = city;
                            refreshLocationUI(tvSelectedLocation);
                        }
                        refreshCollegeUI(tvSelectedCollegeName, tvSelectedCollegeBadge, btnClearCollege);
                    }
            );
            searchDialog.show();
        });

        btnClearCollege.setOnClickListener(v -> {
            selectedCollegeId = null;
            selectedCollegeName = null;
            selectedCollegeBadge = null;
            refreshCollegeUI(tvSelectedCollegeName, tvSelectedCollegeBadge, btnClearCollege);
        });

        // 7. Save Button Click
        btnSave.setOnClickListener(v -> {
            String studentName = etStudentName.getText() != null ? etStudentName.getText().toString().trim() : "";

            // Validate required fields based on active formRules
            if (showCourseField && (selectedCourseId == null || selectedCourseId <= 0)) {
                Toast.makeText(activity, "Please select your Course / Program", Toast.LENGTH_SHORT).show();
                return;
            }
            if (showCourseField && showSessionField && (selectedSessionNumber == null || selectedSessionNumber <= 0)) {
                Toast.makeText(activity, "Please select your current Semester / Year", Toast.LENGTH_SHORT).show();
                return;
            }
            if (showLocationField && !isForeigner && (selectedDistrict == null || selectedDistrict.isEmpty())) {
                Toast.makeText(activity, "Please select your District & City", Toast.LENGTH_SHORT).show();
                return;
            }
            if (showCollegeField && !isForeigner && collegeMandatory && (selectedCollegeId == null || selectedCollegeId <= 0)) {
                Toast.makeText(activity, "Please select your College / Campus", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject savePayload = buildSaveRequestJson(studentName);

            btnSave.setEnabled(false);
            btnSave.setText("");
            pbSave.setVisibility(View.VISIBLE);

            networkManager.saveUserProfile(savePayload, new ProfileNetworkManager.JsonCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    if (activity.isFinishing() || activity.isDestroyed()) return;
                    JSONObject savedProfile = response.optJSONObject("profile");
                    dataStorage.saveCompletedUserProfile(savedProfile, serverConfigVersion);

                    Toast.makeText(activity, "Profile updated!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    if (listener != null) {
                        listener.onProfileSaved(savedProfile);
                    }
                }

                @Override
                public void onError(VolleyError error) {
                    if (activity.isFinishing() || activity.isDestroyed()) return;
                    btnSave.setEnabled(true);
                    btnSave.setText("Save & Continue");
                    pbSave.setVisibility(View.GONE);
                }
            });
        });

        dialog.show();
    }

    private void parseBootstrapPayload() {
        serverConfigVersion = bootstrapData.optInt("configVersion", 1);
        isForeigner = bootstrapData.optBoolean("isForeigner", false);
        detectedCountryCode = bootstrapData.optString("detectedCountryCode", "NP");

        JSONObject rules = bootstrapData.optJSONObject("formRules");
        if (rules != null) {
            showCourseField = rules.optBoolean("showCourseField", true);
            showSessionField = rules.optBoolean("showSessionField", true);
            showCollegeField = rules.optBoolean("showCollegeField", true);
            collegeMandatory = rules.optBoolean("collegeMandatory", false);
            showLocationField = rules.optBoolean("showLocationField", true);
        }

        availableCourses.clear();
        JSONArray coursesArr = bootstrapData.optJSONArray("availableCourses");
        if (coursesArr != null) {
            for (int i = 0; i < coursesArr.length(); i++) {
                JSONObject c = coursesArr.optJSONObject(i);
                if (c != null) availableCourses.add(c);
            }
        }

        availableLocations.clear();
        JSONArray locsArr = bootstrapData.optJSONArray("availableLocations");
        if (locsArr != null) {
            for (int i = 0; i < locsArr.length(); i++) {
                JSONObject l = locsArr.optJSONObject(i);
                if (l != null) availableLocations.add(l);
            }
        }
    }

    private void refreshCourseAndSessionUI(
            TextView tvSelectedCourse,
            LinearLayout sectionSession,
            TextView tvSelectedSessionBadge,
            ChipGroup chipGroupSessions) {

        JSONObject matchedCourse = null;
        if (selectedCourseId != null) {
            for (JSONObject c : availableCourses) {
                if (c.optLong("id") == selectedCourseId) {
                    matchedCourse = c;
                    break;
                }
            }
        }

        if (matchedCourse == null) {
            tvSelectedCourse.setText("Tap to choose your Course...");
            tvSelectedCourse.setTextColor(Color.parseColor("#64748B"));
            chipGroupSessions.removeAllViews();
            sectionSession.setVisibility(View.GONE);
            return;
        }

        String code = matchedCourse.optString("courseCode", "");
        String name = matchedCourse.optString("name", "");
        selectedCourseLevel = matchedCourse.optString("level", "");
        selectedCourseAffiliation = matchedCourse.optString("affiliationBoard", "");

        tvSelectedCourse.setText(code.isEmpty() ? name : (code + " — " + name));
        tvSelectedCourse.setTextColor(Color.parseColor("#0F172A"));

        if (!showSessionField) {
            sectionSession.setVisibility(View.GONE);
            return;
        }

        JSONArray sessionOptions = matchedCourse.optJSONArray("sessionOptions");
        chipGroupSessions.removeAllViews();

        if (sessionOptions == null || sessionOptions.length() == 0) {
            sectionSession.setVisibility(View.GONE);
            return;
        }

        sectionSession.setVisibility(View.VISIBLE);

        // Reset session if stored session index exceeds this course's total options
        if (selectedSessionNumber != null && (selectedSessionNumber < 1 || selectedSessionNumber > sessionOptions.length())) {
            selectedSessionNumber = null;
            selectedSessionLabel = null;
        }

        float density = activity.getResources().getDisplayMetrics().density;
        boolean anyPreSelected = false;

        // Populate 1-Based High-Contrast Session Chips (Index 0 -> Session 1)
        for (int i = 0; i < sessionOptions.length(); i++) {
            final int oneBasedSessionNum = i + 1;
            final String label = sessionOptions.optString(i, "Session " + oneBasedSessionNum);

            Chip chip = new Chip(activity);
            chip.setId(View.generateViewId());
            chip.setText(label);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setTextSize(13f);

            // Apply High-Contrast Visual Styling
            chip.setChipBackgroundColor(chipBgColors);
            chip.setTextColor(chipTextColors);
            chip.setChipStrokeColor(chipStrokeColors);
            chip.setChipStrokeWidth(1.2f * density);
            chip.setChipCornerRadius(10f * density);
            chip.setChipMinHeight(40f * density);

            // Show clear white checkmark icon when selected
            chip.setCheckedIconVisible(true);
            chip.setCheckedIconTint(ColorStateList.valueOf(Color.WHITE));

            // Add comfortable horizontal padding
            chip.setChipStartPadding(10f * density);
            chip.setChipEndPadding(10f * density);

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                chip.setTypeface(null, isChecked ? Typeface.BOLD : Typeface.NORMAL);
                if (isChecked) {
                    selectedSessionNumber = oneBasedSessionNum;
                    selectedSessionLabel = label;
                    updateSessionStatusBadge(tvSelectedSessionBadge, label);
                }
            });

            chipGroupSessions.addView(chip);

            if (selectedSessionNumber != null && selectedSessionNumber == oneBasedSessionNum) {
                chip.setChecked(true);
                anyPreSelected = true;
            }
        }

        if (!anyPreSelected) {
            updateSessionStatusBadge(tvSelectedSessionBadge, null);
        }
    }

    private void updateSessionStatusBadge(TextView tvSelectedSessionBadge, String activeLabel) {
        if (tvSelectedSessionBadge == null) return;

        if (activeLabel != null && !activeLabel.isEmpty()) {
            tvSelectedSessionBadge.setText("✓ " + activeLabel);
            tvSelectedSessionBadge.setTextColor(Color.parseColor("#15803D")); // Crisp Green Text
            tvSelectedSessionBadge.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#DCFCE7"))); // Soft Green Pill
        } else {
            tvSelectedSessionBadge.setText("Tap to Select");
            tvSelectedSessionBadge.setTextColor(Color.parseColor("#D97706")); // Amber Warning Text
            tvSelectedSessionBadge.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#FEF3C7"))); // Soft Amber Pill
        }
    }

    private void showCoursePickerDialog(
            TextView tvSelectedCourse,
            LinearLayout sectionSession,
            TextView tvSelectedSessionBadge,
            ChipGroup chipGroupSessions) {

        if (availableCourses.isEmpty()) {
            Toast.makeText(activity, "No active courses available for this app.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = new String[availableCourses.size()];
        int checkedItem = -1;
        for (int i = 0; i < availableCourses.size(); i++) {
            JSONObject c = availableCourses.get(i);
            String code = c.optString("courseCode", "");
            String name = c.optString("name", "");
            items[i] = code.isEmpty() ? name : (code + " — " + name);
            if (selectedCourseId != null && c.optLong("id") == selectedCourseId) {
                checkedItem = i;
            }
        }

        new AlertDialog.Builder(activity)
                .setTitle("Select Course / Program")
                .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                    JSONObject picked = availableCourses.get(which);
                    long newCourseId = picked.optLong("id");
                    if (selectedCourseId == null || selectedCourseId != newCourseId) {
                        selectedCourseId = newCourseId;
                        selectedSessionNumber = 1; // Default to 1st session when switching course
                        selectedSessionLabel = null;
                    }
                    refreshCourseAndSessionUI(tvSelectedCourse, sectionSession, tvSelectedSessionBadge, chipGroupSessions);
                    dialog.dismiss();
                })
                .show();
    }

    private void showLocationPickerDialog(TextView tvSelectedLocation) {
        if (availableLocations.isEmpty()) {
            // Fallback custom input if admin hasn't uploaded locations CSV yet
            showManualLocationDialog(tvSelectedLocation);
            return;
        }

        String[] items = new String[availableLocations.size()];
        int checkedItem = -1;
        for (int i = 0; i < availableLocations.size(); i++) {
            JSONObject loc = availableLocations.get(i);
            String city = loc.optString("city", "");
            String dist = loc.optString("district", "");
            items[i] = city + " (" + dist + ")";
            if (selectedLocationId != null && loc.optLong("id") == selectedLocationId) {
                checkedItem = i;
            }
        }

        new AlertDialog.Builder(activity)
                .setTitle("Select Your City & District")
                .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                    JSONObject picked = availableLocations.get(which);
                    selectedLocationId = picked.optLong("id");
                    selectedCity = picked.optString("city", "");
                    selectedDistrict = picked.optString("district", "");
                    refreshLocationUI(tvSelectedLocation);
                    dialog.dismiss();
                })
                .setNeutralButton("Type Manually", (dialog, which) -> showManualLocationDialog(tvSelectedLocation))
                .show();
    }

    private void showManualLocationDialog(TextView tvSelectedLocation) {
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * activity.getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);

        final TextInputEditText etDist = new TextInputEditText(activity);
        etDist.setHint("District (e.g. Rupandehi)");
        if (selectedDistrict != null) etDist.setText(selectedDistrict);

        final TextInputEditText etCty = new TextInputEditText(activity);
        etCty.setHint("City / Town (e.g. Butwal)");
        if (selectedCity != null) etCty.setText(selectedCity);

        container.addView(etDist);
        container.addView(etCty);

        new AlertDialog.Builder(activity)
                .setTitle("Enter Your Location")
                .setView(container)
                .setPositiveButton("Apply", (d, w) -> {
                    selectedLocationId = null;
                    selectedDistrict = etDist.getText() != null ? etDist.getText().toString().trim() : "";
                    selectedCity = etCty.getText() != null ? etCty.getText().toString().trim() : "";
                    refreshLocationUI(tvSelectedLocation);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshLocationUI(TextView tvSelectedLocation) {
        if (selectedDistrict != null && !selectedDistrict.isEmpty()) {
            String display = (selectedCity != null && !selectedCity.isEmpty())
                    ? (selectedCity + ", " + selectedDistrict)
                    : selectedDistrict;
            tvSelectedLocation.setText(display);
            tvSelectedLocation.setTextColor(Color.parseColor("#0F172A"));
        } else {
            tvSelectedLocation.setText("Select District & City...");
            tvSelectedLocation.setTextColor(Color.parseColor("#64748B"));
        }
    }

    private void refreshCollegeUI(TextView tvName, TextView tvBadge, TextView btnClear) {
        if (selectedCollegeId != null && selectedCollegeId > 0 && selectedCollegeName != null && !selectedCollegeName.isEmpty()) {
            tvName.setText(selectedCollegeName);
            tvName.setTextColor(Color.parseColor("#0F172A"));

            if (selectedCollegeBadge != null && !selectedCollegeBadge.isEmpty()) {
                tvBadge.setText("Chat Badge: " + selectedCollegeBadge);
                tvBadge.setVisibility(View.VISIBLE);
            } else {
                tvBadge.setVisibility(View.GONE);
            }
            btnClear.setVisibility(collegeMandatory ? View.GONE : View.VISIBLE);
        } else {
            tvName.setText(collegeMandatory ? "Search your College / Campus *" : "Search your College / Campus (Optional)");
            tvName.setTextColor(Color.parseColor("#64748B"));
            tvBadge.setVisibility(View.GONE);
            btnClear.setVisibility(View.GONE);
        }
    }

    private JSONObject buildSaveRequestJson(String studentName) {
        JSONObject body = new JSONObject();
        try {
            body.put("name", studentName.isEmpty() ? JSONObject.NULL : studentName);
            body.put("countryCode", detectedCountryCode);

            if (showCourseField && selectedCourseId != null && selectedCourseId > 0) {
                body.put("courseTypeId", selectedCourseId);
            } else {
                body.put("courseTypeId", JSONObject.NULL);
            }

            if (showCourseField && showSessionField && selectedSessionNumber != null && selectedSessionNumber > 0) {
                body.put("currentSession", selectedSessionNumber);
                body.put("sessionLabel", selectedSessionLabel != null ? selectedSessionLabel : JSONObject.NULL);
            } else {
                body.put("currentSession", JSONObject.NULL);
                body.put("sessionLabel", JSONObject.NULL);
            }

            if (showCollegeField && !isForeigner && selectedCollegeId != null && selectedCollegeId > 0) {
                body.put("collegeId", selectedCollegeId);
            } else {
                body.put("collegeId", JSONObject.NULL);
            }

            if (showLocationField && !isForeigner) {
                body.put("locationId", (selectedLocationId != null && selectedLocationId > 0) ? selectedLocationId : JSONObject.NULL);
                body.put("district", (selectedDistrict != null && !selectedDistrict.isEmpty()) ? selectedDistrict : JSONObject.NULL);
                body.put("city", (selectedCity != null && !selectedCity.isEmpty()) ? selectedCity : JSONObject.NULL);
            } else {
                body.put("locationId", JSONObject.NULL);
                body.put("district", JSONObject.NULL);
                body.put("city", JSONObject.NULL);
            }
        } catch (JSONException ignored) {
        }
        return body;
    }

    private String cleanStr(String val) {
        if (val == null || val.equalsIgnoreCase("null")) return "";
        return val.trim();
    }
}