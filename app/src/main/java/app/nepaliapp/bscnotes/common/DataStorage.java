package app.nepaliapp.bscnotes.common;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

public class DataStorage {
    private static final String USER_DETAILS = "j";
    private static final String USER_CHOICE = "c";
    private static final String USER_ADS = "ad";
    private static final String USER_PROFILE_PREFS = "user_app_profile_prefs";
    private static final long TTL_LIMIT = 4 * 60 * 1000;

    Context context;
    private static final String LAST_AD_FAIL_KEY = "last_ad_fail_timestamp";

    public DataStorage(Context context) {
        this.context = context.getApplicationContext();
    }

    public void usersAdsInfo(String userZone, Boolean showPopup) {
        SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences(USER_DETAILS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_zone", userZone); // or YELLOW/RED
        editor.putBoolean("adsView", showPopup);
        editor.putLong("zone_timestamp", System.currentTimeMillis());
        editor.apply();
    }

    public String getAdsZoneStatus() {
        SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences(USER_DETAILS, Context.MODE_PRIVATE);
        String zone = sharedPreferences.getString("user_zone", null);
        long savedTime = sharedPreferences.getLong("zone_timestamp", 0);

        if (zone == null || zone.equalsIgnoreCase("refresh")) {
            return "NO_DATA";
        }
        long timeElapsed = System.currentTimeMillis() - savedTime;

        if (timeElapsed < TTL_LIMIT) {
            return "OK";
        } else {
            return "CHECK_SERVER";
        }
    }

    // Helper to get the actual zone string
    public String getUserZone() {
        SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences(USER_DETAILS, Context.MODE_PRIVATE);
        return sharedPreferences.getString("user_zone", "NONE");
    }

    public Boolean getBooleanForAdHome() {
        SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences(USER_DETAILS, Context.MODE_PRIVATE);
        long savedTime = sharedPreferences.getLong("zone_timestamp", 0);
        long timeElapsed = System.currentTimeMillis() - savedTime;
        if (timeElapsed < TTL_LIMIT) {
            return sharedPreferences.getBoolean("adsView", false);
        } else {
            return true;
        }
    }

    public void UpdateUserCountry(String country) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(USER_DETAILS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("country", country);
        editor.apply();
    }

    public String getUserCountry() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(USER_DETAILS, Context.MODE_PRIVATE);
        return sharedPreferences.getString("country", "unknown");
    }

    public void userUpdateDecision(String decision) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(USER_CHOICE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("updateChoice", decision);
        editor.apply();
    }

