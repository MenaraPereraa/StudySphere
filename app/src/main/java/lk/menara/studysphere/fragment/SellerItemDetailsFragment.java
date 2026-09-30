package lk.menara.studysphere.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Locale;

import lk.menara.studysphere.R;
import lk.menara.studysphere.adapter.ReviewAdapter;
import lk.menara.studysphere.databinding.FragmentSellerItemDetailsBinding;
import lk.menara.studysphere.model.Product;
import lk.menara.studysphere.model.Review;

public class SellerItemDetailsFragment extends Fragment implements OnMapReadyCallback {

    private FragmentSellerItemDetailsBinding binding;
    private Product product;
    private GoogleMap googleMap;
    private FirebaseFirestore firestore;
    private String productId;
    private static final String TAG = "SellerDetails";

    public SellerItemDetailsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentSellerItemDetailsBinding.inflate(inflater, container, false);
        firestore = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            productId = getArguments().getString("productId");
        }

        binding.mapView.onCreate(savedInstanceState);

        binding.backButton.setOnClickListener(v -> requireActivity().onBackPressed());
        binding.btnViewReviews.setOnClickListener(v -> showReviewsDialog());
        binding.btnEdit.setOnClickListener(v -> navigateToEditItem());
        binding.editButton.setOnClickListener(v -> navigateToEditItem());

        fetchProductFromFirestore();

        return binding.getRoot();
    }

    private void fetchProductFromFirestore() {
        if (productId == null || productId.isEmpty()) {
            Toast.makeText(getContext(), "Invalid Product ID", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        firestore.collection("products").document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        product = documentSnapshot.toObject(Product.class);
                        if (product != null) {
                            product.setId(documentSnapshot.getId());
                            loadProductUI();
                            binding.mapView.getMapAsync(this);
                        }
                    } else {
                        Toast.makeText(getContext(), "Product not found", Toast.LENGTH_SHORT).show();
                    }
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProductUI() {
        if (product == null || binding == null) return;

        binding.productName.setText(product.getName());
        binding.productCategory.setText(product.getCategory());
        binding.productRating.setText(String.format(Locale.US, "%.1f ⭐", product.getRating()));
        binding.productDescription.setText(product.getDescription());
        binding.stockQuantity.setText(product.getAvailableQty() + " units available");
        binding.productPrice.setText(String.format(Locale.US, "LKR %,.2f", product.getPrice()));

        if (product.getImage() != null && !product.getImage().isEmpty()) {
            try {
                String base64 = product.getImage();
                if (base64.contains(",")) {
                    base64 = base64.split(",")[1];
                }
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                binding.productImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                binding.productImage.setImageResource(R.drawable.default_profile);
            }
        }
    }

    private void showReviewsDialog() {
        if (product == null || product.getId() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_reviews_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);

        RecyclerView rvReviews = view.findViewById(R.id.rv_reviews);
        TextView tvNoReviews = view.findViewById(R.id.tv_no_reviews);

        rvReviews.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        ArrayList<Review> reviewList = new ArrayList<>();
        ReviewAdapter adapter = new ReviewAdapter(reviewList);
        rvReviews.setAdapter(adapter);

        firestore.collection("products")
                .document(product.getId())
                .collection("reviews")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        tvNoReviews.setVisibility(View.VISIBLE);
                        rvReviews.setVisibility(View.GONE);
                    } else {
                        tvNoReviews.setVisibility(View.GONE);
                        rvReviews.setVisibility(View.VISIBLE);
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Review review = doc.toObject(Review.class);
                            reviewList.add(review);
                        }
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Reviews Error: ", e));

        bottomSheetDialog.show();
    }
    private void showLoading(boolean isVisible) {
        if (isAdded() && binding != null && binding.loadingLayout != null) {
            binding.loadingLayout.getRoot().setVisibility(isVisible ? View.VISIBLE : View.GONE);
            binding.mainScrollView.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        if (product != null) {
            LatLng location;
            if (product.getLatitude() != 0 || product.getLongitude() != 0) {
                location = new LatLng(product.getLatitude(), product.getLongitude());
            } else {
                location = new LatLng(6.9271, 79.8612);
            }
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(location).title(product.getName()));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
        }
    }

    private void navigateToEditItem() {
        if (product == null) return;
        AddItemFragment addItemFragment = new AddItemFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("edit_product", product);
        addItemFragment.setArguments(bundle);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.content_frame, addItemFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override public void onResume() { super.onResume(); if(binding != null) binding.mapView.onResume(); }
    @Override public void onPause() { if(binding != null) binding.mapView.onPause(); super.onPause(); }
    @Override public void onDestroy() { if(binding != null) { binding.mapView.onDestroy(); binding = null; } super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if(binding != null) binding.mapView.onLowMemory(); }
}