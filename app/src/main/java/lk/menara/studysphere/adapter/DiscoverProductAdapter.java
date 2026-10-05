package lk.menara.studysphere.adapter;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lk.menara.studysphere.R;
import lk.menara.studysphere.model.Product;

public class DiscoverProductAdapter extends RecyclerView.Adapter<DiscoverProductAdapter.ViewHolder> {

    private Context context;
    private List<Product> productList;
    private OnItemClickListener listener;
    private List<String> wishlistIds = new ArrayList<>();

    public interface OnItemClickListener {
        void onItemClick(Product product);
        void onAddClick(Product product);
        void onWishlistClick(Product product);
    }

    public DiscoverProductAdapter(Context context, List<Product> productList, OnItemClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    public void setWishlistIds(List<String> ids) {
        this.wishlistIds = ids;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_discover_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        if (product == null) return;

        holder.name.setText(product.getName() != null ? product.getName() : "No name");
        holder.rating.setText(String.valueOf(product.getRating()));
        holder.ratingValue.setText(String.valueOf(product.getRating()));
        holder.location.setText(getLocationFromLatLng(product.getLatitude(), product.getLongitude()));
        holder.description.setText(product.getDescription() != null ? product.getDescription() : "");

        if (!TextUtils.isEmpty(product.getImage())) {
            Glide.with(context)
                    .load(product.getImage())
                    .placeholder(R.drawable.add_photo_alternate_24px)
                    .error(R.drawable.add_photo_alternate_24px)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.add_photo_alternate_24px);
        }

        String productId = product.getId();

        if (wishlistIds != null && wishlistIds.contains(productId)) {
            holder.wishlistButton.setColorFilter(context.getColor(android.R.color.holo_red_dark));
        } else {
            holder.wishlistButton.setColorFilter(context.getColor(R.color.md_theme_onSurfaceVariant));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(product);
        });

        holder.addButton.setOnClickListener(v -> {
            if (listener != null) listener.onAddClick(product);
        });

        holder.wishlistButton.setOnClickListener(v -> {
            if (listener != null) listener.onWishlistClick(product);

            if (wishlistIds.contains(productId)) {
                wishlistIds.remove(productId);
                holder.wishlistButton.setColorFilter(context.getColor(R.color.md_theme_onSurfaceVariant));
            } else {
                wishlistIds.add(productId);
                holder.wishlistButton.setColorFilter(context.getColor(android.R.color.holo_red_dark));
            }
        });
    }

    private String getLocationFromLatLng(double lat, double lng) {
        if (lat == 0.0 && lng == 0.0) {
            return "Location not set";
        }
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Lat: " + String.format("%.4f", lat) + ", Lng: " + String.format("%.4f", lng);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void updateList(List<Product> newList) {
        this.productList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image, wishlistButton;
        TextView name, rating, ratingValue, location, description;
        MaterialButton addButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.img_product);
            name = itemView.findViewById(R.id.txt_name);
            rating = itemView.findViewById(R.id.txt_rating);
            ratingValue = itemView.findViewById(R.id.txt_rating_value);
            location = itemView.findViewById(R.id.txt_location);
            description = itemView.findViewById(R.id.txt_description);
            addButton = itemView.findViewById(R.id.btn_add);
            wishlistButton = itemView.findViewById(R.id.btn_favorite);
        }
    }
}