package app.nepaliapp.bscnotes.auth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import app.nepaliapp.bscnotes.R;
import app.nepaliapp.bscnotes.auth.fragments.LoginFragment;


public class SigninManagerActivity extends AppCompatActivity {
    FrameLayout fragmentArea;
    TextView slogan;

    public Bundle savedAuthData = null;
    @SuppressLint({"SourceLockedOrientationActivity", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin_manager);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

// 1. Get data from Intent with Null-Safety
        Intent intent = getIntent();

// 1. Get data using the CORRECT keys sent by MainActivity
        String sensorEmail = (intent != null && intent.hasExtra("PRE_FILLED_EMAIL"))
                ? intent.getStringExtra("PRE_FILLED_EMAIL") : null;

        String unSensorEmail = (intent != null && intent.hasExtra("REAL_EMAIL"))
                ? intent.getStringExtra("REAL_EMAIL") : null;

        String deviceId = (intent != null && intent.hasExtra("DEVICE_ID"))
                ? intent.getStringExtra("DEVICE_ID") : "UNKNOWN_DEVICE";

// 2. Pack it into the Bundle for the Fragment
        Bundle bundle = new Bundle();
        bundle.putString("SENSOR_EMAIL", sensorEmail);
        bundle.putString("UNSENSOR_EMAIL", unSensorEmail);
        bundle.putString("DEVICE_ID", deviceId);
//        android.util.Log.d("daa", "Received: " + sensorEmail + " | " + unSensorEmail);
        init();
        slogan.post(() -> {
            // Get total available height from slogan bottom to screen bottom
            int availableHeight = fragmentArea.getRootView().getHeight() - slogan.getBottom();

            // Only update if current height exceeds available
            if (fragmentArea.getHeight() > availableHeight) {
                ViewGroup.LayoutParams params = fragmentArea.getLayoutParams();
                params.height = availableHeight - 15; // limit the fragment height
                fragmentArea.setLayoutParams(params);
            }
        });

        if (savedInstanceState == null) {
            replaceFragmentsForLogin(new LoginFragment(), bundle);
        }
        findViewById(android.R.id.content).setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });

        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

            }
        };

        findViewById(android.R.id.content).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideKeyboard();
                return false;
            }
        });
    }

    public void init() {
        fragmentArea = findViewById(R.id.Fragmentarea);
        slogan = findViewById(R.id.slogan);
    }

    // --- THE ADJUSTMENT METHOD FROM YOUR OLD PROJECT ---
    public void adjustFragmentHeight() {
        slogan.post(() -> {
            int screenHeight = fragmentArea.getRootView().getHeight();
            int availableHeight = screenHeight - slogan.getBottom() - 10; // 10px margin below slogan

            ViewGroup.LayoutParams params = fragmentArea.getLayoutParams();

            // Measure fragment content height
            int contentHeight = 0;
            if (fragmentArea.getChildAt(0) != null) {
                fragmentArea.getChildAt(0).measure(
                        View.MeasureSpec.makeMeasureSpec(fragmentArea.getWidth(), View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                contentHeight = fragmentArea.getChildAt(0).getMeasuredHeight();
            }

            // Set height: smaller of contentHeight or availableHeight
            params.height = Math.min(contentHeight, availableHeight);

            fragmentArea.setLayoutParams(params);
        });
    }

    public void replaceFragmentsForLogin(Fragment fragment, Bundle data){
        if (data != null) {
            fragment.setArguments(data);
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.Fragmentarea, fragment);
        fragmentTransaction.commit();

        // Your existing height adjustment logic
        fragmentArea.post(this::adjustFragmentHeight);

    }

    public void replaceFragments(Fragment fragment) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.Fragmentarea);
        if (currentFragment instanceof LoginFragment) {
            this.savedAuthData = currentFragment.getArguments();
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.Fragmentarea, fragment);
        fragmentTransaction.addToBackStack(null); // Keep this to allow "Back" button
        fragmentTransaction.commit();

        // Adjust height after fragment is attached
        fragmentArea.post(this::adjustFragmentHeight);
    }



    // Method to hide the keyboard
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}