package com.riskengine.risksystem.controller;

import com.riskengine.risksystem.market.model.*;
import com.riskengine.risksystem.market.service.*;
import com.riskengine.risksystem.market.simulation.MarketSimulator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for market data and trading operations
 */
@RestController
@RequestMapping("/api/market")
public class MarketController {
    @Autowired
    private MarketSimulator marketSimulator;
    
    @Autowired
    private TradingService tradingService;
    
    @Autowired
    private PortfolioService portfolioService;
    
    /**
     * Get all available assets in the market
     */
    @GetMapping("/assets")
    public ResponseEntity<List<Asset>> getAssets() {
        return ResponseEntity.ok(marketSimulator.getAvailableAssets());
    }
    
    /**
     * Get current price for a specific asset
     */
    @GetMapping("/price/{symbol}")
    public ResponseEntity<AssetPrice> getPrice(@PathVariable String symbol) {
        try {
            return ResponseEntity.ok(marketSimulator.getCurrentPrice(symbol));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get all current market prices
     */
    @GetMapping("/prices")
    public ResponseEntity<Map<String, AssetPrice>> getAllPrices() {
        return ResponseEntity.ok(marketSimulator.getAllPrices());
    }
    
    /**
     * Get price history for a specific asset
     */
    @GetMapping("/history/{symbol}")
    public ResponseEntity<List<AssetPrice>> getPriceHistory(@PathVariable String symbol) {
        try {
            return ResponseEntity.ok(marketSimulator.getPriceHistory(symbol));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Request data for order placement
     */
    @Data
    public static class OrderRequest {
        private String userId;
        private String symbol;
        private Order.OrderSide side;
        private double quantity;
        private double price;
        private Order.OrderType type;
    }
    
    /**
     * Place a new order
     */
    @PostMapping("/order")
    public ResponseEntity<TradingService.OrderResult> placeOrder(@RequestBody OrderRequest request) {
        TradingService.OrderResult result = tradingService.placeOrder(
            request.getUserId(),
            request.getSymbol(),
            request.getSide(),
            request.getQuantity(),
            request.getPrice(),
            request.getType()
        );
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get open orders for a user
     */
    @GetMapping("/orders/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable String userId) {
        return ResponseEntity.ok(tradingService.getUserOpenOrders(userId));
    }
    
    /**
     * Cancel an order
     */
    @DeleteMapping("/order/{userId}/{orderId}")
    public ResponseEntity<TradingService.OrderResult> cancelOrder(
            @PathVariable String userId, 
            @PathVariable String orderId) {
        TradingService.OrderResult result = tradingService.cancelOrder(userId, orderId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get user portfolio
     */
    @GetMapping("/portfolio/{userId}")
    public ResponseEntity<Portfolio> getPortfolio(@PathVariable String userId) {
        return ResponseEntity.ok(portfolioService.getPortfolio(userId));
    }
    
    /**
     * Get portfolio value
     */
    @GetMapping("/portfolio/{userId}/value")
    public ResponseEntity<Double> getPortfolioValue(@PathVariable String userId) {
        return ResponseEntity.ok(portfolioService.getPortfolioValue(userId));
    }
    
    /**
     * Deposit funds request
     */
    @Data
    public static class FundsRequest {
        private double amount;
    }
    
    /**
     * Deposit funds to portfolio
     */
    @PostMapping("/portfolio/{userId}/deposit")
    public ResponseEntity<?> depositFunds(
            @PathVariable String userId, 
            @RequestBody FundsRequest request) {
        try {
            portfolioService.depositFunds(userId, request.getAmount());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Withdraw funds from portfolio
     */
    @PostMapping("/portfolio/{userId}/withdraw")
    public ResponseEntity<?> withdrawFunds(
            @PathVariable String userId, 
            @RequestBody FundsRequest request) {
        try {
            boolean success = portfolioService.withdrawFunds(userId, request.getAmount());
            if (success) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().body("Insufficient funds");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}