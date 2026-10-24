package app.nepaliapp.bscnotes.common_auth;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Header;
import com.android.volley.toolbox.BaseHttpStack;
import com.android.volley.toolbox.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttp3Stack extends BaseHttpStack {
    private final OkHttpClient client;

    public OkHttp3Stack(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public HttpResponse executeRequest(Request<?> request, Map<String, String> additionalHeaders)
            throws IOException, AuthFailureError {

        // Match Volley's timeout to OkHttp
        OkHttpClient.Builder builder = client.newBuilder();
        int timeoutMs = request.getTimeoutMs();
        builder.connectTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        builder.readTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        builder.writeTimeout(timeoutMs, TimeUnit.MILLISECONDS);

        okhttp3.Request.Builder okHttpRequestBuilder = new okhttp3.Request.Builder();
        okHttpRequestBuilder.url(request.getUrl());

        // Add Headers
        Map<String, String> headers = request.getHeaders();
        for (String name : headers.keySet()) {
            okHttpRequestBuilder.addHeader(name, headers.get(name));
        }
        for (String name : additionalHeaders.keySet()) {
            okHttpRequestBuilder.addHeader(name, additionalHeaders.get(name));
        }

        // Set Connection Method
        setConnectionParametersForRequest(okHttpRequestBuilder, request);

        OkHttpClient tempClient = builder.build();
        Call okHttpCall = tempClient.newCall(okHttpRequestBuilder.build());
        Response okHttpResponse = okHttpCall.execute();

        return convertResponse(okHttpResponse);
    }

    private HttpResponse convertResponse(Response okHttpResponse) throws IOException {
        int code = okHttpResponse.code();
        Headers okHeaders = okHttpResponse.headers();
        List<Header> volleyHeaders = new ArrayList<>();
        for (int i = 0, len = okHeaders.size(); i < len; i++) {
            volleyHeaders.add(new Header(okHeaders.name(i), okHeaders.value(i)));
        }

        ResponseBody body = okHttpResponse.body();
        InputStream content = body != null ? body.byteStream() : null;
        int contentLength = body != null ? (int) body.contentLength() : 0;

        return new HttpResponse(code, volleyHeaders, contentLength, content);
    }

    private static void setConnectionParametersForRequest(okhttp3.Request.Builder builder, Request<?> request)
            throws AuthFailureError {
        switch (request.getMethod()) {
            case Request.Method.GET: builder.get(); break;
            case Request.Method.POST: builder.post(createRequestBody(request)); break;
            case Request.Method.PUT: builder.put(createRequestBody(request)); break;
            case Request.Method.DELETE: builder.delete(); break;
            case Request.Method.PATCH: builder.patch(createRequestBody(request)); break;
        }
    }

    private static RequestBody createRequestBody(Request<?> r) throws AuthFailureError {
        byte[] body = r.getBody();
        if (body == null) return null;
        return RequestBody.create(body, MediaType.parse(r.getBodyContentType()));
    }
}