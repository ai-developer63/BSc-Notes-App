package app.nepaliapp.bscnotes.common;

import static android.content.Context.TELEPHONY_SERVICE;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaDrm;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import app.nepaliapp.bscnotes.MainActivity;
import app.nepaliapp.bscnotes.R;
import app.nepaliapp.bscnotes.common_auth.CommonSecurityClass;
import app.nepaliapp.bscnotes.common_auth.MySingleton;
import app.nepaliapp.bscnotes.common_auth.StorageClass;

public class CommonFunctions {

    public static void triggerHardwareCheck(Context context, boolean force) {
        SharedPreferences prefs = context.getSharedPreferences("SecurityLogs", Context.MODE_PRIVATE);
        long lastUpdate = prefs.getLong("last_hw_update", 0);
        long now = System.currentTimeMillis();

        // Only fire if forced (e.g., Wallet) or if 30 mins has passed
        if (force || (now - lastUpdate) > (1800 * 1000)) {
            CommonSecurityClass security = new CommonSecurityClass(context);
            JSONObject data = security.getSecurityWithVPNDataJSON();

            // Use your existing Volley function
            new CommonFunctions().HardwareStatusUpdater(context, data);

            prefs.edit().putLong("last_hw_update", now).apply();
        }
    }

    public void HardwareStatusUpdater(Context context, JSONObject securityTime) {
        RequestQueue requestQueue = MySingleton.getInstance(context).getRequestQueue();
        Url url = new Url();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url.getDeviceRegister(), securityTime, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        requestQueue.add(request);
    }

    /**
     * Resolves 2-letter ISO Country Code ("NP", "IN", "US", etc.) safely for Profile Bootstrap.
     */
    public static String getResolvedCountryCode(Context context) {
        try {
            DataStorage dataStorage = new DataStorage(context);
            String savedCountry = dataStorage.getUserCountry();
            if (savedCountry != null && !savedCountry.isEmpty() && !savedCountry.equalsIgnoreCase("unknown")) {
                String clean = savedCountry.trim().toUpperCase(Locale.ROOT);
                if (clean.equals("NEPAL")) return "NP";
                if (clean.equals("INDIA")) return "IN";
                if (clean.length() == 2) return clean;
            }

            TelephonyManager tm = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
            if (tm != null) {
                String simIso = tm.getSimCountryIso();
                if (simIso != null && simIso.trim().length() == 2) {
                    return simIso.trim().toUpperCase(Locale.ROOT);
                }
                String netIso = tm.getNetworkCountryIso();
                if (netIso != null && netIso.trim().length() == 2) {
                    return netIso.trim().toUpperCase(Locale.ROOT);
                }
            }

            String localeCountry = Locale.getDefault().getCountry();
            if (localeCountry != null && localeCountry.trim().length() == 2) {
                return localeCountry.trim().toUpperCase(Locale.ROOT);
            }
        } catch (Exception ignored) {
        }
        return "NP";
    }

