package com.riskengine.risksystem.dto;

import lombok.Data;

/**
 * Data transfer object for risk evaluation requests
 */
@Data
public class RiskScoreRequestDTO {
    private Long transactionId;
}