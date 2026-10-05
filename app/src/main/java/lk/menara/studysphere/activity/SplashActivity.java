package lk.menara.studysphere.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import lk.menara.studysphere.R;
import lk.menara.studysphere.util.NetworkUtil;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY = 3000;

    private Handler splashHandler;
    private LottieAnimationView splashLottie;
    private boolean hasNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        setupFullscreen();
        setupLottie();

        MaterialButton getStartedBtn = findViewById(R.id.btnBuy);
        getStartedBtn.setOnClickListener(v -> handleGetStartedClick());

        checkNetworkStatus();
    }

    /**
     * Makes sure the splash animation plays on real devices, even when the phone has
     * animations disabled (Battery Saver, "Remove animations", animator scale = off).
     */
    private void setupLottie() {
        splashLottie = findViewById(R.id.splashLottie);

        // Play even if the system animation scale is 0 / disabled
        splashLottie.setIgnoreDisabledSystemAnimations(true);

        // If the animation still doesn't draw on a specific phone's GPU, uncomment this line:
        // splashLottie.setRenderMode(com.airbnb.lottie.RenderMode.SOFTWARE);

        // Shows the real reason in Logcat if the animation file fails to load
        splashLottie.setFailureListener(throwable ->
                Log.e(TAG, "Lottie animation failed to load", throwable));

        splashLottie.setRepeatCount(com.airbnb.lottie.LottieDrawable.INFINITE);
        splashLottie.playAnimation();
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
        // Prevents opening two screens if the timer and the button fire together
        if (hasNavigated) return;
        hasNavigated = true;

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
    protected void onResume() {
        super.onResume();
        // Some devices pause the animation when the app is backgrounded; restart if needed
        if (splashLottie != null && !splashLottie.isAnimating()) {
            splashLottie.playAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (splashHandler != null) {
            splashHandler.removeCallbacksAndMessages(null);
        }
        if (splashLottie != null) {
            splashLottie.cancelAnimation();
        }
    }
}