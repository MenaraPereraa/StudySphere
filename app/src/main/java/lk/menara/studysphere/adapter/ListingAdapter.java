package lk.menara.studysphere.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import lk.menara.studysphere.R;
import lk.menara.studysphere.model.Product;

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ViewHolder> {

    private static final int LOW_STOCK_LIMIT = 5;

    private Context context;
    private List<Product> productList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public ListingAdapter(Context context, List<Product> productList, OnItemClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Product product = productList.get(position);

        if (product == null) {
            holder.name.setText("Unknown");
            holder.price.setText("N/A");
            holder.image.setImageResource(R.drawable.add_photo_alternate_24px);
            holder.itemView.setEnabled(false);
            holder.itemView.setClickable(false);
            return;
        }

        holder.name.setText(product.getName() != null ? product.getName() : "No name");

        String priceText = product.getFormattedPrice() != null ? product.getFormattedPrice() : "N/A";
        int qty = product.getAvailableQty();
        int red = ContextCompat.getColor(context, android.R.color.holo_red_dark);

        // Reset first, because RecyclerView reuses rows
        holder.price.setTextColor(holder.defaultPriceColors);
        holder.itemView.setAlpha(1f);
        holder.itemView.setEnabled(true);
        holder.itemView.setClickable(true);

        if (qty <= 0) {
            // Out of stock: dim the row and disable the click
            holder.price.setText("Out of stock");
            holder.price.setTextColor(red);
            holder.itemView.setAlpha(0.5f);
            holder.itemView.setEnabled(false);
            holder.itemView.setClickable(false);
            holder.itemView.setOnClickListener(null);
        } else {
            if (qty < LOW_STOCK_LIMIT) {
                holder.price.setText(priceText + " • Low stock");
                holder.price.setTextColor(red);
            } else {
                holder.price.setText(priceText);
            }
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(product);
                }
            });
        }

        if (!TextUtils.isEmpty(product.getImage())) {
            Glide.with(context)
                    .load(product.getImage())
                    .placeholder(R.drawable.add_photo_alternate_24px)
                    .error(R.drawable.add_photo_alternate_24px)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.add_photo_alternate_24px);
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        TextView name, price;
        ColorStateList defaultPriceColors;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.listing_item_image);
            name = itemView.findViewById(R.id.listing_item_name);
            price = itemView.findViewById(R.id.listing_item_price);
            defaultPriceColors = price.getTextColors();
        }
    }
}