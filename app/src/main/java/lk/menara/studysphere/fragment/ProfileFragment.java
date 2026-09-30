package lk.menara.studysphere.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import lk.menara.studysphere.R;
import lk.menara.studysphere.activity.MainActivity;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private Uri selectedImageUri = null;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private View loadingOverlay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

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
                    ((MainActivity) getActivity()).getDrawerLayout().openDrawer(androidx.core.view.GravityCompat.START);
                }
            } else {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else {
                    requireActivity().onBackPressed();
                }
            }
        });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (isAdded()) {
                            Glide.with(this).load(selectedImageUri).circleCrop().into(profileImage);
                        }
                    }
                });

        profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        if (currentUser != null) {
            showLoading(true);
            db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                showLoading(false);
                if (!isAdded() || documentSnapshot == null) return;

                if (documentSnapshot.exists()) {
                    tvName.setText(documentSnapshot.getString("name"));
                    tvEmail.setText(documentSnapshot.getString("email"));
                    tvPhone.setText(documentSnapshot.getString("mobile"));
                    String role = documentSnapshot.getString("role");
                    tvRole.setText("seller".equals(role) ? "Seller" : "Buyer");

                    String imageUrl = documentSnapshot.getString("profilePicUrl");
                    loadProfileImage(imageUrl, profileImage);
                }
            }).addOnFailureListener(e -> {
                showLoading(false);
                if (isAdded()) Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
            });
        }

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
                Glide.with(this).load(bitmap).circleCrop().into(profileImage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Glide.with(this).load(imageUrl).circleCrop().into(profileImage);
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
                        String imageUrl = task.getResult().toString();

                        db.collection("users").document(userId)
                                .update("profilePicUrl", imageUrl)
                                .addOnSuccessListener(unused -> {
                                    showLoading(false);
                                    if (isAdded()) {
                                        loadProfileImage(imageUrl, profileImage);
                                        Toast.makeText(getContext(), "Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
                                        selectedImageUri = null;
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
}