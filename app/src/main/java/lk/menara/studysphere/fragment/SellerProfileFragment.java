package lk.menara.studysphere.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.menara.studysphere.R;
import lk.menara.studysphere.databinding.FragmentSellerProfileBinding;

public class SellerProfileFragment extends Fragment {

    private FragmentSellerProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSellerProfileBinding.inflate(inflater, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        binding.backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        if (binding.updateProfile != null) {
            binding.updateProfile.setVisibility(View.GONE);
        }

        loadSellerData();

        return binding.getRoot();
    }

    private void showLoading(boolean isVisible) {
        if (isAdded() && binding != null && binding.loadingLayout != null) {
            binding.loadingLayout.getRoot().setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    private void loadSellerData() {
        if (currentUser == null) return;

        showLoading(true);

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    showLoading(false);

                    if (doc.exists() && isAdded()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String mobile = doc.getString("mobile");
                        String role = doc.getString("role");
                        String imageUrl = doc.getString("profilePicUrl");

                        binding.tvName.setText(name != null ? name : "N/A");
                        binding.tvEmail.setText(email != null ? email : "N/A");
                        binding.tvPhone.setText(mobile != null ? mobile : "N/A");

                        if (role != null && !role.isEmpty()) {
                            binding.tvRole.setText(role.substring(0, 1).toUpperCase() + role.substring(1));
                        }

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_profile)
                                    .circleCrop()
                                    .into(binding.profileImage);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}