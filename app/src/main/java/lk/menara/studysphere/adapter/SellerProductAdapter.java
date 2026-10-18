package lk.menara.studysphere.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import lk.menara.studysphere.R;
import lk.menara.studysphere.model.Product;

public class SellerProductAdapter extends RecyclerView.Adapter<SellerProductAdapter.ViewHolder> implements Filterable {

    private static final int LOW_STOCK_LIMIT = 5;

    private List<Product> productList;
    private List<Product> productListFull;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public SellerProductAdapter(List<Product> productList, OnItemClickListener listener) {
        this.productList = productList;
        this.listener = listener;
        this.productListFull = new ArrayList<>(productList); // Initialize full list
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seller_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.name.setText(product.getName());
        holder.category.setText(product.getCategory());

        // Price + stock status
        int qty = product.getAvailableQty();
        if (qty <= 0) {
            holder.price.setText(product.getFormattedPrice() + " • Out of stock");
        } else if (qty < LOW_STOCK_LIMIT) {
            holder.price.setText(product.getFormattedPrice() + " • Low stock");
        } else {
            holder.price.setText(product.getFormattedPrice());
        }

        // Glide handles the image URL from your Product model
        Glide.with(holder.itemView.getContext())
                .load(product.getImage())
                .placeholder(R.drawable.default_profile)
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(product));
    }

    @Override
    public int getItemCount() { return productList.size(); }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Product> filteredList = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    // No filter applied, return the full list
                    filteredList.addAll(productListFull);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    // Apply the filter to the full product list
                    for (Product product : productListFull) {
                        if (product.getName().toLowerCase().contains(filterPattern) ||
                                product.getCategory().toLowerCase().contains(filterPattern) ||
                                product.getDescription().toLowerCase().contains(filterPattern)) {
                            filteredList.add(product);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                productList.clear();
                productList.addAll((List<Product>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    public void updateFullList(List<Product> fullList) {
        this.productListFull = new ArrayList<>(fullList);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, category, price;
        ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_product_name);
            category = itemView.findViewById(R.id.tv_product_category);
            price = itemView.findViewById(R.id.tv_product_price);
            image = itemView.findViewById(R.id.iv_product_image);
        }
    }
}