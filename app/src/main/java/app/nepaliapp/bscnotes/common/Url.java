package app.nepaliapp.bscnotes.common;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Url {

//    API_KEY2=https://psmn.subhakharsoftware.com/
//    API_KEY1=https://pgyan.subhakharsoftware.com/
//

    // Global Unique App Key for Header: X-App-Code (BSc Notes[TU])
    public static final String APP_CODE = "82e373f855584f76";

    String officialUrl = "https://bscnotes.subhakharsoftware.com/";
    String smnBaseUrl = "https://smnads.subhakharsoftware.com/";

    // Login Part is here
    String login = "api/auth/login";
    String signup = "api/auth/signup";
    String refreshToken = "auth/refresh";
    String otpRequest = "api/auth/otpRequest";
    String checkOtp = "api/auth/verifyotp";
    String setPassword = "api/auth/setpassword";
    String cloudflareStarter = "config/gyankosh";
    String ownServerStarter = "api/auth/initial-check";
    String deviceChecker = "api/auth/check";
    String dashboard = "api/dash";
    String healthChecker = "api/auth/health";
    String deviceRegister = "api/request/sync-device";

    // Dynamic User App-Level Profile & College Engine Endpoints
    String profileBootstrap = "api/user-profile/bootstrap";
    String profileSave = "api/user-profile/save";
    String profileCourses = "api/user-profile/courses";
    String profileLocations = "api/user-profile/locations";
    String collegeSearch = "api/colleges/search";
    String collegeRequestMissing = "api/colleges/request-missing";

    public String getSetPassword() {
        return officialUrl + setPassword;
    }

    public String getOtpRequest() {
        return officialUrl + otpRequest;
    }

    public String getCheckOtp() {
        return officialUrl + checkOtp;
    }

    public String getCloudflareStarter() {
        return "https://subhakharsoftware.com/" + cloudflareStarter;
    }

    public String getOwnServerStarter() {
        return officialUrl + ownServerStarter;
    }

    public String getDeviceChecker() {
        return officialUrl + deviceChecker;
    }

    public String getHealthChecker() {
        return officialUrl + healthChecker;
    }

    public String getRefreshToken() {
        return officialUrl + refreshToken;
    }

    public String getSignup() {
        return officialUrl + signup;
    }

    public String getLogin() {
        return officialUrl + login;
    }

    public String getDeviceRegister() {
        return officialUrl + deviceRegister;
    }

    /* ===================== USER PROFILE & COLLEGE ENDPOINTS ===================== */

    public String getProfileBootstrap(String countryCode) {
        if (countryCode != null && !countryCode.trim().isEmpty()) {
            return officialUrl + profileBootstrap + "?countryCode=" + encodeParam(countryCode.trim());
        }
        return officialUrl + profileBootstrap;
    }

    public String getProfileSave() {
        return officialUrl + profileSave;
    }

    public String getProfileCourses(String educationLevel) {
        if (educationLevel != null && !educationLevel.trim().isEmpty()) {
            return officialUrl + profileCourses + "?level=" + encodeParam(educationLevel.trim());
        }
        return officialUrl + profileCourses;
    }

    public String getProfileLocations(String countryCode, String district) {
        StringBuilder sb = new StringBuilder(officialUrl).append(profileLocations);
        String safeCountry = (countryCode != null && !countryCode.trim().isEmpty()) ? countryCode.trim() : "NP";
        sb.append("?countryCode=").append(encodeParam(safeCountry));
        if (district != null && !district.trim().isEmpty()) {
            sb.append("&district=").append(encodeParam(district.trim()));
        }
        return sb.toString();
    }

    public String getCollegeSearch(String query, String district, String level, String affiliation, int page, int size) {
        StringBuilder sb = new StringBuilder(officialUrl).append(collegeSearch);
        sb.append("?page=").append(Math.max(page, 0));
        sb.append("&size=").append(size <= 0 ? 30 : size);

        if (query != null && !query.trim().isEmpty()) {
            sb.append("&q=").append(encodeParam(query.trim()));
        }
        if (district != null && !district.trim().isEmpty()) {
            sb.append("&district=").append(encodeParam(district.trim()));
        }
        if (level != null && !level.trim().isEmpty()) {
            sb.append("&level=").append(encodeParam(level.trim()));
        }
        if (affiliation != null && !affiliation.trim().isEmpty()) {
            sb.append("&affiliation=").append(encodeParam(affiliation.trim()));
        }
        return sb.toString();
    }

    public String getCollegeDetails(long collegeId) {
        return officialUrl + "api/colleges/" + collegeId;
    }

    public String getCollegeSelectPopularity(long collegeId) {
        return officialUrl + "api/colleges/" + collegeId + "/select";
    }

    public String getRequestMissingCollege() {
        return officialUrl + collegeRequestMissing;
    }

    private String encodeParam(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }
}