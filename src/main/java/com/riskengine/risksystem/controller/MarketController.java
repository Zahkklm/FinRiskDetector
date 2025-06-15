package com.riskengine.risksystem.controller;

import com.riskengine.risksystem.dto.FundsRequestDTO;
import com.riskengine.risksystem.dto.OrderRequestDTO;
import com.riskengine.risksystem.market.model.*;
import com.riskengine.risksystem.market.service.*;
import com.riskengine.risksystem.market.simulation.MarketSimulator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for market data and trading operations.
 * Provides endpoints for asset information, price data, trading, and portfolio management.
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
     * 
     * @return List of all tradable assets with their details
     */
    @GetMapping("/assets")
    public ResponseEntity<List<Asset>> getAssets() {
        return ResponseEntity.ok(marketSimulator.getAvailableAssets());
    }
    
    /**
     * Get current price for a specific asset
     * 
     * @param symbol The unique identifier for the asset (e.g., "BTC-USD")
     * @return Current price information for the requested asset
     * @throws 404 Not Found if the symbol doesn't exist
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
     * 
     * @return Map of all asset symbols to their current price information
     */
    @GetMapping("/prices")
    public ResponseEntity<Map<String, AssetPrice>> getAllPrices() {
        return ResponseEntity.ok(marketSimulator.getAllPrices());
    }
    
    /**
     * Get price history for a specific asset
     * 
     * @param symbol The unique identifier for the asset (e.g., "BTC-USD")
     * @return List of historical prices with timestamps
     * @throws 404 Not Found if the symbol doesn't exist
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
     * Place a new order
     * 
     * @param request Order details including user ID, asset symbol, side (buy/sell),
     *                quantity, price, and order type (market/limit)
     * @return OrderResult containing success status, order ID, and message
     * @throws 400 Bad Request if order parameters are invalid
     * @throws 403 Forbidden if the order is rejected due to high risk
     */
    @PostMapping("/order")
    public ResponseEntity<TradingService.OrderResult> placeOrder(@RequestBody OrderRequestDTO request) {
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
     * 
     * @param userId The unique identifier for the user
     * @return List of the user's open orders
     */
    @GetMapping("/orders/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable String userId) {
        return ResponseEntity.ok(tradingService.getUserOpenOrders(userId));
    }
    
    /**
     * Cancel an order
     * 
     * @param userId The unique identifier for the user
     * @param orderId The unique identifier for the order to cancel
     * @return OrderResult containing success status and message
     * @throws 404 Not Found if the order doesn't exist
     * @throws 403 Forbidden if the user doesn't own the order
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
     * 
     * @param userId The unique identifier for the user
     * @return Portfolio containing cash balance and asset holdings
     */
    @GetMapping("/portfolio/{userId}")
    public ResponseEntity<Portfolio> getPortfolio(@PathVariable String userId) {
        return ResponseEntity.ok(portfolioService.getPortfolio(userId));
    }
    
    /**
     * Get portfolio total value (cash + assets at current market prices)
     * 
     * @param userId The unique identifier for the user
     * @return Total portfolio value in USD
     */
    @GetMapping("/portfolio/{userId}/value")
    public ResponseEntity<Double> getPortfolioValue(@PathVariable String userId) {
        return ResponseEntity.ok(portfolioService.getPortfolioValue(userId));
    }
    
    /**
     * Deposit funds to portfolio
     * 
     * @param userId The unique identifier for the user
     * @param request Object containing the amount to deposit
     * @return 200 OK if deposit successful
     * @throws 400 Bad Request if amount is invalid (negative or zero)
     */
    @PostMapping("/portfolio/{userId}/deposit")
    public ResponseEntity<?> depositFunds(
            @PathVariable String userId, 
            @RequestBody FundsRequestDTO request) {
        try {
            portfolioService.depositFunds(userId, request.getAmount());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Withdraw funds from portfolio
     * 
     * @param userId The unique identifier for the user
     * @param request Object containing the amount to withdraw
     * @return 200 OK if withdrawal successful
     * @throws 400 Bad Request if amount is invalid or exceeds available balance
     */
    @PostMapping("/portfolio/{userId}/withdraw")
    public ResponseEntity<?> withdrawFunds(
            @PathVariable String userId, 
            @RequestBody FundsRequestDTO request) {
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