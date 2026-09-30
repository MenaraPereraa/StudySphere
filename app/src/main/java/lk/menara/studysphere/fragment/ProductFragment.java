package lk.menara.studysphere.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth; // 🆕 Firebase Auth import කරන්න

import java.util.ArrayList;
import java.util.List;

import lk.menara.studysphere.R;
import lk.menara.studysphere.adapter.ListingAdapter;
import lk.menara.studysphere.model.Product;

public class ProductFragment extends Fragment {

    private static final String ARG_PRODUCTS = "arg_products";
    private RecyclerView recyclerViewProducts;
    private ListingAdapter listingAdapter;
    private List<Product> filteredProductsList = new ArrayList<>();

    public static ProductFragment newInstance(List<Product> products) {
        ProductFragment fragment = new ProductFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PRODUCTS, (java.io.Serializable) products);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_product, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().onBackPressed();
            }
        });

        recyclerViewProducts = view.findViewById(R.id.recycler_products);
        recyclerViewProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        if (getArguments() != null) {
            List<Product> allProducts = (List<Product>) getArguments().getSerializable(ARG_PRODUCTS);

            String currentUserId = "";
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }

            filteredProductsList.clear();
            if (allProducts != null) {
                for (Product p : allProducts) {
                    if (!p.getSellerId().equals(currentUserId)) {
                        filteredProductsList.add(p);
                    }
                }
            }
        }

        listingAdapter = new ListingAdapter(requireContext(), filteredProductsList, product -> {
            ProductDetailsFragment detailsFragment = ProductDetailsFragment.newInstance(product);
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, detailsFragment)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerViewProducts.setAdapter(listingAdapter);
    }
}