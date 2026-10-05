package lk.menara.studysphere.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private FloatingActionButton fabScrollTop;
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;

    private final Set<String> uncheckedIds = new HashSet<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCart = view.findViewById(R.id.cart_cart_items);
        tvTotal = view.findViewById(R.id.cart_text_total);
        btnProceed = view.findViewById(R.id.cart_btn_proceed);
        backButton = view.findViewById(R.id.back_button);
        fabScrollTop = view.findViewById(R.id.fab_scroll_top);

        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        rvCart.setLayoutManager(new LinearLayoutManager(getContext()));
        cartAdapter = new CartAdapter(getContext(), cartItems);
        rvCart.setAdapter(cartAdapter);

        rvCart.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateScrollButton();
            }
        });

        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        fabScrollTop.setOnClickListener(v -> {
            if (cartItems.isEmpty()) return;
            if (rvCart.canScrollVertically(-1)) {
                rvCart.smoothScrollToPosition(0);
            } else {
                rvCart.smoothScrollToPosition(cartItems.size() - 1);
            }
        });

        cartAdapter.setOnItemClickListener(new CartAdapter.OnItemClickListener() {
            @Override
            public void onPlusClick(CartItem item, int position) {
                updateQuantity(item, item.getQuantity() + 1);
            }

            @Override
            public void onMinusClick(CartItem item, int position) {
                if (item.getQuantity() > 1) {
                    updateQuantity(item, item.getQuantity() - 1);
                } else {
                    removeFromCart(item, view);
                }
            }

            @Override
            public void onRemoveClick(CartItem item, int position) {
                removeFromCart(item, view);
            }

            @Override
            public void onCheckboxClick(CartItem item, int position, boolean isChecked) {
                item.setChecked(isChecked);
                if (isChecked) {
                    uncheckedIds.remove(item.getId());
                } else {
                    uncheckedIds.add(item.getId());
                }
                updateTotal();
            }

            @Override
            public void onItemClick(CartItem item) {}
        });

        btnProceed.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Snackbar.make(view, "Cart is empty!", Snackbar.LENGTH_SHORT).show();
                return;
            }

            ArrayList<String> selectedIds = new ArrayList<>();
            for (CartItem item : cartItems) {
                if (item.isChecked()) {
                    selectedIds.add(item.getId());
                }
            }

            if (selectedIds.isEmpty()) {
                Snackbar.make(view, "Select at least one item", Snackbar.LENGTH_SHORT).show();
                return;
            }

            CheckoutFragment checkoutFragment = new CheckoutFragment();
            Bundle args = new Bundle();
            args.putStringArrayList("selected_ids", selectedIds);
            checkoutFragment.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, checkoutFragment)
                    .addToBackStack(null)
                    .commit();
        });

        loadCartItems();
    }

    private void updateScrollButton() {
        if (fabScrollTop == null || rvCart == null) return;
        boolean canScrollUp = rvCart.canScrollVertically(-1);
        boolean canScrollDown = rvCart.canScrollVertically(1);

        if (!canScrollUp && !canScrollDown) {
            fabScrollTop.hide();
            return;
        }

        fabScrollTop.show();
        fabScrollTop.animate().rotation(canScrollUp ? 90f : -90f).setDuration(150).start();
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
                        item.setChecked(!uncheckedIds.contains(doc.getId()));
                        cartItems.add(item);
                    }
                    cartAdapter.notifyDataSetChanged();
                    updateTotal();
                    if (rvCart != null) rvCart.post(this::updateScrollButton);
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

    private void removeFromCart(CartItem item, View parentView) {
        String uid = firebaseAuth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).collection("cart")
                .document(item.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    uncheckedIds.remove(item.getId());
                    updateTotal();

                    Snackbar.make(parentView, item.getTitle() + " removed from cart", Snackbar.LENGTH_LONG)
                            .setAction("UNDO", v -> restoreCartItem(uid, item))
                            .setAnchorView(fabScrollTop)
                            .show();
                });
    }

    private void restoreCartItem(String uid, CartItem item) {
        Map<String, Object> cartMap = new HashMap<>();
        cartMap.put("title", item.getTitle());
        cartMap.put("price", item.getPrice());
        cartMap.put("quantity", item.getQuantity());
        cartMap.put("imageUrl", item.getImageUrl());

        db.collection("users").document(uid).collection("cart")
                .document(item.getId())
                .set(cartMap);
    }

    private void updateTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            if (item.isChecked()) {
                total += item.getPrice() * item.getQuantity();
            }
        }
        tvTotal.setText(String.format("LKR %.2f", total));
    }
}