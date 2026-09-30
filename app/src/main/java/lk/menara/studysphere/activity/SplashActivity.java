package lk.menara.studysphere.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import lk.menara.studysphere.R;
import lk.menara.studysphere.util.NetworkUtil;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3000;
    private Handler splashHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        setupFullscreen();

        checkNetworkStatus();

        MaterialButton getStartedBtn = findViewById(R.id.btnBuy);
        getStartedBtn.setOnClickListener(v -> handleGetStartedClick());
    }

    private void checkNetworkStatus() {
        if (!NetworkUtil.isNetworkAvailable(this)) {
            navigateTo(NoInternetActivity.class);
        } else {
            splashHandler = new Handler(Looper.getMainLooper());
            splashHandler.postDelayed(this::checkAuthAndProceed, SPLASH_DELAY);
        }
    }

    private void handleGetStartedClick() {
        if (splashHandler != null) {
            splashHandler.removeCallbacksAndMessages(null);
        }

        if (NetworkUtil.isNetworkAvailable(this)) {
            checkAuthAndProceed();
        } else {
            navigateTo(NoInternetActivity.class);
        }
    }

    private void checkAuthAndProceed() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean isSeller = prefs.getBoolean("isSeller", false);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            navigateTo(isSeller ? SellerMainActivity.class : MainActivity.class);
        } else {
            navigateTo(SignInActivity.class);
        }
    }

    private void navigateTo(Class<?> destination) {
        Intent intent = new Intent(SplashActivity.this, destination);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void setupFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (splashHandler != null) {
            splashHandler.removeCallbacksAndMessages(null);
        }
    }
}