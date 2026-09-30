package lk.menara.studysphere.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.jakewharton.threetenabp.AndroidThreeTen;
import lk.menara.studysphere.R;
import lk.menara.studysphere.databinding.ActivityMainBinding;
import lk.menara.studysphere.fragment.*;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnItemSelectedListener {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private ImageView headerProfilePic;
    private TextView headerUserName;
    private TextView headerUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidThreeTen.init(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        setupToolbarClicks();

        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) Log.e(TAG, "Subscription failed");
                });

        binding.navView.setNavigationItemSelectedListener(this);
        binding.bottomNavigation.setOnItemSelectedListener(this);

        View header = binding.navView.getHeaderView(0);
        headerProfilePic = header.findViewById(R.id.header_profile_pic);
        headerUserName = header.findViewById(R.id.header_user_name);
        headerUserEmail = header.findViewById(R.id.header_user_email);

        headerProfilePic.setOnClickListener(v -> openImagePicker());

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            binding.bottomNavigation.setSelectedItemId(R.id.bottom_nav_home);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
            }
        });

        updateMenuBasedOnAuthStatus();
        loadUserProfile();
    }

    private void setupToolbarClicks() {
        View toolbarProfile = findViewById(R.id.profile_image);
        if (toolbarProfile != null) {
            toolbarProfile.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        }
        View menuIcon = findViewById(R.id.menu_icon);
        if (menuIcon != null) {
            menuIcon.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        }
    }

    private void loadUserProfile() {
        ImageView toolbarImg = findViewById(R.id.profile_image);
        TextView toolbarUserText = findViewById(R.id.username_text);
        TextView toolbarGreetText = findViewById(R.id.greeting_text);

        if (firebaseAuth.getCurrentUser() == null) {
            headerUserName.setText("You're not logged in");
            headerUserEmail.setText("Join StudySphere today");
            headerProfilePic.setImageResource(R.drawable.person_24px);
            if (toolbarUserText != null) toolbarUserText.setText("Guest User");
            if (toolbarGreetText != null) toolbarGreetText.setText("Welcome");
            if (toolbarImg != null) toolbarImg.setImageResource(R.drawable.person_24px);
            return;
        }

        String uid = firebaseAuth.getCurrentUser().getUid();
        firebaseFirestore.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (isDestroyed() || isFinishing()) return;
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String imageUrl = doc.getString("profilePicUrl");

                        headerUserName.setText(name);
                        headerUserEmail.setText(email);
                        if (toolbarUserText != null) toolbarUserText.setText(name);

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this).load(imageUrl).circleCrop().into(headerProfilePic);
                            if (toolbarImg != null) Glide.with(this).load(imageUrl).circleCrop().into(toolbarImg);
                        } else {
                            headerProfilePic.setImageResource(R.drawable.person_24px);
                            if (toolbarImg != null) toolbarImg.setImageResource(R.drawable.person_24px);
                        }
                    }
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;
        boolean isSideNav = false;

        if (id == R.id.bottom_nav_home) fragment = new HomeFragment();
        else if (id == R.id.bottom_nav_discover) fragment = new DiscoverFragment();
        else if (id == R.id.bottom_nav_cart) fragment = new CartFragment();
        else if (id == R.id.bottom_nav_profile) fragment = new ProfileFragment();
        else if (id == R.id.side_nav_profile) { fragment = new ProfileFragment(); isSideNav = true; }
        else if (id == R.id.side_nav_category) { fragment = new CategoryFragment(); isSideNav = true; }
        else if (id == R.id.side_nav_wishlist) { fragment = new WishlistFragment(); isSideNav = true; }
        else if (id == R.id.side_nav_payment_history) { fragment = new PaymentHistoryFragment(); isSideNav = true; }
        else if (id == R.id.notification_icon) {
            startActivity(new Intent(this, NotificationActivity.class));
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        else if (id == R.id.side_nav_login) {
            startActivity(new Intent(this, SignInActivity.class));
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        else if (id == R.id.side_nav_logout) {
            firebaseAuth.signOut();
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        }
        else if (id == R.id.side_nav_switch_seller) {
            switchUserRole();
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if (fragment != null) {
            if (isSideNav) loadFragmentWithBackStack(fragment);
            else loadFragment(fragment);
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
    }

    private void loadFragmentWithBackStack(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).addToBackStack(null).commit();
    }

    private void updateMenuBasedOnAuthStatus() {
        boolean isSignedIn = firebaseAuth.getCurrentUser() != null;
        binding.navView.getMenu().findItem(R.id.side_nav_login).setVisible(!isSignedIn);
        binding.navView.getMenu().findItem(R.id.side_nav_logout).setVisible(isSignedIn);
        if (isSignedIn) checkUserRole();
    }

    private void checkUserRole() {
        String uid = firebaseAuth.getCurrentUser().getUid();
        firebaseFirestore.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if ("seller".equals(doc.getString("role"))) {
                        startActivity(new Intent(this, SellerMainActivity.class));
                        finish();
                    }
                });
    }

    private void switchUserRole() {
        if (firebaseAuth.getCurrentUser() == null) return;
        String uid = firebaseAuth.getCurrentUser().getUid();
        firebaseFirestore.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Intent intent = new Intent(this, SellerVerificationActivity.class);
                        intent.putExtra("phone", doc.getString("mobile"));
                        startActivity(intent);
                    }
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    Glide.with(this).load(uri).circleCrop().into(headerProfilePic);
                }
            });

    public DrawerLayout getDrawerLayout() {
        return binding.drawerLayout;
    }
}