    public static String getUniqueId(Context context) {
        // 1. Try Widevine DRM (Highest Security)
        String id = getMediaDrmId();

        // 2. Fallback to Android ID (SSAID) if DRM is null
        if (id == null || id.isEmpty() || id.equalsIgnoreCase("null")) {
            id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        // 3. Fallback to your Storage-based Generated ID ONLY if Hardware ID failed
        if (id == null || id.isEmpty() || id.equalsIgnoreCase("null")) {
            // This is where you call your UUID generator with the "GEN_" prefix
            id = getDeviceId(context);
        }

        return id;
    }

    public static String getMediaDrmId() {
        // Widevine UUID constant
        final UUID WIDEVINE_UUID = new UUID(-0x121074568853675dL, -0x5c1d283bb351ec13L);
        MediaDrm mediaDrm = null;

        try {
            // 1. Check if Widevine is supported on this specific hardware
            if (!MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID)) {
                Log.w("DeviceInfo", "Widevine DRM not supported on this device.");
                return null;
            }

            mediaDrm = new MediaDrm(WIDEVINE_UUID);
            byte[] deviceUniqueId = mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID);

            if (deviceUniqueId == null || deviceUniqueId.length == 0) {
                return null;
            }

            // 2. Convert to Hex String (efficiently)
            StringBuilder sb = new StringBuilder();
            for (byte b : deviceUniqueId) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            // This catches UnsupportedSchemeException or IllegalStateException
            Log.e("DeviceInfo", "Failed to retrieve MediaDrm ID: " + e.getMessage());
            return null;
        } finally {
            // 3. Proper Resource Management
            if (mediaDrm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    mediaDrm.close();
                } else {
                    mediaDrm.release();
                }
            }
        }
    }

    public static String getDeviceId(Context context) {
        StorageClass storageClass = new StorageClass(context);
        String deviceId = storageClass.getDeviceUniqueID();

        if (Objects.equals(deviceId, "token_kali_xa") || deviceId == null) {
            deviceId = "GEN_" + UUID.randomUUID().toString();
            storageClass.updateDeviceUniqueID(deviceId);
        }

        return deviceId;
    }

    public static void smartRestart(Context context) {
        if (context == null) return;

        // Case 1: Context is an Activity
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.finish();
            context.startActivity(activity.getIntent());
            return;
        }

        // Case 2: Context is a ContextWrapper (like Fragment or custom context)
        Context baseContext = context;
        while (baseContext instanceof ContextWrapper) {
            if (baseContext instanceof Activity) {
                Activity activity = (Activity) baseContext;
                activity.finish();
                context.startActivity(activity.getIntent());
                return;
            }
            baseContext = ((ContextWrapper) baseContext).getBaseContext();
        }

        // Case 3: Fallback - no Activity found
        Log.e("SmartRestart", "No Activity context found for restart");
    }

    public void handleErrorResponse(Context context, VolleyError error) {
        StorageClass storageClass = new StorageClass(context);
        String message = "Unknown error";
        boolean hasCustomBackendMessage = false;

        // 1. Try to read the custom JSON message from Spring Boot first!
        if (error != null && error.networkResponse != null && error.networkResponse.data != null) {
            try {
                String jsonString = new String(error.networkResponse.data, "UTF-8");
                JSONObject errorJson = new JSONObject(jsonString);

                // Check if our Spring Boot GlobalExceptionHandler sent a "message" key
                if (errorJson.has("message")) {
                    message = errorJson.getString("message");
                    hasCustomBackendMessage = true;
                }
            } catch (Exception e) {
                // Not JSON (e.g., an HTML crash page). We will ignore it and use the fallbacks below.
            }
        }

        // 2. Your Standard Error Handling
        if (error instanceof com.android.volley.AuthFailureError) {
            message = "Session Expired. Please login again.";
            storageClass.clearAll();
            Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);

            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                ((android.app.Activity) context).finish();
            }
        } else if (error instanceof com.android.volley.NetworkError) {
            if (!hasCustomBackendMessage) message = "Network Error";
            alertDialogForNetworkError(context);
        } else if (error instanceof com.android.volley.ServerError) {
            if (!hasCustomBackendMessage) message = "Server Error";
        } else if (error instanceof com.android.volley.TimeoutError) {
            if (!hasCustomBackendMessage) message = "Timeout Error";
        }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private void alertDialogForNetworkError(Context context) {
        if (!(context instanceof Activity) || ((Activity) context).isFinishing()) {
            Log.e("CommonFunctions", "Cannot show dialog, context is not a running Activity.");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Network Error");
        builder.setMessage("Please check your internet connection and try again.");
        builder.setCancelable(false);
        builder.setPositiveButton("Retry", (dialog, which) -> {
            smartRestart(context);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Parses Volley errors for Anti-Cheat Security flags.
     * Returns TRUE if a security error was caught and handled.
     * Returns FALSE if it's a normal error (so the caller can handle it).
     */
    public boolean interceptSecurityAlert(Context context, VolleyError error, Runnable onAcknowledge) {
        if (context == null || error == null || error.networkResponse == null || error.networkResponse.data == null) {
            return false;
        }

        try {
            String jsonStr = new String(error.networkResponse.data, StandardCharsets.UTF_8);
            JSONObject errorObj = new JSONObject(jsonStr);
            String backendMessage = errorObj.optString("message", "");

            if (backendMessage.startsWith("SECURITY_FLAG:") || backendMessage.startsWith("SECURITY_BAN:")) {
                boolean isBan = backendMessage.startsWith("SECURITY_BAN:");
                String cleanMessage = backendMessage.replace("SECURITY_FLAG: ", "")
                        .replace("SECURITY_BAN: ", "");

                showCustomSecurityDialog(context, isBan, cleanMessage, onAcknowledge);
                return true; // We handled it!
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; // Not a security error
    }

    private void showCustomSecurityDialog(Context context, boolean isBan, String message, Runnable onAcknowledge) {
        if (!(context instanceof Activity) || ((Activity) context).isFinishing()) {
            return;
        }

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_security_alert, null);

        AlertDialog customDialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false) // Force acknowledgment
                .create();

        if (customDialog.getWindow() != null) {
            customDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvSecurityTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvSecurityMessage);
        MaterialButton btnAck = dialogView.findViewById(R.id.btnSecurityAcknowledge);

        tvTitle.setText(isBan ? "🚨 Account Banned" : "⚠️ Security Alert");
        tvMessage.setText(message);

        btnAck.setOnClickListener(v -> {
            customDialog.dismiss();
            if (onAcknowledge != null) {
                onAcknowledge.run();
            }
        });

        customDialog.show();
    }
}