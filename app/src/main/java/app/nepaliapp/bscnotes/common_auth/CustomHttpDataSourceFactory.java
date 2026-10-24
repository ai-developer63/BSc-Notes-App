package app.nepaliapp.bscnotes.common_auth;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;

import java.util.HashMap;
import java.util.Map;

import app.nepaliapp.bscnotes.common.Url;

@UnstableApi
public class CustomHttpDataSourceFactory implements HttpDataSource.Factory {

    private final DefaultHttpDataSource.Factory internalFactory;

    public CustomHttpDataSourceFactory(String jwtToken) {
        this.internalFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000);

        Map<String, String> headers = new HashMap<>();
        headers.put("X-App-Code", Url.APP_CODE);
        if (jwtToken != null && !jwtToken.isEmpty()) {
            String formatted = jwtToken.startsWith("Bearer ") ? jwtToken : "Bearer " + jwtToken;
            headers.put("Authorization", formatted);
        }
        this.internalFactory.setDefaultRequestProperties(headers);
    }

    @NonNull
    @Override
    public HttpDataSource createDataSource() {
        return internalFactory.createDataSource();
    }

    @NonNull
    @Override
    public HttpDataSource.Factory setDefaultRequestProperties(@NonNull Map<String, String> defaultRequestProperties) {
        internalFactory.setDefaultRequestProperties(defaultRequestProperties);
        return this;
    }
}