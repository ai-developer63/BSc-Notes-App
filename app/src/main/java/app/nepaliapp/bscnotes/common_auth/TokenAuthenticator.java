package app.nepaliapp.bscnotes.common_auth;

import android.content.Context;

import org.json.JSONObject;

import java.io.IOException;

import app.nepaliapp.bscnotes.common.Url;
import okhttp3.Authenticator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

public class TokenAuthenticator implements Authenticator {
    Context context;
    StorageClass storage;
    // 1. Define a static lock at the class level
    private static final Object refreshLock = new Object();
    public TokenAuthenticator(Context context,StorageClass storageClass) {
        this.context = context;
        this.storage = storageClass;
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        // Stop if we've already tried to refresh once for this request
        if (responseCount(response) >= 2) return null;

        synchronized (this) {
            String currentToken = storage.getJwtToken();

            // If the token was already changed by another thread, just retry with the new one
            String authHeader = response.request().header("Authorization");
            if (authHeader != null && !authHeader.contains(currentToken)) {
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + storage.getJwtToken())
                        .build();
            }

            // Refresh call to Spring Boot
            String newJwt = refreshTokensSync();

            if (newJwt != null) {
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + newJwt)
                        .build();
            }
        }
        return null;
    }

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) result++;
        return result;
    }

    private String refreshTokensSync() {
        // Capture the token we HAVE before entering the lock
        String oldRefreshToken = storage.getRefreshToken();

        synchronized (refreshLock) {
            // 2. DOUBLE-CHECK: Did another thread already refresh it?
            String currentRefreshToken = storage.getRefreshToken();

            // If the token in storage is different than our 'old' one,
            // it means another thread already finished the refresh!
            if (oldRefreshToken != null && !oldRefreshToken.equals(currentRefreshToken)) {
                return storage.getJwtToken(); // Return the new JWT already saved by the other thread
            }

            // 3. Proceed with the Network call ONLY if we are the first thread here
            Url url = new Url();
            OkHttpClient client = new OkHttpClient();

            if (currentRefreshToken == null) return null;

            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("refreshToken", currentRefreshToken);
                // Don't forget deviceId if your backend requires it!
            } catch (Exception e) { return null; }

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url(url.getRefreshToken())
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject data = new JSONObject(response.body().string());
                    String newJwt = data.getString("accessToken");
                    String newRefresh = data.getString("refreshToken");

                    // 4. CRITICAL: Save BOTH tokens synchronously
                    storage.updateJwtToken(newJwt);
                    storage.updateRefreshToken(newRefresh);

                    return newJwt;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
