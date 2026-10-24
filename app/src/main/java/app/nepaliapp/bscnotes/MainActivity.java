package app.nepaliapp.bscnotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.aheaditec.talsec_security.security.api.Talsec;
import com.aheaditec.talsec_security.security.api.TalsecConfig;
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

import app.nepaliapp.bscnotes.auth.SigninManagerActivity;
import app.nepaliapp.bscnotes.common.CommonFunctions;
import app.nepaliapp.bscnotes.common.DataStorage;
import app.nepaliapp.bscnotes.common.MaintainaceActivity;
import app.nepaliapp.bscnotes.common.Url;
import app.nepaliapp.bscnotes.common_auth.MySingleton;
import app.nepaliapp.bscnotes.common_auth.StorageClass;
import app.nepaliapp.bscnotes.BuildConfig;
import app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity;

public class MainActivity extends AppCompatActivity {
    private static final int SPLASH_DELAY = 2000;
    RequestQueue requestQueue;
    Url url;
    ProgressBar progressBar;
    Button restartBtn;
    TextView sloganTxt;
    CommonFunctions commonFunctions;
    DataStorage storage;
    StorageClass storageClass;
    String deviceId;
    public static int compareVersion(String localVersion, String serverVersion) {
        if (localVersion == null || serverVersion == null) return 0;

        String[] localParts = localVersion.split("\\.");
        String[] serverParts = serverVersion.split("\\.");

        int length = Math.max(localParts.length, serverParts.length);

        for (int i = 0; i < length; i++) {
            int localPart = i < localParts.length ? Integer.parseInt(localParts[i]) : 0;
            int serverPart = i < serverParts.length ? Integer.parseInt(serverParts[i]) : 0;

            if (localPart < serverPart) return -1;
            if (localPart > serverPart) return 1;
            // else continue
        }
        return 0; // equal
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
// Convert BOTH your Upload SHA-256 and Google Play's SHA-256 to Base64
        String[] expectedHashes = new String[]{
                "mTZRjN/CNMeNgmvTtFhQ8lFI4jJCGPofvR44R6u9u/4=", // Your Play Store Hash
                "8KC9mycqFbrtLp7n+ZFDl4Wwq7+053ownfOnaMpGIi0="  // Keep your local debug hash here too
        };
        TalsecConfig config = new TalsecConfig.Builder(
                "app.nepaliapp.bscnotes",
                expectedHashes) // From Step 2
                .watcherMail("subhakharsoftware@gmail.com")
                .prod(!BuildConfig.DEBUG) // when is Development and Production auto handled
                .build();

// 2. Start Protection
        Talsec.start(this, config);


        findingById();

        fetchStartupConfig(false);
//        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(url.getCloudflareStarter(), new Response.Listener<JSONObject>() {
//            @Override
//            public void onResponse(JSONObject jsonObject) {
//                handleAppUpdate(jsonObject);
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError volleyError) {
//
//                progressBar.setVisibility(View.INVISIBLE);
//                restartBtn.setVisibility(View.VISIBLE);
//                String message = "Unknown error";
//                if (volleyError instanceof AuthFailureError) {
//                    message = "Email or Password is wrong";
//                } else if (volleyError instanceof NetworkError) {
//                    message = "Network Error";
//                } else if (volleyError instanceof ServerError) {
//                    message = "Server Error";
//                } else if (volleyError instanceof TimeoutError) {
//                    message = "Timeout Error";
//                }
//                sloganTxt.setText(message);
//                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
//            }
//        });
//        jsonObjectRequest.setShouldCache(false);
//        requestQueue.add(jsonObjectRequest);


        restartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overridePendingTransition(0, 0); // Disable close animation
                recreate(); // Restart the activity
                overridePendingTransition(0, 0); // Disable start animation
            }
        });
    }

    private void fetchStartupConfig(boolean useFallbackUrl) {
        // Determine which URL to use based on the boolean flag
        String targetUrl = useFallbackUrl ? url.getCloudflareStarter() : url.getOwnServerStarter();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(targetUrl, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                handleAppUpdate(jsonObject);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                // If it's the first attempt, and it's a Server or Timeout error, trigger the fallback
                if (!useFallbackUrl && (volleyError instanceof ServerError || volleyError instanceof TimeoutError)) {
                    Log.d("MainActivity", "Primary server failed. Switching to Cloudflare fallback...");
                    fetchStartupConfig(true); // Call this method again with the fallback URL
                    return; // Stop execution here so we don't show the error UI yet
                }

                // If we are already on the fallback, or it's a different kind of error, show the error UI
                progressBar.setVisibility(View.INVISIBLE);
                restartBtn.setVisibility(View.VISIBLE);

                String message = "Unknown error";
                if (volleyError instanceof AuthFailureError) {
                    message = "Email or Password is wrong";
                } else if (volleyError instanceof NetworkError) {
                    message = "Network Error";
                } else if (volleyError instanceof ServerError) {
                    message = "Server Error";
                } else if (volleyError instanceof TimeoutError) {
                    message = "Timeout Error";
                }

                sloganTxt.setText(message);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        jsonObjectRequest.setShouldCache(false);
        requestQueue.add(jsonObjectRequest);
    }

    private void findingById() {
        progressBar = findViewById(R.id.progressBar);
        restartBtn = findViewById(R.id.retryBtn);
        sloganTxt = findViewById(R.id.slogan);
        storage = new DataStorage(this);
        storageClass = new StorageClass(this);
        deviceId  = CommonFunctions.getUniqueId(this);
        requestQueue = MySingleton.getInstance(getApplicationContext()).getRequestQueue();
        url = new Url();
        commonFunctions = new CommonFunctions();
    }


    private void handleAppUpdate(JSONObject jsonObject) {
        String serverContext = jsonObject.optString("state");
//        Log.d("state", serverContext);

        // Helper: get proper boolean from server
        boolean updateCancelable = jsonObject.optBoolean("updatecancelable");
        try {
            updateCancelable = jsonObject.getBoolean("updatecancelable");
//            Log.d("No fall part", String.valueOf(updateCancelable));
        } catch (JSONException e) {
//            Log.d("fall part", "handleAppUpdate: ");
            // fallback if server sends string
            updateCancelable = "true".equalsIgnoreCase(jsonObject.optString("updatecancelable", "false"));
        }

        switch (serverContext.toLowerCase()) {
            case "running":
                String serverVersion = jsonObject.optString("latestversonName", "").trim();
                String localVersion = getAppVersionName();
                int result = compareVersion(localVersion, serverVersion);


                if (result < 0) {
                    // App outdated
                    if (storage.getUserUpdateDecision().equalsIgnoreCase("later") && updateCancelable) {
                        goNext(jsonObject);
                    } else {
                        showUpdateDialog(updateCancelable, isUpdateNow -> {
                            if (isUpdateNow) {
                                redirectToPlayStore();
                            } else {
                                storage.userUpdateDecision("later");
                                goNext(jsonObject);
                            }
                        });
                    }
                } else {
                    // Already updated or developer version
                    goNext(jsonObject);
                    if (result > 0) {
//                        Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case "maintainace":
                Intent intent = new Intent(MainActivity.this, MaintainaceActivity.class);
                intent.putExtra("END_TIME", jsonObject.optString("End"));
                intent.putExtra("Title_Message",jsonObject.optString("message"));
                startActivity(intent);
                break;

            default:
                Toast.makeText(MainActivity.this, "Unknown server state", Toast.LENGTH_SHORT).show();
                goNext(jsonObject);
                break;
        }
    }

    // Helper function to redirect user based on JWT
    private void goNext(JSONObject jsonObject) {
        if (storageClass.getJwtToken() == null) {
            warningEmulator();
        } else {
            checkHealth(deviceId,jsonObject);

        }
    }


    private void showUpdateDialog(boolean isCancelable, UpdateDialogCallback callback) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.update_layout, null);

        // Create dialog
        final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(isCancelable)
                .create();

        // Initialize views
        ImageView ivLogo = dialogView.findViewById(R.id.ivAppLogo);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnUpdate = dialogView.findViewById(R.id.btnUpdateNow);
        Button btnLater = dialogView.findViewById(R.id.btnLater);

        if (!isCancelable) {
            btnLater.setVisibility(View.GONE);
        }

        // Button listeners
        btnUpdate.setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) callback.onUpdateClicked(true);
        });

        btnLater.setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) callback.onUpdateClicked(false);
        });

        // Show dialog
        dialog.show();
    }


    private void redirectToPlayStore() {
        String packageName = "app.nepaliapp.gyankosh";
        try {
            // 1. Attempt to launch the native Play Store app view
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            startActivity(marketIntent);
        } catch (android.content.ActivityNotFoundException e) {
            try {
                // 2. Safe Fallback: Attempt to open the web browser link
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                startActivity(browserIntent);
            } catch (Exception fatalEx) {
                // 3. Ultimate Safety Net: Executed if both Play Store and Web Browsers are missing/disabled
                Toast.makeText(getApplicationContext(), "Unable to open Play Store or Web Browser. Please update manually.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getAppVersionName() {
        try {
            Context context = getApplicationContext();
            PackageManager pm = context.getPackageManager();
            PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);

            return pInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void warningEmulator() {
        if (isFinishing() || isDestroyed()) {
            return;
        }



        if (deviceId == null || deviceId.isEmpty()) {
            // Show the Hard Block Popup
            new AlertDialog.Builder(this)
                    .setTitle("Device Not Supported")
                    .setMessage("For security reasons, this reward app can only be used on physical Android devices. Emulators and virtual environments are not permitted.")
                    .setCancelable(false) // User cannot click outside or back button
                    .setPositiveButton("Exit", (dialog, which) -> {
                        finishAffinity(); // Closes the entire app
                    })
                    .show();
        } else {
            proceedToLogin(deviceId);
        }
    }

    private void proceedToLogin(String deviceId) {

        // 1. Create the JSON object to match your UniqueDeviceIdDTO
        JSONObject postData = new JSONObject();
        try {
            postData.put("deviceId", deviceId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 2. Build the POST request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                url.getDeviceChecker(),
                postData,
                response -> {
                    try {


                        // 1. Get values safely and TRIM them
                        boolean isBlocked = response.optBoolean("blocked", false);
                        String status = response.optString("status", "").trim().toLowerCase(); // Normalize
                        String maskedEmail = response.optString("maskedEmail", "");
                        String realEmail = response.optString("realEmail", "");


                        if (isBlocked) {

                            showHardwareBlockedDialog(this, deviceId);
                            return; // Stop here
                        }

                        // 2. Logic Check
                        if (status.equals("linked")) {


                            Intent intent = new Intent(MainActivity.this, SigninManagerActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                            intent.putExtra("PRE_FILLED_EMAIL", maskedEmail);
                            intent.putExtra("REAL_EMAIL", realEmail);
                            intent.putExtra("DEVICE_ID", deviceId);
                            startActivity(intent);
                            finish();
                        } else {
//                            Log.d("GyanKosh_Response", "No link found. Moving to Signin (New)");
                            Intent intent = new Intent(MainActivity.this, SigninManagerActivity.class);
                            startActivity(intent);
                            finish();
                        }

                    } catch (Exception e) {
                        Log.e("GyanKosh_Error", "Parsing error: " + e.getMessage());
                        e.printStackTrace();
                        sloganTxt.setText("Parsing Error");
                    }
                },
                volleyError -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    restartBtn.setVisibility(View.VISIBLE);

                    String message = "Connection Error";
                    if (volleyError.networkResponse != null && volleyError.networkResponse.statusCode == 403) {
                        message = "Device blocked or Unauthorized";
                    } else if (volleyError instanceof TimeoutError) {
                        message = "Server timeout, try again";
                    }

                    sloganTxt.setText(message);
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                }
        );

        jsonObjectRequest.setShouldCache(false);
        requestQueue.add(jsonObjectRequest);
    }

    private void checkHealth(String deviceId,JSONObject jsonObject) {
        // 1. Create the JSON object to match your UniqueDeviceIdDTO
        JSONObject postData = new JSONObject();
        try {
            postData.put("deviceId", CommonFunctions.getUniqueId(getApplicationContext()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

//        Log.d("health Object", postData.toString());
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, url.getHealthChecker(), postData, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                if (response.optBoolean("isAccountLive")) {
                    showAccountBlockedDialog(MainActivity.this, deviceId,"Account Suspended","Our system detected unethical activity violating our Fair Play policy. Access to game features has been suspended.Please Take ScreenShot and share with our Customer Support During Appeal");
                }else{
                    Intent intent = new Intent(MainActivity.this, DashBoardManagerActivity.class);
                    intent.putExtra("who", jsonObject.optString("who"));
                    startActivity(intent);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.INVISIBLE);
                restartBtn.setVisibility(View.VISIBLE);
                String message = "Unknown error";
                if (error instanceof AuthFailureError) {
                    message = "Email or Password is wrong";
                } else if (error instanceof NetworkError) {
                    message = "Network Error";
                } else if (error instanceof ServerError) {
                    message = "Server Error";
                } else if (error instanceof TimeoutError) {
                    message = "Timeout Error";
                }
                sloganTxt.setText(message);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
        requestQueue.add(objectRequest);


    }

    public void showHardwareBlockedDialog(Context context, String deviceId) {
        // Inflate the custom layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_dialog_blocked, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomDialogTheme);
        builder.setView(dialogView);
        builder.setCancelable(false); // Prevents closing by tapping outside

        final AlertDialog dialog = builder.create();

        // Make background transparent so CardView corners show properly0
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        MaterialButton btnAppeal = dialogView.findViewById(R.id.btnAppeal);
        MaterialButton btnExit = dialogView.findViewById(R.id.btnExit);
        TextView deviceIds = dialogView.findViewById(R.id.tvDeviceID);
        deviceIds.setText(deviceId);
        btnAppeal.setOnClickListener(v -> {
            // Logic to navigate to your Support Fragment
            //TODO as transfer them To support center
//            navigateToSupportFragment();
            dialog.dismiss();
        });

        btnExit.setOnClickListener(v -> {
            // Close the application
            if (context instanceof Activity) {
                ((Activity) context).finishAffinity();
            }
        });

        dialog.show();
    }

    public void showAccountBlockedDialog(Context context, String deviceId, String messages, String headings) {

        ContextThemeWrapper wrapper = new ContextThemeWrapper(context, com.google.android.material.R.style.Theme_Design_Light);
        // Inflate the custom layout
        View dialogView = LayoutInflater.from(wrapper).inflate(R.layout.layout_dialog_blocked, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomDialogTheme);
        builder.setView(dialogView);
        builder.setCancelable(false); // Prevents closing by tapping outside

        final AlertDialog dialog = builder.create();



        MaterialButton btnAppeal = dialogView.findViewById(R.id.btnAppeal);
        MaterialButton btnExit = dialogView.findViewById(R.id.btnExit);
        TextView deviceIds = dialogView.findViewById(R.id.tvDeviceID);
        TextView heading = dialogView.findViewById(R.id.heading);
        TextView message = dialogView.findViewById(R.id.message);
        heading.setText(messages);
        message.setText(headings);
        deviceIds.setText(deviceId);
        btnAppeal.setOnClickListener(v -> {
            // Logic to navigate to your Support Fragment
            //TODO as transfer them To support center
//            navigateToSupportFragment();
            dialog.dismiss();
        });

        btnExit.setOnClickListener(v -> {
            // Close the application
            if (context instanceof Activity) {
                ((Activity) context).finishAffinity();
            }
        });

        dialog.show();
    }


    public interface UpdateDialogCallback {
        void onUpdateClicked(boolean isUpdateNow);
    }
}