package lk.menara.studysphere.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lk.menara.studysphere.R;
import lk.menara.studysphere.adapter.DiscoverProductAdapter;
import lk.menara.studysphere.model.Product;
import lk.menara.studysphere.util.CacheDatabaseHelper;

public class DiscoverFragment extends Fragment {

    private RecyclerView recyclerView;
    private DiscoverProductAdapter adapter;
    private List<Product> allProducts = new ArrayList<>();
    private FirebaseFirestore db;
    private EditText searchEditText;
    private Chip chipAll, chipRated, chipNearby;
    private View loadingLayout, mainContent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discover, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        loadingLayout = view.findViewById(R.id.loadingLayout);
        mainContent = view.findViewById(R.id.mainContent);
        recyclerView = view.findViewById(R.id.recycler_discover_products);
        searchEditText = view.findViewById(R.id.search_edit_text);
        chipAll = view.findViewById(R.id.chip_all);
        chipRated = view.findViewById(R.id.chip_most_rated);
        chipNearby = view.findViewById(R.id.chip_nearby);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        view.findViewById(R.id.back_button).setOnClickListener(v -> requireActivity().onBackPressed());

        showLoading(true);
        loadProducts();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterProducts(); }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        View.OnClickListener chipListener = v -> {
            chipAll.setChecked(v.getId() == R.id.chip_all);
            chipRated.setChecked(v.getId() == R.id.chip_most_rated);
            chipNearby.setChecked(v.getId() == R.id.chip_nearby);
            filterProducts();
        };
        chipAll.setOnClickListener(chipListener);
        chipRated.setOnClickListener(chipListener);
        chipNearby.setOnClickListener(chipListener);
    }

    private void showLoading(boolean isVisible) {
        if (loadingLayout != null && mainContent != null) {
            loadingLayout.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            mainContent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        }
    }

    private void loadProducts() {
        if (!isAdded() || getContext() == null) return;
        
        CacheDatabaseHelper cacheDb = CacheDatabaseHelper.getInstance(getContext());
        
        db.collection("products").get().addOnSuccessListener(snapshots -> {
            if (!isAdded() || getContext() == null) return;
            
            allProducts.clear();
            if (snapshots.isEmpty()) { showLoading(false); return; }
            for (QueryDocumentSnapshot doc : snapshots) {
                Product product = doc.toObject(Product.class);
                if (product != null) {
                    product.setId(doc.getId());
                    allProducts.add(product);
                }
            }
            cacheDb.put(CacheDatabaseHelper.KEY_DISCOVER_PRODUCTS, allProducts);
            loadProductRatings();
        }).addOnFailureListener(e -> {
            if (!isAdded() || getContext() == null) return;
            
            String cachedJson = cacheDb.get(CacheDatabaseHelper.KEY_DISCOVER_PRODUCTS);
            if (cachedJson != null) {
                try {
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Product>>() {}.getType();
                    List<Product> cachedProducts = new com.google.gson.Gson().fromJson(cachedJson, listType);
                    if (cachedProducts != null && !cachedProducts.isEmpty()) {
                        allProducts.clear();
                        allProducts.addAll(cachedProducts);
                        loadProductRatings();
                        Toast.makeText(getContext(), "Showing offline data", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            showLoading(false);
        });
    }

    private void loadProductRatings() {
        final int[] processed = {0};
        for (Product p : allProducts) {
            db.collection("products").document(p.getId()).collection("reviews").get()
                    .addOnSuccessListener(snap -> {
                        if (!isAdded() || getContext() == null) return;
                        
                        float total = 0;
                        for (QueryDocumentSnapshot d : snap) {
                            Double r = d.getDouble("rating");
                            if (r != null) total += r.floatValue();
                        }
                        p.setRating(snap.isEmpty() ? 0 : total / snap.size());
                        processed[0]++;
                        if (processed[0] == allProducts.size()) {
                            showLoading(false);
                            filterProducts();
                        }
                    }).addOnFailureListener(e -> {
                        if (!isAdded() || getContext() == null) return;
                        
                        processed[0]++;
                        if (processed[0] == allProducts.size()) showLoading(false);
                    });
        }
    }

    private void filterProducts() {
        if (!isAdded() || getContext() == null) return;
        
        String query = searchEditText.getText().toString().toLowerCase().trim();
        List<Product> filtered = new ArrayList<>(allProducts);

        if (!query.isEmpty()) {
            filtered = filtered.stream()
                    .filter(p -> p.getName().toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }

        if (chipRated.isChecked()) {
            Collections.sort(filtered, (p1, p2) -> Float.compare(p2.getRating(), p1.getRating()));
        }

        if (adapter == null) {
            adapter = new DiscoverProductAdapter(getContext(), filtered, new DiscoverProductAdapter.OnItemClickListener() {
                @Override public void onItemClick(Product product) {}
                @Override public void onAddClick(Product product) {}
                @Override public void onWishlistClick(Product product) { toggleWishlist(product); }
            });
            recyclerView.setAdapter(adapter);
            loadUserWishlist();
        } else {
            adapter.updateList(filtered);
        }
    }

    private void loadUserWishlist() {
        if (!isAdded() || getContext() == null) return;
        
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        db.collection("users").document(userId).collection("wishlist").get()
                .addOnSuccessListener(snapshots -> {
                    if (!isAdded() || getContext() == null) return;
                    
                    List<String> ids = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        ids.add(doc.getId());
                    }
                    if (adapter != null) adapter.setWishlistIds(ids);
                });
    }

    private void toggleWishlist(Product product) {
        if (!isAdded() || getContext() == null) return;
        
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference wishRef = db.collection("users")
                .document(userId)
                .collection("wishlist")
                .document(product.getId());

        wishRef.get().addOnSuccessListener(doc -> {
            if (!isAdded() || getContext() == null) return;
            
            if (doc.exists()) {
                wishRef.delete().addOnSuccessListener(aVoid -> {
                    if (!isAdded() || getContext() == null) return;
                    Toast.makeText(getContext(), "Removed from Wishlist", Toast.LENGTH_SHORT).show();
                });
            } else {
                Map<String, Object> wishItem = new HashMap<>();
                wishItem.put("productId", product.getId());
                wishItem.put("timestamp", System.currentTimeMillis());
                wishRef.set(wishItem).addOnSuccessListener(aVoid -> {
                    if (!isAdded() || getContext() == null) return;
                    Toast.makeText(getContext(), "Added to Wishlist", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}