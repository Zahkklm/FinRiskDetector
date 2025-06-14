package com.riskengine.risksystem.market.service;

import com.riskengine.risksystem.market.model.*;
import com.riskengine.risksystem.market.simulation.MarketSimulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user portfolios and executes trades
 */
@Service
@Slf4j
public class PortfolioService {
    /** User portfolios (userId → portfolio) */
    private final Map<String, Portfolio> portfolios = new ConcurrentHashMap<>();
    
    @Autowired
    private MarketSimulator marketSimulator;
    
    /**
     * Get or create a user's portfolio
     * 
     * @param userId User ID
     * @return User's portfolio
     */
    public Portfolio getPortfolio(String userId) {
        return portfolios.computeIfAbsent(userId, id -> {
            Portfolio portfolio = new Portfolio(id);
            portfolio.setCashBalance(10000.0); // Default starting balance
            return portfolio;
        });
    }
    
    /**
     * Execute a trade by updating the user's portfolio
     * 
     * @param order The executed order
     * @param executionPrice Price at which the order was executed
     * @return true if successful, false if failed (e.g., insufficient funds)
     */
    public boolean executeTrade(Order order, double executionPrice) {
        if (order.getStatus() != Order.OrderStatus.FILLED) {
            return false; // Only execute filled orders
        }
        
        Portfolio portfolio = getPortfolio(order.getUserId());
        double tradeValue = order.getQuantity() * executionPrice;
        
        if (order.getSide() == Order.OrderSide.BUY) {
            // Check if user has enough cash
            if (portfolio.getCashBalance() < tradeValue) {
                log.warn("Insufficient funds for trade: {}", order);
                return false;
            }
            
            // Update portfolio
            portfolio.withdrawCash(tradeValue);
            portfolio.addHolding(order.getSymbol(), order.getQuantity());
            
            log.info("Buy executed: {} {} of {} at ${}", 
                    order.getUserId(), order.getQuantity(), 
                    order.getSymbol(), executionPrice);
            
        } else { // SELL
            // Check if user has enough of the asset
            if (!portfolio.removeHolding(order.getSymbol(), order.getQuantity())) {
                log.warn("Insufficient holdings for trade: {}", order);
                return false;
            }
            
            // Update portfolio
            portfolio.depositCash(tradeValue);
            
            log.info("Sell executed: {} {} of {} at ${}", 
                    order.getUserId(), order.getQuantity(), 
                    order.getSymbol(), executionPrice);
        }
        
        return true;
    }
    
    /**
     * Calculate total portfolio value for a user
     * 
     * @param userId User ID
     * @return Total portfolio value
     */
    public double getPortfolioValue(String userId) {
        Portfolio portfolio = getPortfolio(userId);
        Map<String, AssetPrice> prices = marketSimulator.getAllPrices();
        
        return portfolio.calculateTotalValue(prices);
    }
    
    /**
     * Add funds to a user's portfolio
     * 
     * @param userId User ID
     * @param amount Amount to add
     */
    public void depositFunds(String userId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        
        Portfolio portfolio = getPortfolio(userId);
        portfolio.depositCash(amount);
        
        log.info("Funds deposited: {} added ${}", userId, amount);
    }
    
    /**
     * Withdraw funds from a user's portfolio
     * 
     * @param userId User ID
     * @param amount Amount to withdraw
     * @return true if successful, false if insufficient funds
     */
    public boolean withdrawFunds(String userId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        
        Portfolio portfolio = getPortfolio(userId);
        boolean success = portfolio.withdrawCash(amount);
        
        if (success) {
            log.info("Funds withdrawn: {} withdrew ${}", userId, amount);
        } else {
            log.warn("Withdrawal failed: {} insufficient funds for ${}", userId, amount);
        }
        
        return success;
    }
}