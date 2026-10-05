package lk.menara.studysphere.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

import lk.menara.studysphere.R;
import lk.menara.studysphere.activity.NotificationActivity;
import lk.menara.studysphere.databinding.FragmentHomeBinding;
import lk.menara.studysphere.model.Category;
import lk.menara.studysphere.model.Product;
import lk.menara.studysphere.adapter.CategoryAdapter;
import lk.menara.studysphere.adapter.ProductAdapter;
import lk.menara.studysphere.model.User;
import lk.menara.studysphere.util.CacheDatabaseHelper;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private List<Product> allProducts = new ArrayList<>();
    private List<Product> topRatedProducts = new ArrayList<>();
    private ProductAdapter topRatedAdapter;
    private ProductAdapter nearestAdapter;

    private static class ProductWithDistance {
        Product product;
        float distanceKm;
        ProductWithDistance(Product product, float distanceKm) {
            this.product = product;
            this.distanceKm = distanceKm;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        showHomeUI(false);

        setupRecyclerViews();
        setupHeader();
        setupMenuIcon();
        setupNotificationIcon();
        setupCategories();
        loadFeaturedProducts();
        requestLocationPermission();
        setupSearch();

        return binding.getRoot();
    }

    private void showHomeUI(boolean show) {
        if (binding != null) {
            binding.homeMainContent.setVisibility(show ? View.VISIBLE : View.INVISIBLE);

            if (binding.loadingLayout != null) {
                binding.loadingLayout.loadingOverlay.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        }
    }

    private void setupRecyclerViews() {
        topRatedAdapter = new ProductAdapter(new ArrayList<>(), this::openDetails);
        binding.recyclerTopRated.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerTopRated.setAdapter(topRatedAdapter);

        nearestAdapter = new ProductAdapter(new ArrayList<>(), this::openDetails);
        binding.recyclerNearest.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerNearest.setAdapter(nearestAdapter);
    }

    private void loadFeaturedProducts() {
        // NEW: Try loading from cache first if available
        CacheDatabaseHelper cacheDb = CacheDatabaseHelper.getInstance(requireContext());
        
        FirebaseFirestore.getInstance().collection("products").get()
                .addOnSuccessListener(query -> {
                    if (!isAdded() || binding == null) return;
                    List<Product> products = new ArrayList<>();
                    for (DocumentSnapshot doc : query) {
                        Product p = doc.toObject(Product.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            products.add(p);
                        }
                    }
                    allProducts.clear();
                    allProducts.addAll(products);

                    // NEW: Cache the products
                    cacheDb.put(CacheDatabaseHelper.KEY_HOME_PRODUCTS, products);

                    if (products.isEmpty()) {
                        showHomeUI(true);
                    } else {
                        loadProductRatings(products);
                    }
                })
                .addOnFailureListener(e -> {
                    // NEW: Load from cache on failure
                    String cachedJson = cacheDb.get(CacheDatabaseHelper.KEY_HOME_PRODUCTS);
                    if (cachedJson != null) {
                        try {
                            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Product>>() {}.getType();
                            List<Product> cachedProducts = new com.google.gson.Gson().fromJson(cachedJson, listType);
                            if (cachedProducts != null && !cachedProducts.isEmpty()) {
                                allProducts.clear();
                                allProducts.addAll(cachedProducts);
                                loadProductRatings(cachedProducts);
                                Toast.makeText(getContext(), "Showing offline data", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    showHomeUI(true);
                });
    }

    private void loadProductRatings(List<Product> products) {
        final int total = products.size();
        final int[] count = {0};

        for (Product product : products) {
            FirebaseFirestore.getInstance().collection("products").document(product.getId())
                    .collection("reviews").get().addOnSuccessListener(snapshot -> {
                        float totalRating = 0f;
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Number r = doc.getDouble("rating");
                            if (r != null) totalRating += r.floatValue();
                        }
                        product.setRating(snapshot.size() > 0 ? totalRating / snapshot.size() : 0);
                        count[0]++;

                        if (count[0] == total) {
                            showTopRated(products);
                            loadNearestProducts();
                            showHomeUI(true);
                        }
                    }).addOnFailureListener(e -> {
                        count[0]++;
                        if (count[0] == total) showHomeUI(true);
                    });
        }
    }

    private void showTopRated(List<Product> products) {
        if (!isAdded() || binding == null) return;
        List<Product> sortedList = new ArrayList<>(products);
        Collections.sort(sortedList, (a, b) -> Float.compare(b.getRating(), a.getRating()));

        topRatedProducts.clear();
        for (Product p : sortedList) {
            if (p.getRating() > 0) topRatedProducts.add(p);
            if (topRatedProducts.size() == 10) break;
        }
        topRatedAdapter.updateData(topRatedProducts);
    }

    private void loadNearestProducts() {
        if (currentLocation == null || allProducts.isEmpty()) return;
        List<ProductWithDistance> distList = new ArrayList<>();
        for (Product p : allProducts) {
            if (p.getLatitude() != 0 && p.getLongitude() != 0) {
                Location loc = new Location("");
                loc.setLatitude(p.getLatitude());
                loc.setLongitude(p.getLongitude());
                float dist = currentLocation.distanceTo(loc) / 1000f;
                distList.add(new ProductWithDistance(p, dist));
            }
        }
        Collections.sort(distList, (a, b) -> Float.compare(a.distanceKm, b.distanceKm));
        List<Product> nearestOnly = new ArrayList<>();
        for (int i = 0; i < Math.min(10, distList.size()); i++) {
            nearestOnly.add(distList.get(i).product);
        }
        if (isAdded() && binding != null) nearestAdapter.updateData(nearestOnly);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000);

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                currentLocation = locationResult.getLastLocation();
                if (currentLocation != null) {
                    loadNearestProducts();
                    fusedLocationClient.removeLocationUpdates(this);
                }
            }
        }, Looper.getMainLooper());
    }

    private void setupSearch() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase();
                if (query.isEmpty()) {
                    topRatedAdapter.updateData(topRatedProducts);
                } else {
                    List<Product> filtered = new ArrayList<>();
                    for (Product p : allProducts) {
                        if (p.getName().toLowerCase().contains(query)) filtered.add(p);
                    }
                    topRatedAdapter.updateData(filtered);
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable editable) {}
        });
    }

    private void setupHeader() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;

        if (hour >= 4 && hour < 12) {
            greeting = "Good Morning";
        } else if (hour >= 12 && hour < 16) {
            greeting = "Good Afternoon";
        } else if (hour >= 16 && hour < 22) {
            greeting = "Good Evening";
        } else {
            greeting = "Good Night";
        }

        if (binding != null && binding.includeHeader != null) {
            binding.includeHeader.greetingText.setText(greeting);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (!isAdded() || binding == null) return;
                        User user = doc.toObject(User.class);
                        String name = (user != null && user.getName() != null) ? user.getName() : "User";
                        binding.includeHeader.usernameText.setText(name);

                        if (user != null && user.getProfilePicUrl() != null && !user.getProfilePicUrl().isEmpty()) {
                            Glide.with(requireContext())
                                    .load(user.getProfilePicUrl())
                                    .circleCrop()
                                    .into(binding.includeHeader.profileImage);
                        }
                    });
        }
    }

    private void setupMenuIcon() {
        binding.includeHeader.menuIcon.setOnClickListener(v -> {
            DrawerLayout drawer = requireActivity().findViewById(R.id.drawer_layout);
            if (drawer != null) drawer.openDrawer(GravityCompat.START);
        });
    }

    private void setupNotificationIcon() {
        binding.includeHeader.notificationIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NotificationActivity.class);
            startActivity(intent);
        });
    }

    private void setupCategories() {
        binding.recyclerCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        CategoryAdapter adapter = new CategoryAdapter(new ArrayList<>(), true);
        binding.recyclerCategories.setAdapter(adapter);
        FirebaseFirestore.getInstance().collection("categories").get().addOnSuccessListener(q -> {
            List<Category> list = new ArrayList<>();
            for (DocumentSnapshot doc : q) {
                Category c = doc.toObject(Category.class);
                if (c != null) { c.setCategoryId(doc.getId()); list.add(c); }
            }
            adapter.updateData(list);
        });
    }

    private void openDetails(Product product) {
        ProductDetailsFragment fragment = ProductDetailsFragment.newInstance(product);
        getParentFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).addToBackStack(null).commit();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}