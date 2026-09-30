package lk.menara.studysphere.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;
import lk.menara.studysphere.R;
import lk.menara.studysphere.adapter.OrderAdapter;
import lk.menara.studysphere.databinding.FragmentSellerOrdersBinding;
import lk.menara.studysphere.model.Order;

public class SellerOrdersFragment extends Fragment implements OrderAdapter.OnOrderClickListener {

    private FragmentSellerOrdersBinding binding;
    private FirebaseFirestore db;
    private OrderAdapter adapter;
    private List<Order> allOrdersList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSellerOrdersBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();

        setupRecyclerView();
        loadOrdersFromFirebase();

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new OrderAdapter(allOrdersList, this);
        binding.recyclerViewOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewOrders.setAdapter(adapter);
    }

    private void loadOrdersFromFirebase() {
        db.collection("orders")
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    allOrdersList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Order order = doc.toObject(Order.class);
                        if (order != null) {
                            order.setOrderId(doc.getId());
                            allOrdersList.add(order);
                        }
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onOrderClick(Order order) {
        openOrderDetails(order);
    }

    private void openOrderDetails(Order order) {
        SellerOrderDetailsFragment fragment = new SellerOrderDetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putString("orderId", order.getOrderId());
        fragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}