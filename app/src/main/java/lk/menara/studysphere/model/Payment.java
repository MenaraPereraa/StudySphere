package lk.menara.studysphere.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}