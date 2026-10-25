# BSc Notes — Complete Dashboard & Fragment Navigation Manual

**Target File:** `app/src/main/java/app/nepaliapp/bscnotes/dashboard/DashBoardManagerActivity.java`  
**Architecture:** Two-Tier Single-Activity Pattern (5 Cached Root Tabs + 20+ BackStack Sub-Fragments)

---

## 1. Architecture Diagram

```text
DashBoardManagerActivity (Main Shell)
 │
 ├── TIER 1: 5 Root Bottom-Nav Tabs (Kept alive in RAM via add / hide / show)
 │    ├── "HOME"     -> HomeFragment      (R.id.nav_home)
 │    ├── "COURSES"  -> CourseFragment    (R.id.nav_courses)
 │    ├── "VIDEOS"   -> VideosFragment    (R.id.nav_videos)
 │    ├── "UNLOCK"   -> UnlockFragment    (R.id.nav_unlock)
 │    └── "CHAT"     -> ChatFragment      (R.id.nav_chat)
 │
 └── TIER 2: Deep Sub-Fragments (Stacked on BackStack; destroyed on Back to free RAM)
      ├── Level 1 (e.g., PracticalFragment, OldIsGoldFragment, SubjectListFragment)
      ├── Level 2 (e.g., UnitListFragment, PastPaperYearFragment)
      └── Level 3 (e.g., PdfViewerFragment, VideoPlayerFragment - Fullscreen)
```

---

## 2. Full Copy-Paste Code: `DashBoardManagerActivity.java`

Copy and paste this entire class into `DashBoardManagerActivity.java`:

```java
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
            activeBaseFragment = getSupportFragmentManager().findFragmentByTag(getCurrentBaseTag());
        }

        syncProfileBootstrapFromServer();
    }

    private void setupBottomNavigation() {
        BadgeDrawable chatBadge = bottomNavigationView.getOrCreateBadge(R.id.nav_notice);
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
            } else if (id == R.id.nav_notice) {
                chatBadge.setVisible(false);
                switchBaseTab("CHAT", new NoticeFragment());
                return true;
            }
            return false;
        });

        // Tapping the ALREADY selected bottom tab while inside a deep screen pops back to the base tab
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

        if (topVisibleFragment != null) {
            ft.hide(topVisibleFragment);
        }

        ft.add(R.id.fragmentContainer, subFragment, subFragment.getClass().getSimpleName());
        ft.addToBackStack(subFragment.getClass().getSimpleName());
        ft.commit();

        if (hideBottomNav) {
            setBottomNavVisibility(false);
        }
    }

    public void openSubFragment(Fragment subFragment) {
        openSubFragment(subFragment, false);
    }

    public void popOneStepBack() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        }
    }

    public void returnToCurrentBaseTab() {
        clearDeepFragmentsImmediately();
        setBottomNavVisibility(true);
    }

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
        if (id == R.id.nav_notice) return "CHAT";
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
    // UNIFIED BACK BUTTON (1-by-1 BackStack -> Home Tab -> Exit App)
    // =========================================================================
    private void setupUnifiedBackListener() {
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                setBottomNavVisibility(true);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getSupportFragmentManager();

                // PRIORITY 1: If inside any deep Sub-Fragment, pop back 1-by-1
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                    return;
                }

                // PRIORITY 2: If on Courses/Videos/Unlock/Chat root tab, return to Home tab
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
            controller.setAppearanceLightNavigationBars(true);
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
                            savedProfile -> {
                            }
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
```

---

## 3. Function-by-Function Deep Dive (Why It Works & How to Use Each)

### Function 1: `switchBaseTab(String tag, Fragment fallbackNewInstance)`
* **What it does:** Manages the 5 main Bottom Navigation tabs (`"HOME"`, `"COURSES"`, `"VIDEOS"`, `"UNLOCK"`, `"CHAT"`).
* **Why it is better than `.replace()`:** `.replace()` destroys the old Fragment's view, wiping out scroll position, Glide images, and Carousels. `switchBaseTab` hides the current root tab (`ft.hide(activeBaseFragment)`), checks if the target tab already exists in `FragmentManager` by its tag (`fm.findFragmentByTag(tag)`), creates it only on the very first click (`ft.add`), and simply calls `ft.show(target)` on all future clicks.
* **Memory Impact:** Only the 5 core tabs stay in memory.

---

### Function 2: `openSubFragment(Fragment subFragment, boolean hideBottomNav)`
* **What it does:** Opens any of your 15–20 deeper screens (e.g., `PracticalFragment`, `OldIsGoldFragment`, `SubjectDetailFragment`, `PdfViewerFragment`) on top of the current screen.
* **Why it works without breaking the 5 Base Tabs:** Instead of calling `.replace()` (which would delete your 5 cached root tabs), it finds whichever fragment is currently visible on screen (`getTopVisibleFragment()`), hides it, adds the new `subFragment` on top, and records the transaction in the BackStack (`ft.addToBackStack(...)`).
* **Copy-Paste Usage (Inside any Fragment):**

