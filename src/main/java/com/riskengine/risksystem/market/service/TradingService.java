package com.riskengine.risksystem.market.service;

import com.riskengine.risksystem.market.model.*;
import com.riskengine.risksystem.market.simulation.MarketSimulator;
import com.riskengine.risksystem.service.RiskScoringService;
import com.riskengine.risksystem.model.Transaction;
import com.riskengine.risksystem.model.UserProfile;
import com.riskengine.risksystem.model.RiskScore;
import com.riskengine.risksystem.model.RiskLevel;
import com.riskengine.risksystem.repository.TransactionRepository;
import com.riskengine.risksystem.repository.UserProfileRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Core trading service that processes orders with risk assessment
 */
@Service
@Slf4j
public class TradingService {
    @Autowired
    private MarketSimulator marketSimulator;
    
    @Autowired
    private OrderBookService orderBookService;
    
    @Autowired
    private RiskScoringService riskScoringService;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    /**
     * Result of a trading operation
     */
    public record OrderResult(
        Order order,
        boolean success,
        String message
    ) {}
    
    /**
     * Place a new order with risk assessment
     * 
     * @param userId User placing the order
     * @param symbol Asset symbol
     * @param side Buy or sell
     * @param quantity Amount to trade
     * @param price Limit price (if applicable)
     * @param type Market or limit order
     * @return Result of the order placement
     */
    public OrderResult placeOrder(String userId, String symbol, 
                                 Order.OrderSide side, double quantity, 
                                 double price, Order.OrderType type) {
        log.info("Processing order request: {} {} {} at {} ({})", 
                userId, side, quantity, price, type);
        
        // Validate inputs
        if (quantity <= 0) {
            return new OrderResult(null, false, "Quantity must be positive");
        }
        
        if (type == Order.OrderType.LIMIT && price <= 0) {
            return new OrderResult(null, false, "Price must be positive for limit orders");
        }
        
        try {
            // Get current market price
            AssetPrice currentPrice = marketSimulator.getCurrentPrice(symbol);
            
            // Create order
            Order order = Order.create(userId, symbol, type, side, quantity, price);
            
            // For market orders, use current price
            double effectivePrice = (type == Order.OrderType.MARKET) 
                ? currentPrice.getPrice() 
                : price;
            
            // Create transaction record for risk assessment
            Transaction transaction = createTransactionFromOrder(
                userId, symbol, side, quantity, effectivePrice);
            
            // Get user profile for risk assessment
            UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            
            // Assess risk before executing
            RiskScore riskScore = riskScoringService.calculateRiskScore(transaction, userProfile);
            log.info("Risk assessment for order {}: {}", order.getId(), riskScore);
            
            // If high risk, reject the order
            if (riskScore.getLevel() == RiskLevel.HIGH) {
                order.setStatus(Order.OrderStatus.REJECTED);
                order.setStatusReason("High risk transaction");
                return new OrderResult(order, false, "Order rejected due to high risk score");
            }
            
            // Process order according to type
            if (type == Order.OrderType.MARKET) {
                return executeMarketOrder(order, currentPrice);
            } else {
                return placeLimitOrder(order);
            }
            
        } catch (Exception e) {
            log.error("Error processing order", e);
            return new OrderResult(null, false, "Error: " + e.getMessage());
        }
    }
    
    /**
     * Execute a market order immediately
     */
    private OrderResult executeMarketOrder(Order order, AssetPrice currentPrice) {
        // Record the transaction
        Transaction transaction = createTransactionFromOrder(
            order.getUserId(), 
            order.getSymbol(), 
            order.getSide(),
            order.getQuantity(), 
            currentPrice.getPrice()
        );
        transactionRepository.save(transaction);
        
        // Update order status
        order.setStatus(Order.OrderStatus.FILLED);
        order.setUpdatedAt(LocalDateTime.now());
        
        log.info("Market order executed: {} at price {}", 
                order.getId(), currentPrice.getPrice());
        
        return new OrderResult(order, true, "Market order executed successfully");
    }
    
    /**
     * Place a limit order in the order book
     */
    private OrderResult placeLimitOrder(Order order) {
        // Add to order book
        orderBookService.addOrder(order);
        
        return new OrderResult(order, true, "Limit order placed successfully");
    }
    
    /**
     * Create a transaction record from order details
     */
    private Transaction createTransactionFromOrder(
            String userId, String symbol, Order.OrderSide side, 
            double quantity, double price) {
        
        return Transaction.builder()
            .userId(userId)
            .amount(BigDecimal.valueOf(quantity * price))
            .timestamp(LocalDateTime.now())
            .type("TRADE_" + side.name())
            .sourceAccountId(side == Order.OrderSide.BUY ? userId : "EXCHANGE")
            .destinationAccountId(side == Order.OrderSide.BUY ? "EXCHANGE" : userId)
            .build();
    }
    
    /**
     * Get all open orders for a user
     */
    public List<Order> getUserOpenOrders(String userId) {
        return orderBookService.getUserOpenOrders(userId);
    }
    
    /**
     * Cancel an order
     */
    public OrderResult cancelOrder(String userId, String orderId) {
        Order order = orderBookService.getOrder(orderId);
        
        if (order == null) {
            return new OrderResult(null, false, "Order not found");
        }
        
        if (!order.getUserId().equals(userId)) {
            return new OrderResult(order, false, "Not authorized to cancel this order");
        }
        
        Order cancelledOrder = orderBookService.cancelOrder(orderId);
        
        if (cancelledOrder != null && 
            cancelledOrder.getStatus() == Order.OrderStatus.CANCELLED) {
            return new OrderResult(cancelledOrder, true, "Order cancelled successfully");
        } else {
            return new OrderResult(order, false, "Could not cancel order");
        }
    }
    
    /**
     * Process all pending limit orders against current market prices
     * This would typically be called on a schedule
     */
    public void processLimitOrders() {
        log.info("Processing pending limit orders");
        
        Map<String, AssetPrice> prices = marketSimulator.getAllPrices();
        int totalFilled = 0;
        
        for (String symbol : prices.keySet()) {
            AssetPrice price = prices.get(symbol);
            
            List<Order> filledOrders = orderBookService.matchLimitOrders(symbol, price);
            totalFilled += filledOrders.size();
            
            // Record transactions for filled orders
            for (Order order : filledOrders) {
                Transaction transaction = createTransactionFromOrder(
                    order.getUserId(),
                    order.getSymbol(),
                    order.getSide(),
                    order.getQuantity(),
                    price.getPrice()
                );
                transactionRepository.save(transaction);
            }
        }
        
        log.info("Completed limit order processing: {} orders filled", totalFilled);
    }
}