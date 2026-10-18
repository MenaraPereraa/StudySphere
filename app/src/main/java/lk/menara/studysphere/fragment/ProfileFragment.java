package lk.menara.studysphere.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import lk.menara.studysphere.R;
import lk.menara.studysphere.activity.MainActivity;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private Uri selectedImageUri = null;
    private Uri cameraImageUri = null;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private View loadingOverlay;
    private ListenerRegistration userListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            String savedUri = savedInstanceState.getString("cameraImageUri");
            if (savedUri != null) {
                cameraImageUri = Uri.parse(savedUri);
            }
        }

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Camera permission is required to take photo", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (cameraImageUri != null) {
            outState.putString("cameraImageUri", cameraImageUri.toString());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        loadingOverlay = view.findViewById(R.id.loadingLayout);
        ImageView backButton = view.findViewById(R.id.back_button);
        TextInputEditText tvName = view.findViewById(R.id.tv_name);
        TextInputEditText tvEmail = view.findViewById(R.id.tv_email);
        TextInputEditText tvPhone = view.findViewById(R.id.tv_phone);
        TextView tvRole = view.findViewById(R.id.tv_role);
        de.hdodenhof.circleimageview.CircleImageView profileImage = view.findViewById(R.id.profile_image);
        MaterialButton updateProfileBtn = view.findViewById(R.id.update_profile);

        tvEmail.setEnabled(false);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        backButton.setOnClickListener(v -> {
            boolean fromSideNav = getArguments() != null && getArguments().getBoolean("fromSideNav", false);
            if (fromSideNav) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).getDrawerLayout().openDrawer(GravityCompat.START);
                }
            } else {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else if (getActivity() != null) {
                    getActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (isAdded()) {
                            Glide.with(this)
                                    .load(selectedImageUri)
                                    .circleCrop()
                                    .into(profileImage);
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (cameraImageUri != null && isAdded()) {
                            selectedImageUri = cameraImageUri;
                            Glide.with(this)
                                    .load(selectedImageUri)
                                    .skipMemoryCache(true)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .circleCrop()
                                    .into(profileImage);

                            Toast.makeText(requireContext(), "Photo captured! Click Update Profile to save.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        profileImage.setOnClickListener(v -> showImageImportDialog());

        listenToUserProfile(tvName, tvEmail, tvPhone, tvRole, profileImage);

        updateProfileBtn.setOnClickListener(v -> {
            String name = (tvName.getText() != null) ? tvName.getText().toString().trim() : "";
            String phone = (tvPhone.getText() != null) ? tvPhone.getText().toString().trim() : "";

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUser != null) {
                showLoading(true);
                db.collection("users").document(currentUser.getUid())
                        .update("name", name, "mobile", phone)
                        .addOnSuccessListener(unused -> {
                            if (selectedImageUri != null) {
                                uploadProfileImage(selectedImageUri, currentUser.getUid(), profileImage);
                            } else {
                                showLoading(false);
                                Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                                syncProfileWithMainActivity();
                            }
                        })
                        .addOnFailureListener(e -> {
                            showLoading(false);
                            Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        return view;
    }

    private void listenToUserProfile(TextInputEditText tvName, TextInputEditText tvEmail,
                                     TextInputEditText tvPhone, TextView tvRole,
                                     de.hdodenhof.circleimageview.CircleImageView profileImage) {
        if (currentUser == null) return;
        showLoading(true);

        userListener = db.collection("users").document(currentUser.getUid())
                .addSnapshotListener((documentSnapshot, e) -> {
                    showLoading(false);
                    if (e != null || documentSnapshot == null || !documentSnapshot.exists() || !isAdded()) return;

                    tvName.setText(documentSnapshot.getString("name"));
                    tvEmail.setText(documentSnapshot.getString("email"));
                    tvPhone.setText(documentSnapshot.getString("mobile"));
                    String role = documentSnapshot.getString("role");
                    tvRole.setText("seller".equals(role) ? "Seller" : "Buyer");

                    if (selectedImageUri == null) {
                        String imageUrl = documentSnapshot.getString("profilePicUrl");
                        loadProfileImage(imageUrl, profileImage);
                    }
                });
    }

    private void showImageImportDialog() {
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermissionAndOpen();
            } else if (which == 1) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                imagePickerLauncher.launch(intent);
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        photoFile);

                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                cameraLauncher.launch(cameraIntent);
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void loadProfileImage(String imageUrl, de.hdodenhof.circleimageview.CircleImageView profileImage) {
        if (imageUrl == null || imageUrl.isEmpty() || !isAdded()) return;

        if (imageUrl.startsWith("data:image") || imageUrl.startsWith("/9j/")) {
            try {
                String cleanBase64 = imageUrl.contains(",") ? imageUrl.split(",")[1] : imageUrl;
                byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                Glide.with(this)
                        .load(bitmap)
                        .circleCrop()
                        .into(profileImage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Glide.with(this)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .circleCrop()
                    .into(profileImage);
        }
    }

    private void uploadProfileImage(Uri uri, String userId, de.hdodenhof.circleimageview.CircleImageView profileImage) {
        if (getContext() == null || uri == null) return;

        showLoading(true);

        StorageReference storageRef = FirebaseStorage.getInstance("gs://studysphere-87b0b")
                .getReference()
                .child("profile-images/" + userId + ".jpg");

        storageRef.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return storageRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String originalUrl = task.getResult().toString();
                        String updatedUrl = originalUrl + "&t=" + System.currentTimeMillis();

                        db.collection("users").document(userId)
                                .update("profilePicUrl", updatedUrl)
                                .addOnSuccessListener(unused -> {
                                    showLoading(false);
                                    if (isAdded()) {
                                        selectedImageUri = null;
                                        loadProfileImage(updatedUrl, profileImage);
                                        Toast.makeText(getContext(), "Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
                                        syncProfileWithMainActivity();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    showLoading(false);
                                    if (isAdded()) {
                                        Toast.makeText(getContext(), "Firestore update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        showLoading(false);
                        if (isAdded() && task.getException() != null) {
                            Toast.makeText(getContext(), "Upload failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void syncProfileWithMainActivity() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).fetchUserData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }
}