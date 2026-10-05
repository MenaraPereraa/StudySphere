package lk.menara.studysphere.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lk.menara.studysphere.activity.ReportsActivity;
import lk.menara.studysphere.adapter.OrderAdapter;
import lk.menara.studysphere.databinding.FragmentSellerHomeBinding;
import lk.menara.studysphere.model.Order;

public class SellerHomeFragment extends Fragment {

    private FragmentSellerHomeBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private OrderAdapter orderAdapter;
    private List<Order> orderList;

    public SellerHomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentSellerHomeBinding.inflate(inflater, container, false);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        setupRecyclerView();
        setupReportsButton();

        showLoading(true);
        loadDashboardData();

        return binding.getRoot();
    }

    private void setupReportsButton() {
        if (binding != null && binding.btnViewReports != null) {
            binding.btnViewReports.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ReportsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void showLoading(boolean isVisible) {
        if (isAdded() && binding != null && binding.loadingLayout != null) {
            binding.loadingLayout.getRoot().setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    private void setupRecyclerView() {
        orderList = new ArrayList<>();

        orderAdapter = new OrderAdapter(orderList, order -> {
            Toast.makeText(getContext(), "Order ID: " + order.getOrderId(), Toast.LENGTH_SHORT).show();
        });

        binding.rvRecentOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvRecentOrders.setNestedScrollingEnabled(false);
        binding.rvRecentOrders.setAdapter(orderAdapter);
    }

    private void loadDashboardData() {
        String uid = auth.getUid();
        if (uid == null) {
            showLoading(false);
            return;
        }

        firestore.collection("products")
                .whereEqualTo("sellerId", uid)
                .addSnapshotListener((value, error) -> {
                    if (isAdded() && binding != null && value != null) {
                        binding.tvProductCount.setText(String.valueOf(value.size()));
                    }
                });

        firestore.collection("orders")
                .whereIn("status", Arrays.asList("PENDING", "Pending"))
                .addSnapshotListener((value, error) -> {
                    if (isAdded() && binding != null && value != null) {
                        binding.tvPendingCount.setText(String.valueOf(value.size()));
                    }
                });

        firestore.collection("orders")
                .whereIn("status", Arrays.asList("PAID", "DELIVERED", "Delivered", "COMPLETED"))
                .addSnapshotListener((value, error) -> {
                    if (isAdded() && binding != null && value != null) {
                        binding.tvTotalSales.setText(String.valueOf(value.size()));
                    }
                });

        loadRecentOrders();
    }

    private void loadRecentOrders() {
        firestore.collection("orders")
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((value, error) -> {

                    showLoading(false);

                    if (error != null) {
                        Log.e("FIRESTORE_ERROR", "Error: " + error.getMessage());
                        return;
                    }

                    if (isAdded() && binding != null && value != null) {
                        orderList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                orderList.add(order);
                            }
                        }
                        orderAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}