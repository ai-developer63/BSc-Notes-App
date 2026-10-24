package app.nepaliapp.bscnotes.auth.fragments;

import static android.content.Context.TELEPHONY_SERVICE;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.nepaliapp.bscnotes.R;
import app.nepaliapp.bscnotes.auth.SigninManagerActivity;
import app.nepaliapp.bscnotes.common.CommonFunctions;
import app.nepaliapp.bscnotes.common.Url;
import app.nepaliapp.bscnotes.common_auth.MySecurityHandler;
import app.nepaliapp.bscnotes.common_auth.MySingleton;
import app.nepaliapp.bscnotes.common_auth.StorageClass;
import app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity;


public class LoginFragment extends Fragment {
    Button signinButton, signUpButton;
    TextView forgetpass;
    EditText emailEditText, passwordEditText;
    ToggleButton togglePasswordVisibility;
    Context context;
    Url url;
    StorageClass storageClass;
    private RequestQueue requestQueue;
    FrameLayout loadingOverlay;
    CommonFunctions commonFunctions;
    private String deviceId, unsensoredEmail;
    private boolean isDeviceBound = false;

    private static Bundle persistentLockData = null;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        this.requestQueue = MySingleton.getInstance(context).getRequestQueue();
    }
    public LoginFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        init(view);
        if (getArguments() != null && getArguments().containsKey("SENSOR_EMAIL")) {
            persistentLockData = getArguments();
        }
        checkAndApplyDeviceLock(view);

        togglePasswordVisibility.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Show password
                    passwordEditText.setTransformationMethod(null); // Show password in plain text
                } else {
                    // Hide password
                    passwordEditText.setTransformationMethod(new PasswordTransformationMethod());
                }
            }
        });

        signinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String password = passwordEditText.getText().toString();

                // 1. SKIP email validation if device is bound
                if (!isDeviceBound) {
                    String email = emailEditText.getText().toString();
                    if (TextUtils.isEmpty(email)) {
                        emailEditText.setError("Email is required");
                        emailEditText.requestFocus();
                        return;
                    }
                    if (!emailCheckerRegex(email)) {
                        emailEditText.setError("Invalid Email");
                        emailEditText.requestFocus();
                        return;
                    }
                }

                // 2. Password check remains for both cases
                if (password.length() >= 6) {
                    sendLoginRequest();
                } else {
                    passwordEditText.setError("Password must be at least 6 characters");
                    passwordEditText.requestFocus();
                }
            }
        });


        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isDeviceBound){
                    //todo make it harden Contact Support. So later it can be used
                    openMessengerSupport();
                }else{
                    fragmentChanger(new SignupFragment());
                }


            }
        });
        forgetpass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fragmentChanger(new ForgetPasswordFragment());
            }
        });

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitAlertDialog(requireContext());
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                callback
        );

        TextView legalTextView = view.findViewById(R.id.tvLegalAgreement);
        String text = "By continuing, you agree to GyanKosh's \nTerms of Service and Privacy Policy";

        SpannableString spannableString = new SpannableString(text);

// Click listener for Terms of Service
        ClickableSpan termsSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                openUrl("https://subhakharsoftware.com/gyankosh/terms-of-service");
            }
        };

// Click listener for Privacy Policy
        ClickableSpan privacySpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                openUrl("https://subhakharsoftware.com/gyankosh/privacy-policy");
            }
        };

