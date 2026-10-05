package lk.menara.studysphere.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.menara.studysphere.R;
import lk.menara.studysphere.databinding.ActivitySellerMainBinding;
import lk.menara.studysphere.fragment.SellerHomeFragment;
import lk.menara.studysphere.fragment.SellerItemsFragment;
import lk.menara.studysphere.fragment.SellerOrdersFragment;
import lk.menara.studysphere.fragment.SellerProfileFragment;
import lk.menara.studysphere.util.ConnectivityBatteryReceiver;
import lk.menara.studysphere.listener.ConnectivityListener;

public class SellerMainActivity extends AppCompatActivity
        implements BottomNavigationView.OnItemSelectedListener {

    private ActivitySellerMainBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen setup
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        binding = ActivitySellerMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        loadUserDetails();

        // Buyer Mode Button Listener
        binding.btnSwitchBuyer.setOnClickListener(v -> confirmSwitchToBuyer());

        // Bottom Navigation Listener
        binding.bottomNavigation.setOnItemSelectedListener(this);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmSwitchToBuyer();
            }
        });

        if (savedInstanceState == null) {
            loadFragment(new SellerHomeFragment());
            binding.bottomNavigation.getMenu().findItem(R.id.bottom_nav_seller_home).setChecked(true);
        }
    }

    // NEW: Connectivity and Battery receiver fields
    private ConnectivityBatteryReceiver connectivityBatteryReceiver;

    @Override
    protected void onStart() {
        super.onStart();
        // NEW: Register connectivity and battery receiver
        connectivityBatteryReceiver = new ConnectivityBatteryReceiver(
                new ConnectivityListener() {
                    @Override
                    public void onNetworkConnected() {}

                    @Override
                    public void onNetworkDisconnected() {}

                    @Override
                    public void onBatteryLow() {}

                    @Override
                    public void onBatteryOkay() {}

                    @Override
                    public void onPowerConnected() {}

                    @Override
                    public void onPowerDisconnected() {}
                });
        ConnectivityBatteryReceiver.register(this, connectivityBatteryReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // NEW: Unregister connectivity and battery receiver
        if (connectivityBatteryReceiver != null) {
            ConnectivityBatteryReceiver.unregister(this, connectivityBatteryReceiver);
        }
    }

    private void loadUserDetails() {
        if (firebaseAuth.getCurrentUser() == null) return;
        String uid = firebaseAuth.getUid();

        firebaseFirestore.collection("users").document(uid)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) return;

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String imageUrl = documentSnapshot.getString("profilePicUrl");
                        String userName = documentSnapshot.getString("name");

                        binding.sellerName.setText(userName != null ? userName : "Seller Account");

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .circleCrop()
                                    .into(binding.profileImage);
                        } else {
                            binding.profileImage.setImageResource(R.drawable.default_profile);
                        }
                    }
                });
    }

    private void confirmSwitchToBuyer() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Switch to Buyer?")
                .setMessage("Are you sure you want to go back to Buyer mode?")
                .setPositiveButton("Switch", (dialog, which) -> switchToBuyer())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void switchToBuyer() {
        String uid = firebaseAuth.getUid();
        if (uid == null) return;

        firebaseFirestore.collection("users").document(uid)
                .update("role", "buyer")
                .addOnCompleteListener(task -> {
                    Intent intent = new Intent(SellerMainActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Offline: Role updated locally", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        int id = item.getItemId();

        if (id == R.id.bottom_nav_seller_home) fragment = new SellerHomeFragment();
        else if (id == R.id.bottom_nav_seller_items) fragment = new SellerItemsFragment();
        else if (id == R.id.bottom_nav_seller_orders) fragment = new SellerOrdersFragment();
        else if (id == R.id.bottom_nav_seller_profile) fragment = new SellerProfileFragment();

        if (fragment != null) loadFragment(fragment);
        return true;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }
}