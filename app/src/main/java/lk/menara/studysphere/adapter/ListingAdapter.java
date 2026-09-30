package lk.menara.studysphere.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import lk.menara.studysphere.R;
import lk.menara.studysphere.model.Product;

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ViewHolder> {

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
            return;
        }

        holder.name.setText(product.getName() != null ? product.getName() : "No name");
        holder.price.setText(product.getFormattedPrice() != null ? product.getFormattedPrice() : "N/A");

        if (!TextUtils.isEmpty(product.getImage())) {
            Glide.with(context)
                    .load(product.getImage())
                    .placeholder(R.drawable.add_photo_alternate_24px)
                    .error(R.drawable.add_photo_alternate_24px)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.add_photo_alternate_24px);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        TextView name, price;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.listing_item_image);
            name = itemView.findViewById(R.id.listing_item_name);
            price = itemView.findViewById(R.id.listing_item_price);
        }
    }
}