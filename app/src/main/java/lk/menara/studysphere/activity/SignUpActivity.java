package lk.menara.studysphere.activity;

import android.content.Intent;
import android.os.Bundle;
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

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import lk.menara.studysphere.R;
import lk.menara.studysphere.databinding.ActivitySignUpBinding;
import lk.menara.studysphere.model.User;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private CredentialManager credentialManager;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        credentialManager = CredentialManager.create(this);

        binding.signupBtnSignin.setOnClickListener(v -> {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        });

        binding.signupBtnGoogle.setOnClickListener(v -> signUpWithGoogle());
        binding.signupBtnSignup.setOnClickListener(v -> handleEmailSignup());
    }

    private void signUpWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
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
                        runOnUiThread(() -> Toast.makeText(SignUpActivity.this, "Google Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void handleGoogleCredential(GetCredentialResponse result) {
        android.util.Log.d("GoogleAuth", "Credential Received");

        try {
            GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(result.getCredential().getData());
            String idToken = googleCredential.getIdToken();

            android.util.Log.d("GoogleAuth", "ID Token extracted successfully");

            AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);

            firebaseAuth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            android.util.Log.d("GoogleAuth", "Firebase SignIn: Success");
                            checkUserInFirestore();
                        } else {
                            android.util.Log.e("GoogleAuth", "Firebase SignIn: Failed", task.getException());
                            Toast.makeText(this, "Firebase Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

        } catch (Exception e) {
            android.util.Log.e("GoogleAuth", "Error parsing credential: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void checkUserInFirestore() {
        FirebaseUser fUser = firebaseAuth.getCurrentUser();
        if (fUser == null) return;

        firebaseFirestore.collection("users").document(fUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        navigateToMain();
                    } else {
                        showMobileNumberBottomSheet();
                    }
                });
    }

    private void showMobileNumberBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_mobile_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.setCancelable(false);

        EditText etMobile = view.findViewById(R.id.etSheetMobile);
        MaterialButton btnSave = view.findViewById(R.id.btnSheetSave);

        btnSave.setOnClickListener(v -> {
            String mobile = etMobile.getText().toString().trim();
            if (mobile.length() < 9) {
                etMobile.setError("Valid mobile required!");
                return;
            }

            FirebaseUser fUser = firebaseAuth.getCurrentUser();
            String formattedMobile = mobile.startsWith("0") ? "+94" + mobile.substring(1) : mobile;

            User user = new User(fUser.getUid(), fUser.getDisplayName(), fUser.getEmail(), formattedMobile,
                    fUser.getPhotoUrl() != null ? fUser.getPhotoUrl().toString() : null);
            user.setRole("buyer");

            saveUserToFirestore(user);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void saveUserToFirestore(User user) {
        firebaseFirestore.collection("users").document(user.getUid())
                .set(user)
                .addOnSuccessListener(unused -> navigateToMain())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void handleEmailSignup() {
        String name = binding.signupInputName.getText().toString().trim();
        String email = binding.signupInputEmail.getText().toString().trim();
        String mobile = binding.signupInputMobile.getText().toString().trim();
        String password = binding.signupInputPassword.getText().toString().trim();
        String retype = binding.signupInputRetypePassword.getText().toString().trim();

        if (name.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() || mobile.length() < 9) {
            Toast.makeText(this, "Invalid Inputs", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = task.getResult().getUser().getUid();
                        String formattedMobile = "+94" + mobile.substring(1);
                        User user = new User(uid, name, email, formattedMobile, null);
                        user.setRole("buyer");
                        saveUserToFirestore(user);
                    }
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}