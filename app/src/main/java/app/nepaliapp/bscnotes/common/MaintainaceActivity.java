package app.nepaliapp.bscnotes.common;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import app.nepaliapp.bscnotes.R;


public class MaintainaceActivity extends AppCompatActivity {

    private static final String TAG = "MaintainaceActivity";
    private TextView tvCountdownTimer, descriptionTxt;
    private FrameLayout loadingOverlay;

    // FIX: Declare the timer globally
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_maintainace);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Initialize views first
        findByID();
        loadingOverlay.setVisibility(View.VISIBLE);

        // 2. Fetch Intent Extras safely
        String titleMessage = getIntent().getStringExtra("Title_Message");
        String endTime = getIntent().getStringExtra("END_TIME");

        // 3. Apply safe fallbacks for Title UI
        if (titleMessage != null && !titleMessage.trim().isEmpty()) {
            descriptionTxt.setText(titleMessage);
        } else {
            descriptionTxt.setText("Unknown error occurred, so solving it soon.");
        }

        // 4. Apply safe fallbacks for Countdown/API UI
        if (endTime != null && !endTime.trim().isEmpty()) {
            fetchNepaliTime(endTime);
        } else {
            tvCountdownTimer.setText("Very Soon App Server Will Start");
            loadingOverlay.setVisibility(View.GONE);
        }
    }

    private void findByID() {
        tvCountdownTimer = findViewById(R.id.tvCountdownTimer);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        descriptionTxt = findViewById(R.id.tvMaintenanceSubText);
    }

    private void fetchNepaliTime(String END_TIME) {
        String apiUrl = "https://timeapi.io/api/Time/current/zone?timeZone=Asia/Kathmandu";

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                apiUrl,
                null,
                response -> {
                    try {
                        String dateTime = response.getString("dateTime");

                        // Parse API datetime
                        SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                        apiFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kathmandu"));
                        Date currentDate = apiFormat.parse(dateTime.substring(0, 19));

                        // Parse maintenance end time
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kathmandu"));
                        Date endDate = sdf.parse(END_TIME);

                        if (currentDate != null && endDate != null) {
                            long diffMillis = endDate.getTime() - currentDate.getTime();
                            startCountdown(diffMillis);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Parse error: " + e.getMessage());
                        tvCountdownTimer.setText("Error");
                        loadingOverlay.setVisibility(View.GONE);
                    }
                },
                error -> {
                    Log.e(TAG, "API Error: " + error.toString());
                    tvCountdownTimer.setText("Failed to load time");
                    loadingOverlay.setVisibility(View.GONE);
                }
        );
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        queue.add(jsonObjectRequest);
    }

    private void startCountdown(long diffMillis) {
        // FIX: Hide overlay once, rather than every second inside onTick
        loadingOverlay.setVisibility(View.GONE);

        if (diffMillis <= 0) {
            tvCountdownTimer.setText("00:00:00");
            return;
        }

        // FIX: Assign to the global variable
        countDownTimer = new CountDownTimer(diffMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long days = millisUntilFinished / (1000 * 60 * 60 * 24);
                long hours = (millisUntilFinished / (1000 * 60 * 60)) % 24;
                long minutes = (millisUntilFinished / (1000 * 60)) % 60;
                long seconds = (millisUntilFinished / 1000) % 60;

                String timeFormatted;
                if (days > 0) {
                    timeFormatted = String.format(Locale.getDefault(),
                            "%d days %02d:%02d:%02d", days, hours, minutes, seconds);
                } else {
                    timeFormatted = String.format(Locale.getDefault(),
                            "%02d:%02d:%02d", hours, minutes, seconds);
                }

                tvCountdownTimer.setText(timeFormatted);
            }

            @Override
            public void onFinish() {
                tvCountdownTimer.setText("00:00:00");
            }
        }.start();
    }

    // FIX: Cancel the timer if the user closes the app or leaves the screen
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}