package lk.menara.studysphere.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import lk.menara.studysphere.R;
import lk.menara.studysphere.activity.MainActivity;
import lk.menara.studysphere.adapter.PaymentAdapter;
import lk.menara.studysphere.model.Payment;

public class PaymentHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private PaymentAdapter adapter;
    private List<Payment> paymentList;
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private View loadingLayout, mainContent;
    private TextView tvEmptyState;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadingLayout = view.findViewById(R.id.loadingLayout);
        mainContent = view.findViewById(R.id.mainContent);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        recyclerView = view.findViewById(R.id.paymentRecyclerView);
        ImageView backButton = view.findViewById(R.id.back_button);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        paymentList = new ArrayList<>();
        adapter = new PaymentAdapter(paymentList);
        recyclerView.setAdapter(adapter);

        backButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).getDrawerLayout().openDrawer(GravityCompat.START);
            } else {
                getParentFragmentManager().popBackStack();
            }
        });

        loadPaymentHistory();
    }

    private void showLoading(boolean isVisible) {
        if (loadingLayout != null && mainContent != null) {
            loadingLayout.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            mainContent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        }
    }

    private void loadPaymentHistory() {
        if (firebaseAuth.getCurrentUser() == null) return;

        showLoading(true);
        String uid = firebaseAuth.getCurrentUser().getUid();

        db.collection("payment")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    paymentList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Payment payment = document.toObject(Payment.class);
                        paymentList.add(payment);
                    }
                    adapter.notifyDataSetChanged();
                    tvEmptyState.setVisibility(paymentList.isEmpty() ? View.VISIBLE : View.GONE);
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e("PaymentHistory", "Error", e);
                    showLoading(false);
                });
    }
}