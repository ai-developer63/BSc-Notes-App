package app.nepaliapp.bscnotes.dashboard.helper;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import app.nepaliapp.bscnotes.common.CommonFunctions;
import app.nepaliapp.bscnotes.common.Url;
import app.nepaliapp.bscnotes.common_auth.MySingleton;

public class ProfileNetworkManager {

    public static final String TAG_COLLEGE_SEARCH = "TAG_COLLEGE_SEARCH_REQ";

    private final Context context;
    private final RequestQueue requestQueue;
    private final Url url;
    private final CommonFunctions commonFunctions;

    public interface JsonCallback {
        void onSuccess(JSONObject response);
        void onError(VolleyError error);
    }

    public ProfileNetworkManager(Context context) {
        this.context = context;
        this.requestQueue = MySingleton.getInstance(context.getApplicationContext()).getRequestQueue();
        this.url = new Url();
        this.commonFunctions = new CommonFunctions();
    }

    /**
     * 1. GET /api/user-profile/bootstrap?countryCode=NP
     */
    public void fetchBootstrapProfile(JsonCallback callback) {
        String countryCode = CommonFunctions.getResolvedCountryCode(context);
        String endpoint = url.getProfileBootstrap(countryCode);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                endpoint,
                null,
                response -> {
                    if (callback != null) callback.onSuccess(response);
                },
                error -> {
                    if (!commonFunctions.interceptSecurityAlert(context, error, null)) {
                        if (callback != null) callback.onError(error);
                    }
                }
        );
        request.setShouldCache(false);
        request.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.0f));
        requestQueue.add(request);
    }

    /**
     * 2. POST /api/user-profile/save
     */
    public void saveUserProfile(JSONObject payload, JsonCallback callback) {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url.getProfileSave(),
                payload,
                response -> {
                    if (callback != null) callback.onSuccess(response);
                },
                error -> {
                    if (!commonFunctions.interceptSecurityAlert(context, error, null)) {
                        commonFunctions.handleErrorResponse(context, error);
                        if (callback != null) callback.onError(error);
                    }
                }
        );
        request.setShouldCache(false);
        request.setRetryPolicy(new DefaultRetryPolicy(12000, 0, 1.0f));
        requestQueue.add(request);
    }

    /**
     * 3. GET /api/colleges/search (Cancels previous in-flight search automatically)
     */
    public void searchColleges(String query, String district, String level, String affiliation, int page, int size, JsonCallback callback) {
        if (page == 0) {
            requestQueue.cancelAll(TAG_COLLEGE_SEARCH);
        }

        String searchUrl = url.getCollegeSearch(query, district, level, affiliation, page, size);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                searchUrl,
                null,
                response -> {
                    if (callback != null) callback.onSuccess(response);
                },
                error -> {
                    if (!commonFunctions.interceptSecurityAlert(context, error, null)) {
                        if (callback != null) callback.onError(error);
                    }
                }
        );
        request.setTag(TAG_COLLEGE_SEARCH);
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    /**
     * 4. POST /api/colleges/request-missing (Crowdsourced Quarantine Backup)
     */
    public void requestMissingCollege(String name, String city, String district, String level, String affiliation, JsonCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            if (city != null && !city.isEmpty()) body.put("city", city);
            if (district != null && !district.isEmpty()) body.put("district", district);
            if (level != null && !level.isEmpty()) body.put("educationLevel", level);
            if (affiliation != null && !affiliation.isEmpty()) body.put("affiliationBoard", affiliation);
        } catch (JSONException ignored) {
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url.getRequestMissingCollege(),
                body,
                response -> {
                    if (callback != null) callback.onSuccess(response);
                },
                error -> {
                    if (!commonFunctions.interceptSecurityAlert(context, error, null)) {
                        commonFunctions.handleErrorResponse(context, error);
                        if (callback != null) callback.onError(error);
                    }
                }
        );
        request.setShouldCache(false);
        requestQueue.add(request);
    }
}