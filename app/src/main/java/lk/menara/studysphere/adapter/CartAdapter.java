package lk.menara.studysphere.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import lk.menara.studysphere.R;
import lk.menara.studysphere.model.CartItem;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private Context context;
    private List<CartItem> cartList;
    private OnItemClickListener listener;

    public CartAdapter(Context context, List<CartItem> cartList) {
        this.context = context;
        this.cartList = cartList;
    }

    public interface OnItemClickListener {
        void onItemClick(CartItem item);
        void onPlusClick(CartItem item, int position);
        void onMinusClick(CartItem item, int position);
        void onRemoveClick(CartItem item, int position);
        void onCheckboxClick(CartItem item, int position, boolean isChecked);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        CartItem item = cartList.get(position);

        holder.title.setText(item.getTitle() != null ? item.getTitle() : "No Title");
        holder.price.setText(String.format("LKR %.2f", item.getPrice()));
        holder.quantity.setText(String.valueOf(item.getQuantity()));

        holder.checkbox.setChecked(item.isChecked());
        holder.btnMinus.setEnabled(item.getQuantity() > 1);

        if (item.getImageUrl() != null && item.getImageUrl().startsWith("data:image")) {
            try {
                String base64 = item.getImageUrl().split(",")[1];
                byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.image.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.image.setImageResource(R.drawable.default_profile);
            }
        } else {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.default_profile)
                    .into(holder.image);
        }

        holder.btnPlus.setOnClickListener(v -> {
            if (listener != null) listener.onPlusClick(item, position);
        });

        holder.btnMinus.setOnClickListener(v -> {
            if (listener != null) listener.onMinusClick(item, position);
        });

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) listener.onRemoveClick(item, position);
        });

        holder.checkbox.setOnClickListener(v -> {
            if (listener != null)
                listener.onCheckboxClick(item, position, holder.checkbox.isChecked());
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return cartList != null ? cartList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        CheckBox checkbox;
        ImageView image, btnRemove;
        TextView title, price, quantity;
        AppCompatButton btnPlus, btnMinus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            checkbox = itemView.findViewById(R.id.item_cart_checkbox);
            image = itemView.findViewById(R.id.item_cart_image);
            btnRemove = itemView.findViewById(R.id.item_cart_remove);
            title = itemView.findViewById(R.id.item_cart_title);
            price = itemView.findViewById(R.id.item_cart_price);
            quantity = itemView.findViewById(R.id.item_cart_quantity);
            btnPlus = itemView.findViewById(R.id.item_cart_btn_plus);
            btnMinus = itemView.findViewById(R.id.item_cart_btn_minus);
        }
    }
}