    public String getUserUpdateDecision() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(USER_CHOICE, Context.MODE_PRIVATE);
        return sharedPreferences.getString("updateChoice", "unselected");
    }

    public void saveLastAdFailTime(long timestamp) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(USER_DETAILS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(LAST_AD_FAIL_KEY, timestamp);
        editor.apply();
    }

    public long getLastAdFailTime() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(USER_DETAILS, Context.MODE_PRIVATE);
        return sharedPreferences.getLong(LAST_AD_FAIL_KEY, 0);
    }

    /* =========================================================================
       DYNAMIC USER APP-LEVEL PROFILE LOCAL CACHE (0ms UI & Chat Access)
       ========================================================================= */

    public void saveUserProfileFromBootstrap(JSONObject bootstrapResponse) {
        if (bootstrapResponse == null) return;
        SharedPreferences prefs = context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        boolean profileComplete = bootstrapResponse.optBoolean("profileComplete", false);
        int serverConfigVersion = bootstrapResponse.optInt("configVersion", 1);
        boolean isForeigner = bootstrapResponse.optBoolean("isForeigner", false);
        String detectedCountry = bootstrapResponse.optString("detectedCountryCode", "NP");

        editor.putBoolean("profileComplete", profileComplete);
        editor.putBoolean("isForeigner", isForeigner);
        editor.putString("countryCode", detectedCountry);

        // Only update local seen configVersion if profile is already complete
        if (profileComplete) {
            editor.putInt("lastSeenConfigVersion", serverConfigVersion);
        }

        JSONObject profileObj = bootstrapResponse.optJSONObject("profile");
        if (profileObj != null) {
            writeProfileJsonToEditor(editor, profileObj);
        }

        editor.apply();
    }

    public void saveCompletedUserProfile(JSONObject profileObj, int confirmedConfigVersion) {
        SharedPreferences prefs = context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("profileComplete", true);
        if (confirmedConfigVersion > 0) {
            editor.putInt("lastSeenConfigVersion", confirmedConfigVersion);
        }
        if (profileObj != null) {
            writeProfileJsonToEditor(editor, profileObj);
        }
        editor.apply();
    }

    private void writeProfileJsonToEditor(SharedPreferences.Editor editor, JSONObject p) {
        editor.putLong("profileId", p.optLong("id", 0L));
        editor.putLong("userId", p.optLong("userId", 0L));
        editor.putString("userName", cleanNull(p.optString("userName", "")));
        editor.putString("userEmail", cleanNull(p.optString("userEmail", "")));
        editor.putString("userPhone", cleanNull(p.optString("userPhone", "")));
        editor.putString("countryCode", cleanNull(p.optString("countryCode", "NP")));
        editor.putBoolean("isForeigner", p.optBoolean("isForeigner", false));

        editor.putLong("courseTypeId", p.isNull("courseTypeId") ? 0L : p.optLong("courseTypeId", 0L));
        editor.putString("courseCode", cleanNull(p.optString("courseCode", "")));
        editor.putString("courseName", cleanNull(p.optString("courseName", "")));
        editor.putInt("currentSession", p.isNull("currentSession") ? 0 : p.optInt("currentSession", 0));
        editor.putString("sessionLabel", cleanNull(p.optString("sessionLabel", "")));

        editor.putLong("collegeId", p.isNull("collegeId") ? 0L : p.optLong("collegeId", 0L));
        editor.putString("collegeName", cleanNull(p.optString("collegeName", "")));
        editor.putString("collegeChatBadge", cleanNull(p.optString("collegeChatBadge", "")));
        editor.putString("collegeLogoUrl", cleanNull(p.optString("collegeLogoUrl", "")));

        editor.putLong("locationId", p.isNull("locationId") ? 0L : p.optLong("locationId", 0L));
        editor.putString("district", cleanNull(p.optString("district", "")));
        editor.putString("city", cleanNull(p.optString("city", "")));
    }

    private String cleanNull(String val) {
        if (val == null || val.equalsIgnoreCase("null")) return "";
        return val.trim();
    }

    public boolean isProfileComplete() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getBoolean("profileComplete", false);
    }

    public int getLastSeenConfigVersion() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getInt("lastSeenConfigVersion", 0);
    }

    public String getProfileUserName() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getString("userName", "");
    }

    public long getCourseTypeId() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getLong("courseTypeId", 0L);
    }

    public String getCourseCode() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getString("courseCode", "");
    }

    public String getCourseName() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getString("courseName", "");
    }

    public int getCurrentSession() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getInt("currentSession", 0);
    }

    public String getSessionLabel() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getString("sessionLabel", "");
    }

    public long getCollegeId() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getLong("collegeId", 0L);
    }

    public String getCollegeName() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getString("collegeName", "");
    }

    public String getCollegeChatBadge() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getString("collegeChatBadge", "");
    }

    public String getCollegeLogoUrl() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getString("collegeLogoUrl", "");
    }

    public long getLocationId() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getLong("locationId", 0L);
    }

    public String getDistrict() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getString("district", "");
    }

    public String getCity() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getString("city", "");
    }

    public boolean isForeigner() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getBoolean("isForeigner", false);
    }

    public String getProfileCountryCode() {
        return context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .getString("countryCode", "NP");
    }

    public void clearProfileCache() {
        context.getSharedPreferences(USER_PROFILE_PREFS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}