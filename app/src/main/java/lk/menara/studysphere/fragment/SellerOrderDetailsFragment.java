package lk.menara.studysphere.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.menara.studysphere.R;
import lk.menara.studysphere.databinding.FragmentSellerOrderDetailsBinding;
import lk.menara.studysphere.model.Order;
import lk.menara.studysphere.model.Payment;
import lk.menara.studysphere.model.Product;
import lk.menara.studysphere.util.DateTimePickerHelper;

public class SellerOrderDetailsFragment extends Fragment {

    private FragmentSellerOrderDetailsBinding binding;
    private FirebaseFirestore db;
    private String orderId;

    private Order currentOrder;
    private Payment currentPayment;
    private final Map<String, Product> productMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSellerOrderDetailsBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            orderId = getArguments().getString("orderId");
            loadDataFromFirestore();
        }

        binding.backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        // Tracking date/time picker
        binding.btnSetTrackingDatetime.setOnClickListener(v -> {
            if (currentOrder != null) {
                DateTimePickerHelper.showDateTimePicker(this, currentOrder.getTrackingDateTime(),
                        (timestamp, formatted) -> saveTrackingDateTime(timestamp));
            }
        });

        return binding.getRoot();
    }

    private void loadDataFromFirestore() {
        db.collection("orders").document(orderId).get().addOnSuccessListener(doc -> {
            currentOrder = doc.toObject(Order.class);
            if (currentOrder != null) {
                currentOrder.setOrderId(doc.getId());

                // Checkout saves payments in the "payment" collection (singular)
                db.collection("payment").whereEqualTo("orderId", orderId).get()
                        .addOnSuccessListener(paymentDocs -> {
                            if (!paymentDocs.isEmpty()) {
                                currentPayment = paymentDocs.getDocuments().get(0).toObject(Payment.class);
                            }
                            loadProductsAndPopulateUI();
                        })
                        .addOnFailureListener(e -> loadProductsAndPopulateUI());
            }
        });
    }

    private void loadProductsAndPopulateUI() {
        if (currentOrder.getOrderItems() == null || currentOrder.getOrderItems().isEmpty()) {
            updateUI();
            return;
        }

        final int total = currentOrder.getOrderItems().size();
        final int[] count = {0};
        for (Order.OrderItem item : currentOrder.getOrderItems()) {
            db.collection("products").document(item.getProductId()).get()
                    .addOnSuccessListener(pDoc -> {
                        Product p = pDoc.toObject(Product.class);
                        if (p != null) productMap.put(item.getProductId(), p);

                        count[0]++;
                        if (count[0] == total) updateUI();
                    })
                    .addOnFailureListener(e -> {
                        count[0]++;
                        if (count[0] == total) updateUI();
                    });
        }
    }

    private void updateUI() {
        if (binding == null || currentOrder == null) return;
        View root = binding.getRoot();

        // Order / payment info
        setTextSafe(root, R.id.tv_order_no_value, currentOrder.getOrderId());

        if (currentOrder.getOrderDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault());
            setTextSafe(root, R.id.tv_date_value, sdf.format(currentOrder.getOrderDate().toDate()));
        }

        String paymentId = (currentPayment != null && currentPayment.getPaymentId() != null)
                ? currentPayment.getPaymentId() : "-";
        String transactionId = (currentPayment != null && currentPayment.getTransactionId() != null)
                ? currentPayment.getTransactionId() : "-";
        setTextSafe(root, R.id.tv_payment_id_value, paymentId);
        setTextSafe(root, R.id.tv_transaction_id_value, transactionId);

        // Customer details
        if (currentOrder.getShippingAddress() != null) {
            Order.Address addr = currentOrder.getShippingAddress();
            setTextSafe(root, R.id.tv_customer_name, addr.getName());
            setTextSafe(root, R.id.tv_customer_email, addr.getEmail());
            setTextSafe(root, R.id.tv_customer_address, addr.getContact());
        }

        // Order items table (all items)
        LinearLayout container = root.findViewById(R.id.items_container);
        container.removeAllViews();

        double subtotal = 0;
        List<Order.OrderItem> items = currentOrder.getOrderItems();
        if (items != null) {
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            int index = 1;
            for (Order.OrderItem item : items) {
                Product p = productMap.get(item.getProductId());
                double amount = item.getUnitPrice() * item.getQuantity();
                subtotal += amount;

                View row = inflater.inflate(R.layout.item_seller_order_line, container, false);
                setTextSafe(row, R.id.tv_line_no, String.valueOf(index));
                setTextSafe(row, R.id.tv_line_name, p != null ? p.getName() : "Unknown Item");
                setTextSafe(row, R.id.tv_line_qty, String.valueOf(item.getQuantity()));
                setTextSafe(row, R.id.tv_line_amount, String.format(Locale.US, "%,.2f", amount));

                if (index % 2 == 0) row.setBackgroundColor(Color.parseColor("#E8F3EF"));
                container.addView(row);
                index++;
            }
        }

        // Totals (shipping = total - items subtotal)
        double total = currentOrder.getTotalAmount();
        double shipping = Math.max(0, total - subtotal);
        setTextSafe(root, R.id.tv_subtotal_value, String.format(Locale.US, "LKR %,.2f", subtotal));
        setTextSafe(root, R.id.tv_shipping_value, String.format(Locale.US, "LKR %,.2f", shipping));
        setTextSafe(root, R.id.tv_total_value, String.format(Locale.US, "LKR %,.2f", total));

        // Status
        if (binding.chipOrderStatus != null && currentOrder.getStatus() != null) {
            binding.chipOrderStatus.setText(currentOrder.getStatus());
        }

        // Tracking (shows "Not set" when empty)
        setTextSafe(root, R.id.tv_tracking_datetime_value,
                DateTimePickerHelper.formatDateTime(currentOrder.getTrackingDateTime()));
    }

    // Save tracking date/time to Firestore
    private void saveTrackingDateTime(Timestamp timestamp) {
        if (orderId != null && timestamp != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("trackingDateTime", timestamp);
            updates.put("trackingUpdatedAt", Timestamp.now());

            db.collection("orders").document(orderId).update(updates)
                    .addOnSuccessListener(v -> {
                        currentOrder.setTrackingDateTime(timestamp);
                        currentOrder.setTrackingUpdatedAt(Timestamp.now());
                        updateUI();
                        Toast.makeText(getContext(), "Tracking date updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed to update tracking", Toast.LENGTH_SHORT).show());
        }
    }

    private void setTextSafe(View root, int id, String text) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(text);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}