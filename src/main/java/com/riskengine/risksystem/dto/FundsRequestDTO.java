package com.riskengine.risksystem.dto;

import lombok.Data;

/**
 * Data transfer object for portfolio fund operations.
 * Used for deposit and withdrawal requests.
 */
@Data
public class FundsRequestDTO {
    private double amount;
}