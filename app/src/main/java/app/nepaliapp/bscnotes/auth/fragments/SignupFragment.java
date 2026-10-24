package app.nepaliapp.bscnotes.auth.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.hbb20.CountryCodePicker;

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
import app.nepaliapp.bscnotes.common_auth.MySingleton;
import app.nepaliapp.bscnotes.common_auth.StorageClass;
import app.nepaliapp.bscnotes.dashboard.DashBoardManagerActivity;


public class SignupFragment extends Fragment {
    ImageView backImg;
    LinearLayout stepOneLayout, stepTwoLayout;
    AppCompatButton nextButton, signUpButton, backToStepOneButton;
    EditText nameEditText, emailEditText, passwordEditText, refferedEditText, phoneEditText;
    CountryCodePicker ccp;
    ToggleButton togglePasswordVisibility;
    StorageClass storageClass;
    Url url;
    Context context;
    RequestQueue requestQueue;
    CommonFunctions commonFunctions;
    FrameLayout loadingOverlay;

    public SignupFragment() {
        // Required empty public constructor
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        this.requestQueue = MySingleton.getInstance(context).getRequestQueue();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signup, container, false);
        init(view);
        stepOneLayout.setVisibility(View.VISIBLE);
        stepTwoLayout.setVisibility(View.GONE);


