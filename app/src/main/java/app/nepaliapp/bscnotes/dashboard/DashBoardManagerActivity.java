package app.nepaliapp.bscnotes.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.VolleyError;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

import app.nepaliapp.bscnotes.R;
import app.nepaliapp.bscnotes.common.DataStorage;
import app.nepaliapp.bscnotes.dashboard.fragments.ChatFragment;
import app.nepaliapp.bscnotes.dashboard.fragments.CourseFragment;
import app.nepaliapp.bscnotes.dashboard.fragments.HomeFragment;
import app.nepaliapp.bscnotes.dashboard.fragments.UnlockFragment;
import app.nepaliapp.bscnotes.dashboard.fragments.VideosFragment;
import app.nepaliapp.bscnotes.dashboard.helper.ProfileNetworkManager;
import app.nepaliapp.bscnotes.dashboard.helper.ProfileSetupDialogHelper;

public class DashBoardManagerActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private View bottomNavDivider;
    private ProfileNetworkManager profileNetworkManager;
    private DataStorage dataStorage;

    // Tracks which of the 5 Root Tabs is currently active
    private Fragment activeBaseFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dash_board_manager);

        profileNetworkManager = new ProfileNetworkManager(this);
        dataStorage = new DataStorage(this);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavDivider = findViewById(R.id.bottomNavDivider);

        setupStatusBarAndInsets();
        setupBottomNavigation();
        setupUnifiedBackListener();

        if (savedInstanceState == null) {
            switchBaseTab("HOME", new HomeFragment());
        } else {
            // Re-sync activeBaseFragment reference after rotation
            activeBaseFragment = getSupportFragmentManager().findFragmentByTag(getCurrentBaseTag());
        }

        syncProfileBootstrapFromServer();
    }

    private void setupBottomNavigation() {
        BadgeDrawable chatBadge = bottomNavigationView.getOrCreateBadge(R.id.nav_chat);
        chatBadge.setBackgroundColor(Color.parseColor("#16A34A"));
        chatBadge.setVisible(true);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // Always clear any open deep screens if the user taps a Bottom Nav tab
            clearDeepFragmentsImmediately();

            if (id == R.id.nav_home) {
                switchBaseTab("HOME", new HomeFragment());
                return true;
            } else if (id == R.id.nav_courses) {
                switchBaseTab("COURSES", new CourseFragment());
                return true;
            } else if (id == R.id.nav_videos) {
                switchBaseTab("VIDEOS", new VideosFragment());
                return true;
            } else if (id == R.id.nav_unlock) {
                switchBaseTab("UNLOCK", new UnlockFragment());
                return true;
            } else if (id == R.id.nav_chat) {
                chatBadge.setVisible(false);
                switchBaseTab("CHAT", new ChatFragment());
                return true;
            }
            return false;
        });

        // If user taps the ALREADY selected bottom tab while inside a deep screen, pop back to the base tab
        bottomNavigationView.setOnItemReselectedListener(item -> returnToCurrentBaseTab());
    }

    // =========================================================================
    // TIER 1: ROOT 5 TABS (Kept in memory using add / hide / show)
    // =========================================================================
    private void switchBaseTab(String tag, Fragment fallbackNewInstance) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) return;

        FragmentTransaction ft = fm.beginTransaction();

        if (activeBaseFragment != null) {
            ft.hide(activeBaseFragment);
        }

        Fragment target = fm.findFragmentByTag(tag);
        if (target == null) {
            target = fallbackNewInstance;
            ft.add(R.id.fragmentContainer, target, tag);
        } else {
            ft.show(target);
        }

        activeBaseFragment = target;
        ft.commit();
        setBottomNavVisibility(true);
    }

    // =========================================================================
    // TIER 2: DEEP SUB-FRAGMENTS (Destroyed on Back to save RAM)
    // =========================================================================

    /**
     * Call this from ANY Fragment to open a deeper screen (e.g., Subject List -> Chapter -> PDF Viewer).
     *
     * @param subFragment   The new Fragment to open
     * @param hideBottomNav Pass true if full-screen (like PDF or Video Player), false to keep BottomNav visible
     */
    public void openSubFragment(Fragment subFragment, boolean hideBottomNav) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) return;

        Fragment topVisibleFragment = getTopVisibleFragment();

        FragmentTransaction ft = fm.beginTransaction();
        ft.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );

        // Hide whatever screen is currently showing (whether it's a Base Tab or a previous Sub-Fragment)
        if (topVisibleFragment != null) {
            ft.hide(topVisibleFragment);
        }

        // Add the deep fragment on top and record it in the BackStack
        ft.add(R.id.fragmentContainer, subFragment, subFragment.getClass().getSimpleName());
        ft.addToBackStack(subFragment.getClass().getSimpleName());
        ft.commit();

        if (hideBottomNav) {
            setBottomNavVisibility(false);
        }
    }

    /**
     * Convenience overload: Opens a deeper Fragment and keeps the BottomNav visible by default.
     */
    public void openSubFragment(Fragment subFragment) {
        openSubFragment(subFragment, false);
    }

    /**
     * Goes back exactly ONE step (e.g., from Level 3 -> Level 2 -> Level 1 -> Base Tab).
     * Automatically destroys the popped sub-fragment to free memory.
     */
    public void popOneStepBack() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        }
    }

    /**
     * Instantly destroys ALL deep sub-fragments and drops the user back onto the active 5 Base Tab.
     */
    public void returnToCurrentBaseTab() {
        clearDeepFragmentsImmediately();
        setBottomNavVisibility(true);
    }

    /**
     * Clears any deep sub-fragments AND switches the Bottom Navigation to a specific tab
     * (e.g., navigateToBaseTab(R.id.nav_unlock) or navigateToBaseTab(R.id.nav_home)).
     */
    public void navigateToBaseTab(int menuTabId) {
        clearDeepFragmentsImmediately();
        setBottomNavVisibility(true);
        if (bottomNavigationView.getSelectedItemId() != menuTabId) {
            bottomNavigationView.setSelectedItemId(menuTabId);
        }
    }

    private void clearDeepFragmentsImmediately() {
        FragmentManager fm = getSupportFragmentManager();
        if (!fm.isStateSaved() && fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    private Fragment getTopVisibleFragment() {
        FragmentManager fm = getSupportFragmentManager();
        for (Fragment frag : fm.getFragments()) {
            if (frag != null && frag.isVisible()) {
                return frag;
            }
        }
        return activeBaseFragment;
    }

    private String getCurrentBaseTag() {
        int id = bottomNavigationView.getSelectedItemId();
        if (id == R.id.nav_courses) return "COURSES";
        if (id == R.id.nav_videos) return "VIDEOS";
        if (id == R.id.nav_unlock) return "UNLOCK";
        if (id == R.id.nav_chat) return "CHAT";
        return "HOME";
    }

    public void setBottomNavVisibility(boolean visible) {
        int vis = visible ? View.VISIBLE : View.GONE;
        bottomNavigationView.setVisibility(vis);
        if (bottomNavDivider != null) {
            bottomNavDivider.setVisibility(vis);
        }
    }

    // =========================================================================
    // UNIFIED BACK BUTTON (Fixes the "Jumping Straight to Home" Bug)
    // =========================================================================
    private void setupUnifiedBackListener() {
        // Automatically restore BottomNav visibility whenever the last sub-fragment is popped
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                setBottomNavVisibility(true);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getSupportFragmentManager();

                // PRIORITY 1: If user is inside any deep Sub-Fragment (1, 2, or 3 levels deep),
                // pop back 1-by-1!
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                    return;
                }

                // PRIORITY 2: If at the root of Courses/Videos/Unlock/Chat, go back to Home tab
                if (bottomNavigationView.getSelectedItemId() != R.id.nav_home) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                    return;
                }

                // PRIORITY 3: Already on Home tab with 0 deep fragments -> Exit App
                finish();
            }
        });
    }

    private void setupStatusBarAndInsets() {
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
            windowControllerSetLightNav(controller);
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (v, insets) -> insets);

        View rootView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            bottomNavigationView.setPadding(0, 0, 0, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void windowControllerSetLightNav(WindowInsetsControllerCompat controller) {
        controller.setAppearanceLightNavigationBars(true);
    }

    private void syncProfileBootstrapFromServer() {
        profileNetworkManager.fetchBootstrapProfile(new ProfileNetworkManager.JsonCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (isFinishing() || isDestroyed()) return;

                int prevSeenVersion = dataStorage.getLastSeenConfigVersion();
                int serverConfigVersion = response.optInt("configVersion", 1);
                boolean profileComplete = response.optBoolean("profileComplete", false);

                dataStorage.saveUserProfileFromBootstrap(response);

                if (!profileComplete || serverConfigVersion > prevSeenVersion) {
                    new ProfileSetupDialogHelper(
                            DashBoardManagerActivity.this,
                            response,
                            !profileComplete,
                            savedProfile -> {}
                    ).show();
                }
            }

            @Override
            public void onError(VolleyError error) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(DashBoardManagerActivity.this, "Unable to sync profile.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}