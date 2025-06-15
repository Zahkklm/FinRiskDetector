package com.riskengine.risksystem.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data transfer object for direct transaction risk evaluation
 */
@Data
public class TransactionRiskRequestDTO {
    private String userId;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String type;
    private String sourceAccountId;
    private String destinationAccountId;
}