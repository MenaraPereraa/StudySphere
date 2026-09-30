package lk.menara.studysphere.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import lk.menara.studysphere.databinding.ActivitySellerVerificationBinding;

public class SellerVerificationActivity extends AppCompatActivity {

    private static final String REQUEST_TAG = "SMS_OTP";
    private static final String SMS_URL = "https://www.smslenz.lk/api/send-sms";
    private static final String SMS_USER_ID = "1488";
    private static final String SMS_API_KEY = "46044b1e-ccc3-4d43-9dbc-272a187565ef";
    private static final String SMS_SENDER_ID = "SMSlenzDEMO";

    private ActivitySellerVerificationBinding binding;
    private String generatedOtp;
    private String userPhoneNumber;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        requestQueue = Volley.newRequestQueue(this);

        userPhoneNumber = getIntent().getStringExtra("phone");

        setupOtpInputs();

        if (userPhoneNumber != null && !userPhoneNumber.isEmpty()) {
            sendOtpViaSmsLenz(userPhoneNumber);
        } else {
            Toast.makeText(this, "Mobile number not found!", Toast.LENGTH_SHORT).show();
        }

        binding.verifyButton.setOnClickListener(view -> {
            String enteredOtp = getOtpFromFields();
            if (enteredOtp.length() < 6) {
                Toast.makeText(this, "Please enter 6-digit OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            if (generatedOtp != null && enteredOtp.equals(generatedOtp)) {
                updateUserRoleToSeller();
            } else {
                Toast.makeText(this, "Invalid OTP!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(REQUEST_TAG);
        }
    }

    private void showLoading(boolean isVisible) {
        runOnUiThread(() -> {
            if (binding.loadingLayout != null) {
                binding.loadingLayout.getRoot().setVisibility(isVisible ? View.VISIBLE : View.GONE);
            }
            binding.verifyButton.setEnabled(!isVisible);
        });
    }

    private String normalizeLkMobile(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 11 && digits.startsWith("94")) return "+" + digits;
        if (digits.length() == 10 && digits.startsWith("0")) return "+94" + digits.substring(1);
        if (digits.length() == 9) return "+94" + digits;
        return null;
    }

    private void sendOtpViaSmsLenz(String rawPhoneNumber) {
        final String phoneNumber = normalizeLkMobile(rawPhoneNumber);
        if (phoneNumber == null) {
            Toast.makeText(this, "Invalid mobile number", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        generatedOtp = String.valueOf(100000 + new SecureRandom().nextInt(900000));
        final String message = "Your StudySphere OTP is " + generatedOtp;

        StringRequest stringRequest = new StringRequest(Request.Method.POST, SMS_URL,
                response -> {
                    showLoading(false);
                    Log.d("SMS_RESPONSE", response.length() > 300 ? response.substring(0, 300) : response);
                    boolean success = false;
                    try {
                        success = new JSONObject(response).optBoolean("success", false);
                    } catch (JSONException ignored) {
                    }
                    if (success) {
                        Toast.makeText(this, "OTP Sent Successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        generatedOtp = null;
                        Toast.makeText(this, "SMS provider rejected the request", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    showLoading(false);
                    generatedOtp = null;
                    String body = "";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        body = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                        if (body.length() > 300) body = body.substring(0, 300);
                    }
                    Log.e("SMS_ERROR", error + " " + body);
                    Toast.makeText(this, "Failed to send OTP. Check Connection!", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", SMS_USER_ID);
                params.put("api_key", SMS_API_KEY);
                params.put("sender_id", SMS_SENDER_ID);
                params.put("contact", phoneNumber);
                params.put("message", message);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        stringRequest.setTag(REQUEST_TAG);
        requestQueue.add(stringRequest);
    }

    private void updateUserRoleToSeller() {
        if (mAuth.getCurrentUser() == null) return;

        showLoading(true);

        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("role", "seller");
        data.put("isSellerVerified", true);

        db.collection("users").document(uid).update(data)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Verification Successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, SellerMainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e("FIRESTORE_ERROR", String.valueOf(e.getMessage()));
                    Toast.makeText(this, "Database Error!", Toast.LENGTH_SHORT).show();
                });
    }

    private String getOtpFromFields() {
        return binding.otpDigit1.getText().toString().trim() +
                binding.otpDigit2.getText().toString().trim() +
                binding.otpDigit3.getText().toString().trim() +
                binding.otpDigit4.getText().toString().trim() +
                binding.otpDigit5.getText().toString().trim() +
                binding.otpDigit6.getText().toString().trim();
    }

    private void setupOtpInputs() {
        addOtpWatcher(binding.otpDigit1, binding.otpDigit2, null);
        addOtpWatcher(binding.otpDigit2, binding.otpDigit3, binding.otpDigit1);
        addOtpWatcher(binding.otpDigit3, binding.otpDigit4, binding.otpDigit2);
        addOtpWatcher(binding.otpDigit4, binding.otpDigit5, binding.otpDigit3);
        addOtpWatcher(binding.otpDigit5, binding.otpDigit6, binding.otpDigit4);
        addOtpWatcher(binding.otpDigit6, null, binding.otpDigit5);
    }

    private void addOtpWatcher(EditText current, EditText next, EditText previous) {
        current.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1 && next != null) next.requestFocus();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        current.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (current.getText().toString().isEmpty() && previous != null) {
                    previous.requestFocus();
                    return true;
                }
            }
            return false;
        });
    }
}