        nextButton.setOnClickListener(v -> {
            if (TextUtils.isEmpty(nameEditText.getText().toString())) {
                nameEditText.setError("Name is Required");
                nameEditText.requestFocus();
                return;
            }
            ccp.registerCarrierNumberEditText(phoneEditText);
            // Check validity
            if (ccp.isValidFullNumber()) {
                stepOneLayout.setVisibility(View.GONE);
                stepTwoLayout.setVisibility(View.VISIBLE);
            } else {
                phoneEditText.setError("Invalid Phone Number");
                phoneEditText.requestFocus();
                return;
            }
            String fullNumber = ccp.getFullNumberWithPlus(); // e.g. +9779812345678
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(emailEditText.getText().toString())) {
                    emailEditText.setError("Email is Required");
                    emailEditText.requestFocus();
                } else if (!emailCheckerRegex(emailEditText.getText().toString())) {
                    emailEditText.setError("Enter valid Email");
                    emailEditText.requestFocus();
                } else if (TextUtils.isEmpty(passwordEditText.getText().toString())) {
                    passwordEditText.setError("Password is Required");
                    passwordEditText.requestFocus();
                } else if (!(passwordEditText.getText().length() >= 6)) {
                    passwordEditText.setError("Minimum 6 digit Password is Required");
                    passwordEditText.requestFocus();
                } else {
                    registerRequest();
                }

            }
        });


        backToStepOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stepOneLayout.setVisibility(View.VISIBLE);
                stepTwoLayout.setVisibility(View.GONE);
            }
        });


        backImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isVisible(stepOneLayout)) {
                    if (nameEditText.getText().toString().isEmpty() && emailEditText.getText().toString().isEmpty() && passwordEditText.getText().toString().isEmpty() && phoneEditText.getText().toString().isEmpty()) {
                        fragmentChanger(new LoginFragment());
                    } else {
                        showWarningAlertDialog(requireContext());
                    }
                } else {
                    stepOneLayout.setVisibility(View.VISIBLE);
                    stepTwoLayout.setVisibility(View.GONE);
                }
            }
        });


        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isVisible(stepOneLayout)) {
                    if (nameEditText.getText().toString().isEmpty() && emailEditText.getText().toString().isEmpty() && passwordEditText.getText().toString().isEmpty() && phoneEditText.getText().toString().isEmpty()) {
                        fragmentChanger(new LoginFragment());
                    } else {
                        showWarningAlertDialog(requireContext());
                    }
                } else {
                    stepOneLayout.setVisibility(View.VISIBLE);
                    stepTwoLayout.setVisibility(View.GONE);
                }
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                callback
        );
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


        return view;
    }

    private void fragmentChanger(Fragment fragment) {
        if (getActivity() != null && getActivity() instanceof SigninManagerActivity) {
            ((SigninManagerActivity) getActivity()).replaceFragments(fragment);
        }

    }

    public void init(View view) {
        //Initialization
        storageClass = new StorageClass(requireContext());
        url = new Url();

        // Step groups
        stepOneLayout = view.findViewById(R.id.stepOneLayout);
        stepTwoLayout = view.findViewById(R.id.stepTwoLayout);

        // Buttons
        nextButton = view.findViewById(R.id.nextButton);
        signUpButton = view.findViewById(R.id.signUpButton);
        backToStepOneButton = view.findViewById(R.id.backToStepOneButton);
        backImg = view.findViewById(R.id.backBtn);
        // EditTexts
        nameEditText = view.findViewById(R.id.FullName);
        emailEditText = view.findViewById(R.id.EmailEdittext);
        passwordEditText = view.findViewById(R.id.PasswordEdittextEdittext);
        refferedEditText = view.findViewById(R.id.Referral_id);
        phoneEditText = view.findViewById(R.id.phoneEditText);
        ccp = view.findViewById(R.id.ccp);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        togglePasswordVisibility = view.findViewById(R.id.togglePasswordVisibility);
        commonFunctions = new CommonFunctions();


    }

    private boolean isVisible(View v) {
        return v.getVisibility() == View.VISIBLE;
    }

    private void showWarningAlertDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Going to Sign in page");
        builder.setMessage("Are you sure you want back? All Entered data will be lost");
        builder.setPositiveButton("Yes", (dialog, which) -> fragmentChanger(new LoginFragment()));
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.show();

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

    private JSONObject getSignupData() {
        JSONObject object = new JSONObject();
        try {
            object.put("name", nameEditText.getText().toString().trim());
            object.put("phoneNumber", ccp.getFullNumberWithPlus());
            object.put("emailId", emailEditText.getText().toString().trim());
            object.put("password", passwordEditText.getText().toString().trim());
            object.put("deviceID", CommonFunctions.getUniqueId(requireContext()));
            String referred = refferedEditText.getText().toString().trim();
            if (referred.isEmpty()) {
                referred = "none";
            }
            object.put("refer", referred);
            object.put("country", ccp.getSelectedCountryName());
        } catch (JSONException e) {
            Toast.makeText(context, "Device Error,Try after restart App", Toast.LENGTH_SHORT).show();
        }

        return object;
    }

    private void registerRequest() {
        loadingOverlay.setVisibility(View.VISIBLE);
        JsonObjectRequest resiteredObject = new JsonObjectRequest(Request.Method.POST, url.getSignup(), getSignupData(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                loadingOverlay.setVisibility(View.GONE);
                String token = response.optString("token");
                String refreshtoken =  response.optString("refreshToken");
                Toast.makeText(context, response.optString("message"), Toast.LENGTH_SHORT).show();
                storageClass.updateJwtToken(token);
                storageClass.updateRefreshToken(refreshtoken);
                Intent intent = new Intent(getActivity(), DashBoardManagerActivity.class);
                intent.putExtra("who", response.optString("who"));
                startActivity(intent);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                loadingOverlay.setVisibility(View.GONE);
                String message = "Unknown Error";
                if (error instanceof AuthFailureError) {
                    message = "Email or Password is wrong";
                } else if (error instanceof NetworkError) {
                    message = "Network Error";
                } else if (error instanceof ServerError) {
                    try {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            // Convert the error data to a string
                            String errorData = new String(error.networkResponse.data, "UTF-8");

                            // Parse the error JSON
                            JSONObject jsonError = new JSONObject(errorData);

                            message = jsonError.optString("error", "Unknown error occurred");

                        } else {
                            message = "No response from server";
                        }
                    } catch (Exception e) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    }
                } else if (error instanceof TimeoutError) {
                    message = "Timeout Error";
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
        requestQueue.add(resiteredObject);

    }


}