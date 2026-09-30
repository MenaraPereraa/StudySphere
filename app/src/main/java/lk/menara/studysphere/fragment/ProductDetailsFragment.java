package lk.menara.studysphere.fragment;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.menara.studysphere.R;
import lk.menara.studysphere.model.Product;
import lk.menara.studysphere.model.Review;

public class ProductDetailsFragment extends Fragment {

    private AppCompatButton btnAddToCart, btnBuyNow;
    private ImageView productImage;
    private TextView productTitle, productPrice, productDescription, productRatingText;
    private TextView quantityText, btnIncrease, btnDecrease, tvSellerName, tvAvailableQty;
    private TextView btnAddReview;
    private LinearLayout reviewsContainer;

    private View btnCallSeller;
    private MapView mapView;
    private GoogleMap googleMap;

    private View loadingLayout;
    private View mainContent;

    private int selectedQuantity = 1;
    private int initialStock = 0;
    private Product product;
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private String contactPhoneNumber = "";

    public static ProductDetailsFragment newInstance(Product product) {
        ProductDetailsFragment fragment = new ProductDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable("arg_product", product);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            product = (Product) getArguments().getSerializable("arg_product");
            if (product != null) {
                initialStock = product.getAvailableQty();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_product_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        showLoading(true);
        loadProductDetails();
        fetchContactDetails();
        loadReviewsAndFinish();
        setupListeners();
    }

    private void initViews(View view) {
        loadingLayout = view.findViewById(R.id.loadingLayout);
        mainContent = view.findViewById(R.id.mainContent);
        productImage = view.findViewById(R.id.product_image);
        productTitle = view.findViewById(R.id.product_title);
        productPrice = view.findViewById(R.id.product_price);
        productDescription = view.findViewById(R.id.product_description);
        productRatingText = view.findViewById(R.id.product_rating);
        quantityText = view.findViewById(R.id.tv_quantity);
        btnIncrease = view.findViewById(R.id.btn_increase);
        btnDecrease = view.findViewById(R.id.btn_decrease);
        btnAddReview = view.findViewById(R.id.btn_add_review);
        reviewsContainer = view.findViewById(R.id.reviews_container);
        btnAddToCart = view.findViewById(R.id.product_details_btn_add_cart);
        btnBuyNow = view.findViewById(R.id.product_details_btn_buy_now);
        btnCallSeller = view.findViewById(R.id.btn_call_seller);
        tvSellerName = view.findViewById(R.id.tv_seller_name);
        tvAvailableQty = view.findViewById(R.id.tv_available_qty);

        mapView = view.findViewById(R.id.map_view);
        mapView.onCreate(null);
        mapView.onResume();
        mapView.getMapAsync(map -> {
            googleMap = map;
            showProductLocation();
        });
    }

    private void showLoading(boolean isVisible) {
        if (loadingLayout != null && mainContent != null) {
            loadingLayout.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            mainContent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        }
    }

    private void setupListeners() {
        btnIncrease.setOnClickListener(v -> {
            if (selectedQuantity < initialStock) {
                selectedQuantity++;
                updateQtyUI();
            } else {
                Toast.makeText(getContext(), "Out of stock!", Toast.LENGTH_SHORT).show();
            }
        });

        btnDecrease.setOnClickListener(v -> {
            if (selectedQuantity > 1) {
                selectedQuantity--;
                updateQtyUI();
            }
        });

        btnCallSeller.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(contactPhoneNumber)) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + contactPhoneNumber));
                startActivity(intent);
            }
        });

        btnAddReview.setOnClickListener(v -> showReviewDialog());
        btnAddToCart.setOnClickListener(v -> addToCartAction());
        btnBuyNow.setOnClickListener(v -> addToCartAction());
    }

    private void updateQtyUI() {
        quantityText.setText(String.valueOf(selectedQuantity));

        int remainingStock = initialStock - selectedQuantity;
        if (tvAvailableQty != null) {
            tvAvailableQty.setText("(Available: " + remainingStock + ")");
        }

        productPrice.setText(String.format(Locale.US, "LKR %,.2f", product.getPrice() * selectedQuantity));
    }

    private void loadProductDetails() {
        if (product == null) return;
        productTitle.setText(product.getName());
        productDescription.setText(product.getDescription());
        updateQtyUI();

        if (!TextUtils.isEmpty(product.getImage())) {
            try {
                String base64Str = product.getImage().contains(",") ? product.getImage().split(",")[1] : product.getImage();
                byte[] bytes = Base64.decode(base64Str, Base64.DEFAULT);
                productImage.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            } catch (Exception e) {
                productImage.setImageResource(R.drawable.add_photo_alternate_24px);
            }
        }
    }

    private void addToCartAction() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        String uid = firebaseAuth.getUid();

        db.runTransaction(transaction -> {
            DocumentSnapshot productRef = transaction.get(db.collection("products").document(product.getId()));
            long currentStock = productRef.getLong("availableQty");

            if (currentStock < selectedQuantity) {
                throw new RuntimeException("Insufficient stock!");
            }
            transaction.update(productRef.getReference(), "availableQty", currentStock - selectedQuantity);

            Map<String, Object> cartItem = new HashMap<>();
            cartItem.put("productId", product.getId());
            cartItem.put("title", product.getName());
            cartItem.put("price", product.getPrice());
            cartItem.put("quantity", selectedQuantity);
            cartItem.put("imageUrl", product.getImage());

            transaction.set(db.collection("users").document(uid).collection("cart").document(product.getId()), cartItem);

            return null;
        }).addOnSuccessListener(result -> {
            showLoading(false);
            Toast.makeText(getContext(), "Success!", Toast.LENGTH_SHORT).show();
            goToCart();
        }).addOnFailureListener(e -> {
            showLoading(false);
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void goToCart() {
        if (isAdded()) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new CartFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void fetchContactDetails() {
        if (product == null || TextUtils.isEmpty(product.getSellerId())) return;
        db.collection("users").document(product.getSellerId()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvSellerName.setText(doc.getString("name"));
                        contactPhoneNumber = doc.getString("mobile");
                    }
                });
    }

    private void loadReviewsAndFinish() {
        if (product == null) { showLoading(false); return; }
        db.collection("products").document(product.getId()).collection("reviews")
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) {
                        reviewsContainer.removeAllViews();
                        List<Review> list = snap.toObjects(Review.class);
                        for (Review r : list) {
                            reviewsContainer.addView(createReviewCard(r));
                        }
                    }
                    showLoading(false);
                });
    }

    private void showReviewDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_review, null);
        dialog.setContentView(view);
        RatingBar rb = view.findViewById(R.id.ratingBar);
        EditText et = view.findViewById(R.id.et_review);
        AppCompatButton btnSubmit = view.findViewById(R.id.btn_submit_review);
        btnSubmit.setOnClickListener(v -> {
            String comment = et.getText().toString().trim();
            if (!TextUtils.isEmpty(comment)) {
                saveReview(comment, rb.getRating(), dialog);
            }
        });
        dialog.show();
    }

    private void saveReview(String comment, float rating, BottomSheetDialog dialog) {
        String uid = firebaseAuth.getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            String name = userDoc.getString("name");
            String userImage = userDoc.getString("profileImage");
            String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
            Review review = new Review(name != null ? name : "Anonymous", comment, rating, date, userImage);
            db.collection("products").document(product.getId()).collection("reviews")
                    .add(review)
                    .addOnSuccessListener(r -> {
                        Toast.makeText(getContext(), "Review Added!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
        });
    }

    private View createReviewCard(Review review) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(550, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 20, 0);
        card.setLayoutParams(params);
        card.setRadius(25f);
        card.setCardElevation(2f);
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        TextView name = new TextView(getContext());
        name.setText(review.getUserName());
        name.setTypeface(null, Typeface.BOLD);
        RatingBar rb = new RatingBar(getContext(), null, android.R.attr.ratingBarStyleSmall);
        rb.setRating(review.getRating());
        rb.setIsIndicator(true);
        TextView comment = new TextView(getContext());
        comment.setText(review.getComment());
        layout.addView(name);
        layout.addView(rb);
        layout.addView(comment);
        card.addView(layout);
        return card;
    }

    private void showProductLocation() {
        if (googleMap == null || product == null) return;
        LatLng loc = new LatLng(product.getLatitude(), product.getLongitude());
        googleMap.addMarker(new MarkerOptions().position(loc).title("Pickup Location"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f));
    }

    @Override public void onResume() { super.onResume(); if(mapView != null) mapView.onResume(); }
    @Override public void onPause() { if(mapView != null) mapView.onPause(); super.onPause(); }
    @Override public void onDestroy() { if(mapView != null) mapView.onDestroy(); super.onDestroy(); }
}