// Set spans (update indices based on your exact text)
        spannableString.setSpan(termsSpan, 40, 56, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(privacySpan, 61, 75, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        legalTextView.setText(spannableString);
        legalTextView.setMovementMethod(LinkMovementMethod.getInstance());

        return view;
    }

    private void openUrl(String url) {
        try {
            // Define the color scheme parameters
            CustomTabColorSchemeParams colorSchemeParams = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ContextCompat.getColor(requireContext(), R.color.Colour_After_click))
                    .build();

            // Build the intent using the new scheme
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                    .setDefaultColorSchemeParams(colorSchemeParams)
                    .build();

            customTabsIntent.launchUrl(requireContext(), Uri.parse(url));
        } catch (Exception e) {
            // Fallback to standard browser if Custom Tabs fails
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
    }

    private void checkAndApplyDeviceLock(View view) {
        Bundle args = persistentLockData;
        // If internal args are empty, check the Activity's backup
        if ((args == null || !args.containsKey("SENSOR_EMAIL")) && getActivity() instanceof SigninManagerActivity) {
            args = ((SigninManagerActivity) getActivity()).savedAuthData;
        }
        if (args != null) {
            String sensorEmail = args.getString("SENSOR_EMAIL");
            unsensoredEmail = args.getString("UNSENSOR_EMAIL");
            deviceId = args.getString("DEVICE_ID");

            if (sensorEmail != null && !sensorEmail.isEmpty()) {
                isDeviceBound = true;

                // Update UI elements to "Locked" state
                TextView loginTitle = view.findViewById(R.id.Logintextid);
                loginTitle.setText("Welcome Back!");
                loginTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_verified_user, 0);
                loginTitle.setCompoundDrawablePadding(20);

                emailEditText.setBackgroundResource(R.drawable.edittext_locked_design);
                emailEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_outline, 0, 0, 0);
                emailEditText.setCompoundDrawablePadding(30);

                TextView emailLabel = view.findViewById(R.id.TextEmail);
                emailLabel.setText("Secured Account");
                int accentColor = ContextCompat.getColor(requireContext(), R.color.Colour_After_click);
                emailLabel.setTextColor(accentColor);

                emailEditText.setText(sensorEmail);
                emailEditText.setFocusable(false);
                emailEditText.setAlpha(0.7f);

                // Show info dialog when they click the locked email field
                emailEditText.setOnClickListener(v -> showDeviceBoundInfo());

                // Change Signup button to Support
                signUpButton.setText("Contact Support");

                // Focus password field automatically
                passwordEditText.requestFocus();
            }
        }
    }
    private void showDeviceBoundInfo() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Device Protection Active")
                .setMessage("To prevent unauthorized access and fraud, GyanKosh links your physical device to your primary email.\n\n" +
                        "If you need to change accounts, please contact support.")
                .setPositiveButton("I Understand", null)
                .setNeutralButton("Contact Support", (d, w) -> openMessengerSupport())
                .show();
    }


    private void copyDeviceIdToClipboard(String deviceId) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("GyanKosh ID", deviceId);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);

            // On Android 13+, the system shows its own confirmation UI.
            // For older versions, we show our own Toast.
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                Toast.makeText(requireContext(), "Device ID copied for Support", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void openMessengerSupport() {
        copyDeviceIdToClipboard("Support Id : "+deviceId);
        // Replace 'YOUR_FACEBOOK_ID_OR_USERNAME' with your actual ID or Page Name
        // Example: "gyankosh.official" or a number like "100012345678"
        String messengerUrl = "fb-messenger://user/977391522117904";

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(android.net.Uri.parse(messengerUrl));

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            // If Messenger app is not installed, open the browser version
            String webUrl = "https://m.me/977391522117904";
            Intent webIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(webUrl));
            startActivity(webIntent);
        }
    }
    private void showExitAlertDialog(Context context) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Exit App");
        builder.setMessage("Are you sure you want to exit the app?");
        builder.setPositiveButton("Yes", (dialog, which) -> requireActivity().finishAffinity());
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.show();

    }

    private void fragmentChanger(Fragment fragment) {
        if (getActivity() != null && getActivity() instanceof SigninManagerActivity) {
            ((SigninManagerActivity) getActivity()).replaceFragments(fragment);
        }

    }

    private void init(View view) {
        signUpButton = view.findViewById(R.id.signUpButton);
        forgetpass = view.findViewById(R.id.Forgetpassword);
        signinButton = view.findViewById(R.id.signinButton);
        emailEditText = view.findViewById(R.id.EmailEditText);
        passwordEditText = view.findViewById(R.id.PasswordEdittext);
        togglePasswordVisibility = view.findViewById(R.id.togglePasswordVisibility);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        //initialization
        url = new Url();
        storageClass = new StorageClass(context);
        commonFunctions = new CommonFunctions();
    }

    private boolean emailCheckerRegex(String Email) {
        if (Email.isEmpty()) {
            return false;
        } else {
            Pattern pattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
            Matcher matcher = pattern.matcher(Email);
            return matcher.matches();
        }

    }

    private void sendLoginRequest() {
        loadingOverlay.setVisibility(View.VISIBLE);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url.getLogin(), objectMaker(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String token = response.getString("token");
                            String refreshToken = response.getString("refreshToken");
                            storageClass.updateJwtToken(token);
                            storageClass.updateRefreshToken(refreshToken);
                            Intent intent = new Intent(getActivity(), DashBoardManagerActivity.class);
                            startActivity(intent);
                        } catch (JSONException e) {
                            Toast.makeText(context, "Error on Getting Server Code", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        loadingOverlay.setVisibility(View.GONE);
                        handleErrorResponse(error);
                    }
                });

        requestQueue.add(request);
    }

    private void handleErrorResponse(VolleyError error) {
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
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public JSONObject objectMaker() {
        JSONObject object = new JSONObject();

        try {
            String emailToSubmit = isDeviceBound ? unsensoredEmail : emailEditText.getText().toString().trim();
            TelephonyManager tm = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
            String simCountry = tm.getSimCountryIso();
            String simNetwork= tm.getNetworkCountryIso();

            String deviceID = CommonFunctions.getUniqueId(requireContext());
            JSONObject sec = MySecurityHandler.getSecurityJson();
            object.put("emailOrPhone", emailToSubmit);
            object.put("password", passwordEditText.getText().toString().trim());
            object.put("deviceId", deviceID);
            // Stealth Security Payload (Mapping all keys)
            // Even if a hacker sees "r" or "u", they won't know it security telemetry
            object.put("r", sec.optBoolean("r", false)); // Rooted
            object.put("d", sec.optBoolean("d", false)); // Debugger
            object.put("e", sec.optBoolean("e", false)); // Emulator
            object.put("t", sec.optBoolean("t", false)); // Tampered
            object.put("h", sec.optBoolean("h", false)); // Hooked (Frida/Xposed)
            object.put("m", sec.optBoolean("m", false)); // Malware
            object.put("u", sec.optBoolean("u", false)); // Untrusted Source

            // Device Context (For Fingerprinting)
            object.put("mo", sec.optString("model", "Unknown"));
            object.put("br", sec.optString("brand", "Unknown"));
            object.put("sm",simCountry);
            object.put("smt",simNetwork);

        } catch (JSONException e) {
            Toast.makeText(context, "Data Creation went Wrong", Toast.LENGTH_SHORT).show();
            Log.e("TAG", "JSON error", e);
        }

        return object;
    }



}