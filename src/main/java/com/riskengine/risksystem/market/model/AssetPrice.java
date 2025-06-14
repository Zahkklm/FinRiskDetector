package com.riskengine.risksystem.market.model;

import lombok.Value;
import java.time.LocalDateTime;

/**
 * Immutable snapshot of an asset's price at a specific moment in time
 */
@Value
public class AssetPrice {
    /** Asset symbol */
    String symbol;
    
    /** Current price in base currency */
    double price;
    
    /** Price after all fees (used for order execution) */
    double netPrice;
    
    /** Lowest price within the current time window */
    double low;
    
    /** Highest price within the current time window */
    double high;
    
    /** Trading volume within the current time window */
    double volume;
    
    /** When this price was recorded */
    LocalDateTime timestamp;
}