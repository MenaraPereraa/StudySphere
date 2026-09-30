package lk.menara.studysphere.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import lk.menara.studysphere.R;
import lk.menara.studysphere.databinding.ActivitySignInBinding;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private CredentialManager credentialManager;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        credentialManager = CredentialManager.create(this);

        if (firebaseAuth.getCurrentUser() != null) {
            goToMain();
            return;
        }


        binding.signinBtnSignin.setOnClickListener(v -> performEmailSignIn());

        binding.googleSignInBtn.setOnClickListener(v -> signInWithGoogle());

        binding.signinForgotPassword.setOnClickListener(v -> showForgotPasswordSheet());

        binding.signinBtnSignup.setOnClickListener(v -> {
            startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
        });
    }

    private void performEmailSignIn() {
        String email = binding.signinInputEmail.getText().toString().trim();
        String password = binding.signinInputPassword.getText().toString().trim();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.signinInputEmail.setError("Valid email required");
            return;
        }
        if (password.isEmpty()) {
            binding.signinInputPassword.setError("Password required");
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        goToMain();
                    } else {
                        Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithGoogle() {
        com.google.android.libraries.identity.googleid.GetGoogleIdOption googleIdOption =
                new com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(getString(R.string.default_web_client_id))
                        .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(this, request, null, executor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        runOnUiThread(() -> handleGoogleCredential(result));
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        runOnUiThread(() -> Log.e("GoogleAuth", "Error: " + e.getMessage()));
                    }
                });
    }

    private void handleGoogleCredential(GetCredentialResponse result) {
        try {
            GoogleIdTokenCredential credentialObject = GoogleIdTokenCredential.createFrom(result.getCredential().getData());
            AuthCredential credential = GoogleAuthProvider.getCredential(credentialObject.getIdToken(), null);

            firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            checkUserInFirestore();
                        }
                    });
        } catch (Exception e) {
            Log.e("GoogleAuth", "Parsing Error", e);
        }
    }

    // --- LOGIC: Forgot Password BottomSheet ---
    private void showForgotPasswordSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_forgot_password_sheet, null);
        dialog.setContentView(view);

        EditText etEmail = view.findViewById(R.id.etSheetEmail);
        MaterialButton btnReset = view.findViewById(R.id.btnSheetReset);

        btnReset.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter valid email");
                return;
            }

            firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Reset link sent to your email!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        });
        dialog.show();
    }

    private void checkUserInFirestore() {
        String uid = firebaseAuth.getCurrentUser().getUid();
        firebaseFirestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        goToMain();
                    } else {
                        startActivity(new Intent(this, SignUpActivity.class));
                        finish();
                    }
                });
    }

    private void goToMain() {
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}