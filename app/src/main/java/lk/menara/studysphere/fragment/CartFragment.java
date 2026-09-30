package lk.menara.studysphere.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import lk.menara.studysphere.R;
import lk.menara.studysphere.adapter.CartAdapter;
import lk.menara.studysphere.model.CartItem;

public class CartFragment extends Fragment {

    private RecyclerView rvCart;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems = new ArrayList<>();
    private TextView tvTotal;
    private MaterialButton btnProceed;
    private ImageView backButton;
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        rvCart = view.findViewById(R.id.cart_cart_items);
        tvTotal = view.findViewById(R.id.cart_text_total);
        btnProceed = view.findViewById(R.id.cart_btn_proceed);
        backButton = view.findViewById(R.id.back_button);

        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        rvCart.setLayoutManager(new LinearLayoutManager(getContext()));
        cartAdapter = new CartAdapter(getContext(), cartItems);
        rvCart.setAdapter(cartAdapter);

        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        cartAdapter.setOnItemClickListener(new CartAdapter.OnItemClickListener() {
            @Override
            public void onPlusClick(CartItem item, int position) {
                updateQuantity(item, item.getQuantity() + 1);
            }

            @Override
            public void onMinusClick(CartItem item, int position) {
                if (item.getQuantity() > 1) {
                    updateQuantity(item, item.getQuantity() - 1);
                }
            }

            @Override
            public void onRemoveClick(CartItem item, int position) {
                removeFromCart(item);
            }

            @Override
            public void onCheckboxClick(CartItem item, int position, boolean isChecked) {
                item.setChecked(isChecked);
                updateTotal();
            }

            @Override
            public void onItemClick(CartItem item) {}
        });

        btnProceed.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(getContext(), "Cart is empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean hasSelected = false;
            for (CartItem item : cartItems) {
                if (item.isChecked()) {
                    hasSelected = true;
                    break;
                }
            }

            if (!hasSelected) {
                Toast.makeText(getContext(), "Select at least one item", Toast.LENGTH_SHORT).show();
                return;
            }

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new CheckoutFragment())
                    .addToBackStack(null)
                    .commit();
        });

        loadCartItems();
    }

    private void loadCartItems() {
        if (firebaseAuth.getCurrentUser() == null) return;
        String uid = firebaseAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).collection("cart")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;

                    cartItems.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        CartItem item = new CartItem();
                        item.setId(doc.getId());
                        item.setTitle(doc.getString("title"));

                        Double price = doc.getDouble("price");
                        item.setPrice(price != null ? price : 0.0);

                        Long qty = doc.getLong("quantity");
                        item.setQuantity(qty != null ? qty.intValue() : 1);

                        item.setImageUrl(doc.getString("imageUrl"));
                        item.setChecked(true);
                        cartItems.add(item);
                    }
                    cartAdapter.notifyDataSetChanged();
                    updateTotal();
                });
    }

    private void updateQuantity(CartItem item, int newQuantity) {
        String uid = firebaseAuth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).collection("cart")
                .document(item.getId())
                .update("quantity", newQuantity)
                .addOnSuccessListener(aVoid -> updateTotal());
    }

    private void removeFromCart(CartItem item) {
        String uid = firebaseAuth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).collection("cart")
                .document(item.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Removed from cart", Toast.LENGTH_SHORT).show();
                    updateTotal();
                });
    }

    private void updateTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            if (item.isChecked()) {
                total += item.getPrice() * item.getQuantity();
            }
        }
        tvTotal.setText("LKR " + String.format("%.2f", total));
    }
}