package lk.menara.studysphere.fragment;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

import lk.menara.studysphere.R;
import lk.menara.studysphere.databinding.FragmentCheckoutBinding;
import lk.menara.studysphere.listener.FirestoreCallback;
import lk.menara.studysphere.model.CartItem;
import lk.menara.studysphere.model.Order;
import lk.menara.studysphere.model.Payment;
import lk.menara.studysphere.model.Product;
import lk.menara.studysphere.model.User;
import lk.menara.studysphere.util.ReceiptGenerator;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;

    private double total = 0.0;
    private boolean isProcessing = false;
    private String currentOrderId;

    private List<String> selectedIds;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private static final String CHANNEL_ID = "payment_notifications";

    private final String MERCHANT_ID = "1222752";
    private final String MERCHANT_SECRET = "NjYyODE3MTU5MzY4OTEwMzI2OTcwNDUwNzU1NzI0NzMyMA==";
    private final String CLIENT_ID = "4OVybuzw1Wi4JDDSNV6WHs3D0";
    private final String CLIENT_SECRET = "4vXAc28DX5X8cNdZr7eOvs4kpG67wPxNw4TzFN3Jsd9t";

    public static class TokenResponse {
        @SerializedName("access_token")
        String accessToken;
    }

    public static class PaymentSearchResponse {
        int status;
        String msg;
        List<PaymentData> data;
    }

    public static class PaymentData {
        @SerializedName("order_id")
        String orderId;
        String status;
        @SerializedName("status_code")
        int statusCode = -1;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            selectedIds = getArguments().getStringArrayList("selected_ids");
        }

        createNotificationChannel();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);
        binding.backButton.setOnClickListener(v -> requireActivity().onBackPressed());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadUserProfile();
        calculateOrderTotal();
        setupBiometricAuth();

        binding.checkoutBtnProceed.setOnClickListener(v -> {
            if (total > 0 && !isProcessing) {
                checkBiometricAndProceed();
            } else if (total <= 0) {
                Toast.makeText(getContext(), "Cart is empty!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean isVisible) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (binding != null && binding.loadingLayout != null) {
                    binding.loadingLayout.getRoot().setVisibility(isVisible ? View.VISIBLE : View.GONE);
                }
                if (binding != null) {
                    binding.checkoutBtnProceed.setEnabled(!isVisible);
                }
            });
        }
    }

    private void setupBiometricAuth() {
        executor = ContextCompat.getMainExecutor(requireContext());
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                startPayHerePayment();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                isProcessing = false;
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    Toast.makeText(getContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm Payment")
                .setSubtitle("Please authenticate to proceed with LKR " + total)
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();
    }

    private void checkBiometricAndProceed() {
        BiometricManager biometricManager = BiometricManager.from(requireContext());
        int canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);

        switch (canAuth) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                biometricPrompt.authenticate(promptInfo);
                break;

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(getContext(), "This device doesn't support fingerprint verification.", Toast.LENGTH_SHORT).show();
                startPayHerePayment();
                break;

            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(getContext(), "No fingerprint is set up on this device.", Toast.LENGTH_SHORT).show();
                startPayHerePayment();
                break;

            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(getContext(), "Fingerprint hardware is currently unavailable.", Toast.LENGTH_SHORT).show();
                startPayHerePayment();
                break;

            default:
                Toast.makeText(getContext(), "Biometric verification isn't available. Proceeding to secure checkout.", Toast.LENGTH_SHORT).show();
                startPayHerePayment();
                break;
        }
    }

    private void startPayHerePayment() {
        isProcessing = true;
        currentOrderId = "ORD-" + System.currentTimeMillis();

        InitRequest req = new InitRequest();
        req.setSandBox(true);
        req.setMerchantId(MERCHANT_ID);
        req.setMerchantSecret(MERCHANT_SECRET);
        req.setCurrency("LKR");
        req.setAmount(total);
        req.setOrderId(currentOrderId);
        req.setItemsDescription("StudySphere Order " + currentOrderId);

        req.getCustomer().setFirstName(binding.shippingDetailsName.getText().toString());
        req.getCustomer().setLastName("Customer");
        req.getCustomer().setEmail(binding.shippingDetailsEmail.getText().toString());
        req.getCustomer().setPhone(binding.shippingDetailsContact.getText().toString());
        req.getCustomer().getAddress().setAddress("Colombo");
        req.getCustomer().getAddress().setCity("Colombo");
        req.getCustomer().getAddress().setCountry("Sri Lanka");

        Intent intent = new Intent(getActivity(), PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
        paymentLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> paymentLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    PHResponse<StatusResponse> response = (PHResponse<StatusResponse>)
                            result.getData().getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                    if (response != null && response.isSuccess()) {
                        verifyPaymentWithOAuth(response.getData());
                    } else {
                        handleUiError("Payment Failed or Canceled");
                    }
                } else {
                    isProcessing = false;
                }
            });

    private void verifyPaymentWithOAuth(StatusResponse statusResponse) {
        if (getActivity() == null) return;
        showLoading(true);

        String authString = CLIENT_ID + ":" + CLIENT_SECRET;
        String authBase64 = Base64.encodeToString(authString.getBytes(), Base64.NO_WRAP);

        RequestBody tokenBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .build();

        Request tokenRequest = new Request.Builder()
                .url("https://sandbox.payhere.lk/merchant/v1/oauth/token")
                .header("Authorization", "Basic " + authBase64)
                .post(tokenBody)
                .build();

        httpClient.newCall(tokenRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                showLoading(false);
                handleUiError("Verification Network Error");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        TokenResponse token = gson.fromJson(response.body().string(), TokenResponse.class);
                        if (token != null && token.accessToken != null) {
                            searchPayment(token.accessToken, statusResponse);
                        } else {
                            showLoading(false);
                            handleUiError("Token error");
                        }
                    } catch (Exception e) {
                        showLoading(false);
                        handleUiError("Token error");
                    }
                } else {
                    response.close();
                    showLoading(false);
                    handleUiError("Token request failed");
                }
            }
        });
    }

    private void searchPayment(String accessToken, StatusResponse statusResponse) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Request searchRequest = new Request.Builder()
                    .url("https://sandbox.payhere.lk/merchant/v1/payment/search?order_id=" + currentOrderId)
                    .header("Authorization", "Bearer " + accessToken)
                    .get()
                    .build();

            httpClient.newCall(searchRequest).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    showLoading(false);
                    handleUiError("Verification Network Error");
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            PaymentSearchResponse result = gson.fromJson(response.body().string(), PaymentSearchResponse.class);

                            if (result != null && result.status == 1 && result.data != null && !result.data.isEmpty()) {
                                PaymentData transaction = result.data.get(0);
                                String statusValue = transaction.status == null ? "" : transaction.status;
                                int statusCode = transaction.statusCode;

                                if (statusValue.equalsIgnoreCase("RECEIVED") || statusCode == 2 || statusCode == 3) {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> saveOrderToFirestore(statusResponse));
                                    }
                                    return;
                                }
                            }
                            showLoading(false);
                            handleUiError("Payment not confirmed by PayHere.");
                        } catch (Exception e) {
                            showLoading(false);
                            handleUiError("Verification Parsing Error");
                        }
                    } else {
                        response.close();
                        showLoading(false);
                        handleUiError("Payment verification failed");
                    }
                }
            });
        }, 3000);
    }

    private void saveOrderToFirestore(StatusResponse statusResponse) {
        String uid = firebaseAuth.getUid();
        if (uid == null) { showLoading(false); return; }

        getCartItems(cartItems -> {
            List<String> productIds = new ArrayList<>();
            for (CartItem item : cartItems) productIds.add(item.getProductId());

            getProductsByIds(productIds, productMap -> {
                List<Order.OrderItem> orderItemsList = new ArrayList<>();
                for (CartItem cartItem : cartItems) {
                    orderItemsList.add(Order.OrderItem.builder()
                            .productId(cartItem.getProductId())
                            .unitPrice(cartItem.getPrice())
                            .quantity(cartItem.getQuantity())
                            .build());
                }

                Order order = new Order();
                order.setOrderId(currentOrderId);
                order.setUserId(uid);
                order.setTotalAmount(total);
                order.setStatus("PAID");
                order.setOrderDate(Timestamp.now());
                order.setOrderItems(orderItemsList);
                order.setShippingAddress(Order.Address.builder()
                        .name(binding.shippingDetailsName.getText().toString())
                        .email(binding.shippingDetailsEmail.getText().toString())
                        .contact(binding.shippingDetailsContact.getText().toString())
                        .build());

                Payment payment = Payment.builder()
                        .paymentId("PY-" + System.currentTimeMillis())
                        .orderId(currentOrderId)
                        .userId(uid)
                        .amount(total)
                        .paymentMethod("PayHere")
                        .status("SUCCESS")
                        .paymentDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()))
                        .transactionId(String.valueOf(statusResponse.getPaymentNo()))
                        .build();

                db.collection("orders").document(currentOrderId).set(order)
                        .addOnSuccessListener(aVoid -> {

                            db.collection("payment").document(payment.getPaymentId()).set(payment)
                                    .addOnSuccessListener(paymentVoid -> {

                                        sendPaymentNotification("Payment Successful", "Your payment for Order " + currentOrderId + " was successful!");
                                        saveNotificationToFirestore("Payment Successful", "Your payment for Order " + currentOrderId + " was successful!");
                                        saveMessageToSQLite("Payment Successful for " + currentOrderId);

                                        File file = ReceiptGenerator.generateReceipt(requireContext(), order, payment, productMap);
                                        showLoading(false);
                                        if (file != null) ReceiptGenerator.shareReceipt(requireContext(), file);
                                        clearCartAndFinish(uid);

                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("FirestoreError", "Payment collection error", e);
                                        showLoading(false);
                                    });
                        })
                        .addOnFailureListener(e -> {
                            showLoading(false);
                            handleUiError("Firestore Order Error");
                        });
            });
        });
    }

    private void saveNotificationToFirestore(String title, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("type", "Payment");

        db.collection("notifications").add(notification)
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error saving notification", e));
    }

    private void sendPaymentNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.check_circle_24px)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Payment Updates";
            String description = "Notifications for successful payments";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void saveMessageToSQLite(String message) {
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("message", message);
            values.put("timestamp", System.currentTimeMillis());
            db.insert("notifications", null, values);
            db.close();
        } catch (Exception e) {
            Log.e("SQLiteError", "Error saving message", e);
        }
    }

    public static class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, "StudySphereDB", null, 1);
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE notifications (id INTEGER PRIMARY KEY AUTOINCREMENT, message TEXT, timestamp LONG)");
        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS notifications");
            onCreate(db);
        }
    }

    private void clearCartAndFinish(String uid) {
        db.collection("users").document(uid).collection("cart").get()
                .addOnSuccessListener(qds -> {
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot ds : qds) {
                        if (selectedIds == null || selectedIds.contains(ds.getId())) {
                            batch.delete(ds.getReference());
                        }
                    }
                    batch.commit().addOnSuccessListener(unused -> navigateHome());
                });
    }

    private void handleUiError(String msg) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                isProcessing = false;
                showLoading(false);
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void navigateHome() {
        if (isAdded()) {
            getParentFragmentManager().beginTransaction()
                    .replace(lk.menara.studysphere.R.id.content_frame, new HomeFragment())
                    .commit();
        }
    }

    private void calculateOrderTotal() {
        getCartItems(cartItems -> {
            if (binding == null) return;

            double subTotal = 0;
            if (cartItems != null) {
                for (CartItem item : cartItems) subTotal += item.getPrice() * item.getQuantity();
            }
            double shipping = 400.0;
            total = subTotal + shipping;
            binding.checkoutSubtotal.setText(String.format(Locale.US, "LKR %,.2f", subTotal));
            binding.checkoutShipping.setText(String.format(Locale.US, "LKR %,.2f", shipping));
            binding.checkoutTotal.setText(String.format(Locale.US, "LKR %,.2f", total));
            setupBiometricAuth();
        });
    }

    private void getCartItems(FirestoreCallback<List<CartItem>> callback) {
        String uid = firebaseAuth.getUid();
        if (uid == null) return;
        db.collection("users").document(uid).collection("cart").get()
                .addOnSuccessListener(qds -> {
                    List<CartItem> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : qds) {
                        if (selectedIds != null && !selectedIds.contains(doc.getId())) continue;

                        CartItem item = doc.toObject(CartItem.class);
                        item.setId(doc.getId());
                        list.add(item);
                    }
                    callback.onCallback(list);
                });
    }

    private void loadUserProfile() {
        String uid = firebaseAuth.getUid();
        if (uid == null) return;
        db.collection("users").document(uid).get()
                .addOnSuccessListener(ds -> {
                    User u = ds.toObject(User.class);
                    if (u != null && binding != null) {
                        binding.shippingDetailsName.setText(u.getName());
                        binding.shippingDetailsEmail.setText(u.getEmail());
                        binding.shippingDetailsContact.setText(u.getMobile());
                    }
                });
    }

    private void getProductsByIds(List<String> productIds, FirestoreCallback<Map<String, Product>> callback) {
        if (productIds.isEmpty()) {
            callback.onCallback(new HashMap<>());
            return;
        }
        db.collection("products").whereIn(FieldPath.documentId(), productIds).get()
                .addOnSuccessListener(query -> {
                    Map<String, Product> map = new HashMap<>();
                    for (DocumentSnapshot doc : query) {
                        Product p = doc.toObject(Product.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            map.put(doc.getId(), p);
                        }
                    }
                    callback.onCallback(map);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}