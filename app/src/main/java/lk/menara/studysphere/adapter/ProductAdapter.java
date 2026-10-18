package lk.menara.studysphere.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import lk.menara.studysphere.R;
import lk.menara.studysphere.model.Product;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private final List<Product> products;
    private final OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(List<Product> products, OnProductClickListener listener) {
        this.products = products != null ? products : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        if (product == null) return;

        // Set product name
        holder.productName.setText(product.getName() != null ? product.getName() : "No name");

        // Set rating
        holder.ratingBar.setRating(product.getRating());
        holder.ratingText.setText(String.valueOf(product.getRating()));

        // Load image using Glide
        loadProductImage(holder.productImage, product.getImage());

        // Price / stock status + click handling
        if (product.getAvailableQty() <= 0) {
            // Out of stock: dim the row and disable the click
            holder.productPrice.setText("Out of stock");
            holder.itemView.setAlpha(0.5f);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
        } else {
            holder.productPrice.setText("LKR ".concat(String.valueOf(product.getPrice())));
            holder.itemView.setAlpha(1f);
            holder.itemView.setClickable(true);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product);
            });
        }
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    private void loadProductImage(ImageView imageView, String imageUrl) {
        if (TextUtils.isEmpty(imageUrl)) {
            imageView.setImageResource(R.drawable.add_photo_alternate_24px);
            return;
        }

        Glide.with(imageView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.add_photo_alternate_24px)
                .error(R.drawable.add_photo_alternate_24px)
                .into(imageView);
    }

    public void updateData(List<Product> newProducts) {
        products.clear();
        if (newProducts != null) {
            products.addAll(newProducts);
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName;
        TextView productPrice;
        RatingBar ratingBar;
        TextView ratingText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            ratingText = itemView.findViewById(R.id.ratingText);
        }
    }
}