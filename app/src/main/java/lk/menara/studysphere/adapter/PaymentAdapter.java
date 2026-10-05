package lk.menara.studysphere.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.util.List;
import java.util.Locale;

import lk.menara.studysphere.R;
import lk.menara.studysphere.model.Payment;
import lk.menara.studysphere.util.DateTimePickerHelper;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.ViewHolder> {

    private final List<Payment> paymentList;

    public PaymentAdapter(List<Payment> paymentList) {
        this.paymentList = paymentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.payment_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Payment payment = paymentList.get(position);

        String paymentId = payment.getPaymentId() != null
                ? payment.getPaymentId().replace("-", "\u2011")
                : "N/A";
        holder.title.setText("Payment ID: " + paymentId);

        String date = payment.getPaymentDate();
        if (date != null && date.length() >= 10) {
            holder.location.setText(date.substring(0, 10));
        } else {
            holder.location.setText(date != null ? date : "No Date");
        }

        holder.amount.setText(String.format(Locale.US, "LKR %.2f", payment.getAmount()));
        holder.tickets.setText("TXN: " + (payment.getTransactionId() != null ? payment.getTransactionId() : "N/A"));
        holder.status.setText(payment.getStatus() != null ? payment.getStatus() : "Pending");

        String method = payment.getPaymentMethod();
        if (method != null && method.length() >= 2) {
            holder.icon.setText(method.substring(0, 2).toUpperCase(Locale.ROOT));
        } else {
            holder.icon.setText("PY");
        }

        Timestamp trackingTime = payment.getTrackingDateTime();
        if (trackingTime != null) {
            holder.tracking.setText("Pickup: " + DateTimePickerHelper.formatDateTime(trackingTime));
            holder.tracking.setVisibility(View.VISIBLE);
        } else {
            holder.tracking.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return paymentList != null ? paymentList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, location, amount, tickets, status, icon, tracking;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.titleText);
            location = itemView.findViewById(R.id.locationDateText);
            amount = itemView.findViewById(R.id.amountText);
            tickets = itemView.findViewById(R.id.ticketCountText);
            status = itemView.findViewById(R.id.statusText);
            icon = itemView.findViewById(R.id.iconText);
            tracking = itemView.findViewById(R.id.trackingText);
        }
    }
}