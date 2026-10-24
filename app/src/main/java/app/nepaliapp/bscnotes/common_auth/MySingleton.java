package app.nepaliapp.bscnotes.common_auth;

import android.content.Context;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import app.nepaliapp.bscnotes.common.Url;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class MySingleton {
    private static MySingleton instance;
    private RequestQueue requestQueue;
    private final Context ctx;
    private final StorageClass storage;

    private MySingleton(Context context) {
        this.ctx = context.getApplicationContext();
        this.storage = new StorageClass(ctx);
        this.requestQueue = getRequestQueue();
    }

    public static synchronized MySingleton getInstance(Context context) {
        if (instance == null) instance = new MySingleton(context);
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        String path = original.url().encodedPath();

                        // Note: Removed the query param "?timeZone=..." because encodedPath() strips query strings
                        boolean isPublicEndpoint = path.contains("api/auth/login") ||
                                path.contains("api/auth/signup") ||
                                path.contains("/api/auth/check") ||
                                path.contains("/auth/refresh") ||
                                path.contains("api/auth/health") ||
                                path.contains("/quizeapp") ||
                                path.contains("/api/Time/current/zone");

                        // 1. Always attach X-App-Code to the builder
                        Request.Builder requestBuilder = original.newBuilder()
                                .header("X-App-Code", Url.APP_CODE);

                        // 2. Append Authorization header to the SAME requestBuilder
                        if (!isPublicEndpoint) {
                            String token = storage.getJwtToken();
                            if (token != null && !token.isEmpty()) {
                                requestBuilder.header("Authorization", "Bearer " + token);
                            }
                        }

                        // 3. Build and proceed using requestBuilder (NOT original)
                        return chain.proceed(requestBuilder.build());
                    })
                    .authenticator(new TokenAuthenticator(ctx, storage))
                    .build();

            requestQueue = Volley.newRequestQueue(ctx, new OkHttp3Stack(okHttpClient));
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(com.android.volley.Request<T> req) {
        requestQueue.add(req);
    }
}
