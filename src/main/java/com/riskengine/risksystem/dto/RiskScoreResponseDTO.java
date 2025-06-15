package com.riskengine.risksystem.dto;

import com.riskengine.risksystem.model.RiskLevel;
import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Data transfer object for risk assessment results
 */
@Data
@Builder
public class RiskScoreResponseDTO {
    private Long transactionId;
    private double score;
    private RiskLevel level;
    private LocalDateTime createdAt;
}