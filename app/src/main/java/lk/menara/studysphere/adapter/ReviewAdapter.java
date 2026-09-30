package lk.menara.studysphere.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import lk.menara.studysphere.R;
import lk.menara.studysphere.model.Review;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<Review> reviews;

    public ReviewAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);

        holder.userName.setText(review.getUserName());
        holder.reviewText.setText(review.getComment());
        holder.ratingBar.setRating(review.getRating());
        holder.date.setText(review.getDate());

        if (holder.ratingValue != null) {
            holder.ratingValue.setText(String.valueOf(review.getRating()));
        }

    }

    private void showInitials(ReviewViewHolder holder, String name) {
        holder.userInitials.setVisibility(View.VISIBLE);
        holder.userInitials.setText(getInitials(name));
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "U";
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (initials.length() < 2 && !part.isEmpty()) {
                initials.append(part.charAt(0));
            }
        }
        return initials.toString().toUpperCase();
    }

    @Override
    public int getItemCount() {
        return reviews != null ? reviews.size() : 0;
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView userName, reviewText, date, userInitials, ratingValue;
        RatingBar ratingBar;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.review_user);
            reviewText = itemView.findViewById(R.id.review_text);
            ratingBar = itemView.findViewById(R.id.review_rating);
            date = itemView.findViewById(R.id.review_date);
            ratingValue = itemView.findViewById(R.id.review_rating_value);
        }
    }
}