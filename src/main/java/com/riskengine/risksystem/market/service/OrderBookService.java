package com.riskengine.risksystem.market.service;

import com.riskengine.risksystem.market.model.Order;
import com.riskengine.risksystem.market.model.AssetPrice;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages order books for all market assets
 */
@Service
@Slf4j
public class OrderBookService {
    /** Order books for each asset (symbol → orders) */
    private final Map<String, List<Order>> orderBooks = new ConcurrentHashMap<>();
    
    /** Order lookup by ID */
    private final Map<String, Order> ordersById = new ConcurrentHashMap<>();
    
    /**
     * Add an order to the appropriate order book
     * 
     * @param order Order to add
     * @return Updated order with new status
     */
    public Order addOrder(Order order) {
        // Create order book for symbol if it doesn't exist
        orderBooks.computeIfAbsent(order.getSymbol(), k -> new ArrayList<>());
        
        // Update order status
        order.setStatus(Order.OrderStatus.OPEN);
        order.setUpdatedAt(LocalDateTime.now());
        
        // Add to order book
        orderBooks.get(order.getSymbol()).add(order);
        ordersById.put(order.getId(), order);
        
        log.info("Order added to book: {}", order);
        return order;
    }
    
    /**
     * Get all open orders for a specific asset
     * 
     * @param symbol Asset symbol
     * @return List of open orders
     */
    public List<Order> getOpenOrders(String symbol) {
        return orderBooks.getOrDefault(symbol, List.of()).stream()
            .filter(o -> o.getStatus() == Order.OrderStatus.OPEN || 
                   o.getStatus() == Order.OrderStatus.PARTIALLY_FILLED)
            .toList();
    }
    
    /**
     * Get all open orders for a specific user
     * 
     * @param userId User ID
     * @return List of user's open orders
     */
    public List<Order> getUserOpenOrders(String userId) {
        return ordersById.values().stream()
            .filter(o -> o.getUserId().equals(userId) && 
                  (o.getStatus() == Order.OrderStatus.OPEN || 
                   o.getStatus() == Order.OrderStatus.PARTIALLY_FILLED))
            .toList();
    }
    
    /**
     * Cancel an order
     * 
     * @param orderId Order ID
     * @return Updated order or null if not found
     */
    public Order cancelOrder(String orderId) {
        Order order = ordersById.get(orderId);
        if (order == null) {
            return null;
        }
        
        if (order.getStatus() == Order.OrderStatus.OPEN || 
            order.getStatus() == Order.OrderStatus.PARTIALLY_FILLED) {
            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            log.info("Order cancelled: {}", order);
        }
        
        return order;
    }
    
    /**
     * Match limit orders against current market price
     * 
     * @param symbol Asset symbol
     * @param currentPrice Current market price
     * @return List of orders that were filled
     */
    public List<Order> matchLimitOrders(String symbol, AssetPrice currentPrice) {
        List<Order> filledOrders = new ArrayList<>();
        List<Order> orders = orderBooks.getOrDefault(symbol, List.of());
        
        for (Order order : orders) {
            if (order.getStatus() != Order.OrderStatus.OPEN) {
                continue;
            }
            
            if (order.getType() == Order.OrderType.LIMIT) {
                boolean shouldFill = switch (order.getSide()) {
                    case BUY -> currentPrice.getPrice() <= order.getPrice();
                    case SELL -> currentPrice.getPrice() >= order.getPrice();
                };
                
                if (shouldFill) {
                    order.setStatus(Order.OrderStatus.FILLED);
                    order.setUpdatedAt(LocalDateTime.now());
                    filledOrders.add(order);
                    
                    log.info("Filled limit order: {} at price {}", 
                             order.getId(), currentPrice.getPrice());
                }
            }
        }
        
        return filledOrders;
    }
    
    /**
     * Get a specific order by ID
     * 
     * @param orderId Order ID
     * @return Order or null if not found
     */
    public Order getOrder(String orderId) {
        return ordersById.get(orderId);
    }
}