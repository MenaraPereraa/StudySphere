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
import lk.menara.studysphere.model.Category;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> implements Filterable {

    private List<Category> categoryList;
    private List<Category> categoryListFull;
    private OnItemClickListener listener;
    private boolean isHome;

    public CategoryAdapter(List<Category> categoryList, boolean isHome) {
        this.categoryList = categoryList;
        this.categoryListFull = new ArrayList<>(categoryList);
        this.isHome = isHome;
    }

    public interface OnItemClickListener {
        void onItemClick(Category category);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isHome ? R.layout.item_category_home : R.layout.item_category;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categoryList.get(position);

        holder.categoryName.setText(category.getName());

        if (holder.categoryDescription != null) {
            holder.categoryDescription.setText(category.getDescription());
        }

        if (category.getImageUrl() != null && !category.getImageUrl().isEmpty()) {
            var glideRequest = Glide.with(holder.itemView.getContext())
                    .load(category.getImageUrl())
                    .placeholder(R.drawable.category_24px)
                    .error(R.drawable.category_24px);

            if (isHome) {
                glideRequest.circleCrop().into(holder.categoryImage);
            } else {
                glideRequest.centerCrop().into(holder.categoryImage);
            }
        } else {
            holder.categoryImage.setImageResource(R.drawable.category_24px);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(category);
        });
    }


    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public void updateData(List<Category> newList) {
        categoryList.clear();
        categoryList.addAll(newList);
        categoryListFull.clear();
        categoryListFull.addAll(newList);
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return categoryFilter;
    }

    private final Filter categoryFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Category> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(categoryListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Category item : categoryListFull) {
                    if (item.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            categoryList.clear();
            categoryList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryImage;
        TextView categoryName, categoryDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryImage = itemView.findViewById(R.id.category_image);
            categoryName = itemView.findViewById(R.id.category_name);
            categoryDescription = itemView.findViewById(R.id.category_description);
        }
    }
}