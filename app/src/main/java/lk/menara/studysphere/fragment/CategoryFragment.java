package lk.menara.studysphere.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import lk.menara.studysphere.R;
import lk.menara.studysphere.activity.MainActivity;
import lk.menara.studysphere.adapter.CategoryAdapter;
import lk.menara.studysphere.model.Category;
import lk.menara.studysphere.model.Product;

public class CategoryFragment extends Fragment {

    private RecyclerView recyclerViewCategories;
    private CategoryAdapter categoryAdapter;
    private List<Category> categoryList;
    private View loadingLayout;
    private View categoryMainContent;

    public CategoryFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Views
        loadingLayout = view.findViewById(R.id.loadingLayout);
        categoryMainContent = view.findViewById(R.id.categoryMainContent);
        recyclerViewCategories = view.findViewById(R.id.recycler_view_categories);
        EditText searchField = view.findViewById(R.id.search_edit_text);
        ImageView backButton = view.findViewById(R.id.back_button);

        recyclerViewCategories.setLayoutManager(new GridLayoutManager(getContext(), 2));
        categoryList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(categoryList, false);
        recyclerViewCategories.setAdapter(categoryAdapter);

        backButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).getDrawerLayout().openDrawer(GravityCompat.START);
            }
        });

        setupCategoryClick();
        setupSearch(searchField);

        loadCategories();
    }

    private void showLoading(boolean isVisible) {
        if (loadingLayout != null && categoryMainContent != null) {
            loadingLayout.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            categoryMainContent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        }
    }

    private void loadCategories() {
        showLoading(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("categories").get().addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                List<Category> categories = task.getResult().toObjects(Category.class);
                categoryAdapter.updateData(categories);
            } else {
                Toast.makeText(getContext(), "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCategoryClick() {
        categoryAdapter.setOnItemClickListener(category -> {
            showLoading(true);
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("products").whereEqualTo("category", category.getName())
                    .get()
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            List<Product> products = task.getResult().toObjects(Product.class);
                            ProductFragment fragment = ProductFragment.newInstance(products);
                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.content_frame, fragment)
                                    .addToBackStack(null).commit();
                        } else {
                            Toast.makeText(getContext(), "Failed to load products", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void setupSearch(EditText searchField) {
        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                categoryAdapter.getFilter().filter(s);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }
}