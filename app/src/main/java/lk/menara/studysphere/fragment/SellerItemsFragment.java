package lk.menara.studysphere.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import lk.menara.studysphere.R;
import lk.menara.studysphere.adapter.SellerProductAdapter;
import lk.menara.studysphere.databinding.FragmentSellerItemsBinding;
import lk.menara.studysphere.model.Product;

public class SellerItemsFragment extends Fragment implements SellerProductAdapter.OnItemClickListener {

    private FragmentSellerItemsBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private List<Product> productList;
    private SellerProductAdapter adapter;

    public SellerItemsFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentSellerItemsBinding.inflate(inflater, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        productList = new ArrayList<>();
        adapter = new SellerProductAdapter(productList, this);
        binding.recyclerViewItems.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewItems.setAdapter(adapter);

        binding.btnAddItem.setOnClickListener(v -> {
            AddItemFragment addItemFragment = new AddItemFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, addItemFragment)
                    .addToBackStack(null)
                    .commit();
        });

        showLoading(true);
        new android.os.Handler().postDelayed(this::loadSellerItems, 200);

        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        return binding.getRoot();
    }

    private void showLoading(boolean isVisible) {
        if (isAdded() && binding != null && binding.loadingLayout != null) {
            binding.loadingLayout.getRoot().setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    private void loadSellerItems() {
        if (firebaseAuth.getCurrentUser() == null) {
            showLoading(false);
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String sellerId = firebaseAuth.getCurrentUser().getUid();

        firebaseFirestore.collection("products")
                .whereEqualTo("sellerId", sellerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);

                    productList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setId(document.getId());
                        productList.add(product);
                    }
                    adapter.updateFullList(new ArrayList<>(productList));
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Failed to load items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onItemClick(Product product) {
        if (product == null) return;

        SellerItemDetailsFragment detailsFragment = new SellerItemDetailsFragment();
        Bundle bundle = new Bundle();


        bundle.putString("productId", product.getId());
        detailsFragment.setArguments(bundle);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.content_frame, detailsFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}