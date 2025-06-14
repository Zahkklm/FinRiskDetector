package com.riskengine.risksystem.market.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a trading order in the system
 */
@Data
@Builder
public class Order {
    /** Unique order identifier */
    private final String id;
    
    /** User who placed the order */
    private final String userId;
    
    /** Asset being traded */
    private final String symbol;
    
    /** Market or limit order */
    private final OrderType type;
    
    /** Buy or sell direction */
    private final OrderSide side;
    
    /** Amount of the asset to trade */
    private final double quantity;
    
    /** Target price for limit orders */
    private final double price;
    
    /** Current status of the order */
    private OrderStatus status;
    
    /** When the order was placed */
    private final LocalDateTime createdAt;
    
    /** When the order was last updated */
    private LocalDateTime updatedAt;
    
    /** Reason for rejection if applicable */
    private String statusReason;
    
    /**
     * Types of orders supported by the system
     */
    public enum OrderType {
        /** Execute immediately at market price */
        MARKET,
        
        /** Execute only at specified price or better */
        LIMIT
    }
    
    /**
     * Possible order directions
     */
    public enum OrderSide {
        BUY, SELL
    }
    
    /**
     * Possible states of an order
     */
    public enum OrderStatus {
        PENDING,    // Initial state
        OPEN,       // Active in the order book
        PARTIALLY_FILLED, // Some quantity executed
        FILLED,     // Completely executed
        CANCELLED,  // Cancelled by user
        REJECTED    // Rejected by system
    }
    
    /**
     * Factory method to create a new order with default values
     */
    public static Order create(String userId, String symbol, OrderType type, 
                              OrderSide side, double quantity, double price) {
        return Order.builder()
            .id(UUID.randomUUID().toString())
            .userId(userId)
            .symbol(symbol)
            .type(type)
            .side(side)
            .quantity(quantity)
            .price(price)
            .status(OrderStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}