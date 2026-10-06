package lk.menara.studysphere.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;
import com.jakewharton.threetenabp.AndroidThreeTen;

import lk.menara.studysphere.R;
import lk.menara.studysphere.databinding.ActivityMainBinding;
import lk.menara.studysphere.fragment.CartFragment;
import lk.menara.studysphere.fragment.CategoryFragment;
import lk.menara.studysphere.fragment.DiscoverFragment;
import lk.menara.studysphere.fragment.HomeFragment;
import lk.menara.studysphere.fragment.PaymentHistoryFragment;
import lk.menara.studysphere.fragment.ProfileFragment;
import lk.menara.studysphere.fragment.WishlistFragment;
import lk.menara.studysphere.service.MyFirebaseMessagingService;
import lk.menara.studysphere.util.SwipeGestureDetector;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnItemSelectedListener,
        SensorEventListener {

    private static final String TAG = "MainActivity";
    private static final float SHAKE_THRESHOLD_G = 2.7f;
    private static final long SHAKE_COOLDOWN_MS = 1500;

    private ActivityMainBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private ImageView headerProfilePic;
    private TextView headerUserName;
    private TextView headerUserEmail;
    private SwipeGestureDetector swipeGestureDetector;
    private boolean isNavigating = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ListenerRegistration cartListener;
    private int lastCartCount = 0;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) Log.w(TAG, "Notification permission denied");
            });

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    Glide.with(this).load(uri).circleCrop().into(headerProfilePic);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidThreeTen.init(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        setupToolbarClicks();

        MyFirebaseMessagingService.createNotificationChannel(this);
        requestNotificationPermission();

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

        initializeSwipeGestureDetector();
        updateMenuBasedOnAuthStatus();
        loadUserProfile();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateMenuBasedOnAuthStatus();
        loadUserProfile();
        listenToCartCount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeCartListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeCartListener();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
        float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
        float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;
        double gForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD_G) {
            long now = System.currentTimeMillis();
            if (now - lastShakeTime < SHAKE_COOLDOWN_MS) return;
            lastShakeTime = now;
            onShakeDetected();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void onShakeDetected() {
        if (isDestroyed() || isFinishing()) return;
        Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
        loadUserProfile();
        listenToCartCount();
        refreshCurrentFragment();
    }

    private void refreshCurrentFragment() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (current != null && !getSupportFragmentManager().isStateSaved()) {
            getSupportFragmentManager().beginTransaction()
                    .detach(current)
                    .attach(current)
                    .commit();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (swipeGestureDetector != null) {
            swipeGestureDetector.onTouch(null, ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void listenToCartCount() {
        removeCartListener();
        if (firebaseAuth.getCurrentUser() == null) {
            lastCartCount = 0;
            updateCartBadge(0);
            return;
        }
        String uid = firebaseAuth.getCurrentUser().getUid();
        cartListener = firebaseFirestore.collection("users")
                .document(uid)
                .collection("cart")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    if (isDestroyed() || isFinishing()) return;
                    updateCartBadge(snapshot.size());
                });
    }

    private void removeCartListener() {
        if (cartListener != null) {
            cartListener.remove();
            cartListener = null;
        }
    }

    private void updateCartBadge(int count) {
        if (count > 0) {
            BadgeDrawable badge = binding.bottomNavigation.getOrCreateBadge(R.id.bottom_nav_cart);
            badge.setVisible(true);
            badge.setMaxCharacterCount(3);
            badge.setNumber(count);
            badge.setBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_primary));
            badge.setBadgeTextColor(ContextCompat.getColor(this, R.color.md_theme_onPrimary));
            if (count > lastCartCount) animateCartIcon();
        } else {
            binding.bottomNavigation.removeBadge(R.id.bottom_nav_cart);
        }
        lastCartCount = count;
    }

    private void animateCartIcon() {
        View cartView = binding.bottomNavigation.findViewById(R.id.bottom_nav_cart);
        if (cartView == null) return;
        cartView.animate()
                .scaleX(1.25f)
                .scaleY(1.25f)
                .setDuration(150)
                .withEndAction(() -> cartView.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                .start();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void setupToolbarClicks() {
        View toolbarProfile = findViewById(R.id.profile_image);
        if (toolbarProfile != null) {
            toolbarProfile.setOnClickListener(v -> {
                loadUserProfile();
                binding.drawerLayout.openDrawer(GravityCompat.START);
            });
        }
        View menuIcon = findViewById(R.id.menu_icon);
        if (menuIcon != null) {
            menuIcon.setOnClickListener(v -> {
                loadUserProfile();
                binding.drawerLayout.openDrawer(GravityCompat.START);
            });
        }
    }

    private void initializeSwipeGestureDetector() {
        swipeGestureDetector = new SwipeGestureDetector(this, new SwipeGestureDetector.GestureCallback() {
            @Override
            public void onSwipeLeft() {
                navigateToNextBottomTab(true);
            }

            @Override
            public void onSwipeRight() {
                navigateToNextBottomTab(false);
            }

            @Override
            public void onSwipeUp() {
            }

            @Override
            public void onSwipeDown() {
            }

            @Override
            public void onDoubleTap() {
            }

            @Override
            public void onLongPress() {
                if (!binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    loadUserProfile();
                    binding.drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });
    }

    private void navigateToNextBottomTab(boolean isLeftSwipe) {
        if (isNavigating || binding.drawerLayout.isDrawerOpen(GravityCompat.START)) return;

        int currentId = binding.bottomNavigation.getSelectedItemId();
        int nextId = currentId;

        if (isLeftSwipe) {
            if (currentId == R.id.bottom_nav_home) {
                nextId = R.id.bottom_nav_discover;
            } else if (currentId == R.id.bottom_nav_discover) {
                nextId = R.id.bottom_nav_cart;
            } else if (currentId == R.id.bottom_nav_cart) {
                nextId = R.id.bottom_nav_profile;
            }
        } else {
            if (currentId == R.id.bottom_nav_profile) {
                nextId = R.id.bottom_nav_cart;
            } else if (currentId == R.id.bottom_nav_cart) {
                nextId = R.id.bottom_nav_discover;
            } else if (currentId == R.id.bottom_nav_discover) {
                nextId = R.id.bottom_nav_home;
            }
        }

        if (nextId != currentId) {
            isNavigating = true;
            binding.bottomNavigation.setSelectedItemId(nextId);
            handler.postDelayed(() -> isNavigating = false, 500);
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
        if (firebaseAuth.getCurrentUser() == null) return;
        String uid = firebaseAuth.getCurrentUser().getUid();
        firebaseFirestore.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (isDestroyed() || isFinishing()) return;
                    if (doc.exists() && "seller".equals(doc.getString("role"))) {
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
                    if (isDestroyed() || isFinishing()) return;
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

    public DrawerLayout getDrawerLayout() {
        return binding.drawerLayout;
    }
}