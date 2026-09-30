package lk.menara.studysphere.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.menara.studysphere.R;
import lk.menara.studysphere.activity.MainActivity;
import lk.menara.studysphere.adapter.DiscoverProductAdapter;
import lk.menara.studysphere.model.Product;

public class WishlistFragment extends Fragment {

    private RecyclerView recyclerView;
    private DiscoverProductAdapter adapter;
    private List<Product> wishlistProducts = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;

    private View loadingLayout;
    private View mainContent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wishlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        loadingLayout = view.findViewById(R.id.loadingLayout);
        mainContent = view.findViewById(R.id.mainContent);
        recyclerView = view.findViewById(R.id.recycler_wishlist);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        ImageView backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).getDrawerLayout()
                        .openDrawer(androidx.core.view.GravityCompat.START);
            }
        });

        loadWishlistProducts();
    }

    private void showLoading(boolean isVisible) {
        if (loadingLayout != null && mainContent != null) {
            loadingLayout.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            mainContent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        }
    }

    private void loadWishlistProducts() {
        if (firebaseAuth.getCurrentUser() == null) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        showLoading(true);

        String uid = firebaseAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).collection("wishlist")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;

                    List<String> productIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        productIds.add(doc.getId());
                    }

                    if (productIds.isEmpty()) {
                        showEmptyState();
                        showLoading(false);
                    } else {
                        loadProductDetails(productIds);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    if (isAdded()) Toast.makeText(requireContext(), "Failed to load", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProductDetails(List<String> productIds) {
        wishlistProducts.clear();
        db.collection("products").get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String docId = doc.getId();
                        if (productIds.contains(docId)) {
                            Product product = doc.toObject(Product.class);
                            if (product != null) {
                                product.setId(docId);
                                product.setProductId(docId);
                                wishlistProducts.add(product);
                            }
                        }
                    }
                    loadWishlistProductRatings();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                });
    }

    private void loadWishlistProductRatings() {
        if (wishlistProducts.isEmpty()) {
            setupAdapter();
            showLoading(false);
            return;
        }

        final int[] loadedCount = {0};
        final int totalProducts = wishlistProducts.size();

        for (Product product : wishlistProducts) {
            db.collection("products").document(product.getId()).collection("reviews").get()
                    .addOnSuccessListener(querySnapshot -> {
                        float totalRating = 0f;
                        if (!querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot doc : querySnapshot) {
                                Double r = doc.getDouble("rating");
                                if (r != null) totalRating += r.floatValue();
                            }
                            product.setRating(totalRating / querySnapshot.size());
                        } else {
                            product.setRating(0f);
                        }

                        loadedCount[0]++;
                        if (loadedCount[0] == totalProducts) {
                            setupAdapter();
                            showLoading(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        loadedCount[0]++;
                        if (loadedCount[0] == totalProducts) {
                            setupAdapter();
                            showLoading(false);
                        }
                    });
        }
    }

    private void setupAdapter() {
        adapter = new DiscoverProductAdapter(requireContext(), wishlistProducts,
                new DiscoverProductAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(Product product) {
                        ProductDetailsFragment fragment = ProductDetailsFragment.newInstance(product);
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.content_frame, fragment)
                                .addToBackStack(null).commit();
                    }

                    @Override
                    public void onAddClick(Product product) {

                        if (firebaseAuth.getCurrentUser() == null) {
                            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String uid = firebaseAuth.getCurrentUser().getUid();

                        String productId = product.getProductId() != null
                                ? product.getProductId()
                                : product.getId();

                        if (productId == null) {
                            Toast.makeText(requireContext(), "Product ID error", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Map<String, Object> cartItem = new HashMap<>();
                        cartItem.put("productId", productId);
                        cartItem.put("title", product.getName());
                        cartItem.put("price", product.getPrice());
                        cartItem.put("quantity", 1);
                        cartItem.put("imageUrl", product.getImage());

                        db.collection("users")
                                .document(uid)
                                .collection("cart")
                                .document(productId)
                                .set(cartItem)
                                .addOnSuccessListener(unused -> {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), "Added to cart", Toast.LENGTH_SHORT).show();
                                        removeFromWishlist(product);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), "Failed to add", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onWishlistClick(Product product) {
                        removeFromWishlist(product);
                    }
                });

        recyclerView.setAdapter(adapter);
    }

    private void removeFromWishlist(Product product) {
        String uid = firebaseAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).collection("wishlist")
                .document(product.getProductId()).delete()
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Removed", Toast.LENGTH_SHORT).show();
                        loadWishlistProducts();
                    }
                });
    }

    private void showEmptyState() {
        wishlistProducts.clear();
        adapter = new DiscoverProductAdapter(requireContext(), wishlistProducts, null);
        recyclerView.setAdapter(adapter);
        if (isAdded()) Toast.makeText(requireContext(), "Wishlist is empty", Toast.LENGTH_SHORT).show();
    }
}