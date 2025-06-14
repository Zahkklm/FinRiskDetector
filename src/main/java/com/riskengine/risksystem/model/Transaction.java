package com.riskengine.risksystem.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;  // Changed from Long to String
    
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal amount;
    
    @Column(name = "transaction_date")
    private LocalDateTime timestamp;
    
    @Column(name = "transaction_type")
    private String type;
    
    @Column(name = "source_account_id")
    private String sourceAccountId;
    
    @Column(name = "destination_account_id")
    private String destinationAccountId;
    
    @Column(name = "risk_score")
    private Double riskScore;
}