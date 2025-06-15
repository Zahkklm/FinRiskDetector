package com.riskengine.risksystem.dto;

import com.riskengine.risksystem.model.RiskLevel;
import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data transfer object for transaction operations
 */
@Data
@Builder
public class TransactionDTO {
    private Long id;
    private String userId;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String type;
    private String sourceAccountId;
    private String destinationAccountId;
    private Double riskScore;
    private RiskLevel riskLevel;
}