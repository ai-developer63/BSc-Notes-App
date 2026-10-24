package app.nepaliapp.bscnotes.dashboard;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.VolleyError;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import app.nepaliapp.bscnotes.R;
import app.nepaliapp.bscnotes.common.DataStorage;
import app.nepaliapp.bscnotes.dashboard.helper.ProfileNetworkManager;
import app.nepaliapp.bscnotes.dashboard.helper.ProfileSetupDialogHelper;

public class ProfileActivity extends AppCompatActivity {
    private TextView tvDashboardUserName;
    private TextView tvDashboardCourseSession;
    private TextView tvDashboardCollegeBadge;
    private MaterialButton btnEditStudyProfile;

    private DataStorage dataStorage;
    private ProfileNetworkManager profileNetworkManager;
    private JSONObject latestBootstrapPayload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dash_board_manager);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        renderHeaderFromLocalCache();
        syncProfileBootstrapFromServer(false);
    }

    private void initViews() {
        dataStorage = new DataStorage(this);
        profileNetworkManager = new ProfileNetworkManager(this);

        tvDashboardUserName = findViewById(R.id.tvDashboardUserName);
        tvDashboardCourseSession = findViewById(R.id.tvDashboardCourseSession);
        tvDashboardCollegeBadge = findViewById(R.id.tvDashboardCollegeBadge);
        btnEditStudyProfile = findViewById(R.id.btnEditStudyProfile);

        btnEditStudyProfile.setOnClickListener(v -> {
            if (latestBootstrapPayload != null) {
                openProfileSetupDialog(latestBootstrapPayload, false);
            } else {
                syncProfileBootstrapFromServer(true);
            }
        });
    }

    /**
     * Step 1 of Golden Path: Call /api/user-profile/bootstrap on Launch
     */
    private void syncProfileBootstrapFromServer(boolean forceOpenDialog) {
        profileNetworkManager.fetchBootstrapProfile(new ProfileNetworkManager.JsonCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (isFinishing() || isDestroyed()) return;

                latestBootstrapPayload = response;

                int prevSeenVersion = dataStorage.getLastSeenConfigVersion();
                int serverConfigVersion = response.optInt("configVersion", 1);
                boolean profileComplete = response.optBoolean("profileComplete", false);

                // Cache pre-filled or completed profile locally
                dataStorage.saveUserProfileFromBootstrap(response);
                renderHeaderFromLocalCache();

                // Open setup dialog if incomplete, if admin bumped configVersion, or if user clicked "Edit Profile"
                if (forceOpenDialog || !profileComplete || serverConfigVersion > prevSeenVersion) {
                    openProfileSetupDialog(response, !profileComplete);
                }
            }

            @Override
            public void onError(VolleyError error) {
                if (isFinishing() || isDestroyed()) return;
                if (forceOpenDialog) {
                    Toast.makeText(ProfileActivity.this, "Unable to load profile settings. Check connection.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openProfileSetupDialog(JSONObject bootstrapPayload, boolean isMandatoryCompletion) {
        ProfileSetupDialogHelper dialogHelper = new ProfileSetupDialogHelper(
                this,
                bootstrapPayload,
                isMandatoryCompletion,
                savedProfile -> {
                    renderHeaderFromLocalCache();
                    // Refresh bootstrap silently in background so latestBootstrapPayload has updated state
                    profileNetworkManager.fetchBootstrapProfile(new ProfileNetworkManager.JsonCallback() {
                        @Override
                        public void onSuccess(JSONObject refreshed) {
                            latestBootstrapPayload = refreshed;
                            dataStorage.saveUserProfileFromBootstrap(refreshed);
                        }

                        @Override
                        public void onError(VolleyError error) {}
                    });
                }
        );
        dialogHelper.show();
    }

    /**
     * Reads cached profile in 0ms from DataStorage for UI headers and Chat Badges
     */
    @SuppressLint("SetTextI18n")
    private void renderHeaderFromLocalCache() {
        String name = dataStorage.getProfileUserName();
        String courseCode = dataStorage.getCourseCode();
        String courseName = dataStorage.getCourseName();
        String sessionLabel = dataStorage.getSessionLabel();
        String chatBadge = dataStorage.getCollegeChatBadge();
        String collegeName = dataStorage.getCollegeName();
        String city = dataStorage.getCity();
        String district = dataStorage.getDistrict();

        tvDashboardUserName.setText(!name.isEmpty() ? ("Hi, " + name) : "Welcome, Student");

        StringBuilder courseLine = new StringBuilder();
        if (!courseCode.isEmpty()) {
            courseLine.append(courseCode);
        } else if (!courseName.isEmpty()) {
            courseLine.append(courseName);
        }
        if (!sessionLabel.isEmpty()) {
            if (courseLine.length() > 0) courseLine.append(" • ");
            courseLine.append(sessionLabel);
        }
        tvDashboardCourseSession.setText(courseLine.length() > 0 ? courseLine.toString() : "Tap 'Edit Profile' to select Course & Semester");

        if (!chatBadge.isEmpty()) {
            tvDashboardCollegeBadge.setText(chatBadge + (!collegeName.isEmpty() ? " (" + collegeName + ")" : ""));
        } else if (!district.isEmpty()) {
            tvDashboardCollegeBadge.setText(!city.isEmpty() ? (city + ", " + district) : district);
        } else if (dataStorage.isForeigner()) {
            tvDashboardCollegeBadge.setText("International Student (" + dataStorage.getProfileCountryCode() + ")");
        } else {
            tvDashboardCollegeBadge.setText("No institution selected");
        }
    }
}