package lk.menara.studysphere.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    private String paymentId;
    private String orderId;
    private String item_id;
    private String userId;
    private double amount;
    private String paymentMethod;
    private String status;
    private String paymentDate;
    private String transactionId;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Timestamp trackingDateTime;

    @Exclude
    public Timestamp getTrackingDateTime() {
        return trackingDateTime;
    }

    @Exclude
    public void setTrackingDateTime(Timestamp trackingDateTime) {
        this.trackingDateTime = trackingDateTime;
    }

    public void setAmount(Object amount) {
        if (amount instanceof Number) {
            this.amount = ((Number) amount).doubleValue();
        } else if (amount instanceof String) {
            try {
                this.amount = Double.parseDouble((String) amount);
            } catch (Exception e) {
                this.amount = 0.0;
            }
        }
    }
}