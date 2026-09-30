package lk.menara.studysphere.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lk.menara.studysphere.R;
import lk.menara.studysphere.databinding.FragmentSellerOrderDetailsBinding;
import lk.menara.studysphere.model.Order;
import lk.menara.studysphere.model.Payment;
import lk.menara.studysphere.model.Product;
import lk.menara.studysphere.util.ReceiptGenerator;

public class SellerOrderDetailsFragment extends Fragment {

    private FragmentSellerOrderDetailsBinding binding;
    private FirebaseFirestore db;
    private String orderId;

    private Order currentOrder;
    private Payment currentPayment;
    private Map<String, Product> productMap = new HashMap<>();

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

        binding.btnGenerateReceipt.setOnClickListener(v -> {
            if (currentOrder != null && currentPayment != null) {
                File pdfFile = ReceiptGenerator.generateReceipt(requireContext(), currentOrder, currentPayment, productMap);
                if (pdfFile != null) ReceiptGenerator.shareReceipt(requireContext(), pdfFile);
            } else {
                Toast.makeText(getContext(), "Loading data, please wait...", Toast.LENGTH_SHORT).show();
            }
        });

        return binding.getRoot();
    }

    private void loadDataFromFirestore() {
        db.collection("orders").document(orderId).get().addOnSuccessListener(doc -> {
            currentOrder = doc.toObject(Order.class);
            if (currentOrder != null) {
                currentOrder.setOrderId(doc.getId());

                db.collection("payments").whereEqualTo("orderId", orderId).get().addOnSuccessListener(paymentDocs -> {
                    if (!paymentDocs.isEmpty()) {
                        currentPayment = paymentDocs.getDocuments().get(0).toObject(Payment.class);
                    }

                    loadProductsAndPopulateUI();
                });
            }
        });
    }

    private void loadProductsAndPopulateUI() {
        if (currentOrder.getOrderItems() == null || currentOrder.getOrderItems().isEmpty()) {
            updateUI();
            return;
        }

        final int[] count = {0};
        for (Order.OrderItem item : currentOrder.getOrderItems()) {
            db.collection("products").document(item.getProductId()).get().addOnSuccessListener(pDoc -> {
                Product p = pDoc.toObject(Product.class);
                if (p != null) productMap.put(item.getProductId(), p);

                count[0]++;
                if (count[0] == currentOrder.getOrderItems().size()) {
                    updateUI();
                }
            });
        }
    }

    private void updateUI() {
        View root = binding.getRoot();

        setTextSafe(root, R.id.tv_order_no_value, currentOrder.getOrderId());
        if (currentOrder.getOrderDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            setTextSafe(root, R.id.tv_date_value, sdf.format(currentOrder.getOrderDate().toDate()));
        }

        if (currentOrder.getShippingAddress() != null) {
            Order.Address addr = currentOrder.getShippingAddress();
            setTextSafe(root, R.id.tv_customer_name, "Name: " + addr.getName());
            setTextSafe(root, R.id.tv_customer_email, "Email: " + addr.getEmail());
            setTextSafe(root, R.id.tv_customer_address, "Contact: " + addr.getContact());
        }

        if (!currentOrder.getOrderItems().isEmpty()) {
            Order.OrderItem item = currentOrder.getOrderItems().get(0);
            Product p = productMap.get(item.getProductId());
            setTextSafe(root, R.id.tv_item_name, p != null ? p.getName() : "Unknown Item");
            setTextSafe(root, R.id.tv_item_qty, String.valueOf(item.getQuantity()));
            setTextSafe(root, R.id.tv_item_price, String.format(Locale.getDefault(), "LKR %,.2f", item.getUnitPrice()));
        }

        String totalFormatted = String.format(Locale.getDefault(), "LKR %,.2f", currentOrder.getTotalAmount());
        setTextSafe(root, R.id.tv_subtotal_value, totalFormatted);
        setTextSafe(root, R.id.tv_total_value, totalFormatted);

        if (binding.chipOrderStatus != null) {
            binding.chipOrderStatus.setText(currentOrder.getStatus());
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