package lk.menara.studysphere.fragment;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import lk.menara.studysphere.activity.MapPickerActivity;
import lk.menara.studysphere.databinding.FragmentAddItemBinding;
import lk.menara.studysphere.model.Category;
import lk.menara.studysphere.model.Product;

public class AddItemFragment extends Fragment {

    private static final String STORAGE_BUCKET_URL = "gs://studysphere-87b0b";

    private FragmentAddItemBinding binding;
    private List<String> categoryNames = new ArrayList<>();
    private Uri selectedImageUri = null;
    private String existingImageUrl = null;
    private String pendingCategory = null;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private Double selectedLat = null;
    private Double selectedLng = null;
    private Product editProduct = null;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && isAdded() && binding != null) {
                    selectedImageUri = uri;
                    updateImageUI(uri);
                }
            });

    private final ActivityResultLauncher<Intent> mapLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (!isAdded() || binding == null) return;
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedLat = result.getData().getDoubleExtra("lat", 0);
                    selectedLng = result.getData().getDoubleExtra("lng", 0);

                    String address = getAddressFromLatLng(selectedLat, selectedLng);
                    binding.location.setText(address);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddItemBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance(STORAGE_BUCKET_URL);

        if (getArguments() != null && getArguments().containsKey("edit_product")) {
            editProduct = (Product) getArguments().getSerializable("edit_product");
            setupEditMode();
        }

        loadCategories();

        binding.backButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.imageUploadCard.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        binding.location.setOnClickListener(v -> mapLauncher.launch(new Intent(requireContext(), MapPickerActivity.class)));
        binding.btnCancel.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnSave.setOnClickListener(v -> saveItem());

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private String getAddressFromLatLng(double lat, double lng) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = address.getAddressLine(0);

                if (fullAddress != null && fullAddress.contains("+")) {
                    StringBuilder readableAddress = new StringBuilder();

                    if (address.getFeatureName() != null && !address.getFeatureName().contains("+")) {
                        readableAddress.append(address.getFeatureName()).append(", ");
                    }
                    if (address.getSubLocality() != null) {
                        readableAddress.append(address.getSubLocality()).append(", ");
                    }
                    if (address.getLocality() != null) {
                        readableAddress.append(address.getLocality()).append(", ");
                    }
                    if (address.getCountryName() != null) {
                        readableAddress.append(address.getCountryName());
                    }

                    String result = readableAddress.toString().trim();
                    if (!result.isEmpty()) {
                        return result.endsWith(",") ? result.substring(0, result.length() - 1) : result;
                    }
                }

                return fullAddress;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Kolonnawa, Sri Lanka";
    }

    private void setupEditMode() {
        if (editProduct == null) return;

        binding.uploadTitleText.setText("Edit Product");
        binding.btnSave.setText("Update Item");

        binding.itemName.setText(editProduct.getName());
        pendingCategory = editProduct.getCategory();
        binding.price.setText(String.valueOf(editProduct.getPrice()));
        binding.stock.setText(String.valueOf(editProduct.getAvailableQty()));
        binding.description.setText(editProduct.getDescription());

        selectedLat = editProduct.getLatitude();
        selectedLng = editProduct.getLongitude();

        if (selectedLat != null && selectedLat != 0) {
            binding.location.setText(getAddressFromLatLng(selectedLat, selectedLng));
        }

        if (editProduct.getImage() != null && !editProduct.getImage().isEmpty()) {
            existingImageUrl = editProduct.getImage();
            Glide.with(requireContext())
                    .load(existingImageUrl)
                    .into(binding.uploadImageView);
            hideImagePlaceholder();
        }
    }

    private void saveItem() {
        if (auth.getCurrentUser() == null) return;

        String itemName = binding.itemName.getText().toString().trim();
        String category = binding.categorySpinner.getSelectedItemPosition() > 0
                ? String.valueOf(binding.categorySpinner.getSelectedItem())
                : "";
        String priceStr = binding.price.getText().toString().trim();
        String stockStr = binding.stock.getText().toString().trim();
        String description = binding.description.getText().toString().trim();

        if (TextUtils.isEmpty(itemName) || TextUtils.isEmpty(category) || TextUtils.isEmpty(priceStr)) {
            Toast.makeText(requireContext(), "Fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        int stock;
        try {
            price = Double.parseDouble(priceStr);
            stock = TextUtils.isEmpty(stockStr) ? 0 : Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Enter a valid price and stock quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSave.setEnabled(false);

        Product product = new Product();
        product.setName(itemName);
        product.setCategory(category);
        product.setPrice(price);
        product.setAvailableQty(stock);
        product.setDescription(description);

        if (selectedImageUri != null) {
            uploadImageToStorageAndSave(product);
        } else {
            saveProductToFirestore(product, existingImageUrl);
        }
    }

    private void uploadImageToStorageAndSave(Product product) {
        String imageFileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = storage.getReference().child("product-images/" + imageFileName);

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> saveProductToFirestore(product, downloadUri.toString()))
                                .addOnFailureListener(e -> onSaveFailed("Could not get image URL: " + e.getMessage()))
                )
                .addOnFailureListener(e -> onSaveFailed("Image upload failed: " + e.getMessage()));
    }

    private void saveProductToFirestore(Product product, String imageUrl) {
        String id = (editProduct != null) ? editProduct.getId() : firestore.collection("products").document().getId();

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("sellerId", auth.getUid());
        data.put("name", product.getName());
        data.put("category", product.getCategory());
        data.put("description", product.getDescription());
        data.put("price", product.getPrice());
        data.put("availableQty", product.getAvailableQty());
        data.put("image", imageUrl);
        data.put("updatedAt", System.currentTimeMillis());

        // Location is saved as a "location" map with "lat" and "lng".
        // Product.setLocation(Map) reads these and fills latitude/longitude.
        if (selectedLat != null && selectedLng != null) {
            Map<String, Object> location = new HashMap<>();
            location.put("lat", selectedLat);
            location.put("lng", selectedLng);
            data.put("location", location);
        }

        firestore.collection("products").document(id).set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Success!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> onSaveFailed("Error: " + e.getMessage()));
    }

    private void onSaveFailed(String message) {
        if (!isAdded() || binding == null) return;
        binding.btnSave.setEnabled(true);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void updateImageUI(Uri uri) {
        Glide.with(requireContext()).load(uri).into(binding.uploadImageView);
        hideImagePlaceholder();
    }

    private void hideImagePlaceholder() {
        binding.uploadImageView.setColorFilter(null);
        binding.uploadImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        binding.uploadTitleText.setVisibility(View.GONE);
        binding.uploadSubtitleText.setVisibility(View.GONE);
        binding.uploadPlaceholder.setVisibility(View.GONE);
    }

    private void loadCategories() {
        firestore.collection("categories").get().addOnSuccessListener(snapshot -> {
            if (!isAdded() || binding == null) return;
            categoryNames.clear();
            categoryNames.add("Select Category");
            for (Category cat : snapshot.toObjects(Category.class)) categoryNames.add(cat.getName());

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_spinner_item, categoryNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.categorySpinner.setAdapter(adapter);

            if (pendingCategory != null) {
                int pos = categoryNames.indexOf(pendingCategory);
                if (pos >= 0) binding.categorySpinner.setSelection(pos);
            }
        });
    }
}