```java
// 1. Open a normal sub-fragment (Bottom Navigation stays VISIBLE)
if (getActivity() instanceof DashBoardManagerActivity) {
    ((DashBoardManagerActivity) getActivity()).openSubFragment(new PracticalFragment());
}

// 2. Open a full-screen sub-fragment like PDF or Video Player (Bottom Navigation is HIDDEN)
if (getActivity() instanceof DashBoardManagerActivity) {
    ((DashBoardManagerActivity) getActivity()).openSubFragment(new PdfViewerFragment(), true);
}
```

---

### Function 3: `popOneStepBack()`
* **What it does:** Pops the top entry off the `FragmentManager` BackStack (`fm.popBackStack()`).
* **What happens to memory:** Android reverses the transaction from `openSubFragment()`—it **completely destroys** the top sub-fragment (freeing RAM immediately) and automatically re-shows the screen underneath it (Level 3 $\rightarrow$ Level 2 $\rightarrow$ Level 1 $\rightarrow$ Base Tab).
* **Copy-Paste Usage (For a Top-Left `<-` Back Button inside a Sub-Fragment):**

```java
btnBack.setOnClickListener(v -> {
    if (getActivity() instanceof DashBoardManagerActivity) {
        ((DashBoardManagerActivity) getActivity()).popOneStepBack();
    }
});
```

---

### Function 4: `returnToCurrentBaseTab()`
* **What it does:** Immediately pops **all** deep sub-fragments off the BackStack (`POP_BACK_STACK_INCLUSIVE`) and makes sure the Bottom Navigation bar is visible.
* **When to use it:** Suppose a student went 3 levels deep inside Courses (`CourseFragment` $\rightarrow$ `PhysicsFragment` $\rightarrow$ `MechanicsFragment` $\rightarrow$ `PdfFragment`) and clicks a "Back to Courses" button, or taps the Courses icon on the Bottom Navigation bar again.
* **Copy-Paste Usage (Inside any Deep Sub-Fragment):**

```java
btnCloseAllToRoot.setOnClickListener(v -> {
    if (getActivity() instanceof DashBoardManagerActivity) {
        ((DashBoardManagerActivity) getActivity()).returnToCurrentBaseTab();
    }
});
```

---

### Function 5: `navigateToBaseTab(int menuTabId)`
* **What it does:** Destroys any open deep sub-fragments (`clearDeepFragmentsImmediately()`) and programmatically switches the `BottomNavigationView` to one of the 5 main tabs (`R.id.nav_home`, `R.id.nav_courses`, `R.id.nav_videos`, `R.id.nav_unlock`, `R.id.nav_chat`).
* **Copy-Paste Usage (Switching Root Tabs from `HomeFragment` or any Deep Screen):**

```java
// Jump straight to the Unlock tab:
if (getActivity() instanceof DashBoardManagerActivity) {
    ((DashBoardManagerActivity) getActivity()).navigateToBaseTab(R.id.nav_unlock);
}

// Jump straight to the Courses tab:
if (getActivity() instanceof DashBoardManagerActivity) {
    ((DashBoardManagerActivity) getActivity()).navigateToBaseTab(R.id.nav_courses);
}

// Jump straight to the Videos tab:
if (getActivity() instanceof DashBoardManagerActivity) {
    ((DashBoardManagerActivity) getActivity()).navigateToBaseTab(R.id.nav_videos);
}

// Jump straight to the Home tab:
if (getActivity() instanceof DashBoardManagerActivity) {
    ((DashBoardManagerActivity) getActivity()).navigateToBaseTab(R.id.nav_home);
}
```

---

### Function 6: `setupUnifiedBackListener()`
* **What it does:** Controls the physical/gesture Android Back button and watches the BackStack count.
* **Why it fixes the "Jumping Straight to Home" bug:**
    1. **Priority 1 (`fm.getBackStackEntryCount() > 0`):** It checks if any deep sub-fragments are open first. If yes, it calls `fm.popBackStack()` so the user steps back **1-by-1** (`Level 3 -> Level 2 -> Level 1 -> Base Tab`).
    2. **Auto-Restore BottomNav:** An `OnBackStackChangedListener` watches the stack count; the moment the stack reaches `0`, it calls `setBottomNavVisibility(true)` so if a full-screen PDF hid the bottom bar, it reappears automatically when returning to the root tab.
    3. **Priority 2 (`selectedItemId != R.id.nav_home`):** Only when `BackStackEntryCount == 0` (the user is sitting on a root tab like *Courses* or *Chat*) does pressing Back return them to the *Home* tab.
    4. **Priority 3 (`finish()`):** If the user is already on *Home* with `0` deep fragments, the app closes cleanly.

