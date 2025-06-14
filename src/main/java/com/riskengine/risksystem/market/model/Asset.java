package com.riskengine.risksystem.market.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;

/**
 * Represents a tradable financial asset in the market
 */
@Data
@AllArgsConstructor
@Builder
public class Asset {
    /** Unique identifier for the asset */
    private final String symbol;
    
    /** Human-readable name */
    private final String name;
    
    /** Type of financial instrument */
    private final AssetType type;
    
    /** Base currency for the asset */
    private final String currency;
    
    /** Typical volatility value (0-1 scale) */
    private final double volatility;
    
    /**
     * Types of financial assets supported by the system
     */
    public enum AssetType {
        STOCK, CRYPTO, FOREX, COMMODITY
    }
}