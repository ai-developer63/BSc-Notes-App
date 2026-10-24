package app.nepaliapp.bscnotes.auth.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.nepaliapp.bscnotes.R;
import app.nepaliapp.bscnotes.auth.SigninManagerActivity;
import app.nepaliapp.bscnotes.common.ResetPasswordStage;
import app.nepaliapp.bscnotes.common.Url;
import app.nepaliapp.bscnotes.common_auth.MySingleton;


public class ForgetPasswordFragment extends Fragment {
    Button actionButton;
    Context context;

    EditText emailEditText, passwordEditText, otpEditTxt;
    LoginFragment loginFragment = new LoginFragment();
    TextView emailHeaderTxt, passwordHeaderTxt;
    ImageView imageView;
    ToggleButton togglePasswordVisibility;
    Url url;
    FrameLayout loadingOverlay;
    private RequestQueue requestQueue;
    private ResetPasswordStage currentStage;

    public ForgetPasswordFragment() {
        // Required empty public constructor
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forget_password, container, false);

        init(view);
        currentStage = ResetPasswordStage.EMAIL_INPUT;
        loadingOverlay.setVisibility(View.GONE);
        imageView.setOnClickListener(v -> {
            otpEditTxt.clearFocus();
            if (otpEditTxt.getText().toString().trim().isEmpty()) {
                fragmentChanger(loginFragment);
            } else {
                showWarningAlertDialog(requireContext());
            }
        });


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

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                otpEditTxt.clearFocus();
                if (otpEditTxt.getText().toString().trim().isEmpty()) {
                    fragmentChanger(loginFragment);
                } else {
                    showWarningAlertDialog(requireContext());
                }
            }
        };
        updateUIForCurrentStage();
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentStage.equals(ResetPasswordStage.EMAIL_INPUT)) {
                    if (TextUtils.isEmpty(emailEditText.getText().toString())) {
                        emailEditText.setError("Email is Required");
                        emailEditText.requestFocus();
                    } else if (!emailCheckerRegex(emailEditText.getText().toString())) {
                        emailEditText.setError("Enter valid Email");
                        emailEditText.requestFocus();
                    } else {
                        actionButton.setVisibility(View.GONE);
                        loadingOverlay.setVisibility(View.VISIBLE);
                        sendEmail();

                    }
                } else if (currentStage.equals(ResetPasswordStage.OTP_VALIDATION)) {
                    loadingOverlay.setVisibility(View.VISIBLE);
                    emailEditText.setEnabled(false);
                    actionButton.setVisibility(View.GONE);
                    verifyOtp();


                } else if (currentStage.equals(ResetPasswordStage.PASSWORD_UPDATE)) {
                    emailEditText.setEnabled(false);
                    if (TextUtils.isEmpty(passwordEditText.getText().toString())) {
                        passwordEditText.setError("Password is Required");
                    } else if (!(passwordEditText.getText().length() >= 6)) {
                        passwordEditText.setError("Minimum 6 digit Password is Required");
                    } else {
                        actionButton.setVisibility(View.GONE);
                        setPassword();

                        updateUIForCurrentStage();
                    }
                }

            }
        });


        return view;
    }

    private void init(View view) {
        //Initialization
        url = new Url();
        requestQueue = MySingleton.getInstance(requireContext()).getRequestQueue();

        //findViewByID
        actionButton = view.findViewById(R.id.signUpButton);
        emailEditText = view.findViewById(R.id.EmailEdittext);
        passwordEditText = view.findViewById(R.id.PasswordEdittextEdittext);
        otpEditTxt = view.findViewById(R.id.otpEditText);
        imageView = view.findViewById(R.id.backBtn);
        emailHeaderTxt = view.findViewById(R.id.TextEmail);
        passwordHeaderTxt = view.findViewById(R.id.TextPassword);
        togglePasswordVisibility = view.findViewById(R.id.togglePasswordVisibility);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);


    }

    private void fragmentChanger(Fragment fragment) {
        if (getActivity() != null && getActivity() instanceof SigninManagerActivity) {
            ((SigninManagerActivity) getActivity()).replaceFragments(fragment);
        }

    }

    private void adjustContainerHeight() {
        if (getActivity() != null && getActivity() instanceof SigninManagerActivity) {
            ((SigninManagerActivity) getActivity()).adjustFragmentHeight();
        }
    }

    private void updateUIForCurrentStage() {
        // Hide all views initially
        otpEditTxt.setVisibility(View.GONE);
        passwordHeaderTxt.setVisibility(View.GONE);
        passwordEditText.setVisibility(View.GONE);
        actionButton.setVisibility(View.GONE);

        switch (currentStage) {
            case EMAIL_INPUT:
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setText("Send OTP");
                break;
            case OTP_VALIDATION:
                otpEditTxt.setVisibility(View.VISIBLE);
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setText("Validate OTP");
                break;
            case PASSWORD_UPDATE:
                passwordEditText.setVisibility(View.VISIBLE);
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setText("Update Password");
                break;
        }
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

    private void showWarningAlertDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Going to Sign in page");
        builder.setMessage("Are you sure you want to go back ?");
        builder.setPositiveButton("Yes", (dialog, which) -> fragmentChanger(new LoginFragment()));
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.show();

    }

    private JSONObject objectMaker() {
        JSONObject obj = new JSONObject();
        try {
            if (currentStage.equals(ResetPasswordStage.OTP_VALIDATION)) {
                obj.put("email", emailEditText.getText().toString());
                obj.put("otp", otpEditTxt.getText().toString());
            } else if (currentStage.equals(ResetPasswordStage.PASSWORD_UPDATE)) {
                obj.put("email", emailEditText.getText().toString());
                obj.put("otp", otpEditTxt.getText().toString());
                obj.put("newpassword", passwordEditText.getText().toString());
            } else {
                obj.put("email", emailEditText.getText().toString());
            }
        } catch (JSONException e) {
            Toast.makeText(context, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
        }
        return obj;
    }

    private void setPassword() {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                url.getSetPassword(),
                objectMaker(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(context, "Password Updated Successfully", Toast.LENGTH_SHORT).show();
                        loadingOverlay.setVisibility(View.GONE);
                        fragmentChanger(new LoginFragment());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        loadingOverlay.setVisibility(View.GONE);
                        actionButton.setVisibility(View.VISIBLE);

                        if (volleyError.networkResponse != null && volleyError.networkResponse.data != null) {
                            try {
                                String responseBody = new String(volleyError.networkResponse.data, "utf-8");
                                JSONObject errorObj = new JSONObject(responseBody);

                                // Server error message
                                String errorMessage = errorObj.optString("error", "Something went wrong");
                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();

                                if (errorMessage.toLowerCase().contains("password")) {
                                    passwordEditText.setError(errorMessage);
                                    passwordEditText.requestFocus();
                                }

                            } catch (Exception e) {
                                Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show();
                            }
                        } else if (volleyError instanceof AuthFailureError) {
                            Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show();
                        } else if (volleyError instanceof NetworkError) {
                            loadingOverlay.setVisibility(View.GONE);
                            actionButton.setVisibility(View.VISIBLE);
                            Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show();
                        } else if (volleyError instanceof ServerError) {
                            loadingOverlay.setVisibility(View.GONE);
                            actionButton.setVisibility(View.VISIBLE);
                            Toast.makeText(context, "Server Error", Toast.LENGTH_SHORT).show();
                        } else if (volleyError instanceof TimeoutError) {
                            loadingOverlay.setVisibility(View.GONE);
                            actionButton.setVisibility(View.VISIBLE);
                            Toast.makeText(context, "Timeout Error", Toast.LENGTH_SHORT).show();
                        } else {
                            loadingOverlay.setVisibility(View.GONE);
                            actionButton.setVisibility(View.VISIBLE);
                            Toast.makeText(context, "Unexpected Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Retry policy
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                30 * 1000, // 30s timeout
                0,         // no retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(jsonObjectRequest);
    }


    private void verifyOtp() {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                url.getCheckOtp(),
                objectMaker(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        // OTP Verified Successfully
                        Toast.makeText(context, "OTP Verified", Toast.LENGTH_SHORT).show();
                        loadingOverlay.setVisibility(View.GONE);
                        currentStage = ResetPasswordStage.PASSWORD_UPDATE;
                        emailEditText.setEnabled(false);
                        updateUIForCurrentStage();
                        adjustContainerHeight();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        loadingOverlay.setVisibility(View.GONE);
                        actionButton.setVisibility(View.VISIBLE);

                        if (volleyError.networkResponse != null && volleyError.networkResponse.data != null) {
                            try {
                                String responseBody = new String(volleyError.networkResponse.data, "utf-8");
                                JSONObject errorObj = new JSONObject(responseBody);

                                // Handle server-side error message
                                String errorMessage = errorObj.optString("error", "Something went wrong");
                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();

                                if (errorMessage.toLowerCase().contains("otp")) {
                                    otpEditTxt.setError(errorMessage);
                                    otpEditTxt.requestFocus();
                                }

                            } catch (Exception e) {
                                Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show();
                            }
                        } else if (volleyError instanceof AuthFailureError) {
                            emailEditText.setError("Multiple accounts in our system");
                            emailEditText.requestFocus();
                        } else if (volleyError instanceof NetworkError) {
                            loadingOverlay.setVisibility(View.GONE);
                            actionButton.setVisibility(View.VISIBLE);
                            Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show();
                        } else if (volleyError instanceof ServerError) {
                            loadingOverlay.setVisibility(View.GONE);
                            actionButton.setVisibility(View.VISIBLE);
                            Toast.makeText(context, "Server Error", Toast.LENGTH_SHORT).show();
                        } else if (volleyError instanceof TimeoutError) {
                            loadingOverlay.setVisibility(View.GONE);
                            actionButton.setVisibility(View.VISIBLE);
                            Toast.makeText(context, "Timeout Error", Toast.LENGTH_SHORT).show();
                        } else {
                            loadingOverlay.setVisibility(View.GONE);
                            actionButton.setVisibility(View.VISIBLE);
                            Toast.makeText(context, "Unexpected Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Add retry policy (same as sendEmail)
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                30 * 1000, // 30s timeout
                0,         // no retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(jsonObjectRequest);
    }


    private void sendEmail() {
        // UI state: Show loading, hide button
        loadingOverlay.setVisibility(View.VISIBLE);
        actionButton.setVisibility(View.GONE);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                url.getOtpRequest(),
                objectMaker(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (!isAdded()) return;

                        // 1. STAGE UPDATE: Only move forward on success
                        currentStage = ResetPasswordStage.OTP_VALIDATION;

                        // 2. UI UPDATE
                        loadingOverlay.setVisibility(View.GONE);
                        emailEditText.setEnabled(false); // Lock email to prevent mismatch during OTP entry
                        updateUIForCurrentStage();
                        adjustContainerHeight();

                        Toast.makeText(context, "OTP Sent Successfully", Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (!isAdded()) return;

                        // UI STATE: Reset so user can try again
                        loadingOverlay.setVisibility(View.GONE);
                        actionButton.setVisibility(View.VISIBLE);

                        // 1. Handle HTTP Error Responses (429, 400, 404, 500 etc.)
                        if (volleyError.networkResponse != null) {
                            int statusCode = volleyError.networkResponse.statusCode;

                            try {
                                String responseBody = new String(volleyError.networkResponse.data, "utf-8");
                                JSONObject errorObj = new JSONObject(responseBody);
                                String serverMessage = errorObj.optString("error", "Request failed");

                                if (statusCode == 429) {
                                    // Specific handling for your TimedCache block
                                    Toast.makeText(context, "Security: " + serverMessage, Toast.LENGTH_LONG).show();
                                } else if (statusCode == 400) {
                                    emailEditText.setError(serverMessage);
                                    emailEditText.requestFocus();
                                } else {
                                    Toast.makeText(context, serverMessage, Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(context, "Server Error: " + statusCode, Toast.LENGTH_SHORT).show();
                            }
                            return; // Exit after handling server-side error
                        }

                        // 2. Handle Client-Side / Network Errors
                        if (volleyError instanceof TimeoutError) {
                            Toast.makeText(context, "Request timed out. Check your connection.", Toast.LENGTH_SHORT).show();
                        } else if (volleyError instanceof NetworkError) {
                            Toast.makeText(context, "Network Error: No internet connection.", Toast.LENGTH_SHORT).show();
                        } else if (volleyError instanceof AuthFailureError) {
                            Toast.makeText(context, "Session expired or authentication failed.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "An unexpected error occurred. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Set RetryPolicy: 30s timeout, 0 retries (to prevent double OTP requests)
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(jsonObjectRequest);
    }


}