package lk.menara.studysphere.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.menara.studysphere.R;
import lk.menara.studysphere.databinding.ItemNotificationBinding;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Map<String, Object>> notifications;

    public NotificationAdapter(List<Map<String, Object>> notifications) {
        this.notifications = (notifications != null) ? notifications : new ArrayList<>();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NotificationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Map<String, Object> notificationData = notifications.get(position);
        holder.bind(notificationData);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void setNotifications(List<Map<String, Object>> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ItemNotificationBinding binding;

        public NotificationViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Map<String, Object> data) {
            String title = String.valueOf(data.getOrDefault("title", "StudySphere"));
            String message = String.valueOf(data.getOrDefault("message", ""));
            String type = String.valueOf(data.getOrDefault("type", "Announcement"));

            binding.notificationTitle.setText(title);
            binding.notificationMessage.setText(message);

            Object timestampObj = data.get("timestamp");
            if (timestampObj instanceof Long) {
                long timeInMillis = (long) timestampObj;
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                binding.notificationTime.setText(sdf.format(new Date(timeInMillis)));
            }

            int iconResId = getIconForType(type);
            binding.notificationIcon.setImageResource(iconResId);

            binding.getRoot().setBackgroundColor(
                    ContextCompat.getColor(binding.getRoot().getContext(),
                            android.R.color.transparent));
        }

        private int getIconForType(String type) {
            if (type == null) return R.drawable.notifications_24px;

            switch (type) {
                case "Announcement":
                    return R.drawable.notifications_24px;
                case "Alert":
                    return R.drawable.notifications_24px;
                default:
                    return R.drawable.notifications_24px;
            }
        }
    }
}