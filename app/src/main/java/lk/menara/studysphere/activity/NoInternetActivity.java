package lk.menara.studysphere.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import lk.menara.studysphere.R;
import lk.menara.studysphere.util.NetworkUtil;

public class NoInternetActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(lk.menara.studysphere.R.layout.activity_no_internet);

        findViewById(R.id.btnRetry).setOnClickListener(v -> {
            if (NetworkUtil.isNetworkAvailable(this)) {
                startActivity(new Intent(NoInternetActivity.this, SplashActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Still no internet connection!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}