---

### Function 7: `setupStatusBarAndInsets()`
* **What it does:** Configures Edge-to-Edge white system bars with dark icons and prevents the Material `BottomNavigationView` from floating above the bottom of the screen.
* **How it works:** Material's `BottomNavigationView` has a built-in listener that tries to pad itself internally. By calling `ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (v, insets) -> insets)` and returning `WindowInsetsCompat.CONSUMED` on the root layout, we disable Material's internal auto-padding and apply the exact `bars.top` to the root layout and `bars.bottom` to the bottom bar manually.

---

### Function 8: `syncProfileBootstrapFromServer()`
* **What it does:** Calls `GET /api/user-profile/bootstrap` once when the dashboard launches.
* **How it works:** Saves the latest user profile to `DataStorage` for instant 0ms reads across all fragments. If `profileComplete == false` or the backend `configVersion` was incremented by the admin, it automatically triggers `ProfileSetupDialogHelper` over the dashboard.

---

## 4. Template for Creating Any New Sub-Fragment (Out of Your 20 Fragments)

Whenever you create a new deep Fragment (e.g., `PracticalFragment.java`), use this copy-paste template so arguments, back buttons, and touch protection work out of the box:

```java
package app.nepaliapp.bscnotes.dashboard.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import app.nepaliapp.bscnotes.R;
import app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity;

public class PracticalFragment extends Fragment {

    private static final String ARG_ID = "arg_id";
    private static final String ARG_TITLE = "arg_title";

    private long itemId;
    private String itemTitle;

    public PracticalFragment() {
        // Required empty public constructor
    }

    // Use this static helper to pass data into the fragment safely
    public static PracticalFragment newInstance(long id, String title) {
        PracticalFragment fragment = new PracticalFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, id);
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            itemId = getArguments().getLong(ARG_ID, 0L);
            itemTitle = getArguments().getString(ARG_TITLE, "");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_practical, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Example: Wire top-left back button to go 1 step back
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() instanceof DashBoardManagerActivity) {
                    ((DashBoardManagerActivity) getActivity()).popOneStepBack();
                }
            });
        }

        // Example: Open an even deeper Level 2 Fragment from here
        // if (getActivity() instanceof DashBoardManagerActivity) {
        //     ((DashBoardManagerActivity) getActivity()).openSubFragment(new PdfViewerFragment(), true);
        // }
    }
}
```
# BSc Notes — Fragment-Side Navigation Helper Methods

This reference guide provides clean, private helper methods that you can paste directly into any of your Fragments (e.g., `HomeFragment`, `CourseFragment`, or any deep sub-fragment).

These helpers wrap around `DashBoardManagerActivity` so you don't have to repeatedly type casting checks (`if (getActivity() instanceof DashBoardManagerActivity)`) everywhere in your UI code.

---

## 1. Copy-Paste These Helper Methods Inside Your Fragments

Paste these private methods at the bottom of any Fragment class (e.g., inside `HomeFragment.java` or `CourseFragment.java`):

```java
    // =========================================================================
// NAVIGATION HELPER METHODS FOR THIS FRAGMENT
// =========================================================================

/**
 * Opens a deeper sub-fragment (Level 1, 2, or 3) while keeping the Bottom Nav visible.
 */
private void navigateToSubScreen(Fragment subFragment) {
    if (requireActivity() instanceof app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity) {
        ((app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity) requireActivity()).openSubFragment(subFragment, false);
    }
}

/**
 * Opens a full-screen sub-fragment (like a PDF Viewer or Video Player) and hides the Bottom Nav.
 */
private void navigateToFullScreenSubScreen(Fragment subFragment) {
    if (requireActivity() instanceof app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity) {
        ((app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity) requireActivity()).openSubFragment(subFragment, true);
    }
}

/**
 * Goes back exactly 1 step in the BackStack (destroys the current sub-fragment).
 */
private void goBackOneStep() {
    if (requireActivity() instanceof app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity) {
        ((app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity) requireActivity()).popOneStepBack();
    }
}

/**
 * Instantly clears all deep sub-fragments and jumps to one of the 5 main root tabs.
 * @param menuTabId e.g., R.id.nav_home, R.id.nav_courses, R.id.nav_videos, R.id.nav_unlock, R.id.nav_chat
 */
private void jumpToRootTab(int menuTabId) {
    if (requireActivity() instanceof app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity) {
        ((app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity) requireActivity()).navigateToBaseTab(menuTabId);
    }
}
### Root XML Rule for All Sub-Fragments
In the root layout of every sub-fragment XML (e.g., `fragment_practical.xml`), always include these three lines so touches never leak through to hidden fragments underneath:

```xml
android:background="#F5F7FB"
android:clickable="true"
android:focusable="true"
```