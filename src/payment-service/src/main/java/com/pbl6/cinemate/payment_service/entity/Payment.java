package com.pbl6.cinemate.payment_service.entity;

import com.pbl6.cinemate.payment_service.enums.PaymentMethod;
import com.pbl6.cinemate.payment_service.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "user_email", length = 255)
    private String userEmail;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;
    
    @Column(name = "transaction_id", unique = true)
    private String transactionId;
    
    @Column(name = "vnp_txn_ref", length = 100, unique = true)
    private String vnpTxnRef;
    
    @Column(name = "vnp_transaction_no", length = 100)
    private String vnpTransactionNo;
    
    @Column(name = "vnp_bank_code", length = 20)
    private String vnpBankCode;
    
    @Column(name = "vnp_card_type", length = 20)
    private String vnpCardType;
    
    @Column(name = "vnp_order_info", columnDefinition = "TEXT")
    private String vnpOrderInfo;
    
    @Column(name = "vnp_pay_date", length = 14)
    private String vnpPayDate;
    
    @Column(name = "vnp_response_code", length = 2)
    private String vnpResponseCode;
    
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
